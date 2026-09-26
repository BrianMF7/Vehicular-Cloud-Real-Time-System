package gui;

import javax.swing.JPanel;

public class PanelFactory {

    public static JPanel create(String role) {
        if (role.equals("owner")) {
            return new OwnerPanel();
        }
        if (role.equals("client")) {
            return new ClientPanel();
        }
        throw new IllegalArgumentException("No screen for " + role);
    }

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
