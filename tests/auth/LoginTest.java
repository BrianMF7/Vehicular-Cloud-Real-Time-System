package tests.auth;

import model.User;
import storage.UserStorage;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

//JUnit 5 test for login features
public class LoginTest {
    
    private UserStorage storage;

    @TempDir
    Path tempDir;
    
    @BeforeEach
    public void setUp() {
        //Use a temp file so tests do not erase the real users.txt
        storage = UserStorage.getInstance();
        storage.useFileForTesting(tempDir.resolve("users.txt"));
    }
    
    @Test
    public void testSignUpSuccess() {
        //Test successful sign up
        boolean result = storage.signUp("John Doe", "john@test.com", "pass123");
        assertTrue(result, "Sign up should succeed");
        
        //Verify user count increased
        assertTrue(storage.getTotalUsers() >= 1, "Should have at least 1 user");
        
        //Verify email exists
        assertTrue(storage.emailExists("john@test.com"), "Email should exist");
    }
    
    @Test
    public void testSignUpDuplicateEmail() {
        //Create first user
        storage.signUp("John Doe", "john@test.com", "pass123");
        
        //Try to create another with same email
        boolean result = storage.signUp("Jane Smith", "john@test.com", "pass456");
        assertFalse(result, "Sign up with duplicate email should fail");
    }
    
    @Test
    public void testSignInSuccess() {
        //Create a user
        storage.signUp("John Doe", "john@test.com", "pass123");
        
        //Try to sign in
        User user = storage.signIn("john@test.com", "pass123");
        assertNotNull(user, "User should not be null");
        assertEquals("John Doe", user.getName(), "Name should match");
        assertEquals("john@test.com", user.getEmail(), "Email should match");
    }
    
    @Test
    public void testSignInWrongPassword() {
        //Create a user
        storage.signUp("John Doe", "john@test.com", "pass123");
        
        //Try to sign in with wrong password
        User user = storage.signIn("john@test.com", "wrongpass");
        assertNull(user, "User should be null with wrong password");
    }
    
    @Test
    public void testSignInUserNotFound() {
        //Try to sign in with email that doesn't exist
        User user = storage.signIn("nobody@test.com", "pass");
        assertNull(user, "User should be null when not found");
    }
    
    @Test
    public void testCheckPassword() {
        //The user stores a hash, not the typed password
        User user = new User("Test User", "test@test.com", User.hashPassword("mypass"));
        
        assertTrue(user.checkPassword("mypass"), "Correct password should return true");
        assertFalse(user.checkPassword("wrongpass"), "Wrong password should return false");
    }

    @Test
    public void testEmailIgnoresCapitalLetters() {
        storage.signUp("John Doe", "John@test.com", "pass123");

        assertTrue(storage.emailExists("john@test.com"), "Email check should ignore capitals");
        assertFalse(storage.signUp("Jane", "JOHN@test.com", "other"), "Same email should not sign up twice");

        User user = storage.signIn("john@test.com", "pass123");
        assertNotNull(user, "Sign in should accept a different email casing");
        assertEquals("john@test.com", user.getEmail(), "Saved email should be lowercase");
    }

    @Test
    public void testAccountStaysAfterReload() {
        Path accountFile = tempDir.resolve("users.txt");
        storage.useFileForTesting(accountFile);
        assertTrue(storage.signUp("John Doe", "john@test.com", "pass123"));

        //Load the same file again, the way the app does on the next start
        storage.useFileForTesting(accountFile);
        User user = storage.signIn("john@test.com", "pass123");
        assertNotNull(user, "Account should still be there after reload");
        assertEquals("John Doe", user.getName());
    }
    
    @Test
    public void testEmailExists() {
        //Test email check
        storage.signUp("John Doe", "john@test.com", "pass123");
        
        assertTrue(storage.emailExists("john@test.com"), "Registered email should exist");
        assertFalse(storage.emailExists("nobody@test.com"), "Unregistered email should not exist");
    }
}
