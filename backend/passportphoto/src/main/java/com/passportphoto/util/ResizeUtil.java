/*
 * ResizeUtil.java
 *
 * Utility class for calculating image dimensions while maintaining aspect ratio,
 * typically used in FIT-based resizing strategies for OpenCV.
 *
 */

package com.passportphoto.util;

import org.opencv.core.Size;

/**
 * The {@code ResizeUtil} class provides static helper methods
 * for resizing operations that preserve aspect ratio.
 * <p>
 * This class is non-instantiable.
 */
public final class ResizeUtil {
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ResizeUtil() {
        // Utility class - do not instantiate
    }

    /**
     * Calculates the new image size that fits within the target dimensions
     * while maintaining the original aspect ratio.
     *
     * @param originalSize the original image size
     * @param targetWidth  the target width constraint
     * @param targetHeight the target height constraint
     * @return a {@link Size} object representing the new dimensions
     */
    public static Size calculateFitSize(Size originalSize, int targetWidth, int targetHeight) {
        double originalAspect = (double) originalSize.width / originalSize.height;
        double targetAspect = (double) targetWidth / targetHeight;
    
        int newWidth, newHeight;
        if (originalAspect > targetAspect) {
            newWidth = targetWidth;
            newHeight = (int) Math.round(targetWidth / originalAspect);
        } else {
            newHeight = targetHeight;
            newWidth = (int) Math.round(targetHeight * originalAspect);
        }
        return new Size(newWidth, newHeight);
    }
}
