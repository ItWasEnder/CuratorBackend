package tv.ender.discord.backend.activities;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import tv.ender.common.ReadWriteLock;
import tv.ender.common.Result;
import tv.ender.discord.backend.BonusStatus;
import tv.ender.discord.backend.interfaces.IActivity;
import tv.ender.firebase.backend.UserData;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Accessors(chain = true)
public class Raffle implements IActivity {
    public static final int BASE_TICKETS = 100;
    public static final int LOSS_MULTIPLIER = 5;

    private final ReadWriteLock lock = new ReadWriteLock();
    private final Map<UserData, Integer> entrantsTicketsMap = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean canEnter = new AtomicBoolean(true);
    private final AtomicLong endTime = new AtomicLong(-1);
    private final UUID uuid = UUID.randomUUID();
    @Setter
    private int winnerSlots = 1;

    @Override
    public Set<UserData> getParticipants() {
        try {
            this.lock.readLock();

            return this.entrantsTicketsMap.keySet();
        } finally {
            this.lock.readUnlock();
        }
    }

    public int getTotalTickets() {
        try {
            this.lock.readLock();

            return this.entrantsTicketsMap.values().stream().mapToInt(Integer::intValue).sum();
        } finally {
            this.lock.readUnlock();
        }
    }

    /**
     * End the raffle
     *
     * @return the winner of the raffle
     */
    public Result<UserData[]> end() {
        /* shutdown raffle */
        this.running.set(false);

        /* Pick a random winner weighting to the number of tickets they have */
        this.endTime.set(System.currentTimeMillis());

        final UserData[] winners = new UserData[this.winnerSlots];
        final int totalTickets = this.getTotalTickets();
        final Map<UserData, Double> entrantsWeightMap = new HashMap<>();
        final Random random = ThreadLocalRandom.current();

        /* calculate probabilities of entrants */
        try {
            this.lock.readLock();

            for (final Map.Entry<UserData, Integer> entry : this.entrantsTicketsMap.entrySet()) {
                entrantsWeightMap.put(entry.getKey(), (double) entry.getValue() / totalTickets);
            }
        } finally {
            this.lock.readUnlock();
        }

        for (int i = 0; i < this.winnerSlots; i++) {
            double randomValue = random.nextDouble();
            double cumulativeProbability = 0.0;

            for (final Map.Entry<UserData, Double> entry : entrantsWeightMap.entrySet()) {
                cumulativeProbability += entry.getValue();

                if (randomValue <= cumulativeProbability) {
                    winners[i] = entry.getKey();
                    break;
                }
            }
        }

        return Result.pass(winners, "Raffle ended successfully");
    }

    /**
     * Reset the raffle
     */
    public Result<Raffle> reset() {
        if (!this.running.get()) {
            return Result.fail(this, "Raffle is not running");
        }

        /* disable entries */
        this.running.set(false);

        try {
            this.lock.writeLock();
            this.entrantsTicketsMap.clear();
        } finally {
            this.lock.writeUnlock();
        }

        this.running.set(true);

        return Result.pass(this, "Raffle reset successfully");
    }

    /**
     * Enter a user into the raffle
     *
     * @param user The user to enter
     * @param tier The tier of the user
     */
    public Result<UserData> enter(UserData user, BonusStatus tier) {
        if (!this.running.get()) {
            return Result.fail(user, "Raffle is not running");
        }

        if (!this.canEnter.get()) {
            return Result.fail(user, "Raffle is not accepting entries");
        }

        /* calculate tickets */
        int tickets = BASE_TICKETS + this.getTierBonus(tier) + (user.getLosses() * LOSS_MULTIPLIER);

        /* deposit tickets */
        try {
            this.lock.readLock();

            if (this.entrantsTicketsMap.containsKey(user)) {
                return Result.fail(user, "User already entered into the raffle");
            } else {
                this.lock.readUnlock();

                try {
                    this.lock.writeLock();
                    this.entrantsTicketsMap.put(user, tickets);
                } finally {
                    this.lock.writeUnlock();
                }
            }
        } finally {
            this.lock.releaseAnyReadLocks();
        }

        return Result.pass(user, "Entered with %d tickets into raffle".formatted(tickets));
    }

    private int getTierBonus(BonusStatus tier) {
        return switch (tier) {
            case BOOSTER -> 25;
            case TIER_1, TIER_1_GIFTED -> 50;
            case TIER_2, TIER_2_GIFTED -> 100;
            case TIER_3, TIER_3_GIFTED -> 150;
            default -> 0;
        };
    }

    @Override
    public void cancel() {
        this.reset();
        this.running.set(false);
    }
}
