package gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

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

    public OwnerPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        //put all the labels and fields in the middle
        JPanel fields = new JPanel(new GridBagLayout());
        addRow(fields, 0, "Vehicle owner ID / full name *", ownerId);
        addRow(fields, 1, "Make *", make);
        addRow(fields, 2, "Model *", model);
        addRow(fields, 3, "Year *", year);
        addRow(fields, 4, "Arrival (MM-dd-yyyy HH:mm) *", arrival);
        addRow(fields, 5, "Estimated departure (MM-dd-yyyy HH:mm) *", departure);
        addRow(fields, 6, "Compute power (units) *", computePower);
        add(fields, BorderLayout.CENTER);

        //button and hint go at the bottom
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.add(new JLabel("Times use your computer's local time. * Required"), BorderLayout.NORTH);
        JButton submit = new JButton("Register car");
        submit.addActionListener(event -> registerCar());
        footer.add(submit, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private static void addRow(JPanel panel, int row, String label, JTextField field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(4, 4, 4, 12);
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    private void registerCar() {
        //grab everything the user typed
        String owner = ownerId.getText().trim();
        String carMake = make.getText().trim();
        String carModel = model.getText().trim();
        String carYear = year.getText().trim();
        String arrivalText = arrival.getText().trim();
        String departureText = departure.getText().trim();
        String powerText = computePower.getText().trim();
        
        //make sure nothing is blank
        if (owner.isEmpty() || carMake.isEmpty() || carModel.isEmpty() || carYear.isEmpty()
                || arrivalText.isEmpty() || departureText.isEmpty() || powerText.isEmpty()) {
            showError("Complete every required vehicle field.");
            return;
        }

        try {
            //check the dates are real and make sense
            LocalDateTime arrivalTime = LocalDateTime.parse(arrivalText, TIME_FORMAT);
            LocalDateTime departureTime = LocalDateTime.parse(departureText, TIME_FORMAT);
            if (!departureTime.isAfter(arrivalTime) || !departureTime.isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Departure must be after arrival and in the future.");
            }
            //make a random id for this car
            String vehicleId = UUID.randomUUID().toString();
            SubmitStrategy submission = new OwnerSubmit(owner, vehicleId, carMake, carModel,
                    carYear, arrivalTime.format(TIME_FORMAT), departureTime.format(TIME_FORMAT), powerText);
            submission.submit();
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

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Could not register car", JOptionPane.ERROR_MESSAGE);
    }
}
