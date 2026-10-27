package storage;

import java.util.ArrayList;
import java.util.List;

import model.Job;
import model.Vehicle;

// Changes saved log lines into Job and Vehicle objects.
public class RecordParser {

    // Parse a CLIENT line from either the current GUI format or an older format.
    public static Job parseClientRecord(String recordLine) {
        if (recordLine == null || !recordLine.contains("CLIENT|")) {
            return null;
        }

        try {
            String[] parts = recordLine.split("\\|", -1);
            int clientIndex = findRecordType(parts, "CLIENT");
            if (clientIndex == -1) {
                return null;
            }
            int dataIndex = clientIndex + 1;

            // Current GUI format:
            // timestamp|CLIENT|username|email|clientID|jobID|availability|duration|deadline|priority
            if (parts.length >= dataIndex + 7) {
                String clientId = parts[dataIndex + 2].trim();
                String jobId = parts[dataIndex + 3].trim();
                int duration = Integer.parseInt(parts[dataIndex + 5].trim());
                String deadline = parts[dataIndex + 6].trim();
                int priority = parts.length > dataIndex + 7
                        ? Integer.parseInt(parts[dataIndex + 7].trim())
                        : 1;
                return new Job(clientId, jobId, duration, deadline, priority);
            }

            // Older format: CLIENT|clientID|jobID|duration|deadline
            if (parts.length >= dataIndex + 4) {
                String clientId = parts[dataIndex].trim();
                String jobId = parts[dataIndex + 1].trim();
                int duration = Integer.parseInt(parts[dataIndex + 2].trim());
                String deadline = parts[dataIndex + 3].trim();
                return new Job(clientId, jobId, duration, deadline);
            }
        } catch (Exception ex) {
            System.err.println("Error parsing client record: " + ex.getMessage());
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
            System.err.println("Error parsing owner record: " + ex.getMessage());
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
            for (String record : fileManager.loadClientRecords()) {
                Job job = parseClientRecord(record);
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
}
