/*
 * ImageCroppingService.java
 *
 * This service handles image conversion, cropping, and Base64 encoding
 * for use in the Passport Picture Project.
 *
 */

package com.passportphoto.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * The {@code ImageCroppingService} provides methods for converting uploaded images
 * to {@link BufferedImage}, cropping them, and converting the result to Base64.
 */
@Service
public class ImageCroppingService {

    /**
     * Converts a multipart image file into a {@link BufferedImage}.
     *
     * @param imageFile the uploaded multipart file
     * @return the image as a BufferedImage
     * @throws IOException if reading the input stream fails
     */
    public BufferedImage convertToBufferedImage(MultipartFile imageFile) throws IOException {
        return ImageIO.read(imageFile.getInputStream());
    }

    /**
     * Crops the input image to the specified rectangle.
     *
     * @param inputImage the original image
     * @param x          x-coordinate of the top-left crop corner
     * @param y          y-coordinate of the top-left crop corner
     * @param width      width of the cropped area
     * @param height     height of the cropped area
     * @return the cropped image as a {@link BufferedImage}
     */
    public BufferedImage cropImage(BufferedImage inputImage, int x, int y, int width, int height) {
        BufferedImage croppedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        croppedImage.getGraphics().drawImage(inputImage, 0, 0, width, height, x, y, x + width, y + height, null);
        return croppedImage;
    }

    /**
     * Converts a {@link BufferedImage} to a Base64-encoded JPEG string.
     *
     * @param image the image to encode
     * @return a Base64-encoded string representing the image
     * @throws IOException if encoding fails
     */
    public String convertToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}