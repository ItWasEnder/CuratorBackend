package discord;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import tv.ender.common.ReadWriteLock;
import tv.ender.common.Result;
import tv.ender.discord.backend.BonusStatus;
import tv.ender.discord.backend.activities.Raffle;
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


class RaffleTest {
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
    void testFullRaffle() {
        System.out.println("DataMap size: " + dataMap.size());

        Raffle raffle = createRaffle().setWinnerSlots(3);

        final var toUse = new ConcurrentLinkedQueue<>(dataMap);
        final Iterator<UserData> it = toUse.iterator();
        Map<String, Integer> calculatedTickets = new ConcurrentHashMap<>();
        ReadWriteLock lock = new ReadWriteLock();
        ThreadGroup group = new ThreadGroup("RaffleTest");
        Thread[] threadPool = new Thread[10];

        System.out.println("Starting threads...");
        for (int i = 0; i < threadPool.length; i++) {
            threadPool[i] = new Thread(group, () -> {
                while (it.hasNext()) {
                    final UserData data = it.next();

                    if (data == null) {
                        continue;
                    }

                    if (lock.read(() -> calculatedTickets.containsKey(data))) {
                        System.out.println("Handled by different thread...");
                        return;
                    }

                    /* pick random status */
                    final BonusStatus bonusStatus = BonusStatus.values()[rand.nextInt(BonusStatus.values().length)];

                    Result<UserData> result = raffle.enter(data, bonusStatus);

                    if (result.isSuccessful()) {
                        final int tickets = raffle.calculateTickets(data, bonusStatus);

                        lock.write(() -> {
                            calculatedTickets.computeIfAbsent(data.getDiscordId(), k -> tickets);
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

        System.out.printf("Finished entering %d users into prediction...%n", calculatedTickets.keySet().size());
        assertEquals(dataMap.size(), raffle.getParticipants().size());
        assertTrue(TestUtils.uniqueItems(raffle.getParticipants().toArray(new UserData[0])));

        for (var entry : raffle.getEntrantsTicketsMap().entrySet()) {
            var user = entry.getKey();
            var amount = entry.getValue();

            if (calculatedTickets.containsKey(user.getDiscordId())) {
                if (!Objects.equals(calculatedTickets.get(user.getDiscordId()), amount)) {
                    fail(String.format("Expected %s to have %d tickets in raffle, however %d was tracked", user.getName(), calculatedTickets.get(user.getDiscordId()), amount));
                    return;
                }
            } else {
                fail(String.format("Expected %s to not be in the raffle, but they were", user.getName()));
                return;
            }
        }

        System.out.println("Finished checking entrants...");
        System.out.println("Starting winner selection...");

        var winnersResult = raffle.end();
        UserData[] winners = winnersResult.getHolder();

        if (!winnersResult.isSuccessful()) {
            fail(winnersResult.getMessage());
            return;
        }

        System.out.printf("Checking winning selection %d/%d...%n", winners.length, raffle.getWinnerSlots());
        assertEquals(winners.length, raffle.getWinnerSlots());

        assertTrue(TestUtils.uniqueItems(winners), "There is a duplicate winner! " + TestUtils.toString(winners));

        for (var user : winners) {
            // Get the total number of tickets entered by all participants
            int total = raffle.getTotalTickets();

            // Get the number of tickets entered by the winner
            int winnerTickets = raffle.getEntrantsTicketsMap().get(user);

            // Calculate the probability of winning and losing
            double winProbability = (double) winnerTickets / total;
            double loseProbability = 1 - winProbability;

            // Calculate the odds of winning
            double odds = winProbability / loseProbability;

            System.out.println(user.getName() + " won with an odds of " + 1 + ":" + (int) Math.round(1 / odds));
        }
    }

    @Test
    void failAlreadyEntered() {
        var randomUser = dataMap.iterator().next();
        var prediction = createRaffle();

        prediction.enter(randomUser, BonusStatus.NONE);

        var result = prediction.enter(randomUser, BonusStatus.NONE);

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    @Test
    void failRaffleClosed() {
        var randomUser = dataMap.iterator().next();
        var raffle = createRaffle();
        raffle.enter(randomUser, BonusStatus.NONE);

        raffle.end();

        var result = raffle.enter(TestUtils.createUserData(), BonusStatus.NONE);

        assertFalse(result.isSuccessful());

        System.out.println(result.getMessage());
    }

    private static Raffle createRaffle() {
        return new Raffle();
    }
}
