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
    private final String computePower;

    public OwnerSubmit(String ownerId, String vehicleId, String make, 
                       String model, String year, String arrivalTime, 
                       String departureTime) {
        this(ownerId, vehicleId, make, model, year, arrivalTime, departureTime, null);
    }

    public OwnerSubmit(String ownerId, String vehicleId, String make,
                       String model, String year, String arrivalTime,
                       String departureTime, String computePower) {
        this.ownerId = ownerId;
        this.vehicleId = vehicleId;
            this.make = make;
           this.model = model;
         this.year = year;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.computePower = computePower;
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
        if (computePower != null) {
            try {
                double value = Double.parseDouble(computePower.trim());
                if (!Double.isFinite(value) || value <= 0) {
                    throw new Exception("Compute power must be a positive number.");
                }
            } catch (NumberFormatException ex) {
                throw new Exception("Compute power must be a positive number.");
            }
        }
    }

      private String buildRecord() {
        String username = MainFrame.getCurrentUser();
        String email = MainFrame.getCurrentEmail();
         return "OWNER|" +
               trim(username) + "|" +
                 trim(email) + "|" +
                 trim(ownerId) + "|" +
                trim(vehicleId) + "|" +
               trim(make) + "|" + 
               trim(model) + "|" + 
               trim(year) + "|" + 
               trim(arrivalTime) + "|" + 
               trim(departureTime) +
               (computePower == null ? "" : "|" + trim(computePower));
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
      }
}
