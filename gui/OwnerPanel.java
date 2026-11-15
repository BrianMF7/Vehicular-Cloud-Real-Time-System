package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.UUID;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import controller.ControllerClient;
import controller.PendingRequestRegistry;
import controller.VehicularCloudController;
import model.RequestStatus;
import model.RequestType;
import model.ServerResponse;
import model.SubmissionRequest;
import model.Vehicle;

public class OwnerPanel extends JPanel {
    //How we read and write dates
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("MM-dd-uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

    //All the text boxes for owner info
    private final JTextField ownerId = new JTextField(22);
     private final JTextField make = new JTextField(22);
    private final JTextField model = new JTextField(22);
    private final JTextField year = new JTextField(22);
    private final DateTimePicker arrival = new DateTimePicker();
    private final DateTimePicker departure = new DateTimePicker();
    private final JTextField computePower = new JTextField(22);
    private final ControllerClient client;
    private final PendingRequestRegistry reviewQueue;
    private final boolean requireLocalController;
    private final JButton submit = new JButton("Submit car for review");
    private final JLabel submissionStatus = new JLabel(" ");
    private String sendingRequestId;
    private final DefaultTableModel vehicleRows = new DefaultTableModel(
            new String[] {"Vehicle ID", "Owner", "Car model", "Current status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel tableMessage = new JLabel();
    private final Timer refreshTimer = new Timer(5000, event -> refreshVehicles());

    public OwnerPanel() {
        this(new ControllerClient(), ReviewQueue.get(), true);
    }

    OwnerPanel(ControllerClient client, PendingRequestRegistry reviewQueue) {
        this(client, reviewQueue, false);
    }

    private OwnerPanel(ControllerClient client, PendingRequestRegistry reviewQueue,
            boolean requireLocalController) {
        this.client = Objects.requireNonNull(client, "Controller client is required.");
        this.reviewQueue = Objects.requireNonNull(reviewQueue, "Review queue is required.");
        this.requireLocalController = requireLocalController;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel registration = new JPanel(new BorderLayout(0, 8));
        //Put all the labels and fields in the middle
        JPanel fields = FormRows.fields();
        FormRows.addRow(fields, 0, "Vehicle owner ID / full name *", ownerId);
        FormRows.addRow(fields, 1, "Make *", make);
        FormRows.addRow(fields, 2, "Model *", model);
           FormRows.addRow(fields, 3, "Year *", year);
        FormRows.addRow(fields, 4, "Arrival date and time *", arrival);
        FormRows.addRow(fields, 5, "Estimated departure date and time *", departure);
        FormRows.addRow(fields, 6, "Compute power (units) *", computePower);
        registration.add(fields, BorderLayout.CENTER);

        //Button and hint go at the bottom
        submit.addActionListener(event -> registerCar());
        JPanel submissionFooter = new JPanel(new BorderLayout(0, 4));
        submissionFooter.add(FormRows.footer("Choose a date and time using your computer's local time. * Required", submit), BorderLayout.NORTH);
        submissionFooter.add(submissionStatus, BorderLayout.SOUTH);
        registration.add(submissionFooter, BorderLayout.SOUTH);
        add(registration, BorderLayout.NORTH);

        JPanel listPanel = new JPanel(new BorderLayout(0, 8));
        listPanel.setBorder(BorderFactory.createTitledBorder("Vehicles and review requests"));
        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.add(new JLabel("Registered vehicles and submissions awaiting review"), BorderLayout.WEST);
        JButton refresh = new JButton("Refresh list");
        refresh.addActionListener(event -> refreshVehicles());
        listHeader.add(refresh, BorderLayout.EAST);
        listPanel.add(listHeader, BorderLayout.NORTH);

        JTable table = new JTable(vehicleRows);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setPreferredWidth(290);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.setPreferredScrollableViewportSize(new Dimension(650, 220));
        listPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        listPanel.add(tableMessage, BorderLayout.SOUTH);
        add(listPanel, BorderLayout.CENTER);
        refreshVehicles();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refreshVehicles();
        refreshTimer.start();
    }

    @Override
    public void removeNotify() {
        refreshTimer.stop();
        super.removeNotify();
    }

    private void registerCar() {
         //Grab everything the user typed
        String owner = ownerId.getText().trim();
           String carMake = make.getText().trim();
        String carModel = model.getText().trim();
           String carYear = year.getText().trim();
        LocalDateTime arrivalTime = arrival.getSelectedDateTime();
        LocalDateTime departureTime = departure.getSelectedDateTime();
        String powerText = computePower.getText().trim();
        
        //Make sure nothing is blank
            if (owner.isEmpty() || carMake.isEmpty() || carModel.isEmpty() || carYear.isEmpty()
                || arrivalTime == null || departureTime == null || powerText.isEmpty()) {
                 showError("Complete every required vehicle field.");
            return;
            }

        try {
            //Check the dates are real and make sense
               if (!departureTime.isAfter(arrivalTime) || !departureTime.isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Departure must be after arrival and in the future.");
               }
            int yearValue = Integer.parseInt(carYear);
            if (yearValue < 1995 || yearValue > 2027) {
                showError("Year must be between 1995 and 2027.");
                return;
            }
            double power = Double.parseDouble(powerText);
            if (!Double.isFinite(power) || power <= 0) {
                showError("Compute power must be a positive number.");
                return;
            }
            if (containsBreak(owner) || containsBreak(carMake) || containsBreak(carModel)) {
                showError("Vehicle fields cannot contain | or line breaks.");
                return;
            }
            if (requireLocalController && !ReviewQueue.isRunning()) {
                showError("Controller service is unavailable in this app. Close other running copies and restart Main.");
                return;
            }
            //Makes a random id for this car
            String vehicleId = UUID.randomUUID().toString();
            Vehicle vehicle = new Vehicle(vehicleId, carMake, carModel, yearValue,
                    arrivalTime.format(TIME_FORMAT), departureTime.format(TIME_FORMAT));
            String accountEmail = MainFrame.getCurrentEmail();
            SubmissionRequest request = SubmissionRequest.forVehicle(vehicle, owner, power,
                    MainFrame.getCurrentUser(), accountEmail);
            sendingRequestId = request.getRequestId();
            submit.setEnabled(false);
            submissionStatus.setText("Sending car to controller for review...");
            Thread submissionThread = new Thread(() -> {
                try {
                    ServerResponse decision = client.submit(request, acknowledgement ->
                            SwingUtilities.invokeLater(() -> {
                                submissionStatus.setText("ACKNOWLEDGED: " + acknowledgement.getMessage());
                                refreshVehicles();
                                if (request.getRequestId().equals(sendingRequestId)) {
                                    clearFields();
                                    sendingRequestId = null;
                                    submit.setEnabled(true);
                                }
                            }));
                    SwingUtilities.invokeLater(() -> {
                        RequestStatus status = decision.getStatus();
                        String title = status == RequestStatus.ACCEPTED ? "Car Accepted"
                                : status == RequestStatus.REJECTED ? "Car Rejected" : "Car Submission Error";
                        String message = "Vehicle ID: " + vehicleId + "\nStatus: " + status
                                + "\n\n" + decision.getMessage();
                        submissionStatus.setText(status + ": " + decision.getMessage());
                        refreshVehicles();
                        MainFrame.queueClientNotification(accountEmail, title, message);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        String message = "Could not communicate with controller: " + ex.getMessage();
                        submissionStatus.setText(message);
                        if (request.getRequestId().equals(sendingRequestId)) {
                            sendingRequestId = null;
                            submit.setEnabled(true);
                        }
                        MainFrame.queueClientNotification(accountEmail, "Car Submission Error", message);
                    });
                }
            }, "vehicle-submission");
            submissionThread.start();
        } catch (NumberFormatException ex) {
            showError("Year and compute power must be numbers.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void clearFields() {
        ownerId.setText("");
        make.setText("");
          model.setText("");
        year.setText("");
        arrival.clearSelection();
        departure.clearSelection();
        computePower.setText("");
       }

    private void refreshVehicles() {
        VehicularCloudController controller = VehicularCloudController.getInstance();
        List<Vehicle> vehicles = controller.getVehicles();
        String currentEmail = MainFrame.getCurrentEmail();
        LocalDateTime now = LocalDateTime.now();
        int registeredCount = 0;
        int pendingCount = 0;
        vehicleRows.setRowCount(0);
        for (int i = vehicles.size() - 1; i >= 0; i--) {
            Vehicle vehicle = vehicles.get(i);
            String savedEmail = controller.getVehicleAccountEmail(vehicle.getVehicleId());
            if (!currentEmail.isEmpty() && !currentEmail.equalsIgnoreCase(savedEmail)) {
                continue;
            }
            String carModel = vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getYear() + ")";
            vehicleRows.addRow(new Object[] {vehicle.getVehicleId(),
                    controller.getVehicleOwnerId(vehicle.getVehicleId()),
                    carModel, statusAt(vehicle, now)});
            registeredCount++;
        }
        for (SubmissionRequest request : reviewQueue.list()) {
            if (request.getType() != RequestType.VEHICLE
                    || (!currentEmail.isEmpty()
                    && !currentEmail.equalsIgnoreCase(request.getRequesterEmail()))) {
                continue;
            }
            Vehicle vehicle = request.getVehicle();
            String carModel = vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getYear() + ")";
            vehicleRows.addRow(new Object[] {vehicle.getVehicleId(), request.getOwnerId(),
                    carModel, "Pending review"});
            pendingCount++;
        }
        tableMessage.setText(registeredCount + " registered, " + pendingCount + " waiting for review.");
    }

    private boolean containsBreak(String value) {
        return value.contains("|") || value.contains("\n") || value.contains("\r");
    }

    //Same status labels the table used before, read from the controller's vehicle
    private String statusAt(Vehicle vehicle, LocalDateTime now) {
        try {
            LocalDateTime arrivalTime = LocalDateTime.parse(vehicle.getArrivalTime(), TIME_FORMAT);
            LocalDateTime departureTime = LocalDateTime.parse(vehicle.getDepartureTime(), TIME_FORMAT);
            if (!departureTime.isAfter(arrivalTime)) {
                return "Unknown";
            }
            if (now.isBefore(arrivalTime)) {
                return "Registered";
            }
            if (now.isBefore(departureTime)) {
                return "Available";
            }
            return "Unavailable";
        } catch (DateTimeParseException ex) {
            return "Unknown";
        }
    }

      private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Could not register car", JOptionPane.ERROR_MESSAGE);
    }
     }
