package gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;

//This is the main window you see when the app opens.
//First you pick a button. Then you fill out a form.
public class MainFrame extends JFrame implements SaveListener {
    //Remembers this window so a form can say "save worked" or "save failed".
    private static MainFrame active;

    //We keep two views in here and flip between them: the buttons, or the form.
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    //Empty space in the middle where we drop in the owner or client form.
    private final JPanel formHolder = new JPanel(new BorderLayout());
    private final JPanel backBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final JLabel stepLabel = new JLabel("Step 1 of 2");
    private final JLabel promptLabel = new JLabel("Pick one option to start.");
    //Remembers who you are right now either owner, client, or nobody yet.
    private String role = "";
    //Stores the username for this session
     private String currentUser = "";
    //Stores the email for this session
      private String currentEmail = "";
    //Text field where user types their name
       private final JTextField usernameField = new JTextField(20);
    //Text field where user types their email
    private final JTextField emailField = new JTextField(20);

    public MainFrame() {
        super("Vehicular Cloud Console");
        active = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Top of the window that shows which step you're on, plus a short tip.
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 8));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        header.add(stepLabel);
        header.add(promptLabel);

        //Welcome screen with project info and buttons at bottom
        JPanel welcomeScreen = new JPanel(new BorderLayout(0, 16));
           welcomeScreen.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        
        //Content area with welcome text
           JPanel welcomeContent = new JPanel();
           welcomeContent.setLayout(new BoxLayout(welcomeContent, BoxLayout.Y_AXIS));
        
        JLabel title = new JLabel("CRTS: Vehicular Cloud Real Time System");
           Font defaultFont = title.getFont();
           title.setFont(defaultFont.deriveFont(Font.BOLD, defaultFont.getSize() + 4));
        title.setAlignmentX(CENTER_ALIGNMENT);
             welcomeContent.add(title);
        
        welcomeContent.add(Box.createRigidArea(new Dimension(0, 16)));
        
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
            "Enter your name and email below to get started."
        );
        instructions.setEditable(false);
        instructions.setWrapStyleWord(true);
        instructions.setLineWrap(true);
        instructions.setOpaque(false);
        instructions.setFocusable(false);
        instructions.setFont(defaultFont);
        welcomeContent.add(instructions);
        
        welcomeScreen.add(welcomeContent, BorderLayout.CENTER);
        
        //Username and email section sits between instructions and buttons
        JPanel userSection = new JPanel(new GridLayout(2, 2, 8, 8));
        userSection.add(new JLabel("Name:"));
        userSection.add(usernameField);
        userSection.add(new JLabel("Email:"));
        userSection.add(emailField);
        
        //The two big choices at the bottom of welcome screen
        JButton rentButton = new JButton("Rent out a car");
        JButton jobButton = new JButton("Submit a job");
        Dimension choiceSize = new Dimension(220, 40);
        rentButton.setPreferredSize(choiceSize);
        jobButton.setPreferredSize(choiceSize);

        JPanel choices = new JPanel(new GridLayout(2, 1, 0, 12));
        choices.add(rentButton);
        choices.add(jobButton);
        
        //Put username and buttons together at the bottom
        JPanel bottomSection = new JPanel(new BorderLayout(0, 16));
        bottomSection.add(userSection, BorderLayout.NORTH);
        bottomSection.add(choices, BorderLayout.CENTER);
        
        welcomeScreen.add(bottomSection, BorderLayout.SOUTH);

        formHolder.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        //Put both views into the flipper. Welcome first, form second.
        cardPanel.add(welcomeScreen, "choose");
        cardPanel.add(formHolder, "form");

        //Back button sits at the bottom of the window.
        backBar.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));
        JButton backButton = new JButton("Back");
        backBar.add(backButton);

        setLayout(new BorderLayout(0, 8));
        add(header, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        add(backBar, BorderLayout.SOUTH);

        //Clicking this takes you to the owner form.
        rentButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (checkUsername()) {
                    showForm("owner");
                }
            }
        });
        //Clicking this takes you to the client form.
        jobButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (checkUsername()) {
                    showForm("client");
                }
            }
        });
        //Clicking Back goes back to the first screen.
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                showChoice();
            }
        });

        //Set a roomy size once, then show the first screen in the middle of the monitor.
        setPreferredSize(new Dimension(600, 600));
        pack();
        showChoice();
        setLocationRelativeTo(null);
    }

    //Forms call this to say the save worked.
    public static void reportSaved() {
        if (active != null) {
            active.saved();
        }
    }

    //Forms call this to say the save failed.
    public static void reportFailed(String message) {
        if (active != null) {
            active.failed(message);
        }
    }

    //Pop up "Saved.", then clear the form so someone else can fill it out.
    public void saved() {
        JOptionPane.showMessageDialog(this, "Saved.");
        if (!role.isEmpty()) {
            showForm(role);
        }
    }

    //Pop up an error. If we got no message, use a simple default one.
    public void failed(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "Check the form and try again.";
        }
        JOptionPane.showMessageDialog(this, message, "Could not save", JOptionPane.ERROR_MESSAGE);
    }

    //Make sure user typed name and email before continuing
    private boolean checkUsername() {
        String name = usernameField.getText().trim();
        String email = emailField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter your name to continue.");
            return false;
        }
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter your email to continue.");
            return false;
        }
        currentUser = name;
        currentEmail = email;
        return true;
    }

    //Gets the current username for saving with records
    public static String getCurrentUser() {
        return active != null ? active.currentUser : "";
    }

    //Gets the current email for saving with records
    public static String getCurrentEmail() {
        return active != null ? active.currentEmail : "";
    }

    //Show the first screen again and hide Back.
    private void showChoice() {
        role = "";
        currentUser = "";
        currentEmail = "";
        usernameField.setText("");
        emailField.setText("");
        stepLabel.setText("Step 1 of 2");
        promptLabel.setText("Pick one option to start.");
        backBar.setVisible(false);
        cards.show(cardPanel, "choose");
        revalidate();
        repaint();
    }

    //Show the second screen. Grab the right form and put it in the middle.
    private void showForm(String nextRole) {
        role = nextRole;
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
}
