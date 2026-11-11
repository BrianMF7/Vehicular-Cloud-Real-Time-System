
package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import com.toedter.calendar.JDateChooser;
import controller.VehicularCloudController;
import model.Job;
import model.SubmissionRequest;
import scheduling.ScheduleEntry;
import scheduling.ScheduleResult;
import javax.swing.SwingUtilities;
import controller.ControllerClient;
import model.RequestStatus;
import model.ServerResponse;

public class ClientPanel extends JPanel {

    private final JTextField clientIdField = new JTextField(22);
    private final JTextField jobIdField = new JTextField(22);

    // Calendar date selectors.
    private final JDateChooser jobAvailabilityChooser = new JDateChooser();

    private final JSpinner startHour = new JSpinner(new SpinnerNumberModel(9, 1, 12, 1));

    private final JSpinner startMinute = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

    private final JComboBox<String> startAmPm = new JComboBox<>(new String[]{"AM", "PM"});

    private final JTextField jobDurationField = new JTextField(22);

    private final JDateChooser jobDeadlineChooser = new JDateChooser();

    private final JSpinner endHour = new JSpinner(new SpinnerNumberModel(5, 1, 12, 1));

    private final JSpinner endMinute = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

    private final JComboBox<String> endAmPm = new JComboBox<>(new String[]{"AM", "PM"});

    private final JTextField priorityField = new JTextField(22);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{
                    "Job ID",
                    "Client ID",
                    "Duration (days)",
                    "Priority",
                    "Status"
            }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable scheduleTable = new JTable(tableModel);
    private final JLabel jobStatus = new JLabel("Your jobs only. Completion time is shown to the controller.");

    public ClientPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        configureAmPm(startAmPm);
        configureAmPm(endAmPm);
        configureDateChooser(jobAvailabilityChooser);
        configureDateChooser(jobDeadlineChooser);
        startAmPm.setSelectedItem("AM");
        endAmPm.setSelectedItem("PM");

        // Title section.
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Submit a Job");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        titlePanel.add(title, BorderLayout.NORTH);

        titlePanel.add(new JLabel("Enter the job details below. Fields marked * are required."), BorderLayout.SOUTH);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        add(titlePanel, BorderLayout.NORTH);
        // Form fields.
        JPanel fields = FormRows.fields();
        // Availability date and start time.
        JPanel availabilityRow = new JPanel(new BorderLayout(10, 0));
        availabilityRow.add(jobAvailabilityChooser, BorderLayout.CENTER);
        JPanel startTimeControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

        startTimeControls.add(new JLabel("Start time:"));
        startTimeControls.add(startHour);
        startTimeControls.add(new JLabel(":"));
        startTimeControls.add(startMinute);
        startTimeControls.add(startAmPm);

        availabilityRow.add(startTimeControls, BorderLayout.EAST);
        // Deadline date and end time.
        JPanel deadlineRow = new JPanel(new BorderLayout(10, 0));
        deadlineRow.add(jobDeadlineChooser, BorderLayout.CENTER);
        JPanel endTimeControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

        endTimeControls.add(new JLabel("End time:"));
        endTimeControls.add(endHour);
        endTimeControls.add(new JLabel(":"));
        endTimeControls.add(endMinute);
        endTimeControls.add(endAmPm);

        deadlineRow.add(endTimeControls, BorderLayout.EAST);
        // Add form rows.
        FormRows.addRow(fields, 0, "Client ID *", clientIdField);
        FormRows.addRow(fields, 1, "Job ID *", jobIdField);

        FormRows.addRow(fields, 2, "Job availability *", availabilityRow);
        FormRows.addRow(fields, 3, "Job duration (days) *", jobDurationField);
        FormRows.addRow(fields, 4, "Job deadline *", deadlineRow);
        FormRows.addRow(fields, 5, "Priority (positive whole number) *", priorityField);
        JLabel hint = new JLabel("Select dates using the calendar. Duration must fit between availability and deadline. " + "End date/time must be after start date/time.");
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(fields, BorderLayout.CENTER);
        form.add(hint, BorderLayout.SOUTH);

