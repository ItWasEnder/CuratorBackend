package tv.ender.discord.backend.commands;

import tv.ender.discord.backend.commands.impl.PingCommand;
import tv.ender.discord.backend.commands.impl.PredictionCommand;
import tv.ender.discord.backend.commands.impl.SettingsCommand;
import tv.ender.discord.backend.interfaces.Command;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Commands {
    public static PingCommand PING = new PingCommand();
    public static PredictionCommand PREDICTION = new PredictionCommand();
    public static SettingsCommand SETTINGS = new SettingsCommand();

    private static final Map<String, Command> COMMAND_MAP = new HashMap<>();

    static {
        COMMAND_MAP.put(PING.cmd(), PING);
        COMMAND_MAP.put(PREDICTION.cmd(), PREDICTION);
        COMMAND_MAP.put(SETTINGS.cmd(), SETTINGS);
    }

    public static Command[] values() {
        return COMMAND_MAP.values().toArray(new Command[0]);
    }

    public static Optional<Command> get(String cmd) {
        if (!COMMAND_MAP.containsKey(cmd)) {
            return Optional.empty();
        }

        return Optional.of(COMMAND_MAP.get(cmd));
    }
}
