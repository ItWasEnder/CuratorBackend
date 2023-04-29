package tv.ender.discord.listeners;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;
import tv.ender.discord.backend.events.ButtonListener;
import tv.ender.discord.backend.interfaces.EventListener;

public class ButtonInteractionEventListener extends ButtonListener implements EventListener<ButtonInteractionEvent> {

    @Override
    public Class<ButtonInteractionEvent> getEventType() {
        return ButtonInteractionEvent.class;
    }

    @Override
    public Mono<Void> execute(ButtonInteractionEvent event) {
        return this.processCommand(event);
    }
}