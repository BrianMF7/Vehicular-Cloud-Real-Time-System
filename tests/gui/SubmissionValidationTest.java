package gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validation tests for required fields in the Client and Owner submission workflows.
 * These tests deliberately use missing values, so they fail before any file is written.
 */
class SubmissionValidationTest {

    @Test
    void clientSubmitRejectsNullClientId() {
        Exception ex = assertThrows(Exception.class,
                () -> new ClientSubmit(null, "JOB-1", "12-01-2026", "2", "12-10-2026").submit());
        assertEquals("Client ID is required.", ex.getMessage());
    }

    @Test
    void clientSubmitRejectsBlankJobId() {
        Exception ex = assertThrows(Exception.class,
                () -> new ClientSubmit("CLIENT-1", "   ", "12-01-2026", "2", "12-10-2026").submit());
        assertEquals("Job ID is required.", ex.getMessage());
    }

    @Test
    void clientSubmitRejectsMissingAvailability() {
        Exception ex = assertThrows(Exception.class,
                () -> new ClientSubmit("CLIENT-1", "JOB-1", "", "2", "12-10-2026").submit());
        assertEquals("Job Availability is required.", ex.getMessage());
    }

    @Test
    void clientSubmitRejectsMissingDuration() {
        Exception ex = assertThrows(Exception.class,
                () -> new ClientSubmit("CLIENT-1", "JOB-1", "12-01-2026", " ", "12-10-2026").submit());
        assertEquals("Job duration is required.", ex.getMessage());
    }

    @Test
    void clientSubmitRejectsMissingDeadline() {
        Exception ex = assertThrows(Exception.class,
                () -> new ClientSubmit("CLIENT-1", "JOB-1", "12-01-2026", "2", null).submit());
        assertEquals("Job deadline is required.", ex.getMessage());
    }

    @Test
    void ownerSubmitRejectsMissingOwnerId() {
        Exception ex = assertThrows(Exception.class,
                () -> new OwnerSubmit("", "VEH-1", "Toyota", "Camry", "2020",
                        "12-01-2026 09:00", "12-02-2026 09:00").submit());
        assertEquals("Owner ID is needed.", ex.getMessage());
    }

    @Test
    void ownerSubmitRejectsMissingVehicleId() {
        Exception ex = assertThrows(Exception.class,
                () -> new OwnerSubmit("OWNER-1", " ", "Toyota", "Camry", "2020",
                        "12-01-2026 09:00", "12-02-2026 09:00").submit());
        assertEquals("Vehicle ID is needed.", ex.getMessage());
    }

    @Test
    void ownerSubmitRejectsMissingMake() {
        Exception ex = assertThrows(Exception.class,
                () -> new OwnerSubmit("OWNER-1", "VEH-1", null, "Camry", "2020",
                        "12-01-2026 09:00", "12-02-2026 09:00").submit());
        assertEquals("Make is needed.", ex.getMessage());
    }

    @Test
    void ownerSubmitRejectsMissingModel() {
        Exception ex = assertThrows(Exception.class,
                () -> new OwnerSubmit("OWNER-1", "VEH-1", "Toyota", " ", "2020",
                        "12-01-2026 09:00", "12-02-2026 09:00").submit());
        assertEquals("Model is needed.", ex.getMessage());
    }

    @Test
    void ownerSubmitRejectsMissingYear() {
        Exception ex = assertThrows(Exception.class,
                () -> new OwnerSubmit("OWNER-1", "VEH-1", "Toyota", "Camry", "",
                        "12-01-2026 09:00", "12-02-2026 09:00").submit());
        assertEquals("Year is required.", ex.getMessage());
    }

    @Test
    void ownerSubmitRejectsMissingArrival() {
        Exception ex = assertThrows(Exception.class,
                () -> new OwnerSubmit("OWNER-1", "VEH-1", "Toyota", "Camry", "2020",
                        " ", "12-02-2026 09:00").submit());
        assertEquals("Arrival time is required.", ex.getMessage());
    }

    @Test
    void ownerSubmitRejectsMissingDeparture() {
        Exception ex = assertThrows(Exception.class,
                () -> new OwnerSubmit("OWNER-1", "VEH-1", "Toyota", "Camry", "2020",
                        "12-01-2026 09:00", null).submit());
        assertEquals("Departure time is required.", ex.getMessage());
    }
}
