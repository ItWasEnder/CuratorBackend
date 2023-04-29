package tv.ender.discord.backend.commands.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import tv.ender.common.Promise;
import tv.ender.discord.backend.GuildInstance;
import tv.ender.discord.backend.activities.Prediction;
import tv.ender.discord.backend.interfaces.Command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RaffleCommand implements Command {
    final String globalOdds = """
            Everyone: +100 Tickets
            Bad Luck Protection*: +5 Tickets
                        
            *Per consecutive loss, resets when you win.
            """;

    final String roleOdds = """
            Server Booster: +25 Tickets
            Twitch Subscriber: Tier 1: +50 Tickets
            KNOWER (Discord Sub Tier 1): +50 Tickets
            SUPER KNOWER (Discord Sub Tier 2): +100 Tickets
            THE ONES WHO KNOW (Discord Sub Tier 3): +150 Tickets
            Gifted T1: +50 Tickets
            Gifted T2: +100 Tickets
            Gifted T3: +150 Tickets
            Tier 2 Good Morning: +100 Tickets
            Twitch Subscriber: Tier 2: +100 Tickets
            Twitch Subscriber: Tier 3: +150 Tickets
            """;

    @Override
    public String cmd() {
        return "raffle";
    }

    @Override
    public String description() {
        return "Manage raffles.";
    }

    @Override
    public Collection<ApplicationCommandOptionData> options() {
        final List<ApplicationCommandOptionData> options = new ArrayList<>();

        options.add(this.option("command",
                "Command to run.",
                ApplicationCommandOption.Type.STRING.getValue(),
                true
        ).addChoice(this.choice("start")
        ).addChoice(this.choice("end")
        ).addChoice(this.choice("reset")
        ).build());

        options.add(this.option("description",
                "Prediction description.",
                ApplicationCommandOption.Type.STRING.getValue(),
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
                    Promise<String> cmd = Promise.of("nil");
                    Promise<String> description = Promise.of("nil");

                    command.getOption("command")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .ifPresent(value -> cmd.set(value.asString()));

                    command.getOption("description")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .ifPresent(value -> description.set(value.asString()));

                    if ("start" .equalsIgnoreCase(cmd.get())) {
                        var activity = new Prediction();
                        instance.addActivity(activity).join();

                        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                .color(Color.of(38, 38, 38))
                                .title(description.get())
                                .description("Raffle time! Click below to enter. The winner(s) will be chosen at random.")
                                .addField("Raffle End", "<t:1682735100:R>", true)
                                .addField("Entries", "0", true)
                                .addField("Total Tickets", "0", true)
                                .addField("Global Odds", this.globalOdds, false)
                                .addField("Role Odds", this.roleOdds, true)
                                .build();

                        List<Button> actions = List.of(
                                Button.primary("enter", "Enter Raffle"),
                                Button.secondary("end", "End Raffle"),
                                Button.secondary("redo", "Redo Raffle")
                        );

                        /* send and map raffle message */
                        instance.getGuild().getChannelById(Snowflake.of(instance.getGuildData().getActivityChannel()))
                                .filter(GuildMessageChannel.class::isInstance)
                                .flatMap(channel -> Mono.just(channel).cast(GuildMessageChannel.class))
                                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                                        .addEmbed(embed)
                                        .addAllComponents(List.of(ActionRow.of(actions)))
                                        .build()))
                                .doOnNext(message -> {
                                    /* map activity to message */
                                    instance.mapActivityMessage(message.getId(), activity);
                                })
                                .subscribe();

                        event.getInteractionResponse()
                                .createFollowupMessageEphemeral("Raffle started!")
                                .subscribe();
                    }
                }).then();
    }
}
