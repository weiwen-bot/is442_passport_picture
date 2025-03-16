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
        {"code":"jpn", "name":"Japan"},
        {"code":"usa", "name":"United States"},
        {"code":"sgp", "name":"Singapore"},
        {"code":"chn", "name":"China"},
        {"code":"mas", "name":"Malaysia"}
    ]
    """;

    @GetMapping("/countries")
    public ResponseEntity<String> getCountryList() {
        return ResponseEntity.ok(COUNTRY_LIST_JSON);
    }

    @PostMapping("/resize")
    public ResponseEntity<ImageResizeResponse> resizeImage(@RequestParam("image") MultipartFile file, @RequestParam("country") String country) {
        try {
            return ResponseEntity.ok(imageResizingService.resizeImage(file, country));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ImageResizeResponse("error", "Image resize failed: " + e.getMessage(), null));
        }
    }
}
