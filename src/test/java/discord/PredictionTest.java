package discord;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import tv.ender.common.ReadWriteLock;
import tv.ender.common.Result;
import tv.ender.discord.backend.activities.Prediction;
import tv.ender.firebase.backend.UserData;
import tv.ender.test.TestUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;


class PredictionTest {
    private static final Random rand = new Random();
    private static Set<UserData> dataMap = new HashSet<>();

    @BeforeAll
    static void setup() {
        dataMap = new HashSet<>(
                TestUtils.createUsers(rand.nextInt(10, 55))
        );

        /* check duplicates */
        boolean dupes = TestUtils.uniqueItems(dataMap.toArray(new UserData[0]));
        System.out.println("Checking for duplicates...");
        System.out.println("NoDuplicates: " + dupes);
        assertTrue(dupes);
    }

    @Test
    void testFullPrediction() {
        System.out.println("DataMap size: " + dataMap.size());

        Prediction prediction = createPrediction();

        final var toUse = new ConcurrentLinkedQueue<>(dataMap);
        final Iterator<UserData> it = toUse.iterator();
        ReadWriteLock lock = new ReadWriteLock();
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

                    if (lock.read(() -> depositedTickets.containsKey(data.getDiscordId()))) {
                        System.out.println("Handled by another thread...");
                        continue;
                    }

                    if (data.getTokens() < 1) {
                        System.out.println("No tokens...");
                        continue;
                    }

                    final int ticketAmount = data.getTokens() == 1 ? 1 : rand.nextInt(1, data.getTokens());

                    /* pick random option */
                    final String option = options[rand.nextInt(options.length)];
                    Result<UserData> result = prediction.enter(data, ticketAmount, option);

                    if (result.isSuccessful()) {
                        lock.write(() -> {
                            depositedTickets.compute(data.getDiscordId(), (k, v) -> v == null ? ticketAmount : v + ticketAmount);
                            initialBalances.putIfAbsent(data.getDiscordId(), data.getTokens());
                            optionPicks.put(data.getDiscordId(), option);
                        });
                    }

                    synchronized (toUse) {
                        toUse.remove(data);
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
        assertEquals(dataMap.size(), prediction.getParticipants().size());

        for (var entry : prediction.getEntrantsTokenMap().entrySet()) {
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

        System.out.println("Total tickets entered " + prediction.getTotalTickets());
        System.out.printf("Checking winning selection %d/%d...%n", winners.size(), expectedWiningCount);
        assertEquals(expectedWiningCount, winners.size());

        for (var user : winners) {
            var oldBalance = initialBalances.get(user.getDiscordId());
            var onePercent = (int) (prediction.getTotalTickets() * 0.01);
            var bet = prediction.getEntrantsTokenMap().get(user);

            if (user.getTokens() <= oldBalance && oldBalance > onePercent && bet > 0) {
                System.out.println("Bet placed: " + bet);
                fail(String.format("Expected %s to have more tickets than before: new: %d | old: %d", user.getName(), user.getTokens(), oldBalance));
                return;
            }
        }

        System.out.println("All winners paid out sufficiently...");
    }

    @Test
    void failBetTooSmall() {
        var randomUser = dataMap.iterator().next();
        var prediction = createPrediction();

        var result = prediction.enter(randomUser, 0, "Option1");

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    @Test
    void failAlreadyPickedOption() {
        var randomUser = dataMap.iterator().next();
        var prediction = createPrediction();

        prediction.enter(randomUser, 1, "Option1");

        var result = prediction.enter(randomUser, 1, "Option2");

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    @Test
    void failNotEnoughTickets() {
        var randomUser = dataMap.iterator().next();
        var prediction = createPrediction();

        var result = prediction.enter(randomUser, Integer.MAX_VALUE, "Option1");

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    @Test
    void failPredictionClosed() {
        var randomUser = dataMap.iterator().next();
        var prediction = createPrediction();
        prediction.enter(randomUser, 1, "Option1");

        prediction.end("Option1");

        var result = prediction.enter(randomUser, 1, "Option1");

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    @Test
    void failIncorrectWinningOption() {
        var randomUser = dataMap.iterator().next();
        var prediction = createPrediction();
        prediction.enter(randomUser, 1, "Option1");

        var result = prediction.end("Option2");

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    @Test
    void passEnterMultipleTimes() {
        var randomUser = dataMap.iterator().next().setTokens(100);
        var prediction = createPrediction();

        int ticketsBet = 0;
        for (int i = 1; i < 10; i++) {
            var result = prediction.enter(randomUser, i, "Option1");
            ticketsBet += i;

            assertTrue(result.isSuccessful());

            System.out.println(result.getMessage());
        }

        assertEquals(ticketsBet, (long) prediction.getEntrantsTokenMap().get(randomUser));

        System.out.println("Total tickets bet: " + ticketsBet + " | " + prediction.getEntrantsTokenMap().get(randomUser));
    }

    @Test
    void resetPrediction() {
        var randomUser = dataMap.iterator().next().setTokens(100);
        var prediction = createPrediction();

        prediction.enter(randomUser, 100, "Option1");

        assertEquals(0, randomUser.getTokens());
        assertEquals(1, prediction.getParticipants().size());
        assertEquals(100, prediction.getEntrantsTokenMap().get(randomUser).intValue());

        var result = prediction.reset();

        assertEquals(0, prediction.getParticipants().size());

        assertEquals(100, randomUser.getTokens());

        assertTrue(result.isSuccessful());
    }


    private static Prediction createPrediction() {
        return new Prediction();
    }
}
