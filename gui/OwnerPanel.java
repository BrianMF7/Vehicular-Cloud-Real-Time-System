package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.UUID;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import storage.FileManager;

public class OwnerPanel extends JPanel {
    //How we read and write dates
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("MM-dd-uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

    //All the text boxes for owner info
    private final JTextField ownerId = new JTextField(22);
     private final JTextField make = new JTextField(22);
    private final JTextField model = new JTextField(22);
    private final JTextField year = new JTextField(22);
      private final JTextField arrival = new JTextField(22);
      private final JTextField departure = new JTextField(22);
    private final JTextField computePower = new JTextField(22);
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
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel registration = new JPanel(new BorderLayout(0, 8));
        //Put all the labels and fields in the middle
        JPanel fields = FormRows.fields();
        FormRows.addRow(fields, 0, "Vehicle owner ID / full name *", ownerId);
        FormRows.addRow(fields, 1, "Make *", make);
        FormRows.addRow(fields, 2, "Model *", model);
           FormRows.addRow(fields, 3, "Year *", year);
        FormRows.addRow(fields, 4, "Arrival (MM-dd-yyyy HH:mm) *", arrival);
           FormRows.addRow(fields, 5, "Estimated departure (MM-dd-yyyy HH:mm) *", departure);
        FormRows.addRow(fields, 6, "Compute power (units) *", computePower);
        registration.add(fields, BorderLayout.CENTER);

        //Button and hint go at the bottom
        JButton submit = new JButton("Register car");
           submit.addActionListener(event -> registerCar());
           registration.add(FormRows.footer("Times use your computer's local time. * Required", submit), BorderLayout.SOUTH);
        add(registration, BorderLayout.NORTH);

        JPanel listPanel = new JPanel(new BorderLayout(0, 8));
        listPanel.setBorder(BorderFactory.createTitledBorder("Registered vehicles"));
        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.add(new JLabel("Vehicles saved in the system"), BorderLayout.WEST);
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
        String arrivalText = arrival.getText().trim();
         String departureText = departure.getText().trim();
        String powerText = computePower.getText().trim();
        
        //Make sure nothing is blank
            if (owner.isEmpty() || carMake.isEmpty() || carModel.isEmpty() || carYear.isEmpty()
                || arrivalText.isEmpty() || departureText.isEmpty() || powerText.isEmpty()) {
                 showError("Complete every required vehicle field.");
            return;
            }

        try {
            //Check the dates are real and make sense
            LocalDateTime arrivalTime = LocalDateTime.parse(arrivalText, TIME_FORMAT);
             LocalDateTime departureTime = LocalDateTime.parse(departureText, TIME_FORMAT);
               if (!departureTime.isAfter(arrivalTime) || !departureTime.isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Departure must be after arrival and in the future.");
               }
            //Makes a random id for this car
                String vehicleId = UUID.randomUUID().toString();
            SubmitStrategy submission = new OwnerSubmit(owner, vehicleId, carMake, carModel,
                    carYear, arrivalTime.format(TIME_FORMAT), departureTime.format(TIME_FORMAT), powerText);
             submission.submit();
            refreshVehicles();
              JOptionPane.showMessageDialog(this, "Car registered. Vehicle ID: " + vehicleId);
            clearFields();
        } catch (DateTimeParseException ex) {
            showError("Use MM-dd-yyyy HH:mm for arrival and departure, for example 10-01-2026 09:30.");
          } catch (Exception ex) {
               showError(ex.getMessage());
        }
        }

    private void clearFields() {
        ownerId.setText("");
        make.setText("");
          model.setText("");
        year.setText("");
              arrival.setText("");
        departure.setText("");
        computePower.setText("");
       }

    private void refreshVehicles() {
        try {
            List<FileManager.VehicleEntry> vehicles = FileManager.getInstance().readVehicles();
            LocalDateTime now = LocalDateTime.now();
            String currentEmail = MainFrame.getCurrentEmail();
            int visibleCount = 0;
            vehicleRows.setRowCount(0);
            for (int i = vehicles.size() - 1; i >= 0; i--) {
                FileManager.VehicleEntry vehicle = vehicles.get(i);
                if (!currentEmail.isEmpty() && !vehicle.belongsToAccount(currentEmail)) {
                    continue;
                }
                vehicleRows.addRow(new Object[] {vehicle.getVehicleId(), vehicle.getOwnerId(),
                        vehicle.getCarModel(), vehicle.getStatusAt(now)});
                visibleCount++;
            }
            tableMessage.setText(visibleCount + " vehicle(s). Status reflects arrival/departure times.");
        } catch (Exception ex) {
            tableMessage.setText("Could not refresh vehicles: " + ex.getMessage());
        }
    }

      private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Could not register car", JOptionPane.ERROR_MESSAGE);
    }
     }
