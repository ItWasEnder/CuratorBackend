package tv.ender.firebase.backend;

import com.google.cloud.firestore.DocumentSnapshot;
import discord4j.core.object.entity.Guild;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@Builder
public class GuildData {
    private String guildId;
    private String guildName;
    private String botPrefix;
    private String botStatus;
    private String activityType;
    private List<String> adminRoles;
    @Setter(AccessLevel.NONE)
    private boolean banned;
    private int startingTickets;
    private int members;

    public static GuildData of(Guild guild) {
        var builder = GuildData.builder()
                .guildId(guild.getId().asString())
                .guildName(guild.getName())
                .botPrefix("!")
                .botStatus("online")
                .activityType("playing")
                .adminRoles(new ArrayList<>())
                .banned(false)
                .members(guild.getMemberCount())
                .startingTickets(100);

        return builder.build();
    }

    /**
     * Bans the guild from using the bot
     */
    public void ban() {
        this.banned = true;
    }

    /**
     * Creates a GuildData object from a Firestore document snapshot
     *
     * @param snapshot The document snapshot
     * @return The GuildData object
     * @throws IllegalArgumentException If the document snapshot is invalid
     */
    public static GuildData fromDocument(DocumentSnapshot snapshot) {
        Map<String, Object> data = snapshot.getData();

        try {
            String guildId = (String) data.get("guildId");
            String guildName = (String) data.get("guildName");
            String botPrefix = (String) data.get("botPrefix");
            String botStatus = (String) data.get("botStatus");
            String activityType = (String) data.get("activityType");
            ArrayList<String> arrRoles = (ArrayList<String>) data.get("adminRoles");
            boolean banned = (Boolean) data.get("banned");
            int startingTickets = ((Long) data.get("startingTickets")).intValue();

            var builder = GuildData.builder()
                    .guildId(guildId)
                    .guildName(guildName)
                    .botPrefix(botPrefix)
                    .botStatus(botStatus)
                    .activityType(activityType)
                    .adminRoles(new ArrayList<>(arrRoles))
                    .banned(banned)
                    .startingTickets(startingTickets);

            return builder.build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse user data from document snapshot", e);
        }
    }
}
