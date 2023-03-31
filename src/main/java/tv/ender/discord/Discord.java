package tv.ender.discord;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import tv.ender.common.ReadWriteLock;
import tv.ender.common.Result;
import tv.ender.discord.backend.BotInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Discord {
    private static Discord instance;
    private final Map<String, BotInstance> botInstances = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReadWriteLock();
    private final DiscordClient client;

    private Discord() {
        System.out.println("Initializing Discord Bot...");

        this.client = DiscordClient.create(System.getProperty("BOT_TOKEN"));

        System.out.println("Discord initialized!");
    }

    public void connect() {
        System.out.println("Connecting Discord Bot...");

        GatewayDiscordClient gateway = this.client.login().block();

        if (gateway == null) {
            System.out.println("Failed to connect to Discord!");
            return;
        }

        gateway.on(MessageCreateEvent.class).subscribe(event -> {
            Message message = event.getMessage();
            if ("!ping".equals(message.getContent())) {
                MessageChannel channel = message.getChannel().block();
                channel.createMessage("Pong!").block();
            } else if ("!die".equals(message.getContent())) {
                gateway.logout().block();
            }
        });

        gateway.onDisconnect().block();
    }

    public Result<BotInstance> getBotInstance(String guildId) {
        try {
            this.lock.readLock();

            if (!this.botInstances.containsKey(guildId)) {
                return Result.fail("No bot instance registered for guild " + guildId);
            } else {
                return Result.pass(this.botInstances.get(guildId), "Bot instance is available");
            }
        } finally {
            this.lock.readUnlock();
        }
    }

    public static Discord get() {
        if (Discord.instance == null) {
            Discord.instance = new Discord();
        }
        return Discord.instance;
    }
}
