package com.passportphoto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.passportphoto.config.ConfigEnv;

import jakarta.annotation.PostConstruct;
@SpringBootApplication
public class PhotoprocessorApplication {

    private final ConfigEnv configEnv;

    @Autowired
    public PhotoprocessorApplication(ConfigEnv configEnv) {
        this.configEnv = configEnv;
    }
    @PostConstruct
    public void init() {
        System.out.println("Trying to load OpenCV DLL from: ");
        System.load(configEnv.getDdlpath());
        // System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV DLL loaded successfully!");
    }


    public static void main(String[] args) {
        SpringApplication.run(PhotoprocessorApplication.class, args);
    }
}