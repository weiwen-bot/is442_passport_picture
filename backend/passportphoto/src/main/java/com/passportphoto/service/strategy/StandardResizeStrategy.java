/*
 * StandardResizeStrategy.java
 *
 * This class implements the ResizeStrategy interface to perform standard
 * BGR image resizing using OpenCV. It dynamically chooses interpolation
 * based on whether the image is being upscaled or downscaled.
 *
 */

package com.passportphoto.service.strategy;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import static com.passportphoto.util.ResizeUtil.calculateFitSize;

/**
 * The {@code StandardResizeStrategy} class resizes images to the specified
 * width and height while maintaining aspect ratio. It chooses the best
 * OpenCV interpolation method depending on whether the image is scaled up or down.
 */
public class StandardResizeStrategy implements ResizeStrategy {

    /**
     * Resizes the given image to the target dimensions using OpenCV.
     * If the image is already the correct size, it returns the original.
     * Uses INTER_AREA for downscaling and INTER_CUBIC for upscaling.
     *
     * @param image        the input BGR image as an OpenCV {@link Mat}
     * @param targetWidth  the desired width of the image
     * @param targetHeight the desired height of the image
     * @return the resized {@link Mat} image
     */
    @Override
    public Mat resize(Mat image, int targetWidth, int targetHeight) {
        int w = image.width();
        int h = image.height();

        if (w == targetWidth && h == targetHeight) {
            return image;
        }

        Size newSize = calculateFitSize(image.size(), targetWidth, targetHeight);
        Mat resized = new Mat();

        // Choose interpolation based on scaling direction
        int interpolation = (newSize.width < w || newSize.height < h) ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;

        Imgproc.resize(image, resized, newSize, 0, 0, interpolation);
        return resized;
    }
}
