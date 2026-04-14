package com.parking.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class FileController {
    
    @GetMapping
    public ResponseEntity<Resource> getFile(@RequestParam String path) {
        try {
            String normalizedPath = path.replace("\\", "/");
            Path filePath = Paths.get(normalizedPath).toAbsolutePath();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists()) {
                filePath = Paths.get(System.getProperty("user.dir"), normalizedPath).toAbsolutePath();
                resource = new UrlResource(filePath.toUri());
            }
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            String contentType = "image/jpeg";
            String fileName = filePath.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.endsWith(".gif")) {
                contentType = "image/gif";
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
