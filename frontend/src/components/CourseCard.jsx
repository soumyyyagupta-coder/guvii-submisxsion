import React from 'react';

export default function CourseCard({
  course,
  recommendationData,
  isSaved,
  onToggleSave,
  onOpenDetails,
  isAuthenticated
}) {
  if (!course) return null;

  const isDemo = Boolean(course.demo ?? course.isDemo);

  const getDifficultyClass = (level) => {
    const lower = (level || '').toLowerCase();
    if (lower.includes('begin')) return 'badge-level-beginner';
    if (lower.includes('inter')) return 'badge-level-intermediate';
    if (lower.includes('adv')) return 'badge-level-advanced';
    return 'badge-level-intermediate';
  };

  const normScore = recommendationData ? Math.round(recommendationData.score || 0) : null;
  const scoreColor = normScore != null
    ? normScore >= 80 ? '#10b981' : normScore >= 60 ? '#3b82f6' : normScore >= 40 ? '#f59e0b' : '#64748b'
    : null;

  return (
    <article className="card" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Top badges */}
      <div className="card-header">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', alignItems: 'center' }}>
          {isDemo && (
            <span className="badge badge-demo" title="Sample demonstration course">
              ★ Demo
            </span>
          )}
          {course.difficultyLevel && (
            <span className={`badge ${getDifficultyClass(course.difficultyLevel)}`}>
              {course.difficultyLevel}
            </span>
          )}
          {course.category && (
            <span className="badge badge-category">{course.category}</span>
          )}
        </div>

        {/* If recommendation score exists */}
        {normScore != null && (
          <div
            title={`Recommendation score: ${normScore}%`}
            style={{
              fontWeight: 700,
              fontSize: '0.9rem',
              color: scoreColor,
              backgroundColor: `${scoreColor}15`,
              border: `1px solid ${scoreColor}40`,
              padding: '0.2rem 0.55rem',
              borderRadius: 'var(--radius-full)',
              whiteSpace: 'nowrap'
            }}
          >
            {normScore}% match
          </div>
        )}
      </div>

      {/* Title */}
      <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--color-text-main)', marginBottom: '0.5rem', lineHeight: 1.4 }}>
        {course.title}
      </h3>

      {/* Platform & Duration */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.75rem' }}>
        {course.platform && (
          <span style={{ fontWeight: 500, color: 'var(--color-text-secondary)' }}>
            🏛️ {course.platform}
          </span>
        )}
        {course.durationHours != null && (
          <span>🕒 {course.durationHours} hrs</span>
        )}
      </div>

      {/* Description excerpt */}
      <p style={{
        fontSize: '0.9rem',
        color: 'var(--color-text-secondary)',
        lineHeight: 1.5,
        marginBottom: '1rem',
        flex: 1,
        display: '-webkit-box',
        WebkitLineClamp: 3,
        WebkitBoxOrient: 'vertical',
        overflow: 'hidden'
      }}>
        {course.description || 'No description provided.'}
      </p>

      {/* Top reason highlight if recommendation */}
      {recommendationData?.reasons && recommendationData.reasons.length > 0 && (
        <div style={{
          background: 'var(--color-surface-hover)',
          borderRadius: 'var(--radius-sm)',
          padding: '0.4rem 0.6rem',
          fontSize: '0.8rem',
          color: 'var(--color-text-secondary)',
          marginBottom: '0.85rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.35rem'
        }}>
          <span style={{ color: '#10b981', fontWeight: 'bold' }}>✓</span>
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {recommendationData.reasons[0]}
          </span>
        </div>
      )}

      {/* Topics & Skills Preview */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem', marginBottom: '1.25rem' }}>
        {(course.topics || []).slice(0, 3).map((topic, i) => (
          <span key={`t-${i}`} style={{ fontSize: '0.75rem', padding: '0.15rem 0.45rem', background: '#f1f5f9', color: '#475569', borderRadius: 'var(--radius-sm)' }}>
            #{topic}
          </span>
        ))}
        {(course.skills || []).slice(0, 2).map((skill, i) => (
          <span key={`s-${i}`} style={{ fontSize: '0.75rem', padding: '0.15rem 0.45rem', background: '#ecfdf5', color: '#047857', borderRadius: 'var(--radius-sm)' }}>
            ⚡ {skill}
          </span>
        ))}
        {((course.topics?.length || 0) + (course.skills?.length || 0)) > 5 && (
          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', alignSelf: 'center' }}>
            +{((course.topics?.length || 0) + (course.skills?.length || 0)) - 5} more
          </span>
        )}
      </div>

      {/* Actions footer */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderTop: '1px solid var(--color-border)', paddingTop: '0.85rem', marginTop: 'auto' }}>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => onOpenDetails(course, recommendationData)}
          aria-label={`View full details for ${course.title}`}
        >
          View Details
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          {isAuthenticated && (
            <button
              type="button"
              className={`btn-icon ${isSaved ? 'active' : ''}`}
              onClick={() => onToggleSave(course.id)}
              title={isSaved ? 'Remove from saved' : 'Save course'}
              aria-label={isSaved ? `Unsave ${course.title}` : `Save ${course.title}`}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill={isSaved ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
              </svg>
            </button>
          )}

          {course.courseUrl && (
            <a
              href={course.courseUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-icon"
              title="Visit external course provider"
              aria-label={`Open website for ${course.title}`}
            >
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
                <polyline points="15 3 21 3 21 9"></polyline>
                <line x1="10" y1="14" x2="21" y2="3"></line>
              </svg>
            </a>
          )}
        </div>
      </div>
    </article>
  );
}
