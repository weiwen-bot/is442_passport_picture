/*
 * ImageCroppingController.java
 *
 * This controller handles the image cropping endpoint for the Passport Picture Project.
 *
 */

package com.passportphoto.controller;

import com.passportphoto.dto.ImageCropRequest;
import com.passportphoto.dto.ImageCropResponse;
import com.passportphoto.service.ImageCroppingService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code ImageCroppingController} provides an endpoint to crop
 * a user-uploaded image based on the dimensions provided in the request.
 */
@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173")
public class ImageCroppingController {

    /** Service that handles image conversion and cropping logic */
    private final ImageCroppingService imageCroppingService;

    /**
     * Constructs the controller with the given image processing service.
     *
     * @param imageCroppingService the service used for cropping and encoding images
     */
    public ImageCroppingController(ImageCroppingService imageCroppingService) {
        this.imageCroppingService = imageCroppingService;
    }

    /**
     * Crops the uploaded image based on provided crop parameters.
     *
     * @param imageFile   the uploaded image
     * @param cropRequest the crop dimensions
     * @return a base64-encoded cropped image
     */
    @PostMapping("/crop")
    public ResponseEntity<ImageCropResponse> cropImage(@RequestParam("image") MultipartFile imageFile,
            @ModelAttribute ImageCropRequest cropRequest) throws Exception {

        String base64ImageStr = imageCroppingService.cropFileToImage(imageFile, cropRequest);
        return ResponseEntity.ok(new ImageCropResponse("success", "Image cropped successfully", base64ImageStr));
    }
}