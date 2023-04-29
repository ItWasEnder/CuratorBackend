package tv.ender.discord.backend;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import tv.ender.common.ReadWriteLock;
import tv.ender.discord.backend.interfaces.IActivity;
import tv.ender.firebase.backend.GuildData;
import tv.ender.firebase.backend.UserData;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Accessors(chain = true)
public class GuildInstance {
    @Getter(AccessLevel.NONE)
    private final ReadWriteLock lock = new ReadWriteLock();

    @Getter(AccessLevel.NONE)
    private final Map<String, UserData> userDataMap = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Map<UUID, IActivity> activities = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Map<Snowflake, IActivity> activityMessages = new ConcurrentHashMap<>();

    private final GuildData guildData;

    @Setter
    private Guild guild;

    private GuildInstance(GuildData guildData) {
        this.guildData = guildData;
    }

    public static GuildInstance of(GuildData guildData) {
        return new GuildInstance(guildData);
    }

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

    public CompletableFuture<UserData> getUser(Member member) {
        var future = new CompletableFuture<UserData>();

        future.completeAsync(() -> {
            if (this.lock.read(() -> this.userDataMap.containsKey(member.getId().asString()))) {
                var userData = this.getUser(member.getId().asString()).join();

                if (userData.getMember() == null) {
                    userData.setMember(member);
                }

                return userData;
            } else {
                var userData = UserData.of(member);

                userData.setTokens(this.getGuildData().getStartingTickets());
                userData.setMember(member);

                this.addUser(userData);

                return userData;
            }
        });

        return future;
    }

    public CompletableFuture<Optional<IActivity>> getActivityFromMessage(Snowflake message) {
        final var future = new CompletableFuture<Optional<IActivity>>();

        future.completeAsync(() -> {
            return this.lock.read(() -> {
                return Optional.ofNullable(this.activityMessages.get(message));
            });
        });

        return future;
    }

    public <T extends IActivity> void mapActivityMessage(Snowflake message, T activity) {
        if (this.lock.read(() -> this.activityMessages.containsKey(message))) {
            throw new IllegalArgumentException("Message already mapped!");
        }

        this.lock.write(() -> this.activityMessages.put(message, activity));
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
    public CompletableFuture<IActivity> getActivity(UUID uuid) {
        final var future = new CompletableFuture<IActivity>();

        future.completeAsync(() -> {
            return this.lock.read(() -> this.activities.get(uuid));
        });

        return future;
    }
}
