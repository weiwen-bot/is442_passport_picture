package com.passportphoto.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
@CrossOrigin(origins = "http://localhost:5173")
public class ImageController {

    private final Path uploadDir = Paths.get("uploads");

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws Exception {
        Path file = uploadDir.resolve(filename);
        Resource resource = new UrlResource(file.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
