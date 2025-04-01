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

    // ---- TUNABLE CONSTANTS ----

    // 1) Horizontal bounding box ratio
    //    e.g. 3.0 => final crop width = faceRect.width * 3
    private static final double FACE_CROP_RATIO = 2.1;

    // Constants (tweak as needed)
    private static final double VERTICAL_TOP_EXTRA = 0.4;
    private static final double VERTICAL_BOTTOM_EXTRA = 0.7;

    @PostConstruct
    public void init() throws IOException {
        System.load(System.getenv("OPENCVDLLPATH"));

        // 1) Load face cascade
        ClassPathResource faceCascade = new ClassPathResource("haarcascade_frontalface_default.xml");
        File faceFile = File.createTempFile("face", ".xml");
        try (InputStream is = faceCascade.getInputStream()) {
            Files.copy(is, faceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        faceDetector = new CascadeClassifier(faceFile.getAbsolutePath());

        // 2) Load eye cascade
        ClassPathResource eyeCascade = new ClassPathResource("haarcascade_eye_tree_eyeglasses.xml");
        File eyeFile = File.createTempFile("eye", ".xml");
        try (InputStream is = eyeCascade.getInputStream()) {
            Files.copy(is, eyeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        eyeDetector = new CascadeClassifier(eyeFile.getAbsolutePath());
    }

    @PostMapping(value = "/center", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> process(@RequestParam("image") MultipartFile file) throws IOException {
        // 1) Decode the uploaded image
        byte[] imageBytes = file.getBytes();
        Mat originalImage = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);

        // 2) Detect face bounding box in the original image
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(originalImage, faces);
        if (faces.empty()) {
            return ResponseEntity.badRequest().body("No face detected.".getBytes());
        }
        // Use first face
        Rect faceRect = faces.toArray()[0];

        // 3) Step A: Ratio-based crop horizontally
        Mat ratioCropped = cropFaceByRatio(originalImage, faceRect);
        if (ratioCropped == null) {
            return ResponseEntity.badRequest().body("Could not crop face to ratio.".getBytes());
        }

        // 4) Step B: Attempt eye-based centering
        Mat eyeCentered = centerHorizontallyByEyesOrFace(ratioCropped);
        if (eyeCentered == null) {
            return ResponseEntity.badRequest().body("Could not center horizontally (no face?).".getBytes());
        }

        // 5) Step C: Vertical crop
        //    Re-detect face in the horizontally cropped image & do vertical cropping
        Mat verticallyCropped = verticalCrop(eyeCentered);
        if (verticallyCropped == null) {
            return ResponseEntity.badRequest().body("Could not vertically crop face.".getBytes());
        }

        // 6) Encode and return final
        MatOfByte output = new MatOfByte();
        Imgcodecs.imencode(".jpg", verticallyCropped, output);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(output.toArray());
    }

    // --------------------------
    //  STAGE 1: RATIO-BASED HORIZONTAL
    // --------------------------
    private Mat cropFaceByRatio(Mat image, Rect faceRect) {
        // Face bounding box midpoint (X)
        double faceMidX = faceRect.x + faceRect.width / 2.0;

        // final width = faceRect.width * FACE_CROP_RATIO
        int finalCropWidth = (int) (faceRect.width * FACE_CROP_RATIO);

        int imgWidth = image.width();
        int imgHeight = image.height();

        // faceMidX at finalCropWidth/2 => cropX = faceMidX - (finalCropWidth/2)
        int cropX = (int) (faceMidX - finalCropWidth / 2.0);

        // clamp
        if (cropX < 0) {
            cropX = 0;
        }
        if (cropX + finalCropWidth > imgWidth) {
            finalCropWidth = imgWidth - cropX;
        }

        if (finalCropWidth <= 0 || cropX >= imgWidth) {
            System.out.println("❌ Could not crop bounding box to ratio " + FACE_CROP_RATIO + ".");
            return null;
        }

        // Crop
        Rect finalCropRect = new Rect(cropX, 0, finalCropWidth, imgHeight);
        return new Mat(image, finalCropRect);
    }

    // --------------------------
    //  STAGE 2: HORIZONTAL EYE-BASED (with fallback)
    // --------------------------
    /**
     * Tries to detect a face & at least 2 eyes in 'croppedImage'.
     * If >=2 eyes found => do eye-based one-sided crop.
     * If <2 eyes => fallback to face-based horizontal center.
     */
    private Mat centerHorizontallyByEyesOrFace(Mat croppedImage) {
        // Detect face in the cropped image
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(croppedImage, faces);
        if (faces.empty()) {
            System.out.println("❌ No face found in centerHorizontallyByEyesOrFace.");
            return null;
        }
        Rect faceRect = faces.toArray()[0];

        // Detect eyes within that face
        Mat faceROI = new Mat(croppedImage, faceRect);
        MatOfRect eyes = new MatOfRect();
        eyeDetector.detectMultiScale(faceROI, eyes);

        // If <2 eyes => fallback to face-based center
        if (eyes.toArray().length < 2) {
            System.out.println("⚠ Less than two eyes, fallback to face-based center.");
            return centerHorizontallyByFace(croppedImage, faceRect);
        }

        // Eye-based center
        Rect[] eyeRects = eyes.toArray();
        // Eye 1 center
        double x1 = faceRect.x + eyeRects[0].x + eyeRects[0].width / 2.0;
        double x2 = faceRect.x + eyeRects[1].x + eyeRects[1].width / 2.0;

        double midpointX = (x1 + x2) / 2.0;

        return eyeBasedHorizontalCrop(croppedImage, midpointX);
    }

    /**
     *  Eye-based horizontal crop: one-sided, so midpoint is at newWidth/2
     */
    private Mat eyeBasedHorizontalCrop(Mat image, double midpointX) {
        int imgWidth = image.width();
        int imgHeight = image.height();
        int eyeX = (int) midpointX;

        // newWidth = 2 * eyeX => midpoint is the center
        int newWidth = eyeX * 2;

        int cropX;
        if (eyeX <= imgWidth / 2) {
            // midpoint on left => crop from right
            cropX = 0;
            if (newWidth > imgWidth) {
                newWidth = imgWidth;
            }
        } else {
            // midpoint on right => crop from left
            cropX = 2 * eyeX - imgWidth;
            if (cropX < 0) {
                newWidth = imgWidth;
                cropX = 0;
            } else if (cropX + newWidth > imgWidth) {
                newWidth = imgWidth - cropX;
            }
        }

        if (newWidth <= 0 || cropX >= imgWidth) {
            System.out.println("❌ Could not eye-center horizontally. Out of bounds.");
            return null;
        }

        Rect cropRect = new Rect(cropX, 0, newWidth, imgHeight);
        return new Mat(image, cropRect);
    }

    /**
     * If <2 eyes => fallback logic: horizontally center by face bounding box midpoint
     * using a one-sided approach (similar to eye-based but using the faceRect midpoint).
     */
    private Mat centerHorizontallyByFace(Mat image, Rect faceRect) {
        int imgWidth = image.width();
        int imgHeight = image.height();

        double faceMidX = faceRect.x + faceRect.width / 2.0;
        int midX = (int) faceMidX;

        // newWidth = 2 * midX => midpoint is the center
        int newWidth = midX * 2;

        int cropX;
        if (midX <= imgWidth / 2) {
            // face midpoint on left => crop from right
            cropX = 0;
            if (newWidth > imgWidth) {
                newWidth = imgWidth;
            }
        } else {
            // midpoint on right => crop from left
            cropX = 2 * midX - imgWidth;
            if (cropX < 0) {
                newWidth = imgWidth;
                cropX = 0;
            } else if (cropX + newWidth > imgWidth) {
                newWidth = imgWidth - cropX;
            }
        }

        if (newWidth <= 0 || cropX >= imgWidth) {
            System.out.println("❌ Could not face-center horizontally. Out of bounds.");
            return null;
        }

        Rect cropRect = new Rect(cropX, 0, newWidth, imgHeight);
        return new Mat(image, cropRect);
    }

    // --------------------------
    //  STAGE 3: VERTICAL CROP
    // --------------------------
    /**
     * Crops vertically using VERTICAL_TOP_RATIO & VERTICAL_BOTTOM_RATIO:
     * - topY = faceRect.y - (faceRect.height * VERTICAL_TOP_RATIO)
     * - bottomY = faceRect.y + faceRect.height + (faceRect.height * VERTICAL_BOTTOM_RATIO)
     *
     * We detect the face again in the horizontally-cropped image, then clamp
     * and perform the vertical crop. The horizontal dimension remains unchanged.
     */
    private Mat verticalCrop(Mat horizontalImage) {
        // 1) Re-detect face in the horizontally cropped image
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(horizontalImage, faces);
        if (faces.empty()) {
            System.out.println("❌ No face found for verticalCrop().");
            return null;
        }
        Rect faceRect = faces.toArray()[0];
    
        // 2) Calculate final desired crop height
        int faceH = faceRect.height;
        double finalCropHeight = faceH * (1 + VERTICAL_TOP_EXTRA + VERTICAL_BOTTOM_EXTRA);
    
        // 3) Where to place faceRect vertically?
        //    The bounding box stays at faceRect.y -> faceRect.y + faceRect.height.
        //    So topMargin = faceH * VERTICAL_TOP_EXTRA
        //    => topY = faceRect.y - topMargin
        double topMargin = faceH * VERTICAL_TOP_EXTRA;
        double topY = faceRect.y - topMargin;
        double bottomY = topY + finalCropHeight; 
        // (Because total height is finalCropHeight = boundingBox + top + bottom)
    
        // 4) Clamp to [0, horizontalImage.height()]
        int imgHeight = horizontalImage.height();
        if (topY < 0) {
            topY = 0;
        }
        if (bottomY > imgHeight) {
            bottomY = imgHeight;
        }
    
        // 5) Final computed crop region
        int cropY = (int) topY;
        int newHeight = (int) (bottomY - topY);
    
        // Validate
        if (newHeight <= 0 || cropY >= imgHeight) {
            System.out.println("❌ Could not vertically crop. Out of bounds.");
            return null;
        }
    
        // 6) Perform the crop (width stays the same)
        int imgWidth = horizontalImage.width();
        Rect vCropRect = new Rect(0, cropY, imgWidth, newHeight);
        return new Mat(horizontalImage, vCropRect);
    }
}
