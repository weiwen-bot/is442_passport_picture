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

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173")

public class ImageResizingController {

    private final ImageResizingService imageResizingService;

    public ImageResizingController(ImageResizingService imageResizingService) {
        this.imageResizingService = imageResizingService;
    }

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

    private String getCountryNameFromCode(String code) {
        return switch (code.toLowerCase()) {
            case "jpn" -> "Japan";
            case "usa" -> "United States";
            case "sgp" -> "Singapore";
            case "chn" -> "China";
            case "mas" -> "Malaysia";
            default -> code.toUpperCase();
        };
    }

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

    @PostMapping("/resize")
    public ResponseEntity<ImageResizeResponse> resizeImage(
        @RequestParam("image") MultipartFile file,  
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
}
