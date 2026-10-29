package gui;

import model.User;
import storage.UserStorage;

import java.awt.BorderLayout;
import java.awt.CardLayout;
 import java.awt.Color;
  import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
   import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;

//Main window with login and app screens
public class MainFrame extends JFrame implements SaveListener {
    
    //Single active frame
        private static MainFrame active;
    
    //Card layout for switching screens
        private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    
    //Form holder for owner/client forms
    private final JPanel formHolder = new JPanel(new BorderLayout());
        private final JPanel backBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private JPanel header; 

    //Labels at top
    private final JLabel stepLabel = new JLabel("Welcome");
         private final JLabel promptLabel = new JLabel("Please sign in to continue.");
    
    //Current session info
    private String role = "";
      private String currentUser = "";
      private String currentEmail = "";
    
    //Login fields
    private final JTextField nameField = new JTextField();
       private final JTextField emailField = new JTextField();
       private final JPasswordField passwordField = new JPasswordField();
      private final JPasswordField confirmPasswordField = new JPasswordField();
    
    //Login mode tracker
    private boolean isSignUpMode = false;
    
    //User greeting label
    private JLabel userGreeting;
    
    public MainFrame() {
        super("Vehicular Cloud Console");
            active = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //Header at top
        header = new JPanel(new GridLayout(2, 1, 0, 8));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
           header.add(stepLabel);
          header.add(promptLabel);
        
        //Create login screen
        JPanel loginScreen = createLoginScreen();
        
        //Create welcome screen (after login)
        JPanel welcomeScreen = createWelcomeScreen();
        
        //Add all screens to card panel
        cardPanel.add(loginScreen, "login");
            cardPanel.add(welcomeScreen, "welcome");
        cardPanel.add(formHolder, "form");
        
        //Back button
        backBar.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));
           JButton backButton = new JButton("Back");
        backBar.add(backButton);
        
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                showWelcome();
            }
             });
        
        setLayout(new BorderLayout(0, 8));
           add(header, BorderLayout.NORTH);
           add(cardPanel, BorderLayout.CENTER);
        add(backBar, BorderLayout.SOUTH);
        
        //Start with login screen
        setPreferredSize(new Dimension(720, 700));
            pack();
          showLogin();
        setLocationRelativeTo(null);
    }
    
    //Create login screen
    private JPanel createLoginScreen() {
        //Main background panel
        JPanel screen = new JPanel(new GridBagLayout());
           screen.setBackground(new Color(243, 244, 246));
        
           GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
           gbc.gridy = 0;
        gbc.insets = new Insets(20, 20, 20, 20);
        
        //White card panel - will be rebuilt dynamically
           JPanel cardPanel = buildLoginCard();
        
        screen.add(cardPanel, gbc);
        
        return screen;
        }
    
    //Build login card based on current mode (Sign In or Sign Up)
    private JPanel buildLoginCard() {
            JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
           cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(36, 48, 36, 48)
            ));
        
        Font defaultFont = new JLabel().getFont();
        
        //Title section
        JLabel title = new JLabel("CRTS: Vehicular Cloud");
           title.setFont(defaultFont.deriveFont(Font.BOLD, 22f));
           title.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(title);
        
        cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        
        String subtitleText = isSignUpMode ? 
            "Create your account" : "Please sign in to continue";
        JLabel subtitle = new JLabel(subtitleText);
          subtitle.setFont(defaultFont.deriveFont(Font.PLAIN, 14f));
             subtitle.setForeground(new Color(107, 114, 128));
          subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(subtitle);
        
        cardPanel.add(Box.createRigidArea(new Dimension(0, 24)));
        
        //Name field, sign up
        if (isSignUpMode) {
            JLabel nameLabel = new JLabel("Name");
               nameLabel.setFont(defaultFont.deriveFont(Font.BOLD, 13f));
              nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardPanel.add(nameLabel);
            
            cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));
            
            nameField.setPreferredSize(new Dimension(340, 38));
               nameField.setMaximumSize(new Dimension(340, 38));
            nameField.setFont(defaultFont.deriveFont(14f));
               nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
               nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardPanel.add(nameField);
            
            cardPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        }
        
        //Email field (both modes)
        JLabel emailLabel = new JLabel("Email");
           emailLabel.setFont(defaultFont.deriveFont(Font.BOLD, 13f));
           emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(emailLabel);
        
        cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        
        emailField.setPreferredSize(new Dimension(340, 38));
           emailField.setMaximumSize(new Dimension(340, 38));
           emailField.setFont(defaultFont.deriveFont(14f));
        emailField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(emailField);
         
        cardPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        
        //Password field (both modes)
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(defaultFont.deriveFont(Font.BOLD, 13f));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(passwordLabel);
        
        cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        
        passwordField.setPreferredSize(new Dimension(340, 38));
        passwordField.setMaximumSize(new Dimension(340, 38));
        passwordField.setFont(defaultFont.deriveFont(14f));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(passwordField);
        
        cardPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        
        //Confirm Password field - only for Sign Up
        if (isSignUpMode) {
            JLabel confirmLabel = new JLabel("Confirm Password");
            confirmLabel.setFont(defaultFont.deriveFont(Font.BOLD, 13f));
            confirmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardPanel.add(confirmLabel);
            
            cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));
            
            confirmPasswordField.setPreferredSize(new Dimension(340, 38));
            confirmPasswordField.setMaximumSize(new Dimension(340, 38));
            confirmPasswordField.setFont(defaultFont.deriveFont(14f));
            confirmPasswordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            confirmPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardPanel.add(confirmPasswordField);
            
            cardPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        }
        
        cardPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        
        //Main action button (Sign In or Sign Up)
        String buttonText = isSignUpMode ? "Sign Up" : "Sign In";
        JButton mainButton = new JButton(buttonText);
        mainButton.setPreferredSize(new Dimension(340, 44));
        mainButton.setMaximumSize(new Dimension(340, 44));
        mainButton.setFont(defaultFont.deriveFont(Font.BOLD, 15f));
        mainButton.setForeground(Color.WHITE);
        mainButton.setBackground(new Color(59, 130, 246));
        mainButton.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        mainButton.setFocusPainted(false);
        mainButton.setOpaque(true);
        mainButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cardPanel.add(mainButton);
        
        //Action listener
        mainButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isSignUpMode) {
                    handleSignUp();
                } else {
                    handleSignIn();
                }
            }
        });
        
        cardPanel.add(Box.createRigidArea(new Dimension(0, 16)));
        
        //Toggle link (switch between Sign In and Sign Up)
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        togglePanel.setOpaque(false);
        togglePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        String linkText = isSignUpMode ? 
            "Already have an account? " : "Don't have an account? ";
        JLabel toggleText = new JLabel(linkText);
        toggleText.setFont(defaultFont.deriveFont(Font.PLAIN, 13f));
        toggleText.setForeground(new Color(107, 114, 128));
        
        String linkButtonText = isSignUpMode ? "Sign In" : "Sign Up";
        JButton toggleLink = new JButton(linkButtonText);
        toggleLink.setFont(defaultFont.deriveFont(Font.BOLD, 13f));
        toggleLink.setForeground(new Color(59, 130, 246));
        toggleLink.setBorder(null);
        toggleLink.setContentAreaFilled(false);
        toggleLink.setFocusPainted(false);
        toggleLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        toggleLink.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleLoginMode();
            }
        });
        
        togglePanel.add(toggleText);
        togglePanel.add(toggleLink);
        cardPanel.add(togglePanel);
        
        return cardPanel;
    }
    
    //Toggle between Sign In and Sign Up mode
    private void toggleLoginMode() {
        isSignUpMode = !isSignUpMode;
        clearLoginFields();
        refreshLoginScreen();
    }
    
    //Refresh the login screen with new mode
    private void refreshLoginScreen() {
        //Find the login screen panel
        Component[] components = cardPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            Component comp = components[i];
            String name = cards.toString(); //Check if it's the login card
            //We need to rebuild the login card
            if (i == 0) { //First component should be login
                JPanel loginScreen = (JPanel) comp;
                loginScreen.removeAll();
                
                //Rebuild with new mode
                JPanel screen = new JPanel(new GridBagLayout());
                screen.setBackground(new Color(243, 244, 246));
                
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.insets = new Insets(20, 20, 20, 20);
                
                JPanel newCard = buildLoginCard();
                screen.add(newCard, gbc);
                
                //Replace the old login screen
                cardPanel.remove(0);
                cardPanel.add(screen, "login", 0);
                break;
            }
        }
        
        cards.show(cardPanel, "login");
        revalidate();
        repaint();
    }
    
    //Create welcome screen (after login)
    private JPanel createWelcomeScreen() {
        JPanel screen = new JPanel(new BorderLayout(0, 20));
        screen.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        //Top panel with user info and sign out button
        JPanel topPanel = new JPanel(new BorderLayout());
        
        //User greeting
        userGreeting = new JLabel("Welcome, User!");
        Font defaultFont = userGreeting.getFont();
        userGreeting.setFont(defaultFont.deriveFont(Font.PLAIN, 14f));
        topPanel.add(userGreeting, BorderLayout.WEST);
        
        //Sign out button
        JButton signOutButton = new JButton("Sign Out");
        signOutButton.setFont(defaultFont.deriveFont(Font.PLAIN, 13f));
        signOutButton.setForeground(new Color(220, 38, 38)); //Red color
        signOutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleSignOut();
            }
        });
        topPanel.add(signOutButton, BorderLayout.EAST);
        
        screen.add(topPanel, BorderLayout.NORTH);
        
        //Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        JLabel title = new JLabel("CRTS: Vehicular Cloud Real Time System");
        title.setFont(defaultFont.deriveFont(Font.BOLD, defaultFont.getSize() + 6));
        title.setAlignmentX(CENTER_ALIGNMENT);
        content.add(title);
        
        content.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JTextArea instructions = new JTextArea();
        instructions.setText(
            "Welcome to the Vehicular Cloud Console.\n\n" +
            "This system allows vehicle owners to rent out their cars for " +
            "cloud computing tasks, and clients to submit computational jobs " +
            "that need processing power.\n\n" +
            "Vehicle owners can register their cars with arrival and departure " +
            "times, making their compute resources available to the cloud.\n\n" +
            "Clients can submit jobs with specific duration requirements and " +
            "deadlines to be processed by available vehicles.\n\n" +
            "Choose an option below to get started."
        );
        instructions.setEditable(false);
        instructions.setWrapStyleWord(true);
        instructions.setLineWrap(true);
        instructions.setOpaque(false);
        instructions.setFocusable(false);
        instructions.setFont(defaultFont.deriveFont(defaultFont.getSize() + 1));
        content.add(instructions);
        
        screen.add(content, BorderLayout.CENTER);
        
        //Action buttons centered
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        
        JButton rentButton = new JButton("Rent out a car");
        JButton jobButton = new JButton("Submit a job");
        
        Dimension choiceSize = new Dimension(280, 40);
        rentButton.setPreferredSize(choiceSize);
        rentButton.setMaximumSize(choiceSize);
        jobButton.setPreferredSize(choiceSize);
        jobButton.setMaximumSize(choiceSize);
        
        Font buttonFont = rentButton.getFont().deriveFont(Font.BOLD, 14f);
        rentButton.setFont(buttonFont);
        jobButton.setFont(buttonFont);
        
        rentButton.setAlignmentX(CENTER_ALIGNMENT);
        jobButton.setAlignmentX(CENTER_ALIGNMENT);
        
        buttonPanel.add(rentButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        buttonPanel.add(jobButton);
        
        buttonWrapper.add(buttonPanel);
        screen.add(buttonWrapper, BorderLayout.SOUTH);
        
        rentButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                showForm("owner");
            }
              });
        
        jobButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                showForm("client");
            }
             });
        
        return screen;
         }
    
    //Handle sign in
    private void handleSignIn() {
        String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
        
        //Check fields
           if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter email and password.", 
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
                 }
        
        //Try to sign in
        User user = UserStorage.getInstance().signIn(email, password);
        
        if (user != null) {
              currentUser = user.getName();
            currentEmail = user.getEmail();
               JOptionPane.showMessageDialog(this, "Welcome back, " + currentUser + "!", 
                "Sign In Successful", JOptionPane.INFORMATION_MESSAGE);
                  clearLoginFields();
            showWelcome();
        } else {
            //Failed
            JOptionPane.showMessageDialog(this, "Invalid email or password.\nPlease try again or sign up.", 
                "Sign In Failed", JOptionPane.ERROR_MESSAGE);
        }
         }
    
    //Handle sign up
    private void handleSignUp() {
        String name = nameField.getText().trim();
           String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();
        
        //Check all fields
             if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", 
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        //Check password match
        if (!password.equals(confirmPassword)) {
             JOptionPane.showMessageDialog(this, "Passwords do not match.", 
                "Password Mismatch", JOptionPane.ERROR_MESSAGE);
            return;
             }
        
        //Check password length
        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password must be at least 4 characters long.", 
                   "Weak Password", JOptionPane.WARNING_MESSAGE);
            return;
            }
        
        //Try to sign up
           boolean success = UserStorage.getInstance().signUp(name, email, password);
        
        if (success) {
            //Success
            currentUser = name;
              currentEmail = email;
            JOptionPane.showMessageDialog(this, "Account created successfully!\nWelcome, " + name + "!", 
                "Sign Up Successful", JOptionPane.INFORMATION_MESSAGE);
            clearLoginFields();
               isSignUpMode = false; 
            showWelcome();
             } else {
            //Email already exists
            JOptionPane.showMessageDialog(this, "Email already registered.\nPlease sign in instead.", 
                "Sign Up Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    //Handle sign out
    private void handleSignOut() {
        int choice = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to sign out?", 
               "Sign Out", 
            JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            //Clear session
              currentUser = "";
            currentEmail = "";
               role = "";
            
            JOptionPane.showMessageDialog(this, "You have been signed out successfully.", 
                "Signed Out", JOptionPane.INFORMATION_MESSAGE);
            
            //Reset to sign in mode
               isSignUpMode = false;
            clearLoginFields();
            showLogin();
        }
       }
    
    //Clear login fields
    private void clearLoginFields() {
        nameField.setText("");
          emailField.setText("");
          passwordField.setText("");
        confirmPasswordField.setText("");
    }
    
    //Show login screen
    private void showLogin() {
        header.setVisible(false); //Hide header on login screen
          backBar.setVisible(false);
        cards.show(cardPanel, "login");
           revalidate();
        repaint();
    }
    
    //Show welcome screen after login
    private void showWelcome() {
          role = "";
        
        //Update user greeting
             if (!currentUser.isEmpty()) {
            userGreeting.setText("Welcome, " + currentUser + "!");
        }
        
        header.setVisible(true); //Show header after login
        stepLabel.setText("Step 1 of 2");
           promptLabel.setText("Pick one option to start.");
        backBar.setVisible(false);
          cards.show(cardPanel, "welcome");
        revalidate();
        repaint();
    }
    
    //Show form
    private void showForm(String nextRole) {
        role = nextRole;
         header.setVisible(true); //Show header on form screens
        JPanel form = PanelFactory.create(nextRole);
          formHolder.removeAll();
        formHolder.add(form, BorderLayout.CENTER);
        
        stepLabel.setText("Step 2 of 2");
        promptLabel.setText(PanelFactory.promptFor(nextRole));
        
        backBar.setVisible(true);
        cards.show(cardPanel, "form");
          formHolder.revalidate();
        formHolder.repaint();
          revalidate();
        repaint();
    }
    
    //Get current user name
    public static String getCurrentUser() {
        return active != null ? active.currentUser : "";
          }
    
    //Get current user email
      public static String getCurrentEmail() {
        return active != null ? active.currentEmail : "";
         }
    
    //Forms call when save worked
    public static void reportSaved() {
        if (active != null) {
              active.saved();
        }
        }
    
    //Forms call when save failed
    public static void reportFailed(String message) {
        if (active != null) {
            active.failed(message);
        }
         }
    
    //Handle successful save
      public void saved() {
           JOptionPane.showMessageDialog(this, "Saved.");
        if (!role.isEmpty()) {
            showForm(role);
           }
    }
    
    //Handle failed save
    public void failed(String message) {
            if (message == null || message.trim().isEmpty()) {
                message = "Check the form and try again.";
            }
        JOptionPane.showMessageDialog(this, message, "Could not save", JOptionPane.ERROR_MESSAGE);
       }
}
