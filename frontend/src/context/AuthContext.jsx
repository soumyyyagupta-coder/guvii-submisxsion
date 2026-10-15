import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api, getToken, getStoredUser, clearSession, setOnUnauthorized } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(() => getToken());
  const [user, setUserState] = useState(() => getStoredUser());
  const [loading, setLoading] = useState(true);
  const [sessionExpiredMessage, setSessionExpiredMessage] = useState('');

  const logout = useCallback(() => {
    clearSession();
    setTokenState(null);
    setUserState(null);
  }, []);

  const refreshUser = useCallback(async () => {
    if (!getToken()) {
      setLoading(false);
      return null;
    }
    try {
      const freshUser = await api.getMe();
      setUserState(freshUser);
      return freshUser;
    } catch (err) {
      if (err.status === 401) {
        logout();
      }
      return null;
    } finally {
      setLoading(false);
    }
  }, [logout]);

  useEffect(() => {
    setOnUnauthorized(() => {
      logout();
      setSessionExpiredMessage('Your session has expired. Please log in again.');
    });

    if (getToken()) {
      refreshUser();
    } else {
      setLoading(false);
    }
  }, [logout, refreshUser]);

  const login = async (email, password) => {
    setSessionExpiredMessage('');
    const data = await api.login({ email, password });
    setTokenState(data.token);
    setUserState(data.user);
    return data;
  };

  const register = async (name, email, password) => {
    setSessionExpiredMessage('');
    const data = await api.register({ name, email, password });
    setTokenState(data.token);
    setUserState(data.user);
    return data;
  };

  const clearSessionMessage = () => {
    setSessionExpiredMessage('');
  };

  const value = {
    token,
    user,
    isAuthenticated: !!token && !!user,
    isAdmin: user?.role === 'ADMIN',
    loading,
    sessionExpiredMessage,
    clearSessionMessage,
    login,
    register,
    logout,
    refreshUser,
    setUser: setUserState,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
