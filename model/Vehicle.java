package model;

public class Vehicle {

    /*  Private fields use encapsulation so our other classes cannot directly change the vehicle's data */
    private String vehicleId;
    private String make;
    private String model;

    // Year is int because it can't have decimals and always a whole numbr
    private int year;

    /* Arrival and departure times show how long the vehicle is expected to be available in the vehicle cloud */
    private String arrivalTime;
    private String departureTime;

    // Constructor initializes a new Vehicle object with its information
    public Vehicle(String vehicleId, String make, String model, int year,
                   String arrivalTime, String departureTime) {

        this.vehicleId = vehicleId;
        this.make = make;
        this.model = model;
        this.year = year;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
    }

    // getters and setters 
    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        // Connected car services only became available in production in the mid-1990s, but they still weren't what we are trying to simulate in this project so 1995 is a safe lower bound
        if (year >= 1995 && year <= 2027) {
        this.year = year;
    }
    }
    
    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getDepartureTime() {
        return departureTime;  
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    /* Overrides Java's default toString() method so a Vehicle
    can be displayed in a readable format */
    @Override
    public String toString() {
        return "Vehicle ID: " + vehicleId +
                ", Make: " + make +
                ", Model: " + model +
                ", Year: " + year +
                ", Arrival Time: " + arrivalTime +
                ", Departure Time: " + departureTime;
    }
}