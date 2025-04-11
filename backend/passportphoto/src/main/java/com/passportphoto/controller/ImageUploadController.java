/*
 * ImageUploadController.java
 *
 * This controller handles the image upload endpoint for the Passport Picture Project.
 *
 */

package com.passportphoto.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.passportphoto.dto.ImageUploadResponse;
import com.passportphoto.service.ImageUploadService;

/**
 * The {@code ImageUploadController} class provides a REST endpoint for
 * uploading images. It accepts multipart image files and returns a response
 * with the base64-encoded image or an error message.
 */
@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173")
public class ImageUploadController {

    /** Service class that handles image upload logic */
    private final ImageUploadService imageUploadService;

    /**
     * Constructs the {@code ImageUploadController} with the required service.
     *
     * @param imageUploadService the service to process uploaded images
     */
    public ImageUploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    /**
     * Uploads an image file and returns a response with the base64-encoded image.
     *
     * @param imageFile the image file to upload
     * @return a response entity containing the upload status and image
     */
    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("image") MultipartFile imageFile) throws Exception {
        
        String base64Image = imageUploadService.uploadImage(imageFile);
        return ResponseEntity.ok(new ImageUploadResponse("success", "Image uploaded successfully", base64Image));

    }
}