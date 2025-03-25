package com.passportphoto.controller;

import com.passportphoto.dto.ImageResizeResponse;
import com.passportphoto.service.ImageResizingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173")

public class ImageResizingController {

    private final ImageResizingService imageResizingService;

    public ImageResizingController(ImageResizingService imageResizingService) {
        this.imageResizingService = imageResizingService;
    }

    private String loadJsonFromClasspath(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @GetMapping("/countries")
    public ResponseEntity<String> getCountryList() {
        try {
            String json = loadJsonFromClasspath("dimensions/countries.json");
            return ResponseEntity.ok(json);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("{\"error\": \"Failed to load countries.json\"}");
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<String> getTemplateList() {
        try {
            String json = loadJsonFromClasspath("dimensions/templates.json");
            return ResponseEntity.ok(json);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("{\"error\": \"Failed to load templates.json\"}");
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
