package com.passportphoto.service.strategy;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import static com.passportphoto.util.ResizeUtil.calculateFitSize;

public class AlphaResizeStrategy implements ResizeStrategy {

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
        // INTER_AREA --> downscale
        // INTER_CUBIC --> upscale
        int interpolation = (newSize.width < w || newSize.height < h) ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;

        Imgproc.resize(rgbaImage, resized, newSize, 0, 0, interpolation);
        return resized;
    }
}
