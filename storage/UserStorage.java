package storage;

import model.User;
import java.util.HashMap;
import java.util.Map;

//Temporary storage for users until we add database
public class UserStorage {
    
    //Single instance
    private static UserStorage instance;
    
    //Store users by email
    private Map<String, User> users;
    
    //Private constructor
    private UserStorage() {
        users = new HashMap<>();
    }
    
    //Get the single instance
    public static UserStorage getInstance() {
        if (instance == null) {
            instance = new UserStorage();
        }
        return instance;
    }
    
    //Sign up a new user
    public boolean signUp(String name, String email, String password) {
        //Check if email already exists
        if (users.containsKey(email)) {
            return false; //Email already registered
        }
        
        //Create and store new user
        User newUser = new User(name, email, password);
        users.put(email, newUser);
        return true;
    }
    
    //Sign in existing user
    public User signIn(String email, String password) {
        //Check if user exists
        User user = users.get(email);
        if (user == null) {
            return null; //User not found
        }
        
        //Check password
        if (user.checkPassword(password)) {
            return user; //Login successful
        }
        
        return null; //Wrong password
    }
    
    //Check if email is registered
    public boolean emailExists(String email) {
        return users.containsKey(email);
    }
    
    //Get total users
    public int getTotalUsers() {
        return users.size();
    }
    
    //Clear all users (for testing)
    public void clearAllUsers() {
        users.clear();
    }
}
