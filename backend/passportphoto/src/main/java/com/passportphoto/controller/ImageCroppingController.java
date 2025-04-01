/*
 * ImageCroppingController.java
 *
 * This controller handles the image cropping endpoint for the Passport Picture Project.
 *
 */

package com.passportphoto.controller;

import com.passportphoto.dto.ImageCropRequest;
import com.passportphoto.service.ImageCroppingService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code ImageCroppingController} provides an endpoint to crop
 * a user-uploaded image based on the dimensions provided in the request.
 */
@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173") 
public class ImageCroppingController {

    /** Service that handles image conversion and cropping logic */
    private final ImageCroppingService imageProcessingService;

    /**
     * Constructs the controller with the given image processing service.
     *
     * @param imageProcessingService the service used for cropping and encoding images
     */
    public ImageCroppingController(ImageCroppingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
    }

    /**
     * Crops the uploaded image based on provided crop parameters.
     *
     * @param imageFile   the uploaded image
     * @param cropRequest the crop dimensions
     * @return a base64-encoded cropped image or an error message
     */
    @PostMapping("/crop")
    public ResponseEntity<?> cropImage(@RequestParam("image") MultipartFile imageFile,
                                       @ModelAttribute ImageCropRequest cropRequest) {
        try {
            if (imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Image file is empty!");
            }

            BufferedImage inputImage = imageProcessingService.convertToBufferedImage(imageFile);
            int cropX = (int) cropRequest.getCropX();
            int cropY = (int) cropRequest.getCropY();
            int cropWidth = (int) cropRequest.getCropWidth();
            int cropHeight = (int) cropRequest.getCropHeight();

            // Validate crop dimensions
            if (cropX < 0 || cropY < 0 || cropWidth <= 0 || cropHeight <= 0 ||
                cropX + cropWidth > inputImage.getWidth() ||
                cropY + cropHeight > inputImage.getHeight()) {
                return ResponseEntity.badRequest().body("Invalid crop dimensions!");
            }

            BufferedImage croppedImage = imageProcessingService.cropImage(inputImage, cropX, cropY, cropWidth, cropHeight);
            String base64Image = imageProcessingService.convertToBase64(croppedImage);

            Map<String, String> response = new HashMap<>();
            response.put("image", "data:image/jpeg;base64," + base64Image);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error processing image: " + e.getMessage());
        }
    }
}