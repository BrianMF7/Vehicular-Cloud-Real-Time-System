package controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Job;
import model.Vehicle;
import scheduling.PriorityScheduler;
import scheduling.ScheduleResult;
import storage.FileManager;

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
    // True when a car is already working on a job.
    private final Map<String, Boolean> inUseByVehicleId;
    // Which car is doing which job.
    private final Map<String, String> vehicleIdByJobId;

    private final FileManager fileManager;
    private final PriorityScheduler scheduler;
    private ScheduleResult lastSchedule;
    // When we are reading the log we should not write it again.
    private boolean loading;

    public static VehicularCloudController getInstance() {
        if (instance == null) {
            instance = new VehicularCloudController();
        }
        return instance;
    }

    public VehicularCloudController() {
        this("VCC-1", "Vehicular Cloud Controller");
    }

    public VehicularCloudController(String controllerID, String name) {
        this.controllerID = controllerID;
        this.name = name;
        this.totalAvailableCompute = 0;
        this.vehicles = new ArrayList<Vehicle>();
        this.jobs = new ArrayList<Job>();
        this.computeByVehicleId = new HashMap<String, Double>();
        this.inUseByVehicleId = new HashMap<String, Boolean>();
        this.vehicleIdByJobId = new HashMap<String, String>();
        this.fileManager = FileManager.getInstance();
        this.scheduler = new PriorityScheduler();
        this.lastSchedule = scheduler.schedule(new ArrayList<Job>());
        this.loading = false;
        loadSavedData();
    }

    public String getControllerID() {
        return controllerID;
    }

    public String getName() {
        return name;
    }

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
        vehicleIdByJobId.put(jobID, vehicle.getVehicleId());
        inUseByVehicleId.put(vehicle.getVehicleId(), Boolean.TRUE);
        refreshAvailableCompute();
        refreshSchedule();
        saveLine("ASSIGN|" + jobID + "|" + vehicle.getVehicleId());
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
        if (vehicle == null || isBlank(vehicle.getVehicleId())) {
            return false;
        }
        if (findVehicle(vehicle.getVehicleId()) != null) {
            return false;
        }
        if (compute < 0) {
            return false;
        }
        vehicles.add(vehicle);
        computeByVehicleId.put(vehicle.getVehicleId(), Double.valueOf(compute));
        inUseByVehicleId.put(vehicle.getVehicleId(), Boolean.FALSE);
        refreshAvailableCompute();
        saveLine("VEHICLE|" + vehicle.getVehicleId() + "|" + safe(vehicle.getMake())
                + "|" + safe(vehicle.getModel()) + "|" + vehicle.getYear()
                + "|" + safe(vehicle.getArrivalTime()) + "|" + safe(vehicle.getDepartureTime())
                + "|" + compute);
        return true;
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
        vehicles.remove(vehicle);
        computeByVehicleId.remove(vehicleID);
        inUseByVehicleId.remove(vehicleID);
        refreshAvailableCompute();
        saveLine("DEREGISTER|" + vehicleID);
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
        if (job == null || isBlank(job.getJobId())) {
            return false;
        }
        if (findJob(job.getJobId()) != null) {
            return false;
        }
        if (job.getEstimatedDuration() <= 0) {
            return false;
        }
        jobs.add(job);
        refreshSchedule();
        saveLine("JOB|" + safe(job.getClientId()) + "|" + job.getJobId()
                + "|" + job.getEstimatedDuration() + "|" + safe(job.getDeadline())
                + "|" + job.getPriority());
        return true;
    }

    public List<Job> getJobs() {
        return new ArrayList<Job>(jobs);
    }

    public List<Vehicle> getVehicles() {
        return new ArrayList<Vehicle>(vehicles);
    }

    // Ask the scheduler for completion times and average waiting time.
    public ScheduleResult scheduleJobs() {
        refreshSchedule();
        return lastSchedule;
    }

    public double getAverageWaitingTime() {
        refreshSchedule();
        return lastSchedule.getAverageWaitingTime();
    }

    public String getAssignedVehicleId(String jobID) {
        return vehicleIdByJobId.get(jobID);
    }

    private void refreshSchedule() {
        lastSchedule = scheduler.schedule(new ArrayList<Job>(jobs));
    }

    private void refreshAvailableCompute() {
        double total = 0;
        for (Vehicle vehicle : vehicles) {
            if (!isInUse(vehicle.getVehicleId())) {
                total = total + getCompute(vehicle.getVehicleId());
            }
        }
        totalAvailableCompute = total;
    }

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

    private boolean isInUse(String vehicleId) {
        Boolean inUse = inUseByVehicleId.get(vehicleId);
        return inUse != null && inUse.booleanValue();
    }

    private double getCompute(String vehicleId) {
        Double value = computeByVehicleId.get(vehicleId);
        if (value == null) {
            return 0;
        }
        return value.doubleValue();
    }

    private void saveLine(String record) {
        if (loading) {
            return;
        }
        try {
            fileManager.saveRecord(record);
        } catch (Exception ex) {
            System.err.println("Could not save: " + ex.getMessage());
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

    private void loadRecord(String[] parts, String type) {
        if ("OWNER".equals(type) || "VEHICLE".equals(type)) {
            loadVehicle(parts, type);
        } else if ("CLIENT".equals(type) || "JOB".equals(type)) {
            loadJob(parts, type);
        } else if ("DEREGISTER".equals(type) && parts.length >= 3) {
            String vehicleId = parts[parts.length - 1];
            Vehicle vehicle = findVehicle(vehicleId);
            if (vehicle != null && !isInUse(vehicleId)) {
                vehicles.remove(vehicle);
                computeByVehicleId.remove(vehicleId);
                inUseByVehicleId.remove(vehicleId);
            }
        } else if ("RELEASE".equals(type) && parts.length >= 3) {
            Vehicle vehicle = findVehicle(parts[parts.length - 1]);
            if (vehicle != null) {
                inUseByVehicleId.put(vehicle.getVehicleId(), Boolean.FALSE);
            }
        }
    }

    private void loadVehicle(String[] parts, String type) {
        try {
            String vehicleId;
            String make;
            String model;
            int year;
            String arrival;
            String departure;
            double compute;
            if ("VEHICLE".equals(type) && parts.length >= 9) {
                vehicleId = parts[2];
                make = parts[3];
                model = parts[4];
                year = Integer.parseInt(parts[5].trim());
                arrival = parts[6];
                departure = parts[7];
                compute = parseCompute(parts[8]);
            } else if ("OWNER".equals(type) && parts.length >= 12) {
                vehicleId = parts[5];
                make = parts[6];
                model = parts[7];
                year = Integer.parseInt(parts[8].trim());
                arrival = parts[9];
                departure = parts[10];
                compute = parseCompute(parts[11]);
            } else if ("OWNER".equals(type) && parts.length >= 10) {
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
            inUseByVehicleId.put(vehicleId, Boolean.FALSE);
        } catch (NumberFormatException ex) {
            // Skip a line that does not have a real year or compute value.
        }
    }

    private void loadJob(String[] parts, String type) {
        try {
            String clientId;
            String jobId;
            int duration;
            String deadline;
            int priority = 1;
            if ("JOB".equals(type) && parts.length >= 7) {
                clientId = parts[2];
                jobId = parts[3];
                duration = Integer.parseInt(parts[4].trim());
                deadline = parts[5];
                priority = Integer.parseInt(parts[6].trim());
            } else if ("CLIENT".equals(type) && parts.length >= 9) {
                clientId = parts[4];
                jobId = parts[5];
                duration = Integer.parseInt(parts[7].trim());
                deadline = parts[8];
            } else if ("CLIENT".equals(type) && parts.length >= 7) {
                clientId = parts[2];
                jobId = parts[3];
                duration = Integer.parseInt(parts[5].trim());
                deadline = parts[6];
            } else if ("CLIENT".equals(type) && parts.length >= 6) {
                clientId = parts[2];
                jobId = parts[3];
                duration = Integer.parseInt(parts[4].trim());
                deadline = parts[5];
            } else {
                return;
            }
            if (isBlank(jobId) || findJob(jobId) != null || duration <= 0 || priority <= 0) {
                return;
            }
            jobs.add(new Job(clientId, jobId, duration, deadline, priority));
        } catch (NumberFormatException ex) {
            // Skip a line that does not have a real duration.
        }
    }

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

    private static String recordType(String[] parts) {
        if (parts.length >= 2 && isKnownType(parts[1])) {
            return parts[1];
        }
        if (parts.length >= 1 && isKnownType(parts[0])) {
            return parts[0];
        }
        return "";
    }

    private static boolean isKnownType(String value) {
        return "OWNER".equals(value) || "CLIENT".equals(value) || "VEHICLE".equals(value)
                || "JOB".equals(value) || "ASSIGN".equals(value) || "RELEASE".equals(value)
                || "DEREGISTER".equals(value);
    }

    private static double parseCompute(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return Double.parseDouble(text.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
