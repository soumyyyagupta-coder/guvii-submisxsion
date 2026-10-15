// API Client for Course Recommendation App

const TOKEN_KEY = 'courserec_token';
const USER_KEY = 'courserec_user';

let onUnauthorizedCallback = null;

export function setOnUnauthorized(cb) {
  onUnauthorizedCallback = cb;
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function getStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setStoredUser(user) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(USER_KEY);
  }
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

async function request(endpoint, options = {}) {
  const headers = {
    'Accept': 'application/json',
    ...(options.headers || {}),
  };

  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(options.body);
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    clearSession();
    if (typeof onUnauthorizedCallback === 'function') {
      onUnauthorizedCallback();
    }
  }

  if (!response.ok) {
    let errorMessage = `Request failed (${response.status})`;
    try {
      const errorData = await response.json();
      if (errorData && errorData.error) {
        errorMessage = errorData.error;
      }
    } catch {
      // response wasn't JSON
    }
    const error = new Error(errorMessage);
    error.status = response.status;
    throw error;
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return await response.json();
  }

  return await response.text();
}

// API methods
export const api = {
  // Auth
  async register({ name, email, password }) {
    const data = await request('/api/auth/register', {
      method: 'POST',
      body: { name, email, password },
    });
    if (data.token) setToken(data.token);
    if (data.user) setStoredUser(data.user);
    return data;
  },

  async login({ email, password }) {
    const data = await request('/api/auth/login', {
      method: 'POST',
      body: { email, password },
    });
    if (data.token) setToken(data.token);
    if (data.user) setStoredUser(data.user);
    return data;
  },

  async getMe() {
    const user = await request('/api/auth/me');
    if (user) setStoredUser(user);
    return user;
  },

  // Profile
  async getProfile() {
    return await request('/api/profile');
  },

  async updateProfile(profileData) {
    return await request('/api/profile', {
      method: 'PUT',
      body: profileData,
    });
  },

  async deleteProfile() {
    const res = await request('/api/profile', {
      method: 'DELETE',
    });
    clearSession();
    return res;
  },

  // Catalog
  async getCourses(filters = {}) {
    const params = new URLSearchParams();
    if (filters.q && filters.q.trim()) params.append('q', filters.q.trim());
    if (filters.topic && filters.topic.trim()) params.append('topic', filters.topic.trim());
    if (filters.level && filters.level.trim()) params.append('level', filters.level.trim());
    if (filters.platform && filters.platform.trim()) params.append('platform', filters.platform.trim());
    if (filters.maxDurationHours !== undefined && filters.maxDurationHours !== '' && filters.maxDurationHours !== null) {
      params.append('maxDurationHours', filters.maxDurationHours);
    }

    const queryStr = params.toString();
    const endpoint = `/api/courses${queryStr ? `?${queryStr}` : ''}`;
    return await request(endpoint);
  },

  async getCourse(id) {
    return await request(`/api/courses/${id}`);
  },

  // Saved Courses
  async getSavedCourses() {
    return await request('/api/saved');
  },

  async saveCourse(courseId) {
    return await request(`/api/saved/${courseId}`, {
      method: 'POST',
    });
  },

  async unsaveCourse(courseId) {
    return await request(`/api/saved/${courseId}`, {
      method: 'DELETE',
    });
  },

  // Recommendations
  async getRecommendations() {
    return await request('/api/recommendations');
  },

  // Admin Catalog CRUD
  async createCourse(courseData) {
    return await request('/api/admin/courses', {
      method: 'POST',
      body: courseData,
    });
  },

  async updateCourse(id, courseData) {
    return await request(`/api/admin/courses/${id}`, {
      method: 'PUT',
      body: courseData,
    });
  },

  async deleteCourse(id) {
    return await request(`/api/admin/courses/${id}`, {
      method: 'DELETE',
    });
  },
};
