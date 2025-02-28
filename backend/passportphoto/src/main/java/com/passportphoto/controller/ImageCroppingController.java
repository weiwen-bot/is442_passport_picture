package com.passportphoto.controller;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend origin
public class ImageCroppingController {

    @Value("${upload.directory:uploads/}") // Set upload directory with default value
    private String uploadDir;

    static {
        System.out.println("OpenCV is initialized successfully.");
    }


    // Crop the uploaded image
    @PostMapping("/crop")
    public ResponseEntity<String> cropImage(
        @RequestParam("image") MultipartFile file,
        @RequestParam("cropX") double x,
        @RequestParam("cropY") double y,
        @RequestParam("cropWidth") double width,
        @RequestParam("cropHeight") double height) {
        try {
            // Convert the uploaded file to BufferedImage
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

             // Cast the double values to int before using them for the subimage
            int cropX = (int) x;
            int cropY = (int) y;
            int cropWidth = (int) width;
            int cropHeight = (int) height;
            
            // Crop the image based on the given coordinates and size
            BufferedImage croppedImage = originalImage.getSubimage(cropX, cropY, cropWidth, cropHeight);

            // Convert the cropped image to a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(croppedImage, "jpg", baos);
            baos.flush();
            byte[] croppedImageBytes = baos.toByteArray();
            baos.close();

            // Convert byte array to Base64 string
            String base64Image = Base64.getEncoder().encodeToString(croppedImageBytes);

            // Return the Base64 string of the cropped image
            return ResponseEntity.ok("{\"status\":\"success\", \"message\":\"Image cropped successfully\", \"image\":\"data:image/jpeg;base64," + base64Image + "\"}");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("{\"status\":\"error\", \"message\":\"Image crop failed\"}");
        }
    }

}
