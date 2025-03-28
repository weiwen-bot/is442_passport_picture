package com.passportphoto.service.strategy;

import org.opencv.core.Mat;

public interface ResizeStrategy {
    Mat resize(Mat image, int targetWidth, int targetHeight);
}