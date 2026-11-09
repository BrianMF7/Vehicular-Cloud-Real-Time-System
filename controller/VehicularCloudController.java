package controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.*;
import scheduling.PriorityScheduler;
import scheduling.ScheduleResult;
import storage.FileManager;
import storage.RecordParser;

// The cloud controller keeps the list of cars and jobs.
// It saves records through FileManager and uses PriorityScheduler
// to get completion times and average waiting time.
public class VehicularCloudController {
    private static VehicularCloudController instance;

    private String controllerID;
    private String name;
    private double totalAvailableCompute;

    // Cars currently in the cloud.
    private final List<Vehicle> vehicles;
    // Jobs that have been submitted.
    private final List<Job> jobs;
    // Compute power for each car. The Vehicle class does not store this yet.
    private final Map<String, Double> computeByVehicleId;
    // Owner name and signed-in email for each car.
    private final Map<String, String> ownerByVehicleId;
    private final Map<String, String> emailByVehicleId;
    // True when a car is already working on a job.
    private final Map<String, Boolean> inUseByVehicleId;
    // Which car is doing which job.
    private final Map<String, String> vehicleIdByJobId;
    // Signed-in name and email stored with each job, like owner info on cars.
    private final Map<String, String> usernameByJobId;
    private final Map<String, String> emailByJobId;

    private final FileManager fileManager;
    private final PriorityScheduler scheduler;
    private ScheduleResult lastSchedule;
    // When we are reading the log we should not write it again.
    private boolean loading;

    // Shared controller used by the car form, job form, and results screen.
    public static VehicularCloudController getInstance() {
        if (instance == null) {
            instance = new VehicularCloudController();
        }
        return instance;
    }

    // Default controller used by the application.
    public VehicularCloudController() {
        this("VCC-1", "Vehicular Cloud Controller");
    }

    // Builds a controller with a custom id and name, then loads saved cars and jobs.
    public VehicularCloudController(String controllerID, String name) {
        this.controllerID = controllerID;
        this.name = name;
        this.totalAvailableCompute = 0;
        this.vehicles = new ArrayList<Vehicle>();
        this.jobs = new ArrayList<Job>();
        this.computeByVehicleId = new HashMap<String, Double>();
        this.ownerByVehicleId = new HashMap<String, String>();
        this.emailByVehicleId = new HashMap<String, String>();
        this.inUseByVehicleId = new HashMap<String, Boolean>();
        this.vehicleIdByJobId = new HashMap<String, String>();
        this.usernameByJobId = new HashMap<String, String>();
        this.emailByJobId = new HashMap<String, String>();
        this.fileManager = FileManager.getInstance();
        this.scheduler = new PriorityScheduler();
        this.lastSchedule = scheduler.schedule(new ArrayList<Job>());
        this.loading = false;
        loadSavedData();
    }

    // Id shown in the system status text.
    public String getControllerID() {
        return controllerID;
    }

    // Display name shown in the system status text.
    public String getName() {
        return name;
    }

    // Compute from cars that are not currently assigned a job.
    public double getTotalAvailableCompute() {
        return totalAvailableCompute;
    }

    // Give a waiting job to a free car. Returns false if we cannot do it.
    public boolean assignJob(String jobID) {
        Job job = findJob(jobID);
        if (job == null) {
            return false;
        }
        if (vehicleIdByJobId.containsKey(jobID)) {
            return false;
        }
        Vehicle vehicle = findAvailableVehicle(Double.valueOf(0));
        if (vehicle == null) {
            return false;
        }
        if (!saveLine("ASSIGN|" + jobID + "|" + vehicle.getVehicleId())) {
            return false;
        }
        vehicleIdByJobId.put(jobID, vehicle.getVehicleId());
        inUseByVehicleId.put(vehicle.getVehicleId(), Boolean.TRUE);
        refreshAvailableCompute();
        refreshSchedule();
        return true;
    }

    // Recalculate waiting and completion times for this job.
    public void monitorJob(String jobID) {
        if (findJob(jobID) == null) {
            return;
        }
        refreshSchedule();
    }

