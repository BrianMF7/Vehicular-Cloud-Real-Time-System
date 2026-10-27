package tests.storage;

import java.util.List;
import model.Checkpoint;
import model.Job;
import model.Vehicle;
import storage.RecordParser;
import storage.Server;

//Simple test class 
public class StorageTest {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Features ===\n");
        //Test 1: Server and Checkpoint
           testServerAndCheckpoints();
        
        //Test 2: Loading saved records
               testLoadingRecords();
    }
    
    //Test creating server and storing checkpoints
    private static void testServerAndCheckpoints() {
        System.out.println("Test 1: Server & Checkpoints");
        
        //Create a server
        Server server = new Server("SERVER-001");
        System.out.println("Created: " + server);
        
              //Create some checkpoints
          Checkpoint cp1 = new Checkpoint("CP-001", "JOB-123", "/data/checkpoint1.img", 15);
        Checkpoint cp2 = new Checkpoint("CP-002", "JOB-123", "/data/checkpoint2.img", 30);
        Checkpoint cp3 = new Checkpoint("CP-003", "JOB-456", "/data/checkpoint3.img", 10);
        
           //Store checkpoints
         server.storeCheckpoint(cp1);
          server.storeCheckpoint(cp2);
        server.storeCheckpoint(cp3);
        System.out.println("Stored 3 checkpoints");
        
            //Store some job results
           server.storeResult("JOB-123", "Completed successfully");
          server.storeResult("JOB-456", "In progress");
        System.out.println("Stored 2 job results");
        
        //Get latest checkpoint for a job
           Checkpoint latest = server.getLatestCheckpoint("JOB-123");
        System.out.println("Latest checkpoint for JOB-123: " + latest);
        
        //Get a result
        String result = server.getResult("JOB-123");
        System.out.println("Result for JOB-123: " + result);
        
             System.out.println("\n" + server + "\n");
    }
    
    //Test loading records from file
    private static void testLoadingRecords() {
            System.out.println("Test 2: Loading Saved Records");
        
        try {
            //Load all jobs from file
            List<Job> jobs = RecordParser.loadAllJobs();
               System.out.println("Loaded " + jobs.size() + " jobs from file:");
              for (Job job : jobs) {
                System.out.println("  - " + job);
                 }
            
            System.out.println();
            
            //Load all vehicles from file
            List<Vehicle> vehicles = RecordParser.loadAllVehicles();
                   System.out.println("Loaded " + vehicles.size() + " vehicles from file:");
            for (Vehicle vehicle : vehicles) {
                System.out.println("  - " + vehicle);
                  }
            
        } catch (Exception e) {
            System.err.println("Error loading records: " + e.getMessage());
                }
        
        System.out.println("Tests Complete");
    }
}
