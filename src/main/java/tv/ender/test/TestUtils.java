package tv.ender.test;

import tv.ender.firebase.backend.UserData;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class TestUtils {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String NUMBERS = "0123456789";
    private static final Random rand = new Random();

    public static boolean uniqueItems(Collection<Object> items) {
        Set<Object> set = new HashSet<>(items);
        return set.size() == items.size();
    }

    public static boolean uniqueItems(Object[] items) {
        Set<Object> set = new HashSet<>(List.of(items));
        return set.size() == items.length;
    }

    public static boolean uniqueItems(UserData[] users) {
        Set<String> names = new HashSet<>();
        Set<String> ids = new HashSet<>();

        for (UserData user : users) {
            if (user == null) {
                continue;
            }

            names.add(user.getName());
            ids.add(user.getDiscordId());
        }

        return names.size() == users.length && ids.size() == users.length;
    }

    public static Collection<UserData> createUsers(int amount) {
        Set<UserData> datas = new HashSet<>();
        /* populate user map */
        for (int i = 0; i < amount; i++) {
            datas.add(createUserData());
        }

        return datas;
    }

    public static UserData createUserData() {
        return UserData.of(randomString(16), randomId(8), randomId(18), rand.nextInt(1, 1024), 0);
    }

    public static String randomString(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be positive");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(rand.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public static String randomId(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be positive");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(NUMBERS.charAt(rand.nextInt(NUMBERS.length())));
        }
        return sb.toString();
    }

    public static String toString(Object[] objects) {
        return Arrays.stream(objects).map(Object::toString).collect(Collectors.joining(", "));
    }

    public static String toString(UserData[] users) {
        return Arrays.stream(users).map(UserData::getName).collect(Collectors.joining(", "));
    }

    public TestUtils() {
        throw new IllegalStateException("Utility class");
    }
}
