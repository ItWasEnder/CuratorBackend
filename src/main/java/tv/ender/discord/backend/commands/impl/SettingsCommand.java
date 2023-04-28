package tv.ender.discord.backend.commands.impl;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;
import tv.ender.discord.backend.GuildInstance;
import tv.ender.discord.backend.interfaces.Command;

public class SettingsCommand implements Command {
    @Override
    public String cmd() {
        return "settings";
    }

    @Override
    public String description() {
        return "Configure this bot instance";
    }

    @Override
    public Mono<Void> handle(GuildInstance instance, User user, ApplicationCommandInteractionEvent event) {
        return event.reply("Hi!").withEphemeral(true);
    }
}
