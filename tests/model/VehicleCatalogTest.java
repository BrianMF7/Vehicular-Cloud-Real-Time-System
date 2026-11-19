package model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests that the owner form's vehicle suggestions stay grouped by make and model year. */
@Tag("unit")
class VehicleCatalogTest {

    // Lexus suggestions contain Lexus models, not a BMW-only model.
    @Test
    void shouldOnlySuggestLexusModelsForLexus() {
        List<String> lexusModels = VehicleCatalog.getModels("Lexus");

        assertTrue(lexusModels.contains("RX"));
        assertFalse(lexusModels.contains("330i"));
    }

    // BMW 330i starts in 2001, so its dropdown should not offer earlier years.
    @Test
    void shouldSuggestOnlyValidYearsForSelectedModel() {
        List<String> years = VehicleCatalog.getYears("BMW", "330i");

        assertTrue(years.contains("2001"));
        assertFalse(years.contains("2000"));
        assertTrue(years.contains("2027"));
    }
}
