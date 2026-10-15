import React from 'react';

export default function ScoreBreakdown({ score, components, reasons }) {
  const normScore = Math.round(score || 0);
  const interest = Math.round(components?.interest || 0);
  const career = Math.round(components?.career || 0);
  const skill = Math.round(components?.skill || 0);
  const level = Math.round(components?.level || 0);

  // Determine color theme for overall score
  const getScoreColor = (val) => {
    if (val >= 80) return '#10b981'; // green
    if (val >= 60) return '#3b82f6'; // blue
    if (val >= 40) return '#f59e0b'; // amber
    return '#64748b'; // slate
  };

  const scoreColor = getScoreColor(normScore);

  return (
    <div className="score-breakdown-card" style={{
      background: 'var(--color-surface-subtle)',
      border: '1px solid var(--color-border)',
      borderRadius: 'var(--radius-md)',
      padding: '1rem',
      marginTop: '1rem',
      marginBottom: '1rem'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
        <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Match Ranking
        </span>
        <div style={{
          display: 'inline-flex',
          alignItems: 'baseline',
          gap: '0.2rem',
          backgroundColor: `${scoreColor}15`,
          border: `1px solid ${scoreColor}40`,
          color: scoreColor,
          padding: '0.25rem 0.65rem',
          borderRadius: 'var(--radius-full)',
          fontWeight: 700,
          fontSize: '1rem'
        }}>
          <span>{normScore}%</span>
          <span style={{ fontSize: '0.75rem', fontWeight: 500 }}>match</span>
        </div>
      </div>

      {/* Component progress bars */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.45rem', marginBottom: '0.75rem' }}>
        <ComponentBar label="Interests (40%)" value={interest} color="#3b82f6" />
        <ComponentBar label="Career Goal (30%)" value={career} color="#8b5cf6" />
        <ComponentBar label="Skill Relevance (20%)" value={skill} color="#06b6d4" />
        <ComponentBar label="Proficiency Level (10%)" value={level} color="#10b981" />
      </div>

      {/* Explanation reasons */}
      {reasons && reasons.length > 0 && (
        <div style={{ marginTop: '0.75rem', borderTop: '1px solid var(--color-border)', paddingTop: '0.75rem' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: '0.4rem' }}>
            Why this course was recommended:
          </div>
          <ul style={{ listStyle: 'none', paddingLeft: 0, display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            {reasons.map((reason, idx) => (
              <li key={idx} style={{ display: 'flex', alignItems: 'flex-start', gap: '0.4rem', fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                <span style={{ color: '#10b981', fontWeight: 'bold' }}>✓</span>
                <span>{reason}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function ComponentBar({ label, value, color }) {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', color: 'var(--color-text-secondary)', marginBottom: '0.15rem' }}>
        <span>{label}</span>
        <span style={{ fontWeight: 600 }}>{value}/100</span>
      </div>
      <div style={{ height: '6px', background: 'var(--color-border)', borderRadius: '3px', overflow: 'hidden' }}>
        <div style={{ width: `${Math.min(100, Math.max(0, value))}%`, height: '100%', background: color, borderRadius: '3px', transition: 'width 0.4s ease' }} />
      </div>
    </div>
  );
}
