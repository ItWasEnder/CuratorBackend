package discord;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import tv.ender.discord.backend.Prediction;
import tv.ender.discord.backend.Result;
import tv.ender.firebase.backend.UserData;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.*;

public class PredictionTest {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String NUMBERS = "0123456789";
    private static final Set<UserData> dataMap = new HashSet<>();
    private static final Random rand = new Random();

    @Rule
    public final Timeout timeout = Timeout.seconds(10);

    @Before
    public void setup() {
        dataMap.clear();

        /* populate user map */
        for (int i = 0; i < rand.nextInt(10, 55); i++) {
            dataMap.add(UserData.of(randomString(8), randomId(18), rand.nextInt(1, 1024), 0));
        }
    }

    @Test
    public void testFullPrediction() {
        Prediction prediction = createPrediction();

        final var toUse = new ConcurrentLinkedQueue<>(dataMap);
        final Iterator<UserData> it = toUse.iterator();
        Map<String, Integer> depositedTickets = new ConcurrentHashMap<>();
        Map<String, String> optionPicks = new ConcurrentHashMap<>();
        Map<String, Integer> initialBalances = new ConcurrentHashMap<>();
        ThreadGroup group = new ThreadGroup("PredictionTest");
        Thread[] threadPool = new Thread[10];

        final String[] options = new String[]{"Option1", "Option2"};

        System.out.println("Starting threads...");
        for (int i = 0; i < threadPool.length; i++) {
            threadPool[i] = new Thread(group, () -> {
                while (it.hasNext()) {
                    final UserData data = it.next();

                    if (data == null) {
                        continue;
                    }

                    final int ticketAmount = rand.nextInt(1, data.getTickets());

                    /* pick random option */
                    final String option = options[rand.nextInt(options.length)];

                    initialBalances.put(data.getDiscordId(), data.getTickets());
                    Result<UserData> result = prediction.enter(data, ticketAmount, option);

                    if (result.isSuccessful()) {
                        depositedTickets.compute(data.getDiscordId(), (k, v) -> v == null ? ticketAmount : v + ticketAmount);
                        optionPicks.put(data.getDiscordId(), option);

                        synchronized (toUse) {
                            toUse.remove(data);
                        }
                    } else {
                        System.out.println(data.getName() + " " + result.getMessage());
                    }
                }

                threadPool[0].interrupt();
            });

            threadPool[i].start();
        }

        while (group.activeCount() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.printf("Finished entering %d users into prediction...%n", optionPicks.keySet().size());
        assertEquals(dataMap.size(), prediction.getEntrants().size());

        for (var entry : prediction.getEntrantsTicketsMap().entrySet()) {
            var user = entry.getKey();
            var amount = entry.getValue();

            if (depositedTickets.containsKey(user.getDiscordId())) {
                if (!Objects.equals(depositedTickets.get(user.getDiscordId()), amount)) {
                    fail(String.format("Expected %s put in %d tickets, however %d was tracked", user.getName(), depositedTickets.get(user.getDiscordId()), amount));
                    return;
                }
            } else {
                fail(String.format("Expected %s to not be in the raffle, but they were", user.getName()));
                return;
            }
        }

        System.out.println("Finished checking entrants...");
        System.out.println("Starting winner selection...");
        String winningOption = options[rand.nextInt(options.length)];

        var expectedWiningCount = optionPicks.values().stream().filter(option -> option.equals(winningOption)).count();
        var winnersResult = prediction.end(winningOption);
        var winners = winnersResult.getHolder();

        if (!winnersResult.isSuccessful()) {
            fail(winnersResult.getMessage());
            return;
        }

        System.out.printf("Checking winning selection %d/%d...%n", winners.size(), expectedWiningCount);
        assertEquals(expectedWiningCount, winners.size());

        for (var user : winners) {
            var oldBalance = initialBalances.get(user.getDiscordId());

            if (user.getTickets() <= oldBalance) {
                fail(String.format("Expected %s to have more tickets than before: new: %d | old: %d", user.getName(), user.getTickets(), oldBalance));
                return;
            }
        }

        System.out.println("All winners paid out sufficiently...");
    }

    @Test
    public void failAlreadyPickedOption() {
        var randomUser = dataMap.iterator().next();
        var prediction = createPrediction();

        prediction.enter(randomUser, 1, "Option1");

        var result = prediction.enter(randomUser, 1, "Option2");

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    @Test
    public void failNotEnoughTickets() {
        var randomUser = dataMap.iterator().next();
        var prediction = createPrediction();

        var result = prediction.enter(randomUser, Integer.MAX_VALUE, "Option1");

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    @Test
    public void failPredictionClosed() {
        var randomUser = dataMap.iterator().next();
        var prediction = createPrediction();
        prediction.enter(randomUser, 1, "Option1");

        prediction.end("Option1");

        var result = prediction.enter(randomUser, 1, "Option1");

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    @Test
    public void failIncorrectWinningOption() {
        var randomUser = dataMap.iterator().next();
        var prediction = createPrediction();
        prediction.enter(randomUser, 1, "Option1");

        var result = prediction.end("Option2");

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    @Test
    public void passEnterMultipleTimes() {
        var randomUser = dataMap.iterator().next().setTickets(100);
        var prediction = createPrediction();

        int ticketsBet = 0;
        for (int i = 1; i < 10; i++) {
            var result = prediction.enter(randomUser, i, "Option1");
            ticketsBet += i;

            assertTrue(result.isSuccessful());

            System.out.println(result.getMessage());
        }

        assertEquals(ticketsBet, (long) prediction.getEntrantsTicketsMap().get(randomUser));
    }

    @Test
    public void resetPrediction() {
        var randomUser = dataMap.iterator().next().setTickets(100);
        var prediction = createPrediction();

        prediction.enter(randomUser, 100, "Option1");

        assertEquals(0, randomUser.getTickets());
        assertEquals(1, prediction.getEntrants().size());
        assertEquals(100, prediction.getEntrantsTicketsMap().get(randomUser).intValue());

        var result = prediction.reset();

        assertEquals(0, prediction.getEntrants().size());

        assertEquals(100, randomUser.getTickets());

        assertTrue(result.isSuccessful());
    }

    private static String randomString(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be positive");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(rand.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private static String randomId(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be positive");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(NUMBERS.charAt(rand.nextInt(NUMBERS.length())));
        }
        return sb.toString();
    }

    private static Prediction createPrediction() {
        return new Prediction();
    }
}
