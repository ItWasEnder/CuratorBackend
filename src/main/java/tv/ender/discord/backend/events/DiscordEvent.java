package tv.ender.discord.backend.events;

import tv.ender.discord.Discord;
import tv.ender.discord.backend.GuildInstance;

public interface DiscordEvent {
    /**
     * Get the Discord instance. This is blocking.
     *
     * @param guildId The guild ID
     * @return The guild instance
     */
    default GuildInstance getGuildInstance(String guildId) {
        return Discord.get().getGuildInstance(guildId).join().getHolder();
    }
}
