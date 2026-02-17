package com.cisnebranco.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/photos")
@Tag(name = "Photos", description = "Serve uploaded inspection photos")
@Slf4j
public class PhotoController {

    private final Path photoBaseDir;

    public PhotoController(@Value("${app.upload.photo-dir}") String photoDir) {
        this.photoBaseDir = Paths.get(photoDir).toAbsolutePath().normalize();
    }

    @Operation(summary = "Serve an inspection photo by OS ID and filename")
    @GetMapping("/{osId}/{filename}")
    public ResponseEntity<Resource> servePhoto(@PathVariable Long osId,
                                                @PathVariable String filename) {
        Path filePath = photoBaseDir.resolve(String.valueOf(osId)).resolve(filename).normalize();

        if (!filePath.startsWith(photoBaseDir)) {
            log.warn("Path traversal attempt blocked: osId={}, filename={}", osId, filename);
            return ResponseEntity.badRequest().build();
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);
        } catch (MalformedURLException e) {
            log.error("Malformed photo URL for osId={}, filename={}: {}", osId, filename, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error serving photo osId={}, filename={}", osId, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
