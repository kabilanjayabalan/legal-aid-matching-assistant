import { useState, useEffect } from "react";
import LogoutButton from "./LogoutButton";
import { FaBars, FaEdit, FaBell } from "react-icons/fa";
import { useNavigate } from "react-router-dom";
import { getEditPageForRole } from "../utils/roleRoutes";
import { useNotifications } from "../hooks/useNotifications";
import { getMyUserId, getNotifications } from "../services/api";
import api from "../services/api";

export default function Header({ onMenuClick }) {
  const navigate = useNavigate();
  const [profileOpen, setProfileOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [profile, setProfile] = useState(null);
  const [userId, setUserId] = useState(null);
  const [allNotifications, setAllNotifications] = useState([]); // Store all notifications
  const [loadingNotifications, setLoadingNotifications] = useState(false);
  const token = sessionStorage.getItem("accessToken");

  // ✅ Use WebSocket hook for LIVE notifications
  const { notifications: wsNotifications, unreadCount, isConnected } = useNotifications(userId);

  // Load user ID and profile
  useEffect(() => {
    if (!token) return;

    // Get user ID for WebSocket
    getMyUserId()
      .then((res) => {
        console.log("User ID:", res.data);
        setUserId(res.data);
        // Load initial notifications from DB
        loadInitialNotifications(res.data);
      })
      .catch((err) => console.error("Failed to get user ID:", err));

    // Get profile info
    api
      .get("/profile/me")
      .then((res) => setProfile(res.data))
      .catch((err) => console.error(err));
  }, [token]);

  // Load initial notifications from database
  const loadInitialNotifications = async (uid) => {
    try {
      setLoadingNotifications(true);
      const res = await getNotifications();
      console.log("Initial notifications from DB:", res.data);
      setAllNotifications(res.data);
    } catch (err) {
      console.error("Failed to load notifications:", err);
    } finally {
      setLoadingNotifications(false);
    }
  };

  // 🔥 Merge WebSocket notifications with DB notifications
  // New real-time notifications appear first, then older DB notifications
  const mergedNotifications = [
    ...wsNotifications,
    ...allNotifications.filter(
      (dbNotif) => !wsNotifications.some((wsNotif) => wsNotif.id === dbNotif.id)
    ),
  ];

  // Get recent notifications (latest 5)
  const recentNotifications = mergedNotifications.slice(0, 5);

  // Calculate actual unread count from merged notifications
  const actualUnreadCount = mergedNotifications.filter((n) => !n.isRead).length;

  const markAsRead = async (id) => {
    try {
      await api.put(`/notifications/${id}/read`);
      console.log("Marked as read:", id);

      // Update in merged notifications
      setAllNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
      );
    } catch (e) {
      console.error("Mark read failed:", e);
    }
  };

  const markAllRead = () => {
    mergedNotifications.forEach((n) => {
      if (!n.isRead) markAsRead(n.id);
    });
  };

  const handleViewAllNotifications = () => {
    setNotificationsOpen(false);
    if (profile) {
      const role = profile.role.toLowerCase();
      navigate(`/dashboard/${role}/notifications`);
    }
  };

  return (
    <>
      {/* HEADER */}
      <header className="w-full bg-blue-950 text-white shadow-md px-6 py-3 flex justify-between items-center z-40 relative">
        {/* LEFT */}
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-800 shadow-inner">
            <span className="text-xl">⚖️</span>
          </div>
          {/* Mobile name */}
          <h1 className="block text-base font-semibold sm:hidden">LegalAid</h1>

          <h1 className="hidden text-lg font-semibold tracking-wide font-serif sm:block">
            LEGAL-AID MATCHING PLATFORM
          </h1>
        </div>

        {/* RIGHT */}
        <div className="flex items-center gap-4">
          {/* Notification Bell */}
          <div className="relative">
            <button
              onClick={() => setNotificationsOpen(!notificationsOpen)}
              onMouseEnter={() => window.innerWidth >= 1024 && setNotificationsOpen(true)}
              className="p-2 rounded-full hover:bg-blue-900 transition relative group"
              title={isConnected ? "Connected to notifications" : "Connecting..."}
            >
              <FaBell className="text-xl" />

              {/* Connection Status Dot */}
              <span
                className={`absolute top-1 right-1 h-2.5 w-2.5 rounded-full border-2 border-blue-950 ${
                  isConnected ? "bg-green-400 animate-pulse" : "bg-gray-400"
                }`}
              />

              {/* Unread Count Badge */}
              {actualUnreadCount > 0 && (
                <span className="absolute -top-1 -right-1 h-5 w-6 bg-red-500 rounded-full border-2 border-blue-950 flex items-center justify-center text-white text-xs font-bold">
                  {actualUnreadCount > 9 ? "9+" : actualUnreadCount}
                </span>
              )}
            </button>

            {/* Notification Dropdown */}
            {notificationsOpen && (
              <>
                <div
                  className="fixed inset-0 z-40 sm:hidden"
                  onClick={() => setNotificationsOpen(false)}
                />
                <div
                  className="
                    fixed sm:absolute
                    inset-x-0 sm:inset-auto
                    top-16 sm:top-full
                    mx-auto sm:mx-0
                    sm:right-0
                    mt-2
                    w-[95vw] sm:w-[420px]
                    bg-white rounded-xl shadow-2xl
                    z-50 text-gray-800
                    overflow-hidden
                    animate-fadeIn
                    max-h-[80vh] sm:max-h-[600px]
                    flex flex-col
                  "
                  onMouseLeave={() => window.innerWidth >= 1024 && setNotificationsOpen(false)}
                >

                  {/* Header */}
                  <div className="p-4 border-b border-gray-100 bg-gradient-to-r from-blue-50 to-white sticky top-0 z-10">
                    <div className="flex justify-between items-center">
                      <div>
                        <h3 className="font-semibold text-gray-900">
                          Notifications
                        </h3>
                        <p className="text-xs text-gray-500 mt-1">
                          {isConnected ? (
                            <span className="flex items-center gap-1">
                              <span className="inline-block w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
                              Live • Connected
                            </span>
                          ) : (
                            <span className="flex items-center gap-1">
                              <span className="inline-block w-2 h-2 bg-gray-400 rounded-full"></span>
                              Reconnecting...
                            </span>
                          )}
                        </p>
                        <p className="text-xs text-gray-400 sm:hidden mt-1">
                          Swipe up to see more notifications
                        </p>
                      </div>
                      {actualUnreadCount > 0 && (
                        <button
                          onClick={markAllRead}
                          className="text-xs text-blue-600 hover:text-blue-700 font-medium hover:underline transition"
                        >
                          Mark all read
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Notifications List */}
                  <div className="flex-1 overflow-y-auto overscroll-contain">
                    {loadingNotifications ? (
                      <div className="p-4 text-center text-gray-500 text-sm">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-950 mx-auto mb-2"></div>
                        <p>Loading notifications...</p>
                      </div>
                    ) : recentNotifications.length === 0 ? (
                      <div className="p-4 text-center text-gray-500 text-sm">
                        <div className="text-3xl mb-2">📭</div>
                        <p>No notifications yet</p>
                        {isConnected && (
                          <p className="text-xs text-gray-400 mt-2">
                            Waiting for new notifications...
                          </p>
                        )}
                      </div>
                    ) : (
                      recentNotifications.map((notif, index) => (
                        <div
                          key={`${notif.id}-${index}`}
                          className={`p-4 border-b border-gray-100 hover:bg-gray-50 transition cursor-pointer group animate-slideDown ${
                            !notif.isRead ? "bg-blue-50/70" : ""
                          }`}
                          onClick={() => markAsRead(notif.id)}
                        >
                          {/* Notification Type Badge */}
                          <div className="flex items-start gap-3">
                            <span className="text-lg flex-shrink-0">
                              {notif.type === "MATCH"
                                ? "🎯"
                                : notif.type === "APPOINTMENT"
                                ? "📅"
                                : notif.type === "MESSAGE"
                                ? "💬"
                                : notif.type === "CASE"
                                ? "📋"
                                : "📬"}
                            </span>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between gap-2">
                                <p className="text-sm font-medium text-gray-900">
                                  {notif.type}
                                </p>
                                {!notif.isRead && (
                                  <span className="inline-block w-2 h-2 bg-blue-500 rounded-full flex-shrink-0"></span>
                                )}
                              </div>
                              <p className="text-sm text-gray-700 mt-1 line-clamp-2">
                                {notif.message}
                              </p>
                              <p className="text-xs text-gray-500 mt-2">
                                {new Date(notif.createdAt).toLocaleString()}
                              </p>
                            </div>
                          </div>
                        </div>
                      ))
                    )}
                  </div>

                  {/* Footer */}
                  <div className="p-3 text-center border-t border-gray-100 bg-gray-50 sticky bottom-0 z-10">
                    <button
                      onClick={handleViewAllNotifications}
                      className="text-sm text-blue-600 font-medium hover:text-blue-700 hover:underline transition"
                    >
                      View all notifications →
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Mobile Menu */}
          <button
            className="lg:hidden text-2xl hover:text-gray-300 transition"
            onClick={onMenuClick}
          >
            <FaBars />
          </button>

          {/* Avatar */}
          <div
            onClick={() => setProfileOpen(true)}
            className="relative group cursor-pointer"
          >
            <div className="w-10 h-10 rounded-full border border-gray-300 overflow-hidden group-hover:ring-2 group-hover:ring-white/70 transition-all">
              <img
                src={`https://ui-avatars.com/api/?name=${
                  profile?.fullName || "User"
                }&background=random`}
                alt="Avatar"
                className="w-full h-full object-cover"
              />
            </div>

            {/* Online Dot */}
            <div className="absolute bottom-0 right-0 h-3 w-3 bg-green-400 border border-white rounded-full"></div>
          </div>
        </div>
      </header>

      {/* RIGHT-SIDE PROFILE PANEL */}
      {profileOpen && (
        <div className="fixed inset-0 z-50 flex justify-end">
          {/* Overlay */}
          <div
            className="absolute inset-0 bg-black/30"
            onClick={() => setProfileOpen(false)}
          />

          {/* PANEL */}
          <div
            className="
              relative
              h-screen
              w-[70%] sm:w-[60%] md:w-[320px] lg:w-[350px] xl:w-[380px]
              bg-white backdrop-blur-xl shadow-2xl
              overflow-y-auto animate-slideLeft rounded-l-3xl
            "
          >
            {profile ? (
              <>
                {/* EDIT PROFILE BUTTON */}
                <div className="mt-3 flex justify-end pr-2">
                  <button
                    onClick={() => {
                      navigate(getEditPageForRole(profile.role));
                      setProfileOpen(false);
                    }}
                    className="px-4 py-2 bg-blue-900 text-white rounded-lg hover:bg-blue-950 
                      flex items-center gap-2"
                  >
                    <FaEdit />
                    Edit
                  </button>
                </div>
                {/* Avatar */}
                <div className="flex flex-col items-center mt-1">
                  <div className="relative">
                    <div className="w-24 h-24 rounded-full overflow-hidden shadow-lg border-4 border-white">
                      <img
                        src={`https://ui-avatars.com/api/?name=${profile.fullName}&background=random`}
                        alt="Avatar"
                        className="w-full h-full object-cover"
                      />
                    </div>
                    <span className="absolute bottom-1 right-1 block w-4 h-4 bg-green-500 border-2 border-white rounded-full"></span>
                  </div>

                  <h3 className="text-xl font-semibold text-gray-800 mt-1">
                    {profile.fullName}
                  </h3>
                  <p className="text-gray-500 text-sm">{profile.email}</p>
                </div>

                {/* Matches Link - Only for Citizen */}
                {profile.role === "CITIZEN" && (
                  <div className="px-6 mt-4">
                    <button
                      onClick={() => {
                        navigate("/dashboard/citizen/matches");
                        setProfileOpen(false);
                      }}
                      className="w-full py-2 bg-blue-50 text-blue-900 rounded-lg font-medium hover:bg-blue-100 transition"
                    >
                      View My Matches
                    </button>
                  </div>
                )}

                <div className="my-3 border-t border-gray-200"></div>

                {/* Details */}
                <div className="px-6 space-y-4">
                  <div className="flex justify-between">
                    <span className="font-medium text-gray-600">Username</span>
                    <span className="text-gray-800">{profile.username}</span>
                  </div>

                  <div className="flex justify-between">
                    <span className="font-medium text-gray-600">Status</span>
                    <span
                      className={`px-2 py-1 text-xs rounded-full 
                      ${
                        profile.approved === true
                          ? "bg-green-100 text-green-700"
                          : profile.approved === false
                          ? "bg-red-100 text-red-700"
                          : "bg-yellow-100 text-yellow-700"
                      }`}
                    >
                      {profile.approved === true
                        ? "Approved"
                        : profile.approved === false
                        ? "Rejected"
                        : "Pending"}
                    </span>
                  </div>

                  <div className="flex justify-between">
                    <span className="font-medium text-gray-600">Role</span>
                    <span className="text-gray-800">{profile.role}</span>
                  </div>

                  <div className="flex justify-between">
                    <span className="font-medium text-gray-600">Member Since</span>
                    <span className="text-gray-800">
                      {profile.createdAt?.split("T")[0]}
                    </span>
                  </div>
                </div>

                <div className="my-4 border-t border-gray-200"></div>

                <div className="flex justify-end px-6 pb-6">
                  <LogoutButton />
                </div>
              </>
            ) : (
              <p className="px-6 py-4">Loading profile...</p>
            )}
          </div>
        </div>
      )}
    </>
  );
}
