package storage;

import java.util.ArrayList;
import java.util.List;
import model.Job;
import model.Vehicle;

//Helper class to parse records loaded from file into model objects
public class RecordParser {
    
    //Parse a CLIENT record line into a Job object
    public static Job parseClientRecord(String recordLine) {
        if (recordLine == null || !recordLine.contains("CLIENT|")) {
               return null;
              }
        
        try {
            String[] parts = recordLine.split("\\|");
            
            //Format: timestamp|CLIENT|username|email|clientID|jobID|duration|deadline
            if (parts.length >= 8) {
                String clientId = parts[4].trim();
                  String jobId = parts[5].trim();
                int duration = Integer.parseInt(parts[6].trim());
                   String deadline = parts[7].trim();
                
                //Create and return Job object
                  return new Job(clientId, jobId, duration, deadline);
            }
                  } catch (Exception e) {
            System.err.println("Error parsing client record: " + e.getMessage());
        }  
        return null;
         }
    
    //Parse an owner record line into owner and vehicle objects
    public static Vehicle parseOwnerRecord(String recordLine) {
        if (recordLine == null || !recordLine.contains("OWNER|")) {
            return null;
                  }
        
        try {
             String[] parts = recordLine.split("\\|");
            
            //Format: timestamp|OWNER|username|email|ownerID|vehicleID|make|model|year|arrival|departure|computePower
               if (parts.length >= 11) {
                String vehicleId = parts[5].trim();
                String make = parts[6].trim();
                    String model = parts[7].trim();
                int year = Integer.parseInt(parts[8].trim());
                   String arrival = parts[9].trim();
                String departure = parts[10].trim();
                
                //Create and return Vehicle object
                return new Vehicle(vehicleId, make, model, year, arrival, departure);
                  }
        } catch (Exception e) {
            System.err.println("Error parsing owner record: " + e.getMessage());
        }
        
        return null;
    }
    
    //Load all jobs from file
    public static List<Job> loadAllJobs() {
           List<Job> jobs = new ArrayList<>();
        
        try {
            List<String> clientRecords = FileManager.getInstance().loadClientRecords();
            
            for (String record : clientRecords) {
                Job job = parseClientRecord(record);
                if (job != null) {
                    jobs.add(job);
                }
                 }
        } catch (Exception e) {
            System.err.println("Error loading jobs: " + e.getMessage());
             }
        return jobs;
         }
    
    //Load all vehicles from file
    public static List<Vehicle> loadAllVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        
        try {
            List<String> ownerRecords = FileManager.getInstance().loadOwnerRecords();
            
               for (String record : ownerRecords) {
                Vehicle vehicle = parseOwnerRecord(record);
                     if (vehicle != null) {
                    vehicles.add(vehicle);
                }
                 }
              } catch (Exception e) {
            System.err.println("Error loading vehicles: " + e.getMessage());
        }  
        return vehicles;
       }
     }
