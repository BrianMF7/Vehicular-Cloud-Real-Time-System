package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides common vehicle choices for the owner form. The form remains editable
 * so an owner can enter a valid vehicle that is not in this starter catalog.
 */
public final class VehicleCatalog {
    public static final int MIN_MODEL_YEAR = 1995;
    public static final int MAX_MODEL_YEAR = 2027;

    private static final Map<String, List<ModelYears>> MODELS_BY_MAKE = createCatalog();

    private VehicleCatalog() {
    }

    /** Returns the available makes in the order shown by the form. */
    public static List<String> getMakes() {
        return new ArrayList<>(MODELS_BY_MAKE.keySet());
    }

    /** Returns model suggestions for a make, or no suggestions for an unlisted make. */
    public static List<String> getModels(String make) {
        List<ModelYears> models = modelsForMake(make);
        if (models == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (ModelYears model : models) {
            names.add(model.name);
        }
        return names;
    }

    /** Returns newest-first model-year suggestions for the selected make and model. */
    public static List<String> getYears(String make, String modelName) {
        List<ModelYears> models = modelsForMake(make);
        if (models == null) {
            return Collections.emptyList();
        }
        for (ModelYears model : models) {
            if (model.name.equalsIgnoreCase(normalize(modelName))) {
                List<String> years = new ArrayList<>();
                for (int year = model.lastYear; year >= model.firstYear; year--) {
                    years.add(Integer.toString(year));
                }
                return years;
            }
        }
        return Collections.emptyList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<ModelYears> modelsForMake(String make) {
        String normalizedMake = normalize(make);
        for (Map.Entry<String, List<ModelYears>> entry : MODELS_BY_MAKE.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(normalizedMake)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Map<String, List<ModelYears>> createCatalog() {
        Map<String, List<ModelYears>> catalog = new LinkedHashMap<>();
        catalog.put("Acura", models(
                model("Integra", 1995, 2027), model("TLX", 2015, 2027),
                model("RDX", 2007, 2027), model("MDX", 2001, 2027)));
        catalog.put("Alfa Romeo", models(
                model("Giulia", 2017, 2027), model("Stelvio", 2018, 2027),
                model("Tonale", 2023, 2027), model("4C", 2015, 2020)));
        catalog.put("Audi", models(
                model("A3", 1996, 2027), model("A4", 1995, 2027),
                model("A6", 1995, 2027), model("Q5", 2009, 2027),
                model("Q7", 2007, 2027)));
        catalog.put("BMW", models(
                model("3 Series", 1995, 2027), model("330i", 2001, 2027),
                model("5 Series", 1995, 2027), model("X3", 2004, 2027),
                model("X5", 2000, 2027)));
        catalog.put("Buick", models(
                model("Encore", 2013, 2027), model("Enclave", 2008, 2027),
                model("Envision", 2016, 2027), model("Envista", 2024, 2027)));
        catalog.put("Cadillac", models(
                model("CT4", 2020, 2027), model("CT5", 2020, 2027),
                model("Escalade", 1999, 2027), model("XT4", 2019, 2027),
                model("XT5", 2017, 2027)));
        catalog.put("Chevrolet", models(
                model("Camaro", 1995, 2024), model("Corvette", 1995, 2027),
                model("Malibu", 1995, 2027), model("Silverado", 1999, 2027),
                model("Equinox", 2005, 2027)));
        catalog.put("Chrysler", models(
                model("300", 2005, 2023), model("Pacifica", 2017, 2027),
                model("Voyager", 2020, 2027), model("Sebring", 1995, 2010),
                model("Town & Country", 1995, 2016)));
        catalog.put("Dodge", models(
                model("Charger", 2006, 2023), model("Challenger", 2008, 2023),
                model("Durango", 1998, 2027), model("Hornet", 2023, 2027),
                model("Grand Caravan", 1995, 2020)));
        catalog.put("FIAT", models(
                model("500", 2012, 2019), model("500X", 2016, 2023),
                model("124 Spider", 2017, 2020)));
        catalog.put("Ford", models(
                model("Mustang", 1995, 2027), model("F-150", 1995, 2027),
                model("Explorer", 1995, 2027), model("Escape", 2001, 2027),
                model("Bronco", 2021, 2027)));
        catalog.put("Genesis", models(
                model("G70", 2019, 2027), model("G80", 2017, 2027),
                model("G90", 2017, 2027), model("GV70", 2022, 2027),
                model("GV80", 2021, 2027)));
        catalog.put("GMC", models(
                model("Acadia", 2007, 2027), model("Canyon", 2004, 2027),
                model("Sierra", 1999, 2027), model("Terrain", 2010, 2027),
                model("Yukon", 1995, 2027)));
        catalog.put("Honda", models(
                model("Civic", 1995, 2027), model("Accord", 1995, 2027),
                model("CR-V", 1997, 2027), model("Pilot", 2003, 2027),
                model("Odyssey", 1995, 2027)));
        catalog.put("Hyundai", models(
                model("Elantra", 1995, 2027), model("Sonata", 1995, 2027),
                model("Tucson", 2005, 2027), model("Santa Fe", 2001, 2027),
                model("IONIQ 5", 2022, 2027)));
        catalog.put("INFINITI", models(
                model("Q50", 2014, 2024), model("QX50", 2008, 2027),
                model("QX55", 2022, 2027), model("QX60", 2014, 2027),
                model("QX80", 2014, 2027)));
        catalog.put("Jaguar", models(
                model("F-PACE", 2017, 2027), model("F-TYPE", 2014, 2024),
                model("I-PACE", 2019, 2027), model("XE", 2017, 2020),
                model("XF", 2009, 2027)));
        catalog.put("Jeep", models(
                model("Compass", 2007, 2027), model("Grand Cherokee", 1995, 2027),
                model("Wrangler", 1995, 2027), model("Gladiator", 2020, 2027),
                model("Wagoneer", 2022, 2027)));
        catalog.put("Kia", models(
                model("Forte", 2010, 2024), model("K5", 2021, 2027),
                model("Soul", 2010, 2027), model("Sportage", 1995, 2027),
                model("Telluride", 2020, 2027), model("EV6", 2022, 2027)));
        catalog.put("Land Rover", models(
                model("Discovery", 1995, 2027), model("Defender", 2020, 2027),
                model("Range Rover", 1995, 2027), model("Range Rover Sport", 2006, 2027),
                model("Range Rover Evoque", 2012, 2027)));
        catalog.put("Lexus", models(
                model("ES", 1995, 2027), model("IS", 1999, 2027),
                model("RX", 1999, 2027), model("NX", 2015, 2027),
                model("GX", 2003, 2027)));
        catalog.put("Lincoln", models(
                model("Corsair", 2020, 2027), model("Nautilus", 2019, 2027),
                model("Aviator", 2003, 2027), model("Navigator", 1998, 2027)));
        catalog.put("Mazda", models(
                model("Mazda3", 2004, 2027), model("Mazda6", 2003, 2027),
                model("CX-5", 2013, 2027), model("CX-9", 2007, 2027),
                model("MX-5 Miata", 1995, 2027)));
        catalog.put("Mercedes-Benz", models(
                model("C-Class", 1995, 2027), model("E-Class", 1995, 2027),
                model("S-Class", 1995, 2027), model("GLC", 2016, 2027),
                model("GLE", 2016, 2027)));
        catalog.put("MINI", models(
                model("Cooper", 2002, 2027), model("Countryman", 2011, 2027),
                model("Clubman", 2008, 2024)));
        catalog.put("Mitsubishi", models(
                model("Eclipse Cross", 2018, 2027), model("Mirage", 2014, 2027),
                model("Outlander", 2003, 2027), model("Outlander Sport", 2011, 2027),
                model("Lancer", 1995, 2017)));
        catalog.put("Nissan", models(
                model("Altima", 1995, 2027), model("Maxima", 1995, 2027),
                model("Sentra", 1995, 2027), model("Rogue", 2008, 2027),
                model("Pathfinder", 1995, 2027)));
        catalog.put("Polestar", models(
                model("2", 2021, 2027), model("3", 2025, 2027),
                model("4", 2025, 2027)));
        catalog.put("Porsche", models(
                model("718 Boxster", 2017, 2027), model("911", 1995, 2027),
                model("Cayenne", 2003, 2027), model("Macan", 2015, 2027),
                model("Taycan", 2020, 2027)));
        catalog.put("Ram", models(
                model("1500", 2011, 2027), model("2500", 2011, 2027),
                model("ProMaster", 2014, 2027), model("ProMaster City", 2015, 2022),
                model("Dakota", 1995, 2011)));
        catalog.put("Rivian", models(
                model("R1T", 2022, 2027), model("R1S", 2023, 2027)));
        catalog.put("Scion", models(
                model("FR-S", 2013, 2016), model("tC", 2005, 2016),
                model("xB", 2004, 2015), model("xD", 2008, 2014),
                model("iA", 2016, 2016)));
        catalog.put("smart", models(
                model("fortwo", 2008, 2019), model("forfour", 2016, 2019)));
        catalog.put("Subaru", models(
                model("Impreza", 1995, 2027), model("Legacy", 1995, 2027),
                model("Outback", 1995, 2027), model("Forester", 1998, 2027),
                model("Crosstrek", 2013, 2027)));
        catalog.put("Tesla", models(
                model("Model S", 2012, 2027), model("Model X", 2016, 2027),
                model("Model 3", 2017, 2027), model("Model Y", 2020, 2027),
                model("Cybertruck", 2024, 2027)));
        catalog.put("Toyota", models(
                model("Camry", 1995, 2027), model("Corolla", 1995, 2027),
                model("RAV4", 1996, 2027), model("Highlander", 2001, 2027),
                model("Tacoma", 1995, 2027), model("Prius", 2001, 2027)));
        catalog.put("Volkswagen", models(
                model("Jetta", 1995, 2027), model("Passat", 1995, 2027),
                model("Tiguan", 2009, 2027), model("Atlas", 2018, 2027),
                model("ID.4", 2021, 2027)));
        catalog.put("Volvo", models(
                model("S60", 2001, 2027), model("S90", 2017, 2027),
                model("XC40", 2019, 2027), model("XC60", 2010, 2027),
                model("XC90", 2003, 2027), model("EX30", 2024, 2027)));
        return Collections.unmodifiableMap(catalog);
    }

    private static List<ModelYears> models(ModelYears... values) {
        List<ModelYears> result = new ArrayList<>();
        Collections.addAll(result, values);
        return Collections.unmodifiableList(result);
    }

    private static ModelYears model(String name, int firstYear, int lastYear) {
        return new ModelYears(name, firstYear, lastYear);
    }

    private static final class ModelYears {
        private final String name;
        private final int firstYear;
        private final int lastYear;

        private ModelYears(String name, int firstYear, int lastYear) {
            this.name = name;
            this.firstYear = firstYear;
            this.lastYear = lastYear;
        }
    }
}
