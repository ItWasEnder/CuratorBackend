package tv.ender.discord.backend.interfaces;

import tv.ender.firebase.backend.UserData;

import java.util.Set;

public interface IActivity {
    Set<UserData> getParticipants();
    void cancel();
}
