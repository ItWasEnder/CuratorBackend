package tv.ender.discord.backend.commands.impl;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.User;
import tv.ender.discord.backend.interfaces.Command;

public class PingCommand implements Command {
    @Override
    public String cmd() {
        return "ping";
    }

    @Override
    public String description() {
        return "Sends a ping request to the bot.";
    }

    @Override
    public void handle(User user, ApplicationCommandInteractionEvent event) {
        event.reply("Pong!").withEphemeral(true).subscribe();
    }
}
