package com.passportphoto.util;

import org.opencv.core.Size;

public final class ResizeUtil {
    
    // Prevent instantiation
    private ResizeUtil() {

    }

    // Method to maintain aspect ratio with FIT strategy
    public static Size calculateFitSize(Size originalSize, int targetWidth, int targetHeight) {
        double originalAspect = (double) originalSize.width / originalSize.height;
        double targetAspect = (double) targetWidth / targetHeight;
    
        int newWidth, newHeight;
        if (originalAspect > targetAspect) {
            // Image is relatively wider -> match target's width
            newWidth = targetWidth;
            newHeight = (int) Math.round(targetWidth / originalAspect);
        } else {
            // Image is relatively taller (or equal) -> match target's height
            newHeight = targetHeight;
            newWidth = (int) Math.round(targetHeight * originalAspect);
        }
        return new Size(newWidth, newHeight);
    }
}
