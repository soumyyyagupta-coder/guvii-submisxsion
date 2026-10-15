import React, { useState, useEffect } from 'react';
import { useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import RecommendationsView from './views/RecommendationsView';
import CatalogView from './views/CatalogView';
import SavedCoursesView from './views/SavedCoursesView';
import ProfileView from './views/ProfileView';
import AdminView from './views/AdminView';
import AuthView from './views/AuthView';

function getViewFromHash(isAuthenticated, isAdmin) {
  const hash = window.location.hash.replace(/^#\/?/, '').toLowerCase();
  const protectedViews = ['recommendations', 'saved', 'profile'];

  if (hash === 'admin') {
    return isAuthenticated && isAdmin ? 'admin' : isAuthenticated ? 'recommendations' : 'login';
  }

  if (protectedViews.includes(hash)) {
    return isAuthenticated ? hash : 'login';
  }

  if (hash === 'login' || hash === 'register') {
    return isAuthenticated ? 'recommendations' : hash;
  }

  if (hash === 'catalog') {
    return 'catalog';
  }

  return isAuthenticated ? 'recommendations' : 'catalog';
}

export default function App() {
  const { isAuthenticated, isAdmin, loading } = useAuth();
  const [view, setViewState] = useState(() => getViewFromHash(false, false));

  const setView = (newView) => {
    setViewState(newView);
    window.location.hash = `#${newView}`;
  };

  // Sync when auth status updates or URL hash changes
  useEffect(() => {
    const handleHashChange = () => {
      setViewState(getViewFromHash(isAuthenticated, isAdmin));
    };

    window.addEventListener('hashchange', handleHashChange);
    // Initial sync
    setViewState(getViewFromHash(isAuthenticated, isAdmin));

    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [isAuthenticated, isAdmin]);

  if (loading) {
    return (
      <div className="loading-container" style={{ height: '100vh' }}>
        <div className="spinner" style={{ width: '36px', height: '36px', borderWidth: '3px' }} />
        <p style={{ fontWeight: 500 }}>Initializing CourseRec platform...</p>
      </div>
    );
  }

  return (
    <div className="app-container">
      <Navbar currentView={view} setView={setView} />

      <main id="main-content" className="main-content">
        {view === 'recommendations' && (
          isAuthenticated ? (
            <RecommendationsView
              onNavigateToProfile={() => setView('profile')}
              onNavigateToCatalog={() => setView('catalog')}
            />
          ) : (
            <AuthView initialMode="login" onSuccess={() => setView('recommendations')} />
          )
        )}

        {view === 'catalog' && (
          <CatalogView onPromptLogin={() => setView('login')} />
        )}

        {view === 'saved' && (
          isAuthenticated ? (
            <SavedCoursesView
              onNavigateToRecommendations={() => setView('recommendations')}
              onNavigateToCatalog={() => setView('catalog')}
            />
          ) : (
            <AuthView initialMode="login" onSuccess={() => setView('saved')} />
          )
        )}

        {view === 'profile' && (
          isAuthenticated ? (
            <ProfileView onProfileUpdated={() => {}} />
          ) : (
            <AuthView initialMode="login" onSuccess={() => setView('profile')} />
          )
        )}

        {view === 'admin' && (
          <AdminView />
        )}

        {view === 'login' && (
          <AuthView initialMode="login" onSuccess={() => setView('recommendations')} />
        )}

        {view === 'register' && (
          <AuthView initialMode="register" onSuccess={() => setView('recommendations')} />
        )}
      </main>

      <footer style={{ borderTop: '1px solid var(--color-border)', background: 'var(--color-surface)', padding: '1.5rem', textAlign: 'center', fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
        <div style={{ maxWidth: '1240px', margin: '0 auto', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
          <div>
            <strong>CourseRec</strong> — Intelligent Course Recommendation Engine
          </div>
          <div style={{ display: 'flex', gap: '1rem' }}>
            <span>Weighted Content-Based Matching</span>
            <span>•</span>
            <span>Spring Boot REST API</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
