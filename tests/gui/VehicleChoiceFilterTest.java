package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests the live prefix matching used by the editable vehicle dropdowns. */
@Tag("unit")
class VehicleChoiceFilterTest {

    // Typing the beginning of Honda should narrow the choices to Honda immediately.
    @Test
    void shouldMatchMakeWhileTypingWithoutCaseSensitivity() {
        List<String> choices = Arrays.asList("BMW", "Honda", "Hyundai", "Toyota");

        assertEquals(Arrays.asList("Honda"), OwnerPanel.matchingChoices(choices, "hon"));
    }

    // An unmatched custom value must remain typeable even though the suggestion list is empty.
    @Test
    void shouldReturnNoSuggestionsForAnUnlistedPrefix() {
        List<String> choices = Arrays.asList("Honda", "Toyota");

        assertTrue(OwnerPanel.matchingChoices(choices, "Zzz").isEmpty());
    }
}