        // Buttons.
        JButton refresh = new JButton("Refresh schedule");
        refresh.addActionListener(event -> refreshSchedule());
        JButton submit = new JButton("Submit job");
        submit.addActionListener(event -> submitJob(submit));
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.add(refresh);
        buttons.add(submit);
        JPanel formSection = new JPanel(new BorderLayout(0, 8));
        formSection.add(form, BorderLayout.CENTER);
        formSection.add(buttons, BorderLayout.SOUTH);

        // Schedule table.
        scheduleTable.setFillsViewportHeight(true);
        scheduleTable.setAutoCreateRowSorter(true);
        JScrollPane tableScroll = new JScrollPane(scheduleTable);
        JPanel schedulePanel = new JPanel(new BorderLayout(0, 8));
        JLabel scheduleTitle = new JLabel("Schedule Results");
        scheduleTitle.setFont(scheduleTitle.getFont().deriveFont(Font.BOLD, 16f));
        JPanel scheduleHeading = new JPanel(new BorderLayout(0, 4));
        scheduleHeading.add(scheduleTitle, BorderLayout.NORTH);
        scheduleHeading.add(new JLabel("Jobs are ordered by priority; lower priority numbers run first."), BorderLayout.SOUTH);

        schedulePanel.add(scheduleHeading, BorderLayout.NORTH);
        schedulePanel.add(tableScroll, BorderLayout.CENTER);
        schedulePanel.add(jobStatus, BorderLayout.SOUTH);

