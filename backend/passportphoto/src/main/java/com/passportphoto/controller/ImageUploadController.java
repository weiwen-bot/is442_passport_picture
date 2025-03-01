package com.passportphoto.controller;

import java.io.IOException;
import java.util.Base64;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend origin
public class ImageUploadController {

    // 1. Upload Image Endpoint
    @PostMapping("/upload")
    public String uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            return "{\"status\":\"success\", \"message\":\"Image uploaded successfully\", \"image\":\"data:image/jpeg;base64," + base64Image + "\"}";
        } catch (IOException e) {
            return "{\"status\":\"error\", \"message\":\"Image processing failed: " + e.getMessage() + "\"}";
        }
    }
}
