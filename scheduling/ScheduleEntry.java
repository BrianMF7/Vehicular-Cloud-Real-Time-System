package scheduling;

import model.Job;

/*
 * Stores the calculated times for one job.
 * Times use the same unit as the job duration.
 */
public final class ScheduleEntry {
    // The job this row belongs to.
    private final Job job;
    // Time before this job starts.
    private final int waitingTime;
    // Time when this job finishes.
    private final int completionTime;

    /*
     * Creates one scheduled job entry.
     *
     * @param job job being scheduled
     * @param waitingTime time before the job starts
     * @param completionTime time when the job finishes
     */
    public ScheduleEntry(Job job, int waitingTime, int completionTime) {
        if (job == null) {
            throw new IllegalArgumentException("Job is required.");
        }
        this.job = job;
        this.waitingTime = waitingTime;
        this.completionTime = completionTime;
    }

    /* @return this entry's job */
    public Job getJob() {
        return job;
    }

    /* @return time the job waits before starting */
    public int getWaitingTime() {
        return waitingTime;
    }

    /* @return time the job finishes */
    public int getCompletionTime() {
        return completionTime;
    }
}
