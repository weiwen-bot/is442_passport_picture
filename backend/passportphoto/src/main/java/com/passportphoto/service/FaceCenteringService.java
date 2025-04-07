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
import org.springframework.web.multipart.MultipartFile;

import com.passportphoto.exceptions.InvalidEyeException;
import com.passportphoto.exceptions.InvalidFaceException;
import com.passportphoto.service.FaceModelLoader;

import com.passportphoto.exceptions.*;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Service
public class FaceCenteringService {

    private final CascadeClassifier faceDetector;
    private final CascadeClassifier eyeDetector;

    public FaceCenteringService(@Qualifier("faceModel") FaceModelLoader faceModel,
            @Qualifier("eyeModel") FaceModelLoader eyeModel) {
        this.faceDetector = faceModel.getFaceModel();
        this.eyeDetector = eyeModel.getFaceModel();

    }

    public MultipartFile centerImage(MultipartFile file) throws IOException{
        byte[] imageBytes = file.getBytes();
        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);

        // Detect faces
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(image, faces);

        if (faces.empty()) {
            throw new InvalidFaceException("No Face Found");
        }

        Rect faceRect = faces.toArray()[0];
        // Imgproc.rectangle(image, faceRect.tl(), faceRect.br(), new Scalar(0, 255, 0),
        // 2); // green face box

        // Detect eyes within face
        Mat faceROI = new Mat(image, faceRect);
        MatOfRect eyes = new MatOfRect();
        eyeDetector.detectMultiScale(faceROI, eyes);

        if (eyes.toArray().length < 2) {
            throw new InvalidEyeException("No eyes Found");
        }

        // Calculate eye centers
        Rect[] eyeRects = eyes.toArray();
        Point[] eyeCenters = new Point[2];

        for (int i = 0; i < 2; i++) {
            Rect r = eyeRects[i];
            eyeCenters[i] = new Point(
                    faceRect.x + r.x + r.width / 2.0,
                    faceRect.y + r.y + r.height / 2.0);
            // Imgproc.circle(image, eyeCenters[i], 5, new Scalar(255, 0, 0), -1); // blue
            // dot
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
        // Imgproc.circle(image, midpoint, 5, new Scalar(0, 0, 255), -1); // red
        // midpoint

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

        // Encode and return the translated image
        MatOfByte output = new MatOfByte();
        Imgcodecs.imencode(".jpg", translatedImage, output);
        byte[] postimageBytes = output.toArray();

        // Create MultipartFile
        MultipartFile multipartFile = new MockMultipartFile(
                "file", // name
                "processed.jpg", // original filename
                "image/jpeg", // content type
                postimageBytes // file content
        );

        return multipartFile;
    }

}
