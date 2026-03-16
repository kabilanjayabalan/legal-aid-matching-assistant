import api from './api';

export const fetchUsers = (params) =>
  api.get("/admin/users", { params });

export const activateUser = (id) =>
  api.put(`/admin/users/${id}/activate`);

export const deactivateUser = (id) =>
  api.put(`/admin/users/${id}/deactivate`);

export const blockUser = (id) =>
  api.put(`/admin/users/${id}/block`);

export const fetchCaseStats = () =>
  api.get('/admin/cases/stats');

export const fetchCasesForMonitoring = (params) =>
  api.get('/admin/cases/monitoring', { params });

export const fetchAppointmentsStats = () =>
  api.get('/admin/appointments/stats');

export const fetchAppointmentsForAdmin = (params) =>
  api.get('/admin/appointments', { params });

export const fetchMatchStats = () =>
  api.get('/admin/matches/stats');

export const fetchMatchesForMonitoring = (params) =>
  api.get('/admin/matches/monitoring', { params });

export const deleteMatch = (matchId) =>
  api.delete(`/admin/matches/${matchId}`);
