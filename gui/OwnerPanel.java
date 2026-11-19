package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import controller.ControllerClient;
import controller.PendingRequestRegistry;
import controller.VehicularCloudController;
import model.RequestStatus;
import model.RequestType;
import model.ServerResponse;
import model.SubmissionRequest;
import model.Vehicle;
import model.VehicleCatalog;

public class OwnerPanel extends JPanel {
    // How we read and write dates.
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("MM-dd-uuuu HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    // All the text boxes for owner information.
    private final JTextField ownerId = new JTextField(22);
    private final JComboBox<String> make = new JComboBox<>();
    private final JComboBox<String> model = new JComboBox<>();
    private final JComboBox<String> year = new JComboBox<>();
    private final DateTimePicker arrival = new DateTimePicker();
    private final DateTimePicker departure = new DateTimePicker();
    private final JTextField computePower = new JTextField(22);

    private final ControllerClient client;
    private final PendingRequestRegistry reviewQueue;

    private final JButton submit = new JButton("Submit car for review");
    private final JLabel submissionStatus = new JLabel(" ");

    private String sendingRequestId;
    private boolean updatingVehicleChoices;
    private boolean filteringVehicleChoices;

    private final DefaultTableModel vehicleRows = new DefaultTableModel(
            new String[] {"Vehicle ID", "Owner ID", "Car model", "Current status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JLabel tableMessage = new JLabel();
    private final Timer refreshTimer =
            new Timer(5000, event -> refreshVehicles());

    public OwnerPanel() {
        this(new ControllerClient(), ReviewQueue.get());
    }

    OwnerPanel(ControllerClient client, PendingRequestRegistry reviewQueue) {
        this.client = Objects.requireNonNull(
                client, "Controller client is required.");
        this.reviewQueue = Objects.requireNonNull(
                reviewQueue, "Review queue is required.");

        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        setUpVehicleChoices();

        JPanel registration = new JPanel(new BorderLayout(0, 8));

        // Put all the labels and fields in the middle.
        JPanel fields = FormRows.fields();

        FormRows.addRow(fields, 0, "Vehicle owner ID *", ownerId);
        FormRows.addRow(fields, 1, "Make *", make);
        FormRows.addRow(fields, 2, "Model *", model);
        FormRows.addRow(fields, 3, "Model year *", year);
        FormRows.addRow(fields, 4, "Arrival date and time *", arrival);
        FormRows.addRow(fields, 5, "Estimated departure date and time *", departure);
        FormRows.addRow(fields, 6, "Compute power (units) *", computePower);

        registration.add(fields, BorderLayout.CENTER);

        // Button and hint go at the bottom.
        submit.addActionListener(event -> registerCar());

        JPanel submissionFooter = new JPanel(new BorderLayout(0, 4));

        submissionFooter.add(
                FormRows.footer(
                        "Choose a date and time using your computer's local time. * Required",
                        submit),
                BorderLayout.NORTH);

        submissionFooter.add(submissionStatus, BorderLayout.SOUTH);
        registration.add(submissionFooter, BorderLayout.SOUTH);

        add(registration, BorderLayout.NORTH);

        JPanel listPanel = new JPanel(new BorderLayout(0, 8));
        listPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Vehicles and review requests"));

        JPanel listHeader = new JPanel(new BorderLayout());

        listHeader.add(
                new JLabel("Registered vehicles and submissions awaiting review"),
                BorderLayout.WEST);

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

        table.setPreferredScrollableViewportSize(
                new Dimension(650, 220));

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

        // Grab everything the user entered.
        String owner = ownerId.getText().trim();
        String carMake = comboText(make);
        String carModel = comboText(model);
        String carYear = comboText(year);

        LocalDateTime arrivalTime = arrival.getSelectedDateTime();
        LocalDateTime departureTime = departure.getSelectedDateTime();

        String powerText = computePower.getText().trim();

        // Make sure nothing is blank.
        if (owner.isEmpty()
                || carMake.isEmpty()
                || carModel.isEmpty()
                || carYear.isEmpty()
                || arrivalTime == null
                || departureTime == null
                || powerText.isEmpty()) {

            showError("Complete every required vehicle field.");
            return;
        }

        try {

            // Check the dates are real and make sense.
            if (!departureTime.isAfter(arrivalTime)
                    || !departureTime.isAfter(LocalDateTime.now())) {

                throw new IllegalArgumentException(
                        "Departure must be after arrival and in the future.");
            }

            int yearValue = Integer.parseInt(carYear);

            if (yearValue < VehicleCatalog.MIN_MODEL_YEAR || yearValue > VehicleCatalog.MAX_MODEL_YEAR) {
                showError("Year must be between " + VehicleCatalog.MIN_MODEL_YEAR
                        + " and " + VehicleCatalog.MAX_MODEL_YEAR + ".");
                return;
            }

            double power = Double.parseDouble(powerText);

            if (!Double.isFinite(power) || power <= 0) {
                showError("Compute power must be a positive number.");
                return;
            }

            if (containsBreak(owner)
                    || containsBreak(carMake)
                    || containsBreak(carModel)) {

                showError("Vehicle fields cannot contain | or line breaks.");
                return;
            }

            // Make a random ID for this car.
            String vehicleId = UUID.randomUUID().toString();

            Vehicle vehicle = new Vehicle(
                    vehicleId,
                    carMake,
                    carModel,
                    yearValue,
                    arrivalTime.format(TIME_FORMAT),
                    departureTime.format(TIME_FORMAT));

            String accountEmail = MainFrame.getCurrentEmail();

            SubmissionRequest request = SubmissionRequest.forVehicle(
                    vehicle,
                    owner,
                    power,
                    MainFrame.getCurrentUser(),
                    accountEmail);

            sendingRequestId = request.getRequestId();

            submit.setEnabled(false);
            submissionStatus.setText(
                    "Sending car to controller for review...");

            Thread submissionThread = new Thread(() -> {

                try {

                    ServerResponse decision = client.submit(
                            request,
                            acknowledgement ->
                                    SwingUtilities.invokeLater(() -> {

                                        String acknowledgementMessage =
                                                "ACKNOWLEDGED: "
                                                        + acknowledgement.getMessage();

                                        submissionStatus.setText(
                                                acknowledgementMessage);

                                        refreshVehicles();

                                        // Display the immediate acknowledgement popup.
                                        JOptionPane.showMessageDialog(
                                                this,
                                                ownerSubmissionDetails(
                                                        owner,
                                                        vehicle,
                                                        power,
                                                        "PENDING")
                                                        + "\n\n"
                                                        + acknowledgementMessage,
                                                "Vehicle Submission Acknowledged",
                                                JOptionPane.INFORMATION_MESSAGE
                                        );
                                    }));

                    SwingUtilities.invokeLater(() -> {

                        RequestStatus status = decision.getStatus();

                        String title =
                                status == RequestStatus.ACCEPTED
                                        ? "Car Accepted"
                                        : status == RequestStatus.REJECTED
                                        ? "Car Rejected"
                                        : "Car Submission Error";

                        String message = ownerSubmissionDetails(
                                owner,
                                vehicle,
                                power,
                                status.toString())
                                + "\n\n"
                                + decision.getMessage();

                        submissionStatus.setText(
                                status + ": " + decision.getMessage());

                        refreshVehicles();

                        // Clear the form only after acceptance.
                        if (status == RequestStatus.ACCEPTED) {
                            clearFields();
                        }

                        if (request.getRequestId().equals(sendingRequestId)) {
                            sendingRequestId = null;
                            submit.setEnabled(true);
                        }

                        // Send the final decision notification to the owner.
                        MainFrame.queueClientNotification(
                                accountEmail,
                                title,
                                message);
                    });

                } catch (Exception ex) {

                    SwingUtilities.invokeLater(() -> {

                        String message =
                                "Could not communicate with controller: "
                                        + ex.getMessage();

                        submissionStatus.setText(message);

                        if (request.getRequestId().equals(sendingRequestId)) {
                            sendingRequestId = null;
                            submit.setEnabled(true);
                        }

                        // Display an immediate connection-error popup.
                        JOptionPane.showMessageDialog(
                                this,
                                message,
                                "Connection Error",
                                JOptionPane.ERROR_MESSAGE
                        );
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

    private String ownerSubmissionDetails(
            String owner,
            Vehicle vehicle,
            double power,
            String status) {

        return "Owner ID: " + owner
                + "\nVehicle ID: " + vehicle.getVehicleId()
                + "\nVehicle: " + vehicle.getYear()
                + " " + vehicle.getMake()
                + " " + vehicle.getModel()
                + "\nArrival: " + vehicle.getArrivalTime()
                + "\nEstimated departure: " + vehicle.getDepartureTime()
                + "\nCompute power: " + power
                + "\nStatus: " + status;
    }

    private void clearFields() {
        ownerId.setText("");
        clearCombo(make);
        clearCombo(model);
        clearCombo(year);
        arrival.clearSelection();
        departure.clearSelection();
        computePower.setText("");
    }

    // Populate the first dropdown and keep all three fields editable for unlisted vehicles.
    private void setUpVehicleChoices() {
        configureEditableCombo(make, "Choose or type a make");
        configureEditableCombo(model, "Choose or type a model");
        configureEditableCombo(year, "Choose or type a year");

        for (String makeName : VehicleCatalog.getMakes()) {
            make.addItem(makeName);
        }
        make.setSelectedIndex(-1);

        make.addActionListener(event -> refreshModelChoices());
        model.addActionListener(event -> refreshYearChoices());

        installLiveFilter(make, VehicleCatalog::getMakes, this::refreshModelChoices);
        installLiveFilter(model, () -> VehicleCatalog.getModels(comboText(make)),
                this::refreshYearChoices);
        installLiveFilter(year, () -> VehicleCatalog.getYears(comboText(make), comboText(model)),
                null);
    }

    // A selected make determines the models shown in the next dropdown.
    private void refreshModelChoices() {
        if (updatingVehicleChoices || filteringVehicleChoices) {
            return;
        }
        updatingVehicleChoices = true;
        replaceChoices(model, VehicleCatalog.getModels(comboText(make)));
        replaceChoices(year, java.util.Collections.<String>emptyList());
        updatingVehicleChoices = false;
    }

    // A selected make and model determine the available model-year suggestions.
    private void refreshYearChoices() {
        if (updatingVehicleChoices || filteringVehicleChoices) {
            return;
        }
        updatingVehicleChoices = true;
        replaceChoices(year, VehicleCatalog.getYears(comboText(make), comboText(model)));
        updatingVehicleChoices = false;
    }

    private static void configureEditableCombo(JComboBox<String> combo, String toolTip) {
        combo.setEditable(true);
        combo.setSelectedIndex(-1);
        combo.setToolTipText(toolTip);
    }

    // Filters an open dropdown as the user types, like a car-shopping website search box.
    private void installLiveFilter(JComboBox<String> combo, Supplier<List<String>> choiceSource,
            Runnable exactMatchAction) {
        JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
        Runnable filterFromEditor = () -> filterChoices(
                combo, choiceSource.get(), exactMatchAction, editor.getText());
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                queueFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                queueFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                queueFilter();
            }

            private void queueFilter() {
                if (filteringVehicleChoices) {
                    return;
                }
                SwingUtilities.invokeLater(filterFromEditor);
            }
        });

        // On macOS, an editable combo box may keep its unfiltered popup open while typing.
        // Filtering on key release updates that popup immediately; the document listener also
        // covers pasted text and other non-keyboard edits.
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                if (!filteringVehicleChoices) {
                    filterFromEditor.run();
                }
            }
        });
    }

    private void filterChoices(JComboBox<String> combo, List<String> choices,
            Runnable exactMatchAction, String typedText) {
        if (filteringVehicleChoices) {
            return;
        }
        List<String> matches = matchingChoices(choices, typedText);

        filteringVehicleChoices = true;
        combo.removeAllItems();
        for (String match : matches) {
            combo.addItem(match);
        }
        combo.setSelectedIndex(-1);
        combo.getEditor().setItem(typedText);
        filteringVehicleChoices = false;

        if (exactMatchAction != null && containsIgnoreCase(matches, typedText)) {
            exactMatchAction.run();
        }
        if (!typedText.isEmpty() && !matches.isEmpty() && combo.isShowing()) {
            SwingUtilities.invokeLater(combo::showPopup);
        } else if (matches.isEmpty()) {
            combo.hidePopup();
        }
    }

    // Package-visible for the focused UI test. Matching ignores letter case and starts at the first letter.
    static List<String> matchingChoices(List<String> choices, String typedText) {
        String prefix = typedText == null ? "" : typedText.trim().toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String choice : choices) {
            if (prefix.isEmpty() || choice.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(choice);
            }
        }
        return matches;
    }

    private static boolean containsIgnoreCase(List<String> choices, String value) {
        for (String choice : choices) {
            if (choice.equalsIgnoreCase(value == null ? "" : value.trim())) {
                return true;
            }
        }
        return false;
    }

    private static void replaceChoices(JComboBox<String> combo, List<String> choices) {
        combo.removeAllItems();
        for (String choice : choices) {
            combo.addItem(choice);
        }
        clearCombo(combo);
    }

    private static String comboText(JComboBox<String> combo) {
        Object value = combo.getEditor().getItem();
        return value == null ? "" : value.toString().trim();
    }

    private static void clearCombo(JComboBox<String> combo) {
        combo.setSelectedIndex(-1);
        combo.getEditor().setItem("");
    }


    private void refreshVehicles() {

        VehicularCloudController controller =
                VehicularCloudController.getInstance();

        // Reload saved records so this process sees vehicles
        // accepted by the controller in a separate process.
        controller.refreshSavedData();

        String currentEmail = MainFrame.getCurrentEmail();
        LocalDateTime now = LocalDateTime.now();

        int registeredCount = 0;
        int pendingCount = 0;

        vehicleRows.setRowCount(0);

        // Only display vehicles when an account is signed in.
        if (currentEmail != null && !currentEmail.trim().isEmpty()) {

            List<Vehicle> vehicles = controller.getVehicles();

            for (int i = vehicles.size() - 1; i >= 0; i--) {

                Vehicle vehicle = vehicles.get(i);

                String savedEmail =
                        controller.getVehicleAccountEmail(
                                vehicle.getVehicleId());

                // Only show vehicles belonging to this account.
                if (savedEmail == null
                        || !currentEmail.trim().equalsIgnoreCase(
                        savedEmail.trim())) {
                    continue;
                }

                String carModel = vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getYear() + ")";
                vehicleRows.addRow(new Object[] {
                        vehicle.getVehicleId(),
                        controller.getVehicleOwnerId(
                                vehicle.getVehicleId()),
                        carModel,
                        statusAt(vehicle, now)
                });
                registeredCount++;
            }
            // Display this owner's submissions still awaiting review.
            for (SubmissionRequest request : reviewQueue.list()) {
                if (request.getType() != RequestType.VEHICLE
                        || request.getRequesterEmail() == null
                        || !currentEmail.trim().equalsIgnoreCase(
                        request.getRequesterEmail().trim())) {
                    continue;
                }
                Vehicle vehicle = request.getVehicle();
                String carModel = vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getYear() + ")";
                vehicleRows.addRow(new Object[] {
                        vehicle.getVehicleId(),
                        request.getOwnerId(),
                        carModel,
                        "Pending review"
                });
                pendingCount++;
            }
        }
        tableMessage.setText(registeredCount + " registered, " + pendingCount + " waiting for review.");
    }


    private boolean containsBreak(String value) {
        return value.contains("|")
                || value.contains("\n")
                || value.contains("\r");
    }

    // Same status labels the table used before.
    private String statusAt(Vehicle vehicle, LocalDateTime now) {

        try {

            LocalDateTime arrivalTime = LocalDateTime.parse(
                    vehicle.getArrivalTime(),
                    TIME_FORMAT);

            LocalDateTime departureTime = LocalDateTime.parse(
                    vehicle.getDepartureTime(),
                    TIME_FORMAT);

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
        JOptionPane.showMessageDialog(
                this,
                message,
                "Could not register car",
                JOptionPane.ERROR_MESSAGE);
    }
}
