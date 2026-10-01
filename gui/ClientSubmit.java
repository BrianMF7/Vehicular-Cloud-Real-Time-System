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
    //constructor
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
    //makes sure that all fields are entered by the user
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
        //checks to makes sure that duration is more than 0, since a jo can't last 0 or negative days from the start
        try {
            int durationValue = Integer.parseInt(duration.trim());
            if (durationValue <= 0) {
                throw new Exception("Duration must be greater than zero.");
            }
        } catch (NumberFormatException ex) {
            throw new Exception("Duration must be a valid number.");
        }
        //This is an exception handling for the date, making sure that date is in the format Month-Day-Year
        try {
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("MM-dd-yyyy");

            LocalDate deadlineDate =
                    LocalDate.parse(deadline.trim(), formatter);

        } catch (DateTimeParseException exception) {
            throw new Exception("Invalid deadline date (must be MM-dd-yyyy)");
        }
    }
    //takes the information that the users enter and stores it into vechicular_cloud_log.txt
    private String buildRecord() {
        return "CLIENT|" + 
               trim(clientId) + "|" + 
               trim(jobId) + "|" + 
               trim(duration) + "|" + 
               trim(deadline);
    }
    //checks to see if there is an empty value
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    //trims the value if there is any extra space
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
