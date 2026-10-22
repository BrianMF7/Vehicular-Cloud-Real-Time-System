package scheduling;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import model.Job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** JUnit 5 tests for {@link PriorityScheduler}. Run with {@code mvn test}. */
class PrioritySchedulerTest {

    // Verifies the required sample: lower priority values run first and produce the expected timing.
    @Test
    void shouldMatchProvidedPriorityExample() {
        List<Job> jobs = Arrays.asList(
                job("P1", 10, 3), job("P2", 1, 1), job("P3", 2, 4),
                job("P4", 1, 5), job("P5", 5, 2));

        ScheduleResult result = new PriorityScheduler().schedule(jobs);
        assertEquals("P2", result.getEntries().get(0).getJob().getJobId());
        assertEquals("P5", result.getEntries().get(1).getJob().getJobId());
        assertEquals("P1", result.getEntries().get(2).getJob().getJobId());
        assertEquals("P3", result.getEntries().get(3).getJob().getJobId());
        assertEquals("P4", result.getEntries().get(4).getJob().getJobId());
        assertEquals(1, result.getEntries().get(0).getCompletionTime());
        assertEquals(6, result.getEntries().get(1).getCompletionTime());
        assertEquals(16, result.getEntries().get(2).getCompletionTime());
        assertEquals(18, result.getEntries().get(3).getCompletionTime());
        assertEquals(19, result.getEntries().get(4).getCompletionTime());
        assertEquals(8.2, result.getAverageWaitingTime(), 0.000001);
    }

    // Verifies the FIFO tie-breaker when two jobs have the same priority value.
    @Test
    void shouldPreserveSubmissionOrderForEqualPriority() {
        ScheduleResult result = new PriorityScheduler().schedule(Arrays.asList(
                job("first", 2, 1), job("second", 3, 1)));
        assertEquals("first", result.getEntries().get(0).getJob().getJobId());
        assertEquals("second", result.getEntries().get(1).getJob().getJobId());
    }

    // Verifies that an empty job list produces an empty schedule and an average waiting time of zero.
    @Test
    void shouldHandleNoJobs() {
        ScheduleResult result = new PriorityScheduler().schedule(Collections.<Job>emptyList());
        assertEquals(0, result.getEntries().size());
        assertEquals(0.0, result.getAverageWaitingTime(), 0.000001);
    }

    // Verifies that one job starts immediately, finishes after its duration, and has no waiting time.
    @Test
    void shouldScheduleOneJobWithoutWaiting() {
        ScheduleResult result = new PriorityScheduler().schedule(Arrays.asList(job("only", 7, 1)));
        assertEquals(0, result.getEntries().get(0).getWaitingTime());
        assertEquals(7, result.getEntries().get(0).getCompletionTime());
        assertEquals(0.0, result.getAverageWaitingTime(), 0.000001);
    }

    // Verifies that priority zero is invalid because priorities must be positive whole numbers.
    @Test
    void shouldRejectZeroPriority() {
        assertThrows(IllegalArgumentException.class,
                () -> new Job("client", "bad-priority", 1, "11-01-2026", 0));
    }

    // Verifies that the scheduler refuses a job with no processing time.
    @Test
    void shouldRejectZeroDuration() {
        assertThrows(IllegalArgumentException.class, () -> new PriorityScheduler().schedule(
                Arrays.asList(job("bad-duration", 0, 1))));
    }

    // Verifies that the scheduler gives a clear error when no job list is supplied.
    @Test
    void shouldRejectNullJobList() {
        assertThrows(IllegalArgumentException.class, () -> new PriorityScheduler().schedule(null));
    }

    private Job job(String jobId, int duration, int priority) {
        return new Job("client", jobId, duration, "11-01-2026", priority);
    }
}
