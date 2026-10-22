package scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the full result after scheduling the jobs.
 * The list cannot be changed after this object is created.
 */
public final class ScheduleResult {
    // One result row for each job.
    private final List<ScheduleEntry> entries;
    // The average time jobs wait before they start.
    private final double averageWaitingTime;

    /*
     * Creates a schedule result.
     *
     * @param entries scheduled job rows
     * @param averageWaitingTime average time jobs waited
     */
    public ScheduleResult(List<ScheduleEntry> entries, double averageWaitingTime) {
        if (entries == null) {
            throw new IllegalArgumentException("Schedule entries are required.");
        }
        // Keep the list from being changed after this result is created.
        this.entries = Collections.unmodifiableList(new ArrayList<ScheduleEntry>(entries));
        this.averageWaitingTime = averageWaitingTime;
    }

    /* @return the scheduled job rows */
    public List<ScheduleEntry> getEntries() {
        return entries;
    }

    /* @return the average job waiting time */
    public double getAverageWaitingTime() {
        return averageWaitingTime;
    }
}
