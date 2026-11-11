package gui;

import controller.PendingRequestRegistry;

// One waiting list shared by the job form and the controller screen.
public class ReviewQueue {

    private static final PendingRequestRegistry registry = new PendingRequestRegistry();

    private ReviewQueue() {
    }

    public static PendingRequestRegistry get() {
        return registry;
    }
}
