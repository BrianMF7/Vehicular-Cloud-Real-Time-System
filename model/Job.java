package model;

public class Job {

    /* Private fields use encapsulation so other classes cannot directly access or change the job's data */
    private String clientId;
    private String jobId;
    private int estimatedDuration;
    private String deadline;

    public Job(String clientId, String jobId, int estimatedDuration,
               String deadline) {

        this.clientId = clientId;
        this.jobId = jobId;
        this.estimatedDuration = estimatedDuration;
        this.deadline = deadline;
    }

    // getters and setters
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public int getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(int estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    /* Overrides Java's default toString() method so the Jobinformation can be displayed in a readable format */
    @Override
    public String toString() {
        return "Client ID: " + clientId +
                ", Job ID: " + jobId +
                ", Estimated Duration: " + estimatedDuration + " minutes" +
                ", Deadline: " + deadline;
    }
}
