package gui;

import javax.swing.*;
import java.awt.*;

public class ClientPanel extends JPanel {
    //componets for the class
    private JLabel clientLabel = new JLabel("Client ID");
    private JTextField clientIdField = new JTextField();

    private JLabel jobLabel = new JLabel("Job ID");
    private JTextField jobIdField = new JTextField();

    private JLabel jobDurationLabel = new JLabel("Job Duration");
    private JTextField jobDurationField = new JTextField();

    private JLabel  jobDeadlineLabel = new JLabel("Job Deadline");
    private JTextField  jobDeadlineField = new JTextField();

    private JButton submitButton = new JButton("Submit job");

    public ClientPanel(){
        setLayout(new BorderLayout(10, 10));
        //Panel
        JPanel identityPanel = new JPanel();
        JPanel jobPanel = new JPanel();
        JPanel buttonPanel = new JPanel();

        identityPanel.setLayout(new GridLayout(4,1,5,5));
        identityPanel.add(clientLabel);
        identityPanel.add(clientIdField);
        identityPanel.add(jobLabel);
        identityPanel.add(jobIdField);

        jobPanel.setLayout(new GridLayout(4,1,5,5));
        jobPanel.add(jobDurationLabel);
        jobPanel.add(jobDurationField);
        jobPanel.add(jobDeadlineLabel);
        jobPanel.add(jobDeadlineField);

        add(identityPanel, BorderLayout.NORTH);
        add(jobPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        buttonPanel.add(submitButton);

        submitButton.addActionListener(event -> {
            String clientId = clientIdField.getText();
            String jobId = jobIdField.getText();
            String duration = jobDurationField.getText();
            String deadline = jobDeadlineField.getText();
            ClientSubmit submit = new ClientSubmit(clientId, jobId, duration, deadline);
            try{
                submit.submit();
                JOptionPane.showMessageDialog(null, "Saved");
                clientIdField.setText("");
                jobIdField.setText("");
                jobDurationField.setText("");
                jobDeadlineField.setText("");
            }
            catch(Exception e){
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        });
    }
}
