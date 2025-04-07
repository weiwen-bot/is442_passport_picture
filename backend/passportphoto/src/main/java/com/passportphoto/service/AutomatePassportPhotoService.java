/*
 * AutomatePassportPhotoService.java
 *
 * This service coordinates the pipeline for automating passport photo processing,
 * including resizing, alignment, and background removal.
 *
 */

package com.passportphoto.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import com.passportphoto.service.FaceCenteringService;

import org.opencv.core.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ai.onnxruntime.*;

/**
 * The {@code AutomatePassportPhotoService} class handles the end-to-end
 * pipeline for processing passport photos, including resizing to ONNX-compatible
 * dimensions and background removal.
 */
@Service
public class AutomatePassportPhotoService {

    private final BackgroundRemovalService backgroundRemovalService;
    private final ImageResizingService imageResizingService;
    private final FaceCenteringService faceCenteringService;

    /**
     * Constructs the service with required dependencies.
     */
    public AutomatePassportPhotoService(BackgroundRemovalService backgroundRemovalService,ImageResizingService imageResizingService, FaceCenteringService faceCenteringService ){
        this.backgroundRemovalService = backgroundRemovalService;
        this.imageResizingService = imageResizingService;
        this.faceCenteringService = faceCenteringService;
        
    }

    public String[] batchProcessing(List<MultipartFile> fileList, String country, String template) throws IOException, OrtException{
        String[] base64List = new String[fileList.size()];
        for (int i = 0; i < fileList.size(); i++){
            String processedImage = automatePassportPhoto(fileList.get(i), country, template);
            base64List[i] = processedImage;
        }
        return base64List;

    }

        

    /**
     * Orchestrates the full passport photo processing pipeline:
     * - Resize image
     * - Round dimensions to multiple of 32
     * - Perform background removal
     *
     * @param file     the uploaded image
     * @param country  country code for standard sizing
     * @param template optional template label
     * @return a processed image as base64 string
     * @throws IOException   if image processing fails
     * @throws OrtException  if ONNX model inference fails
     */
    public String automatePassportPhoto(MultipartFile file, String country, String template) throws IOException, OrtException{
        String base64Image = imageResizingService.resizeImage(file, country, template, null, null);
        MultipartFile resizeFile = convertBase64ToMultipartFile(base64Image,"uploaded-img.jpg");
        MultipartFile centeredImage = faceCenteringService.centerImage(resizeFile);
        String processedBase64 = backgroundRemovalService.processImage(centeredImage, null, null);
        
        return processedBase64;
    }

    /**
     * Converts a base64 image string to a {@link MultipartFile}.
     *
     * @param base64String the data URL of the image
     * @param fileName     the filename to assign
     * @return a MultipartFile wrapping the decoded image
     * @throws IOException if decoding fails
     */
    public MultipartFile convertBase64ToMultipartFile(String base64String, String fileName) throws IOException {
        String[] parts = base64String.split(",");
        String mimeType = parts[0].split(":")[1].split(";")[0];
        String base64Data = parts[1];
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedBytes);
        
        return new MockMultipartFile(fileName, fileName, mimeType, byteArrayInputStream);
    }

    /**
     * Resizes an image (in base64) to the nearest dimensions divisible by 32,
     * which is often required by neural network input layers.
     *
     * @param base64Image the original image in base64 format
     * @return the resized image as a base64 PNG string
     */
    public String resizeToClosestMultipleOf32(String base64Image)  {
        try {
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image.split(",")[1]);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage img = ImageIO.read(byteArrayInputStream);


            int newWidth = roundToNearestMultiple(img.getWidth(), 32);
            int newHeight = roundToNearestMultiple(img.getHeight(), 32);

            BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            System.out.printf("%d %d",newHeight,newWidth);
            Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(img, 0, 0,newWidth,newHeight, null);
            g2d.dispose();


            // Convert the resulting image to base64 string (PNG format)
            String finalBase64 = encodeToBase64(outputImage);
            return finalBase64;
        } catch (IOException e) {
            throw new RuntimeException("Error processing image", e);
        }
    }

    /**
     * Rounds a given value to the nearest multiple of m.
     */
    public int roundToNearestMultiple(int value, int m) {
        int remainder = value % m;
        return remainder <= m / 2 ? value - remainder : value + (m - remainder);
    }

    /**
     * Encodes a {@link BufferedImage} to a PNG base64 data URI string.
     *
     * @param image the image to encode
     * @return a base64 PNG string with data URI prefix
     * @throws IOException if encoding fails
     */
    private String encodeToBase64(BufferedImage image) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(imageBytes);
    }
}