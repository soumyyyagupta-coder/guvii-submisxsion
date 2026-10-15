package com.example.courserec.saved;

import com.example.courserec.catalog.CatalogNotFoundException;
import com.example.courserec.catalog.CatalogService;
import com.example.courserec.catalog.CourseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedCourseServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CatalogService catalogService;

    private SavedCourseService savedCourseService;

    @BeforeEach
    void setUp() {
        savedCourseService = new SavedCourseService(jdbcTemplate, catalogService);
    }

    @Test
    @DisplayName("saveCourse throws CatalogNotFoundException if course does not exist in catalog")
    void testSaveCourseNotFound() {
        when(catalogService.courseExists(100L)).thenReturn(false);

        CatalogNotFoundException ex = assertThrows(CatalogNotFoundException.class, () ->
                savedCourseService.saveCourse(1L, 100L));
        assertTrue(ex.getMessage().contains("100"));
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("saveCourse is idempotent if already saved in database")
    void testSaveCourseAlreadySaved() {
        when(catalogService.courseExists(100L)).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(100L))).thenReturn(1);

        assertDoesNotThrow(() -> savedCourseService.saveCourse(1L, 100L));
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("saveCourse handles race condition DuplicateKeyException gracefully")
    void testSaveCourseDuplicateKeyHandled() {
        when(catalogService.courseExists(100L)).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(100L))).thenReturn(0);
        when(jdbcTemplate.update(anyString(), eq(1L), eq(100L), any())).thenThrow(new DuplicateKeyException("Duplicate"));

        assertDoesNotThrow(() -> savedCourseService.saveCourse(1L, 100L));
    }

    @Test
    @DisplayName("unsaveCourse deletes record idempotently")
    void testUnsaveCourseIdempotent() {
        when(jdbcTemplate.update(anyString(), eq(1L), eq(100L))).thenReturn(1);

        assertDoesNotThrow(() -> savedCourseService.unsaveCourse(1L, 100L));
        verify(jdbcTemplate, times(1)).update(anyString(), eq(1L), eq(100L));
    }

    @Test
    @DisplayName("getSavedCourses returns empty list when user has no saved courses")
    void testGetSavedCoursesEmpty() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn(List.of());

        List<CourseResponse> saved = savedCourseService.getSavedCourses(1L);
        assertNotNull(saved);
        assertTrue(saved.isEmpty());
        verify(catalogService, never()).getCoursesByIdsPreservingOrder(anyList());
    }
}
