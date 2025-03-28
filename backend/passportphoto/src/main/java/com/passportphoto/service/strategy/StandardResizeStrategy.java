package com.passportphoto.service.strategy;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import static com.passportphoto.util.ResizeUtil.calculateFitSize;

public class StandardResizeStrategy implements ResizeStrategy {

    @Override
    // Method to resise BGR image
    public Mat resize(Mat image, int targetWidth, int targetHeight) {
        int w = image.width();
        int h = image.height();

        if (w == targetWidth && h == targetHeight) {
            return image;
        }

        Size newSize = calculateFitSize(image.size(), targetWidth, targetHeight);
        Mat resized = new Mat();

        // Choose interpolation based on scaling direction
        // INTER_AREA --> downscale
        // INTER_CUBIC --> upscale
        int interpolation = (newSize.width < w || newSize.height < h) ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;

        Imgproc.resize(image, resized, newSize, 0, 0, interpolation);
        return resized;
    }

    
}
