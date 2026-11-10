package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import model.Job;
import model.RequestStatus;
import model.ServerResponse;
import model.SubmissionRequest;
import model.Vehicle;

/** Tests the VC Controller server: acknowledgement, Accept/Reject, and file storage. */
@Tag("integration")
class ControllerServerTest {

    private static final Path LOG = Paths.get("vehicular_cloud_log.txt");
    private static final Path LOG_BACKUP = Paths.get("vehicular_cloud_log.txt.servertestbak");

    private ControllerServer server;
    private PendingRequestRegistry registry;
    private VehicularCloudController controller;

    // Move the real log aside so these tests do not overwrite it.
    @BeforeAll
    static void stashExistingLog() throws IOException {
        Files.deleteIfExists(LOG_BACKUP);
        if (Files.exists(LOG)) {
            Files.move(LOG, LOG_BACKUP, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // Start a server on a free port with an empty log for each test.
    @BeforeEach
    void startServer() throws IOException {
        Files.deleteIfExists(LOG);
        registry = new PendingRequestRegistry();
        controller = new VehicularCloudController();
        server = new ControllerServer(0, registry, controller);
        server.start();
    }

    // Close the socket and delete the test log so the next test starts clean.
    @AfterEach
    void stopServer() throws IOException {
        if (server != null) {
            server.stop();
            server = null;
        }
        Files.deleteIfExists(LOG);
    }

    // Put the real log back after the suite finishes.
    @AfterAll
    static void restoreStashedLog() throws IOException {
        Files.deleteIfExists(LOG);
        if (Files.exists(LOG_BACKUP)) {
            Files.move(LOG_BACKUP, LOG, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // Verifies that the server acknowledges a request before any Accept or Reject click.
    @Test
    void shouldAcknowledgeRequestAndKeepItPending() throws Exception {
        SubmissionRequest request = jobRequest("job-ack");
        try (TestClient client = connect()) {
            ServerResponse ack = client.send(request);

            assertEquals(request.getRequestId(), ack.getRequestId());
            assertEquals(RequestStatus.PENDING, ack.getStatus());
            assertFalse(ack.isFinal());
            assertEquals(1, registry.list().size());
            assertEquals(0, controller.getJobs().size());
        }
    }

    // Verifies that Accept stores the job, including start/end times, then tells the client.
    @Test
    void shouldStoreJobWhenAccepted() throws Exception {
        SubmissionRequest request = jobRequest("job-accept");
        try (TestClient client = connect()) {
            client.send(request);
            ServerResponse decision = server.decide(request.getRequestId(), true);
            ServerResponse notified = client.readDecision();

            assertEquals(RequestStatus.ACCEPTED, decision.getStatus());
            assertEquals(RequestStatus.ACCEPTED, notified.getStatus());
            assertEquals(1, controller.getJobs().size());
            assertJobTimes(controller.getJobs().get(0), "job-accept");
            assertEquals("Joshua", controller.getJobAccountName("job-accept"));
            assertEquals("joshua@example.com", controller.getJobAccountEmail("job-accept"));
            // Times belong at the end: duration, deadline, and priority stay in their old spots.
            assertTrue(logContains(
                    "|JOB|Joshua|joshua@example.com|client-1|job-accept|11-10-2026|5|11-20-2026|2|09:00 AM|05:00 PM"));
            assertTrue(registry.list().isEmpty());
        }

        // A new controller should be able to reload those times from the log.
        VehicularCloudController reloaded = new VehicularCloudController();
        assertEquals(1, reloaded.getJobs().size());
        assertJobTimes(reloaded.getJobs().get(0), "job-accept");
    }

    // Verifies that Reject does not write the job and then tells the client it was rejected.
    @Test
    void shouldNotStoreJobWhenRejected() throws Exception {
        SubmissionRequest request = jobRequest("job-reject");
        try (TestClient client = connect()) {
            client.send(request);
            ServerResponse decision = server.decide(request.getRequestId(), false);
            ServerResponse notified = client.readDecision();

            assertEquals(RequestStatus.REJECTED, decision.getStatus());
            assertEquals(RequestStatus.REJECTED, notified.getStatus());
            assertEquals(0, controller.getJobs().size());
            assertFalse(logContains("job-reject"));
            assertTrue(registry.list().isEmpty());
        }
    }

    // Verifies that vehicle requests follow the same Accept path as jobs.
    @Test
    void shouldStoreVehicleWhenAccepted() throws Exception {
        SubmissionRequest request = vehicleRequest("vehicle-accept");
        try (TestClient client = connect()) {
            client.send(request);
            server.decide(request.getRequestId(), true);
            ServerResponse notified = client.readDecision();

            assertEquals(RequestStatus.ACCEPTED, notified.getStatus());
            assertEquals(1, controller.getVehicles().size());
            assertEquals("vehicle-accept", controller.getVehicles().get(0).getVehicleId());
            assertEquals("owner-1", controller.getVehicleOwnerId("vehicle-accept"));
            assertEquals("joshua@example.com", controller.getVehicleAccountEmail("vehicle-accept"));
            assertTrue(logContains("vehicle-accept"));
        }
    }

    // Verifies that vehicle requests are not stored when the controller rejects them.
    @Test
    void shouldNotStoreVehicleWhenRejected() throws Exception {
        SubmissionRequest request = vehicleRequest("vehicle-reject");
        try (TestClient client = connect()) {
            client.send(request);
            server.decide(request.getRequestId(), false);
            ServerResponse notified = client.readDecision();

            assertEquals(RequestStatus.REJECTED, notified.getStatus());
            assertEquals(0, controller.getVehicles().size());
            assertFalse(logContains("vehicle-reject"));
        }
    }

    // Verifies that two clients can wait at the same time on their own worker threads.
    @Test
    void shouldHandleTwoClientsOnSeparateThreads() throws Exception {
        SubmissionRequest job = jobRequest("job-two");
        SubmissionRequest vehicle = vehicleRequest("vehicle-two");
        try (TestClient jobClient = connect();
             TestClient vehicleClient = connect()) {
            jobClient.send(job);
            vehicleClient.send(vehicle);

            assertEquals(2, registry.list().size());
            server.decide(job.getRequestId(), true);
            server.decide(vehicle.getRequestId(), false);

            assertEquals(RequestStatus.ACCEPTED, jobClient.readDecision().getStatus());
            assertEquals(RequestStatus.REJECTED, vehicleClient.readDecision().getStatus());
            assertEquals(1, controller.getJobs().size());
            assertEquals(0, controller.getVehicles().size());
        }
    }

    // Verifies that a decision for an unknown request does not change stored data.
    @Test
    void shouldReturnErrorForUnknownRequest() {
        ServerResponse response = server.decide("missing-id", true);

        assertEquals(RequestStatus.ERROR, response.getStatus());
        assertEquals(0, controller.getJobs().size());
    }

    // Verifies that a non-request object is not queued and gets an error response.
    @Test
    void shouldRejectUnexpectedMessage() throws Exception {
        try (TestClient client = connect()) {
            ServerResponse response = client.sendRaw("not a request");

            assertEquals(RequestStatus.ERROR, response.getStatus());
            assertTrue(registry.list().isEmpty());
        }
    }

    // Connect to the server started for this test.
    private TestClient connect() throws IOException {
        return new TestClient(server.getPort());
    }

    // Checks the job fields added on main: availability, times, and priority.
    private static void assertJobTimes(Job job, String jobId) {
        assertEquals(jobId, job.getJobId());
        assertEquals("client-1", job.getClientId());
        assertEquals("11-10-2026", job.getAvailability());
        assertEquals(5, job.getEstimatedDuration());
        assertEquals("11-20-2026", job.getDeadline());
        assertEquals(2, job.getPriority());
        assertEquals("09:00 AM", job.getStartTime());
        assertEquals("05:00 PM", job.getEndTime());
    }

    // Sample job request used by the server tests, including the new start and end times.
    private static SubmissionRequest jobRequest(String jobId) {
        return SubmissionRequest.forJob(
                new Job("client-1", jobId, 5, "11-20-2026", 2, "11-10-2026",
                        "09:00 AM", "05:00 PM"),
                "Joshua", "joshua@example.com");
    }

    // Sample vehicle request used by the server tests.
    private static SubmissionRequest vehicleRequest(String vehicleId) {
        Vehicle vehicle = new Vehicle(vehicleId, "Toyota", "Camry", 2020,
                "11-10-2026 09:00", "11-10-2026 17:00");
        return SubmissionRequest.forVehicle(vehicle, "owner-1", 12.5,
                "Joshua", "joshua@example.com");
    }

    // True when the test log contains the saved job or vehicle id.
    private static boolean logContains(String value) throws IOException {
        if (!Files.exists(LOG)) {
            return false;
        }
        List<String> lines = Files.readAllLines(LOG);
        for (String line : lines) {
            if (line.contains(value)) {
                return true;
            }
        }
        return false;
    }

    // Tiny socket client that speaks the SubmissionRequest / ServerResponse protocol.
    private static final class TestClient implements AutoCloseable {
        private final Socket socket;
        private final ObjectOutputStream out;
        private final ObjectInputStream in;

        // Create the output stream first so the server does not deadlock on headers.
        private TestClient(int port) throws IOException {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(4000);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        }

        // Send a real request and read the pending acknowledgement.
        private ServerResponse send(SubmissionRequest request) throws Exception {
            return sendRaw(request);
        }

        // Send any object so tests can check invalid messages.
        private ServerResponse sendRaw(Object message) throws Exception {
            out.writeObject(message);
            out.flush();
            return (ServerResponse) in.readObject();
        }

        // Read the final Accept, Reject, or error response.
        private ServerResponse readDecision() throws Exception {
            return (ServerResponse) in.readObject();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
