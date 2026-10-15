package com.example.courserec.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(jdbcTemplate);
    }

    @Test
    @DisplayName("parseList handles null, blanks, comma-separated and bracketed lists correctly")
    void testParseList() {
        assertTrue(CatalogService.parseList(null).isEmpty());
        assertTrue(CatalogService.parseList("   ").isEmpty());
        assertTrue(CatalogService.parseList("[]").isEmpty());

        List<String> commaList = CatalogService.parseList("Java, Python, C++");
        assertEquals(List.of("Java", "Python", "C++"), commaList);

        List<String> bracketedList = CatalogService.parseList("[\"React\", 'TypeScript', 'Node.js']");
        assertEquals(List.of("React", "TypeScript", "Node.js"), bracketedList);
    }

    @Test
    @DisplayName("formatList formats non-empty lists and handles nulls")
    void testFormatList() {
        assertEquals("", CatalogService.formatList(null));
        assertEquals("", CatalogService.formatList(List.of()));
        assertEquals("Docker, Kubernetes", CatalogService.formatList(List.of("Docker", "Kubernetes")));
    }

    @Test
    @DisplayName("createCourse fails with blank title")
    void testCreateCourseBlankTitle() {
        CourseRequest req = new CourseRequest(
                "",
                "Description",
                "Category",
                "Beginner",
                10,
                "Coursera",
                "https://example.com",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false
        );

        CatalogValidationException ex = assertThrows(CatalogValidationException.class, () -> catalogService.createCourse(req));
        assertTrue(ex.getMessage().contains("Title is required") || ex.getMessage().contains("title"));
    }

    @Test
    @DisplayName("createCourse fails with negative duration")
    void testCreateCourseNegativeDuration() {
        CourseRequest req = new CourseRequest(
                "Valid Title",
                "Description",
                "Category",
                "Beginner",
                -5,
                "Coursera",
                "https://example.com",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false
        );

        CatalogValidationException ex = assertThrows(CatalogValidationException.class, () -> catalogService.createCourse(req));
        assertTrue(ex.getMessage().contains("Duration"));
    }

    @Test
    @DisplayName("getCourseById throws CatalogNotFoundException when course does not exist")
    void testGetCourseByIdNotFound() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(999L))).thenReturn(List.of());

        CatalogNotFoundException ex = assertThrows(CatalogNotFoundException.class, () -> catalogService.getCourseById(999L));
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    @DisplayName("updateCourse throws CatalogNotFoundException when course does not exist")
    void testUpdateCourseNotFound() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(888L))).thenReturn(0);

        CourseRequest req = new CourseRequest(
                "Updated Title",
                "Description",
                "Category",
                "Beginner",
                10,
                "Coursera",
                "https://example.com",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false
        );

        assertThrows(CatalogNotFoundException.class, () -> catalogService.updateCourse(888L, req));
    }

    @Test
    @DisplayName("deleteCourse throws CatalogNotFoundException when course does not exist")
    void testDeleteCourseNotFound() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(777L))).thenReturn(0);

        assertThrows(CatalogNotFoundException.class, () -> catalogService.deleteCourse(777L));
    }
}
