/*
 * TransparentBackgroundProcessor.java
 *
 * This class implements the BackgroundProcessor interface to extend an RGBA image
 * by centering it on a transparent canvas of the specified size.
 *
 */

package com.passportphoto.service.processor;

import org.opencv.core.*;
import com.passportphoto.util.CanvasUtil;

/**
 * The {@code TransparentBackgroundProcessor} class centers an RGBA image on a transparent
 * canvas of the specified dimensions, preserving alpha transparency.
 */
public class TransparentBackgroundProcessor implements BackgroundProcessor {
    
    /**
     * Centers the RGBA image on a transparent canvas of target dimensions.
     * If the original image already matches the target size, a clone is returned.
     *
     * @param rgbaImage     the input RGBA image
     * @param targetWidth   the desired canvas width
     * @param targetHeight  the desired canvas height
     * @return the new image centered on a transparent canvas
     */
    @Override
    public Mat process(Mat rgbaImage, int targetWidth, int targetHeight) {
        // 1) If already the same size, return image
        if (rgbaImage.width() == targetWidth && rgbaImage.height() == targetHeight) {
            return rgbaImage.clone();
        }

        // 2) Compute offsets to center the image
        Point offset = CanvasUtil.calculateCenterOffset(targetWidth, targetHeight, rgbaImage.width(), rgbaImage.height());
        int xOffset = (int) offset.x;
        int yOffset = (int) offset.y;

        // 3) Create new transparent canvas
        Mat extended = new Mat(targetHeight, targetWidth, CvType.CV_8UC4, new Scalar(0,0,0,0));

        // 4) Define region of interest and copy the resized image to center
        Rect roi = new Rect(xOffset, yOffset, rgbaImage.width(), rgbaImage.height());
        Mat center = extended.submat(roi);
        rgbaImage.copyTo(center);

        return extended;
    }
}
