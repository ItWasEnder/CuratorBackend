package tv.ender.discord.backend;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import tv.ender.common.ReadWriteLock;
import tv.ender.discord.backend.interfaces.IActivity;
import tv.ender.firebase.backend.GuildData;
import tv.ender.firebase.backend.UserData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Accessors(chain = true)
@AllArgsConstructor(staticName = "of")
public class GuildInstance {
    @Getter(AccessLevel.NONE)
    private final ReadWriteLock lock = new ReadWriteLock();

    @Getter(AccessLevel.NONE)
    private final Map<String, UserData> userDataMap = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Map<UUID, IActivity> activities = new ConcurrentHashMap<>();

    private final GuildData guildData;

    /**
     * Adds a user to the guild instance
     *
     * @param userData The user to add
     * @return A completable future that will complete with the result of the operation
     */
    public CompletableFuture<UserData> addUser(UserData userData) {
        final var future = new CompletableFuture<UserData>();

        try {
            this.lock.readLock();

            if (this.userDataMap.containsKey(userData.getDiscordId())) {
                future.completeExceptionally(new IllegalArgumentException("User already exists!"));
                return future;
            }

            this.lock.readUnlock();
            try {
                this.lock.writeLock();

                future.complete(this.userDataMap.put(userData.getDiscordId(), userData));
            } finally {
                this.lock.writeUnlock();
            }
        } finally {
            this.lock.releaseAnyReadLocks();
        }

        return future;
    }

    /**
     * Removes a user from the guild instance
     *
     * @param discordId The discord ID of the user to remove
     * @return A completable future that will complete with the result of the operation
     */
    public CompletableFuture<UserData> removeUser(String discordId) {
        final var future = new CompletableFuture<UserData>();

        try {
            this.lock.readLock();

            if (!this.userDataMap.containsKey(discordId)) {
                future.completeExceptionally(new NullPointerException("User does not exist!"));
                return future;
            }
            final var user = this.userDataMap.get(discordId);

            /* remove user */
            this.lock.readUnlock();
            try {
                this.lock.writeLock();

                this.userDataMap.remove(discordId);
            } finally {
                this.lock.writeUnlock();
            }

            /* destroy data */
            // TODO Unknown if this should happen. Depends on data storage cost.

            future.complete(user);
        } finally {
            this.lock.releaseAnyReadLocks();
        }

        return future;
    }

    /**
     * Gets a user from the guild instance
     *
     * @param discordId The discord ID of the user to get
     * @return A completable future that will complete with the result of the operation
     */
    public CompletableFuture<UserData> getUser(String discordId) {
        final var future = new CompletableFuture<UserData>();

        try {
            this.lock.readLock();

            try {
                future.complete(this.userDataMap.get(discordId));
            } catch (ClassCastException e) {
                future.completeExceptionally(e);
            }
        } finally {
            this.lock.readUnlock();
        }

        return future;
    }

    /**
     * Adds an activity to the guild instance
     *
     * @param activity The activity to add
     * @return A completable future that will complete with the result of the operation
     */
    public <T extends IActivity> CompletableFuture<T> addActivity(T activity) {
        final var future = new CompletableFuture<T>();

        try {
            this.lock.readLock();

            if (this.activities.containsKey(activity.getUuid())) {
                future.completeExceptionally(new IllegalArgumentException("Activity already exists!"));
                return future;
            }

            this.lock.readUnlock();
            try {
                this.lock.writeLock();

                this.activities.put(activity.getUuid(), activity);

                future.complete(activity);
            } finally {
                this.lock.writeUnlock();
            }
        } finally {
            this.lock.releaseAnyReadLocks();
        }

        return future;
    }

    /**
     * Gets an activity from the guild instance
     *
     * @param uuid The UUID of the activity to get
     * @return A completable future that will complete with the result of the operation
     */
    public <T extends IActivity> CompletableFuture<T> getActivity(UUID uuid) {
        final var future = new CompletableFuture<T>();

        try {
            this.lock.readLock();

            var got = this.activities.get(uuid);

            try {
                future.complete((T) got);
            } catch (ClassCastException e) {
                future.completeExceptionally(e);
            }
        } finally {
            this.lock.readUnlock();
        }

        return future;
    }
}
