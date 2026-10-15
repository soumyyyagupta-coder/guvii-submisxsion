import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import CourseCard from '../components/CourseCard';
import CourseDetailModal from '../components/CourseDetailModal';

const LEVELS = ['', 'Beginner', 'Intermediate', 'Advanced'];

export default function CatalogView({ onPromptLogin }) {
  const { isAuthenticated } = useAuth();

  const [courses, setCourses] = useState([]);
  const [savedIds, setSavedIds] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters
  const [q, setQ] = useState('');
  const [topic, setTopic] = useState('');
  const [level, setLevel] = useState('');
  const [platform, setPlatform] = useState('');
  const [maxDurationHours, setMaxDurationHours] = useState('');

  // Course Detail modal
  const [selectedCourse, setSelectedCourse] = useState(null);

  // Available platforms extracted from loaded courses or popular ones
  const [knownPlatforms, setKnownPlatforms] = useState([]);
  const [knownTopics, setKnownTopics] = useState([]);

  const fetchCourses = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const filters = {};
      if (q) filters.q = q;
      if (topic) filters.topic = topic;
      if (level) filters.level = level;
      if (platform) filters.platform = platform;
      if (maxDurationHours) filters.maxDurationHours = maxDurationHours;

      const [courseList, savedList] = await Promise.all([
        api.getCourses(filters),
        isAuthenticated ? api.getSavedCourses().catch(() => []) : Promise.resolve([]),
      ]);

      const validCourses = Array.isArray(courseList) ? courseList : [];
      setCourses(validCourses);

      if (Array.isArray(savedList)) {
        setSavedIds(new Set(savedList.map((c) => c.id)));
      }

      // Collect known platforms & topics for suggestions
      const platSet = new Set();
      const topSet = new Set();
      validCourses.forEach((c) => {
        if (c.platform) platSet.add(c.platform);
        (c.topics || []).forEach((t) => topSet.add(t));
      });
      setKnownPlatforms((prev) => Array.from(new Set([...prev, ...platSet])));
      setKnownTopics((prev) => Array.from(new Set([...prev, ...topSet])));
    } catch (err) {
      setError(err.message || 'Failed to load course catalog.');
    } finally {
      setLoading(false);
    }
  }, [q, topic, level, platform, maxDurationHours, isAuthenticated]);

  useEffect(() => {
    // Debounce search slightly
    const timer = setTimeout(() => {
      fetchCourses();
    }, 250);
    return () => clearTimeout(timer);
  }, [fetchCourses]);

  const handleClearFilters = () => {
    setQ('');
    setTopic('');
    setLevel('');
    setPlatform('');
    setMaxDurationHours('');
  };

  const handleToggleSave = async (courseId) => {
    if (!isAuthenticated) {
      if (onPromptLogin) onPromptLogin();
      return;
    }

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

  const hasActiveFilters = Boolean(q || topic || level || platform || maxDurationHours);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Course Catalog</h1>
        <p className="page-subtitle">
          Explore courses across various platforms and disciplines. Filter by difficulty, platform, duration, or topics.
        </p>
      </div>

      {/* Filter and Search Box */}
      <div className="card" style={{ marginBottom: '2rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
          {/* Query search */}
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label htmlFor="catalog-search" className="form-label">
              Search by Keyword
            </label>
            <input
              id="catalog-search"
              type="search"
              className="form-input"
              placeholder="e.g. Python, Algorithms, Cloud..."
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
          </div>

          {/* Topic filter */}
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label htmlFor="catalog-topic" className="form-label">
              Topic
            </label>
            <input
              id="catalog-topic"
              type="text"
              className="form-input"
              list="catalog-topics-datalist"
              placeholder="All topics or filter..."
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
            />
            <datalist id="catalog-topics-datalist">
              {knownTopics.map((t) => (
                <option key={t} value={t} />
              ))}
            </datalist>
          </div>

          {/* Level filter */}
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label htmlFor="catalog-level" className="form-label">
              Difficulty Level
            </label>
            <select
              id="catalog-level"
              className="form-select"
              value={level}
              onChange={(e) => setLevel(e.target.value)}
            >
              <option value="">All Difficulty Levels</option>
              {LEVELS.filter(Boolean).map((lvl) => (
                <option key={lvl} value={lvl}>{lvl}</option>
              ))}
            </select>
          </div>

          {/* Platform filter */}
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label htmlFor="catalog-platform" className="form-label">
              Platform
            </label>
            <input
              id="catalog-platform"
              type="text"
              className="form-input"
              list="catalog-platforms-datalist"
              placeholder="All platforms or filter..."
              value={platform}
              onChange={(e) => setPlatform(e.target.value)}
            />
            <datalist id="catalog-platforms-datalist">
              {knownPlatforms.map((p) => (
                <option key={p} value={p} />
              ))}
            </datalist>
          </div>

          {/* Max Duration Hours */}
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label htmlFor="catalog-duration" className="form-label">
              Max Duration (hrs)
            </label>
            <input
              id="catalog-duration"
              type="number"
              min="1"
              max="200"
              className="form-input"
              placeholder="e.g. 20"
              value={maxDurationHours}
              onChange={(e) => setMaxDurationHours(e.target.value)}
            />
          </div>
        </div>

        {hasActiveFilters && (
          <div style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--color-border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
              Filtering active
            </span>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={handleClearFilters}
            >
              Reset Filters
            </button>
          </div>
        )}
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
          <p>Loading course catalog...</p>
        </div>
      ) : courses.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🔍</div>
          <h3 className="empty-state-title">No Courses Match Your Criteria</h3>
          <p className="empty-state-text">
            We couldn't find any courses matching your current search or filter parameters. Try clearing some filters or searching for broader topics.
          </p>
          {hasActiveFilters && (
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleClearFilters}
            >
              Clear All Filters
            </button>
          )}
        </div>
      ) : (
        <div>
          <div style={{ marginBottom: '1.25rem', color: 'var(--color-text-muted)', fontSize: '0.9rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>
              Found <strong>{courses.length}</strong> {courses.length === 1 ? 'course' : 'courses'}
            </span>
          </div>

          <div className="grid-cards">
            {courses.map((course) => {
              const isSaved = savedIds.has(course.id);
              return (
                <CourseCard
                  key={course.id}
                  course={course}
                  isSaved={isSaved}
                  onToggleSave={handleToggleSave}
                  onOpenDetails={(c) => setSelectedCourse(c)}
                  isAuthenticated={isAuthenticated}
                />
              );
            })}
          </div>
        </div>
      )}

      {/* Course Detail Modal */}
      {selectedCourse && (
        <CourseDetailModal
          course={selectedCourse}
          isSaved={savedIds.has(selectedCourse.id)}
          onToggleSave={handleToggleSave}
          isAuthenticated={isAuthenticated}
          onClose={() => setSelectedCourse(null)}
        />
      )}
    </div>
  );
}
