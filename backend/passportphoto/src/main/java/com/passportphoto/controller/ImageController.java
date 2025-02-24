package com.passportphoto.controller;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend origin
public class ImageController {

    private static final String UPLOAD_DIR = "uploads/";

    static {
        Dotenv dotenv = Dotenv.load();
        String opencvDllPath = dotenv.get("OPENCVDLLPATH");
        if (opencvDllPath != null) {
            try {
                System.load(opencvDllPath);
                System.out.println("OpenCV library loaded successfully.");
            } catch (UnsatisfiedLinkError e) {
                throw new RuntimeException("Failed to load OpenCV library", e);
            }
        } else {
            throw new RuntimeException("OPENCVDLLPATH not found in .env file.");
        }
    }

    // Add a method to resize the image before cropping
    private Mat resizeImage(Mat image, int width, int height) {
        Mat resizedImage = new Mat();
        Imgproc.resize(image, resizedImage, new Size(width, height));
        return resizedImage;
    }

     // 2. Crop Image Endpoint
    // Modify the cropImage method to take into account the resized dimensions
    @PostMapping("/crop")
    public String cropImage(
            @RequestParam("filename") String filename,
            @RequestParam("cropX") int cropX,
            @RequestParam("cropY") int cropY,
            @RequestParam("cropWidth") int cropWidth,
            @RequestParam("cropHeight") int cropHeight
    ) {
        String filePath = UPLOAD_DIR + filename;
        Mat image = Imgcodecs.imread(filePath);

        if (image.empty()) {
            return "{\"status\":\"error\", \"message\":\"Image not found\"}";
        }

        // Check if the crop area is within the image bounds
        if (cropX + cropWidth > image.cols() || cropY + cropHeight > image.rows()) {
            return "{\"status\":\"error\", \"message\":\"Crop area exceeds image boundaries\"}";
        }

        // Resize image if required (e.g., based on country dimensions from frontend)
        int targetWidth = cropWidth;
        int targetHeight = cropHeight;
        image = resizeImage(image, targetWidth, targetHeight);

        Rect cropRect = new Rect(cropX, cropY, cropWidth, cropHeight);
        Mat croppedImage = new Mat(image, cropRect);

        String croppedFilename = "cropped_" + filename;
        String croppedFilePath = UPLOAD_DIR + croppedFilename;

        Imgcodecs.imwrite(croppedFilePath, croppedImage);

        return "{\"status\":\"success\", \"message\":\"Image cropped and resized successfully\", \"image\":\"" + croppedFilename + "\"}";
    }

}
