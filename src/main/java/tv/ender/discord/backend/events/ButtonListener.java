package tv.ender.discord.backend.events;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import tv.ender.common.Result;
import tv.ender.discord.Discord;
import tv.ender.discord.backend.interfaces.IActivity;

import java.util.ArrayList;
import java.util.List;

public abstract class ButtonListener implements DiscordEvent {

    public Mono<Void> processCommand(ButtonInteractionEvent event) {
        event.deferReply().subscribe();

        return Mono.just(event)
                .doOnNext(e -> {
                    event.deferReply().withEphemeral(true).subscribe();
                })
                .filter(e -> !event.getInteraction().getUser().isBot())
                .filter(e -> event.getInteraction().getGuildId().isPresent())
                .flatMap(e -> Mono.just(event.getInteraction().getGuildId().get()))
                .flatMap(guildId -> Mono.fromFuture(Discord.get().getGuildInstance(guildId.asString())))
                .filter(Result::isSuccessful)
                .flatMap(result -> Mono.just(result.getHolder()))
                .doOnNext(instance -> {
                    /* check if message is stored */
                    var opt = event.getInteraction().getMessage();

                    if (opt.isEmpty()) {
                        return;
                    }

                    Message message = opt.get();
                    Member member = instance.getGuild().getMemberById(event.getInteraction().getUser().getId()).block();
                    IActivity activity = instance.getActivityFromMessage(message.getId()).join().orElse(null);

                    List<ActionComponent> components = new ArrayList<>();

                    /* disable buttons */
                    if (activity == null || !activity.isRunning()) {
                        message.getComponents().forEach(layout -> {
                            layout.getChildren().forEach(component -> {
                                System.out.println(component.getData().customId().get());
                                if (component.getType() == MessageComponent.Type.BUTTON &&
                                    component.getData().customId().get().equals(event.getCustomId())) {
                                    final Button button = ((Button) MessageComponent.fromData(component.getData()));
                                    components.add(button.disabled());
                                }
                            });
                        });
                    } else {
                        /* handle button press */
                        System.out.println("Pressed: " + event.getCustomId());
                        System.out.println("Activity: " + activity.getUuid());
                        activity.handleButton(member, instance, event);
                    }

                    /* update message buttons */
                    if (components.size() > 0) {
                        message.edit().withComponents(ActionRow.of(components)).subscribe();
                    }
                }).then();
    }
}