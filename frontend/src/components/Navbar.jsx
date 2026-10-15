import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function Navbar({ currentView, setView }) {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <div style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
          <div
            className="nav-brand"
            onClick={() => setView(isAuthenticated ? 'recommendations' : 'catalog')}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                setView(isAuthenticated ? 'recommendations' : 'catalog');
              }
            }}
          >
            <div className="nav-brand-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                <line x1="9" y1="7" x2="15" y2="7"></line>
                <line x1="9" y1="11" x2="13" y2="11"></line>
              </svg>
            </div>
            <span>CourseRec</span>
          </div>

          <nav aria-label="Main Navigation">
            <ul className="nav-links">
              {isAuthenticated && (
                <li>
                  <button
                    type="button"
                    className={`nav-button ${currentView === 'recommendations' ? 'active' : ''}`}
                    onClick={() => setView('recommendations')}
                  >
                    <span>🎯</span> Recommendations
                  </button>
                </li>
              )}

              <li>
                <button
                  type="button"
                  className={`nav-button ${currentView === 'catalog' ? 'active' : ''}`}
                  onClick={() => setView('catalog')}
                >
                  <span>📚</span> Course Catalog
                </button>
              </li>

              {isAuthenticated && (
                <li>
                  <button
                    type="button"
                    className={`nav-button ${currentView === 'saved' ? 'active' : ''}`}
                    onClick={() => setView('saved')}
                  >
                    <span>🔖</span> Saved Courses
                  </button>
                </li>
              )}

              {isAuthenticated && (
                <li>
                  <button
                    type="button"
                    className={`nav-button ${currentView === 'profile' ? 'active' : ''}`}
                    onClick={() => setView('profile')}
                  >
                    <span>👤</span> My Profile
                  </button>
                </li>
              )}

              {isAuthenticated && isAdmin && (
                <li>
                  <button
                    type="button"
                    className={`nav-button ${currentView === 'admin' ? 'active' : ''}`}
                    onClick={() => setView('admin')}
                  >
                    <span>⚙️</span> Catalog Admin
                  </button>
                </li>
              )}
            </ul>
          </nav>
        </div>

        <div className="nav-user">
          {isAuthenticated ? (
            <>
              <div className="user-badge">
                <span style={{ fontWeight: 600 }}>{user?.name || user?.email}</span>
                {isAdmin && <span className="admin-pill">Admin</span>}
              </div>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  logout();
                  setView('login');
                }}
              >
                Log Out
              </button>
            </>
          ) : (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                type="button"
                className={`btn btn-sm ${currentView === 'login' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setView('login')}
              >
                Log In
              </button>
              <button
                type="button"
                className={`btn btn-sm ${currentView === 'register' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setView('register')}
              >
                Register
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
