package tv.ender.discord.backend.interfaces;

import discord4j.core.event.domain.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public interface EventListener<T extends Event> {
    Logger LOG = LoggerFactory.getLogger(EventListener.class);

    Class<T> getEventType();

    Mono<Void> execute(T event);

    default Mono<Void> handleError(Throwable error) {
        // TODO: Fix LOG4J to print error & info
        System.out.println("Unable to process " + this.getEventType().getSimpleName());
        error.printStackTrace();

        LOG.error("Unable to process " + this.getEventType().getSimpleName(), error);
        return Mono.empty();
    }
}