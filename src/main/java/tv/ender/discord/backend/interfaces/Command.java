package tv.ender.discord.backend.interfaces;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;

public interface Command {
    String cmd();
    String description();

    default ApplicationCommandOptionData[] options() {
        return new ApplicationCommandOptionData[0];
    }

    void handle(User user, ApplicationCommandInteractionEvent event);
}
