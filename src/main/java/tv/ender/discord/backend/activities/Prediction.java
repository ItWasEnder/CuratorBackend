package tv.ender.discord.backend.activities;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Member;
import lombok.Getter;
import lombok.experimental.Accessors;
import tv.ender.common.ReadWriteLock;
import tv.ender.common.Result;
import tv.ender.discord.backend.GuildInstance;
import tv.ender.discord.backend.interfaces.IActivity;
import tv.ender.firebase.backend.UserData;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Accessors(chain = true)
public class Prediction implements IActivity {
    private final ReadWriteLock lock = new ReadWriteLock();
    private final Map<UserData, Integer> entrantsTokenMap = new ConcurrentHashMap<>();
    private final Map<UserData, String> entrantPick = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong endTime = new AtomicLong(-1);
    private final UUID uuid = UUID.randomUUID();
    private final Set<String> options = new HashSet<>();

    @Override
    public Set<UserData> getParticipants() {
        return this.lock.read(this.entrantsTokenMap::keySet);
    }

    public int getTotalTickets() {
        return this.lock.read(() -> this.entrantsTokenMap.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Override
    public boolean isRunning() {
        return this.running.get();
    }

    /**
     * End the prediction
     *
     * @param winning The winning option
     * @return A result containing the winners or null if failed
     */
    public Result<Set<UserData>> end(final String winning) {
        /* shutdown raffle */
        this.running.set(false);

        /* Pick a random winner weighting to the number of tickets they have */
        this.endTime.set(System.currentTimeMillis());

        return this.lock.read(() -> {
            if (!this.entrantPick.containsValue(winning)) {
                return Result.fail("Invalid winning option \"%s\" for prediction \"%s\"".formatted(winning, this.uuid));
            }

            Set<UserData> winners = new HashSet<>();
            int winningBets = 0;
            int totalBets = this.getTotalTickets();
            for (var entry : this.entrantsTokenMap.entrySet()) {
                if (Objects.equals(this.entrantPick.get(entry.getKey()), winning)) {
                    winners.add(entry.getKey());
                    winningBets += entry.getValue();
                }
            }

            /* payout */
            for (UserData winner : winners) {
                int viewerBet = this.entrantsTokenMap.get(winner);
                int viewerPayout;

                if (winningBets != 0) {
                    viewerPayout = (int) Math.round(totalBets * (viewerBet / (double) winningBets));
                } else {
                    viewerPayout = viewerBet;
                }

                winner.setTokens(winner.getTokens() + viewerPayout);
            }

            return Result.pass(winners, "Prediction ended with %d winners and %d total bets"
                    .formatted(winners.size(), totalBets));
        });
    }

    /**
     * Reset the prediction
     */
    public Result<Prediction> reset() {
        if (!this.running.get()) {
            return Result.fail(this, "Raffle is not running");
        }

        /* disable entries */
        this.running.set(false);

        this.lock.write(() -> {
            /* refund all tickets */
            this.entrantsTokenMap.forEach((user, tickets) -> user.setTokens(user.getTokens() + tickets));
            this.entrantsTokenMap.clear();
        });

        this.running.set(true);

        return Result.pass(this, "Raffle reset successfully");
    }

    /**
     * Enter a user into the raffle
     *
     * @param user   The user to enter
     * @param option The number of tokens to enter with
     */
    public Result<UserData> enter(UserData user, int tokens, String option) {
        if (!this.running.get()) {
            return Result.fail(user, "Prediction is not running");
        }

        if (tokens > user.getTokens()) {
            return Result.fail(user, "Insufficient tokens: %d > %d".formatted(tokens, user.getTokens()));
        }

        /* bet cannot be Zero */
        if (tokens <= 0) {
            return Result.fail(user, "Bet must be greater than zero");
        }

        /* check user choice */
        if (this.lock.read(() -> this.entrantPick.containsKey(user) && !this.entrantPick.get(user).equals(option))) {
            return Result.fail(user, "Cannot chance choice after entering");
        }

        /* deposit tokens */
        this.lock.write(() -> {
            this.entrantsTokenMap.compute(user, (__, bet) -> (bet == null) ? tokens : bet + tokens);
            this.entrantPick.computeIfAbsent(user, k -> option);
            this.options.add(option);
        });

        /* subtract tokens */
        user.setTokens(user.getTokens() - tokens);

        return Result.pass(user, "Bet %d tokens on %s".formatted(tokens, option));
    }

    @Override
    public void cancel() {
        this.reset();
        this.running.set(false);
    }

    @Override
    public void handleButton(Member member, GuildInstance instance, ButtonInteractionEvent event) {
        var future = new CompletableFuture<Void>();

        future.completeAsync(() -> {
            var interaction = event.getInteraction();
            var userData = instance.getUser(member).join();

            /* check if user has already entered */

            return null;
        });
    }
}
