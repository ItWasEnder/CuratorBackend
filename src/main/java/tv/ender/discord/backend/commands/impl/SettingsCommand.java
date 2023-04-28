package tv.ender.discord.backend.commands.impl;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import tv.ender.common.Promise;
import tv.ender.discord.backend.GuildInstance;
import tv.ender.discord.backend.interfaces.Command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SettingsCommand implements Command {
    @Override
    public String cmd() {
        return "settings";
    }

    @Override
    public String description() {
        return "Configure this bot instance.";
    }

    @Override
    public Collection<ApplicationCommandOptionData> options() {
        final List<ApplicationCommandOptionData> options = new ArrayList<>();

        options.add(this.option("setting",
                "Setting to modify.",
                ApplicationCommandOption.Type.STRING.getValue(),
                true
        ).addChoice(this.choice("set-activity-channel")
        ).build());

        options.add(this.option("channel",
                "Pick a channel.",
                ApplicationCommandOption.Type.CHANNEL.getValue(),
                true
        ).build());

        return options;
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

                    if (!hasPermissions.get()) {
                        event.getInteractionResponse()
                                .createFollowupMessageEphemeral(this.noPerms())
                                .subscribe();

                        return;
                    }

                    /* handle command actions */
                    ApplicationCommandInteraction command = event.getInteraction().getCommandInteraction().get();
                    Promise<String> mentionable = Promise.of("nil");

                    command.getOption("channel")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .ifPresent(value -> mentionable.set(value.asSnowflake().asString()));

                    event.getInteractionResponse()
                            .createFollowupMessageEphemeral("Set activity channel to <#" + mentionable.get() + ">")
                            .subscribe();

                    instance.getGuildData().setActivityChannel(mentionable.get());
                }).then();
    }
}
