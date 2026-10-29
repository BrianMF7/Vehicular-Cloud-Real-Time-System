package model;

//Simple user for login system
public class User {
    
    //User info
    private String name;
    private String email;
    private String password;
    
    //Constructor
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
    
    //Getters
    public String getName() {
        return name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
    
    //Check if password matches
    public boolean checkPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }
    
    @Override
    public String toString() {
        return "User: " + name + " (" + email + ")";
    }
}
