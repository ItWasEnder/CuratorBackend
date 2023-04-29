package tv.ender.firebase.backend;

import com.google.cloud.firestore.DocumentSnapshot;
import discord4j.core.object.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data()
@Accessors(chain = true)
@Builder
public class UserData {
    private String name;
    private String discordId;
    private String guildId;
    private int tokens;
    private int losses;
    private transient Member member;

    /**
     * Creates a UserData object from a Firestore document snapshot
     *
     * @param snapshot The document snapshot
     * @return The UserData object
     * @throws IllegalArgumentException If the document snapshot is invalid
     */
    public static UserData fromDocument(DocumentSnapshot snapshot) {
        Map<String, Object> data = snapshot.getData();

        try {
            String name = (String) data.get("name");
            String discordId = (String) data.get("discordId");
            String guildId = (String) data.get("guildId");
            int tokens = ((Long) data.get("tokens")).intValue();
            int losses = ((Long) data.get("losses")).intValue();

            var builder = UserData.builder()
                    .name(name)
                    .discordId(discordId)
                    .guildId(guildId)
                    .tokens(tokens)
                    .losses(losses);

            return builder.build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse user data from document snapshot", e);
        }
    }

    public static UserData of(Member user) {
        return UserData.builder()
                .name(user.getUsername())
                .discordId(user.getId().asString())
                .guildId(user.getGuildId().asString())
                .build();
    }

    public static UserData of(String name, String discordId, String guildId, int tokens, int losses) {
        return UserData.builder()
                .name(name)
                .discordId(discordId)
                .guildId(guildId)
                .tokens(tokens)
                .losses(losses)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        UserData userData = (UserData) o;
        return this.discordId.equals(userData.discordId);
    }

    @Override
    public int hashCode() {
        return this.discordId.hashCode();
    }
}
