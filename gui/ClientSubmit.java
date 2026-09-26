package gui;

import storage.FileManager;

public class ClientSubmit implements SubmitStrategy {
    private final String clientId;
    private final String jobId;
    private final String duration;
    private final String deadline;

    public ClientSubmit(String clientId, String jobId, String duration, String deadline) {
        this.clientId = clientId;
        this.jobId = jobId;
        this.duration = duration;
        this.deadline = deadline;
    }

    public void submit() throws Exception {
        validateFields();
        String record = buildRecord();
        FileManager.getInstance().saveRecord(record);
    }

    private void validateFields() throws Exception {
        if (isEmpty(clientId)) {
            throw new Exception("Client ID is required.");
        }
        if (isEmpty(jobId)) {
            throw new Exception("Job ID is required.");
        }
        if (isEmpty(duration)) {
            throw new Exception("Job duration is required.");
        }
        if (isEmpty(deadline)) {
            throw new Exception("Job deadline is required.");
        }

        try {
            int durationValue = Integer.parseInt(duration.trim());
            if (durationValue <= 0) {
                throw new Exception("Duration must be greater than zero.");
            }
        } catch (NumberFormatException ex) {
            throw new Exception("Duration must be a valid number.");
        }
    }

    private String buildRecord() {
        return "CLIENT|" + 
               trim(clientId) + "|" + 
               trim(jobId) + "|" + 
               trim(duration) + "|" + 
               trim(deadline);
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
