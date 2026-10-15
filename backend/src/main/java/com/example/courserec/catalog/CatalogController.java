package com.example.courserec.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<CourseResponse> getCourses(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Integer maxDurationHours) {
        return catalogService.searchCourses(q, topic, level, platform, maxDurationHours);
    }

    @GetMapping("/{id}")
    public CourseResponse getCourse(@PathVariable long id) {
        return catalogService.getCourseById(id);
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
