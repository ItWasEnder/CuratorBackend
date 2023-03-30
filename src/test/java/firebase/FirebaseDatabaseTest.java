package firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import tv.ender.App;
import tv.ender.firebase.Firebase;
import tv.ender.firebase.backend.UserData;

import static org.junit.Assert.*;

public class FirebaseDatabaseTest {
    private static final UserData data = UserData.of("ItWasEnder", "125681531824898049", 200, 0);

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    @Before
    public void setup() {
        App.loadEnv();
        Firebase.get();
    }

    @Test
    public void testConnection() {
        assertNotNull(FirebaseApp.getInstance());
    }

    @Test
    public void writeData() {
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
    public void deleteData() {
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
