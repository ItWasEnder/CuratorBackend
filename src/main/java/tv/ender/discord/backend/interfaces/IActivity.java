package tv.ender.discord.backend.interfaces;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Member;
import tv.ender.discord.backend.GuildInstance;
import tv.ender.firebase.backend.UserData;

import java.util.Set;
import java.util.UUID;

public interface IActivity {
    Set<UserData> getParticipants();

    UUID getUuid();

    void cancel();

    boolean isRunning();

    void handleButton(Member member, GuildInstance instance, ButtonInteractionEvent event);
}
