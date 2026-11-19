
package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import controller.VehicularCloudController;
import model.Job;
import model.RequestType;
import model.ServerResponse;
import model.SubmissionRequest;
import model.Vehicle;
import scheduling.ScheduleEntry;
import scheduling.ScheduleResult;

// Controller screen. It can see every request, saved job, and registered vehicle.
public class ControllerPanel extends JPanel {

    private final DefaultTableModel pendingRows = new DefaultTableModel(
            new String[] {"Request ID", "Type", "From", "Details"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel jobRows = new DefaultTableModel(
            new String[] {"Job ID", "Client", "Email", "Priority", "Completion (hours)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // Table containing every accepted vehicle.
    private final DefaultTableModel vehicleRows = new DefaultTableModel(
            new String[] {
                    "Vehicle ID",
                    "Owner ID",
                    "Account Email",
                    "Vehicle",
                    "Arrival",
                    "Estimated Departure",
                    "Compute Power"
            }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable pendingTable = new JTable(pendingRows);
    private final JTable vehicleTable = new JTable(vehicleRows);

    private final JLabel averageLabel =
            new JLabel("Average waiting time (hours): —");

    public ControllerPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("VC Controller");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "Select a request. Accept saves it. Reject does not.");
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setAlignmentX(LEFT_ALIGNMENT);
        titleBlock.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 8, 0));
        titleBlock.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 70));

        titleBlock.add(title);
        titleBlock.add(Box.createRigidArea(new Dimension(0, 4)));
        titleBlock.add(subtitle);

        page.add(titleBlock);

        JButton accept = new JButton("Accept");
        accept.addActionListener(event -> decide(true));

        JButton reject = new JButton("Reject");
        reject.addActionListener(event -> decide(false));

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40));

        buttons.add(accept);
        buttons.add(reject);
        buttons.add(refresh);

        page.add(buttons);
        page.add(Box.createRigidArea(new Dimension(0, 16)));

        // Pending requests.
        page.add(section("Waiting for review", pendingTable));

        page.add(Box.createRigidArea(new Dimension(0, 16)));

        // Saved jobs.
        page.add(section("All saved jobs", new JTable(jobRows)));

        averageLabel.setAlignmentX(LEFT_ALIGNMENT);
        averageLabel.setBorder(
                BorderFactory.createEmptyBorder(8, 0, 8, 0));

        page.add(averageLabel);

        page.add(Box.createRigidArea(new Dimension(0, 16)));

        // All accepted vehicles from every owner.
        page.add(section("All Registered Vehicles", vehicleTable));

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);

        refresh();

        ReviewQueue.get().setOnChange(
                () -> SwingUtilities.invokeLater(this::refresh));
    }

    // Accept or reject the selected waiting request.
    private void decide(boolean accept) {

        int row = pendingTable.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(
                    this, "Select a request first.");
            return;
        }

        String requestId = String.valueOf(
                pendingTable.getValueAt(row, 0));

        ServerResponse response =
                ReviewQueue.decide(requestId, accept);

        JOptionPane.showMessageDialog(
                this, response.getMessage());

        refresh();
    }

    public void refresh() {

        VehicularCloudController controller =
                VehicularCloudController.getInstance();

        // Reload records so this screen sees saved data.
        controller.refreshSavedData();

        // Refresh pending requests.
        pendingRows.setRowCount(0);

        List<SubmissionRequest> waiting =
                ReviewQueue.get().list();

        for (int i = 0; i < waiting.size(); i++) {

            SubmissionRequest request = waiting.get(i);

            pendingRows.addRow(new Object[] {
                    request.getRequestId(),
                    request.getType().toString(),
                    request.getRequesterEmail(),
                    details(request)
            });
        }

        // Refresh saved jobs.
        jobRows.setRowCount(0);

        ScheduleResult result = controller.scheduleJobs();

        for (ScheduleEntry entry : result.getEntries()) {

            Job job = entry.getJob();

            jobRows.addRow(new Object[] {
                    job.getJobId(),
                    job.getClientId(),
                    controller.getJobAccountEmail(job.getJobId()),
                    Integer.valueOf(job.getPriority()),
                    Integer.valueOf(entry.getCompletionTime())
            });
        }

        averageLabel.setText(
                "Average waiting time (hours): "
                        + result.getAverageWaitingTime());

        // Refresh all registered vehicles.
        vehicleRows.setRowCount(0);

        List<Vehicle> vehicles = controller.getVehicles();

        for (int i = vehicles.size() - 1; i >= 0; i--) {

            Vehicle vehicle = vehicles.get(i);

            String vehicleId = vehicle.getVehicleId();

            vehicleRows.addRow(new Object[] {
                    vehicleId,
                    controller.getVehicleOwnerId(vehicleId),
                    controller.getVehicleAccountEmail(vehicleId),
                    vehicle.getYear() + " "
                            + vehicle.getMake() + " "
                            + vehicle.getModel(),
                    vehicle.getArrivalTime(),
                    vehicle.getDepartureTime(),
                    controller.getVehicleComputePower(vehicleId)
            });
        }
    }

    private String details(SubmissionRequest request) {

        if (request.getType() == RequestType.VEHICLE) {

            Vehicle vehicle = request.getVehicle();

            return "Owner ID: " + request.getOwnerId()
                    + " | " + vehicle.getYear()
                    + " " + vehicle.getMake()
                    + " " + vehicle.getModel()
                    + " | Arrival: " + vehicle.getArrivalTime()
                    + " | Departure: " + vehicle.getDepartureTime()
                    + " | Compute: " + request.getComputePower();
        }

        Job job = request.getJob();

        return job.getJobId() + " priority " + job.getPriority();
    }

    // One titled table. The height stays readable, and the columns use the window width.
    private JPanel section(String title, JTable table) {

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setPreferredScrollableViewportSize(new Dimension(680, 180));

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 230));

        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }
}
