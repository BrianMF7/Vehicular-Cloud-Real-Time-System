package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import controller.VehicularCloudController;
import model.Job;
import scheduling.ScheduleEntry;
import scheduling.ScheduleResult;

public class ClientPanel extends JPanel {

    //All the text boxes for job info
    private final JTextField clientIdField = new JTextField(22);
    private final JTextField jobIdField = new JTextField(22);
    private final JTextField jobAvailabilityField = new JTextField(22);
    private final JTextField jobDurationField = new JTextField(22);
    private final JTextField jobDeadlineField = new JTextField(22);

    //Shows completion times after the button is clicked
    private final JTextArea results = new JTextArea(8, 30);

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

        results.setEditable(false);
         results.setLineWrap(true);
          results.setWrapStyleWord(true);
        results.setText("Completion times will show here.");

          JButton calculate = new JButton("Calculate completion time");
        calculate.addActionListener(event -> showCompletionTimes());

        JButton submit = new JButton("Submit job");
           submit.addActionListener(event -> submitJob());

          JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(calculate);
        buttons.add(submit);

         JPanel bottom = new JPanel(new BorderLayout(0, 8));
           bottom.add(new JScrollPane(results), BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        }

    //Ask the controller to schedule every saved job, then show the times on this screen
    private void showCompletionTimes() {
        VehicularCloudController controller = new VehicularCloudController();
           ScheduleResult result = controller.scheduleJobs();

        if (result.getEntries().isEmpty()) {
            results.setText("No jobs saved yet.");
            return;
             }

        StringBuilder text = new StringBuilder();
           for (ScheduleEntry entry : result.getEntries()) {
            Job job = entry.getJob();
            text.append("Job ID: ").append(job.getJobId());
               text.append(", Client ID: ").append(job.getClientId());
            text.append(", Duration: ").append(job.getEstimatedDuration());
              text.append(", Priority: ").append(job.getPriority());
            text.append(", Completion time: ").append(entry.getCompletionTime());
            text.append("\n");
        }
        text.append("Average waiting time: ").append(result.getAverageWaitingTime());
        results.setText(text.toString());
        results.setCaretPosition(0);
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
