import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

const PROFICIENCY_LEVELS = ['Beginner', 'Intermediate', 'Advanced'];

const SUGGESTED_INTERESTS = [
  'Web Development',
  'Machine Learning',
  'Data Science',
  'Cloud Computing',
  'Cybersecurity',
  'DevOps',
  'Mobile Apps',
  'Database Systems',
  'Artificial Intelligence',
  'Software Architecture'
];

export default function ProfileView({ onProfileUpdated }) {
  const { user, setUser, logout } = useAuth();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Form states
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [academicBackground, setAcademicBackground] = useState('');
  const [proficiencyLevel, setProficiencyLevel] = useState('Beginner');
  const [careerGoal, setCareerGoal] = useState('');
  const [interests, setInterests] = useState([]);
  const [interestInput, setInterestInput] = useState('');

  const [skills, setSkills] = useState([]);
  const [newSkillName, setNewSkillName] = useState('');
  const [newSkillLevel, setNewSkillLevel] = useState('Beginner');

  // Delete account confirmation
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getProfile();
      if (data) {
        setName(data.name || '');
        setEmail(data.email || '');
        setAcademicBackground(data.academicBackground || '');
        setProficiencyLevel(data.proficiencyLevel || 'Beginner');
        setCareerGoal(data.careerGoal || '');
        setInterests(Array.isArray(data.interests) ? data.interests : []);
        setSkills(Array.isArray(data.skills) ? data.skills : []);
      }
    } catch (err) {
      setError(err.message || 'Failed to load profile. Please refresh.');
    } finally {
      setLoading(false);
    }
  };

  const handleAddInterest = (item) => {
    const val = (item || interestInput).trim();
    if (!val) return;
    if (!interests.some((i) => i.toLowerCase() === val.toLowerCase())) {
      setInterests([...interests, val]);
    }
    setInterestInput('');
  };

  const handleRemoveInterest = (idx) => {
    setInterests(interests.filter((_, i) => i !== idx));
  };

  const handleAddSkill = () => {
    const sName = newSkillName.trim();
    if (!sName) return;
    if (!skills.some((s) => s.name?.toLowerCase() === sName.toLowerCase())) {
      setSkills([...skills, { name: sName, proficiency: newSkillLevel }]);
    }
    setNewSkillName('');
    setNewSkillLevel('Beginner');
  };

  const handleRemoveSkill = (idx) => {
    setSkills(skills.filter((_, i) => i !== idx));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setSaving(true);

    const payload = {
      name: name.trim(),
      academicBackground: academicBackground.trim(),
      proficiencyLevel,
      careerGoal: careerGoal.trim(),
      interests,
      skills,
    };

    try {
      const updated = await api.updateProfile(payload);
      setSuccessMsg('Your profile has been saved successfully! Recommendations will now reflect your updated interests.');
      if (updated?.name && user) {
        setUser({ ...user, name: updated.name });
      }
      if (onProfileUpdated) onProfileUpdated();
    } catch (err) {
      setError(err.message || 'Failed to update profile.');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteAccount = async () => {
    setDeleting(true);
    setError('');
    try {
      await api.deleteProfile();
      logout();
    } catch (err) {
      setError(err.message || 'Failed to delete account.');
      setShowDeleteModal(false);
      setDeleting(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner" />
        <p>Loading your profile...</p>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '840px', margin: '0 auto' }}>
      <div className="page-header">
        <h1 className="page-title">Learner Profile</h1>
        <p className="page-subtitle">
          Keep your background, interests, and career goals up to date to receive highly personalized course recommendations.
        </p>
      </div>

      {successMsg && (
        <div className="alert alert-success" role="alert">
          <span>✓</span>
          <span>{successMsg}</span>
        </div>
      )}

      {error && (
        <div className="alert alert-danger" role="alert">
          <span>⚠️</span>
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="card" style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '1.25rem', color: 'var(--color-text-main)' }}>
            Personal & Academic Information
          </h2>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem' }}>
            <div className="form-group">
              <label htmlFor="profile-name" className="form-label">
                Full Name <span className="required">*</span>
              </label>
              <input
                id="profile-name"
                type="text"
                className="form-input"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="profile-email" className="form-label">
                Email Address
              </label>
              <input
                id="profile-email"
                type="email"
                className="form-input"
                value={email}
                disabled
                style={{ background: 'var(--color-surface-hover)', cursor: 'not-allowed' }}
              />
              <span className="form-hint">Email address cannot be changed.</span>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem' }}>
            <div className="form-group">
              <label htmlFor="profile-academic" className="form-label">
                Academic Background
              </label>
              <input
                id="profile-academic"
                type="text"
                className="form-input"
                placeholder="e.g. Computer Science B.S., High School, Bootcamper"
                value={academicBackground}
                onChange={(e) => setAcademicBackground(e.target.value)}
              />
              <span className="form-hint">Your current or highest completed education level.</span>
            </div>

            <div className="form-group">
              <label htmlFor="profile-level" className="form-label">
                Overall Experience Level
              </label>
              <select
                id="profile-level"
                className="form-select"
                value={proficiencyLevel}
                onChange={(e) => setProficiencyLevel(e.target.value)}
              >
                {PROFICIENCY_LEVELS.map((lvl) => (
                  <option key={lvl} value={lvl}>
                    {lvl}
                  </option>
                ))}
              </select>
              <span className="form-hint">Used for 10% level alignment in course recommendations.</span>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="profile-career" className="form-label">
              Target Career Goal
            </label>
            <input
              id="profile-career"
              type="text"
              className="form-input"
              placeholder="e.g. Full Stack Web Developer, Data Scientist, Cloud Architect"
              value={careerGoal}
              onChange={(e) => setCareerGoal(e.target.value)}
            />
            <span className="form-hint">Weighted at 30% when matching courses against career paths.</span>
          </div>
        </div>

        {/* Interests Section (40% Weight) */}
        <div className="card" style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--color-text-main)' }}>
              Topics & Interests
            </h2>
            <span className="badge" style={{ background: '#eff6ff', color: '#1d4ed8', border: '1px solid #bfdbfe' }}>
              40% Rec Weight
            </span>
          </div>
          <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem', marginBottom: '1rem' }}>
            What technical subjects and domains excite you most? Courses covering these topics are given high priority.
          </p>

          <div className="form-group">
            <label htmlFor="interest-input" className="form-label">
              Add Interests
            </label>
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <input
                id="interest-input"
                type="text"
                className="form-input"
                placeholder="Type a topic (e.g. Machine Learning, React, Python) and press Enter"
                value={interestInput}
                onChange={(e) => setInterestInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddInterest();
                  }
                }}
              />
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => handleAddInterest()}
              >
                Add
              </button>
            </div>

            {/* Selected interests */}
            <div className="chips-container" style={{ minHeight: '52px', marginBottom: '0.75rem' }}>
              {interests.length === 0 ? (
                <span style={{ color: 'var(--color-text-muted)', fontSize: '0.9rem', fontStyle: 'italic', paddingLeft: '0.5rem' }}>
                  No interests added yet. Click suggestions below or type your own.
                </span>
              ) : (
                interests.map((item, idx) => (
                  <span key={idx} className="chip">
                    #{item}
                    <button
                      type="button"
                      className="chip-remove"
                      onClick={() => handleRemoveInterest(idx)}
                      aria-label={`Remove interest ${item}`}
                    >
                      ×
                    </button>
                  </span>
                ))
              )}
            </div>

            {/* Suggestions */}
            <div>
              <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', fontWeight: 600, marginRight: '0.5rem' }}>
                Popular suggestions:
              </span>
              <div style={{ display: 'inline-flex', flexWrap: 'wrap', gap: '0.35rem', marginTop: '0.4rem' }}>
                {SUGGESTED_INTERESTS.filter((s) => !interests.includes(s)).slice(0, 7).map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => handleAddInterest(s)}
                    style={{
                      background: 'var(--color-surface-hover)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-full)',
                      padding: '0.2rem 0.55rem',
                      fontSize: '0.78rem',
                      color: 'var(--color-text-secondary)',
                      cursor: 'pointer'
                    }}
                  >
                    + {s}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Current Skills Section (20% Weight) */}
        <div className="card" style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--color-text-main)' }}>
              Current Skills & Proficiencies
            </h2>
            <span className="badge" style={{ background: '#ecfdf5', color: '#065f46', border: '1px solid #a7f3d0' }}>
              20% Rec Weight
            </span>
          </div>
          <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem', marginBottom: '1.25rem' }}>
            List programming languages, tools, or libraries you already know. Helps check course prerequisites and build on your existing foundation.
          </p>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'flex-end', marginBottom: '1.25rem' }}>
            <div style={{ flex: '2 1 200px' }}>
              <label htmlFor="skill-name-input" className="form-label">
                Skill Name
              </label>
              <input
                id="skill-name-input"
                type="text"
                className="form-input"
                placeholder="e.g. Python, JavaScript, SQL, Git"
                value={newSkillName}
                onChange={(e) => setNewSkillName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddSkill();
                  }
                }}
              />
            </div>

            <div style={{ flex: '1 1 140px' }}>
              <label htmlFor="skill-level-select" className="form-label">
                Proficiency
              </label>
              <select
                id="skill-level-select"
                className="form-select"
                value={newSkillLevel}
                onChange={(e) => setNewSkillLevel(e.target.value)}
              >
                {PROFICIENCY_LEVELS.map((lvl) => (
                  <option key={lvl} value={lvl}>{lvl}</option>
                ))}
              </select>
            </div>

            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleAddSkill}
            >
              Add Skill
            </button>
          </div>

          {/* Current skills list */}
          {skills.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '1.5rem', background: 'var(--color-surface-hover)', borderRadius: 'var(--radius-sm)', color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>
              No current skills listed. Adding your current skills helps recommend courses with matching prerequisites.
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '0.75rem' }}>
              {skills.map((s, idx) => (
                <div
                  key={idx}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '0.6rem 0.85rem',
                    background: 'var(--color-surface-hover)',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-sm)'
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--color-text-main)' }}>
                      {s.name}
                    </div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--color-text-muted)' }}>
                      {s.proficiency}
                    </div>
                  </div>
                  <button
                    type="button"
                    className="chip-remove"
                    onClick={() => handleRemoveSkill(idx)}
                    aria-label={`Remove skill ${s.name}`}
                    style={{ fontSize: '1.1rem' }}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Submit Actions */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
          <button
            type="submit"
            className="btn btn-primary btn-lg"
            disabled={saving}
          >
            {saving ? (
              <>
                <span className="spinner" style={{ width: '18px', height: '18px', borderWidth: '2px' }} />
                <span>Saving Profile...</span>
              </>
            ) : (
              <span>Save Profile Changes</span>
            )}
          </button>

          <button
            type="button"
            className="btn btn-danger-outline"
            onClick={() => setShowDeleteModal(true)}
          >
            Delete Account
          </button>
        </div>
      </form>

      {/* Account Deletion Confirmation Modal */}
      {showDeleteModal && (
        <div className="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="delete-account-title">
          <div className="modal-content" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3 id="delete-account-title" className="modal-title" style={{ color: 'var(--color-danger)' }}>
                Delete Account
              </h3>
              <button
                type="button"
                className="btn-icon"
                onClick={() => setShowDeleteModal(false)}
                aria-label="Close"
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              <p style={{ color: 'var(--color-text-secondary)', lineHeight: 1.6, marginBottom: '1rem' }}>
                Are you sure you want to delete your account? This action is permanent and will remove your saved courses, profile, and session data.
              </p>
              <div className="alert alert-danger" style={{ marginBottom: 0 }}>
                <span>⚠️ This cannot be undone.</span>
              </div>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowDeleteModal(false)}
                disabled={deleting}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleDeleteAccount}
                disabled={deleting}
              >
                {deleting ? 'Deleting Account...' : 'Yes, Delete My Account'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
