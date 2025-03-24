package com.passportphoto.controller;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/face")
@CrossOrigin(origins = "http://localhost:5173")
public class FaceController {

    private CascadeClassifier faceDetector;
    private CascadeClassifier eyeDetector;

    @PostConstruct
    public void init() throws IOException {
        System.load(System.getenv("OPENCVDLLPATH"));

        // Load face cascade
        ClassPathResource faceCascade = new ClassPathResource("haarcascade_frontalface_default.xml");
        File faceFile = File.createTempFile("face", ".xml");
        try (InputStream is = faceCascade.getInputStream()) {
            Files.copy(is, faceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        faceDetector = new CascadeClassifier(faceFile.getAbsolutePath());

        // Load eye cascade
        ClassPathResource eyeCascade = new ClassPathResource("haarcascade_eye.xml");
        File eyeFile = File.createTempFile("eye", ".xml");
        try (InputStream is = eyeCascade.getInputStream()) {
            Files.copy(is, eyeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        eyeDetector = new CascadeClassifier(eyeFile.getAbsolutePath());
    }

    @PostMapping(value = "/center", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> process(@RequestParam("image") MultipartFile file) throws IOException {
        byte[] imageBytes = file.getBytes();
        Mat image = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);

        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(image, faces);

        if (faces.toArray().length != 1) {
            return ResponseEntity.badRequest().body("Exactly one face must be present.".getBytes());
        }

        Rect faceRect = faces.toArray()[0];
        Imgproc.rectangle(image, faceRect.tl(), faceRect.br(), new Scalar(0, 255, 0), 2); // draw face box

        // Detect eyes within face
        Mat faceROI = new Mat(image, faceRect);
        MatOfRect eyes = new MatOfRect();
        eyeDetector.detectMultiScale(faceROI, eyes);

        Rect[] eyeRects = eyes.toArray();
        if (eyeRects.length != 2) {
            return ResponseEntity.badRequest().body("Exactly two eyes must be visible.".getBytes());
        }

        // Compute eye centers
        Point[] eyeCenters = new Point[2];
        for (int i = 0; i < 2; i++) {
            Rect r = eyeRects[i];
            eyeCenters[i] = new Point(
                faceRect.x + r.x + r.width / 2,
                faceRect.y + r.y + r.height / 2
            );
            Imgproc.circle(image, eyeCenters[i], 5, new Scalar(255, 0, 0), -1); // draw blue eye point
        }

        // Sort eye positions (left to right)
        if (eyeCenters[0].x > eyeCenters[1].x) {
            Point temp = eyeCenters[0];
            eyeCenters[0] = eyeCenters[1];
            eyeCenters[1] = temp;
        }

        // Midpoint between two eyes
        Point midpoint = new Point(
            (eyeCenters[0].x + eyeCenters[1].x) / 2,
            (eyeCenters[0].y + eyeCenters[1].y) / 2
        );
        Imgproc.circle(image, midpoint, 5, new Scalar(0, 0, 255), -1); // red midpoint

        // Crop to center the midpoint
        int imageCenterX = image.cols() / 2;
        int eyeOffset = (int) (midpoint.x - imageCenterX);

        // If eyes are off-center, crop the opposite side to center
        int cropX = Math.max(0, eyeOffset); // crop left if eyeOffset > 0
        int cropWidth = image.cols() - Math.abs(eyeOffset);

        if (cropWidth <= 0 || cropX + cropWidth > image.cols()) {
            return ResponseEntity.badRequest().body("Failed to center image — invalid crop.".getBytes());
        }

        Rect cropRect = new Rect(cropX, 0, cropWidth, image.rows());
        Mat cropped = new Mat(image, cropRect);

        // Encode and return
        MatOfByte output = new MatOfByte();
        Imgcodecs.imencode(".jpg", cropped, output);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(output.toArray());
    }
}
