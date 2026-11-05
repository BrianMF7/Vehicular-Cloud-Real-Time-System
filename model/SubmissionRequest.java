package model;

import java.io.Serializable;
import java.util.UUID;

/*
 * A message sent by a client or vehicle owner to the VC Controller server.
 * The server decides whether the submitted job or vehicle should be saved.
 */
public final class SubmissionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final RequestType type;
    private final Job job;
    private final Vehicle vehicle;
    private final String ownerId;
    private final double computePower;
    private final String requesterName;
    private final String requesterEmail;
    private final long submittedAtMillis;

    private SubmissionRequest(RequestType type, Job job, Vehicle vehicle, String ownerId,
            double computePower, String requesterName, String requesterEmail) {
        this.requestId = UUID.randomUUID().toString();
        this.type = type;
        this.job = job;
        this.vehicle = vehicle;
        this.ownerId = clean(ownerId);
        this.computePower = computePower;
        this.requesterName = clean(requesterName);
        this.requesterEmail = clean(requesterEmail);
        this.submittedAtMillis = System.currentTimeMillis();
    }

    /* Creates a request to submit a job for server approval. */
    public static SubmissionRequest forJob(Job job, String requesterName, String requesterEmail) {
        if (job == null) {
            throw new IllegalArgumentException("Job is required.");
        }
        return new SubmissionRequest(RequestType.JOB, job, null, "", 0,
                requesterName, requesterEmail);
    }

    /* Creates a request to register a vehicle for server approval. */
    public static SubmissionRequest forVehicle(Vehicle vehicle, String ownerId, double computePower,
            String requesterName, String requesterEmail) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle is required.");
        }
        return new SubmissionRequest(RequestType.VEHICLE, null, vehicle, ownerId, computePower,
                requesterName, requesterEmail);
    }

    public String getRequestId() {
        return requestId;
    }

    public RequestType getType() {
        return type;
    }

    public Job getJob() {
        return job;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public double getComputePower() {
        return computePower;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public long getSubmittedAtMillis() {
        return submittedAtMillis;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
