import api from './api';


export const getUserRoleAnalytics = () => {
  return api.get('/admin/analytics/users');
};

export const getCasesAnalytics = async () => {
  return api.get('/admin/analytics/cases');
};

export const getUserAnalytics = async () => {
  return api.get('/admin/analytics/users');
};

export const getOverviewAnalytics = async () => {
  return api.get('/admin/analytics/overview');
};

export const getTrendsAnalytics = (range = 'monthly') => {
  return api.get('/admin/analytics/trends', {
    params: { range }
  });
};

export const getImpactAnalytics = () => {
  return api.get('/admin/analytics/impact');
};



