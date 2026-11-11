
package gui;

import controller.VehicularCloudController;
import model.Job;
import scheduling.ScheduleEntry;
import scheduling.ScheduleResult;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Locale;

public class ClientPanel extends JPanel {

    private final JTextField clientIdField = new JTextField(22);
    private final JTextField jobIdField = new JTextField(22);

    private final JTextField jobAvailabilityField = new JTextField(12);

    private final JSpinner startHour =
            new JSpinner(new SpinnerNumberModel(9, 1, 12, 1));

    private final JSpinner startMinute =
            new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

    private final JComboBox<String> startAmPm =
            new JComboBox<>(new String[]{"AM", "PM"});

    private final JTextField jobDurationField = new JTextField(22);

    private final JTextField jobDeadlineField = new JTextField(12);

    private final JSpinner endHour =
            new JSpinner(new SpinnerNumberModel(5, 1, 12, 1));

    private final JSpinner endMinute =
            new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

    private final JComboBox<String> endAmPm =
            new JComboBox<>(new String[]{"AM", "PM"});

    private final JTextField priorityField = new JTextField(22);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{
                    "Job ID",
                    "Client ID",
                    "Duration (days)",
                    "Priority",
                    "Waiting time (days)",
                    "Completion time (days)"
            }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable scheduleTable = new JTable(tableModel);

    private final JLabel averageWaitingTime =
            new JLabel("Average waiting time: —");

    public ClientPanel() {

        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        configureAmPm(startAmPm);
        configureAmPm(endAmPm);

        // Set the initial time selections.
        startAmPm.setSelectedItem("AM");
        endAmPm.setSelectedItem("PM");

        // Title section.
        JPanel titlePanel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Submit a Job");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        titlePanel.add(title, BorderLayout.NORTH);

        titlePanel.add(
                new JLabel("Enter the job details below. Fields marked * are required."),
                BorderLayout.SOUTH
        );

        titlePanel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8, 0)
        );

        add(titlePanel, BorderLayout.NORTH);

        // Form fields.
        JPanel fields = FormRows.fields();

        // Availability date and start time.
        JPanel availabilityRow = new JPanel(new BorderLayout(10, 0));

        availabilityRow.add(
                jobAvailabilityField,
                BorderLayout.CENTER
        );

        JPanel startTimeControls =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

        startTimeControls.add(new JLabel("Start time:"));
        startTimeControls.add(startHour);
        startTimeControls.add(new JLabel(":"));
        startTimeControls.add(startMinute);
        startTimeControls.add(startAmPm);

        availabilityRow.add(
                startTimeControls,
                BorderLayout.EAST
        );

        // Deadline date and end time.
        JPanel deadlineRow = new JPanel(new BorderLayout(10, 0));

        deadlineRow.add(
                jobDeadlineField,
                BorderLayout.CENTER
        );

        JPanel endTimeControls =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

        endTimeControls.add(new JLabel("End time:"));
        endTimeControls.add(endHour);
        endTimeControls.add(new JLabel(":"));
        endTimeControls.add(endMinute);
        endTimeControls.add(endAmPm);

        deadlineRow.add(
                endTimeControls,
                BorderLayout.EAST
        );

        // Add form rows.
        FormRows.addRow(fields, 0, "Client ID *", clientIdField);
        FormRows.addRow(fields, 1, "Job ID *", jobIdField);

        FormRows.addRow(
                fields, 2, "Job availability (MM-dd-yyyy) *", availabilityRow
        );

        FormRows.addRow(
                fields, 3, "Job duration (days) *", jobDurationField
        );

        FormRows.addRow(
                fields, 4, "Job deadline (MM-dd-yyyy) *", deadlineRow
        );

        FormRows.addRow(
                fields, 5, "Priority (positive whole number) *", priorityField
        );

        JLabel hint = new JLabel(
                "Dates must use MM-dd-yyyy. Duration must fit between availability and deadline. "
                        + "End date/time must be after start date/time."
        );

        JPanel form = new JPanel(new BorderLayout(0, 8));

        form.add(fields, BorderLayout.CENTER);
        form.add(hint, BorderLayout.SOUTH);

        // Buttons.
        JButton refresh = new JButton("Refresh schedule");
        refresh.addActionListener(event -> refreshSchedule());

        JButton submit = new JButton("Submit job");
        submit.addActionListener(event -> submitJob());

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

        scheduleTitle.setFont(
                scheduleTitle.getFont().deriveFont(Font.BOLD, 16f)
        );

        JPanel scheduleHeading = new JPanel(new BorderLayout(0, 4));

        scheduleHeading.add(scheduleTitle, BorderLayout.NORTH);

        scheduleHeading.add(
                new JLabel("Jobs are ordered by priority; lower priority numbers run first."),
                BorderLayout.SOUTH
        );

        schedulePanel.add(scheduleHeading, BorderLayout.NORTH);
        schedulePanel.add(tableScroll, BorderLayout.CENTER);
        schedulePanel.add(averageWaitingTime, BorderLayout.SOUTH);

        // Main content.
        JPanel content = new JPanel(new BorderLayout(0, 16));

        content.add(formSection, BorderLayout.NORTH);
        content.add(schedulePanel, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    // Refresh the schedule using the shared controller.
    private void refreshSchedule() {

        try {

            ScheduleResult result =
                    VehicularCloudController.getInstance().scheduleJobs();

            tableModel.setRowCount(0);

            for (ScheduleEntry entry : result.getEntries()) {

                Job job = entry.getJob();

                tableModel.addRow(new Object[]{
                        job.getJobId(),
                        job.getClientId(),
                        job.getEstimatedDuration(),
                        job.getPriority(),
                        entry.getWaitingTime(),
                        entry.getCompletionTime()
                });
            }

            averageWaitingTime.setText(
                    result.getEntries().isEmpty()
                            ? "Average waiting time: —"
                            : String.format(
                            Locale.US,
                            "Average waiting time: %.2f days",
                            result.getAverageWaitingTime()
                    )
            );

        } catch (RuntimeException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Could not refresh schedule: " + ex.getMessage(),
                    "Schedule error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Submit a job.
    private void submitJob() {
        ClientSubmit submit = new ClientSubmit(
                clientIdField.getText(),
                jobIdField.getText(),
                jobAvailabilityField.getText(),
                jobDurationField.getText(),
                jobDeadlineField.getText(),
                priorityField.getText(),
                formatTime(startHour, startMinute, startAmPm),
                formatTime(endHour, endMinute, endAmPm)
        );

        try {

            submit.submit();

            JOptionPane.showMessageDialog(
                    this,
                    "Job saved.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            refreshSchedule();

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

        jobAvailabilityField.setText("");
        jobDurationField.setText("");
        jobDeadlineField.setText("");

        priorityField.setText("");

        startHour.setValue(9);
        startMinute.setValue(0);
        startAmPm.setSelectedItem("AM");

        endHour.setValue(5);
        endMinute.setValue(0);
        endAmPm.setSelectedItem("PM");
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
