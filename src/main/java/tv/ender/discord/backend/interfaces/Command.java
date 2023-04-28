package tv.ender.discord.backend.interfaces;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;
import reactor.core.publisher.Mono;
import tv.ender.discord.backend.GuildInstance;

import java.util.Collection;
import java.util.List;

public interface Command {
    String cmd();
    String description();

    default Collection<ApplicationCommandOptionData> options() {
        return List.of();
    }

    Mono<Void> handle(GuildInstance instance, User user, ApplicationCommandInteractionEvent event);

    default String noPerms() {
        return "You do not have permission to use this command.";
    }

    default ApplicationCommandOptionChoiceData choice(String option) {
        return ApplicationCommandOptionChoiceData.builder()
                .name(option)
                .value(option)
                .build();
    }

    default ImmutableApplicationCommandOptionData.Builder option(String name, String description, int type, boolean required) {
        return ApplicationCommandOptionData.builder()
                .name(name)
                .description(description)
                .type(type)
                .required(required);
    }
}
