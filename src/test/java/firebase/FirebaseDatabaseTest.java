package firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tv.ender.firebase.Firebase;
import tv.ender.firebase.backend.UserData;

import static org.junit.jupiter.api.Assertions.*;

class FirebaseDatabaseTest {
    private static final UserData data = UserData.of("ItWasEnder", "125681531824898049", "1090379681330630748", 200, 0);

    @BeforeAll
    static void setup() {
//        App.loadEnv();
        Firebase.get();
    }

    @Test
    void testConnection() {
        assertNotNull(FirebaseApp.getInstance());
    }

    @Test
    void writeData() {
        var db = FirestoreClient.getFirestore();

        try {
            System.out.println("Writing data to datastore...");
            ApiFuture<WriteResult> future = db.collection(Firebase.USERS).document(data.getDiscordId())
                    .set(data);
            future.get();
            System.out.println("Data written...");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void deleteData() {
        var db = FirestoreClient.getFirestore();

        try {
            System.out.println("Retrieving data from datastore...");
            ApiFuture<DocumentSnapshot> future = db.collection(Firebase.USERS).document(data.getDiscordId()).get();
            var user = UserData.fromDocument(future.get());
            System.out.println("Data retrieved: " + user);

            db.collection("users").document(user.getDiscordId()).delete().get();

            System.out.println("Deleting data from datastore...");
            assertFalse(db.collection(Firebase.USERS).document(data.getDiscordId()).get().get().exists());
            System.out.println("Data deleted...");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
