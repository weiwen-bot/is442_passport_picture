package com.passportphoto.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ai.onnxruntime.*;

@Service
public class AutomatePassportPhotoService {

    private final BackgroundRemovalService backgroundRemovalService;

    private final ImageResizingService imageResizingService;

    public AutomatePassportPhotoService(BackgroundRemovalService backgroundRemovalService ,ImageResizingService imageResizingService ){
        this.backgroundRemovalService = backgroundRemovalService;
        this.imageResizingService = imageResizingService;
        
    }

    public String[] batchProcessing(List<MultipartFile> fileList, String country, String template) throws IOException, OrtException{
        String[] base64List = new String[fileList.size()];
        for (int i = 0; i < fileList.size(); i++){
            String processedImage = automatePassportPhoto(fileList.get(i), country, template);
            base64List[i] = processedImage;
        }
        return base64List;

    }

    public String automatePassportPhoto(MultipartFile file, String country, String template) throws IOException, OrtException{
        String base64String = imageResizingService.resizeImage(file, country, template, null, null);
        String resizeBase64Image = resizeToClosestMultipleOf32(base64String);
        MultipartFile resizeFile = convertBase64ToMultipartFile(resizeBase64Image,"uploaded-img.jpg");
        String processedBase64 = backgroundRemovalService.processImage(resizeFile, null, null);
        return processedBase64;
        
    }
    public MultipartFile convertBase64ToMultipartFile(String base64String, String fileName) throws IOException {
        // Split the Base64 string to extract the MIME type and the actual Base64 data
        String[] parts = base64String.split(",");
        String mimeType = parts[0].split(":")[1].split(";")[0]; // Extract MIME type (e.g., image/jpeg, image/png)
        String base64Data = parts[1]; // Extract Base64 data (skip the MIME part)

        // Decode the Base64 data to get the byte array
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

        // Create a ByteArrayInputStream from the decoded byte array
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedBytes);

        // Create and return a MultipartFile from the byte array, with dynamic MIME type
        return new MockMultipartFile(fileName, fileName, mimeType, byteArrayInputStream);
    }

    public String resizeToClosestMultipleOf32(String base64Image) {
        try {
            // Decode the base64 string into bytes
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image.split(",")[1]);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage img = ImageIO.read(byteArrayInputStream);
            System.out.println("Before");
            System.out.println(img.getWidth());
            System.out.println(img.getHeight());
            System.out.println();

            // Calculate new dimensions rounded to the closest multiple of 32
            int newWidth = roundToNearestMultiple(img.getWidth(), 32);
            int newHeight = roundToNearestMultiple(img.getHeight(), 32);

            // Create a new image with the new size
            // Image scaledImage = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            System.out.printf("%d %d",newHeight,newWidth);
            // Draw the scaled image onto the new buffered image
            Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(img, 0, 0,newWidth,newHeight, null);
            g2d.dispose();
            System.out.println("After");
            System.out.println(outputImage.getWidth());
            System.out.println(outputImage.getHeight());
            System.out.println();

            // Convert the resulting image to base64 string (PNG format)
            String finalBase64 = encodeToBase64(outputImage);
            return finalBase64;
        } catch (IOException e) {
            throw new RuntimeException("Error processing image", e);
        }
    }

    // Method to round a number to the nearest multiple of the given factor
    public int roundToNearestMultiple(int value, int m) {
        int remainder = value % m;
        // If remainder is less than or equal to half of m, subtract remainder
        // Otherwise, add the difference to the next multiple of m
        return remainder <= m / 2 ? value - remainder : value + (m - remainder);
    }
    // Method to encode the BufferedImage to Base64
    private String encodeToBase64(BufferedImage image) throws IOException {
        // Convert the image to a ByteArrayOutputStream
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        // Encode the byte array to Base64
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(imageBytes);
    }

}