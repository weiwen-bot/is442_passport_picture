/*
 * ModelSessionManager.java
 *
 * This service handles loading the MODNet ONNX model and provides access
 * to the initialized OrtSession for inference use throughout the application.
 *
 */

package com.passportphoto.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.stereotype.Service;

import com.passportphoto.util.Constants;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

/**
 * The {@code ModelSessionManager} class is responsible for loading the MODNet
 * ONNX model from the resources directory and initializing a shared OrtSession
 * for background removal inference.
 */
@Service
public class ModelSessionManager {

    private final OrtSession session;

    private final Constants constants;

    /**
     * Constructor that initializes the ONNX session.
     *
     * @throws Exception if the model file is not found or cannot be read
     */
    public ModelSessionManager(Constants constants) throws Exception {
        this.constants = constants;
        this.session = initializeSession();
        
    }
     
    /**
     * Loads the ONNX model from the classpath and creates a temporary file
     * for ONNX Runtime to consume.
     *
     * @return an {@link OrtSession} ready for inference
     * @throws Exception if the model cannot be read or loaded
     */
    private OrtSession initializeSession() throws Exception {
        InputStream modelStream = getClass().getClassLoader().getResourceAsStream(constants.getModelName());
        if (modelStream == null) {
            throw new FileNotFoundException("MODNet model not found in resources: " + constants.getModelName());
        }
        
        File tempModelFile = File.createTempFile("modnet", ".onnx");
        tempModelFile.deleteOnExit();

        try (OutputStream out = new FileOutputStream(tempModelFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = modelStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        OrtEnvironment env = OrtEnvironment.getEnvironment();
        return env.createSession(tempModelFile.getAbsolutePath(), new OrtSession.SessionOptions());
    }

    /**
     * Returns the loaded ONNX {@link OrtSession} instance.
     *
     * @return the session for inference
     */
    public OrtSession getSession() {
        return session;
    }
}
