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

    private static final String TEMPLATE_LIST_JSON = """
    [
        {"label":"2R", "size":"600x900"},
        {"label":"3R", "size":"1050x1500"},
        {"label":"4R", "size":"1200x1800"},
        {"label":"Instagram Post", "size":"1080x1080"},
        {"label":"Instagram Story", "size":"1080x1920"},
        {"label":"YouTube Thumbnail", "size":"1280x720"},
        {"label":"HD Wallpaper", "size":"1920x1080"},
        {"label":"4K Wallpaper", "size":"3840x2160"}
    ]
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
