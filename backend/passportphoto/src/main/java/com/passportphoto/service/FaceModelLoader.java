package com.passportphoto.service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;


import org.opencv.objdetect.CascadeClassifier;

@Component
public class FaceModelLoader {

    private final CascadeClassifier facialModel;

    public FaceModelLoader(@Value("${model.path}") String modelPath) throws Exception{
        this.facialModel = initializeFaceModel(modelPath);

    } 

    private CascadeClassifier initializeFaceModel(String filepath) throws Exception{
        String temp = filepath.split("\\.")[0];
        ClassPathResource faceCascade = new ClassPathResource(filepath);
        File faceFile = File.createTempFile(temp, ".xml");
        try (InputStream is = faceCascade.getInputStream()) {
            Files.copy(is, faceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        CascadeClassifier faceDetector = new CascadeClassifier(faceFile.getAbsolutePath());

        return faceDetector;

    }

    public CascadeClassifier getFaceModel() {
        return this.facialModel;
    }
    
}
