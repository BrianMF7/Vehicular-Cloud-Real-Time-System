package scheduling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import model.Job;

/* Schedules jobs by priority. All jobs arrive at time 0.
 * A lower priority number runs first. Jobs with the same priority keep
 * the order in which they were submitted.
 */
public final class PriorityScheduler {

    /*
     * Sorts the jobs and calculates waiting and completion times.
     * @param submittedJobs jobs in the order they were submitted
     * @return the finished schedule
     */
    public ScheduleResult schedule(List<Job> submittedJobs) {
        if (submittedJobs == null) {
            throw new IllegalArgumentException("Submitted jobs are required.");
        }

        // Use a copy so the original list is not changed.
        List<Job> orderedJobs = new ArrayList<Job>(submittedJobs);
        for (Job job : orderedJobs) {
            validate(job);
        }

        // Equal priorities keep their original order.
        orderedJobs.sort(Comparator.comparingInt(Job::getPriority));

        List<ScheduleEntry> entries = new ArrayList<ScheduleEntry>();
        // Since all jobs arrive at time 0, this is the next job's wait time.
        int elapsedTime = 0;
        int totalWaitingTime = 0;
        for (Job job : orderedJobs) {
            int waitingTime = elapsedTime;
            // Add this job's duration to the total time.
            elapsedTime = Math.addExact(elapsedTime, job.getEstimatedDuration());
            totalWaitingTime = Math.addExact(totalWaitingTime, waitingTime);
            entries.add(new ScheduleEntry(job, waitingTime, elapsedTime));
        }

        // No jobs means there is no waiting time.
        double averageWaitingTime = entries.isEmpty()
                ? 0.0
                : (double) totalWaitingTime / entries.size();
        return new ScheduleResult(entries, averageWaitingTime);
    }

    /* Checks that the job has valid duration and priority values. */
    private void validate(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("A submitted job cannot be null.");
        }
        if (job.getEstimatedDuration() <= 0) {
            throw new IllegalArgumentException("Job duration must be greater than zero.");
        }
        if (job.getPriority() <= 0) {
            throw new IllegalArgumentException("Job priority must be greater than zero.");
        }
    }
}
