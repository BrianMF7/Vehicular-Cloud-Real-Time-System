package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import model.Job;

class ClientHoursTest {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-uuuu");

    @Test
    void shouldAcceptHoursWithinOneDay() throws Exception {
        String day = LocalDate.now().plusDays(2).format(DATE_FORMAT);
        ClientSubmit submit = new ClientSubmit("client", "same-day", day, "2", day,
                "1", "09:30 AM", "12:00 PM");

        Job job = submit.createJob();

        assertEquals(2, job.getEstimatedDuration());
        assertEquals(day, job.getAvailability());
        assertEquals(day, job.getDeadline());
    }

    @Test
    void shouldRejectDurationLongerThanSelectedHours() {
        String day = LocalDate.now().plusDays(2).format(DATE_FORMAT);
        ClientSubmit submit = new ClientSubmit("client", "too-long", day, "3", day,
                "1", "09:30 AM", "12:00 PM");

        Exception error = assertThrows(Exception.class, submit::createJob);

        assertEquals("Duration cannot exceed 2 whole hours between availability and deadline.",
                error.getMessage());
    }

    @Test
    void shouldCountHoursAcrossMidnight() throws Exception {
        LocalDate start = LocalDate.now().plusDays(2);
        ClientSubmit submit = new ClientSubmit("client", "overnight",
                start.format(DATE_FORMAT), "2", start.plusDays(1).format(DATE_FORMAT),
                "1", "11:00 PM", "01:00 AM");

        assertEquals(2, submit.createJob().getEstimatedDuration());
    }
}
