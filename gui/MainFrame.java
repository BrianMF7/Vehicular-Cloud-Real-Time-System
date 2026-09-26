package gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class MainFrame extends JFrame implements SaveListener {
    /* The open window, so a panel can report a save without this class building that panel's fields. */
    private static MainFrame active;

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final JPanel formHolder = new JPanel(new BorderLayout());
    private final JPanel backBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final JLabel stepLabel = new JLabel("Step 1 of 2");
    private final JLabel promptLabel = new JLabel("Pick one option to start.");
    private String role = "";

    public MainFrame() {
        super("Vehicular Cloud Console");
        active = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 8));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        header.add(stepLabel);
        header.add(promptLabel);

        JButton rentButton = new JButton("Rent out a car");
        JButton jobButton = new JButton("Submit a job");
        Dimension choiceSize = new Dimension(220, 40);
        rentButton.setPreferredSize(choiceSize);
        jobButton.setPreferredSize(choiceSize);

        JPanel choices = new JPanel(new GridLayout(2, 1, 0, 8));
        choices.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        choices.add(rentButton);
        choices.add(jobButton);

        formHolder.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        cardPanel.add(choices, "choose");
        cardPanel.add(formHolder, "form");

        backBar.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));
        JButton backButton = new JButton("Back");
        backBar.add(backButton);

        setLayout(new BorderLayout(0, 8));
        add(header, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        add(backBar, BorderLayout.SOUTH);

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
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                showChoice();
            }
        });

        showChoice();
        setLocationRelativeTo(null);
    }

    public static void reportSaved() {
        if (active != null) {
            active.saved();
        }
    }

    public static void reportFailed(String message) {
        if (active != null) {
            active.failed(message);
        }
    }

    public void saved() {
        JOptionPane.showMessageDialog(this, "Saved.");
        /* A new panel starts empty, which clears the form for the next entry. */
        if (!role.isEmpty()) {
            showForm(role);
        }
    }

    public void failed(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "Check the form and try again.";
        }
        JOptionPane.showMessageDialog(this, message, "Could not save", JOptionPane.ERROR_MESSAGE);
    }

    private void showChoice() {
        role = "";
        stepLabel.setText("Step 1 of 2");
        promptLabel.setText("Pick one option to start.");
        backBar.setVisible(false);
        cards.show(cardPanel, "choose");
        pack();
    }

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
        pack();
    }
}
