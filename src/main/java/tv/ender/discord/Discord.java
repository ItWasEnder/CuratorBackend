package tv.ender.discord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import tv.ender.common.ReadWriteLock;
import tv.ender.common.Result;
import tv.ender.discord.backend.GuildInstance;
import tv.ender.discord.backend.interfaces.EventListener;
import tv.ender.discord.listeners.MessageCreateListener;
import tv.ender.firebase.backend.GuildData;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Discord {
    private static final Logger LOG = LoggerFactory.getLogger(Discord.class);
    private static Discord instance;

    private final Map<String, GuildInstance> guildInstances = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReadWriteLock();
    private GatewayDiscordClient client;

    private Discord() {
        // Empty
    }

    /**
     * Get the singleton instance of the Discord bot
     *
     * @param listener The listener to register
     * @param <T>      The type of event to listen for
     */
    public <T extends Event> void registerListener(EventListener<T> listener) {
        this.client.on(listener.getEventType())
                .flatMap(listener::execute)
                .onErrorResume(listener::handleError)
                .subscribe();
    }

    /**
     * Initiate the bot connection to the Discord API
     */
    public void connect() {
        System.out.println("Initializing Discord Bot...");

        DiscordClientBuilder.create(System.getenv().get("BOT_TOKEN"))
                .build()
                .login()
                .flatMap(gateway -> {
                    System.out.println("Discord Bot is now running!");

                    this.client = gateway;

//                    this.registerListener(new MessageCreateListener());

                    return Mono.just(gateway);
                })
                .onErrorResume(throwable -> {
                    LOG.error("Failed to connect to Discord API!", throwable);
                    return Mono.empty();
                })
                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                .block();

        /* keep thread alive */
        this.client.onDisconnect().block();

        System.out.println("Shutting down Discord Bot...");
    }

    /**
     * Gets the singleton instance of the Discord class
     *
     * @return The singleton instance of the gateway
     */
    public CompletableFuture<GatewayDiscordClient> getClient() {
        final var future = new CompletableFuture<GatewayDiscordClient>();

        if (this.client == null) {
            future.completeExceptionally(new IllegalStateException("Gateway is not connected!"));
        } else {
            future.complete(this.client);
        }

        return future;
    }

    /**
     * Gets the singleton instance of the Discord class
     *
     * @param guildId The guild ID to get the instance for
     * @return A completable future that will complete with the result of the operation
     */
    public CompletableFuture<Result<GuildInstance>> getGuildInstance(String guildId) {
        final var future = new CompletableFuture<Result<GuildInstance>>();

        try {
            this.lock.readLock();

            if (!this.guildInstances.containsKey(guildId)) {
                future.complete(Result.fail("No Guild instance registered for " + guildId));
            } else {
                future.complete(Result.pass(this.guildInstances.get(guildId), "Guild instance is available"));
            }
        } finally {
            this.lock.readUnlock();
        }

        return future;
    }

    /**
     * Registers a guild instance for the given guild data
     *
     * @param guildData The guild data to register
     * @return A completable future that will complete with the result of the operation
     */
    public CompletableFuture<Result<GuildInstance>> registerGuildInstance(GuildData guildData) {
        final var future = new CompletableFuture<Result<GuildInstance>>();

        try {
            this.lock.readLock();

            if (!this.guildInstances.containsKey(guildData.getGuildId())) {
                this.lock.readUnlock();

                try {
                    this.lock.writeLock();

                    var inst = this.guildInstances.put(guildData.getGuildId(), GuildInstance.of(guildData));

                    future.complete(Result.pass(inst, "Guild instance registered"));
                } finally {
                    this.lock.writeUnlock();
                }
            } else {
                future.complete(Result.fail("Guild instance already registered for guild " + guildData.getGuildId()));
            }
        } finally {
            this.lock.releaseAnyReadLocks();
        }

        return future;
    }

    public static Discord get() {
        if (Discord.instance == null) {
            Discord.instance = new Discord();
        }
        return Discord.instance;
    }
}
