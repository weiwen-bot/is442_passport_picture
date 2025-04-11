/*
 * FaceCenteringService.java
 *
 * This service does the Image Centering and Face Detection
 *
 */
package com.passportphoto.service;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;

import com.passportphoto.exceptions.InvalidEyeException;
import com.passportphoto.exceptions.InvalidFaceException;
import com.passportphoto.service.FaceModelLoader;
import com.passportphoto.util.ImageConverterUtil;
import com.passportphoto.util.ValidationUtil;
import com.passportphoto.util.ResizeUtil;

import com.passportphoto.exceptions.*;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * The {@code FaceCenteringService} class handles Face and Eye
 * Detection of the Image and proceeds to center it via Opencv functions
 */
@Service
public class FaceCenteringService {

    private final CascadeClassifier faceDetector;
    private final CascadeClassifier eyeDetector;

    /**
     * Constructs the service with required dependencies.
     */
    public FaceCenteringService(@Qualifier("faceModel") FaceModelLoader faceModel,
            @Qualifier("eyeModel") FaceModelLoader eyeModel) {
        this.faceDetector = faceModel.getFaceModel();
        this.eyeDetector = eyeModel.getFaceModel();

    }
    /**
     * Performs Centering of the Image by Face and Eyes if detected
     *
     *
     * @param file Takes in a MultipartFile Image
     * @return a processed image as base64 string
     * @throws Exception  if face or eye detection fails
     */
    public MultipartFile centerImage(MultipartFile file) throws Exception {

        Mat image = ImageConverterUtil.convertFileToMat(file);

        // Detect faces
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(image, faces);

        ValidationUtil.validateFace(faces);

        Rect faceRect = faces.toArray()[0];

        // Detect eyes within face
        Mat faceROI = new Mat(image, faceRect);
        MatOfRect eyes = new MatOfRect();
        eyeDetector.detectMultiScale(faceROI, eyes);

        ValidationUtil.validateEye(eyes);

        // Calculate eye centers
        Rect[] eyeRects = eyes.toArray();
        Point[] eyeCenters = new Point[2];

        for (int i = 0; i < 2; i++) {
            Rect r = eyeRects[i];
            eyeCenters[i] = new Point(
                    faceRect.x + r.x + r.width / 2.0,
                    faceRect.y + r.y + r.height / 2.0);

        }

        // Sort left to right
        if (eyeCenters[0].x > eyeCenters[1].x) {
            Point temp = eyeCenters[0];
            eyeCenters[0] = eyeCenters[1];
            eyeCenters[1] = temp;
        }

        // Midpoint between eyes
        Point midpoint = new Point(
                (eyeCenters[0].x + eyeCenters[1].x) / 2.0,
                (eyeCenters[0].y + eyeCenters[1].y) / 2.0);

        // Get center of the image
        Point imageCenter = new Point(image.cols() / 2.0, image.rows() / 2.0);

        // Compute translation needed to center the face
        double dx = imageCenter.x - midpoint.x;
        double dy = imageCenter.y - midpoint.y;

        // Create translation matrix
        Mat translationMatrix = Mat.eye(2, 3, CvType.CV_64F);
        translationMatrix.put(0, 2, dx); // x translation
        translationMatrix.put(1, 2, dy); // y translation

        // Translate image to center face
        Mat translatedImage = new Mat();
        Imgproc.warpAffine(image, translatedImage, translationMatrix, image.size(), Imgproc.INTER_LINEAR,
                Core.BORDER_CONSTANT, new Scalar(255, 255, 255));

        MultipartFile multipartFile = ImageConverterUtil.convertMatToMultipartFile(translatedImage);

        return multipartFile;
    }

}
