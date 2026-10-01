package gui;

import javax.swing.JPanel;

// Picks which form to show, and the tip text above it.
// Owner = renting out a car. Client = submitting a job.
public class PanelFactory {

    // Give back the right form. If the role is unknown, stop.
    public static JPanel create(String role) {
        if (role.equals("owner")) {
            return new OwnerPanel();
        }
        if (role.equals("client")) {
            return new ClientPanel();
        }
        throw new IllegalArgumentException("No screen for " + role);
    }

    // The short tip that sits above the form.
    public static String promptFor(String role) {
        if (role.equals("owner")) {
            return "Enter the car you want to rent out.";
        }
        if (role.equals("client")) {
            return "Enter the job you want to submit.";
        }
        return "Enter the information below.";
    }
}
