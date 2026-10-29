package model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//Simple user for the temporary login system
public class User {

    //The password is stored as a hash, not the original text.
    private String name;
    private String email;
    private String passwordHash;

    //Constructor receives the hash that UserStorage already created
    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    //Getters
    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    //Turn a password into a hash. The same password always gives the same hash.
    public static String hashPassword(String password) {
        String text = password == null ? "" : password;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : bytes) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required.");
        }
    }

    //Check the typed password against the stored hash
    public boolean checkPassword(String inputPassword) {
        return passwordHash != null && passwordHash.equals(hashPassword(inputPassword));
    }

    //One saved line: email|name|password hash
    public String toFileLine() {
        String safeName = name == null ? "" : name.replace("|", " ");
        return email + "|" + safeName + "|" + passwordHash;
    }

    @Override
    public String toString() {
        return "User: " + name + " (" + email + ")";
    }
}
