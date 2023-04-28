package tv.ender.discord.backend.commands.impl;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import tv.ender.common.Promise;
import tv.ender.discord.backend.GuildInstance;
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
    public Mono<Void> handle(GuildInstance instance, User user, ApplicationCommandInteractionEvent event) {
        return Mono.just(event)
                .doOnNext(e -> {
                    event.deferReply().withEphemeral(true).subscribe();
                })
                .flatMap(e -> event.getInteraction().getGuildId()
                        .map(user::asMember)
                        .orElseGet(Mono::empty))
                .doOnNext(member -> {
                    Promise<Boolean> hasPermissions = Promise.of(false);

                    /* check admin perm */
                    member.getBasePermissions().doOnNext(permission -> {
                        hasPermissions.set(permission.and(PermissionSet.of(Permission.ADMINISTRATOR))
                                .contains(Permission.ADMINISTRATOR));
                    }).block();

                    /* if not admin then check role ids */
                    if (!hasPermissions.get()) {
                        for (var role : member.getRoleIds()) {
                            if (instance.getGuildData().getAdminRoles().contains(role.asString())) {
                                hasPermissions.set(true);
                                break;
                            }
                        }
                    }

                    /* handle command actions */
                    if (!hasPermissions.get()) {
                        event.getInteractionResponse()
                                .createFollowupMessageEphemeral(this.noPerms())
                                .subscribe();

                        return;
                    }

                    event.getInteractionResponse()
                            .createFollowupMessageEphemeral("You have permission to use this command.")
                            .subscribe();

                    System.out.println("User " + user.getUsername() + " has permission to use this command.");
                }).then();
    }
}
