package tv.ender.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.concurrent.CompletableFuture;

/**
 * TODO
 *   -
 */
public class Firebase {
    private static Firebase instance;
    private Firestore firestore;

    /* collections */
    public static final String USERS = "users";
    public static final String GUILDS = "guilds";

    private Firebase() {
        System.out.println("Initializing Firebase...");

        try {
            final var bytes = new ByteArrayInputStream(System.getenv().get("SERVICE_ACCOUNT_DATA").getBytes());

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(bytes))
                    .setConnectTimeout(10000)
                    .build();

            FirebaseApp.initializeApp(options);
            this.firestore = FirestoreClient.getFirestore();

            System.out.println("Firebase initialized!");
        } catch (Exception ex) {
            System.out.println("Firebase failed to initialize. Exiting...");
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
    public CompletableFuture<WriteResult> writeUser(UserData data) {
        final var api = this.firestore.collection(USERS).document(data.getDiscordId()).set(data);
        final var future = new CompletableFuture<WriteResult>();

        api.addListener(() -> {
            try {
                future.completeAsync(() -> {
                    try {
                        return api.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, Runnable::run);

        return future;
    }

    /**
     * Writes a guild to the database
     *
     * @param data The guild data
     * @return The write result
     */
    public CompletableFuture<WriteResult> writeGuild(GuildData data) {
        final var api = this.firestore.collection(GUILDS).document(data.getGuildId()).set(data);
        final var future = new CompletableFuture<WriteResult>();

        api.addListener(() -> {
            future.completeAsync(() -> {
                try {
                    return api.get();
                } catch (Exception e) {
                    e.printStackTrace();

                    throw new RuntimeException(e);
                }
            });
        }, Runnable::run);

        return future;
    }

    /**
     * Gets a user from the database
     *
     * @param discordId The discord ID of the user
     * @return The user as a document snapshot
     */
    public CompletableFuture<DocumentSnapshot> getUser(String discordId) {
        final var api = this.firestore.collection(USERS).document(discordId).get();
        final var future = new CompletableFuture<DocumentSnapshot>();

        api.addListener(() -> {
            future.completeAsync(() -> {
                try {
                    return api.get();
                } catch (Exception e) {
                    e.printStackTrace();

                    throw new RuntimeException(e);
                }
            });
        }, Runnable::run);

        return future;
    }

    /**
     * Gets a guild from the database
     *
     * @param guildId The guild ID
     * @return The guild as a document snapshot
     */
    public CompletableFuture<DocumentSnapshot> getGuild(String guildId) {
        final var api = this.firestore.collection(GUILDS).document(guildId).get();
        final var future = new CompletableFuture<DocumentSnapshot>();

        api.addListener(() -> {
            future.completeAsync(() -> {
                try {
                    return api.get();
                } catch (Exception e) {
                    e.printStackTrace();

                    throw new RuntimeException(e);
                }
            });
        }, Runnable::run);

        return future;
    }

    /**
     * Gets the Firebase instance
     *
     * @return The Firebase instance
     */
    public static Firebase get() {
        if (Firebase.instance == null) {
            Firebase.instance = new Firebase();
        }

        return Firebase.instance;
    }
}
