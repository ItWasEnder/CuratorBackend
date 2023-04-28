package tv.ender.discord.backend.events;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;
import tv.ender.common.Result;
import tv.ender.discord.Discord;
import tv.ender.discord.backend.commands.Commands;

public abstract class ApplicationCommandListener implements DiscordEvent {

    public Mono<Void> processCommand(ApplicationCommandInteractionEvent event) {
        final User user = event.getInteraction().getUser();

        return Mono.just(event)
                .filter(e -> !user.isBot())
                .filter(e -> event.getInteraction().getGuildId().isPresent())
                .flatMap(e -> Mono.just(event.getInteraction().getGuildId().get()))
                .flatMap(guildId -> Mono.fromFuture(Discord.get().getGuildInstance(guildId.asString())))
                .filter(Result::isSuccessful)
                .flatMap(result -> Mono.just(result.getHolder()))
                .flatMap(instance -> Commands.get(event.getCommandName())
                        .orElseThrow(() -> new RuntimeException("Command not found."))
                        .handle(instance, user, event));
    }
}