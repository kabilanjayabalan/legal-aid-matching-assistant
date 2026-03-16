// src/services/api.js
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
});

// ========= Concurrency Queue Logic =========
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });

  failedQueue = [];
};

// ========= Helper: Parse JWT Expiry =========
const parseJwt = (token) => {
  try {
    return JSON.parse(atob(token.split('.')[1]));
  } catch (e) {
    return null;
  }
};

// ========= Helper: Silent Refresh Timer =========
let refreshTimeoutId = null;

const startSilentRefresh = (token) => {
  if (refreshTimeoutId) clearTimeout(refreshTimeoutId);

  const decoded = parseJwt(token);
  if (!decoded || !decoded.exp) return;

  // Calculate time until expiry (in ms)
  const expiresIn = (decoded.exp * 1000) - Date.now();
  
  // Refresh 10 seconds before expiry (or immediately if already close)
  // Ensure we don't set a negative timeout, but also don't spam if already expired
  const refreshTime = Math.max(0, expiresIn - 10000); 

  // console.log(`Silent refresh scheduled in ${Math.round(refreshTime/1000)} seconds`);

  refreshTimeoutId = setTimeout(async () => {
    const refreshToken = sessionStorage.getItem("refreshToken");
    if (isRefreshing) return;
    if (!refreshToken) return;

    try {
      // console.log("Triggering silent refresh...");
      const res = await axios.post("http://localhost:8080/auth/refresh-token", 
        { refreshToken: refreshToken }
      );

      const newAccessToken = res.data.accessToken;
      const newRefreshToken = res.data.refreshToken;

      sessionStorage.setItem("accessToken", newAccessToken);
      if (newRefreshToken) {
          sessionStorage.setItem("refreshToken", newRefreshToken);
      }
      
      // Restart timer with new token
      startSilentRefresh(newAccessToken);
      
    } catch (err) {
      console.error("Silent refresh failed", err);
      // If silent refresh fails, we let the interceptor handle it later
      // or we could logout, but better to let the user try and fail naturally
      sessionStorage.clear();
      window.location.href = "/signin";
    }
  }, refreshTime);
};

// Initialize timer on load if token exists
const initialToken = sessionStorage.getItem("accessToken");
if (initialToken) {
  startSilentRefresh(initialToken);
}

