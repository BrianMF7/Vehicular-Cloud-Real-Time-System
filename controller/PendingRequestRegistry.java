package controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.ServerResponse;
import model.SubmissionRequest;

// Holds job and vehicle requests until the VC Controller clicks Accept or Reject.
public class PendingRequestRegistry {
    // Keep arrival order so the review screen shows the oldest request first.
    private final Map<String, PendingRequest> waiting = new LinkedHashMap<String, PendingRequest>();
    // Optional listener, used by the review screen to refresh when the queue changes.
    private Runnable onChange;

    // Put a new request in the queue and tell the waiting client worker about it.
    public PendingRequest add(SubmissionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required.");
        }
        PendingRequest pending = new PendingRequest(request);
        Runnable listener;
        // Copy the listener while the map is locked, then call it after.
        synchronized (this) {
            waiting.put(request.getRequestId(), pending);
            listener = onChange;
        }
        notifyChange(listener);
        return pending;
    }

    // Remove a request so Accept or Reject can handle it. Returns null if it is gone.
    public PendingRequest take(String requestId) {
        if (requestId == null) {
            return null;
        }
        PendingRequest pending;
        Runnable listener;
        synchronized (this) {
            pending = waiting.remove(requestId);
            listener = pending == null ? null : onChange;
        }
        notifyChange(listener);
        return pending;
    }

    // Look up a waiting request without removing it.
    public synchronized PendingRequest get(String requestId) {
        if (requestId == null) {
            return null;
        }
        return waiting.get(requestId);
    }

    // Copy of the waiting requests so the review screen can draw the list.
    public synchronized List<SubmissionRequest> list() {
        List<SubmissionRequest> requests = new ArrayList<SubmissionRequest>();
        for (PendingRequest pending : waiting.values()) {
            requests.add(pending.getRequest());
        }
        return requests;
    }

    // The review screen uses this to redraw when a request arrives or leaves.
    public synchronized void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    // Run the listener outside the lock so the screen can call list() safely.
    private static void notifyChange(Runnable listener) {
        if (listener != null) {
            listener.run();
        }
    }

    // One request waiting for a controller decision.
    public static final class PendingRequest {
        private final SubmissionRequest request;
        // Null until Accept or Reject fills it in.
        private ServerResponse decision;

        private PendingRequest(SubmissionRequest request) {
            this.request = request;
        }

        // The original job or vehicle message from the client.
        public SubmissionRequest getRequest() {
            return request;
        }

        // Store the final response and wake the worker thread that is waiting on this request.
        public synchronized void complete(ServerResponse decision) {
            this.decision = decision;
            notifyAll();
        }

        // Block until Accept or Reject sets the final response.
        public synchronized ServerResponse awaitDecision() throws InterruptedException {
            while (decision == null) {
                wait();
            }
            return decision;
        }
    }
}
