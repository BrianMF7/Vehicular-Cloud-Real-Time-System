package storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

public class FileManager implements RecordWriter {
                private static FileManager instance;
         private final Path filePath;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FileManager() {
        this(Paths.get("vehicular_cloud_log.txt"));
    }

    // Lets tests use a temporary file instead of the real application log.
    public FileManager(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path is required.");
        }
        this.filePath = filePath;
        ensureFileExists();
    }

    public static FileManager getInstance() {
              if (instance == null) {
                instance = new FileManager();
             }
            return instance;
             }

      private void ensureFileExists() {
                  if (!Files.exists(filePath)) {
            try {
                    Path parent = filePath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.createFile(filePath);
                    } catch (IOException ex) {
                System.err.println("No log file created: " + ex.getMessage());
             }
                  }
    }

      public synchronized void saveRecord(String record) throws Exception {
           if (record == null || record.trim().isEmpty()) {
            throw new IllegalArgumentException("Record can't be empty.");
                }

        String timestamp = LocalDateTime.now().format(formatter);
             String line = timestamp + "|" + record;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile(), true))) {
                  writer.write(line);
            writer.newLine();
              } catch (IOException ex) {
              throw new Exception("Couldn't save: " + ex.getMessage());
             }
    }
    
    //Load all records from the file
    public List<String> loadAllRecords() throws Exception {
        List<String> records = new ArrayList<>();
        
         try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
                 String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    records.add(line); //Add each line to the list
                }
                }
               } catch (IOException ex) {
            throw new Exception("Couldn't load records: " + ex.getMessage());
        }
        
        return records;
    }
    
    //Load only OWNER records
           public List<String> loadOwnerRecords() throws Exception {
         List<String> ownerRecords = new ArrayList<>();
        List<String> allRecords = loadAllRecords();
        
        for (String record : allRecords) {
            if (record.contains("OWNER|")) {
                ownerRecords.add(record);
            }
            }
        
        return ownerRecords;
          }

    /** Vehicle rows for the owner's table, including older saved registrations. */
    public List<VehicleEntry> readVehicles() throws Exception {
        List<VehicleEntry> vehicles = new ArrayList<>();
        for (String record : loadOwnerRecords()) {
            String[] fields = record.split("\\|", -1);
            if (fields.length < 9 || !"OWNER".equals(fields[1])) {
                continue;
            }
            // Current records include username and email; older ones start with owner ID.
            int ownerIndex = fields.length >= 11 ? 4 : 2;
            String accountEmail = ownerIndex == 4 ? fields[3].trim() : "";
            if (fields.length <= ownerIndex + 6 || fields[ownerIndex + 1].trim().isEmpty()) {
                continue;
            }
            vehicles.add(new VehicleEntry(accountEmail, fields[ownerIndex + 1].trim(),
                    fields[ownerIndex].trim(), fields[ownerIndex + 2].trim(),
                    fields[ownerIndex + 3].trim(), fields[ownerIndex + 4].trim(),
                    fields[ownerIndex + 5].trim(), fields[ownerIndex + 6].trim()));
        }
        return vehicles;
    }

    public static final class VehicleEntry {
        private static final DateTimeFormatter VEHICLE_TIME = DateTimeFormatter
                .ofPattern("MM-dd-uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

        private final String vehicleId;
        private final String accountEmail;
        private final String ownerId;
        private final String make;
        private final String model;
        private final String year;
        private final String arrivalTime;
        private final String departureTime;

        private VehicleEntry(String accountEmail, String vehicleId, String ownerId, String make, String model,
                             String year, String arrivalTime, String departureTime) {
            this.accountEmail = accountEmail;
            this.vehicleId = vehicleId;
            this.ownerId = ownerId;
            this.make = make;
            this.model = model;
            this.year = year;
            this.arrivalTime = arrivalTime;
            this.departureTime = departureTime;
        }

        public String getVehicleId() {
            return vehicleId;
        }

        public String getOwnerId() {
            return ownerId;
        }

        public boolean belongsToAccount(String email) {
            return email != null && !email.isEmpty() && accountEmail.equalsIgnoreCase(email);
        }

        public String getCarModel() {
            return make + " " + model + " (" + year + ")";
        }

        public String getStatusAt(LocalDateTime now) {
            try {
                LocalDateTime arrival = LocalDateTime.parse(arrivalTime, VEHICLE_TIME);
                LocalDateTime departure = LocalDateTime.parse(departureTime, VEHICLE_TIME);
                if (!departure.isAfter(arrival)) {
                    return "Unknown";
                }
                if (now.isBefore(arrival)) {
                    return "Registered";
                }
                return now.isBefore(departure) ? "Available" : "Unavailable";
            } catch (DateTimeParseException ex) {
                return "Unknown";
            }
        }
    }
    
    //Load only client records
    public List<String> loadClientRecords() throws Exception {
        List<String> clientRecords = new ArrayList<>();
          List<String> allRecords = loadAllRecords();
        
        for (String record : allRecords) {
            if (record.contains("CLIENT|")) {
                clientRecords.add(record);
            }
              }
        
        return clientRecords;
        }
    
    //Get the file path
    public String getFilePath() {
        return filePath.toString();
           }
}

