package com.passportphoto.controller;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend origin
public class ImageUploadController {

    // 1. Upload Image Endpoint  
    @PostMapping("/upload")
    public String uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            // Convert MultipartFile to OpenCV Mat
            Mat image = Imgcodecs.imdecode(new MatOfByte(file.getBytes()), Imgcodecs.IMREAD_COLOR);

            // Print original size
            System.out.println("Original Image Size: " + image.width() + "x" + image.height());

            // Resize Image (Example: Resize to 2000x2000)
            Mat resizedImage = new Mat();
            Size size = new Size(2000, 1000);
            Imgproc.resize(image, resizedImage, size);

            // Print resized size
            System.out.println("Resized Image Size: " + resizedImage.width() + "x" + resizedImage.height());

            // Convert Mat to byte array
            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".jpg", resizedImage, matOfByte);
            byte[] imageBytes = matOfByte.toArray();

            // Convert to Base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            return "{\"status\":\"success\", \"message\":\"Image uploaded successfully\", \"image\":\"data:image/jpeg;base64," + base64Image + "\"}";
        } catch (IOException e) {
            return "{\"status\":\"error\", \"message\":\"Image processing failed: " + e.getMessage() + "\"}";
        }
    }
}
