package com.passportphoto.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import com.passportphoto.util.Constants;
import ai.onnxruntime.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ModelSessionManager {

    private final OrtSession session;

    public ModelSessionManager() throws Exception {
        this.session = initializeSession();
    }
     /**
     * Load the ONNX model from resources.
     */
    private OrtSession initializeSession() throws Exception {
        InputStream modelStream = getClass().getClassLoader().getResourceAsStream(Constants.MODEL_NAME);
        if (modelStream == null) {
            throw new FileNotFoundException("MODNet model not found in resources: " + Constants.MODEL_NAME);
        }
        // Create a Temp file as input stream is being used
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

    public OrtSession getSession() {
        return session;
    }


    
}
