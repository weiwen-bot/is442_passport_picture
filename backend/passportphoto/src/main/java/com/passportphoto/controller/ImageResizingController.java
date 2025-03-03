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
    public ResponseEntity<String> resizeImage(@RequestParam("image") MultipartFile file, @RequestParam("country") String country) {
        try {
            // Convert uploaded image to BufferedImage
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            // Convert BufferedImage to OpenCV Mat
            Mat imageMat = bufferedImageToMat(originalImage);

            // Get target passport size
            int[] dimensions = getPassportPhotoDimensions(country);
            int targetWidth = dimensions[0];
            int targetHeight = dimensions[1];

            // 1. Resize using COVER strategy
            Size newSize = calculateCoverSize(imageMat.size(), targetWidth, targetHeight);
            Mat resizedMat = new Mat();
            Imgproc.resize(imageMat, resizedMat, newSize, 0, 0, Imgproc.INTER_AREA);

            // 2. Center and crop if needed
            Mat croppedMat = centerCrop(resizedMat, targetWidth, targetHeight);
            resizedMat.release();

            // 3. Extend the background to fill any remaining gaps
            Mat finalImage = extendBackground(croppedMat, targetWidth, targetHeight);
            croppedMat.release();

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

    // Method to maintain aspect ratio with COVER strategy
    private Size calculateCoverSize(Size originalSize, int targetWidth, int targetHeight) {
        double originalAspect = originalSize.width / originalSize.height;
        double targetAspect = (double) targetWidth / targetHeight;
    
        int newWidth, newHeight;
    
        if (originalAspect > targetAspect) {
            // Wider than target aspect ratio → Scale by height
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * originalAspect);
        } else {
            // Taller than target aspect ratio → Scale by width
            newWidth = targetWidth;
            newHeight = (int) (targetWidth / originalAspect);
        }
    
        return new Size(newWidth, newHeight);
    }

    // Method to center crop
    private Mat centerCrop(Mat image, int targetWidth, int targetHeight) {
        int imgWidth = image.width();
        int imgHeight = image.height();

        if (imgWidth < targetWidth || imgHeight < targetHeight) {
            System.out.println("Warning: Image is smaller than target size. Skipping crop.");
            return image.clone();
        }

        int startX = (int) ((image.width() - targetWidth) / 2.0);
        int startY = (int) ((image.height() - targetHeight) / 2.0);
        
        // Ensure we don't go out of bounds
        startX = Math.max(0, startX);
        startY = Math.max(0, startY);
        
        // Create ROI for center crop
        Rect roi = new Rect(startX, startY, 
                            Math.min(targetWidth, image.width() - startX), 
                            Math.min(targetHeight, image.height() - startY));
        
        return new Mat(image, roi);
    }

    private Mat extendBackground(Mat image, int targetWidth, int targetHeight) {
        // If the image already matches the target size, return it as is
        if (image.width() == targetWidth && image.height() == targetHeight) {
            return image.clone();
        }
        
        // Create a new canvas of the target size
        Mat extendedMat = new Mat(targetHeight, targetWidth, image.type());
        
        // Calculate where to place the image in the canvas
        int xOffset = (targetWidth - image.width()) / 2;
        int yOffset = (targetHeight - image.height()) / 2;
        
        // Create a ROI in the target canvas
        Rect roi = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat destinationRoi = extendedMat.submat(roi);
        
        // Copy the image to the ROI
        image.copyTo(destinationRoi);
        
        // If there are any gaps, fill them with content-aware extension
        if (xOffset > 0 || yOffset > 0) {
            // Handle left edge
            if (xOffset > 0) {
                for (int y = 0; y < image.height(); y++) {
                    for (int x = 0; x < xOffset; x++) {
                        double blendFactor = (double) x / xOffset;
                        byte[] edgePixel = new byte[3];
                        image.get(y, 0, edgePixel);
                        extendedMat.put(y + yOffset, x, edgePixel);
                    }
                }
            }
            
            // Handle right edge
            if (xOffset > 0) {
                int rightStart = xOffset + image.width();
                for (int y = 0; y < image.height(); y++) {
                    for (int x = rightStart; x < targetWidth; x++) {
                        byte[] edgePixel = new byte[3];
                        image.get(y, image.width() - 1, edgePixel);
                        extendedMat.put(y + yOffset, x, edgePixel);
                    }
                }
            }
            
            // Handle top edge
            if (yOffset > 0) {
                for (int x = 0; x < image.width(); x++) {
                    for (int y = 0; y < yOffset; y++) {
                        byte[] edgePixel = new byte[3];
                        image.get(0, x, edgePixel);
                        extendedMat.put(y, x + xOffset, edgePixel);
                    }
                }
            }
            
            // Handle bottom edge
            if (yOffset > 0) {
                int bottomStart = yOffset + image.height();
                for (int x = 0; x < image.width(); x++) {
                    for (int y = bottomStart; y < targetHeight; y++) {
                        byte[] edgePixel = new byte[3];
                        image.get(image.height() - 1, x, edgePixel);
                        extendedMat.put(y, x + xOffset, edgePixel);
                    }
                }
            }
            
            // Fill the corners with the corner pixels
            // Top-left corner
            if (xOffset > 0 && yOffset > 0) {
                byte[] cornerPixel = new byte[3];
                image.get(0, 0, cornerPixel);
                for (int y = 0; y < yOffset; y++) {
                    for (int x = 0; x < xOffset; x++) {
                        extendedMat.put(y, x, cornerPixel);
                    }
                }
            }
            
            // Top-right corner
            if (xOffset > 0 && yOffset > 0) {
                byte[] cornerPixel = new byte[3];
                image.get(0, image.width() - 1, cornerPixel);
                for (int y = 0; y < yOffset; y++) {
                    for (int x = xOffset + image.width(); x < targetWidth; x++) {
                        extendedMat.put(y, x, cornerPixel);
                    }
                }
            }
            
            // Bottom-left corner
            if (xOffset > 0 && yOffset > 0) {
                byte[] cornerPixel = new byte[3];
                image.get(image.height() - 1, 0, cornerPixel);
                for (int y = yOffset + image.height(); y < targetHeight; y++) {
                    for (int x = 0; x < xOffset; x++) {
                        extendedMat.put(y, x, cornerPixel);
                    }
                }
            }
            
            // Bottom-right corner
            if (xOffset > 0 && yOffset > 0) {
                byte[] cornerPixel = new byte[3];
                image.get(image.height() - 1, image.width() - 1, cornerPixel);
                for (int y = yOffset + image.height(); y < targetHeight; y++) {
                    for (int x = xOffset + image.width(); x < targetWidth; x++) {
                        extendedMat.put(y, x, cornerPixel);
                    }
                }
            }
        }
        
        // Apply a slight blur to any extended regions to make them look more natural
        int blurRadius = 3;
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(extendedMat, blurred, new Size(blurRadius * 2 + 1, blurRadius * 2 + 1), 0);
        
        // Create a mask for the original image area
        Mat mask = new Mat(targetHeight, targetWidth, CvType.CV_8UC1, new Scalar(255));
        Imgproc.rectangle(mask, new Rect(xOffset, yOffset, image.width(), image.height()), new Scalar(0), -1);
        
        // Only apply blur to the extended regions
        blurred.copyTo(extendedMat, mask);
        
        return extendedMat;
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
