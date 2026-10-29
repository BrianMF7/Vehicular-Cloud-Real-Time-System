package gui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ClientPanel extends JPanel {

    //All the text boxes for job info
    private final JTextField clientIdField = new JTextField(22);
    private final JTextField jobIdField = new JTextField(22);
      private final JTextField jobAvailabilityField = new JTextField(22);
      private final JTextField jobDurationField = new JTextField(22);
       private final JTextField jobDeadlineField = new JTextField(22);

    public ClientPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        //Same row layout as the rent car form 
        JPanel fields = FormRows.fields();
         FormRows.addRow(fields, 0, "Client ID *", clientIdField);
          FormRows.addRow(fields, 1, "Job ID *", jobIdField);
         FormRows.addRow(fields, 2, "Job availability (MM-dd-yyyy) *", jobAvailabilityField);
        FormRows.addRow(fields, 3, "Job duration (days) *", jobDurationField);
         FormRows.addRow(fields, 4, "Job deadline (MM-dd-yyyy) *", jobDeadlineField);
          add(fields, BorderLayout.CENTER);

        //Hint on the left, button on the right, same as Register car
          JButton submit = new JButton("Submit job");
           submit.addActionListener(event -> submitJob());
        add(FormRows.footer("Dates use MM-dd-yyyy. * Required", submit), BorderLayout.SOUTH);
    }

    private void submitJob() {
        String clientId = clientIdField.getText().trim();
           String jobId = jobIdField.getText().trim();
        String jobAvailability = jobAvailabilityField.getText().trim();
           String duration = jobDurationField.getText().trim();
        String deadline = jobDeadlineField.getText().trim();

        ClientSubmit submit = new ClientSubmit(clientId, jobId, jobAvailability, duration, deadline);
        try {
            submit.submit();
            JOptionPane.showMessageDialog(this, "Saved");
            clearFields();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Could not submit job", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        clientIdField.setText("");
          jobIdField.setText("");
           jobAvailabilityField.setText("");
           jobDurationField.setText("");
        jobDeadlineField.setText("");
       }
    }
