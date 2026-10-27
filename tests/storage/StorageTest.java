package tests.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import model.Checkpoint;
import model.Job;
import model.Vehicle;
import storage.FileManager;
import storage.RecordParser;
import storage.Server;

/** JUnit tests for saved records, checkpoints, and server results. */
class StorageTest {
    @TempDir
    Path temporaryDirectory;

    // Verifies that the server stores results and checkpoints for a job.
    @Test
    void shouldStoreResultsAndCheckpoints() throws InterruptedException {
        Server server = new Server("SERVER-001");
        Checkpoint first = new Checkpoint("CP-001", "JOB-123", "/data/first.img", 15);
        Thread.sleep(2);
        Checkpoint latest = new Checkpoint("CP-002", "JOB-123", "/data/latest.img", 30);

        assertTrue(server.storeResult("JOB-123", "Completed successfully"));
        assertTrue(server.storeCheckpoint(first));
        assertTrue(server.storeCheckpoint(latest));

        assertEquals("Completed successfully", server.getResult("JOB-123"));
        assertEquals(2, server.getAllCheckpointsForJob("JOB-123").size());
        assertSame(latest, server.getLatestCheckpoint("JOB-123"));
    }

    // Verifies that FileManager timestamps records and RecordParser reads the current GUI format.
    @Test
    void shouldSaveAndLoadCurrentGuiRecords() throws Exception {
        FileManager fileManager = new FileManager(temporaryDirectory.resolve("test-log.txt"));
        fileManager.saveRecord("CLIENT|Sajid|sajid@example.com|client-1|job-1|10-30-2026|7|11-02-2026|2");
        fileManager.saveRecord("OWNER|Sajid|sajid@example.com|owner-1|car-1|Toyota|Camry|2020|09:00|17:00|10.0");

        List<String> records = fileManager.loadAllRecords();
        assertEquals(2, records.size());
        assertTrue(records.get(0).matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\|CLIENT\\|.*"));
        assertEquals(1, fileManager.loadClientRecords().size());
        assertEquals(1, fileManager.loadOwnerRecords().size());

        List<Job> jobs = RecordParser.loadAllJobs(fileManager);
        assertEquals(1, jobs.size());
        assertEquals("client-1", jobs.get(0).getClientId());
        assertEquals("job-1", jobs.get(0).getJobId());
        assertEquals(7, jobs.get(0).getEstimatedDuration());
        assertEquals("11-02-2026", jobs.get(0).getDeadline());
        assertEquals(2, jobs.get(0).getPriority());

        List<Vehicle> vehicles = RecordParser.loadAllVehicles(fileManager);
        assertEquals(1, vehicles.size());
        assertEquals("car-1", vehicles.get(0).getVehicleId());
        assertEquals("Toyota", vehicles.get(0).getMake());
    }

    // Verifies that old client records without user details still load with priority 1
    @Test
    void shouldLoadLegacyClientRecords() {
        Job job = RecordParser.parseClientRecord("CLIENT|client-2|job-2|4|11-03-2026");
        assertNotNull(job);
        assertEquals("client-2", job.getClientId());
        assertEquals("job-2", job.getJobId());
        assertEquals(4, job.getEstimatedDuration());
        assertEquals(1, job.getPriority());
    }

    // Verifies that current client records without a priority use the default priority of 1.
    @Test
    void shouldUseDefaultPriorityWhenCurrentRecordHasNoPriority() {
        Job job = RecordParser.parseClientRecord(
                "2026-10-27 11:00:00|CLIENT|Sajid|sajid@example.com|client-3|job-3|10-30-2026|6|11-03-2026");

        assertNotNull(job);
        assertEquals(6, job.getEstimatedDuration());
        assertEquals(1, job.getPriority());
    }

    // Verifies that empty records are not written to a log file.
    @Test
    void shouldRejectEmptyRecords() throws Exception {
        FileManager fileManager = new FileManager(temporaryDirectory.resolve("empty-record-test.txt"));

        assertThrows(IllegalArgumentException.class, () -> fileManager.saveRecord(""));
        assertThrows(IllegalArgumentException.class, () -> fileManager.saveRecord("   "));
        assertEquals(0, fileManager.loadAllRecords().size());
    }

    // Verifies that the server rejects missing checkpoint and job information.
    @Test
    void shouldRejectInvalidServerData() {
        Server server = new Server("SERVER-002");

        assertFalse(server.storeResult(null, "result"));
        assertFalse(server.storeResult("   ", "result"));
        assertFalse(server.storeCheckpoint(null));
        assertNull(server.getResult("missing-job"));
        assertNull(server.getLatestCheckpoint("missing-job"));
    }
}
