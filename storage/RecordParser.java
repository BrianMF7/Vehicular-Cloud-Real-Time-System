package storage;

import java.util.ArrayList;
import java.util.List;

import model.Job;
import model.Vehicle;

// Changes saved log lines into Job and Vehicle objects.
public class RecordParser {

    // Parse a JOB line:
    // timestamp|JOB|username|email|clientId|jobId|availability|duration|deadline|priority
    public static Job parseJobRecord(String recordLine) {
        if (recordLine == null || !recordLine.contains("JOB|")) {
            return null;
        }

        try {
            String[] parts = recordLine.split("\\|", -1);
            int jobIndex = findRecordType(parts, "JOB");
            if (jobIndex < 0) {
                return null;
            }
            int dataIndex = jobIndex + 1;
            if (parts.length != dataIndex + 8) {
                return null;
            }
            String clientId = parts[dataIndex + 2].trim();
            String jobId = parts[dataIndex + 3].trim();
            String availability = parts[dataIndex + 4].trim();
            int duration = Integer.parseInt(parts[dataIndex + 5].trim());
            String deadline = parts[dataIndex + 6].trim();
            int priority = Integer.parseInt(parts[dataIndex + 7].trim());
            if (isBlank(jobId) || duration <= 0 || priority <= 0) {
                return null;
            }
            return new Job(clientId, jobId, duration, deadline, priority, availability);
        } catch (Exception ex) {
            // A bad saved record is ignored so the rest of the log can still load.
        }
        return null;
    }

    // Parse an OWNER line into a Vehicle object.
    public static Vehicle parseOwnerRecord(String recordLine) {
        if (recordLine == null || !recordLine.contains("OWNER|")) {
            return null;
        }

        try {
            String[] parts = recordLine.split("\\|", -1);
            int ownerIndex = findRecordType(parts, "OWNER");
            if (ownerIndex == -1) {
                return null;
            }
            int dataIndex = ownerIndex + 1;

            // timestamp|OWNER|username|email|ownerID|vehicleID|make|model|year|arrival|departure|computePower
            if (parts.length >= dataIndex + 9) {
                String vehicleId = parts[dataIndex + 3].trim();
                String make = parts[dataIndex + 4].trim();
                String model = parts[dataIndex + 5].trim();
                int year = Integer.parseInt(parts[dataIndex + 6].trim());
                String arrival = parts[dataIndex + 7].trim();
                String departure = parts[dataIndex + 8].trim();
                return new Vehicle(vehicleId, make, model, year, arrival, departure);
            }
        } catch (Exception ex) {
            // A bad saved record is ignored so the rest of the log can still load.
        }
        return null;
    }

    // Load jobs from the real application log.
    public static List<Job> loadAllJobs() {
        return loadAllJobs(FileManager.getInstance());
    }

    // Load jobs from a supplied file manager. Tests use this with a temporary file.
    public static List<Job> loadAllJobs(FileManager fileManager) {
        List<Job> jobs = new ArrayList<Job>();
        if (fileManager == null) {
            return jobs;
        }

        try {
            for (String record : fileManager.loadJobRecords()) {
                Job job = parseJobRecord(record);
                if (job != null) {
                    jobs.add(job);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error loading jobs: " + ex.getMessage());
        }
        return jobs;
    }

    // Load vehicles from the real application log.
    public static List<Vehicle> loadAllVehicles() {
        return loadAllVehicles(FileManager.getInstance());
    }

    // Load vehicles from a supplied file manager. Tests use this with a temporary file.
    public static List<Vehicle> loadAllVehicles(FileManager fileManager) {
        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        if (fileManager == null) {
            return vehicles;
        }

        try {
            for (String record : fileManager.loadOwnerRecords()) {
                Vehicle vehicle = parseOwnerRecord(record);
                if (vehicle != null) {
                    vehicles.add(vehicle);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error loading vehicles: " + ex.getMessage());
        }
        return vehicles;
    }

    private static int findRecordType(String[] parts, String type) {
        for (int index = 0; index < parts.length; index++) {
            if (type.equals(parts[index])) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
