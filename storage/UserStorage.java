package storage;

import model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

//Temporary account file until we add a database.
//The file stores the name, email, and password hash which doesn't store the password itself.
public class UserStorage {

    //Single instance
    private static UserStorage instance;

    //Store users by lowercase email
    private Map<String, User> users;

    //users.txt sits next to the app until a database replaces it
    private Path filePath;

    private UserStorage() {
        users = new HashMap<String, User>();
        filePath = Paths.get("users.txt");
        loadUsers();
    }

    //Get the single instance
    public static UserStorage getInstance() {
        if (instance == null) {
            instance = new UserStorage();
        }
        return instance;
    }

    //Tests call this so they use a temp file instead of the real users.txt
    public void useFileForTesting(Path path) {
        filePath = path;
        users = new HashMap<String, User>();
        loadUsers();
    }

    //Sign up a new user
    public boolean signUp(String name, String email, String password) {
        String key = normalizeEmail(email);
        if (key.isEmpty() || users.containsKey(key)) {
            return false;
        }

        User newUser = new User(name, key, User.hashPassword(password));
        users.put(key, newUser);
        saveUsers();
        return true;
    }

    //Sign in existing user
    public User signIn(String email, String password) {
        User user = users.get(normalizeEmail(email));
        if (user == null) {
            return null;
        }
        if (user.checkPassword(password)) {
            return user;
        }
        return null;
    }

    //Check if email is registered
    public boolean emailExists(String email) {
        return users.containsKey(normalizeEmail(email));
    }

    //Get total users
    public int getTotalUsers() {
        return users.size();
    }

    //Clear all users (for testing)
    public void clearAllUsers() {
        users.clear();
        saveUsers();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    //Read saved accounts. A bad line is skipped so one mistake does not block login.
    private void loadUsers() {
        if (filePath == null || !Files.exists(filePath)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
                while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 3);
                   if (parts.length < 3) {
                     continue;
                      }
                String email = normalizeEmail(parts[0]);
                    String name = parts[1];
                String passwordHash = parts[2].trim();
                  if (!email.isEmpty() && !passwordHash.isEmpty()) {
                    users.put(email, new User(name, email, passwordHash));
                }
            }
        } catch (IOException ex) {
            System.err.println("Could not read saved accounts: " + ex.getMessage());
           }
               }

    //Rewrite the whole account file after a sign up or a test reset
    private void saveUsers() {
        if (filePath == null) {
            return;
        }
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
                }
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                for (User user : users.values()) {
                        writer.write(user.toFileLine());
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
              System.err.println("Could not save accounts: " + ex.getMessage());
                }
    }
}
