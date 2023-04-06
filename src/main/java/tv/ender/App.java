package tv.ender;

import botrino.api.Botrino;
import tv.ender.firebase.Firebase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        try {
            System.out.println(new String(Files.readAllBytes(Paths.get("src/main/resources/banner.txt"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /* init firebase */
        Firebase.get();

        /* startup discord bot */
//        Discord.get().connect();
        Botrino.run(args);
    }
}
