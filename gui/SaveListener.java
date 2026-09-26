package gui;

public interface SaveListener {
    void saved();

    void failed(String message);
}
