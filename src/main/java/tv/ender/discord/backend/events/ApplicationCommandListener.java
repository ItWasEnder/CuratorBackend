package tv.ender.discord.backend.events;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;
import tv.ender.discord.backend.commands.Commands;

public abstract class ApplicationCommandListener {

    public Mono<Void> processCommand(ApplicationCommandInteractionEvent event) {
        final User user = event.getInteraction().getUser();

        return Mono.just(event)
                .filter(e -> !user.isBot())
                .doOnNext(e -> {
                    Commands.get(event.getCommandName()).ifPresent(cmd -> {
                        cmd.handle(user, event);
                    });
                }).then();
    }
}