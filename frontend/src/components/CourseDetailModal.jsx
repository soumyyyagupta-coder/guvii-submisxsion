import React, { useEffect } from 'react';
import ScoreBreakdown from './ScoreBreakdown';

export default function CourseDetailModal({
  course,
  recommendationData,
  isSaved,
  onToggleSave,
  isAuthenticated,
  onClose
}) {
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  if (!course) return null;

  const isDemo = Boolean(course.demo ?? course.isDemo);

  const getDifficultyClass = (level) => {
    const lower = (level || '').toLowerCase();
    if (lower.includes('begin')) return 'badge-level-beginner';
    if (lower.includes('inter')) return 'badge-level-intermediate';
    if (lower.includes('adv')) return 'badge-level-advanced';
    return 'badge-level-intermediate';
  };

  return (
    <div
      className="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="course-detail-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="modal-content" style={{ maxWidth: '720px' }}>
        <div className="modal-header">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', paddingRight: '1rem' }}>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center' }}>
              {isDemo && (
                <span className="badge badge-demo" title="This course is sample demonstration data">
                  ★ Demo Course
                </span>
              )}
              {course.category && (
                <span className="badge badge-category">{course.category}</span>
              )}
              {course.difficultyLevel && (
                <span className={`badge ${getDifficultyClass(course.difficultyLevel)}`}>
                  {course.difficultyLevel}
                </span>
              )}
              {course.platform && (
                <span className="badge badge-platform">{course.platform}</span>
              )}
              {course.durationHours != null && (
                <span className="badge" style={{ background: '#f8fafc', border: '1px solid #cbd5e1', color: '#475569' }}>
                  🕒 {course.durationHours} hrs
                </span>
              )}
            </div>
            <h2 id="course-detail-title" className="modal-title" style={{ fontSize: '1.4rem' }}>
              {course.title}
            </h2>
          </div>
          <button
            type="button"
            className="btn-icon"
            onClick={onClose}
            aria-label="Close dialog"
            style={{ fontSize: '1.25rem', width: '36px', height: '36px' }}
          >
            ✕
          </button>
        </div>

        <div className="modal-body">
          {/* If opened from recommendations, show score breakdown */}
          {recommendationData && (
            <ScoreBreakdown
              score={recommendationData.score}
              components={recommendationData.components}
              reasons={recommendationData.reasons}
            />
          )}

          {/* Course description */}
          <div style={{ marginBottom: '1.5rem' }}>
            <h4 style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--color-text-main)', marginBottom: '0.5rem' }}>
              Course Overview
            </h4>
            <p style={{ color: 'var(--color-text-secondary)', lineHeight: 1.7, whiteSpace: 'pre-line' }}>
              {course.description || 'No detailed description provided for this course.'}
            </p>
          </div>

          {/* Topics covered */}
          {course.topics && course.topics.length > 0 && (
            <div style={{ marginBottom: '1.25rem' }}>
              <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: '0.4rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Topics Covered
              </h4>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                {course.topics.map((t, idx) => (
                  <span key={idx} className="chip" style={{ background: '#eff6ff', color: '#1d4ed8' }}>
                    #{t}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Skills acquired */}
          {course.skills && course.skills.length > 0 && (
            <div style={{ marginBottom: '1.25rem' }}>
              <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: '0.4rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Skills You Will Learn
              </h4>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                {course.skills.map((s, idx) => (
                  <span key={idx} className="chip" style={{ background: '#ecfdf5', color: '#047857' }}>
                    ⚡ {s}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Prerequisites */}
          {course.prerequisites && course.prerequisites.length > 0 && (
            <div style={{ marginBottom: '1.25rem' }}>
              <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: '0.4rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Prerequisites
              </h4>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                {course.prerequisites.map((p, idx) => (
                  <span key={idx} className="chip" style={{ background: '#fffbeb', color: '#b45309' }}>
                    📋 {p}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Career goals alignment */}
          {course.careerGoals && course.careerGoals.length > 0 && (
            <div style={{ marginBottom: '1.25rem' }}>
              <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: '0.4rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Target Career Paths
              </h4>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                {course.careerGoals.map((g, idx) => (
                  <span key={idx} className="chip" style={{ background: '#f5f3ff', color: '#6d28d9' }}>
                    🎯 {g}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer">
          {isAuthenticated && onToggleSave && (
            <button
              type="button"
              className={`btn ${isSaved ? 'btn-danger-outline' : 'btn-secondary'}`}
              onClick={() => onToggleSave(course.id)}
            >
              {isSaved ? '♥ Saved in My Courses' : '♡ Save Course'}
            </button>
          )}

          {course.courseUrl && (
            <a
              href={course.courseUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-primary"
            >
              Visit Course Website ↗
            </a>
          )}

          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
