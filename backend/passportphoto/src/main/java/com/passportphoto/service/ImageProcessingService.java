/*
 * ImageProcessingService.java
 *
 * This service provides utility methods for decoding base64 images
 * and resizing images with preserved aspect ratio using Java AWT.
 *
 */

package com.passportphoto.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

/**
 * The {@code ImageProcessingService} provides methods for decoding
 * base64 image strings and resizing images while maintaining aspect ratio.
 */
@Service
public class ImageProcessingService {

    /**
     * Decodes a base64-encoded image string into a {@link BufferedImage}.
     * Expects the string to follow the "data:image/...;base64,..." format.
     *
     * @param base64String the base64 image string
     * @return the decoded image as a BufferedImage, or {@code null} on error
     */
    private BufferedImage decodeBase64ToImage(String base64String) {
        try {
            // Usually the format is "data:image/png;base64,iVBORw0KGgo..."
            String[] parts = base64String.split(",");
            if (parts.length != 2) {
                return null;
            }
            byte[] imageBytes = Base64.getDecoder().decode(parts[1]);
            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Resizes a given image to fit within the specified target width and height
     * while preserving its aspect ratio. The resized image is centered within the target canvas.
     *
     * @param image        the original BufferedImage
     * @param targetWidth  the target width of the canvas
     * @param targetHeight the target height of the canvas
     * @return a resized and centered image of size targetWidth x targetHeight
     */
    private BufferedImage resizeImageWithAspectRatio(BufferedImage image, int targetWidth, int targetHeight) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
    
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth = targetWidth;
        int newHeight = (int) (targetWidth / aspectRatio);
    
        if (targetHeight > targetWidth) {
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * aspectRatio);
        } 

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
    
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    
        int x = (targetWidth - newWidth) / 2;
        int y = (targetHeight - newHeight) / 2;

        g2d.drawImage(image, x, y, newWidth, newHeight, null);
        g2d.dispose();
    
        return resizedImage;
    }
}
