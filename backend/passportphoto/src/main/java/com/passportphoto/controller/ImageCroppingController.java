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

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173") 
public class ImageCroppingController {

    private final ImageCroppingService imageProcessingService;

    public ImageCroppingController(ImageCroppingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
    }

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

            // Ensure crop dimensions are valid
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