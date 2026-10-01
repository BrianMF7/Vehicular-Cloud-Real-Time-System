package gui;

// Simple rule for "what happens after someone tries to save."
// The main window follows this rule.
public interface SaveListener {
    // Save worked.
    void saved();

    // Save failed. message is what we show the user.
    void failed(String message);
}
