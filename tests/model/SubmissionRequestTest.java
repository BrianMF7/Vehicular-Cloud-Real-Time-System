package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

/** Tests the messages that will be sent between the forms and Controller server. */
class SubmissionRequestTest {

    // Verifies that a job request keeps the job and signed-in client information together.
    @Test
    void shouldCreateJobRequest() {
        Job job = new Job("client-1", "job-1", 5, "11-20-2026", 2, "11-10-2026");

        SubmissionRequest request = SubmissionRequest.forJob(job, "Sajid", "sajid@example.com");

        assertEquals(RequestType.JOB, request.getType());
        assertEquals(job, request.getJob());
        assertNull(request.getVehicle());
        assertEquals("Sajid", request.getRequesterName());
        assertEquals("sajid@example.com", request.getRequesterEmail());
        assertFalse(request.getRequestId().isEmpty());
    }

    // Verifies that a vehicle request also keeps the owner and compute information needed on approval.
    @Test
    void shouldCreateVehicleRequest() {
        Vehicle vehicle = new Vehicle("vehicle-1", "Toyota", "Camry", 2020,
                "11-10-2026 09:00", "11-10-2026 17:00");

        SubmissionRequest request = SubmissionRequest.forVehicle(vehicle, "owner-1", 12.5,
                "Sajid", "sajid@example.com");

        assertEquals(RequestType.VEHICLE, request.getType());
        assertEquals(vehicle, request.getVehicle());
        assertNull(request.getJob());
        assertEquals("owner-1", request.getOwnerId());
        assertEquals(12.5, request.getComputePower(), 0.000001);
    }

    // Verifies that each request gets its own ID, so the server can send the response to the right user.
    @Test
    void shouldGiveEachRequestAUniqueId() {
        Job job = new Job("client-1", "job-1", 5, "11-20-2026", 2);

        SubmissionRequest first = SubmissionRequest.forJob(job, "", "");
        SubmissionRequest second = SubmissionRequest.forJob(job, "", "");

        assertNotEquals(first.getRequestId(), second.getRequestId());
    }

    // Verifies that the first server response is a pending acknowledgement, not a final decision.
    @Test
    void shouldCreatePendingAcknowledgement() {
        ServerResponse response = ServerResponse.received("request-1");

        assertEquals(RequestStatus.PENDING, response.getStatus());
        assertFalse(response.isFinal());
        assertTrue(response.getMessage().contains("received"));
    }

    // Verifies which response states are final server decisions.
    @Test
    void shouldIdentifyFinalResponseStates() {
        assertFalse(RequestStatus.PENDING.isFinal());
        assertTrue(RequestStatus.ACCEPTED.isFinal());
        assertTrue(RequestStatus.REJECTED.isFinal());
        assertTrue(RequestStatus.ERROR.isFinal());
    }

    // Verifies that a request can travel through an ObjectOutputStream for the socket-based design.
    @Test
    void shouldSerializeJobRequest() throws Exception {
        SubmissionRequest original = SubmissionRequest.forJob(
                new Job("client-1", "job-1", 5, "11-20-2026", 2), "Sajid", "sajid@example.com");

        SubmissionRequest restored = roundTrip(original);

        assertEquals(original.getRequestId(), restored.getRequestId());
        assertEquals("job-1", restored.getJob().getJobId());
        assertEquals(2, restored.getJob().getPriority());
    }

    // Verifies that malformed requests and responses are refused before they reach the server.
    @Test
    void shouldRejectMissingRequiredValues() {
        assertThrows(IllegalArgumentException.class,
                () -> SubmissionRequest.forJob(null, "Sajid", "sajid@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> SubmissionRequest.forVehicle(null, "owner-1", 10, "Sajid", "sajid@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> new ServerResponse(" ", RequestStatus.PENDING, "Received"));
        assertThrows(IllegalArgumentException.class,
                () -> new ServerResponse("request-1", null, "Received"));
    }

    // Verifies that invalid numeric values cannot be sent to the controller as compute power.
    @Test
    void shouldRejectInvalidComputePower() {
        Vehicle vehicle = new Vehicle("vehicle-1", "Toyota", "Camry", 2020,
                "11-10-2026 09:00", "11-10-2026 17:00");

        assertThrows(IllegalArgumentException.class,
                () -> SubmissionRequest.forVehicle(vehicle, "owner-1", -1, "Sajid", "sajid@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> SubmissionRequest.forVehicle(vehicle, "owner-1", Double.NaN,
                        "Sajid", "sajid@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> SubmissionRequest.forVehicle(vehicle, "owner-1", Double.POSITIVE_INFINITY,
                        "Sajid", "sajid@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> SubmissionRequest.forVehicle(vehicle, "owner-1", Double.NEGATIVE_INFINITY,
                        "Sajid", "sajid@example.com"));
    }

    private SubmissionRequest roundTrip(SubmissionRequest request) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(request);
        }
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (SubmissionRequest) input.readObject();
        }
    }
}