// ========= 1️⃣ Attach Access Token Automatically =========
api.interceptors.request.use(
  (config) => {
    const accessToken = sessionStorage.getItem("accessToken");
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ========= 2️⃣ Auto Refresh Access Token If Expired =========
api.interceptors.response.use(
  (response) => response,

  async (error) => {
    const originalRequest = error.config;

    // Prevent infinite loops on login/refresh endpoints
    if (originalRequest?.url?.includes("/auth/login") || originalRequest?.url?.includes("/auth/refresh-token")) {
      return Promise.reject(error);
    }

    // Check for both 401 (Unauthorized) and 403 (Forbidden)
    if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
      
      if (isRefreshing) {
        return new Promise(function(resolve, reject) {
          failedQueue.push({resolve, reject});
        }).then(token => {
          originalRequest.headers['Authorization'] = 'Bearer ' + token;
          return api(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = sessionStorage.getItem("refreshToken");

      if (!refreshToken) {
        isRefreshing = false;
        sessionStorage.clear();
        window.location.href = "/signin";
        return Promise.reject(error);
      }

      try {
        const res = await axios.post("http://localhost:8080/auth/refresh-token", 
          { refreshToken: refreshToken }
        );

        const newAccessToken = res.data.accessToken;
        const newRefreshToken = res.data.refreshToken;

        sessionStorage.setItem("accessToken", newAccessToken);
        if (newRefreshToken) {
            sessionStorage.setItem("refreshToken", newRefreshToken);
        }
        
        // Restart silent refresh timer
        startSilentRefresh(newAccessToken);

        processQueue(null, newAccessToken);
        isRefreshing = false;

        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);

      } catch (refreshErr) {
        processQueue(refreshErr, null);
        isRefreshing = false;

        console.error("Refresh token failed:", refreshErr);
        sessionStorage.clear();
        window.location.href = "/signin";
        return Promise.reject(refreshErr);
      }
    }

    return Promise.reject(error);
  }
);

// ========= Directory & Search =========
export const searchDirectory = async (params) => {
  return await api.get("/api/directory/search", { params });
};

export const getSpecializations = async () => {
  return await api.get("/api/directory/specializations");
};

export const getFocusAreas = async () => {
  return await api.get("/api/directory/focus-areas");
};

// ========= Cases & Matches =========
export const getMyCases = async () => {
  return await api.get("/cases/my");
};

export const getCaseById = async (caseId) => {
  return await api.get(`/cases/${caseId}`);
};

export const getCaseTimeline = (caseId) =>
  api.get(`/cases/${caseId}/timeline`);

export const updateCase = async (caseId, data) => {
  return await api.post(`/cases/${caseId}/update`, data);
};

export const getMyMatches = async (page = 0, size = 10) => {
  return await api.get("/matches/my-cases", { params: { page, size } });
};

export const acceptMatch = async (matchId) => {
  return await api.put(`/matches/${matchId}/citizen-accept`);
};

export const rejectMatch = async (matchId) => {
  return await api.put(`/matches/${matchId}/citizen-reject`);
};

export const generateMatches = (caseId, sensitivity) =>
  api.post(`/matches/generate/${caseId}`, null, {
    params: sensitivity != null ? { sensitivity } : {},
  });

export const getProviderDashboardStats = async () => {
  return await api.get("/matches/provider/dashboard-stats");
};

// ========= Chats & Presence =========
export const getMyUserId = async () => {
  return await api.get("/chats/me");
};

export const getChatHistory = async (matchId) => {
  return await api.get(`/chats/${matchId}`);
};

export const checkUserOnline = async (email) => {
  return await api.get(`/presence/users/online`, { params: { email } });
};

// ========= Appointments =========
export const createAppointment = async (appointmentData) => {
  return await api.post("/appointments", appointmentData);
};

export const getMyAppointments = async (userId) => {
  return await api.get("/appointments/my", { params: { userId } });
};

export const updateAppointmentStatus = async (matchId, status) => {
  return await api.put(`/appointments/${matchId}/update`, null, {
    params: { status },
  });
};

export const rescheduleAppointment = async (matchId, appointmentData) => {
  return await api.put(`/appointments/${matchId}/reschedule`, appointmentData);
};

export const acceptAppointment = async (matchId) => {
  return await api.put(`/appointments/${matchId}/accept`);
};

export const cancelAppointment = async (matchId) => {
  return await api.put(`/appointments/${matchId}/cancel`);
};

// ========= Profiles =========
export const getMyProfile = async () => {
  return await api.get("/profile/me");
};

export const getMyAssignedCases = async (page = 0, size = 10) => {
  return await api.get("/matches/my/assigned-cases", {
    params: { page, size },
  });
};

export const confirmMatch = async (matchId) => {
  return await api.put(`/matches/${matchId}/confirm`);
};

export const rejectProviderMatch = (matchId) => {
  return api.put(`/matches/${matchId}/provider-reject`);
};

export const getMyMatchRequests = async (page = 0, size = 10) => {
  return await api.get("/matches/my/requests", { params: { page, size } });
};

export const getSavedProfiles = async () => {
  return await api.get("/matches/my/saved");
};

export const saveProfile = async (matchId) => {
  return await api.put(`/matches/${matchId}/save`);
};

export const unsaveProfile = async (matchId) => {
  return await api.put(`/matches/${matchId}/unsave`);
};

// ========= Provider Directory =========
export const getProviderProfile = async (providerId, providerType) => {
  const endpoint =
    providerType === "LAWYER"
      ? `/api/directory/lawyers/${providerId}`
      : `/api/directory/ngos/${providerId}`;
  return await api.get(endpoint);
};

export const getLawyerProfileById = async (id) => {
  return await getProviderProfile(id, "LAWYER");
};

export const getNgoProfileById = async (id) => {
  return await getProviderProfile(id, "NGO");
};

// ========= System Monitoring (Admin) =========
export const getSystemHealth = async () => {
  const res = await api.get("/admin/system-health");
  return res.data;
};

export const getSystemLoadOverTime = async () => {
  const res = await api.get("/admin/system-load-over-time");
  return res.data;
};

export const getServiceActivity = async () => {
  const res = await api.get("/admin/service-activity");
  return res.data;
};

/**
 * Get comprehensive actuator-based system metrics
 * Includes CPU, Memory, JVM, Disk, Threads, GC, HTTP, and Health information
 */
export const getActuatorMetrics = async () => {
  const res = await api.get("/admin/actuator-metrics");
  return res.data;
};

// ========= Chats =========
export const getMyChats = async () => {
  return await api.get("/chats/my");
};

export const sendChatMessage = async (matchId, message) => {
  return await api.post(`/chats/${matchId}`, { message });
};

export const uploadChatFile = async (matchId, file, message = "") => {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("message", message);
  
  return await api.post(`/chats/${matchId}/upload`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
};

export const downloadChatFile = async (fileId) => {
  return await api.get(`/chats/files/${fileId}`, {
    responseType: "blob",
  });
};
// ========= Notifications =========
export const getNotifications = async () => {
  return await api.get("/notifications");
};

export const getUnreadNotificationCount = async () => {
  return await api.get("/notifications/unread-count");
};

export const markNotificationAsRead = async (notificationId) => {
  return await api.put(`/notifications/${notificationId}/read`);
};

export const deleteNotification = async (notificationId) => {
  return await api.delete(`/notifications/${notificationId}`);
};

export const markAllNotificationsAsRead = async () => {
  return await api.put("/notifications/mark-all-read");
};

export default api;
