package gui;

import java.awt.*;

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

    //Labels and text fields for job availability
    private JLabel jobAvailabilityLabel = new JLabel("Job Availability: (MM-dd-yyyy) ");
    private JTextField jobAvailabilityField = new JTextField();

    //Labels and text fields for job duration
    private JLabel jobDurationLabel = new JLabel("Job Duration: (Days) ");
    private JTextField jobDurationField = new JTextField();

    //Labels and text fields for job deadline
    private JLabel jobDeadlineLabel = new JLabel("Job Deadline: (MM-dd-yyyy)");
    private JTextField jobDeadlineField = new JTextField();

    // Button used to submit the job
    private JButton submitButton = new JButton("Submit job ");

    public ClientPanel() {
        // Set the main panel layout
        setLayout(new BorderLayout(10, 10));
        // Create a panel for the labels and text fields
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // Add spacing between rows and columns
        gbc.insets = new Insets(5, 2, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;
        // Set the consistent size for all text fields
        Dimension fieldSize = new Dimension(400, 25);

        clientIdField.setPreferredSize(fieldSize);
        jobIdField.setPreferredSize(fieldSize);
        jobAvailabilityField.setPreferredSize(fieldSize);
        jobDurationField.setPreferredSize(fieldSize);
        jobDeadlineField.setPreferredSize(fieldSize);

        // Add the client ID label and text field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        inputPanel.add(clientLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        inputPanel.add(clientIdField, gbc);

        // Add the job ID label and text field
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(jobLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(jobIdField, gbc);

        // Add the availability label and text field
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(jobAvailabilityLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(jobAvailabilityField, gbc);

        // Add the duration label and text field
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(jobDurationLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(jobDurationField, gbc);

        // Add the deadline label and text field
        gbc.gridx = 0;
        gbc.gridy = 4;
        inputPanel.add(jobDeadlineLabel, gbc);
        gbc.gridx = 1;
        inputPanel.add(jobDeadlineField, gbc);

        // Create a separate panel for the Submit Job button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(submitButton);

        add(inputPanel, BorderLayout.WEST);
        add(buttonPanel, BorderLayout.SOUTH);
        // Handle the job submission when the button is clicked
        submitButton.addActionListener(event -> {
            // Get the values entered by the user
            String clientId = clientIdField.getText();
            String jobId = jobIdField.getText();
            String jobAvailability = jobAvailabilityField.getText();
            String duration = jobDurationField.getText();
            String deadline = jobDeadlineField.getText();

            // Create a ClientSubmit object using the entered information
            ClientSubmit submit = new ClientSubmit (clientId, jobId,jobAvailability, duration, deadline);
            // Submit the job and display a success message
            try {
                submit.submit();
                JOptionPane.showMessageDialog(null, "Saved");
                // Clear the fields after a successful submission
                clientIdField.setText("");
                jobIdField.setText("");
                jobAvailabilityField.setText("");
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
