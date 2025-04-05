/*
 * AlphaResizeStrategy.java
 *
 * This class implements the ResizeStrategy interface to resize RGBA images
 * using OpenCV. It dynamically selects the best interpolation method based
 * on scaling direction while preserving the alpha channel.
 *
 */

package com.passportphoto.service.strategy;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static com.passportphoto.util.ResizeUtil.calculateFitSize;

/**
 * The {@code AlphaResizeStrategy} class resizes images with an alpha channel (RGBA)
 * to the specified dimensions while maintaining the aspect ratio.
 * It uses OpenCV's interpolation methods depending on the scaling direction.
 */
public class AlphaResizeStrategy implements ResizeStrategy {

    /**
     * Resizes the given RGBA image to the specified target width and height.
     * Returns the original image if it's already the correct size.
     * Uses INTER_AREA for downscaling and INTER_CUBIC for upscaling.
     *
     * @param rgbaImage    the RGBA input image as an OpenCV {@link Mat}
     * @param targetWidth  the desired width
     * @param targetHeight the desired height
     * @return the resized {@link Mat} image
     */
    @Override
    public Mat resize(Mat rgbaImage, int targetWidth, int targetHeight) {
        int w = rgbaImage.width();
        int h = rgbaImage.height();

        if (w == targetWidth && h == targetHeight) {
            return rgbaImage;
        }

        Size newSize = calculateFitSize(rgbaImage.size(), targetWidth, targetHeight);
        Mat resized = new Mat();

        // Choose interpolation based on scaling direction
        int interpolation = (newSize.width < w || newSize.height < h) ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;

        Imgproc.resize(rgbaImage, resized, newSize, 0, 0, interpolation);
        return resized;
    }
}
