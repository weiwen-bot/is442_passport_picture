/*
 * BackgroundRemovalController.java
 *
 * This controller handles background removal and transformation
 * for passport photo images in the Passport Picture Project.
 *
 */

package com.passportphoto.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.passportphoto.dto.ImageBackgroundRemovalResponse;
import com.passportphoto.service.BackgroundRemovalService;

/**
 * The {@code BackgroundRemovalController} provides an endpoint to remove
 * or replace the background of a user-uploaded image.
 */
@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173/")
public class BackgroundRemovalController {

    /** Service that handles background processing and replacement */
    private final BackgroundRemovalService backgroundRemovalService;

    /**
     * Constructs the controller with the required background removal service.
     *
     * @param backgroundRemovalService service to process background transformations
     */
    public BackgroundRemovalController(BackgroundRemovalService backgroundRemovalService) {
        this.backgroundRemovalService = backgroundRemovalService;
    }

    /**
     * Removes or replaces the background of the uploaded image.
     *
     * @param file             the image file to process
     * @param colorString      optional hex color for solid background
     * @param backgroundString optional code for preset background
     * @return a base64-encoded processed image or an error message
     */
    @PostMapping("/removebg")
    public ResponseEntity<ImageBackgroundRemovalResponse> removalbg(
            @RequestParam(value = "image", required = false) MultipartFile file,
            @RequestParam(value = "colorString", required = false) String colorString,
            @RequestParam(value = "backgroundString", required = false) String backgroundString) throws Exception {
        String processedBase64 = backgroundRemovalService.processImage(file, colorString, backgroundString);
        return ResponseEntity
                .ok(new ImageBackgroundRemovalResponse("success", "Image Removed Background", processedBase64));
    }
}
