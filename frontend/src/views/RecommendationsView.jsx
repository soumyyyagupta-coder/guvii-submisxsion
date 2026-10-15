import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../api/client';
import CourseCard from '../components/CourseCard';
import CourseDetailModal from '../components/CourseDetailModal';

export default function RecommendationsView({ onNavigateToProfile, onNavigateToCatalog }) {
  const [recommendations, setRecommendations] = useState([]);
  const [savedIds, setSavedIds] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Selected course for detail modal
  const [selectedItem, setSelectedItem] = useState(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [recsData, savedData] = await Promise.all([
        api.getRecommendations(),
        api.getSavedCourses().catch(() => []),
      ]);

      setRecommendations(Array.isArray(recsData) ? recsData : []);
      if (Array.isArray(savedData)) {
        setSavedIds(new Set(savedData.map((c) => c.id)));
      }
    } catch (err) {
      setError(err.message || 'Unable to load recommendations. Please try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleToggleSave = async (courseId) => {
    const isCurrentlySaved = savedIds.has(courseId);
    try {
      if (isCurrentlySaved) {
        await api.unsaveCourse(courseId);
        setSavedIds((prev) => {
          const next = new Set(prev);
          next.delete(courseId);
          return next;
        });
      } else {
        await api.saveCourse(courseId);
        setSavedIds((prev) => new Set(prev).add(courseId));
      }
    } catch (err) {
      alert(`Could not ${isCurrentlySaved ? 'unsave' : 'save'} course: ${err.message}`);
    }
  };

  const handleOpenDetails = (course, recData) => {
    setSelectedItem({ course, recData });
  };

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title">Personalized Recommendations</h1>
          <p className="page-subtitle">
            Curated especially for you using transparent, multi-factor matching based on your interests, goals, skills, and experience level.
          </p>
        </div>

        <button
          type="button"
          className="btn btn-secondary"
          onClick={fetchData}
          disabled={loading}
          style={{ alignSelf: 'flex-start' }}
        >
          <span>↻</span> Refresh Recommendations
        </button>
      </div>

      {/* Algorithm Transparency Card */}
      <div style={{
        background: 'linear-gradient(to right, #eff6ff, #f0fdf4)',
        border: '1px solid #bfdbfe',
        borderRadius: 'var(--radius-md)',
        padding: '1rem 1.25rem',
        marginBottom: '2rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '0.75rem'
      }}>
        <div style={{ fontSize: '0.875rem', color: '#1e3a8a' }}>
          <strong>Ranking Formula:</strong>{' '}
          <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>
            0.40 × Interest + 0.30 × Career Goal + 0.20 × Skill Relevance + 0.10 × Level
          </span>
        </div>
        <button
          type="button"
          onClick={onNavigateToProfile}
          className="btn btn-sm"
          style={{ background: '#2563eb', color: '#ffffff', border: 'none' }}
        >
          Tune Profile Factors ➔
        </button>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">
          <span>⚠️</span>
          <div style={{ flex: 1 }}>
            <div>{error}</div>
            <button
              type="button"
              className="btn btn-sm btn-secondary"
              onClick={fetchData}
              style={{ marginTop: '0.5rem' }}
            >
              Try Again
            </button>
          </div>
        </div>
      )}

      {loading ? (
        <div className="loading-container">
          <div className="spinner" />
          <p>Analyzing catalog and computing course match scores...</p>
        </div>
      ) : recommendations.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🎯</div>
          <h3 className="empty-state-title">No Recommendations Available Yet</h3>
          <p className="empty-state-text">
            Add your favorite topics, current skills, and target career goals in your profile to enable the matching engine, or browse all courses in the catalog.
          </p>
          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
            <button
              type="button"
              className="btn btn-primary"
              onClick={onNavigateToProfile}
            >
              Set Up Profile
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onNavigateToCatalog}
            >
              Browse Full Catalog
            </button>
          </div>
        </div>
      ) : (
        <div>
          <div style={{ marginBottom: '1rem', color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>
            Showing <strong>{recommendations.length}</strong> ranked courses tailored to your profile:
          </div>

          <div className="grid-cards">
            {recommendations.map((rec, index) => {
              const course = rec.course;
              if (!course) return null;
              const isSaved = savedIds.has(course.id);

              return (
                <CourseCard
                  key={course.id ?? index}
                  course={course}
                  recommendationData={rec}
                  isSaved={isSaved}
                  onToggleSave={handleToggleSave}
                  onOpenDetails={(c, r) => handleOpenDetails(c, r)}
                  isAuthenticated={true}
                />
              );
            })}
          </div>
        </div>
      )}

      {/* Course Detail Modal */}
      {selectedItem && (
        <CourseDetailModal
          course={selectedItem.course}
          recommendationData={selectedItem.recData}
          isSaved={savedIds.has(selectedItem.course?.id)}
          onToggleSave={handleToggleSave}
          isAuthenticated={true}
          onClose={() => setSelectedItem(null)}
        />
      )}
    </div>
  );
}
