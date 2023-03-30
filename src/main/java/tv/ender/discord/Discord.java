package tv.ender.discord;

public class Discord {
    private static Discord instance;

    private Discord() {
        System.out.println("Initializing Discord Bot...");



        System.out.println("Discord initialized!");
    }

    public void connect() {
        System.out.println("Connecting Discord Bot...");
    }

    public static Discord get() {
        if (Discord.instance == null) {
            Discord.instance = new Discord();
        }
        return Discord.instance;
    }
}
