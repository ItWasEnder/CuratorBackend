package tv.ender.discord.backend.events;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import tv.ender.common.Promise;
import tv.ender.discord.Discord;

public abstract class MessageListener {

    public Mono<Void> processCommand(Message eventMessage) {
        final Promise<String> author = new Promise<>();

        return Mono.just(eventMessage)
                .filter(message -> {
                    final Boolean isNotBot = message.getAuthor().map(user -> !user.isBot()).orElse(false);

                    if (isNotBot) {
                        message.getAuthor().ifPresent(user -> author.set(user.getUsername()));
                    }

                    return isNotBot;
                })
                .flatMap(message -> {
                    final String content = message.getContent();

                    if (!content.startsWith("!")) {
                        return Mono.empty();
                    }

                    if (content.equalsIgnoreCase("!ping")) {
                        message.getChannel().subscribe(channel -> channel.createMessage("Pong!").subscribe());
                    } else if (content.equalsIgnoreCase("!author")) {
                        message.getChannel().subscribe(channel -> channel.createMessage("Author: " + author.get()).subscribe());
                    } else if (content.equalsIgnoreCase("!help")) {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage("Commands: !ping, !author, !help"));
                    } else if (content.equalsIgnoreCase("!die")) {
                        return message.getChannel()
                                .flatMap(channel -> {
                                    channel.createMessage("Shutting down...").block();

                                    Discord.get().getClient().thenAccept(client -> client.logout().block());

                                    return Mono.empty();
                                });
                    } else if (content.equalsIgnoreCase("!error")) {
                        throw new RuntimeException("Test error");
                    }

                    return Mono.empty();
                }).then();
    }
}