    // Short summary of cars, jobs, compute, and average wait time.
    public String getSystemStatus() {
        refreshSchedule();
        int availableCount = getAvailableVehicles().size();
        return "Controller: " + name + " (" + controllerID + ")\n"
                + "Vehicles: " + vehicles.size() + " (" + availableCount + " available)\n"
                + "Jobs: " + jobs.size() + " (" + vehicleIdByJobId.size() + " assigned)\n"
                + "Total available compute: " + totalAvailableCompute + "\n"
                + "Average waiting time: " + lastSchedule.getAverageWaitingTime();
    }

    // Add a car. Compute is treated as 0 if the caller does not pass it.
    public boolean registerVehicle(Vehicle vehicle) {
        return registerVehicle(vehicle, 0);
    }

    // Add a car and remember how much compute it offers.
    public boolean registerVehicle(Vehicle vehicle, double compute) {
        return registerVehicle(vehicle, compute, "", "");
    }

    // Add a car and remember who registered it. The car form uses this method.
    public boolean registerVehicle(Vehicle vehicle, double compute, String ownerId, String accountEmail) {
        if (vehicle == null || isBlank(vehicle.getVehicleId())) {
            return false;
        }
        if (findVehicle(vehicle.getVehicleId()) != null) {
            return false;
        }
        if (!Double.isFinite(compute) || compute < 0) {
            return false;
        }
        String vehicleId = vehicle.getVehicleId();
        if (!saveLine("VEHICLE|" + vehicleId + "|" + safe(vehicle.getMake())
                + "|" + safe(vehicle.getModel()) + "|" + vehicle.getYear()
                + "|" + safe(vehicle.getArrivalTime()) + "|" + safe(vehicle.getDepartureTime())
                + "|" + compute + "|" + safe(ownerId) + "|" + safe(accountEmail))) {
            return false;
        }
        vehicles.add(vehicle);
        computeByVehicleId.put(vehicleId, Double.valueOf(compute));
        ownerByVehicleId.put(vehicleId, safe(ownerId));
        emailByVehicleId.put(vehicleId, safe(accountEmail));
        inUseByVehicleId.put(vehicleId, Boolean.FALSE);
        refreshAvailableCompute();
        return true;
    }

    // Owner id stored with the car, or an empty string if none was saved.
    public String getVehicleOwnerId(String vehicleId) {
        String owner = ownerByVehicleId.get(vehicleId);
        return owner == null ? "" : owner;
    }

    // Account email stored with the car, or an empty string if none was saved.
    public String getVehicleAccountEmail(String vehicleId) {
        String email = emailByVehicleId.get(vehicleId);
        return email == null ? "" : email;
    }

    // Take a car out of the cloud. Busy cars have to be released first.
    public boolean deregisterVehicle(String vehicleID) {
        Vehicle vehicle = findVehicle(vehicleID);
        if (vehicle == null) {
            return false;
        }
        if (isInUse(vehicleID)) {
            return false;
        }
        if (!saveLine("DEREGISTER|" + vehicleID)) {
            return false;
        }
        vehicles.remove(vehicle);
        computeByVehicleId.remove(vehicleID);
        ownerByVehicleId.remove(vehicleID);
        emailByVehicleId.remove(vehicleID);
        inUseByVehicleId.remove(vehicleID);
        refreshAvailableCompute();
        return true;
    }

    // First free car that has at least this much compute.
    public Vehicle findAvailableVehicle(Double requiredCompute) {
        double needed = 0;
        if (requiredCompute != null) {
            needed = requiredCompute.doubleValue();
        }
        for (Vehicle vehicle : vehicles) {
            if (!isInUse(vehicle.getVehicleId()) && getCompute(vehicle.getVehicleId()) >= needed) {
                return vehicle;
            }
        }
        return null;
    }

    // Mark a car as free again so another job can use it.
    public void releaseVehicle(Vehicle vehicle) {
        if (vehicle == null || findVehicle(vehicle.getVehicleId()) == null) {
            return;
        }
        String vehicleId = vehicle.getVehicleId();
        List<String> finishedJobs = new ArrayList<String>();
        for (Map.Entry<String, String> entry : vehicleIdByJobId.entrySet()) {
            if (vehicleId.equals(entry.getValue())) {
                finishedJobs.add(entry.getKey());
            }
        }
        for (String jobId : finishedJobs) {
            vehicleIdByJobId.remove(jobId);
        }
        inUseByVehicleId.put(vehicleId, Boolean.FALSE);
        refreshAvailableCompute();
        saveLine("RELEASE|" + vehicleId);
    }

