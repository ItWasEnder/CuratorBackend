package tv.ender.discord.backend.commands.impl;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;
import tv.ender.common.Promise;
import tv.ender.discord.backend.interfaces.Command;

public class PredictionCommand implements Command {
    @Override
    public String cmd() {
        return "prediction";
    }

    @Override
    public String description() {
        return "Manage predictions.";
    }

    @Override
    public void handle(User user, ApplicationCommandInteractionEvent event) {
        event.deferReply().withEphemeral(true).then(
                Mono.just(event).flatMap(e -> {
                    var opt = event.getInteraction().getGuildId();
                    return opt.map(user::asMember).orElseGet(Mono::empty);
                }).doOnNext(member -> {
                    Promise<Boolean> hasAdmin = Promise.of(false);

                    member.getBasePermissions().doOnNext(permission -> {
                        if (permission.contains(Permission.ADMINISTRATOR)) {
                            hasAdmin.set(true);
                        }
                    }).block();

                    if (!hasAdmin.get()) {
                        event.getInteractionResponse()
                                .createFollowupMessageEphemeral("You do not have permission to use this command.")
                                .subscribe();

                        System.out.println("User " + user.getUsername() + " does not have permission to use this command.");
                    } else {
                        event.getInteractionResponse()
                                .createFollowupMessageEphemeral("You have permission to use this command.")
                                .subscribe();

                        System.out.println("User " + user.getUsername() + " has permission to use this command.");
                    }
                }).then()
        ).subscribe();
    }
}
