package gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ClientPanel extends JPanel {

    // Labels and text fields for client information
    private JLabel clientLabel = new JLabel("Client ID: ");
    private JTextField clientIdField = new JTextField();

    // Labels and text fields for job information
    private JLabel jobLabel = new JLabel("Job ID: ");
    private JTextField jobIdField = new JTextField();

    private JLabel jobDurationLabel = new JLabel("Job Duration: ");
    private JTextField jobDurationField = new JTextField();

    private JLabel jobDeadlineLabel = new JLabel("Job Deadline: (MM-dd-yyyy)");
    private JTextField jobDeadlineField = new JTextField();

    // Button used to submit the job
    private JButton submitButton = new JButton("Submit job: ");

    public ClientPanel() {

        // Set the main panel layout
        setLayout(new BorderLayout(10, 10));

        // Create panels for client information, job information, and the submit button
        JPanel identityPanel = new JPanel();
        JPanel jobPanel = new JPanel();
        JPanel buttonPanel = new JPanel();

        // Add client ID and job ID fields to the identity panel
        identityPanel.setLayout(new GridLayout(4, 1, 5, 5));
        identityPanel.add(clientLabel);
        identityPanel.add(clientIdField);
        identityPanel.add(jobLabel);
        identityPanel.add(jobIdField);

        // Add job duration and deadline fields to the job panel
        jobPanel.setLayout(new GridLayout(4, 1, 5, 5));
        jobPanel.add(jobDurationLabel);
        jobPanel.add(jobDurationField);
        jobPanel.add(jobDeadlineLabel);
        jobPanel.add(jobDeadlineField);

        // Add each panel to its position in the main panel
        add(identityPanel, BorderLayout.NORTH);
        add(jobPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add the submit button to the button panel
        buttonPanel.add(submitButton);

        // Handle the job submission when the button is clicked
        submitButton.addActionListener(event -> {

            // Get the values entered by the user
            String clientId = clientIdField.getText();
            String jobId = jobIdField.getText();
            String duration = jobDurationField.getText();
            String deadline = jobDeadlineField.getText();

            // Create a ClientSubmit object using the entered information
            ClientSubmit submit = new ClientSubmit(
                    clientId, jobId, duration, deadline
            );

            // Submit the job and display a success message
            try {
                submit.submit();
                JOptionPane.showMessageDialog(null, "Saved");

                // Clear the fields after a successful submission
                clientIdField.setText("");
                jobIdField.setText("");
                jobDurationField.setText("");
                jobDeadlineField.setText("");
            }

            // Display an error message if validation or submission fails
            catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        });
    }
}