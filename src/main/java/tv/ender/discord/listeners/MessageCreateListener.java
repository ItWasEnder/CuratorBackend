package tv.ender.discord.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;
import tv.ender.discord.backend.events.MessageListener;
import tv.ender.discord.backend.interfaces.EventListener;

public class MessageCreateListener extends MessageListener implements EventListener<MessageCreateEvent> {

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        return this.processCommand(event.getMessage());
    }
}