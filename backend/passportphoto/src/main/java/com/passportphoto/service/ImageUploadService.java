/*
 * ImageUploadService.java
 *
 * This service handles the upload and decoding of images from
 * multipart file uploads using OpenCV and Base64 encoding.
 *
 */

package com.passportphoto.service;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

/**
 * The {@code ImageUploadService} handles image uploads by converting
 * multipart files into OpenCV {@link Mat} objects and encoding them
 * into base64 image strings.
 */
@Service
public class ImageUploadService {

    /**
     * Uploads and encodes an image file into a Base64 JPEG data URL.
     *
     * @param file the uploaded image file
     * @return a base64-encoded image string with data URI prefix
     * @throws IOException if image decoding fails
     */
    public String uploadImage(MultipartFile file) throws IOException {
        Mat image = Imgcodecs.imdecode(new MatOfByte(file.getBytes()), Imgcodecs.IMREAD_COLOR);

        if (image.empty()) {
            throw new IOException("Failed to load image!");
        }

        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, matOfByte);
        byte[] imageBytes = matOfByte.toArray();

        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }
}