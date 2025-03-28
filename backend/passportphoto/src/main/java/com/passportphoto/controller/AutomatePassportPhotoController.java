package com.passportphoto.controller;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.passportphoto.dto.ImgDTO;
import com.passportphoto.service.AutomatePassportPhotoService;
import com.passportphoto.service.BackgroundRemovalService;
import org.springframework.web.multipart.MultipartFile;

import com.passportphoto.exceptions.*;

@RestController
@RequestMapping("/automate")
@CrossOrigin(origins = "http://localhost:5173")

public class AutomatePassportPhotoController {

    private final AutomatePassportPhotoService automatePassportPhotoService;

    public AutomatePassportPhotoController(AutomatePassportPhotoService automatePassportPhotoService) {
        this.automatePassportPhotoService = automatePassportPhotoService;
    }


    @PostMapping("/passportphoto")
    public ResponseEntity<Map<String, String>> automatePassportPhoto(
        @RequestParam(value = "image", required = false) MultipartFile file,
        @RequestParam(value = "country", required = false) String country,
        @RequestParam(value = "template", required = false) String template
    ) {
        try {
            String processedBase64 = automatePassportPhotoService.automatePassportPhoto(file,country,template);
            return ResponseEntity.ok(Collections.singletonMap("processedImage", processedBase64));
        } catch (ImageInvalidFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Image processing failed"));
        }
    }

}
