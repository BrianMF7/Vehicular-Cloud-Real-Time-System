package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import model.Job;
import model.RequestStatus;
import model.ServerResponse;
import model.SubmissionRequest;

/** Tests the queue of requests waiting for Accept or Reject. */
class PendingRequestRegistryTest {

    // Verifies that a new request is held until the controller reviews it.
    @Test
    void shouldHoldRequestsInArrivalOrder() {
        PendingRequestRegistry registry = new PendingRequestRegistry();
        SubmissionRequest first = jobRequest("job-1");
        SubmissionRequest second = jobRequest("job-2");

        registry.add(first);
        registry.add(second);

        List<SubmissionRequest> waiting = registry.list();
        assertEquals(2, waiting.size());
        assertEquals("job-1", waiting.get(0).getJob().getJobId());
        assertEquals("job-2", waiting.get(1).getJob().getJobId());
        assertSame(first, registry.get(first.getRequestId()).getRequest());
    }

    // Verifies that review removes the request from the waiting list.
    @Test
    void shouldRemoveRequestWhenTakenForReview() {
        PendingRequestRegistry registry = new PendingRequestRegistry();
        SubmissionRequest request = jobRequest("job-3");
        registry.add(request);

        PendingRequestRegistry.PendingRequest held = registry.take(request.getRequestId());

        assertEquals("job-3", held.getRequest().getJob().getJobId());
        assertTrue(registry.list().isEmpty());
        assertNull(registry.take(request.getRequestId()));
        assertNull(registry.get(request.getRequestId()));
    }

    // Verifies that the waiting list listener fires when requests arrive or leave.
    @Test
    void shouldNotifyListenerWhenTheQueueChanges() {
        PendingRequestRegistry registry = new PendingRequestRegistry();
        AtomicInteger changes = new AtomicInteger();
        registry.setOnChange(changes::incrementAndGet);

        SubmissionRequest request = jobRequest("job-4");
        registry.add(request);
        registry.take(request.getRequestId());

        assertEquals(2, changes.get());
    }

    // Verifies that a worker can wait for the Accept or Reject decision.
    @Test
    void shouldDeliverDecisionToWaitingWorker() throws Exception {
        PendingRequestRegistry registry = new PendingRequestRegistry();
        PendingRequestRegistry.PendingRequest held = registry.add(jobRequest("job-5"));
        ServerResponse accepted = new ServerResponse(held.getRequest().getRequestId(),
                RequestStatus.ACCEPTED, "Request accepted and stored.");

        // Complete the request from another thread so awaitDecision has to wait.
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(50);
                held.complete(accepted);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        worker.start();

        ServerResponse decision = held.awaitDecision();
        worker.join();

        assertEquals(RequestStatus.ACCEPTED, decision.getStatus());
        assertSame(accepted, decision);
    }

    // Verifies that missing values are refused before they enter the queue.
    @Test
    void shouldRejectMissingRequest() {
        PendingRequestRegistry registry = new PendingRequestRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.add(null));
        assertNull(registry.take(null));
        assertNull(registry.take("missing"));
        assertNull(registry.get("missing"));
    }

    // Sample job request used by the registry tests.
    private static SubmissionRequest jobRequest(String jobId) {
        return SubmissionRequest.forJob(
                new Job("client-1", jobId, 5, "11-20-2026", 2, "11-10-2026"),
                "Joshua", "joshua@example.com");
    }
}
