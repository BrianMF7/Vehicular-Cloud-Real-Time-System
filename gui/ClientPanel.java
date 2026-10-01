package gui;

import javax.swing.*;
import java.awt.*;

public class ClientPanel extends JPanel {
    //Labels and teet field for client information
    private JLabel clientLabel = new JLabel("Client ID: ");
    private JTextField clientIdField = new JTextField();
    //Labels and text fields for job information
    private JLabel jobLabel = new JLabel("Job ID: ");
    private JTextField jobIdField = new JTextField();
    //Labels and text fields for job duration
    private JLabel jobDurationLabel = new JLabel("Job Duration: ");
    private JTextField jobDurationField = new JTextField();
    //Label and text field for deadline
    private JLabel  jobDeadlineLabel = new JLabel("Job Deadline: (MM-dd-yyyy)");
    private JTextField  jobDeadlineField = new JTextField();
    //Button used to submit job
    private JButton submitButton = new JButton("Submit job: ");

    public ClientPanel(){
        //set the maon panel layout
        setLayout(new BorderLayout(10, 10));
        //Create panel for client, job information, and submit button
        JPanel identityPanel = new JPanel();
        JPanel jobPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        //Client ID and job ID fields to identity panel
        identityPanel.setLayout(new GridLayout(4,1,5,5));
        identityPanel.add(clientLabel);
        identityPanel.add(clientIdField);
        identityPanel.add(jobLabel);
        identityPanel.add(jobIdField);
        //Job duration and deadline fields to the job panel
        jobPanel.setLayout(new GridLayout(4,1,5,5));
        jobPanel.add(jobDurationLabel);
        jobPanel.add(jobDurationField);
        jobPanel.add(jobDeadlineLabel);
        jobPanel.add(jobDeadlineField);
        //Position each panel in the mian panel
        add(identityPanel, BorderLayout.NORTH);
        add(jobPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        //This is the submit button
        buttonPanel.add(submitButton);
        //This section is what happens when you press the button
        submitButton.addActionListener(event -> {
            //Get the values entered by the user
            String clientId = clientIdField.getText();
            String jobId = jobIdField.getText();
            String duration = jobDurationField.getText();
            String deadline = jobDeadlineField.getText();
            ClientSubmit submit = new ClientSubmit(clientId, jobId, duration, deadline);
            //when all fields are put in and are in correct format, but a sucess message
            try{
                submit.submit();
                JOptionPane.showMessageDialog(null, "Saved");
                clientIdField.setText("");
                jobIdField.setText("");
                jobDurationField.setText("");
                jobDeadlineField.setText("");
            }
            //when there is a missing field or wrong format
            catch(Exception e){
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        });
    }
}
