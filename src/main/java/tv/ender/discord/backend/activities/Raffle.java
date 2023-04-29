package tv.ender.discord.backend.activities;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Member;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import reactor.core.publisher.Mono;
import tv.ender.common.ReadWriteLock;
import tv.ender.common.Result;
import tv.ender.discord.backend.BonusStatus;
import tv.ender.discord.backend.GuildInstance;
import tv.ender.discord.backend.interfaces.IActivity;
import tv.ender.firebase.backend.UserData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Getter
@Accessors(chain = true)
public class Raffle implements IActivity {
    public static final int BASE_TICKETS = 100;
    public static final int LOSS_MULTIPLIER = 5;

    private final ReadWriteLock lock = new ReadWriteLock();
    private final Map<UserData, Integer> entrantsTicketsMap = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean canEnter = new AtomicBoolean(true);
    private final AtomicLong endTime = new AtomicLong(-1);
    private final UUID uuid = UUID.randomUUID();
    @Setter
    private int winnerSlots = 1;

    @Override
    public Set<UserData> getParticipants() {
        return this.lock.read(this.entrantsTicketsMap::keySet);
    }

    public int getTotalTickets() {
        return this.lock.read(() -> this.entrantsTicketsMap.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Override
    public boolean isRunning() {
        return this.running.get();
    }

    /**
     * End the raffle
     *
     * @return the winner of the raffle
     */
    public Result<UserData[]> end() {
        /* shutdown raffle */
        this.running.set(false);

        /* Pick a random winner weighting to the number of tickets they have */
        this.endTime.set(System.currentTimeMillis());

        final Set<UserData> winners = new HashSet<>();
        final int totalTickets = this.getTotalTickets();
        final Map<UserData, Double> entrantsWeightMap = new HashMap<>();
        final Random random = ThreadLocalRandom.current();

        /* calculate probabilities of entrants */
        this.lock.read(() -> {
            for (final Map.Entry<UserData, Integer> entry : this.entrantsTicketsMap.entrySet()) {
                entrantsWeightMap.put(entry.getKey(), (double) entry.getValue() / totalTickets);
            }
        });

        while (winners.size() != this.winnerSlots) {
            double randomValue = random.nextDouble();
            double cumulativeProbability = 0.0;

            for (final Map.Entry<UserData, Double> entry : entrantsWeightMap.entrySet()) {
                cumulativeProbability += entry.getValue();

                /* cannot win twice */
                if (winners.contains(entry.getKey())) {
                    continue;
                }

                /* find winner and break */
                if (randomValue <= cumulativeProbability) {
                    winners.add(entry.getKey());
                    break;
                }
            }
        }

        return Result.pass(winners.toArray(UserData[]::new), "Raffle ended successfully");
    }

    /**
     * Reset the raffle
     */
    public Result<Raffle> reset() {
        if (!this.running.get()) {
            return Result.fail(this, "Raffle is not running");
        }

        /* disable entries */
        this.running.set(false);

        this.lock.write(this.entrantsTicketsMap::clear);

        this.running.set(true);

        return Result.pass(this, "Raffle reset successfully");
    }

    /**
     * Enter a user into the raffle
     *
     * @param user The user to enter
     * @param tier The tier of the user
     */
    public Result<UserData> enter(UserData user, BonusStatus tier) {
        if (!this.running.get()) {
            return Result.fail(user, "Raffle is not running");
        }

        if (!this.canEnter.get()) {
            return Result.fail(user, "Raffle is not accepting entries");
        }

        /* calculate tickets */
        int tickets = this.calculateTickets(user, tier);

        /* deposit tickets */
        if (this.lock.read(() -> this.entrantsTicketsMap.containsKey(user))) {
            return Result.fail(user, "User already entered into the raffle");
        }

        this.lock.write(() -> this.entrantsTicketsMap.putIfAbsent(user, tickets));

        return Result.pass(user, "Entered with %d tickets into raffle".formatted(tickets));
    }

    public int calculateTickets(UserData user, BonusStatus tier) {
        return BASE_TICKETS + this.getTierBonus(tier) + (user.getLosses() * LOSS_MULTIPLIER);
    }

    private int getTierBonus(BonusStatus tier) {
        return switch (tier) {
            case BOOSTER -> 25;
            case TIER_1, TIER_1_GIFTED -> 50;
            case TIER_2, TIER_2_GIFTED -> 100;
            case TIER_3, TIER_3_GIFTED -> 150;
            default -> 0;
        };
    }

    @Override
    public void cancel() {
        this.reset();
        this.running.set(false);
    }

    @Override
    public void handleButton(Member member, GuildInstance instance, ButtonInteractionEvent event) {
        System.out.println("Handling button on raffle %s".formatted(this.uuid.toString()));
        var userData = instance.getUser(member).join();
        System.out.println("thang2");

        if ("enter".equals(event.getCustomId())) {
            System.out.println("Entered raffle");
            member.getRoles()
                    .flatMap(role -> Mono.just(role.getName()))
                    .doOnNext(name -> {
                        System.out.println("Role: " + name);
                    })
                    .blockLast();

            var result = this.enter(userData, BonusStatus.NONE);

            if (result.isSuccessful()) {
                event.getInteractionResponse()
                        .createFollowupMessageEphemeral("Entered into raffle with %d tickets".formatted(this.getEntrantsTicketsMap().get(userData)))
                        .block();
            } else {
                event.getInteractionResponse()
                        .createFollowupMessageEphemeral(result.getMessage())
                        .block();
            }
        } else if ("end".equals(event.getCustomId())) {
            System.out.println("Ended raffle");
            var winners = this.end();

            if (winners.isSuccessful()) {
                event.getInteractionResponse()
                        .createFollowupMessage("Raffle ended successfully with the winners %s".formatted(Arrays.stream(winners.getHolder())
                                .map(UserData::getMember)
                                .map(Member::getMention)
                                .collect(Collectors.joining(", "))))
                        .block();
            } else {
                event.getInteractionResponse()
                        .createFollowupMessageEphemeral(winners.getMessage())
                        .block();
            }
        }
    }
}
