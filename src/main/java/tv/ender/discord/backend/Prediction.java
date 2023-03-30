package tv.ender.discord.backend;

import lombok.Getter;
import lombok.experimental.Accessors;
import tv.ender.common.ReadWriteLock;
import tv.ender.firebase.backend.UserData;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Accessors(chain = true)
public class Prediction {
    private final ReadWriteLock lock = new ReadWriteLock();
    private final Map<UserData, Integer> entrantsTicketsMap = new ConcurrentHashMap<>();
    private final Map<UserData, String> entrantPick = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong endTime = new AtomicLong(-1);
    private final UUID identifier = UUID.randomUUID();

    public Set<UserData> getEntrants() {
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
     * End the prediction
     *
     * @param winning The winning option
     * @return A result containing the winners or null if failed
     */
    public Result<Set<UserData>> end(final String winning) {
        /* shutdown raffle */
        this.running.set(false);

        /* Pick a random winner weighting to the number of tickets they have */
        this.endTime.set(System.currentTimeMillis());
        try {
            this.lock.readLock();
            if (!this.entrantPick.containsValue(winning)) {
                return Result.fail("Invalid winning option \"%s\" for prediction \"%s\"".formatted(winning, this.identifier));
            }

            Set<UserData> winners = new HashSet<>();
            int winningBets = 0;
            int totalBets = this.getTotalTickets();
            for (var entry : this.entrantsTicketsMap.entrySet()) {
                if (Objects.equals(this.entrantPick.get(entry.getKey()), winning)) {
                    winners.add(entry.getKey());
                    winningBets += entry.getValue();
                }
            }

            /* payout */
            for (UserData winner : winners) {
                int viewerBet = this.entrantsTicketsMap.get(winner);
                int viewerPayout;

                if (winningBets != 0) {
                    viewerPayout = (int) Math.round(totalBets * (viewerBet / (double) winningBets));
                } else {
                    viewerPayout = viewerBet;
                }

                winner.setTickets(winner.getTickets() + viewerPayout);
            }

            return Result.pass(winners, "Prediction ended with %d winners and %d total bets"
                    .formatted(winners.size(), totalBets));
        } finally {
            this.lock.releaseAnyReadLocks();
        }
    }

    /**
     * Reset the prediction
     */
    public Result<Prediction> reset() {
        if (!this.running.get()) {
            return Result.fail(this, "Raffle is not running");
        }

        /* disable entries */
        this.running.set(false);

        try {
            /* refund all tickets */
            this.lock.writeLock();
            this.entrantsTicketsMap.forEach((user, tickets) -> user.setTickets(user.getTickets() + tickets));

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
     * @param user   The user to enter
     * @param option The number of tickets to enter with
     */
    public Result<UserData> enter(UserData user, int tickets, String option) {
        if (!this.running.get()) {
            return Result.fail(user, "Prediction is not running");
        }

        if (tickets > user.getTickets()) {
            return Result.fail(user, "Insufficient tickets: %d > %d".formatted(tickets, user.getTickets()));
        }

        /* check user choice */
        try {
            this.lock.readLock();
            if (this.entrantPick.containsKey(user) && !this.entrantPick.get(user).equals(option)) {
                return Result.fail(user, "Cannot chance choice after entering");
            } else {
                this.lock.readUnlock();

                try {
                    this.lock.writeLock();
                    this.entrantPick.put(user, option);
                } finally {
                    this.lock.writeUnlock();
                }
            }
        } finally {
            this.lock.releaseAnyReadLocks();
        }

        /* deposit tickets */
        try {
            this.lock.readLock();
            if (this.entrantsTicketsMap.containsKey(user)) {
                this.lock.readUnlock();

                try {
                    this.lock.writeLock();
                    this.entrantsTicketsMap.put(user, tickets + this.entrantsTicketsMap.get(user));
                } finally {
                    this.lock.writeUnlock();
                }
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

        /* subtract tickets */
        user.setTickets(user.getTickets() - tickets);

        return Result.pass(user, "Bet %d tickets on %s".formatted(tickets, option));
    }
}
