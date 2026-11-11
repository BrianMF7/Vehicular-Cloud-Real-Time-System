package controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.Job;
import model.RequestStatus;
import model.ServerResponse;
import model.SubmissionRequest;
import model.Vehicle;
import scheduling.ScheduleEntry;
import scheduling.ScheduleResult;

/** JUnit 5 tests for {@link VehicularCloudController}. Run with {@code mvn test}. */
class VehicularCloudControllerTest {

    private static final Path LOG = Paths.get("vehicular_cloud_log.txt");
    private static final Path LOG_BACKUP = Paths.get("vehicular_cloud_log.txt.testbak");

    // Move the real log aside so these tests do not overwrite it.
    @BeforeAll
    static void stashExistingLog() throws IOException {
        Files.deleteIfExists(LOG_BACKUP);
        if (Files.exists(LOG)) {
            Files.move(LOG, LOG_BACKUP, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // Start each test with an empty log so records from another test cannot leak in.
    @BeforeEach
    void deleteTestLog() throws IOException {
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

    // Verifies default names, a custom name, and that a missing log starts empty.
    @Test
    void shouldCreateControllers() {
        VehicularCloudController defaults = new VehicularCloudController();
        assertEquals("VCC-1", defaults.getControllerID());
        assertEquals("Vehicular Cloud Controller", defaults.getName());
        assertEquals(0.0, defaults.getTotalAvailableCompute(), 0.000001);
        assertEquals(0, defaults.getVehicles().size());
        assertEquals(0, defaults.getJobs().size());

        VehicularCloudController custom = new VehicularCloudController("VCC-9", "Lab Cloud");
        assertEquals("VCC-9", custom.getControllerID());
        assertEquals("Lab Cloud", custom.getName());

        VehicularCloudController empty = controller();
        assertEquals(0, empty.getVehicles().size());
        assertEquals(0, empty.getJobs().size());
    }

    // Verifies that getInstance keeps returning the same controller.
    @Test
    void shouldReuseGetInstance() {
        VehicularCloudController first = VehicularCloudController.getInstance();
        assertNotNull(first);
        assertSame(first, VehicularCloudController.getInstance());
    }

    // Verifies that cars can be added with compute, without compute, or with missing extra fields.
    @Test
    void shouldRegisterVehicles() {
        VehicularCloudController controller = controller();
        assertTrue(controller.registerVehicle(vehicle("car-1"), 10.0));
        assertTrue(controller.registerVehicle(vehicle("car-2")));
        assertTrue(controller.registerVehicle(vehicle("car-3"), 0.0));
        assertTrue(controller.registerVehicle(new Vehicle("car-null", null, null, 2020, null, null), 3.0));

        assertEquals(4, controller.getVehicles().size());
        assertEquals(13.0, controller.getTotalAvailableCompute(), 0.000001);
        assertEquals("car-1", controller.findAvailableVehicle(Double.valueOf(0)).getVehicleId());

        controller.getVehicles().clear();
        assertEquals(4, controller.getVehicles().size());
    }

    // Verifies that null, blank, duplicate, negative, NaN, and infinite compute cars are refused.
    @Test
    void shouldRejectInvalidVehicles() {
        VehicularCloudController controller = controller();
        assertFalse(controller.registerVehicle(null));
        assertFalse(controller.registerVehicle(new Vehicle(null, "Toyota", "Camry", 2020, "09:00", "17:00")));
        assertFalse(controller.registerVehicle(vehicle("   ")));
        assertFalse(controller.registerVehicle(vehicle("car-1"), -1.0));
        assertFalse(controller.registerVehicle(vehicle("car-nan"), Double.NaN));
        assertFalse(controller.registerVehicle(vehicle("car-infinite"), Double.POSITIVE_INFINITY));
        assertFalse(controller.registerVehicle(vehicle("car-negative-infinite"), Double.NEGATIVE_INFINITY));
        assertEquals(0, controller.getVehicles().size());

        assertTrue(controller.registerVehicle(vehicle("car-1"), 10.0));
        assertFalse(controller.registerVehicle(vehicle("car-1"), 20.0));
        assertEquals(1, controller.getVehicles().size());
        assertEquals(10.0, controller.getTotalAvailableCompute(), 0.000001);
    }

    // Verifies that only idle cars can leave, including after a job is released.
    @Test
    void shouldDeregisterIdleVehiclesButNotBusyOnes() {
        VehicularCloudController controller = controller();
        Vehicle vehicle = vehicle("car-1");
        controller.registerVehicle(vehicle, 10.0);
        controller.submitJob(job("job-1", 5, 1));

        assertFalse(controller.deregisterVehicle("missing"));
        assertFalse(controller.deregisterVehicle(null));

        controller.assignJob("job-1");
        assertFalse(controller.deregisterVehicle("car-1"));
        assertEquals(1, controller.getVehicles().size());

        controller.releaseVehicle(vehicle);
        assertTrue(controller.deregisterVehicle("car-1"));
        assertEquals(0, controller.getVehicles().size());
        assertEquals(0.0, controller.getTotalAvailableCompute(), 0.000001);
    }

    // Verifies that the next free car is chosen by compute and that busy cars are skipped.
    @Test
    void shouldFindAvailableVehicles() {
        VehicularCloudController controller = controller();
        controller.registerVehicle(vehicle("car-low"), 5.0);
        controller.registerVehicle(vehicle("car-high"), 20.0);
        assertEquals("car-low", controller.findAvailableVehicle(null).getVehicleId());
        assertEquals("car-low", controller.findAvailableVehicle(Double.valueOf(-1)).getVehicleId());

        controller.submitJob(job("job-1", 2, 1));
        controller.assignJob("job-1");
        assertEquals("car-high", controller.findAvailableVehicle(Double.valueOf(10)).getVehicleId());
        assertNull(controller.findAvailableVehicle(Double.valueOf(25)));

        List<Vehicle> available = controller.getAvailableVehicles();
        assertEquals(1, available.size());
        assertEquals("car-high", available.get(0).getVehicleId());
        available.clear();
        assertEquals(1, controller.getAvailableVehicles().size());

        controller.submitJob(job("job-2", 2, 2));
        controller.assignJob("job-2");
        assertNull(controller.findAvailableVehicle(Double.valueOf(0)));
        assertEquals(0, controller.getAvailableVehicles().size());
    }

    // Verifies that release ignores unknown cars and frees a car for the next job.
    @Test
    void shouldReleaseAVehicleSoAnotherJobCanUseIt() {
        VehicularCloudController controller = controller();
        Vehicle vehicle = vehicle("car-1");
        controller.registerVehicle(vehicle, 10.0);
        controller.submitJob(job("job-1", 4, 1));
        controller.submitJob(job("job-2", 3, 2));

        assertDoesNotThrow(() -> controller.releaseVehicle(null));
        assertDoesNotThrow(() -> controller.releaseVehicle(vehicle("car-2")));

        assertTrue(controller.assignJob("job-1"));
        controller.releaseVehicle(vehicle);
        assertNull(controller.getAssignedVehicleId("job-1"));
        assertEquals(10.0, controller.getTotalAvailableCompute(), 0.000001);
        assertTrue(controller.assignJob("job-2"));
        assertEquals("car-1", controller.getAssignedVehicleId("job-2"));
    }

    // Verifies that jobs are stored, copies cannot change the list, and a missing client id is allowed.
    @Test
    void shouldSubmitJobs() {
        VehicularCloudController controller = controller();
        assertTrue(controller.submitJob(job("job-1", 5, 2)));
        assertTrue(controller.submitJob(new Job(null, "job-2", 3, "11-01-2026", 1)));
        assertEquals(2, controller.getJobs().size());
        assertEquals("job-1", controller.getJobs().get(0).getJobId());

        controller.getJobs().clear();
        assertEquals(2, controller.getJobs().size());
    }

    // Verifies that null, blank, duplicate, and non-positive duration jobs are refused.
    @Test
    void shouldRejectInvalidJobs() {
        VehicularCloudController controller = controller();
        assertFalse(controller.submitJob(null));
        assertFalse(controller.submitJob(new Job("client", null, 5, "11-01-2026", 1)));
        assertFalse(controller.submitJob(job("   ", 5, 1)));
        assertFalse(controller.submitJob(new Job("client", "job-0", 0, "11-01-2026", 1)));
        assertFalse(controller.submitJob(new Job("client", "job-neg", -3, "11-01-2026", 1)));

        assertTrue(controller.submitJob(job("job-1", 5, 1)));
        assertFalse(controller.submitJob(job("job-1", 8, 2)));
        assertEquals(1, controller.getJobs().size());
    }

    // Verifies that jobs go to the first free car and a second job needs another car.
    @Test
    void shouldAssignJobsToFreeVehicles() {
        VehicularCloudController controller = controller();
        controller.registerVehicle(vehicle("car-1"), 10.0);
        controller.registerVehicle(vehicle("car-2"), 8.0);
        controller.submitJob(job("job-1", 4, 1));
        controller.submitJob(job("job-2", 3, 2));
        controller.submitJob(job("job-3", 2, 3));

        assertTrue(controller.assignJob("job-1"));
        assertEquals("car-1", controller.getAssignedVehicleId("job-1"));
        assertEquals(8.0, controller.getTotalAvailableCompute(), 0.000001);

        assertTrue(controller.assignJob("job-2"));
        assertEquals("car-2", controller.getAssignedVehicleId("job-2"));
        assertFalse(controller.assignJob("job-3"));
        assertNull(controller.getAssignedVehicleId("job-3"));
        assertEquals(0, controller.getAvailableVehicles().size());
    }

    // Verifies that assign returns false when the job or a free car is missing.
    @Test
    void shouldRejectAssignWhenItCannotBeDone() {
        VehicularCloudController controller = controller();
        controller.registerVehicle(vehicle("car-1"), 10.0);
        controller.registerVehicle(vehicle("car-2"), 8.0);
        controller.submitJob(job("job-1", 4, 1));

        assertFalse(controller.assignJob("missing"));
        assertFalse(controller.assignJob(null));
        assertTrue(controller.assignJob("job-1"));
        assertFalse(controller.assignJob("job-1"));
        assertEquals("car-1", controller.getAssignedVehicleId("job-1"));

        VehicularCloudController noCars = controller();
        noCars.submitJob(job("job-1", 4, 1));
        assertFalse(noCars.assignJob("job-1"));
    }

    // Verifies that monitor ignores unknown jobs and assigned-vehicle lookup can return null.
    @Test
    void shouldMonitorJobs() {
        VehicularCloudController controller = controller();
        assertDoesNotThrow(() -> controller.monitorJob("missing"));
        assertDoesNotThrow(() -> controller.monitorJob(null));
        assertNull(controller.getAssignedVehicleId("missing"));
        assertNull(controller.getAssignedVehicleId(null));

        controller.submitJob(job("job-1", 5, 1));
        assertDoesNotThrow(() -> controller.monitorJob("job-1"));
        assertEquals(0.0, controller.getAverageWaitingTime(), 0.000001);
    }

    // Verifies the status text for an empty cloud and after a job is assigned.
    @Test
    void shouldShowSystemStatus() {
        String empty = controller().getSystemStatus();
        assertTrue(empty.contains("Controller: Test Controller (T-1)"));
        assertTrue(empty.contains("Vehicles: 0 (0 available)"));
        assertTrue(empty.contains("Jobs: 0 (0 assigned)"));
        assertTrue(empty.contains("Total available compute: 0.0"));
        assertTrue(empty.contains("Average waiting time: 0.0"));

        VehicularCloudController controller = controller();
        controller.registerVehicle(vehicle("car-1"), 10.0);
        controller.submitJob(job("job-1", 5, 1));
        controller.assignJob("job-1");
        String busy = controller.getSystemStatus();
        assertTrue(busy.contains("Vehicles: 1 (0 available)"));
        assertTrue(busy.contains("Jobs: 1 (1 assigned)"));
        assertTrue(busy.contains("Total available compute: 0.0"));
    }

    // Verifies the assignment example: jobs arrive at time 0, run by priority,
    // and the controller reports each completion time plus average wait 8.2.
    @Test
    void shouldComputeCompletionTimesUsingPriority() {
        VehicularCloudController controller = controller();
        List<Job> jobs = Arrays.asList(
                job("P1", 10, 3), job("P2", 1, 1), job("P3", 2, 4),
                job("P4", 1, 5), job("P5", 5, 2));
        for (Job job : jobs) {
            assertTrue(controller.submitJob(job));
        }

        ScheduleResult result = controller.scheduleJobs();
        assertAssignmentPriorityExample(result);
        assertEquals(result.getAverageWaitingTime(), controller.getAverageWaitingTime(), 0.000001);

        // Cars are stored, but they do not change the priority completion times.
        controller.registerVehicle(vehicle("car-1"), 10.0);
        controller.assignJob("P2");
        assertAssignmentPriorityExample(controller.scheduleJobs());
    }

    // Verifies the same completion times after the controller reloads jobs from the log file.
    @Test
    void shouldComputeCompletionTimesFromSavedJobs() throws IOException {
        writeLog(
                jobLine("P1", 10, 3),
                jobLine("P2", 1, 1),
                jobLine("P3", 2, 4),
                jobLine("P4", 1, 5),
                jobLine("P5", 5, 2));
        assertAssignmentPriorityExample(controller().scheduleJobs());
    }

    // Verifies that timestamped VEHICLE and JOB lines load, including a blank compute field.
    @Test
    void shouldLoadVehicleAndJobRecords() throws IOException {
        writeLog(
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota|Camry|2020|09:00|17:00|10.0",
                "2026-10-25 13:00:00|VEHICLE|car-z|Toyota|Camry|2020|09:00|17:00|",
                jobLine("job-1", 5, 2));
        VehicularCloudController controller = controller();
        assertEquals(2, controller.getVehicles().size());
        assertEquals("car-1", controller.getVehicles().get(0).getVehicleId());
        assertEquals(10.0, controller.getTotalAvailableCompute(), 0.000001);

        Job job = controller.getJobs().get(0);
        assertEquals("job-1", job.getJobId());
        assertEquals("client", job.getClientId());
        assertEquals(5, job.getEstimatedDuration());
        assertEquals(2, job.getPriority());
        assertEquals("", job.getAvailability());
    }

    // Verifies that the current JOB line stores username, email, and availability.
    @Test
    void shouldLoadJobRecordsWithAccountAndAvailability() throws IOException {
        writeLog(
                "2026-10-25 13:00:00|JOB|user|mail@test.edu|client|job-1|11-01-2026|5|11-10-2026|2");
        VehicularCloudController controller = controller();
        Job job = controller.getJobs().get(0);
        assertEquals("job-1", job.getJobId());
        assertEquals("client", job.getClientId());
        assertEquals("11-01-2026", job.getAvailability());
        assertEquals(5, job.getEstimatedDuration());
        assertEquals("11-10-2026", job.getDeadline());
        assertEquals(2, job.getPriority());
        assertEquals("user", controller.getJobAccountName("job-1"));
        assertEquals("mail@test.edu", controller.getJobAccountEmail("job-1"));
    }

    // Verifies that OWNER lines load, including owner id and account email.
    @Test
    void shouldLoadOwnerRecords() throws IOException {
        writeLog(
                "2026-10-25 13:00:00|OWNER|user|mail@test.edu|own-1|car-9|Ford|Focus|2018|08:00|18:00|4.5",
                "OWNER|x|y|car-8|Honda|Civic|2019|07:00|16:00|3.0");
        VehicularCloudController controller = controller();
        assertEquals("car-9", controller.getVehicles().get(0).getVehicleId());
        assertEquals("own-1", controller.getVehicleOwnerId("car-9"));
        assertEquals("mail@test.edu", controller.getVehicleAccountEmail("car-9"));
        assertEquals("car-8", controller.getVehicles().get(1).getVehicleId());
        assertEquals(7.5, controller.getTotalAvailableCompute(), 0.000001);
        assertEquals(0, controller.getJobs().size());
    }

    // Verifies that the car form can store owner name and account email with a car.
    @Test
    void shouldRememberOwnerWhenRegisteringAVehicle() {
        VehicularCloudController controller = controller();
        assertTrue(controller.registerVehicle(vehicle("car-1"), 10.0, "own-1", "mail@test.edu"));
        assertEquals("own-1", controller.getVehicleOwnerId("car-1"));
        assertEquals("mail@test.edu", controller.getVehicleAccountEmail("car-1"));
        assertEquals("", controller.getVehicleOwnerId("missing"));
        assertEquals("", controller.getVehicleAccountEmail("missing"));
    }

    // Verifies that ASSIGN is applied after cars and jobs, and DEREGISTER can remove an idle car.
    @Test
    void shouldLoadAssignmentsAndDeregisters() throws IOException {
        writeLog(
                "2026-10-25 13:00:00|ASSIGN|job-1|car-1",
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota|Camry|2020|09:00|17:00|10.0",
                "2026-10-25 13:00:00|VEHICLE|car-2|Ford|Focus|2018|08:00|18:00|4.0",
                jobLine("job-1", 5, 1),
                "2026-10-25 13:00:00|DEREGISTER|car-2");
        VehicularCloudController controller = controller();
        assertEquals("car-1", controller.getAssignedVehicleId("job-1"));
        assertEquals(1, controller.getVehicles().size());
        assertEquals("car-1", controller.getVehicles().get(0).getVehicleId());
        assertEquals(0, controller.getAvailableVehicles().size());
        assertEquals(0.0, controller.getTotalAvailableCompute(), 0.000001);
    }

    // Verifies that empty, unknown, short, and non-numeric log lines are skipped.
    @Test
    void shouldSkipBrokenLogLines() throws IOException {
        writeLog(
                "",
                "   ",
                "2026-10-25 13:00:00|NOPE|foo",
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota",
                "2026-10-25 13:00:00|JOB|client|job-1",
                "2026-10-25 13:00:00|VEHICLE|car-bad|Toyota|Camry|year|09:00|17:00|10.0",
                "2026-10-25 13:00:00|VEHICLE|car-bad2|Toyota|Camry|2020|09:00|17:00|nope",
                "2026-10-25 13:00:00|JOB|||client|job-bad||long|11-01-2026|1",
                "2026-10-25 13:00:00|JOB|||client|job-bad2||5|11-01-2026|high",
                "2026-10-25 13:00:00|VEHICLE||Toyota|Camry|2020|09:00|17:00|10.0",
                "2026-10-25 13:00:00|JOB|||client|||5|11-01-2026|1",
                jobLine("job-zero", 0, 1),
                "2026-10-25 13:00:00|JOB|||client|job-neg||-2|11-01-2026|1",
                jobLine("job-pri", 5, 0),
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota|Camry|2020|09:00|17:00|10.0",
                "2026-10-25 13:00:00|VEHICLE|car-1|Ford|Focus|2018|08:00|18:00|4.0",
                jobLine("job-1", 5, 1),
                "2026-10-25 13:00:00|JOB|||other|job-1||9|11-02-2026|3",
                "2026-10-25 13:00:00|ASSIGN",
                "2026-10-25 13:00:00|RELEASE",
                "2026-10-25 13:00:00|DEREGISTER");
        VehicularCloudController controller = controller();
        assertEquals(1, controller.getVehicles().size());
        assertEquals("Toyota", controller.getVehicles().get(0).getMake());
        assertEquals(1, controller.getJobs().size());
        assertEquals(5, controller.getJobs().get(0).getEstimatedDuration());
        assertNull(controller.getAssignedVehicleId("job-1"));
    }

    // Verifies that ASSIGN is skipped when the job or car is missing, already assigned, or busy.
    @Test
    void shouldSkipInvalidAssignmentsFromTheLog() throws IOException {
        writeLog(
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota|Camry|2020|09:00|17:00|10.0",
                "2026-10-25 13:00:00|VEHICLE|car-2|Ford|Focus|2018|08:00|18:00|8.0",
                jobLine("job-1", 5, 1),
                jobLine("job-2", 3, 2),
                "2026-10-25 13:00:00|ASSIGN|job-1|car-missing",
                "2026-10-25 13:00:00|ASSIGN|job-missing|car-1",
                "2026-10-25 13:00:00|ASSIGN|job-1|car-1",
                "2026-10-25 13:00:00|ASSIGN|job-1|car-2",
                "2026-10-25 13:00:00|ASSIGN|job-2|car-1");
        VehicularCloudController controller = controller();
        assertEquals("car-1", controller.getAssignedVehicleId("job-1"));
        assertNull(controller.getAssignedVehicleId("job-2"));
        assertEquals(1, controller.getAvailableVehicles().size());
    }

    // Verifies current load order: RELEASE does not undo ASSIGN, but DEREGISTER still removes the car.
    @Test
    void shouldFollowReleaseAndDeregisterOrderWhenLoading() throws IOException {
        writeLog(
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota|Camry|2020|09:00|17:00|10.0",
                jobLine("job-1", 5, 1),
                "2026-10-25 13:00:00|ASSIGN|job-1|car-1",
                "2026-10-25 13:00:00|RELEASE|car-1");
        VehicularCloudController released = controller();
        assertEquals("car-1", released.getAssignedVehicleId("job-1"));
        assertEquals(0, released.getAvailableVehicles().size());

        writeLog(
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota|Camry|2020|09:00|17:00|10.0",
                jobLine("job-1", 5, 1),
                "2026-10-25 13:00:00|ASSIGN|job-1|car-1",
                "2026-10-25 13:00:00|DEREGISTER|car-1");
        VehicularCloudController removed = controller();
        assertEquals(0, removed.getVehicles().size());
        assertNull(removed.getAssignedVehicleId("job-1"));
    }

    // Verifies that submitting a job writes one JOB line with account and availability.
    @Test
    void shouldSaveAJobRecord() throws IOException {
        VehicularCloudController controller = controller();
        Job submitted = new Job("client", "job-1", 5, "11-10-2026", 1, "11-01-2026");
        assertTrue(controller.submitJob(submitted, "user", "mail@test.edu"));

        List<String> lines = Files.readAllLines(LOG);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("|JOB|user|mail@test.edu|client|job-1|11-01-2026|5|11-10-2026|1"));
        assertEquals("11-01-2026", controller.getJobs().get(0).getAvailability());
        assertEquals("user", controller.getJobAccountName("job-1"));
        assertEquals("mail@test.edu", controller.getJobAccountEmail("job-1"));
    }

    // Verifies the form save path: ClientSubmit talks to the controller and writes one JOB line.
    @Test
    void shouldLetClientSubmitSaveThroughTheControllerOnly() throws Exception {
        VehicularCloudController controller = controller();
        gui.ClientSubmit submit = new gui.ClientSubmit(
                "client-1", "form-job", "12-01-2026", "2", "12-10-2026", "3");
        submit.submit(controller);

        List<String> lines = Files.readAllLines(LOG);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("|JOB|||client-1|form-job|12-01-2026|2|12-10-2026|3"));
        assertEquals("12-01-2026", controller.getJobs().get(0).getAvailability());
        assertEquals(3, controller.getJobs().get(0).getPriority());

        VehicularCloudController reloaded = controller();
        assertEquals("12-01-2026", reloaded.getJobs().get(0).getAvailability());
        assertEquals(3, reloaded.getJobs().get(0).getPriority());
        assertEquals("form-job", reloaded.getJobs().get(0).getJobId());
    }

    // A rejected request is not written. An accepted one is saved and can be loaded.
    @Test
    void shouldSaveOnlyAcceptedRequests() throws IOException {
        VehicularCloudController controller = controller();
        Job job = new Job("client-1", "job-9", 4, "11-10-2026", 2, "11-02-2026");
        SubmissionRequest rejected = SubmissionRequest.forJob(job, "Brian", "brian@test.com");

        ServerResponse no = controller.decideRequest(rejected, false);
        assertEquals(RequestStatus.REJECTED, no.getStatus());
        assertEquals(0, controller.getJobs().size());
        if (Files.exists(LOG)) {
            assertEquals(0, Files.readAllLines(LOG).size());
        }

        SubmissionRequest accepted = SubmissionRequest.forJob(job, "Brian", "brian@test.com");
        ServerResponse yes = controller.decideRequest(accepted, true);
        assertEquals(RequestStatus.ACCEPTED, yes.getStatus());
        assertEquals(accepted.getRequestId(), yes.getRequestId());
        assertEquals(1, controller.getJobs().size());

        VehicularCloudController reloaded = controller();
        assertEquals("job-9", reloaded.getJobs().get(0).getJobId());
        assertEquals(2, reloaded.getJobs().get(0).getPriority());

        Vehicle car = vehicle("car-9");
        SubmissionRequest carRequest = SubmissionRequest.forVehicle(car, "Brian", 4,
                "Brian", "brian@test.com");
        assertEquals(RequestStatus.ACCEPTED, controller.decideRequest(carRequest, true).getStatus());
        assertEquals("Brian", controller.getVehicleOwnerId("car-9"));
        assertEquals("brian@test.com", controller.getVehicleAccountEmail("car-9"));
    }

    // Verifies that a second controller can read cars, jobs, and the assignment the first one saved.
    @Test
    void shouldReloadSavedData() {
        VehicularCloudController first = controller();
        first.registerVehicle(vehicle("car-1"), 8.0);
        first.submitJob(job("job-1", 5, 1));
        first.assignJob("job-1");

        VehicularCloudController second = new VehicularCloudController("T-2", "Reload Controller");
        assertNotSame(first, second);
        assertEquals(1, second.getVehicles().size());
        assertEquals("car-1", second.getVehicles().get(0).getVehicleId());
        assertEquals(1, second.getJobs().size());
        assertEquals("job-1", second.getJobs().get(0).getJobId());
        assertEquals("car-1", second.getAssignedVehicleId("job-1"));
        assertEquals(0.0, second.getTotalAvailableCompute(), 0.000001);
    }

    // Builds a controller that starts from the test log file.
    private VehicularCloudController controller() {
        return new VehicularCloudController("T-1", "Test Controller");
    }

    // Sample car used by the tests.
    private Vehicle vehicle(String id) {
        return new Vehicle(id, "Toyota", "Camry", 2020, "09:00", "17:00");
    }

    // Sample job used by the tests.
    private Job job(String jobId, int duration, int priority) {
        return new Job("client", jobId, duration, "11-01-2026", priority);
    }

    // JOB log line matching the format the controller writes.
    private String jobLine(String jobId, int duration, int priority) {
        return "2026-10-25 13:00:00|JOB|||client|" + jobId + "||" + duration + "|11-01-2026|" + priority;
    }

    // Assignment example: P2, P5, P1, P3, P4 with completion times 1, 6, 16, 18, 19.
    private void assertAssignmentPriorityExample(ScheduleResult result) {
        assertEquals(5, result.getEntries().size());
        assertEntry(result.getEntries().get(0), "P2", 0, 1);
        assertEntry(result.getEntries().get(1), "P5", 1, 6);
        assertEntry(result.getEntries().get(2), "P1", 6, 16);
        assertEntry(result.getEntries().get(3), "P3", 16, 18);
        assertEntry(result.getEntries().get(4), "P4", 18, 19);
        assertEquals(8.2, result.getAverageWaitingTime(), 0.000001);
    }

    // Checks one scheduled job's waiting and completion times.
    private void assertEntry(ScheduleEntry entry, String jobId, int waitingTime, int completionTime) {
        assertEquals(jobId, entry.getJob().getJobId());
        assertEquals(waitingTime, entry.getWaitingTime());
        assertEquals(completionTime, entry.getCompletionTime());
    }

    // Writes these lines into the test log before a controller is created.
    private void writeLog(String... lines) throws IOException {
        Files.write(LOG, Arrays.asList(lines));
    }
}
