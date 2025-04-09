/*
 * ConfigEnv.java
 *
 * Configuration class to wire model loaders and define environment-specific properties.
 * Loads DLL path and instantiates beans for face and eye model loaders using OpenCV.
 *
 */

package com.passportphoto.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.passportphoto.service.FaceModelLoader;

/**
 * Spring configuration class for application environment setup.
 * Defines beans for face and eye model loading using paths provided in application properties.
 */
@Configuration
public class ConfigEnv {
    
    /** Path to the OpenCV DLL file, injected from application properties. */
    @Value("${opencv.dll.path}")
    private String ddlpath;

    /** 
     * Returns the DLL path used by the application.
     *
     * @return path to the OpenCV native DLL
     */
    public String getDdlpath() {
        return ddlpath;
    }

    /**
     * Bean for loading the face detection model.
     *
     * @param path the path to the face model file
     * @return a FaceModelLoader instance for face detection
     * @throws Exception if the model file cannot be loaded
     */
    @Bean
    @Qualifier("faceModel")
    public FaceModelLoader faceModelLoader(@Value("${model.face.path}") String path) throws Exception {
        return new FaceModelLoader(path);
    }

    /**
     * Bean for loading the eye detection model.
     *
     * @param path the path to the eye model file
     * @return a FaceModelLoader instance for eye detection
     * @throws Exception if the model file cannot be loaded
     */
    @Bean
    @Qualifier("eyeModel")
    public FaceModelLoader eyeModelLoader(@Value("${model.eye.path}") String path) throws Exception {
        return new FaceModelLoader(path);
    }
}

