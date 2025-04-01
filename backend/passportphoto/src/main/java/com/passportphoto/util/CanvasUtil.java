/*
 * CanvasUtil.java
 *
 * Utility class for image canvas operations, such as centering an image
 * within a larger canvas. Used across background processors.
 *
 */

package com.passportphoto.util;

import org.opencv.core.Point;

/**
 * The {@code CanvasUtil} class provides static utility methods for working
 * with image canvas positioning in OpenCV, such as calculating center offsets.
 * <p>
 * This class is non-instantiable.
 */
public final class CanvasUtil {
    
    /**
     * Private constructor to prevent instantiation.
     */
    private CanvasUtil() {
        // Utility class - do not instantiate
    }

    /**
     * Calculates the (x, y) offset needed to center an image inside a canvas.
     * 
     * @param canvasWidth  The width of the larger canvas
     * @param canvasHeight The height of the larger canvas
     * @param imageWidth   The width of the image to be centered
     * @param imageHeight  The height of the image to be centered
     * @return a {@link Point} containing the x and y offsets
     */
    public static Point calculateCenterOffset(int canvasWidth, int canvasHeight, int imageWidth, int imageHeight) {
        int xOffset = (int) Math.round((canvasWidth - imageWidth) / 2.0);
        int yOffset = (int) Math.round((canvasHeight - imageHeight) / 2.0);
        return new Point(xOffset, yOffset);
    }
}
