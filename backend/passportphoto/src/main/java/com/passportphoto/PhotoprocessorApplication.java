/*
 * PhotoprocessorApplication.java
 *
 * Entry point for the Passport Photo Processing Spring Boot application.
 * Responsible for initializing system configuration and loading the OpenCV native library.
 *
 */

package com.passportphoto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.passportphoto.config.ConfigEnv;

import jakarta.annotation.PostConstruct;

/**
 * Main class for launching the Spring Boot application.
 */
@SpringBootApplication
public class PhotoprocessorApplication {

    private final ConfigEnv configEnv;

    /**
     * Constructor injection for loading environment config.
     */
    @Autowired
    public PhotoprocessorApplication(ConfigEnv configEnv) {
        this.configEnv = configEnv;
    }

    /**
     * Initializes the application by loading the OpenCV native library from the configured path.
     */
    @PostConstruct
    public void init() {
        System.out.println("Trying to load OpenCV DLL from: ");
        System.load(configEnv.getDdlpath());
        // System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV DLL loaded successfully!");
    }

    /**
     * Spring Boot application entry point.
     */
    public static void main(String[] args) {
        SpringApplication.run(PhotoprocessorApplication.class, args);
    }
}