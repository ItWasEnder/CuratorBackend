package tv.ender.discord.listeners;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import reactor.core.publisher.Mono;
import tv.ender.discord.backend.events.ApplicationCommandListener;
import tv.ender.discord.backend.interfaces.EventListener;

public class SlashCommandListener extends ApplicationCommandListener implements EventListener<ApplicationCommandInteractionEvent> {

    @Override
    public Class<ApplicationCommandInteractionEvent> getEventType() {
        return ApplicationCommandInteractionEvent.class;
    }

    @Override
    public Mono<Void> execute(ApplicationCommandInteractionEvent event) {
        return this.processCommand(event);
    }
}