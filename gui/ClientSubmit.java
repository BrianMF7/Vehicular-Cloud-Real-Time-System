package gui;

import storage.FileManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
        //This is an exception handling for the date. I commented it since I saw that the text file has a different date.
        /*try {
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("MM-dd-yyyy");

            LocalDate deadlineDate =
                    LocalDate.parse(deadline.trim(), formatter);

        } catch (DateTimeParseException exception) {
            throw new Exception("Invalid deadline date (must be MM-dd-yyyy)");
        }*/
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
