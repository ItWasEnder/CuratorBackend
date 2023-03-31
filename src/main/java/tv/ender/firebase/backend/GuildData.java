package tv.ender.firebase.backend;

import com.google.cloud.firestore.DocumentSnapshot;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@AllArgsConstructor(staticName = "of")
public class GuildData {
    private String guildId;
    private String guildName;
    private String botPrefix;
    private String botStatus;
    private String activityType;
    private List<String> adminRoles;
    @Setter(AccessLevel.NONE)
    private boolean banned;

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
            String[] arrRoles = (String[]) data.get("adminRoles");
            boolean banned = Boolean.parseBoolean((String) data.get("banned"));

            return GuildData.of(guildId, guildName, botPrefix, botStatus, activityType, new ArrayList<>(List.of(arrRoles)), banned);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse user data from document snapshot", e);
        }
    }
}
