/*
 * UniformBackgroundProcessor.java
 *
 * This class implements the BackgroundProcessor interface to extend a BGR image
 * to the target dimensions. It detects uniform background color and fills the
 * padding with that color if possible; otherwise, it uses border replication
 * and selective blur to smooth out transitions.
 *
 */

package com.passportphoto.service.processor;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import com.passportphoto.util.CanvasUtil;

/**
 * The {@code UniformBackgroundProcessor} class extends a BGR image to fit a target size.
 * It detects uniform backgrounds and fills the canvas accordingly, or uses border
 * replication and blur if no uniform color is found.
 */
public class UniformBackgroundProcessor implements BackgroundProcessor {
    
    private static final int BORDER_SIZE = 10;
    private static final double STD_DEV_THRESHOLD = 15.0;

    /**
     * Processes the given image by either extending it with a detected uniform background color
     * or applying replication and soft blur around the image if no uniform background is found.
     *
     * @param bgrImage      the original image
     * @param targetWidth   the desired width
     * @param targetHeight  the desired height
     * @return a new {@link Mat} with the extended background
     */
    @Override
    public Mat process(Mat bgrImage, int targetWidth, int targetHeight) {
        if (bgrImage.width() == targetWidth && bgrImage.height() == targetHeight) {
            return bgrImage.clone();
        }

        BorderStats stats = computeBorderStats(bgrImage, BORDER_SIZE, STD_DEV_THRESHOLD);

        if (stats.isUniform) {
            return extendBackgroundUniform(bgrImage, targetWidth, targetHeight, stats.meanColor);
        } else {
            return extendBackgroundReplicate(bgrImage, targetWidth, targetHeight);
        }
    }

    /**
     * Extends the image by centering it on a uniform color background.
     */
    private Mat extendBackgroundUniform(Mat image, int targetWidth, int targetHeight, Scalar bgColor) {
        if (image.width() == targetWidth && image.height() == targetHeight) {
            return image.clone();
        }

        Point offset = CanvasUtil.calculateCenterOffset(targetWidth, targetHeight, image.width(), image.height());
        int xOffset = (int) offset.x;
        int yOffset = (int) offset.y;

        Mat extendedMat = new Mat(targetHeight, targetWidth, image.type());
        extendedMat.setTo(bgColor);

        Rect roi = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat centerRoi = extendedMat.submat(roi);
        image.copyTo(centerRoi);

        return extendedMat;
    }

    /**
     * Extends the image by replicating the border and applying selective blur.
     */
    private Mat extendBackgroundReplicate(Mat image, int targetWidth, int targetHeight) {
        Point offset = CanvasUtil.calculateCenterOffset(targetWidth, targetHeight, image.width(), image.height());
        int xOffset = (int) offset.x;
        int yOffset = (int) offset.y;
    
        Mat extendedMat = new Mat(targetHeight, targetWidth, image.type());
        extendedMat.setTo(new Scalar(0,0,0));
    
        Rect roi = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat centerRoi = extendedMat.submat(roi);
        image.copyTo(centerRoi);
    
        Mat justCenter = new Mat();
        extendedMat.submat(roi).copyTo(justCenter);
    
        int top = yOffset;
        int bottom = targetHeight - image.height() - yOffset;
        int left = xOffset;
        int right = targetWidth - image.width() - xOffset;
    
        Mat borderReplicated = new Mat();
        Core.copyMakeBorder(justCenter, borderReplicated, top, bottom, left, right, Core.BORDER_REPLICATE);
    
        Rect innerROI = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat blurred = applySoftBlurToBorder(borderReplicated, innerROI);

        justCenter.release();
        return blurred;
    }
    
    /**
     * Applies a soft blur to the border region of the extended image.
     */
    private Mat applySoftBlurToBorder(Mat extendedMat, Rect roi) {
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(extendedMat, blurred, new Size(9, 9), 0); 
    
        Mat mask = Mat.zeros(extendedMat.size(), CvType.CV_8UC1);
        mask.setTo(new Scalar(255));
        Imgproc.rectangle(mask, roi, new Scalar(0), -1);
    
        blurred.copyTo(extendedMat, mask);
    
        blurred.release();
        mask.release();
        return extendedMat;
    }
    
    /**
     * Detects whether the image has a uniform background based on pixel statistics
     * from the image's perimeter band.
     */
    private BorderStats computeBorderStats(Mat bgrImage, int borderSize, double stdDevThreshold) {
        BorderStats result = new BorderStats();
        Mat borderRegion = extractBorderRegion(bgrImage, borderSize);

        if (borderRegion.empty()) {
            result.isUniform = true;
            result.meanColor = new Scalar(128,128,128); // fallback gray
            return result;
        }

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(borderRegion, mean, stddev);
    
        double stdB = stddev.get(0,0)[0];
        double stdG = stddev.get(1,0)[0];
        double stdR = stddev.get(2,0)[0];
    
        result.isUniform = (stdB < stdDevThreshold && stdG < stdDevThreshold && stdR < stdDevThreshold);
    
        double meanB = mean.get(0,0)[0];
        double meanG = mean.get(1,0)[0];
        double meanR = mean.get(2,0)[0];
        result.meanColor = new Scalar(meanB, meanG, meanR);
    
        return result;
    }
    
    /**
     * Extracts a single-column Mat representing the border pixels from all four edges.
     */
    private Mat extractBorderRegion(Mat bgrImage, int borderSize) {
        int w = bgrImage.cols();
        int h = bgrImage.rows();
        if (w < borderSize*2 || h < borderSize*2) {
            return new Mat(); // if image is too small
        }

        Mat top    = bgrImage.submat(0, borderSize, 0, w); 
        Mat bottom = bgrImage.submat(h-borderSize, h, 0, w);
        Mat left   = bgrImage.submat(borderSize, h-borderSize, 0, borderSize);
        Mat right  = bgrImage.submat(borderSize, h-borderSize, w-borderSize, w);

        int topPixels = top.rows() * top.cols();
        int bottomPixels = bottom.rows() * bottom.cols();
        int leftPixels = left.rows() * left.cols();
        int rightPixels = right.rows() * right.cols();
        int totalPixels = topPixels + bottomPixels + leftPixels + rightPixels;

        Mat borderRegion = new Mat(totalPixels, 1, bgrImage.type());

        int currentIndex = 0;
        copySubmatToColumn(top, borderRegion, currentIndex);
        currentIndex += topPixels;
        copySubmatToColumn(bottom, borderRegion, currentIndex);
        currentIndex += bottomPixels;
        copySubmatToColumn(left, borderRegion, currentIndex);
        currentIndex += leftPixels;
        copySubmatToColumn(right, borderRegion, currentIndex);

        return borderRegion;
    }

    /**
     * Copies pixels from a submatrix into a column Mat at the given start index.
     */
    private void copySubmatToColumn(Mat region, Mat dest, int startIndex) {
        int rows = region.rows();
        int cols = region.cols();
        byte[] pixel = new byte[(int) region.elemSize()]; 
    
        int index = startIndex; 
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                region.get(y, x, pixel);
                dest.put(index, 0, pixel);
                index++;
            }
        }
    }

    /**
     * Simple structure to hold background statistics.
     */
    static class BorderStats {
        boolean isUniform;
        Scalar meanColor;
    }
}
