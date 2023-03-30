package tv.ender.discord.backend;

import lombok.Getter;
import lombok.experimental.Accessors;
import tv.ender.common.ReadWriteLock;
import tv.ender.firebase.backend.UserData;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Accessors(chain = true)
public class Raffle {
    private final ReadWriteLock lock = new ReadWriteLock();
    private final Map<UserData, Integer> entrantsTicketsMap = new ConcurrentHashMap<>();
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
     * End the raffle
     *
     * @return the winner of the raffle
     */
    public Result<UserData> end() {
        /* shutdown raffle */
        this.running.set(false);

        /* Pick a random winner weighting to the number of tickets they have */
        this.endTime.set(System.currentTimeMillis());
        // TODO: Implement


        return Result.fail("Not implemented");
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
     * @param user    The user to enter
     * @param tickets The number of tickets to enter with
     */
    public Result<UserData> enter(UserData user, int tickets) {
        if (!this.running.get()) {
            return Result.fail(user, "Raffle is not running");
        }

        if (tickets > user.getTickets()) {
            Result.fail(user, "Insufficient tickets: %d > %d".formatted(tickets, user.getTickets()));
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

        return Result.pass(user, "Deposited %d tickets into raffle".formatted(tickets));
    }
}
