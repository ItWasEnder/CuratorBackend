package tv.ender;

import tv.ender.discord.Discord;
import tv.ender.firebase.Firebase;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class App {
    public static void main(String[] args) {
        try {
            System.out.println(new String(Files.readAllBytes(Paths.get("src/main/resources/banner.txt"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /* load env properties */
        loadEnv();

        /* init firebase */
        Firebase.get();

        /* startup discord bot */
        Discord.get().connect();

        System.exit(0);
    }

    public static void loadEnv() {
        /* load env properties */
        if (Files.exists(Paths.get(".env"))) {
            try (FileInputStream in = new FileInputStream(".env")) {
                Properties properties = new Properties();
                properties.putAll(System.getProperties());
                properties.load(in);

                System.setProperties(properties);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
