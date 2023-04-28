package tv.ender.discord.backend.commands.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ComponentData;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import tv.ender.common.Promise;
import tv.ender.discord.backend.GuildInstance;
import tv.ender.discord.backend.activities.Prediction;
import tv.ender.discord.backend.interfaces.Command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

        options.add(this.option("option1",
                "First prediction option.",
                ApplicationCommandOption.Type.STRING.getValue(),
                false
        ).build());

        options.add(this.option("option2",
                "Second prediction option.",
                ApplicationCommandOption.Type.STRING.getValue(),
                false
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
                    Promise<String> option1 = Promise.of("nil");
                    Promise<String> option2 = Promise.of("nil");

                    command.getOption("command")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .ifPresent(value -> cmd.set(value.asString()));

                    command.getOption("option1")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .ifPresent(value -> option1.set(value.asString()));

                    command.getOption("option2")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .ifPresent(value -> option2.set(value.asString()));

                    if ("start" .equalsIgnoreCase(cmd.get())) {
                        var activity = new Prediction();
                        instance.addActivity(activity).join();

                        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                .color(Color.BLUE)
                                .title("Title")
                                .url("https://discord4j.com")
                                .author("Some Name", "https://discord4j.com", "https://i.imgur.com/F9BhEoz.png")
                                .description("a description")
                                .thumbnail("https://i.imgur.com/F9BhEoz.png")
                                .addField("field title", "value", false)
                                .addField("\u200B", "\u200B", false)
                                .addField("inline field", "value", true)
                                .addField("inline field", "value", true)
                                .addField("inline field", "value", true)
                                .image("https://i.imgur.com/F9BhEoz.png")
                                .timestamp(Instant.now())
                                .footer("footer", "https://i.imgur.com/F9BhEoz.png")
                                .build();

                        System.out.println("built embed");

                        List<Button> actions = List.of(
                                Button.secondary("option1", option1.get()),
                                Button.secondary("option2", option2.get())
                        );

                        /* send and map poll message */
                        instance.getGuild().getChannelById(Snowflake.of(instance.getGuildData().getActivityChannel()))
                                .filter(channel -> channel instanceof GuildMessageChannel)
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
                                .createFollowupMessageEphemeral("Prediction started with options: " + option1.get() + " and " + option2.get() + ".")
                                .subscribe();
                    }
                }).then();
    }
}
