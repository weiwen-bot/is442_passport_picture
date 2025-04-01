/*
 * ImageResizingController.java
 *
 * This controller handles endpoints for resizing images and retrieving
 * country/template dimension metadata for the Passport Picture Project.
 *
 */

package com.passportphoto.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passportphoto.dto.ImageResizeResponse;
import com.passportphoto.service.ImageResizingService;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code ImageResizingController} provides endpoints for resizing passport photos
 * based on country or template dimensions, and retrieving available options.
 */
@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173")

public class ImageResizingController {

    /** Service that performs the actual image resizing logic */
    private final ImageResizingService imageResizingService;

    /**
     * Constructs the controller with the required resizing service.
     *
     * @param imageResizingService the service that handles image resizing
     */
    public ImageResizingController(ImageResizingService imageResizingService) {
        this.imageResizingService = imageResizingService;
    }

    /**
     * Returns a list of countries and their corresponding passport photo dimensions.
     *
     * @return list of countries with display names and dimension strings
     */
    @GetMapping("/countries")
    public ResponseEntity<List<Map<String, String>>> getCountryList() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, int[]> rawMap = mapper.readValue(
                new ClassPathResource("dimensions/countries.json").getInputStream(),
                new TypeReference<Map<String, int[]>>() {}
            );

            List<Map<String, String>> countryList = new ArrayList<>();

            for (Map.Entry<String, int[]> entry : rawMap.entrySet()) {
                String code = entry.getKey();
                int[] dims = entry.getValue();

                Map<String, String> item = new HashMap<>();
                item.put("code", code);
                item.put("name", getCountryNameFromCode(code));
                item.put("dimensions", dims[0] + "x" + dims[1]);
                countryList.add(item);
            }

            return ResponseEntity.ok(countryList);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    /**
     * Returns a list of template labels and their dimension sizes.
     *
     * @return list of templates with size labels
     */
    @GetMapping("/templates")
    public ResponseEntity<List<Map<String, String>>> getTemplateList() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, int[]> rawMap = mapper.readValue(
                new ClassPathResource("dimensions/templates.json").getInputStream(),
                new TypeReference<Map<String, int[]>>() {}
            );

            List<Map<String, String>> templateList = new ArrayList<>();

            for (Map.Entry<String, int[]> entry : rawMap.entrySet()) {
                String label = entry.getKey();
                int[] dims = entry.getValue();

                Map<String, String> item = new HashMap<>();
                item.put("label", label);
                item.put("size", dims[0] + "x" + dims[1]);
                templateList.add(item);
            }

            return ResponseEntity.ok(templateList);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    /**
     * Resizes an uploaded image based on selected country, template, or custom dimensions.
     *
     * @param file          the uploaded image file
     * @param country       the selected country code (optional)
     * @param template      the selected template label (optional)
     * @param customWidth   the custom width (optional)
     * @param customHeight  the custom height (optional)
     * @return the resized image in base64 format
     */
    @PostMapping("/resize")
    public ResponseEntity<ImageResizeResponse> resizeImage(
        @RequestParam(value = "image", required = false) MultipartFile file,
        @RequestParam(value = "country", required = false) String country,
        @RequestParam(value = "template", required = false) String template,
        @RequestParam(value = "customWidth", required = false) Integer customWidth, 
        @RequestParam(value = "customHeight", required = false) Integer customHeight) {
        try {
            String dataUrl = imageResizingService.resizeImage(file, country, template, customWidth, customHeight);
            return ResponseEntity.ok(new ImageResizeResponse("success", "Image resized successfully", dataUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ImageResizeResponse("error", "Image resize failed: " + e.getMessage(), null));
        }
    }

    /**
     * Maps a country code to a human-readable name.
     *
     * @param code the 3-letter country code
     * @return the full country name
     */
    private String getCountryNameFromCode(String code) {
        return switch (code.toLowerCase()) {
            case "jpn" -> "Japan";
            case "sgp" -> "Singapore";
            case "chn" -> "China";
            case "mas" -> "Malaysia";
            default -> code.toUpperCase();
        };
    }
}
