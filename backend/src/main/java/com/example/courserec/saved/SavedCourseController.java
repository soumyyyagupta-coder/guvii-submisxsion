package com.example.courserec.saved;

import com.example.courserec.auth.AuthService;
import com.example.courserec.auth.AuthenticatedUser;
import com.example.courserec.catalog.CatalogNotFoundException;
import com.example.courserec.catalog.CatalogValidationException;
import com.example.courserec.catalog.CourseResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/saved")
public class SavedCourseController {

    private final SavedCourseService savedCourseService;
    private final AuthService authService;

    public SavedCourseController(SavedCourseService savedCourseService, AuthService authService) {
        this.savedCourseService = savedCourseService;
        this.authService = authService;
    }

    @GetMapping
    public List<CourseResponse> getSavedCourses(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        AuthenticatedUser user = authService.requireUser(authHeader);
        return savedCourseService.getSavedCourses(user.id());
    }

    @PostMapping("/{courseId}")
    public ResponseEntity<Void> saveCourse(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable long courseId) {
        AuthenticatedUser user = authService.requireUser(authHeader);
        savedCourseService.saveCourse(user.id(), courseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> unsaveCourse(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable long courseId) {
        AuthenticatedUser user = authService.requireUser(authHeader);
        savedCourseService.unsaveCourse(user.id(), courseId);
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
