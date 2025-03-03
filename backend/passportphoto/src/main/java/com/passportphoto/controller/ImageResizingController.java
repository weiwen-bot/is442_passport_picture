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

            // 1. Resize the image to fit the target dimensions
            Mat resizedMat = resizeToTarget(imageMat, targetWidth, targetHeight);

            // 2. Extend the background to fill any remaining gaps
            Mat finalImage = extendBackground(resizedMat, targetWidth, targetHeight);
            resizedMat.release();

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

    // Method to maintain aspect ratio with FIT strategy
    private Size calculateFitSize(Size originalSize, int targetWidth, int targetHeight) {
        double originalAspect = originalSize.width / originalSize.height;
        double targetAspect = (double) targetWidth / targetHeight;
    
        int newWidth, newHeight;
        if (originalAspect > targetAspect) {
            // Image is relatively wider -> match target's width
            newWidth = targetWidth;
            newHeight = (int) (targetWidth / originalAspect);
        } else {
            // Image is relatively taller (or equal) -> match target's height
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * originalAspect);
        }
        return new Size(newWidth, newHeight);
    }
    
    // Method to resize image to target
    private Mat resizeToTarget(Mat image, int targetWidth, int targetHeight) {
        int w = image.width();
        int h = image.height();
    
        // 1) If bigger in either dimension => "fit" using calculateFitSize()
        if (w > targetWidth || h > targetHeight) {
            Size newSize = calculateFitSize(image.size(), targetWidth, targetHeight);
    
            Mat resized = new Mat();
            Imgproc.resize(image, resized, newSize, 0, 0, Imgproc.INTER_AREA);
            return resized;
        }
    
        // 2) If smaller in either dimension => scale up (but do NOT exceed target)
        if (w < targetWidth || h < targetHeight) {
            double scaleX = (double) targetWidth / w;
            double scaleY = (double) targetHeight / h;
            // We use min(...) so we don't exceed the target in the other dimension
            double scale = Math.min(scaleX, scaleY);
    
            int newW = (int) Math.round(w * scale);
            int newH = (int) Math.round(h * scale);
    
            Mat resized = new Mat();
            Imgproc.resize(image, resized, new Size(newW, newH), 0, 0, Imgproc.INTER_AREA);
            return resized;
        }
    
        // 3) If it’s already within targetWidth x targetHeight => no resize
        return image;
    }
    

    // Method to extend background
    private Mat extendBackground(Mat image, int targetWidth, int targetHeight) {
        // If the image already matches the target size, return it as is
        if (image.width() == targetWidth && image.height() == targetHeight) {
            return image.clone();
        }
    
        // Create a new canvas of the target size
        Mat extendedMat = new Mat(targetHeight, targetWidth, image.type(), new Scalar(0, 0, 0));
    
        // Calculate where to place the image in the canvas
        int xOffset = (targetWidth - image.width()) / 2;
        int yOffset = (targetHeight - image.height()) / 2;
    
        // Create a ROI in the target canvas
        Rect roi = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat destinationRoi = extendedMat.submat(roi);
    
        // Copy the image to the ROI
        image.copyTo(destinationRoi);
    
        // If there are any gaps, fill them with improved extension
        if (xOffset > 0 || yOffset > 0) {
            // Extend left edge
            if (xOffset > 0) {
                for (int y = 0; y < image.height(); y++) {
                    // Get the leftmost pixel of this row
                    byte[] edgePixel = new byte[3];
                    image.get(y, 0, edgePixel);
                    
                    // Fill left extension
                    for (int x = 0; x < xOffset; x++) {
                        extendedMat.put(y + yOffset, x, edgePixel);
                    }
                }
            }
            
            // Extend right edge
            if (xOffset > 0) {
                for (int y = 0; y < image.height(); y++) {
                    // Get the rightmost pixel of this row
                    byte[] edgePixel = new byte[3];
                    image.get(y, image.width() - 1, edgePixel);
                    
                    // Fill right extension
                    for (int x = xOffset + image.width(); x < targetWidth; x++) {
                        extendedMat.put(y + yOffset, x, edgePixel);
                    }
                }
            }
            
            // Extend top edge
            if (yOffset > 0) {
                for (int x = 0; x < image.width(); x++) {
                    // Get the topmost pixel of this column
                    byte[] edgePixel = new byte[3];
                    image.get(0, x, edgePixel);
                    
                    // Fill top extension
                    for (int y = 0; y < yOffset; y++) {
                        extendedMat.put(y, x + xOffset, edgePixel);
                    }
                }
            }
            
            // Extend bottom edge
            if (yOffset > 0) {
                for (int x = 0; x < image.width(); x++) {
                    // Get the bottommost pixel of this column
                    byte[] edgePixel = new byte[3];
                    image.get(image.height() - 1, x, edgePixel);
                    
                    // Fill bottom extension
                    for (int y = yOffset + image.height(); y < targetHeight; y++) {
                        extendedMat.put(y, x + xOffset, edgePixel);
                    }
                }
            }
            
            // Fill top-left corner
            if (xOffset > 0 && yOffset > 0) {
                byte[] cornerPixel = new byte[3];
                image.get(0, 0, cornerPixel);
                for (int y = 0; y < yOffset; y++) {
                    for (int x = 0; x < xOffset; x++) {
                        extendedMat.put(y, x, cornerPixel);
                    }
                }
            }
            
            // Fill top-right corner
            if (xOffset > 0 && yOffset > 0) {
                byte[] cornerPixel = new byte[3];
                image.get(0, image.width() - 1, cornerPixel);
                for (int y = 0; y < yOffset; y++) {
                    for (int x = xOffset + image.width(); x < targetWidth; x++) {
                        extendedMat.put(y, x, cornerPixel);
                    }
                }
            }
            
            // Fill bottom-left corner
            if (xOffset > 0 && yOffset > 0) {
                byte[] cornerPixel = new byte[3];
                image.get(image.height() - 1, 0, cornerPixel);
                for (int y = yOffset + image.height(); y < targetHeight; y++) {
                    for (int x = 0; x < xOffset; x++) {
                        extendedMat.put(y, x, cornerPixel);
                    }
                }
            }
            
            // Fill bottom-right corner
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
        // Calculate blur radius based on the extension size
        int maxExtension = Math.max(xOffset, yOffset);
        int blurRadius = Math.max(3, maxExtension / 10);
        
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(extendedMat, blurred, new Size(blurRadius * 2 + 1, blurRadius * 2 + 1), 0);
        
        // Create a mask for the original image area
        Mat mask = new Mat(targetHeight, targetWidth, CvType.CV_8UC1, new Scalar(255));
        Imgproc.rectangle(mask, roi, new Scalar(0), -1);
        
        // Only apply blur to the extended regions
        blurred.copyTo(extendedMat, mask);
        
        // Clean up
        mask.release();
        blurred.release();
        
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
