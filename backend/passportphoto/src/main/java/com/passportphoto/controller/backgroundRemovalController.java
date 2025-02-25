package com.passportphoto.controller;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.passportphoto.dto.ImgDTO;

import com.passportphoto.service.BackgroundRemovalService;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/bg")
@CrossOrigin(origins = "http://localhost:3000")

public class backgroundRemovalController {

    private final BackgroundRemovalService backgroundRemoval;

    public backgroundRemovalController(BackgroundRemovalService backgroundRemoval) {
        this.backgroundRemoval = backgroundRemoval;
    }

    @PostMapping("/transform")
    public ResponseEntity<byte[]> transformImg(@RequestParam("file") MultipartFile file, @ModelAttribute ImgDTO imgDTO) {
        try {
            byte[] result = backgroundRemoval.extractFacewater(imgDTO, file); // Transform logic
             HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=processed.png");
            headers.add(HttpHeaders.CONTENT_TYPE, "image/png");
            return new ResponseEntity<>(result, headers, HttpStatus.OK); // Return transformed image
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
