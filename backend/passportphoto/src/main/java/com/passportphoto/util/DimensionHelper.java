package com.passportphoto.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.util.Map;

public final class DimensionHelper {
    private static final Map<String, int[]> COUNTRY_DIMENSIONS = loadDimensions("dimensions/countries.json");
    private static final Map<String, int[]> TEMPLATE_DIMENSIONS = loadDimensions("dimensions/templates.json");
    private static final int[] DEFAULT_SIZE = {413, 531};

    // prevent instantiation
    private DimensionHelper() {

    }

    public static int[] getTargetDimensions(String country, String template, Integer customWidth, Integer customHeight) {
        if (country != null && !country.isEmpty()) {
            return COUNTRY_DIMENSIONS.getOrDefault(country.toLowerCase(), DEFAULT_SIZE);
        }
        if (template != null && !template.isEmpty()) {
            return TEMPLATE_DIMENSIONS.getOrDefault(template, DEFAULT_SIZE);
        }
        return new int[]{customWidth, customHeight};
    }

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