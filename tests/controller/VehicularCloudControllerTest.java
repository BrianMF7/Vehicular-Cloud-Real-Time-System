package controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.Job;
import model.Vehicle;
import scheduling.PriorityScheduler;
import scheduling.ScheduleResult;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @BeforeEach
    void deleteTestLog() throws IOException {
        Files.deleteIfExists(LOG);
    }

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

    // Verifies that null, blank, duplicate, and negative compute cars are refused.
    @Test
    void shouldRejectInvalidVehicles() {
        VehicularCloudController controller = controller();
        assertFalse(controller.registerVehicle(null));
        assertFalse(controller.registerVehicle(new Vehicle(null, "Toyota", "Camry", 2020, "09:00", "17:00")));
        assertFalse(controller.registerVehicle(vehicle("   ")));
        assertFalse(controller.registerVehicle(vehicle("car-1"), -1.0));
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

    // Verifies that waiting times match PriorityScheduler and still include unassigned jobs.
    @Test
    void shouldMatchSchedulerWaitingTimes() {
        VehicularCloudController controller = controller();
        List<Job> jobs = Arrays.asList(
                job("P1", 10, 3), job("P2", 1, 1), job("P3", 2, 4),
                job("P4", 1, 5), job("P5", 5, 2));
        for (Job job : jobs) {
            controller.submitJob(job);
        }

        ScheduleResult expected = new PriorityScheduler().schedule(jobs);
        ScheduleResult actual = controller.scheduleJobs();
        assertEquals(8.2, actual.getAverageWaitingTime(), 0.000001);
        assertEquals(expected.getAverageWaitingTime(), controller.getAverageWaitingTime(), 0.000001);
        assertEquals("P2", actual.getEntries().get(0).getJob().getJobId());

        controller.registerVehicle(vehicle("car-1"), 10.0);
        double before = controller.getAverageWaitingTime();
        controller.assignJob("P2");
        assertEquals(before, controller.getAverageWaitingTime(), 0.000001);
        assertEquals(5, controller.scheduleJobs().getEntries().size());
    }

    // Verifies that timestamped VEHICLE and JOB lines load, including a blank compute field.
    @Test
    void shouldLoadVehicleAndJobRecords() throws IOException {
        writeLog(
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota|Camry|2020|09:00|17:00|10.0",
                "2026-10-25 13:00:00|VEHICLE|car-z|Toyota|Camry|2020|09:00|17:00|",
                "2026-10-25 13:00:00|JOB|client|job-1|5|11-01-2026|2");
        VehicularCloudController controller = controller();
        assertEquals(2, controller.getVehicles().size());
        assertEquals("car-1", controller.getVehicles().get(0).getVehicleId());
        assertEquals(10.0, controller.getTotalAvailableCompute(), 0.000001);

        Job job = controller.getJobs().get(0);
        assertEquals("job-1", job.getJobId());
        assertEquals("client", job.getClientId());
        assertEquals(5, job.getEstimatedDuration());
        assertEquals(2, job.getPriority());
    }

    // Verifies that OWNER and CLIENT lines from the GUI also load.
    @Test
    void shouldLoadOwnerAndClientRecords() throws IOException {
        writeLog(
                "2026-10-25 13:00:00|OWNER|user|mail@test.edu|own-1|car-9|Ford|Focus|2018|08:00|18:00|4.5",
                "OWNER|x|y|car-8|Honda|Civic|2019|07:00|16:00|3.0",
                "2026-10-25 13:00:00|CLIENT|user|mail@test.edu|client-1|job-9|11-01-2026|8|11-10-2026");
        VehicularCloudController controller = controller();
        assertEquals("car-9", controller.getVehicles().get(0).getVehicleId());
        assertEquals("car-8", controller.getVehicles().get(1).getVehicleId());
        assertEquals(7.5, controller.getTotalAvailableCompute(), 0.000001);

        Job job = controller.getJobs().get(0);
        assertEquals("job-9", job.getJobId());
        assertEquals("client-1", job.getClientId());
        assertEquals(8, job.getEstimatedDuration());
        assertEquals(1, job.getPriority());
    }

    // Verifies that ASSIGN is applied after cars and jobs, and DEREGISTER can remove an idle car.
    @Test
    void shouldLoadAssignmentsAndDeregisters() throws IOException {
        writeLog(
                "2026-10-25 13:00:00|ASSIGN|job-1|car-1",
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota|Camry|2020|09:00|17:00|10.0",
                "2026-10-25 13:00:00|VEHICLE|car-2|Ford|Focus|2018|08:00|18:00|4.0",
                "2026-10-25 13:00:00|JOB|client|job-1|5|11-01-2026|1",
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
                "2026-10-25 13:00:00|JOB|client|job-bad|long|11-01-2026|1",
                "2026-10-25 13:00:00|JOB|client|job-bad2|5|11-01-2026|high",
                "2026-10-25 13:00:00|VEHICLE||Toyota|Camry|2020|09:00|17:00|10.0",
                "2026-10-25 13:00:00|JOB|client||5|11-01-2026|1",
                "2026-10-25 13:00:00|JOB|client|job-zero|0|11-01-2026|1",
                "2026-10-25 13:00:00|JOB|client|job-neg|-2|11-01-2026|1",
                "2026-10-25 13:00:00|JOB|client|job-pri|5|11-01-2026|0",
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota|Camry|2020|09:00|17:00|10.0",
                "2026-10-25 13:00:00|VEHICLE|car-1|Ford|Focus|2018|08:00|18:00|4.0",
                "2026-10-25 13:00:00|JOB|client|job-1|5|11-01-2026|1",
                "2026-10-25 13:00:00|JOB|other|job-1|9|11-02-2026|3",
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
                "2026-10-25 13:00:00|JOB|client|job-1|5|11-01-2026|1",
                "2026-10-25 13:00:00|JOB|client|job-2|3|11-01-2026|2",
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
                "2026-10-25 13:00:00|JOB|client|job-1|5|11-01-2026|1",
                "2026-10-25 13:00:00|ASSIGN|job-1|car-1",
                "2026-10-25 13:00:00|RELEASE|car-1");
        VehicularCloudController released = controller();
        assertEquals("car-1", released.getAssignedVehicleId("job-1"));
        assertEquals(0, released.getAvailableVehicles().size());

        writeLog(
                "2026-10-25 13:00:00|VEHICLE|car-1|Toyota|Camry|2020|09:00|17:00|10.0",
                "2026-10-25 13:00:00|JOB|client|job-1|5|11-01-2026|1",
                "2026-10-25 13:00:00|ASSIGN|job-1|car-1",
                "2026-10-25 13:00:00|DEREGISTER|car-1");
        VehicularCloudController removed = controller();
        assertEquals(0, removed.getVehicles().size());
        assertNull(removed.getAssignedVehicleId("job-1"));
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

    private VehicularCloudController controller() {
        return new VehicularCloudController("T-1", "Test Controller");
    }

    private Vehicle vehicle(String id) {
        return new Vehicle(id, "Toyota", "Camry", 2020, "09:00", "17:00");
    }

    private Job job(String jobId, int duration, int priority) {
        return new Job("client", jobId, duration, "11-01-2026", priority);
    }

    private void writeLog(String... lines) throws IOException {
        Files.write(LOG, Arrays.asList(lines));
    }
}
