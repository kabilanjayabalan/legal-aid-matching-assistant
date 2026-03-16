import { create } from "zustand";

const useAuthStore = create((set) => ({
  user: null,
  role: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,

  // Called after successful login
  login: (userData, role, accessToken, refreshToken) =>
    set(() => {
      // Save to sessionStorage
      sessionStorage.setItem("accessToken", accessToken);
      sessionStorage.setItem("refreshToken", refreshToken);
      sessionStorage.setItem("role", role);
      sessionStorage.setItem("email", userData.email);
      sessionStorage.setItem("isAuthenticated", "true");

      return {
        user: userData,
        role,
        accessToken,
        refreshToken,
        isAuthenticated: true,
      };
    }),

  logout: () =>
    set(() => {
      sessionStorage.removeItem("accessToken");
      sessionStorage.removeItem("refreshToken");
      sessionStorage.removeItem("role");
      sessionStorage.removeItem("email");
      sessionStorage.removeItem("isAuthenticated");

      return {
        user: null,
        role: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
      };
    }),

  // Rehydrate state after page refresh
  initializeAuth: () =>
    set(() => {
      const accessToken = sessionStorage.getItem("accessToken");
      const refreshToken = sessionStorage.getItem("refreshToken");
      const role = sessionStorage.getItem("role");
      const email = sessionStorage.getItem("email");
      const isAuthenticated = sessionStorage.getItem("isAuthenticated") === "true";

      if (accessToken && refreshToken && isAuthenticated) {
        return {
          accessToken,
          refreshToken,
          role,
          user: { email },
          isAuthenticated: true,
        };
      }

      return {};
    }),
}));

export default useAuthStore;
