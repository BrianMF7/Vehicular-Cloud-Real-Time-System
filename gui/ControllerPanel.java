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
import javax.swing.table.DefaultTableModel;

import controller.PendingRequestRegistry;
import controller.VehicularCloudController;
import model.Job;
import model.RequestType;
import model.ServerResponse;
import model.SubmissionRequest;
import model.Vehicle;
import scheduling.ScheduleEntry;
import scheduling.ScheduleResult;

// Controller screen. It can see every request and every saved job.
public class ControllerPanel extends JPanel {

    private final DefaultTableModel pendingRows = new DefaultTableModel(
            new String[] {"Request ID", "Type", "From", "Details"}, 0) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel jobRows = new DefaultTableModel(
            new String[] {"Job ID", "Client", "Email", "Priority", "Completion time"}, 0) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable pendingTable = new JTable(pendingRows);
    private final JLabel averageLabel = new JLabel("Average waiting time: —");

    public ControllerPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("VC Controller");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Select a request. Accept saves it. Reject does not.");
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setAlignmentX(LEFT_ALIGNMENT);
        titleBlock.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        titleBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
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
        JButton signOut = new JButton("Sign out");
        signOut.addActionListener(event -> MainFrame.signOutFromScreen());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        buttons.add(accept);
        buttons.add(reject);
        buttons.add(refresh);
        buttons.add(signOut);
        page.add(buttons);
        page.add(Box.createRigidArea(new Dimension(0, 16)));

        page.add(section("Waiting for review", pendingTable));
        page.add(Box.createRigidArea(new Dimension(0, 16)));
        page.add(section("All saved jobs", new JTable(jobRows)));
        averageLabel.setAlignmentX(LEFT_ALIGNMENT);
        averageLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        page.add(averageLabel);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        refresh();
    }

    // Accept or reject the selected waiting request.
    private void decide(boolean accept) {
        int row = pendingTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a request first.");
            return;
        }
        String requestId = String.valueOf(pendingTable.getValueAt(row, 0));
        PendingRequestRegistry.PendingRequest pending = ReviewQueue.get().take(requestId);
        if (pending == null) {
            JOptionPane.showMessageDialog(this, "That request is no longer waiting.");
            refresh();
            return;
        }
        ServerResponse response = VehicularCloudController.getInstance()
                .decideRequest(pending.getRequest(), accept);
        pending.complete(response);
        JOptionPane.showMessageDialog(this, response.getMessage());
        refresh();
    }

    public void refresh() {
        pendingRows.setRowCount(0);
        List<SubmissionRequest> waiting = ReviewQueue.get().list();
        for (int i = 0; i < waiting.size(); i++) {
            SubmissionRequest request = waiting.get(i);
            pendingRows.addRow(new Object[] {
                request.getRequestId(),
                request.getType().toString(),
                request.getRequesterEmail(),
                details(request)
            });
        }

        jobRows.setRowCount(0);
        ScheduleResult result = VehicularCloudController.getInstance().scheduleJobs();
        for (ScheduleEntry entry : result.getEntries()) {
            Job job = entry.getJob();
            jobRows.addRow(new Object[] {
                job.getJobId(),
                job.getClientId(),
                VehicularCloudController.getInstance().getJobAccountEmail(job.getJobId()),
                Integer.valueOf(job.getPriority()),
                Integer.valueOf(entry.getCompletionTime())
            });
        }
        averageLabel.setText("Average waiting time: " + result.getAverageWaitingTime());
    }

    private String details(SubmissionRequest request) {
        if (request.getType() == RequestType.VEHICLE) {
            Vehicle vehicle = request.getVehicle();
            return vehicle.getVehicleId() + " " + vehicle.getMake() + " " + vehicle.getModel();
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
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}
