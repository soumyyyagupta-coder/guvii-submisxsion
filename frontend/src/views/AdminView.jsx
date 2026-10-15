import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

const LEVELS = ['Beginner', 'Intermediate', 'Advanced'];

const EMPTY_FORM = {
  title: '',
  description: '',
  category: '',
  difficultyLevel: 'Beginner',
  durationHours: '',
  platform: '',
  courseUrl: '',
  careerGoals: '',
  prerequisites: '',
  topics: '',
  skills: '',
  demo: false,
};

export default function AdminView() {
  const { isAdmin } = useAuth();

  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [searchFilter, setSearchFilter] = useState('');

  // Form modal state
  const [modalMode, setModalMode] = useState(null); // 'create' | 'edit' | null
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  // Delete modal state
  const [courseToDelete, setCourseToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const fetchCourses = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getCourses();
      setCourses(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load course list.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isAdmin) {
      fetchCourses();
    }
  }, [isAdmin, fetchCourses]);

  if (!isAdmin) {
    return (
      <div className="empty-state">
        <div className="empty-state-icon">🔒</div>
        <h3 className="empty-state-title">Access Restricted</h3>
        <p className="empty-state-text">
          You must be signed in with an Administrator account to manage catalog courses.
        </p>
      </div>
    );
  }

  const handleOpenCreate = () => {
    setModalMode('create');
    setEditingId(null);
    setFormData(EMPTY_FORM);
    setFormErrors({});
  };

  const handleOpenEdit = (course) => {
    setModalMode('edit');
    setEditingId(course.id);
    setFormData({
      title: course.title || '',
      description: course.description || '',
      category: course.category || '',
      difficultyLevel: course.difficultyLevel || 'Beginner',
      durationHours: course.durationHours ?? '',
      platform: course.platform || '',
      courseUrl: course.courseUrl || '',
      careerGoals: (course.careerGoals || []).join(', '),
      prerequisites: (course.prerequisites || []).join(', '),
      topics: (course.topics || []).join(', '),
      skills: (course.skills || []).join(', '),
      demo: Boolean(course.demo ?? course.isDemo),
    });
    setFormErrors({});
  };

  const validateForm = () => {
    const errs = {};
    if (!formData.title.trim()) errs.title = 'Title is required';
    if (!formData.description.trim()) errs.description = 'Description is required';
    if (!formData.category.trim()) errs.category = 'Category is required';
    if (!formData.platform.trim()) errs.platform = 'Platform is required';
    if (formData.durationHours && isNaN(Number(formData.durationHours))) {
      errs.durationHours = 'Duration must be a number';
    }
    setFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const parseCommaList = (str) => {
    if (!str) return [];
    return str
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
  };

  const handleSubmitForm = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setSubmitting(true);
    setError('');
    setSuccessMsg('');

    const payload = {
      title: formData.title.trim(),
      description: formData.description.trim(),
      category: formData.category.trim(),
      difficultyLevel: formData.difficultyLevel,
      durationHours: formData.durationHours ? parseInt(formData.durationHours, 10) : 0,
      platform: formData.platform.trim(),
      courseUrl: formData.courseUrl.trim(),
      careerGoals: parseCommaList(formData.careerGoals),
      prerequisites: parseCommaList(formData.prerequisites),
      topics: parseCommaList(formData.topics),
      skills: parseCommaList(formData.skills),
      demo: formData.demo,
    };

    try {
      if (modalMode === 'create') {
        await api.createCourse(payload);
        setSuccessMsg(`Successfully created course "${payload.title}".`);
      } else if (modalMode === 'edit') {
        await api.updateCourse(editingId, payload);
        setSuccessMsg(`Successfully updated course "${payload.title}".`);
      }
      setModalMode(null);
      await fetchCourses();
    } catch (err) {
      setError(err.message || 'Operation failed. Please check inputs.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!courseToDelete) return;
    setDeleting(true);
    setError('');
    setSuccessMsg('');
    try {
      await api.deleteCourse(courseToDelete.id);
      setSuccessMsg(`Course "${courseToDelete.title}" deleted.`);
      setCourseToDelete(null);
      await fetchCourses();
    } catch (err) {
      setError(err.message || 'Failed to delete course.');
    } finally {
      setDeleting(false);
    }
  };

  const filteredCourses = courses.filter((c) => {
    if (!searchFilter.trim()) return true;
    const term = searchFilter.toLowerCase();
    return (
      c.title?.toLowerCase().includes(term) ||
      c.category?.toLowerCase().includes(term) ||
      c.platform?.toLowerCase().includes(term)
    );
  });

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
            <h1 className="page-title" style={{ marginBottom: 0 }}>Course Catalog Administration</h1>
            <span className="admin-pill">Admin Only</span>
          </div>
          <p className="page-subtitle">
            Create, modify, and manage courses in the master catalog. Changes immediately affect matching recommendations.
          </p>
        </div>

        <button
          type="button"
          className="btn btn-primary"
          onClick={handleOpenCreate}
        >
          + Add New Course
        </button>
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

      {/* Course management table card */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '0.75rem' }}>
          <div style={{ flex: '1 1 240px', maxWidth: '360px' }}>
            <label htmlFor="admin-search-filter" className="sr-only" style={{ display: 'none' }}>
              Filter courses
            </label>
            <input
              id="admin-search-filter"
              type="search"
              className="form-input"
              placeholder="Filter catalog by title, platform, category..."
              value={searchFilter}
              onChange={(e) => setSearchFilter(e.target.value)}
            />
          </div>
          <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
            Total courses: <strong>{courses.length}</strong>
          </div>
        </div>

        {loading ? (
          <div className="loading-container">
            <div className="spinner" />
            <p>Loading course database...</p>
          </div>
        ) : filteredCourses.length === 0 ? (
          <div className="empty-state" style={{ padding: '2rem 1rem' }}>
            <p>No courses match your filter.</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid var(--color-border)', color: 'var(--color-text-muted)' }}>
                  <th style={{ padding: '0.75rem 0.5rem' }}>ID</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Title</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Category</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Platform</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Level</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Duration</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Status</th>
                  <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredCourses.map((c) => {
                  const isDemo = Boolean(c.demo ?? c.isDemo);
                  return (
                    <tr key={c.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                      <td style={{ padding: '0.75rem 0.5rem', fontFamily: 'monospace', color: 'var(--color-text-muted)' }}>
                        #{c.id}
                      </td>
                      <td style={{ padding: '0.75rem 0.5rem', fontWeight: 600, color: 'var(--color-text-main)', maxWidth: '280px' }}>
                        <div>{c.title}</div>
                        {c.topics && c.topics.length > 0 && (
                          <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontWeight: 400 }}>
                            {c.topics.slice(0, 3).map((t) => `#${t}`).join(' ')}
                          </div>
                        )}
                      </td>
                      <td style={{ padding: '0.75rem 0.5rem', color: 'var(--color-text-secondary)' }}>
                        {c.category}
                      </td>
                      <td style={{ padding: '0.75rem 0.5rem', color: 'var(--color-text-secondary)' }}>
                        {c.platform}
                      </td>
                      <td style={{ padding: '0.75rem 0.5rem' }}>
                        <span className={`badge ${c.difficultyLevel === 'Beginner' ? 'badge-level-beginner' : c.difficultyLevel === 'Advanced' ? 'badge-level-advanced' : 'badge-level-intermediate'}`}>
                          {c.difficultyLevel}
                        </span>
                      </td>
                      <td style={{ padding: '0.75rem 0.5rem', color: 'var(--color-text-secondary)' }}>
                        {c.durationHours ? `${c.durationHours}h` : '—'}
                      </td>
                      <td style={{ padding: '0.75rem 0.5rem' }}>
                        {isDemo ? (
                          <span className="badge badge-demo">★ Demo</span>
                        ) : (
                          <span className="badge" style={{ background: '#f1f5f9', color: '#475569' }}>Standard</span>
                        )}
                      </td>
                      <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '0.4rem' }}>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => handleOpenEdit(c)}
                            aria-label={`Edit ${c.title}`}
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger-outline btn-sm"
                            onClick={() => setCourseToDelete(c)}
                            aria-label={`Delete ${c.title}`}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add / Edit Course Modal */}
      {modalMode && (
        <div className="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="course-form-modal-title">
          <div className="modal-content" style={{ maxWidth: '680px' }}>
            <div className="modal-header">
              <h3 id="course-form-modal-title" className="modal-title">
                {modalMode === 'create' ? 'Add New Catalog Course' : `Edit Course #${editingId}`}
              </h3>
              <button
                type="button"
                className="btn-icon"
                onClick={() => setModalMode(null)}
                aria-label="Close"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitForm}>
              <div className="modal-body" style={{ maxHeight: '70vh' }}>
                <div className="form-group">
                  <label htmlFor="form-title" className="form-label">
                    Course Title <span className="required">*</span>
                  </label>
                  <input
                    id="form-title"
                    type="text"
                    className={`form-input ${formErrors.title ? 'error' : ''}`}
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    placeholder="e.g. Modern Full-Stack Web Development"
                    required
                  />
                  {formErrors.title && <div className="form-error">{formErrors.title}</div>}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
                  <div className="form-group">
                    <label htmlFor="form-category" className="form-label">
                      Category <span className="required">*</span>
                    </label>
                    <input
                      id="form-category"
                      type="text"
                      className={`form-input ${formErrors.category ? 'error' : ''}`}
                      value={formData.category}
                      onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                      placeholder="e.g. Computer Science"
                      required
                    />
                    {formErrors.category && <div className="form-error">{formErrors.category}</div>}
                  </div>

                  <div className="form-group">
                    <label htmlFor="form-platform" className="form-label">
                      Platform Provider <span className="required">*</span>
                    </label>
                    <input
                      id="form-platform"
                      type="text"
                      className={`form-input ${formErrors.platform ? 'error' : ''}`}
                      value={formData.platform}
                      onChange={(e) => setFormData({ ...formData, platform: e.target.value })}
                      placeholder="e.g. Coursera, edX, Udemy"
                      required
                    />
                    {formErrors.platform && <div className="form-error">{formErrors.platform}</div>}
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
                  <div className="form-group">
                    <label htmlFor="form-level" className="form-label">
                      Difficulty Level
                    </label>
                    <select
                      id="form-level"
                      className="form-select"
                      value={formData.difficultyLevel}
                      onChange={(e) => setFormData({ ...formData, difficultyLevel: e.target.value })}
                    >
                      {LEVELS.map((lvl) => (
                        <option key={lvl} value={lvl}>{lvl}</option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label htmlFor="form-duration" className="form-label">
                      Duration (Hours)
                    </label>
                    <input
                      id="form-duration"
                      type="number"
                      min="1"
                      className={`form-input ${formErrors.durationHours ? 'error' : ''}`}
                      value={formData.durationHours}
                      onChange={(e) => setFormData({ ...formData, durationHours: e.target.value })}
                      placeholder="e.g. 24"
                    />
                    {formErrors.durationHours && <div className="form-error">{formErrors.durationHours}</div>}
                  </div>
                </div>

                <div className="form-group">
                  <label htmlFor="form-url" className="form-label">
                    Course Website URL
                  </label>
                  <input
                    id="form-url"
                    type="url"
                    className="form-input"
                    value={formData.courseUrl}
                    onChange={(e) => setFormData({ ...formData, courseUrl: e.target.value })}
                    placeholder="https://example.com/course"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="form-desc" className="form-label">
                    Course Description <span className="required">*</span>
                  </label>
                  <textarea
                    id="form-desc"
                    className={`form-textarea ${formErrors.description ? 'error' : ''}`}
                    rows={4}
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Comprehensive overview of syllabus and learning outcomes..."
                    required
                  />
                  {formErrors.description && <div className="form-error">{formErrors.description}</div>}
                </div>

                <div className="form-group">
                  <label htmlFor="form-topics" className="form-label">
                    Topics (comma-separated)
                  </label>
                  <input
                    id="form-topics"
                    type="text"
                    className="form-input"
                    value={formData.topics}
                    onChange={(e) => setFormData({ ...formData, topics: e.target.value })}
                    placeholder="e.g. React, JavaScript, REST APIs, Frontend"
                  />
                  <span className="form-hint">Used for matching against user interests (40% weight).</span>
                </div>

                <div className="form-group">
                  <label htmlFor="form-skills" className="form-label">
                    Skills Taught (comma-separated)
                  </label>
                  <input
                    id="form-skills"
                    type="text"
                    className="form-input"
                    value={formData.skills}
                    onChange={(e) => setFormData({ ...formData, skills: e.target.value })}
                    placeholder="e.g. TypeScript, State Management, Responsive Design"
                  />
                  <span className="form-hint">Matched against user skills and prerequisites (20% weight).</span>
                </div>

                <div className="form-group">
                  <label htmlFor="form-prereqs" className="form-label">
                    Prerequisites (comma-separated)
                  </label>
                  <input
                    id="form-prereqs"
                    type="text"
                    className="form-input"
                    value={formData.prerequisites}
                    onChange={(e) => setFormData({ ...formData, prerequisites: e.target.value })}
                    placeholder="e.g. Basic HTML, Basic JavaScript"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="form-goals" className="form-label">
                    Target Career Goals (comma-separated)
                  </label>
                  <input
                    id="form-goals"
                    type="text"
                    className="form-input"
                    value={formData.careerGoals}
                    onChange={(e) => setFormData({ ...formData, careerGoals: e.target.value })}
                    placeholder="e.g. Frontend Developer, Full Stack Engineer"
                  />
                  <span className="form-hint">Matched against user's target career path (30% weight).</span>
                </div>

                <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginTop: '1rem' }}>
                  <input
                    id="form-demo"
                    type="checkbox"
                    checked={formData.demo}
                    onChange={(e) => setFormData({ ...formData, demo: e.target.checked })}
                    style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                  />
                  <label htmlFor="form-demo" style={{ fontSize: '0.925rem', fontWeight: 600, cursor: 'pointer' }}>
                    Label as Sample / Demo Course data
                  </label>
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setModalMode(null)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submitting}
                >
                  {submitting ? 'Saving Course...' : modalMode === 'create' ? 'Create Course' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {courseToDelete && (
        <div className="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="delete-course-title">
          <div className="modal-content" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3 id="delete-course-title" className="modal-title" style={{ color: 'var(--color-danger)' }}>
                Delete Course
              </h3>
              <button
                type="button"
                className="btn-icon"
                onClick={() => setCourseToDelete(null)}
                aria-label="Close"
              >
                ✕
              </button>
            </div>
            <div className="modal-body">
              <p style={{ color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
                Are you sure you want to delete <strong>"{courseToDelete.title}"</strong> (ID: #{courseToDelete.id})? This will permanently remove it from the catalog and any student recommendations or saved lists.
              </p>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setCourseToDelete(null)}
                disabled={deleting}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleDelete}
                disabled={deleting}
              >
                {deleting ? 'Deleting...' : 'Delete Course'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