    // Cars that are not currently assigned a job.
    public List<Vehicle> getAvailableVehicles() {
        List<Vehicle> available = new ArrayList<Vehicle>();
        for (Vehicle vehicle : vehicles) {
            if (!isInUse(vehicle.getVehicleId())) {
                available.add(vehicle);
            }
        }
        return available;
    }

    // Put a new job in the list so it can be assigned later.
    public boolean submitJob(Job job) {
        return submitJob(job, "", "");
    }

    // Save the job once, including the signed-in account. Returns false if the log write fails.

    public boolean submitJob(Job job, String username, String email) {
        if (job == null || isBlank(job.getJobId())) {
            return false;
        }
        if (findJob(job.getJobId()) != null) {
            return false;
        }
        if (job.getEstimatedDuration() <= 0) {
            return false;
        }
        String jobId = job.getJobId();

        // Save the job, including its start and end times.
        // Save the job using the updated field order.
        if (!saveLine("JOB|" + safe(username)
                + "|" + safe(email)
                + "|" + safe(job.getClientId())
                + "|" + safe(jobId)
                + "|" + safe(job.getAvailability())
                + "|" + safe(job.getStartTime())
                + "|" + job.getEstimatedDuration()
                + "|" + safe(job.getDeadline())
                + "|" + safe(job.getEndTime())
                + "|" + job.getPriority())) {

            return false;
        }

        jobs.add(job);
        usernameByJobId.put(jobId, safe(username));
        emailByJobId.put(jobId, safe(email));
        refreshSchedule();

        return true;
    }

