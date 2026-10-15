package com.example.courserec.catalog;

import com.example.courserec.auth.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final CatalogService catalogService;
    private final AuthService authService;

    public AdminCourseController(CatalogService catalogService, AuthService authService) {
        this.catalogService = catalogService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody CourseRequest request) {
        authService.requireAdmin(authHeader);
        CourseResponse created = catalogService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable long id,
            @RequestBody CourseRequest request) {
        authService.requireAdmin(authHeader);
        CourseResponse updated = catalogService.updateCourse(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable long id) {
        authService.requireAdmin(authHeader);
        catalogService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(CatalogNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CatalogValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(CatalogValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
