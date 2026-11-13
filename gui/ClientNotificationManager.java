package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClientNotificationManager {

    private static final Map<String, List<ClientNotification>> notifications = new HashMap<>();

    private ClientNotificationManager() {
    }
    // Store a notification for the account that submitted the job.
    public static synchronized void add(
            String email,
            String title,
            String message
    ) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        notifications.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new ClientNotification(title, message));
    }
    // Retrieve and remove notifications belonging to this account.
    public static synchronized List<ClientNotification> takeFor(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        List<ClientNotification> pending = notifications.remove(key);
        return pending == null
                ? new ArrayList<>()
                : new ArrayList<>(pending);
    }

    public static final class ClientNotification {
        private final String title;
        private final String message;

        private ClientNotification(String title, String message) {
            this.title = title;
            this.message = message;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }
    }
}