    //This is to help store the job or car only when the controller accepts the request.
    public ServerResponse decideRequest(SubmissionRequest request, boolean accept) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required.");
        }
        if (!accept) {
            return new ServerResponse(request.getRequestId(), RequestStatus.REJECTED,
                    "Request rejected. Nothing was saved.");
        }
        boolean saved = false;
        if (request.getType() == RequestType.JOB) {
            saved = submitJob(request.getJob(), request.getRequesterName(), request.getRequesterEmail());
        } else if (request.getType() == RequestType.VEHICLE) {
            saved = registerVehicle(request.getVehicle(), request.getComputePower(),
                    request.getOwnerId(), request.getRequesterEmail());
        }
        if (!saved) {
            return new ServerResponse(request.getRequestId(), RequestStatus.ERROR,
                    "Could not save the request.");
        }
        return new ServerResponse(request.getRequestId(), RequestStatus.ACCEPTED,
                "Request accepted and stored.");
    }
    // Signed-in name stored with the job, or an empty string if none was saved.
    public String getJobAccountName(String jobId) {
        String username = usernameByJobId.get(jobId);
        return username == null ? "" : username;
    }

    // Signed-in email stored with the job, or an empty string if none was saved.
    public String getJobAccountEmail(String jobId) {
        String email = emailByJobId.get(jobId);
        return email == null ? "" : email;
    }

    // Copy of the submitted jobs so callers cannot change the real list.
    public List<Job> getJobs() {
        return new ArrayList<Job>(jobs);
    }

    // Copy of the registered cars so callers cannot change the real list.
    public List<Vehicle> getVehicles() {
        return new ArrayList<Vehicle>(vehicles);
    }

    // Ask the scheduler for completion times and average waiting time.
    public ScheduleResult scheduleJobs() {
        refreshSchedule();
        return lastSchedule;
    }

    // Average waiting time from the latest schedule.
    public double getAverageWaitingTime() {
        refreshSchedule();
        return lastSchedule.getAverageWaitingTime();
    }

    // Car assigned to this job, or null if the job is still waiting.
    public String getAssignedVehicleId(String jobID) {
        return vehicleIdByJobId.get(jobID);
    }

    // Ask the scheduler for a new set of waiting and completion times.
    private void refreshSchedule() {
        lastSchedule = scheduler.schedule(new ArrayList<Job>(jobs));
    }

    // Add up compute from cars that are not busy.
    private void refreshAvailableCompute() {
        double total = 0;
        for (Vehicle vehicle : vehicles) {
            if (!isInUse(vehicle.getVehicleId())) {
                total = total + getCompute(vehicle.getVehicleId());
            }
        }
        totalAvailableCompute = total;
    }

    // Find a registered car by id.
    private Vehicle findVehicle(String vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        for (Vehicle vehicle : vehicles) {
            if (vehicleId.equals(vehicle.getVehicleId())) {
                return vehicle;
            }
        }
        return null;
    }

    // Find a submitted job by id.
    private Job findJob(String jobId) {
        if (jobId == null) {
            return null;
        }
        for (Job job : jobs) {
            if (jobId.equals(job.getJobId())) {
                return job;
            }
        }
        return null;
    }

    // True when a car is already working on a job.
    private boolean isInUse(String vehicleId) {
        Boolean inUse = inUseByVehicleId.get(vehicleId);
        return inUse != null && inUse.booleanValue();
    }

    // Compute stored for this car, or 0 if none was saved.
    private double getCompute(String vehicleId) {
        Double value = computeByVehicleId.get(vehicleId);
        if (value == null) {
            return 0;
        }
        return value.doubleValue();
    }

    // Append one record through FileManager unless we are still loading the log.
    // Returns false when the file write fails so callers do not pretend it saved.
    private boolean saveLine(String record) {
        if (loading) {
            return true;
        }
        try {
            fileManager.saveRecord(record);
            return true;
        } catch (Exception ex) {
            System.err.println("Could not save: " + ex.getMessage());
            return false;
        }
    }

    // Read cars and jobs that were already written to the log file.
    private void loadSavedData() {
        Path path = Paths.get("vehicular_cloud_log.txt");
        if (!Files.exists(path)) {
            return;
        }
        loading = true;
        try {
            List<String> lines = Files.readAllLines(path);
            List<String> assignLines = new ArrayList<String>();
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                String type = recordType(parts);
                if ("ASSIGN".equals(type)) {
                    assignLines.add(line);
                } else {
                    loadRecord(parts, type);
                }
            }
            for (String line : assignLines) {
                loadAssign(line.split("\\|", -1));
            }
            refreshAvailableCompute();
            if (!jobs.isEmpty()) {
                refreshSchedule();
            }
        } catch (IOException ex) {
            System.err.println("Could not load saved records.");
        } finally {
            loading = false;
        }
    }

    // Apply one saved line, except ASSIGN which is handled after cars and jobs exist.
    private void loadRecord(String[] parts, String type) {
        if ("OWNER".equals(type) || "VEHICLE".equals(type)) {
            loadVehicle(parts, type);
        } else if ("JOB".equals(type)) {
            loadJob(parts);
        } else if ("DEREGISTER".equals(type) && parts.length >= 3) {
            String vehicleId = parts[parts.length - 1];
            Vehicle vehicle = findVehicle(vehicleId);
            if (vehicle != null && !isInUse(vehicleId)) {
                vehicles.remove(vehicle);
                computeByVehicleId.remove(vehicleId);
                ownerByVehicleId.remove(vehicleId);
                emailByVehicleId.remove(vehicleId);
                inUseByVehicleId.remove(vehicleId);
            }
        } else if ("RELEASE".equals(type) && parts.length >= 3) {
            Vehicle vehicle = findVehicle(parts[parts.length - 1]);
            if (vehicle != null) {
                inUseByVehicleId.put(vehicle.getVehicleId(), Boolean.FALSE);
            }
        }
    }

    // Rebuild a car from a VEHICLE or OWNER line, including owner and email when present.
    private void loadVehicle(String[] parts, String type) {
        try {
            String vehicleId;
            String make;
            String model;
            int year;
            String arrival;
            String departure;
            double compute;
            String owner = "";
            String email = "";
            if ("VEHICLE".equals(type) && parts.length >= 9) {
                vehicleId = parts[2];
                make = parts[3];
                model = parts[4];
                year = Integer.parseInt(parts[5].trim());
                arrival = parts[6];
                departure = parts[7];
                compute = parseCompute(parts[8]);
                if (parts.length >= 11) {
                    owner = parts[9];
                    email = parts[10];
                }
            } else if ("OWNER".equals(type) && parts.length >= 12) {
                email = parts[3];
                owner = parts[4];
                vehicleId = parts[5];
                make = parts[6];
                model = parts[7];
                year = Integer.parseInt(parts[8].trim());
                arrival = parts[9];
                departure = parts[10];
                compute = parseCompute(parts[11]);
            } else if ("OWNER".equals(type) && parts.length >= 10) {
                owner = parts[2];
                vehicleId = parts[3];
                make = parts[4];
                model = parts[5];
                year = Integer.parseInt(parts[6].trim());
                arrival = parts[7];
                departure = parts[8];
                compute = parseCompute(parts[9]);
            } else {
                return;
            }
            if (isBlank(vehicleId) || findVehicle(vehicleId) != null) {
                return;
            }
            vehicles.add(new Vehicle(vehicleId, make, model, year, arrival, departure));
            computeByVehicleId.put(vehicleId, Double.valueOf(compute));
            ownerByVehicleId.put(vehicleId, safe(owner));
            emailByVehicleId.put(vehicleId, safe(email));
            inUseByVehicleId.put(vehicleId, Boolean.FALSE);
        } catch (NumberFormatException ex) {
            // Skip a line that does not have a real year or compute value.
        }
    }

    // Rebuild a job from a JOB line.
    private void loadJob(String[] parts) {
        Job parsed = RecordParser.parseJobRecord(String.join("|", parts));
        if (parsed == null || findJob(parsed.getJobId()) != null) {
            return;
        }
        rememberJob(parsed, jobField(parts, 0), jobField(parts, 1));
    }

    // Keep the job and the account that submitted it.
    private void rememberJob(Job job, String username, String email) {
        jobs.add(job);
        usernameByJobId.put(job.getJobId(), safe(username));
        emailByJobId.put(job.getJobId(), safe(email));
    }

    // Field after JOB: 0 is username, 1 is email.
    private static String jobField(String[] parts, int offset) {
        int jobIndex = indexOfType(parts, "JOB");
        int index = jobIndex + 1 + offset;
        if (jobIndex < 0 || index >= parts.length) {
            return "";
        }
        return parts[index];
    }

    // Index of a record kind in a split log line.
    private static int indexOfType(String[] parts, String type) {
        for (int i = 0; i < parts.length; i++) {
            if (type.equals(parts[i])) {
                return i;
            }
        }
        return -1;
    }

    // Mark a car busy after its job has been loaded.
    private void loadAssign(String[] parts) {
        if (parts.length < 4) {
            return;
        }
        String jobId = parts[2];
        String vehicleId = parts[3];
        if (findJob(jobId) == null || findVehicle(vehicleId) == null) {
            return;
        }
        if (vehicleIdByJobId.containsKey(jobId) || isInUse(vehicleId)) {
            return;
        }
        vehicleIdByJobId.put(jobId, vehicleId);
        inUseByVehicleId.put(vehicleId, Boolean.TRUE);
    }

    // Record kind from a timestamped line (second field) or a line that starts with the kind.
    private static String recordType(String[] parts) {
        if (parts.length >= 2 && isKnownType(parts[1])) {
            return parts[1];
        }
        if (parts.length >= 1 && isKnownType(parts[0])) {
            return parts[0];
        }
        return "";
    }

    // True for the record kinds this controller knows how to load.
    private static boolean isKnownType(String value) {
        return "OWNER".equals(value) || "VEHICLE".equals(value)
                || "JOB".equals(value) || "ASSIGN".equals(value) || "RELEASE".equals(value)
                || "DEREGISTER".equals(value);
    }

    // Parse compute from a saved field. Blank values are treated as 0.
    private static double parseCompute(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return Double.parseDouble(text.trim());
    }

    // True when the text is missing or only spaces.
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // Trim saved text, or use an empty string when the value is missing.
    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
