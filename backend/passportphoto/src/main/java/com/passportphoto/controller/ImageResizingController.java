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

            // Resize using OpenCV
            Mat resizedMat = new Mat();
            Size size = new Size(targetWidth, targetHeight);
            Imgproc.resize(imageMat, resizedMat, size);

            // Convert back to BufferedImage
            BufferedImage resizedImage = matToBufferedImage(resizedMat);

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

    // Method to convert BufferedImage to OpenCV Mat
    private Mat bufferedImageToMat(BufferedImage image) {
        int type = image.getType();
    
        if (type == BufferedImage.TYPE_BYTE_GRAY) {
            // Convert grayscale image to RGB (3 channels)
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g2d = rgbImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            image = rgbImage;
        }
    
        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }

    // Method to convert OpenCV Mat back to BufferedImage
    private BufferedImage matToBufferedImage(Mat mat) {
        byte[] data = new byte[mat.rows() * mat.cols() * (int) (mat.elemSize())];
        mat.get(0, 0, data);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_3BYTE_BGR);
        image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
        return image;
    }

    // Method to get passport photo dimensions based on the selected country
    // pixels = size in mm / 25.4 * DPI
    private int[] getPassportPhotoDimensions(String country) {
        switch (country.toLowerCase()) {
            case "JPN":
                return new int[]{413, 531}; // 35mm x 45mm at 300 dpi
            case "USA":
                return new int[]{602, 602}; // 51mm x 51mm at 300 dpi
            case "SGP":
                return new int[]{413, 531}; // 35mm x 45mm at 300 dpi
            case "CHN":
                return new int[]{390, 567}; // 33mm x 48mmm at 300 dpi
            case "MAS":
                return new int[]{413, 591}; // 35mm x 50 mm at 300 dpi
            default:
                return new int[]{413, 531}; // Default to JPN/SGP size
        }
    }
}
