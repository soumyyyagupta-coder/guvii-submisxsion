import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../api/client';
import CourseCard from '../components/CourseCard';
import CourseDetailModal from '../components/CourseDetailModal';

export default function SavedCoursesView({ onNavigateToRecommendations, onNavigateToCatalog }) {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedCourse, setSelectedCourse] = useState(null);

  const fetchSaved = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getSavedCourses();
      setCourses(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load saved courses.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSaved();
  }, [fetchSaved]);

  const handleUnsave = async (courseId) => {
    try {
      await api.unsaveCourse(courseId);
      setCourses((prev) => prev.filter((c) => c.id !== courseId));
    } catch (err) {
      alert(`Could not remove saved course: ${err.message}`);
    }
  };

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">Saved Courses</h1>
          <p className="page-subtitle">
            Your personalized shortlist of courses to enroll in, study, or review later.
          </p>
        </div>

        <button
          type="button"
          className="btn btn-secondary"
          onClick={fetchSaved}
          disabled={loading}
        >
          <span>↻</span> Refresh List
        </button>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">
          <span>⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="loading-container">
          <div className="spinner" />
          <p>Loading your saved courses...</p>
        </div>
      ) : courses.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🔖</div>
          <h3 className="empty-state-title">No Saved Courses Yet</h3>
          <p className="empty-state-text">
            Bookmark interesting courses from your personalized recommendations or the general catalog to review them here anytime.
          </p>
          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
            <button
              type="button"
              className="btn btn-primary"
              onClick={onNavigateToRecommendations}
            >
              Check Recommendations
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onNavigateToCatalog}
            >
              Browse Catalog
            </button>
          </div>
        </div>
      ) : (
        <div>
          <div style={{ marginBottom: '1.25rem', color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>
            You have <strong>{courses.length}</strong> {courses.length === 1 ? 'course' : 'courses'} in your saved list.
          </div>

          <div className="grid-cards">
            {courses.map((course) => (
              <CourseCard
                key={course.id}
                course={course}
                isSaved={true}
                onToggleSave={handleUnsave}
                onOpenDetails={(c) => setSelectedCourse(c)}
                isAuthenticated={true}
              />
            ))}
          </div>
        </div>
      )}

      {/* Course Detail Modal */}
      {selectedCourse && (
        <CourseDetailModal
          course={selectedCourse}
          isSaved={true}
          onToggleSave={handleUnsave}
          isAuthenticated={true}
          onClose={() => setSelectedCourse(null)}
        />
      )}
    </div>
  );
}