        // Main content.
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.add(formSection, BorderLayout.NORTH);
        content.add(schedulePanel, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    // Refresh the schedule using the shared controller.
    private void refreshSchedule() {
        try {
            VehicularCloudController controller = VehicularCloudController.getInstance();
            ScheduleResult result = controller.scheduleJobs();
            String mine = MainFrame.getCurrentEmail();
            tableModel.setRowCount(0);
            for (ScheduleEntry entry : result.getEntries()) {
                Job job = entry.getJob();
                String ownerEmail = controller.getJobAccountEmail(job.getJobId());
                // A user only sees jobs saved under their own email.
                if (!mine.isEmpty() && !mine.equalsIgnoreCase(ownerEmail)) {
                    continue;
                }
                tableModel.addRow(new Object[]{
                        job.getJobId(),
                        job.getClientId(),
                        job.getEstimatedDuration(),
                        job.getPriority(),
                        "ACCEPTED"
                });
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Could not refresh schedule: " + ex.getMessage(), "Schedule error", JOptionPane.ERROR_MESSAGE);
        }
    }
    // Send the job to the controller. It is not saved until accepted.
    private void submitJob(JButton submitButton) {
        ClientSubmit submit = new ClientSubmit(
                clientIdField.getText(),
                jobIdField.getText(),
                formatDate(jobAvailabilityChooser),
                jobDurationField.getText(),
                formatDate(jobDeadlineChooser),
                priorityField.getText(),
                formatTime(startHour, startMinute, startAmPm),
                formatTime(endHour, endMinute, endAmPm)
        );
        try {
            Job job = submit.createJob();
            SubmissionRequest request = SubmissionRequest.forJob(job, MainFrame.getCurrentUser(), MainFrame.getCurrentEmail());
            String submittingEmail = MainFrame.getCurrentEmail();
            ControllerClient client = new ControllerClient();
            submitButton.setEnabled(false);
            jobStatus.setText("Sending job submission to controller...");
            // Socket communication runs in the background.
            Thread submissionThread = new Thread(() -> {
                try {
                    ServerResponse decision = client.submit(request, acknowledgement ->
                            SwingUtilities.invokeLater(() -> {
                                jobStatus.setText("ACKNOWLEDGED: " + acknowledgement.getMessage());
                                // Show the submitted job while the controller reviews it.
                                updateJobRow(job, "PENDING");
                            })
                    );
                    // Display the final controller decision.
                    SwingUtilities.invokeLater(() -> {
                        String title;
                        String message;

                        if (decision.getStatus() == RequestStatus.ACCEPTED) {
                            title = "Job Accepted";
                            message = "Job ID: " + job.getJobId()
                                    + "\nStatus: ACCEPTED"
                                    + "\n\n" + decision.getMessage();

                            clearFields();
                            refreshSchedule();
                            updateJobRow(job, "ACCEPTED");

                        } else if (decision.getStatus() == RequestStatus.REJECTED) {
                        title = "Job Rejected";
                        message = "Job ID: " + job.getJobId()
                                + "\nStatus: REJECTED"
                                + "\n\n" + decision.getMessage();

                        updateJobRow(job, "REJECTED");

                        } else {
                            title = "Submission Error";
                            message = decision.getMessage();
                        }
                        // Store the notification for the original submitter.
                        MainFrame.queueClientNotification(
                                submittingEmail,
                                title,
                                message
                        );
                        submitButton.setEnabled(true);
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        jobStatus.setText("Could not communicate with controller: " + ex.getMessage());
                        JOptionPane.showMessageDialog(
                                this,
                                ex.getMessage(),
                                "Connection Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        submitButton.setEnabled(true);
                    });
                }
            }, "job-submission");
            submissionThread.start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Could not submit job",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Reset the form.
    private void clearFields() {
        clientIdField.setText("");
        jobIdField.setText("");
        jobAvailabilityChooser.setDate(null);
        jobDurationField.setText("");
        jobDeadlineChooser.setDate(null);
        priorityField.setText("");
        startHour.setValue(9);
        startMinute.setValue(0);
        startAmPm.setSelectedItem("AM");
        endHour.setValue(5);
        endMinute.setValue(0);
        endAmPm.setSelectedItem("PM");
    }

    // Configure a popup calendar and display dates as MM-dd-yyyy.
    private static void configureDateChooser(JDateChooser chooser) {
        chooser.setDateFormatString("MM-dd-yyyy");
        chooser.setPreferredSize(new Dimension(150, 25));
        chooser.getDateEditor().getUiComponent().setEnabled(true);
        if (chooser.getDateEditor().getUiComponent() instanceof JTextField) {
            ((JTextField) chooser.getDateEditor().getUiComponent()).setEditable(false);
        }
    }

    // Convert the selected calendar date into the format expected by ClientSubmit.
    private static String formatDate(JDateChooser chooser) {
        if (chooser.getDate() == null) {
            return "";
        }
        return new SimpleDateFormat("MM-dd-yyyy", Locale.US).format(chooser.getDate());
    }

    // Add a job to the table or update its existing status.
    private void updateJobRow(Job job, String status) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String existingJobId = tableModel.getValueAt(row, 0).toString();

            if (existingJobId.equals(job.getJobId())) {
                tableModel.setValueAt(status, row, 4);
                return;
            }
        }

        tableModel.addRow(new Object[]{
                job.getJobId(),
                job.getClientId(),
                job.getEstimatedDuration(),
                job.getPriority(),
                status
        });
    }

    private static void configureAmPm(JComboBox<String> combo) {
        combo.setEditable(false);
        combo.setPrototypeDisplayValue("PM");
        combo.setPreferredSize(new Dimension(65, 25));
        combo.setMinimumSize(new Dimension(65, 25));
    }
    // Convert the selected time into HH:mm AM/PM format.
    private static String formatTime(
            JSpinner hour,
            JSpinner minute,
            JComboBox<String> amPm) {
        return String.format(
                Locale.US,
                "%02d:%02d %s",
                (Integer) hour.getValue(),
                (Integer) minute.getValue(),
                amPm.getSelectedItem()
        );
    }
}
