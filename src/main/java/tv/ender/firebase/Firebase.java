package tv.ender.firebase;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import tv.ender.firebase.backend.GuildData;
import tv.ender.firebase.backend.UserData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

public class Firebase {
    private static Firebase instance;

    /* collections */
    public static final String USERS = "users";
    public static final String GUILDS = "guilds";

    private Firebase() {
        System.out.println("Initializing Firebase...");

        File file = new File(System.getProperty("SERVICE_ACCOUNT_PATH"));

        this.loadOrCreate(file);

        try (FileInputStream serviceAccount = new FileInputStream(file)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setConnectTimeout(10000)
                    .build();

            FirebaseApp.initializeApp(options);
            FirestoreClient.getFirestore();

            System.out.println("Firebase initialized!");
        } catch (Exception ex) {
            System.out.println("Firebase failed to initialize. Exiting...");
            ex.printStackTrace();

            Runtime.getRuntime().exit(1);
        }
    }

    private void loadOrCreate(File file) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            if (!file.exists() && System.getProperty("SERVICE_ACCOUNT_DATA") != null) {
                boolean created = file.createNewFile();
                if (created) {
                    /* write json string to file */
                    try (JsonWriter writer = gson.newJsonWriter(new FileWriter(file))) {
                        JsonObject object = gson.fromJson(System.getProperty("SERVICE_ACCOUNT_DATA"), JsonObject.class);
                        gson.toJson(object, writer);

                        writer.flush();
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Failed to create service account file. Exiting...");
            ex.printStackTrace();

            Runtime.getRuntime().exit(1);
        }
    }

    /**
     * Writes a user to the database
     *
     * @param data The user data
     * @return The write result
     */
    public ApiFuture<WriteResult> writeUser(UserData data) {
        var db = FirestoreClient.getFirestore();

        return db.collection(USERS).document(data.getDiscordId()).set(data);
    }

    /**
     * Writes a guild to the database
     *
     * @param data The guild data
     * @return The write result
     */
    public ApiFuture<WriteResult> writeGuild(GuildData data) {
        var db = FirestoreClient.getFirestore();

        return db.collection(GUILDS).document(data.getGuildId()).set(data);
    }



    public static Firebase get() {
        if (Firebase.instance == null) {
            Firebase.instance = new Firebase();
        }

        return Firebase.instance;
    }
}
