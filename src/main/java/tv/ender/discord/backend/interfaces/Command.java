package tv.ender.discord.backend.interfaces;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import reactor.core.publisher.Mono;
import tv.ender.discord.backend.GuildInstance;

public interface Command {
    String cmd();
    String description();

    default ApplicationCommandOptionData[] options() {
        return new ApplicationCommandOptionData[0];
    }

    Mono<Void> handle(GuildInstance instance, User user, ApplicationCommandInteractionEvent event);

    default String noPerms() {
        return "You do not have permission to use this command.";
    }
}
