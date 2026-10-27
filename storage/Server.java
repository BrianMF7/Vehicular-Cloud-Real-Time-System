package storage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Checkpoint;

//Server stores job results and checkpoints
public class Server {
    
    //Server info
    private String serverID;
       private LocalDateTime createdAt;
    
    //Map to store checkpoints 
       private Map<String, Checkpoint> savedCheckpoints;
    
    //Map to store job results
          private Map<String, String> jobResults;
    
    public Server(String serverID) {
           this.serverID = serverID;
        this.createdAt = LocalDateTime.now();
          this.savedCheckpoints = new HashMap<>();
        this.jobResults = new HashMap<>();
    }
    
    //Store a job result in the server
    public boolean storeResult(String jobID, String result) {
          if (jobID == null || jobID.trim().isEmpty()) {
            return false; 
           }
           if (result == null) {
               result = ""; 
        }
            jobResults.put(jobID, result);
        return true;
        }
    
    public String getResult(String jobID) {
        return jobResults.get(jobID); 
          }
    
    //Save a checkpoint to the server
    public boolean storeCheckpoint(Checkpoint checkpoint) {
        if (checkpoint == null) {
            return false;
           }
        savedCheckpoints.put(checkpoint.getCheckpointID(), checkpoint);
            return true;
    }
    
    //Get the most recent checkpoint for a specific job
    public Checkpoint getLatestCheckpoint(String jobID) {
             Checkpoint latest = null;
        
        //Loop through all checkpoints to find new one.
        for (Checkpoint cp : savedCheckpoints.values()) {
            if (cp.getJobID().equals(jobID)) {
                if (latest == null || cp.getTimestamp().isAfter(latest.getTimestamp())) {
                    latest = cp; 
                }
                }
          }
        return latest;
    }
    
    //Get all checkpoints for a job
    public List<Checkpoint> getAllCheckpointsForJob(String jobID) {
        List<Checkpoint> checkpoints = new ArrayList<>();
        
        for (Checkpoint cp : savedCheckpoints.values()) {
            if (cp.getJobID().equals(jobID)) {
                checkpoints.add(cp);
            }
           }
        
        return checkpoints;
          }
    
    public String getServerID() {
        return serverID;
            }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
          }
    
    public int getTotalCheckpoints() {
           return savedCheckpoints.size();
          }
    
    public int getTotalResults() {
        return jobResults.size();
         }
    
    @Override
    public String toString() {
        return "Server ID: " + serverID +
               ", Created: " + createdAt +
               ", Total Checkpoints: " + savedCheckpoints.size() +
               ", Total Results: " + jobResults.size();
       }
}
