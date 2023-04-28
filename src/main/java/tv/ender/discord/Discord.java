package tv.ender.discord;

import com.google.firebase.database.annotations.NotNull;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tv.ender.common.Promise;
import tv.ender.common.ReadWriteLock;
import tv.ender.common.Result;
import tv.ender.discord.backend.GuildInstance;
import tv.ender.discord.backend.commands.Commands;
import tv.ender.discord.backend.interfaces.Command;
import tv.ender.discord.backend.interfaces.EventListener;
import tv.ender.discord.listeners.MessageCreateListener;
import tv.ender.discord.listeners.SlashCommandListener;
import tv.ender.firebase.Firebase;
import tv.ender.firebase.backend.GuildData;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

        Promise<Long> applicationID = new Promise<>();

        DiscordClientBuilder.create(System.getenv().get("BOT_TOKEN"))
                .build()
                .login()
                .flatMap(gateway -> {
                    System.out.println("Discord Bot is now running!");

                    this.client = gateway;

                    this.registerListener(new MessageCreateListener());
                    this.registerListener(new SlashCommandListener());

                    applicationID.set(gateway.getRestClient().getApplicationId().block());

                    return Mono.just(gateway);
                })
                .flatMap(gateway -> Mono.just(gateway.getGuilds()))
                .doOnNext(guilds -> guilds.doOnNext(guild -> {
                    final var doc = Firebase.get().getGuild(guild.getId().asString()).join();
                    GuildInstance instance = null;

                    if (!doc.exists()) {
                        final var dat = GuildData.of(guild);
                        instance = this.registerGuildInstance(dat).join().getHolder();
                    } else {
                        final var dat = GuildData.fromDocument(doc);
                        /* update member count */
                        dat.setMembers(guild.getMemberCount());
                        instance = this.registerGuildInstance(dat).join().getHolder();
                    }

                    /* reference guild entity */
                    if (instance != null) {
                        instance.setGuild(guild);
                    }

                    /* register commands */
                    this.registerGuildCommands(applicationID.get(), guild.getId().asLong()).subscribe();
                }).blockLast())
                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                .onErrorResume(throwable -> {
                    LOG.error("Failed to connect to Discord API!", throwable);
                    return Mono.empty();
                })
                .block();

        System.out.println("Registered Guilds: " + this.guildInstances.size());

        /* keep thread alive */
        this.client.onDisconnect().block();

        System.out.println("Shutting down Discord Bot...");
    }

    private Flux<ApplicationCommandData> registerGuildCommands(long applicationID, long guildId) {
        List<ApplicationCommandRequest> commandRequests = new ArrayList<>();

        for (Command cmd : Commands.values()) {
            commandRequests.add(this.constructRequest(cmd));
        }

        return this.client.getRestClient().getApplicationService().bulkOverwriteGuildApplicationCommand(
                applicationID,
                guildId,
                commandRequests
        );
    }

    public void saveGuilds() {
        /* timestamp and say its saving */
        System.out.println("Saving guilds to firebase...");
        this.lock.read(() -> {
            for (var instance : this.guildInstances.values()) {
                Firebase.get().writeGuild(instance.getGuildData()).join();
            }
        });
    }

    @NotNull
    private ApplicationCommandRequest constructRequest(@NotNull Command command) {
        var builder = ApplicationCommandRequest.builder();

        builder.name(command.cmd());
        builder.description(command.description());

        for (var option : command.options()) {
            builder.addOption(option);
        }

        return builder.build();
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

        future.completeAsync(() -> {
            if (this.lock.read(() -> !this.guildInstances.containsKey(guildId))) {
                return Result.fail("No Guild instance registered for " + guildId);
            } else {
                return Result.pass(this.guildInstances.get(guildId), "Guild instance is available");
            }
        });

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

        future.completeAsync(() -> {
            if (this.lock.read(() -> this.guildInstances.containsKey(guildData.getGuildId()))) {
                return Result.fail("Guild instance already registered for guild " + guildData.getGuildId());
            } else {
                return this.lock.write(() -> {
                    final var inst = GuildInstance.of(guildData);

                    this.guildInstances.put(guildData.getGuildId(), inst);

                    return Result.pass(inst, "Guild instance registered");
                });
            }
        });

        return future;
    }

    /**
     * Checks if a guild is registered
     *
     * @param guildId The guild ID to check
     * @return A completable future that will complete with the result of the operation
     */
    public CompletableFuture<Boolean> isGuildRegistered(String guildId) {
        final var future = new CompletableFuture<Boolean>();

        future.completeAsync(() -> {
            return this.lock.read(() -> this.guildInstances.containsKey(guildId));
        });

        return future;
    }

    public CompletableFuture<Collection<GuildInstance>> getGuildInstances() {
        final var future = new CompletableFuture<Collection<GuildInstance>>();

        future.completeAsync(() -> {
            return this.lock.read(this.guildInstances::values);
        });

        return future;
    }

    public static Discord get() {
        if (Discord.instance == null) {
            Discord.instance = new Discord();
        }
        return Discord.instance;
    }
}
