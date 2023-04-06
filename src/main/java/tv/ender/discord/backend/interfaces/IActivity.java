package tv.ender.discord.backend.interfaces;

import tv.ender.firebase.backend.UserData;

import java.util.Set;
import java.util.UUID;

public interface IActivity {
    Set<UserData> getParticipants();
    UUID getUuid();
    void cancel();
}
