package com.passportphoto.util;

import org.opencv.core.Point;

public final class CanvasUtil {
    
    private CanvasUtil() {
        // Prevent instantiation
    }

    /**
     * Calculates the (x, y) offset needed to center an image inside a canvas.
     * @param canvasWidth  The width of the larger canvas
     * @param canvasHeight The height of the larger canvas
     * @param imageWidth   The width of the image to be centered
     * @param imageHeight  The height of the image to be centered
     * @return A Point object containing the x and y offsets
     */
    public static Point calculateCenterOffset(int canvasWidth, int canvasHeight, int imageWidth, int imageHeight) {
        int xOffset = (int) Math.round((canvasWidth - imageWidth) / 2.0);
        int yOffset = (int) Math.round((canvasHeight - imageHeight) / 2.0);
        return new Point(xOffset, yOffset);
    }
}
