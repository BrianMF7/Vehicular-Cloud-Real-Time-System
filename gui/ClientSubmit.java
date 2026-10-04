package gui;

import storage.FileManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;

public class ClientSubmit implements SubmitStrategy {
    private final String clientId;
    private final String jobId;
    private final String jobAvailability;
    private final String duration;
    private final String deadline;
    //constructor used to initialize the client and job information
    public ClientSubmit(String clientId, String jobId,String jobAvailability, String duration, String deadline) {
        this.clientId = clientId;
        this.jobId = jobId;
        this.jobAvailability = jobAvailability;
        this.duration = duration;
        this.deadline = deadline;
    }
    //check the entered information, create a record and save into file
    public void submit() throws Exception {
        validateFields();
        String record = buildRecord();
        FileManager.getInstance().saveRecord(record);
    }
    //makes sure that all fields that are entered by the user contain correct information
    private void validateFields() throws Exception {
        // Check that all required fields are entered
        if (isEmpty(clientId)) {
            throw new Exception("Client ID is required.");
        }
        if (isEmpty(jobId)) {
            throw new Exception("Job ID is required.");
        }
        if (isEmpty(jobAvailability)) {
            throw new Exception("Job Availability is required.");
        }
        if (isEmpty(duration)) {
            throw new Exception("Job duration is required.");
        }
        if (isEmpty(deadline)) {
            throw new Exception("Job deadline is required.");
        }

        // Validate duration to make sure it is a whole number and less than 0
        int durationValue;
        try {
            durationValue = Integer.parseInt(duration.trim());
        } catch (NumberFormatException ex) {
            throw new Exception("Duration must be a valid whole number of days.");
        }
        if (durationValue <= 0) {
            throw new Exception("Duration must be greater than zero.");
        }

        // Strictly validate the MM-dd-yyyy date format for job deadline and job availability
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-uuuu").withResolverStyle(ResolverStyle.STRICT);
        LocalDate availabilityDate;
        LocalDate deadlineDate;
        try {
            availabilityDate = LocalDate.parse(jobAvailability.trim(), formatter);
        } catch (DateTimeParseException ex) {
            throw new Exception("Invalid availability date. Use MM-dd-yyyy.");
        }
        try {
            deadlineDate = LocalDate.parse(deadline.trim(), formatter);
        } catch (DateTimeParseException ex) {
            throw new Exception("Invalid deadline date. Use MM-dd-yyyy.");
        }

        // Get today's date
        LocalDate today = LocalDate.now();

        // Availability cannot be in the past from today's date
        if (availabilityDate.isBefore(today)) {throw new Exception("Job availability cannot be in the past.");
        }
        // Deadline cannot be in the past from today's date
        if (deadlineDate.isBefore(today)) {
            throw new Exception("Job deadline cannot be in the past.");
        }
        // Deadline must be after availability
        if (!deadlineDate.isAfter(availabilityDate)) {
            throw new Exception("Deadline must be after job availability.");
        }
        // Calculate minimum duration in calendar days
        long minimumDuration = ChronoUnit.DAYS.between(availabilityDate, deadlineDate);

        // Duration can't go past the the days available for the job
        if (durationValue > minimumDuration) {
            throw new Exception("Duration cannot exceed " + minimumDuration + " days between availability and deadline.");
        }
    }
    //takes the information that the users enter and stores it into vechicular_cloud_log.txt
    private String buildRecord() {
        String username = MainFrame.getCurrentUser();
        String email = MainFrame.getCurrentEmail();
        return "CLIENT|" + 
               trim(username) + "|" +
               trim(email) + "|" +
               trim(clientId) + "|" + 
               trim(jobId) + "|" +
               trim(jobAvailability) + "|" +
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
