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

