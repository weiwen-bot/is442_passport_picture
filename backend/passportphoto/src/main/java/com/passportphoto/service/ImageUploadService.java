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

import com.passportphoto.util.ImageConverterUtil;
import com.passportphoto.util.ValidationUtil;

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
     * @param imageFile the uploaded image file
     * @return a base64-encoded image string with data URI prefix
     * @throws IOException              if image decoding fails
     * @throws IllegalArgumentException if file is null or empty
     */
    public String uploadImage(MultipartFile imageFile) throws Exception {

        ValidationUtil.validateMultipartFile(imageFile);

        Mat image = ImageConverterUtil.convertFileToMat(imageFile);

        ValidationUtil.validateMatImage(image);

        String base64String = ImageConverterUtil.convertMatToDataUrl(image);

        return base64String;
    }
}