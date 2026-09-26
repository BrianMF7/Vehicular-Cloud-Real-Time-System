package storage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileManager implements RecordWriter {
                private static FileManager instance;
         private final String filePath = "vehicular_cloud_log.txt";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FileManager() {
        ensureFileExists();
    }

    public static FileManager getInstance() {
        if (instance == null) {
                instance = new FileManager();
             }
            return instance;
         }

      private void ensureFileExists() {
          Path path = Paths.get(filePath);
                  if (!Files.exists(path)) {
            try {
                    Files.createFile(path);
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

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                  writer.write(line);
            writer.newLine();
        } catch (IOException ex) {
              throw new Exception("Couldn't save: " + ex.getMessage());
             }
    }
}

