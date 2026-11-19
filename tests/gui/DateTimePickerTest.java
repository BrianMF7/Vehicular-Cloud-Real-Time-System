package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests the conversion from the picker’s AM/PM selection to a LocalDateTime hour. */
@Tag("unit")
class DateTimePickerTest {

    // Midnight is 12 AM, which LocalDateTime represents as hour 0.
    @Test
    void shouldConvertMidnightToHourZero() {
        assertEquals(0, DateTimePicker.to24Hour("12", "AM"));
    }

    // Afternoon selections add twelve hours to the displayed hour.
    @Test
    void shouldConvertAfternoonTimeToTwentyFourHourValue() {
        assertEquals(16, DateTimePicker.to24Hour("4", "PM"));
    }

    // Noon stays at hour 12 rather than becoming midnight.
    @Test
    void shouldKeepNoonAtHourTwelve() {
        assertEquals(12, DateTimePicker.to24Hour("12", "PM"));
    }
}
