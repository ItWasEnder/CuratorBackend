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
        
        future.completeAsync(() -> {
            if (this.lock.read(() -> this.userDataMap.containsKey(userData.getDiscordId()))) {
                throw new IllegalArgumentException("User already exists!");
            } else {
                return this.lock.write(() -> this.userDataMap.put(userData.getDiscordId(), userData));
            }
        });

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

        future.completeAsync(() -> {
            if (this.lock.read(() -> !this.userDataMap.containsKey(discordId))) {
                throw new IllegalArgumentException("User does not exist!");
            } else {
                return this.lock.write(() -> this.userDataMap.remove(discordId));
            }
        });

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

        future.completeAsync(() -> {
            return this.lock.read(() -> this.userDataMap.get(discordId));
        });

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

        future.completeAsync(() -> {
            if (this.lock.read(() -> this.activities.containsKey(activity.getUuid()))) {
                throw new IllegalArgumentException("Activity already exists!");
            } else {
                this.lock.write(() -> this.activities.put(activity.getUuid(), activity));
                return activity;
            }
        });

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

        future.completeAsync(() -> {
            return this.lock.read(() -> (T) this.activities.get(uuid));
        });

        return future;
    }
}
