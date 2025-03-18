package com.passportphoto.controller;

import com.passportphoto.service.ImageUploadService;
import com.passportphoto.dto.ImageUploadResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend origin
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    public ImageUploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ImageUploadResponse("error", "Image file is empty!", null));
            }

            String base64Image = imageUploadService.uploadImage(file);
            return ResponseEntity.ok(new ImageUploadResponse("success", "Image uploaded successfully", base64Image));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(new ImageUploadResponse("error", "Image processing failed: " + e.getMessage(), null));
        }
    }
}