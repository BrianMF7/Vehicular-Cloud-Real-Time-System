package model;

import java.time.LocalDateTime;

//Checkpoint saves job progress 
public class Checkpoint {
    
    //Basic info about this checkpoint
    private String checkpointID;
       private String jobID;
    private LocalDateTime timestamp;
       private String imageLocation;
    private int elapsedMinutes;
    
    //Constructor that sets up a new checkpoint
    public Checkpoint(String checkpointID, String jobID, String imageLocation, int elapsedMinutes) {
        this.checkpointID = checkpointID;
          this.jobID = jobID;
           this.imageLocation = imageLocation;
        this.elapsedMinutes = elapsedMinutes;
        this.timestamp = LocalDateTime.now(); 
          }
    
          public String getCheckpointID() {
        return checkpointID;
    }
    
          public String getJobID() {
        return jobID;
       }
    
       public LocalDateTime getTimestamp() {
            return timestamp;
      }
    
    public String getImageLocation() {
          return imageLocation;
    }
    
    public int getElapsedMinutes() {
        return elapsedMinutes;
         }
    
    public void setImageLocation(String imageLocation) {
          this.imageLocation = imageLocation;
          }
    
    public void setElapsedMinutes(int elapsedMinutes) {
         this.elapsedMinutes = elapsedMinutes;
        }
    
    //Makes it easy to print checkpoint info
    @Override
    public String toString() {
        return "Checkpoint ID: " + checkpointID +
               ", Job ID: " + jobID +
               ", Timestamp: " + timestamp +
               ", Location: " + imageLocation +
               ", Elapsed: " + elapsedMinutes + " minutes";
    }
}
