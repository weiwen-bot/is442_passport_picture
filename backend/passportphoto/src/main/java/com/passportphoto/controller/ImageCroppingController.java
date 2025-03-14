package com.passportphoto.controller;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend origin
public class ImageCroppingController {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageCroppingController.class);

    @PostMapping("/crop")
    public ResponseEntity<?> cropImage(@RequestParam("image") MultipartFile imageFile,
                                       @RequestParam("cropX") double x,
                                       @RequestParam("cropY") double y,
                                       @RequestParam("cropWidth") double width,
                                       @RequestParam("cropHeight") double height) {
        try {
            // Check if file is empty
            if (imageFile.isEmpty()) {
                System.out.println("Received empty image file!");
                return ResponseEntity.badRequest().body("Image file is empty!");
            }
    
            // Convert MultipartFile to BufferedImage
            BufferedImage inputImage = ImageIO.read(imageFile.getInputStream());
    
            if (inputImage == null || inputImage.getWidth() <= 0 || inputImage.getHeight() <= 0) {
                System.out.println("Invalid image data received!");
                return ResponseEntity.badRequest().body("Invalid image data received!");
            }
    
            // Log original image size
            System.out.println("Image dimensions: " + inputImage.getWidth() + "x" + inputImage.getHeight());
    
            // Ensure no transparency (for PNGs)
            if (inputImage.getColorModel().hasAlpha()) {
                BufferedImage converted = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                converted.getGraphics().drawImage(inputImage, 0, 0, null);
                inputImage = converted;
            }
    
            // Cast the double values to int
            int cropX = (int) x;
            int cropY = (int) y;
            int cropWidth = (int) width;
            int cropHeight = (int) height;
    
            System.out.println("Crop Dimensions: X=" + cropX + ", Y=" + cropY + ", W=" + cropWidth + ", H=" + cropHeight);
    
            // Ensure crop dimensions are valid
            if (cropX < 0 || cropY < 0 || cropWidth <= 0 || cropHeight <= 0 ||
                cropX + cropWidth > inputImage.getWidth() ||
                cropY + cropHeight > inputImage.getHeight()) {
                System.out.println("Invalid crop dimensions!");
                return ResponseEntity.badRequest().body("Invalid crop dimensions!");
            }
    
            // Correct way to crop an image (Avoid getSubimage issue)
            BufferedImage croppedImage = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_RGB);
            croppedImage.getGraphics().drawImage(inputImage, 0, 0, cropWidth, cropHeight, cropX, cropY, cropX + cropWidth, cropY + cropHeight, null);
    
            // Convert cropped image to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(croppedImage, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
    
            if (imageBytes.length == 0) {
                System.out.println("Cropped image is empty!");
                return ResponseEntity.badRequest().body("Cropped image is empty!");
            }
    
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            System.out.println("Generated Base64 Image: " + base64Image.substring(0, 100) + "...");
    
            Map<String, String> response = new HashMap<>();
            response.put("image", "data:image/jpeg;base64," + base64Image);
    
            return ResponseEntity.ok(response);
    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image: " + e.getMessage());
        }
    }
}
