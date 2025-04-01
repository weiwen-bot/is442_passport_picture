/*
 * DimensionHelper.java
 *
 * Utility class for retrieving target passport photo dimensions from
 * country or template definitions stored in JSON files.
 *
 */

package com.passportphoto.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

/**
 * The {@code DimensionHelper} class loads predefined dimensions for
 * passport photos based on countries or templates from JSON resources.
 * It is a non-instantiable utility class.
 */
public final class DimensionHelper {

    /** Dimensions mapped by country code */
    private static final Map<String, int[]> COUNTRY_DIMENSIONS = loadDimensions("dimensions/countries.json");

    /** Dimensions mapped by template label */
    private static final Map<String, int[]> TEMPLATE_DIMENSIONS = loadDimensions("dimensions/templates.json");

    /** Default fallback dimensions if none are provided or found */
    private static final int[] DEFAULT_SIZE = {413, 531};

    /**
     * Private constructor to prevent instantiation.
     */
    private DimensionHelper() {
        // Utility class - do not instantiate.
    }

    /**
     * Returns target dimensions based on the provided country code, template name,
     * or custom width/height fallback.
     *
     * @param country      the country code (optional)
     * @param template     the template label (optional)
     * @param customWidth  custom width (used if no country/template is given)
     * @param customHeight custom height (used if no country/template is given)
     * @return an array of two integers: [width, height]
     */
    public static int[] getTargetDimensions(String country, String template, Integer customWidth, Integer customHeight) {
        if (country != null && !country.isEmpty()) {
            return COUNTRY_DIMENSIONS.getOrDefault(country.toLowerCase(), DEFAULT_SIZE);
        }
        if (template != null && !template.isEmpty()) {
            return TEMPLATE_DIMENSIONS.getOrDefault(template, DEFAULT_SIZE);
        }
        return new int[]{customWidth, customHeight};
    }

    /**
     * Loads a JSON file containing dimension mappings and returns it as a map.
     *
     * @param resourcePath the path to the JSON file in the classpath
     * @return a map from keys to [width, height] arrays
     */
    private static Map<String, int[]> loadDimensions(String resourcePath) {
        try {
            return new ObjectMapper().readValue(
                new ClassPathResource(resourcePath).getInputStream(),
                new TypeReference<Map<String, int[]>>() {}
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load dimensions: " + resourcePath, e);
        }
    }
}