package com.passportphoto.service;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Service
public class ImageUploadService {

    public String uploadImage(MultipartFile file) throws IOException {
        // Convert MultipartFile to OpenCV Mat
        Mat image = Imgcodecs.imdecode(new MatOfByte(file.getBytes()), Imgcodecs.IMREAD_COLOR);

        // Ensure image is properly loaded
        if (image.empty()) {
            throw new IOException("Failed to load image!");
        }

        // Convert Mat to byte array
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, matOfByte);
        byte[] imageBytes = matOfByte.toArray();

        // Convert to Base64
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }
}
