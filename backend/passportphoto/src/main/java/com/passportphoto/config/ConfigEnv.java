package com.passportphoto.config;

import java.io.IOException;

import org.opencv.objdetect.CascadeClassifier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

import com.passportphoto.service.FaceModelLoader;



@Configuration
public class ConfigEnv {
    @Value("${opencv.dll.path}")
    private String ddlpath;

    public String getDdlpath() {
        return ddlpath;
    }

    @Bean
    @Qualifier("faceModel")
    public FaceModelLoader faceModelLoader(@Value("${model.face.path}") String path) throws Exception {
        return new FaceModelLoader(path);
    }

    @Bean
    @Qualifier("eyeModel")
    public FaceModelLoader eyeModelLoader(@Value("${model.eye.path}") String path) throws Exception {
        return new FaceModelLoader(path);
    }
}

