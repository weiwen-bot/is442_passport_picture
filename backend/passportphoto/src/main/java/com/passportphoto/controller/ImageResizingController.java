package com.passportphoto.controller;

import com.passportphoto.dto.ImageResizeResponse;
import com.passportphoto.service.ImageResizingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173")

public class ImageResizingController {

    private final ImageResizingService imageResizingService;

    public ImageResizingController(ImageResizingService imageResizingService) {
        this.imageResizingService = imageResizingService;
    }

    private static final String COUNTRY_LIST_JSON = """
    [
        {"code":"jpn", "name":"Japan", "dimensions":"413x531"},
        {"code":"usa", "name":"United States", "dimensions":"602x602"},
        {"code":"sgp", "name":"Singapore", "dimensions":"413x531"},
        {"code":"chn", "name":"China", "dimensions":"390x567"},
        {"code":"mas", "name":"Malaysia", "dimensions":"413x591"}
    ]
    """;

    private static final String TEMPLATE_LIST_JSON = """
    [
        {"label":"SMU Student ID", "size":"354x472"},
        {"label":"NUS Student ID", "size":"340, 453"},
        {"label":"NTU Student ID", "size":"354x472"},
        {"label":"LinkedIn Profile", "size":"400x400"},
        {"label":"2R", "size":"600x900"},
    """;

    @GetMapping("/countries")
    public ResponseEntity<String> getCountryList() {
        return ResponseEntity.ok(COUNTRY_LIST_JSON);
    }

    @GetMapping("/templates")
    public ResponseEntity<String> getTemplateList() {
        return ResponseEntity.ok(TEMPLATE_LIST_JSON);
    }

    @PostMapping("/resize")
    public ResponseEntity<ImageResizeResponse> resizeImage(
        @RequestParam("image") MultipartFile file,  
        @RequestParam(value = "country", required = false) String country,
        @RequestParam(value = "template", required = false) String template,
        @RequestParam(value = "customWidth", required = false) Integer customWidth, 
        @RequestParam(value = "customHeight", required = false) Integer customHeight) {
        try {
            return ResponseEntity.ok(imageResizingService.resizeImage(file, country, template, customWidth, customHeight));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ImageResizeResponse("error", "Image resize failed: " + e.getMessage(), null));
        }
    }
}
