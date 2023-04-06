package tv.ender.discord.listeners;

import discord4j.core.event.domain.guild.MemberJoinEvent;
import reactor.core.publisher.Mono;
import tv.ender.discord.backend.events.MemberListener;
import tv.ender.discord.backend.interfaces.EventListener;

public class MemberJoinListener extends MemberListener implements EventListener<MemberJoinEvent> {
    @Override
    public Class<MemberJoinEvent> getEventType() {
        return MemberJoinEvent.class;
    }

    @Override
    public Mono<Void> execute(MemberJoinEvent event) {
        return this.onJoin(event.getMember());
    }
}
