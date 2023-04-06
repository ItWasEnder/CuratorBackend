package tv.ender.discord.backend.events;

import com.google.cloud.firestore.DocumentSnapshot;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;
import tv.ender.common.Result;
import tv.ender.discord.Discord;
import tv.ender.firebase.Firebase;
import tv.ender.firebase.backend.UserData;

import java.util.concurrent.CompletableFuture;

public abstract class MemberListener {
    public Mono<Void> onJoin(Member eventMember) {

        // TODO incorrect logic should be adding user to firestore if not exists not retrieving user from firestore
        /* check and upload user to firestore */
        return Mono.just(eventMember)
                .filter(member -> !member.isBot())
                .flatMap(member -> {
                    final CompletableFuture<DocumentSnapshot> future = Firebase.get().getUser(member.getId().asString());

                    return Mono.fromFuture(future)
                            .filter(documentSnapshot -> !documentSnapshot.exists())
                            .flatMap(documentSnapshot -> Mono.just(UserData.fromDocument(documentSnapshot)));
                }).doOnNext(userData -> {
                    /* obtain guild instance */
                    Mono.fromFuture(Discord.get().getGuildInstance(userData.getGuildId()))
                            .filter(Result::isSuccessful)
                            .flatMap(result -> Mono.just(result.getHolder()))
                            .doOnNext(guild -> {
                                /* add new member to guild data */
                                userData.setTokens(guild.getGuildData().getStartingTickets());
                                guild.addUser(userData);
                            });
                }).then();
    }

    public Mono<Void> onRoleChange(GatewayDiscordClient client) {
        return Mono.empty();
    }


}