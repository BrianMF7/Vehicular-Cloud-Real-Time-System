
package controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.ServerResponse;
import model.SubmissionRequest;

// Holds job requests until the VC Controller clicks Accept or Reject.
public class PendingRequestRegistry {

    // Keep arrival order so the review screen shows the oldest request first.
    private final Map<String, PendingRequest> waiting =
            new LinkedHashMap<String, PendingRequest>();

    // Optional listener, used by the review screen to refresh when the queue changes.
    private Runnable onChange;

    // Put a new request in the queue.
    public PendingRequest add(SubmissionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required.");
        }

        PendingRequest pending = new PendingRequest(request);
        Runnable listener;

        synchronized (this) {
            waiting.put(request.getRequestId(), pending);
            listener = onChange;
        }

        notifyChange(listener);
        return pending;
    }

    // Remove a request so Accept or Reject can handle it.
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

    // Cancel a request only if it is still waiting for a decision.
    public boolean cancel(String requestId, PendingRequest expected) {
        if (requestId == null || expected == null) {
            return false;
        }

        Runnable listener;

        synchronized (this) {
            if (waiting.get(requestId) != expected) {
                return false;
            }

            waiting.remove(requestId);

            // Wake the server worker if it is waiting for a decision.
            expected.cancel();

            listener = onChange;
        }

        notifyChange(listener);
        return true;
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
        List<SubmissionRequest> requests =
                new ArrayList<SubmissionRequest>();

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

    // One job request waiting for a controller decision.
    public static final class PendingRequest {

        private final SubmissionRequest request;

        // Null until Accept or Reject fills it in.
        private ServerResponse decision;

        // True if the client disconnects before a decision.
        private boolean cancelled;

        private PendingRequest(SubmissionRequest request) {
            this.request = request;
        }

        // Cancel the request and wake any thread waiting for a decision.
        private synchronized void cancel() {
            cancelled = true;
            notifyAll();
        }

        // The original job message from the client.
        public SubmissionRequest getRequest() {
            return request;
        }

        // Store the final response and wake the worker thread.
        public synchronized void complete(ServerResponse decision) {
            this.decision = decision;
            notifyAll();
        }

        // Block until Accept/Reject or cancellation.
        public synchronized ServerResponse awaitDecision()
                throws InterruptedException {

            while (decision == null && !cancelled) {
                wait();
            }

            return decision;
        }
    }
}
