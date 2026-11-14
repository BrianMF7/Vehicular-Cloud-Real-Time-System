package gui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import controller.VehicularCloudController;
import model.Job;

public class ClientSubmit implements SubmitStrategy {

    private final String clientId;
    private final String jobId;
    private final String jobAvailability;
    private final String duration;
    private final String deadline;
    private final String priority;
    private final String startTime;
    private final String endTime;

    // Constructor used when priority and times are not supplied.
    public ClientSubmit(
            String clientId,
            String jobId,
            String jobAvailability,
            String duration,
            String deadline) {

        this(clientId, jobId, jobAvailability, duration, deadline, "1");
    }
    // Constructor used when priority is supplied but times are not.
    public ClientSubmit(
            String clientId,
            String jobId,
            String jobAvailability,
            String duration,
            String deadline,
            String priority) {

        this(
                clientId,
                jobId,
                jobAvailability,
                duration,
                deadline,
                priority,
                "",
                ""
        );
    }
    // Constructor used by the updated form with start and end times.
    public ClientSubmit(
            String clientId,
            String jobId,
            String jobAvailability,
            String duration,
            String deadline,
            String priority,
            String startTime,
            String endTime) {

        this.clientId = clientId;
        this.jobId = jobId;
        this.jobAvailability = jobAvailability;
        this.duration = duration;
        this.deadline = deadline;
        this.priority = priority;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Validate the entered information, then save the job.
    public void submit() throws Exception {
        submit(VehicularCloudController.getInstance());
    }

    // Same save path with a supplied controller for testing.
    public void submit(VehicularCloudController controller) throws Exception {
        Job job = createJob();
        if (!controller.submitJob(
                job,
                MainFrame.getCurrentUser(),
                MainFrame.getCurrentEmail())) {

            throw new Exception(
                    jobAlreadySaved(controller, job.getJobId())
                            ? "Job ID already exists."
                            : "Could not save the job."
            );
        }
    }
      // Check the form and build the job. This does not write the file.
    public Job createJob() throws Exception {
        validateFields();
            return new Job(
                    trim(clientId),
                trim(jobId),
                Integer.parseInt(duration.trim()),
                   trim(deadline),
                Integer.parseInt(priority.trim()),
                  trim(jobAvailability),
                trim(startTime),
                trim(endTime)
        );
       }

    // Check whether the job ID already exists.
    private boolean jobAlreadySaved(
            VehicularCloudController controller,
            String jobId) {

        for (Job existing : controller.getJobs()) {

            if (jobId.equals(existing.getJobId())) {
                return true;
            }
        }

        return false;
    }

    // Validate all form fields.
    private void validateFields() throws Exception {

        // Required fields.
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

        if (isEmpty(priority)) {
            throw new Exception("Job priority is required.");
        }

        // Validate priority.
        int priorityValue;

        try {
            priorityValue = Integer.parseInt(priority.trim());
        } catch (NumberFormatException ex) {
            throw new Exception("Priority must be a positive whole number.");
        }
        if (priorityValue <= 0) {
            throw new Exception("Priority must be greater than zero.");
        }
        // Validate duration.
        int durationValue;
        try {
            durationValue = Integer.parseInt(duration.trim());
        } catch (NumberFormatException ex) {
            throw new Exception(
                    "Duration must be a valid whole number of days."
            );
        }
        if (durationValue <= 0) {
            throw new Exception("Duration must be greater than zero.");
        }
        // Strictly validate MM-dd-yyyy dates.
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd-uuuu").withResolverStyle(ResolverStyle.STRICT);
        LocalDate availabilityDate;
        LocalDate deadlineDate;
        try {
            availabilityDate = LocalDate.parse(jobAvailability.trim(), dateFormatter);
        } catch (DateTimeParseException ex) {
            throw new Exception("Invalid availability date. Use MM-dd-yyyy.");
        }
        try {
            deadlineDate = LocalDate.parse(deadline.trim(), dateFormatter);
        } catch (DateTimeParseException ex) {
            throw new Exception("Invalid deadline date. Use MM-dd-yyyy.");
        }
        // Dates cannot be in the past.
        LocalDate today = LocalDate.now();
        if (availabilityDate.isBefore(today)) {
            throw new Exception("Job availability cannot be in the past.");
        }
        if (deadlineDate.isBefore(today)) {
            throw new Exception("Job deadline cannot be in the past.");
        }
        // Preserve the existing rule: deadline date must be later.
        if (!deadlineDate.isAfter(availabilityDate)) {
            throw new Exception("Deadline must be after job availability.");
        }
        // Validate the start and end times.
        validateTimes(availabilityDate, deadlineDate);
        // Calculate duration using calendar days.
        long minimumDuration = ChronoUnit.DAYS.between(availabilityDate, deadlineDate);
        if (durationValue > minimumDuration) {
            throw new Exception("Duration cannot exceed " + minimumDuration + " days between availability and deadline."
            );
        }
    }

    // Validate the time fields and compare the complete date/time values.
    private void validateTimes(
            LocalDate availabilityDate,
            LocalDate deadlineDate) throws Exception {

        boolean startMissing = isEmpty(startTime);
        boolean endMissing = isEmpty(endTime);

        // Older constructors may not supply times.
        if (startMissing && endMissing) {
            return;
        }
        if (startMissing || endMissing) {
            throw new Exception(
                    "Both start time and end time are required."
            );
        }
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

        LocalTime parsedStartTime;
        LocalTime parsedEndTime;

        try {
            parsedStartTime = LocalTime.parse(
                    startTime.trim(),
                    timeFormatter
            );
        } catch (DateTimeParseException ex) {
            throw new Exception(
                    "Invalid start time. Use the format hh:mm AM/PM."
            );
        }
        try {
            parsedEndTime = LocalTime.parse(
                    endTime.trim(),
                    timeFormatter
            );
        } catch (DateTimeParseException ex) {
            throw new Exception(
                    "Invalid end time. Use the format hh:mm AM/PM."
            );
        }
        LocalDateTime startDateTime = LocalDateTime.of(availabilityDate, parsedStartTime);
        LocalDateTime endDateTime = LocalDateTime.of(deadlineDate, parsedEndTime);
        if (!endDateTime.isAfter(startDateTime)) {
            throw new Exception(
                    "End date and time must be after start date and time."
            );
        }
    }
    // Check whether a value is empty.
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    // Remove extra spaces and safely handle null.
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
