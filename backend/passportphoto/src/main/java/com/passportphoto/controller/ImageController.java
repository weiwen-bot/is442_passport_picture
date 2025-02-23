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

import java.util.HashMap;
import java.util.Map;

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

    // Country-Specific Requirements (Sizes and Background Colours)
    private static final Map<String, int[]> COUNTRY_SPECS = new HashMap<>();
    private static final Map<String, Scalar> COUNTRY_BACKGROUNDS = new HashMap<>();

    // Using 300 DPI - (mm * dpi) / 25.4
    static {
        COUNTRY_SPECS.put("USA", new int[]{602, 602}); // USA = 51mm x 51mm
        COUNTRY_SPECS.put("SGP", new int[]{400, 514});
        COUNTRY_SPECS.put("JPN", new int[]{413, 531}); // JAPAN = 35mm x 45mm
        COUNTRY_SPECS.put("CHN", new int[]{390, 567}); // CHINA = 33mm x 48mm
        COUNTRY_SPECS.put("KOR", new int[]{413, 531}); // SOUTH KOREA = 35mm x 45mm
        COUNTRY_SPECS.put("FRA", new int[]{413, 531}); // FRANCE = 35mm x 45mm
        COUNTRY_SPECS.put("MAS", new int[]{413, 591}); // MALAYSIA = 35mm x 50mm

        COUNTRY_BACKGROUNDS.put("USA", new Scalar(242, 240, 239)); // off-white
        COUNTRY_BACKGROUNDS.put("SGP", new Scalar(255, 255, 255));
        COUNTRY_BACKGROUNDS.put("JPN", new Scalar(255, 255, 255));
        COUNTRY_BACKGROUNDS.put("CHN", new Scalar(255, 255, 255));
        COUNTRY_BACKGROUNDS.put("KOR", new Scalar(255, 255, 255));
        COUNTRY_BACKGROUNDS.put("FRA", new Scalar(211, 211, 211)); // light grey
        COUNTRY_BACKGROUNDS.put("MAS", new Scalar(255, 255, 255));
    }

    // 1. Upload Image Endpoint
    @PostMapping("/upload")
    public String uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            return "{\"status\":\"success\", \"message\":\"Image uploaded successfully\", \"image\":\"data:image/jpeg;base64," + base64Image + "\"}";
        } catch (IOException e) {
            return "{\"status\":\"error\", \"message\":\"Image processing failed\"}";
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
            @RequestParam("cropHeight") int cropHeight,
            @RequestParam("country") String country
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

        // Perform cropping 
        Rect cropRect = new Rect(cropX, cropY, cropWidth, cropHeight);
        Mat croppedImage = new Mat(image, cropRect);

        // Resize based on country-specific dimensions
        if (COUNTRY_SPECS.containsKey(country)) {
            int[] dimensions = COUNTRY_SPECS.get(country);
            croppedImage = resizeImage(croppedImage, dimensions[0], dimensions[1]);
        }

        String croppedFilename = "cropped_" + filename;
        String croppedFilePath = UPLOAD_DIR + croppedFilename;

        Imgcodecs.imwrite(croppedFilePath, croppedImage);

        return "{\"status\":\"success\", \"message\":\"Image cropped and resized successfully\", \"image\":\"" + croppedFilename + "\"}";
    }

}
