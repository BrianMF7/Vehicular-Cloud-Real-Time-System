import gui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    // This is where the program starts. It opens the main window.
    public static void main(String[] args) {
        // Try to make the buttons and menus look like normal apps on this computer.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // No big deal if that fails. The window still opens.
            System.err.println("Could not use the system look and feel.");
        }

        // Open the window when Java is ready to draw it.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            }
        });
    }
}
