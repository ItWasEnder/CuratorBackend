package tv.ender.discord.backend;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import tv.ender.common.ReadWriteLock;
import tv.ender.discord.backend.interfaces.IActivity;
import tv.ender.firebase.backend.GuildData;
import tv.ender.firebase.backend.UserData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Accessors(chain = true)
@AllArgsConstructor(staticName = "of")
public class BotInstance {
    private final ReadWriteLock lock = new ReadWriteLock();
    private final Map<String, UserData> userDataMap = new ConcurrentHashMap<>();
    private final Map<UUID, IActivity> activities = new ConcurrentHashMap<>();
    private final GuildData guildData;
    private final Thread botThread;

}
