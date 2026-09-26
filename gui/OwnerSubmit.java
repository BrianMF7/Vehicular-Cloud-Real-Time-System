package gui;

import storage.FileManager;

public class OwnerSubmit implements SubmitStrategy {
    private final String ownerId;
    private final String vehicleId;
    private final String make;
    private final String model;
    private final String year;
    private final String arrivalTime;
    private final String departureTime;

    public OwnerSubmit(String ownerId, String vehicleId, String make, 
                       String model, String year, String arrivalTime, 
                       String departureTime) {
        this.ownerId = ownerId;
        this.vehicleId = vehicleId;
            this.make = make;
           this.model = model;
         this.year = year;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        }

    public void submit() throws Exception {
           validateFields();
            String record = buildRecord();
        FileManager.getInstance().saveRecord(record);
       }

    private void validateFields() throws Exception {
               if (isEmpty(ownerId)) {
            throw new Exception("Owner ID is needed.");
        }
        if (isEmpty(vehicleId)) {
               throw new Exception("Vehicle ID is needed.");
           }
          if (isEmpty(make)) {
              throw new Exception("Make is needed.");
           }
        if (isEmpty(model)) {
            throw new Exception("Model is needed.");
           }
        if (isEmpty(year)) {
            throw new Exception("Year is required.");
            }
        if (isEmpty(arrivalTime)) {
             throw new Exception("Arrival time is required.");
           }
          if (isEmpty(departureTime)) {
            throw new Exception("Departure time is required.");
        }

        try {
                int yearValue = Integer.parseInt(year.trim());
            if (yearValue < 1995 || yearValue > 2027) {
                throw new Exception("Year must be between 1995 and 2027.");
            }
             } catch (NumberFormatException ex) {
            throw new Exception("Year must be a right number.");
            }
    }

      private String buildRecord() {
        return "OWNER|" + 
               trim(ownerId) + "|" + 
               trim(vehicleId) + "|" + 
               trim(make) + "|" + 
               trim(model) + "|" + 
               trim(year) + "|" + 
               trim(arrivalTime) + "|" + 
               trim(departureTime);
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
      }
}
