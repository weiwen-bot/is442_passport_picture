/*
 * ResizeUtil.java
 *
 * Utility class for calculating image dimensions while maintaining aspect ratio,
 * typically used in FIT-based resizing strategies for OpenCV.
 *
 */

package com.passportphoto.util;

import java.awt.image.BufferedImage;
import java.awt.RenderingHints;

import org.opencv.core.Size;
import java.awt.Graphics2D;

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
    
        int newWidth;
        int newHeight;
        if (originalAspect > targetAspect) {
            newWidth = targetWidth;
            newHeight = (int) Math.round(targetWidth / originalAspect);
        } else {
            newHeight = targetHeight;
            newWidth = (int) Math.round(targetHeight * originalAspect);
        }
        return new Size(newWidth, newHeight);
    }
     /**
     * Calculating the Nearest multiple of a value given a multiple
     *
     * @param value int value 
     * @param multiple  The Multiple at which the value will be converted nearest to
     * @return Nearest Multiple int
     */
    public static int roundToNearestMultiple(int value, int multiple) {
        return Math.round((float) value / multiple) * multiple;
    }



     /**
     * Resizing the Image to a given targetwidth and height
     *
     * @param img Buffered Image
     * @param targetWidth  Desired Width the Buffered Image
     * @param targetHeight  Desired Height the Buffered Image
     * @return Buffered Image that is resized height and width
     */

     public static BufferedImage resizeImage(BufferedImage img, int targetWidth, int targetHeight) {
        return resizeImage(img, targetWidth, targetHeight, 0, 0); // Calls the overloaded method with (0, 0) default
    }


    /**
     * Overloaded Resizing the Image to a given target width and height
     *
     * @param img Buffered Image
     * @param targetWidth  Desired Width the Buffered Image
     * @param targetHeight  Desired Height the Buffered Image
     * @param x Drawn Image starting x
     * @param y Drawn Image Starting y
     * @return Buffered Image that is resized height and width
     */
    public static BufferedImage resizeImage(BufferedImage img, int targetWidth, int targetHeight, int x, int y) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();

        // Set rendering hints for better image quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Draw the image starting at coordinates (x, y)
        g2d.drawImage(img, x, y, targetWidth, targetHeight, null);
        g2d.dispose();
        
        return resized;
    }


     /**
     * Resizing the Image to a defined Multiple
     *
     * @param img Buffered Image
     * @param multiple  The Multiple at which the img will resize to
     * @return Buffered Image that is resized to the multiple
     */
    public static BufferedImage resizeToNearestMultiple(BufferedImage img, int multiple) {
        int newWidth = roundToNearestMultiple(img.getWidth(), multiple);
        int newHeight = roundToNearestMultiple(img.getHeight(), multiple);

        BufferedImage outputImage = resizeImage(img, newWidth,newHeight);

        return outputImage;
    }

   /**
	 * Resizes the image while maintaining its aspect ratio, and centers it on a
	 * transparent canvas.
	 */
	public static BufferedImage resizeImageWithAspectRatio(BufferedImage image, int targetWidth, int targetHeight) {
		int originalWidth = image.getWidth();
		int originalHeight = image.getHeight();


		Size size = calculateFitSize(new Size(targetWidth, targetHeight), targetWidth, targetHeight);
		int newWidth = (int) size.width;
		int newHeight = (int) size.height;
		int x = (targetWidth - newWidth) / 2;
		int y = (targetHeight - newHeight) / 2;
		BufferedImage resizedImage = resizeImage(image, targetWidth, targetHeight, x, y);

		return resizedImage;
	}


    


}
