package gui;

import controller.VehicularCloudController;
import model.Job;
import scheduling.ScheduleEntry;
import scheduling.ScheduleResult;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ClientPanel extends JPanel {
    private final JTextField clientIdField = new JTextField(22);
    private final JTextField jobIdField = new JTextField(22);
    private final JTextField jobAvailabilityField = new JTextField(22);
    private final JTextField jobDurationField = new JTextField(22);
    private final JTextField jobDeadlineField = new JTextField(22);
    private final JTextField priorityField = new JTextField(22);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Client ID", "Duration (days)", "Priority",
                    "Waiting time (days)", "Completion time (days)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable scheduleTable = new JTable(tableModel);
    private final JLabel averageWaitingTime = new JLabel("Average waiting time: —");

    public ClientPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Submit a Job");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(new JLabel("Enter the job details below. Fields marked * are required."),
                BorderLayout.SOUTH);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        add(titlePanel, BorderLayout.NORTH);

        JPanel fields = FormRows.fields();
        FormRows.addRow(fields, 0, "Client ID *", clientIdField);
        FormRows.addRow(fields, 1, "Job ID *", jobIdField);
        FormRows.addRow(fields, 2, "Job availability (MM-dd-yyyy) *", jobAvailabilityField);
        FormRows.addRow(fields, 3, "Job duration (days) *", jobDurationField);
        FormRows.addRow(fields, 4, "Job deadline (MM-dd-yyyy) *", jobDeadlineField);
        FormRows.addRow(fields, 5, "Priority (positive whole number) *", priorityField);

        JLabel hint = new JLabel("Dates must use MM-dd-yyyy. Duration must fit between availability and deadline.");
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(fields, BorderLayout.CENTER);
        form.add(hint, BorderLayout.SOUTH);

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

        scheduleTable.setFillsViewportHeight(true);
        scheduleTable.setAutoCreateRowSorter(true);
        JScrollPane tableScroll = new JScrollPane(scheduleTable);
        JPanel schedulePanel = new JPanel(new BorderLayout(0, 8));
        JLabel scheduleTitle = new JLabel("Schedule Results");
        scheduleTitle.setFont(scheduleTitle.getFont().deriveFont(Font.BOLD, 16f));
        JPanel scheduleHeading = new JPanel(new BorderLayout(0, 4));
        scheduleHeading.add(scheduleTitle, BorderLayout.NORTH);
        scheduleHeading.add(new JLabel("Jobs are ordered by priority; lower priority numbers run first."),
                BorderLayout.SOUTH);
        schedulePanel.add(scheduleHeading, BorderLayout.NORTH);
        schedulePanel.add(tableScroll, BorderLayout.CENTER);
        schedulePanel.add(averageWaitingTime, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.add(formSection, BorderLayout.NORTH);
        content.add(schedulePanel, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private void refreshSchedule() {
        try {
            ScheduleResult result = new VehicularCloudController().scheduleJobs();
            tableModel.setRowCount(0);
            for (ScheduleEntry entry : result.getEntries()) {
                Job job = entry.getJob();
                tableModel.addRow(new Object[]{
                        job.getJobId(), job.getClientId(), job.getEstimatedDuration(),
                        job.getPriority(), entry.getWaitingTime(), entry.getCompletionTime()
                });
            }
            averageWaitingTime.setText(result.getEntries().isEmpty()
                    ? "Average waiting time: —"
                    : String.format("Average waiting time: %.2f days", result.getAverageWaitingTime()));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Could not refresh schedule: " + ex.getMessage(),
                    "Schedule error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitJob() {
        ClientSubmit submit = new ClientSubmit(
                clientIdField.getText(), jobIdField.getText(), jobAvailabilityField.getText(),
                jobDurationField.getText(), jobDeadlineField.getText(), priorityField.getText());
        try {
            submit.submit();
            JOptionPane.showMessageDialog(this, "Job saved.", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
            refreshSchedule();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Could not submit job",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        clientIdField.setText("");
        jobIdField.setText("");
        jobAvailabilityField.setText("");
        jobDurationField.setText("");
        jobDeadlineField.setText("");
        priorityField.setText("");
    }
}
