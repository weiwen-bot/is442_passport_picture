/*
 * AutomatePassportPhotoService.java
 *
 * This service coordinates the pipeline for automating passport photo processing,
 * including resizing, alignment, and background removal.
 *
 */

package com.passportphoto.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import com.passportphoto.service.FaceCenteringService;
import com.passportphoto.util.ImageConverterUtil;
import com.passportphoto.util.ResizeUtil;
import com.passportphoto.util.ValidationUtil;

import org.opencv.core.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import ai.onnxruntime.*;

/**
 * The {@code AutomatePassportPhotoService} class handles the end-to-end
 * pipeline for processing passport photos, including resizing to ONNX-compatible
 * dimensions and background removal.
 */
@Service
public class AutomatePassportPhotoService {

    private final BackgroundRemovalService backgroundRemovalService;
    private final ImageResizingService imageResizingService;
    private final FaceCenteringService faceCenteringService;

    /**
     * Constructs the service with required dependencies.
     */
    public AutomatePassportPhotoService(BackgroundRemovalService backgroundRemovalService,ImageResizingService imageResizingService, FaceCenteringService faceCenteringService ){
        this.backgroundRemovalService = backgroundRemovalService;
        this.imageResizingService = imageResizingService;
        this.faceCenteringService = faceCenteringService;
        
    }
    /**
     * Performs batch processing for the full passport photo processing pipeline:
     * - Resize image
     * - Round dimensions to multiple of 32
     * - Perform background removal
     *
     * @param fileList     the list of all Files
     * @param country  country code for standard sizing
     * @param template optional template label
     * @return a processed image as base64 string
     * @throws IOException   if image processing fails
     * @throws OrtException  if ONNX model inference fails
     */
    public String[] batchProcessing(List<MultipartFile> fileList, String country, String template) throws Exception{
        String[] base64List = new String[fileList.size()];
        for (int i = 0; i < fileList.size(); i++){
            String processedImage = automatePassportPhoto(fileList.get(i), country, template);
            base64List[i] = processedImage;
        }
        return base64List;

    }

        

    /**
     * Orchestrates the full passport photo processing pipeline:
     * - Resize image
     * - Round dimensions to multiple of 32
     * - Perform background removal
     *
     * @param file     the uploaded image
     * @param country  country code for standard sizing
     * @param template optional template label
     * @return a processed image as base64 string
     * @throws IOException   if image processing fails
     * @throws OrtException  if ONNX model inference fails
     */
    public String automatePassportPhoto(MultipartFile file, String country, String template) throws Exception{
        String base64Image = imageResizingService.resizeImage(file, country, template, null, null);
        MultipartFile resizeFile = ImageConverterUtil.convertBase64ToMultipartFile(base64Image,"uploaded-img.jpg");
        System.out.println(resizeFile.getBytes().length);
        System.out.println("HEELLO");
        // System.out.println(ImageConverterUtil.convertMultiPartToBufferedImage(ImageConverterUtil.convertMultiPartToBufferedImage(resizeFile, file.getContentType().split("/")[1])));
        MultipartFile centeredImage = faceCenteringService.centerImage(resizeFile);
        System.out.println(centeredImage.getBytes().length);
        String processedBase64 = backgroundRemovalService.processImage(centeredImage, null, null);
        System.out.println(processedBase64);
        return processedBase64;
    }

    // /**
    //  * Converts a base64 image string to a {@link MultipartFile}.
    //  *
    //  * @param base64String the data URL of the image
    //  * @param fileName     the filename to assign
    //  * @return a MultipartFile wrapping the decoded image
    //  * @throws IOException if decoding fails
    //  */
    // public MultipartFile convertBase64ToMultipartFile(String base64String, String fileName) throws IOException {
    //     String[] parts = base64String.split(",");
    //     String mimeType = parts[0].split(":")[1].split(";")[0];
    //     String base64Data = parts[1];
    //     byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
    //     ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedBytes);
        
    //     return new MockMultipartFile(fileName, fileName, mimeType, byteArrayInputStream);
    // }

    
}