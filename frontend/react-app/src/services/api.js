import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: (username, password) =>
    api.post('/auth/login', { username, password }),

  signup: (username, password, email, name) =>
    api.post('/auth/signup', { username, password, email, name }),

  refreshToken: (refreshToken) =>
    api.post('/auth/refresh', { refreshToken }),
};

// Document API
export const documentAPI = {
  upload: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  getAll: () => api.get('/documents'),

  getById: (id) => api.get(`/documents/${id}`),

  delete: (id) => api.delete(`/documents/${id}`),
};

// Chat API
export const chatAPI = {
  sendMessage: (message, sessionId) =>
    api.post('/chat', { message, sessionId }),

  getHistory: (sessionId) =>
    api.get('/chat/history', { params: { sessionId } }),
};

export default api;
