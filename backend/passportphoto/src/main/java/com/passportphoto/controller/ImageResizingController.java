package com.passportphoto.controller;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.awt.Graphics2D;

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173")

public class ImageResizingController {
    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("OpenCV loaded successfully.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV: " + e.getMessage());
        }
    }

    @PostMapping("/resize")
    public ResponseEntity<String> resizeImage(@RequestParam("image") MultipartFile file,
                                              @RequestParam("country") String country) {
        try {
            // Convert uploaded image to BufferedImage
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            // Convert BufferedImage to OpenCV Mat
            Mat imageMat = bufferedImageToMat(originalImage);

            // Get target passport size
            int[] dimensions = getPassportPhotoDimensions(country);
            int targetWidth = dimensions[0];
            int targetHeight = dimensions[1];

            // Resize while maintaining aspect ratio
            Size newSize = calculateAspectRatioSize(imageMat.size(), targetWidth, targetHeight);
            Mat resizedMat = new Mat();
            Imgproc.resize(imageMat, resizedMat, newSize, 0, 0, Imgproc.INTER_AREA);

            // Create a white background image of exact passport size
            Mat finalImage = new Mat(targetHeight, targetWidth, CvType.CV_8UC3, new Scalar(255, 255, 255));

            // Calculate center position for padding
            int xOffset = (targetWidth - (int) newSize.width) / 2;
            int yOffset = (targetHeight - (int) newSize.height) / 2;
            Rect roi = new Rect(xOffset, yOffset, (int) newSize.width, (int) newSize.height);
            Mat destinationROI = finalImage.submat(roi);
            resizedMat.copyTo(destinationROI);

            // Convert back to BufferedImage
            BufferedImage resizedImage = matToBufferedImage(finalImage);

            // Convert to Base64 for frontend
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);
            byte[] resizedImageBytes = baos.toByteArray();
            baos.close();

            String base64Image = Base64.getEncoder().encodeToString(resizedImageBytes);
            return ResponseEntity.ok("{\"status\":\"success\", \"message\":\"Image resized successfully\", \"image\":\"data:image/jpeg;base64," + base64Image + "\"}");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("{\"status\":\"error\", \"message\":\"Image resize failed\"}");
        }
    }

    // Method to maintain aspect ratio
    private Size calculateAspectRatioSize(Size originalSize, int targetWidth, int targetHeight) {
        double originalAspect = originalSize.width / originalSize.height;
        double targetAspect = (double) targetWidth / targetHeight;

        int newWidth, newHeight;
    
        if (originalAspect > targetAspect) {
            // Image is wider than target aspect ratio, fit width
            newWidth = targetWidth;
            newHeight = (int) (targetWidth / originalAspect);
        } else {
            // Image is taller than target aspect ratio, fit height
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * originalAspect);
        }
    
        return new Size(newWidth, newHeight);
    }
    

    // Method to convert BufferedImage to OpenCV Mat
    private Mat bufferedImageToMat(BufferedImage image) {
        // Create a new Mat of the right size
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
    
        // Get RGB data from the image by directly sampling pixels
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                byte[] bgr = new byte[3];
            
                // Extract RGB components and store in BGR order for OpenCV
                bgr[0] = (byte) ((rgb) & 0xFF);        // Blue
                bgr[1] = (byte) ((rgb >> 8) & 0xFF);   // Green
                bgr[2] = (byte) ((rgb >> 16) & 0xFF);  // Red
            
                mat.put(y, x, bgr);
            }
        }
    
        return mat;
    }

    // Method to convert OpenCV Mat back to BufferedImage
    private BufferedImage matToBufferedImage(Mat mat) {
        // Create a BufferedImage
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_3BYTE_BGR);
    
        // Get each pixel from the Mat and set it in the BufferedImage
        byte[] bgr = new byte[3];
        for (int y = 0; y < mat.rows(); y++) {
            for (int x = 0; x < mat.cols(); x++) {
                mat.get(y, x, bgr);
            
                // Convert from BGR to RGB for Java
                int rgb = (bgr[2] & 0xFF) << 16 | (bgr[1] & 0xFF) << 8 | (bgr[0] & 0xFF);
                image.setRGB(x, y, rgb);
            }
        }
    
        return image;
    }

    // Method to get passport photo dimensions based on the selected country
    // pixels = size in mm / 25.4 * DPI
    private int[] getPassportPhotoDimensions(String country) {
        if (country == null || country.trim().isEmpty()) {
            System.out.println("Country input is null or empty, defaulting to 413x531");
            return new int[]{413, 531}; // Default size
        }

        country = country.trim().toLowerCase();
        System.out.println("Received country: [" + country + "]");

        switch (country.toLowerCase()) {
            case "jpn":
                return new int[]{413, 531}; // 35mm x 45mm at 300 dpi
            case "usa":
                return new int[]{602, 602}; // 51mm x 51mm at 300 dpi
            case "sgp":
                return new int[]{413, 531}; // 35mm x 45mm at 300 dpi
            case "chn":
                return new int[]{390, 567}; // 33mm x 48mmm at 300 dpi
            case "mas":
                return new int[]{413, 591}; // 35mm x 50 mm at 300 dpi
            default:
                System.out.println("Country not found, defaulting to 413x531");
                return new int[]{413, 531}; // Default to JPN/SGP size
        }
    }
}
