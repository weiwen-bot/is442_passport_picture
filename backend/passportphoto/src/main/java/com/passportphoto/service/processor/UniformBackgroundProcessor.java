package com.passportphoto.service.processor;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import com.passportphoto.util.CanvasUtil;

/* For a BGR image, we either:
    1. Detect if the background is uniform, and just fill the extension with that colour
    2. Use replicate + blur if background is not uniform
*/

public class UniformBackgroundProcessor implements BackgroundProcessor {
    
    private static final int BORDER_SIZE = 10;
    private static final double STD_DEV_THRESHOLD = 15.0;

    @Override
    public Mat process(Mat bgrImage, int targetWidth, int targetHeight) {
        if (bgrImage.width() == targetWidth && bgrImage.height() == targetHeight) {
            return bgrImage.clone();
        }

        // Compute stats from border region
        BorderStats stats = computeBorderStats(bgrImage, BORDER_SIZE, STD_DEV_THRESHOLD);

        if (stats.isUniform) {
            // Extend with the cornerColor
            return extendBackgroundUniform(bgrImage, targetWidth, targetHeight, stats.meanColor);
        } else {
            // Use replicate + blur
            return extendBackgroundReplicate(bgrImage, targetWidth, targetHeight);
        }
    }

    // Method to extend canvas with uniform colour
    private Mat extendBackgroundUniform(Mat image, int targetWidth, int targetHeight, Scalar bgColor) {
        // 1) If image already matches target, just return
        if (image.width() == targetWidth && image.height() == targetHeight) {
            return image.clone();
        }
        // 2) Create offsets for centering
        Point offset = CanvasUtil.calculateCenterOffset(targetWidth, targetHeight, image.width(), image.height());
        int xOffset = (int) offset.x;
        int yOffset = (int) offset.y;

        // 3) Create a new canvas
        Mat extendedMat = new Mat(targetHeight, targetWidth, image.type());
        // Fill with the uniform color
        extendedMat.setTo(bgColor);

        // 4) Copy the main image into the center
        Rect roi = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat centerRoi = extendedMat.submat(roi);
        image.copyTo(centerRoi);

        return extendedMat;
    }

    // Method to extend background - using OpenCV (replicate + blur)
    private Mat extendBackgroundReplicate(Mat image, int targetWidth, int targetHeight) {
    
        // 1. Create offsets for centering
        Point offset = CanvasUtil.calculateCenterOffset(targetWidth, targetHeight, image.width(), image.height());
        int xOffset = (int) offset.x;
        int yOffset = (int) offset.y;
    
        // 2. Create a larger canvas if needed
        Mat extendedMat = new Mat(targetHeight, targetWidth, image.type());
        // Fill with black
        extendedMat.setTo(new Scalar(0,0,0));
    
        // 3. Copy the main image into the center of extendedMat
        Rect roi = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat centerRoi = extendedMat.submat(roi);
        image.copyTo(centerRoi);
    
        // 4. Extract the center area to a temporary Mat, call copyMakeBorder
        Mat justCenter = new Mat();
        // Copy just the center part from extendedMat
        extendedMat.submat(roi).copyTo(justCenter);
    
        // 5. Compute border sizes
        int top = yOffset;
        int bottom = targetHeight - image.height() - yOffset;
        int left = xOffset;
        int right = targetWidth - image.width() - xOffset;
    
        // 6. copyMakeBorder with replicate
        Mat borderReplicated = new Mat();
        Core.copyMakeBorder(
            justCenter,
            borderReplicated,
            top, bottom, left, right,
            Core.BORDER_REPLICATE
        );
    
        // 7. Light blur only the newly created border region
        Rect innerROI = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat blurred = applySoftBlurToBorder(borderReplicated, innerROI);

        // Clean up
        justCenter.release();
        return blurred;
    }
    
    // Method to do a small blur outside the border region
    private Mat applySoftBlurToBorder(Mat extendedMat, Rect roi) {
    
        // 1. Duplicate the entire extendedMat
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(extendedMat, blurred, new Size(9, 9), 0); 
    
        // 2. Create a mask where the ROI is black (unblurred) and the rest is white (blurred)
        Mat mask = Mat.zeros(extendedMat.size(), CvType.CV_8UC1);
        mask.setTo(new Scalar(255));
        Imgproc.rectangle(mask, roi, new Scalar(0), -1);
    
        // 3. Copy from 'blurred' into 'extendedMat' only where mask=255
        blurred.copyTo(extendedMat, mask);
    
        blurred.release();
        mask.release();
        return extendedMat;
    }

    /* UNIFORM BACKGROUND DETECTION 
     * Gather pixels from a 10px-wide band around all edges, then compute mean/stddev
     * If the stddev is below a threshold, consider it uniform
    */
    private BorderStats computeBorderStats(Mat bgrImage, int borderSize, double stdDevThreshold) {
        BorderStats result = new BorderStats();

        // 1) Extract perimeter region (top/bottom/left/right)
        Mat borderRegion = extractBorderRegion(bgrImage, borderSize);
        if (borderRegion.empty()) {
            // fallback: either treat as uniform or return something default
            result.isUniform = true;
            result.meanColor = new Scalar(128,128,128); // middle gray
            return result;
        }
    
        // 2) Compute mean & stddev
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(borderRegion, mean, stddev);
    
        double stdB = stddev.get(0,0)[0];
        double stdG = stddev.get(1,0)[0];
        double stdR = stddev.get(2,0)[0];
    
        // 3) If all channels have std < threshold => uniform
        result.isUniform = (stdB < stdDevThreshold && stdG < stdDevThreshold && stdR < stdDevThreshold);
    
        double meanB = mean.get(0,0)[0];
        double meanG = mean.get(1,0)[0];
        double meanR = mean.get(2,0)[0];
        result.meanColor = new Scalar(meanB, meanG, meanR);
    
        return result;
    }
    
    private Mat extractBorderRegion(Mat bgrImage, int borderSize) {
        int w = bgrImage.cols();
        int h = bgrImage.rows();
        if (w < borderSize*2 || h < borderSize*2) {
            // If image is too small, no perimeter band to extract
            return new Mat(); // empty
        }

        // 1. Create submats for each edge
        Mat top    = bgrImage.submat(0, borderSize, 0, w); 
        Mat bottom = bgrImage.submat(h-borderSize, h, 0, w);
        Mat left   = bgrImage.submat(borderSize, h-borderSize, 0, borderSize);
        Mat right  = bgrImage.submat(borderSize, h-borderSize, w-borderSize, w);

        // 2. Calculate total number of border pixels
        int topPixels = top.rows() * top.cols();
        int bottomPixels = bottom.rows() * bottom.cols();
        int leftPixels = left.rows() * left.cols();
        int rightPixels = right.rows() * right.cols();
        int totalPixels = topPixels + bottomPixels + leftPixels + rightPixels;

        // 3. Create a single-column mat of the same type, which will hold all these pixels
        Mat borderRegion = new Mat(totalPixels, 1, bgrImage.type());

        // 4. Copy each submat's pixels into 'borderRegion'
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

    private void copySubmatToColumn(Mat region, Mat dest, int startIndex) {
        int rows = region.rows();
        int cols = region.cols();
    
        // Read each pixel in 'region' and store it in 'dest' at row [startIndex + ...], col=0
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

    static class BorderStats {
        boolean isUniform;
        Scalar meanColor;
    }
}
