package com.passportphoto.service.processor;

import org.opencv.core.*;
import com.passportphoto.util.CanvasUtil;

public class TransparentBackgroundProcessor implements BackgroundProcessor {
    
    @Override
    public Mat process(Mat rgbaImage, int targetWidth, int targetHeight) {
        // 1) If already the same size, return image
        if (rgbaImage.width() == targetWidth && rgbaImage.height() == targetHeight) {
            return rgbaImage.clone();
        }

        // 2) Compute offsets
        Point offset = CanvasUtil.calculateCenterOffset(targetWidth, targetHeight, rgbaImage.width(), rgbaImage.height());
        int xOffset = (int) offset.x;
        int yOffset = (int) offset.y;

        // 3) Create new transparent canvas
        Mat extended = new Mat(targetHeight, targetWidth, CvType.CV_8UC4, new Scalar(0,0,0,0));

        // 4) Copy the resized image into the center
        Rect roi = new Rect(xOffset, yOffset, rgbaImage.width(), rgbaImage.height());
        Mat center = extended.submat(roi);
        rgbaImage.copyTo(center);

        return extended;
    }
}
