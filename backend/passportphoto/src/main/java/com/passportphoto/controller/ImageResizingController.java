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
    

    // Method to extend background - using OpenCV
    private Mat extendBackground(Mat image, int targetWidth, int targetHeight) {
        // 1. If image already matches target, just return
        if (image.width() == targetWidth && image.height() == targetHeight) {
            return image.clone();
        }
    
        // 2. Create offsets for centering
        int xOffset = (int) Math.round((targetWidth - image.width()) / 2.0);
        int yOffset = (int) Math.round((targetHeight - image.height()) / 2.0);
    
        // 3. Create a larger canvas if needed
        Mat extendedMat = new Mat(targetHeight, targetWidth, image.type());
        // First, fill with black (or zero), or you could fill with white if you'd like:
        extendedMat.setTo(new Scalar(0,0,0));
    
        // 4. Copy the main image into the center of extendedMat
        Rect roi = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat centerRoi = extendedMat.submat(roi);
        image.copyTo(centerRoi);
    
        // 5. We now replicate the border (which is the region outside the ROI).
        //    Easiest approach: extract the center area to a temporary Mat, call copyMakeBorder
        Mat justCenter = new Mat();
        // Copy just the center part from extendedMat
        extendedMat.submat(roi).copyTo(justCenter);
    
        // Now we build a new Mat of size targetWidth/targetHeight using copyMakeBorder:
        Mat borderReplicated = new Mat();
        int top = yOffset;
        int bottom = targetHeight - image.height() - yOffset;
        int left = xOffset;
        int right = targetWidth - image.width() - xOffset;
    
        Core.copyMakeBorder(
            justCenter,
            borderReplicated,
            top, bottom, left, right,
            Core.BORDER_REPLICATE
        );
    
        // 'borderReplicated' is now the final image with extended edges
        // 6. Light blur *only the newly created border region* if desired
        Mat blurred = applySoftBlurToBorder(borderReplicated, roi);
    
        // Clean up
        justCenter.release();
        return blurred;
    }
    
    private Mat applySoftBlurToBorder(Mat extendedMat, Rect roi) {
        // We'll do a small blur just outside the ROI edges
    
        // 1. Duplicate the entire extendedMat
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(extendedMat, blurred, new Size(9, 9), 0); 
        // use a smaller kernel if you want minimal blur
    
        // 2. Create a mask where the ROI is black (unblurred) and the rest is white (blurred)
        Mat mask = Mat.zeros(extendedMat.size(), CvType.CV_8UC1);
    
        // ROI is the main image region. We want that region *unblurred*, so set it to 0.
        // The region outside ROI remains at 0 => we actually want it to be 255 for blur.
        // We'll do the inverse:
        //   - Fill entire mask with 255
        //   - Then fill the ROI with black
        mask.setTo(new Scalar(255));
        Imgproc.rectangle(mask, roi, new Scalar(0), -1);
    
        // 3. Copy from 'blurred' into 'extendedMat' only where mask=255
        blurred.copyTo(extendedMat, mask);
    
        blurred.release();
        mask.release();
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
