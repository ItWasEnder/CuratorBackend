package tv.ender.discord;

public class Discord {
    private static Discord instance;

    private Discord() {
        System.out.println("Initializing Discord Bot...");



        System.out.println("Discord initialized!");
    }

    public static Discord init() {
        if (Discord.instance == null) {
            Discord.instance = new Discord();
        }
        return Discord.instance;
    }
}
