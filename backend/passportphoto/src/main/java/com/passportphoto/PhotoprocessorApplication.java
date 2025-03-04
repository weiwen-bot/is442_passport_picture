package com.passportphoto;

import org.opencv.core.Core;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

// import com.passportphoto.config;
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
        System.out.println("OpenCV DLL loaded successfully!");
    }


    public static void main(String[] args) {
        SpringApplication.run(PhotoprocessorApplication.class, args);
    }
}