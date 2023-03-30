package tv.ender;

import tv.ender.discord.Discord;
import tv.ender.firebase.Firebase;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class App {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        /* load env properties */
        try (FileInputStream in = new FileInputStream(".env")) {
            System.out.println(new String(Files.readAllBytes(Paths.get("src/main/resources/banner.txt"))));

            Properties properties = new Properties();
            properties.putAll(System.getProperties());
            properties.load(in);

            System.setProperties(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* init firebase */
        Firebase.get();

        /* startup discord bot */
        Discord.get().connect();

        System.exit(0);
    }
}
