import React, { useEffect, useState } from 'react';
import { Bell, X, Check, AlertCircle, Trash2 } from 'lucide-react';
import { useNotifications } from '../hooks/useNotifications';
import { getMyUserId, getNotifications, getUnreadNotificationCount } from '../services/api';

export default function NotificationsPage() {
  const [userId, setUserId] = useState(null);
  const [filter, setFilter] = useState('all');
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [allNotifications, setAllNotifications] = useState([]); // ✅ Store all notifications

  // Load user ID and initial notifications
  useEffect(() => {
    const loadInitialData = async () => {
      try {
        // Get user ID
        const userRes = await getMyUserId();
        const uid = userRes.data;
        console.log('User ID:', uid);
        setUserId(uid);

        // Load existing notifications from DB
        const notifRes = await getNotifications();
        console.log('Initial notifications from DB:', notifRes.data);
        setAllNotifications(notifRes.data);

        // Load unread count
        const countRes = await getUnreadNotificationCount();
        console.log('Unread count:', countRes.data);
      } catch (err) {
        console.error('Failed to load initial data:', err);
      } finally {
        setLoadingInitial(false);
      }
    };

    loadInitialData();
  }, []);

  // Subscribe to WebSocket for real-time updates
  const { notifications: wsNotifications, unreadCount, isConnected, markAsRead } =
    useNotifications(userId);

  // Merge WebSocket notifications with DB notifications
  // WebSocket notifications come first (newest), then DB notifications that aren't duplicates
  const mergedNotifications = [
    ...wsNotifications,
    ...allNotifications.filter(
      (dbNotif) => !wsNotifications.some((wsNotif) => wsNotif.id === dbNotif.id)
    ),
  ];

  // Filter notifications
  const filteredNotifications = mergedNotifications.filter((n) => {
    if (filter === 'all') return true;
    if (filter === 'unread') return !n.isRead;
    return n.type === filter;
  });

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'MATCH':
        return '🎯';
      case 'APPOINTMENT':
        return '📅';
      case 'MESSAGE':
        return '💬';
      case 'CASE':
        return '📋';
      default:
        return '📬';
    }
  };

  const getNotificationColor = (type) => {
    switch (type) {
      case 'MATCH':
        return 'bg-blue-50 border-blue-200';
      case 'APPOINTMENT':
        return 'bg-purple-50 border-purple-200';
      case 'MESSAGE':
        return 'bg-green-50 border-green-200';
      case 'CASE':
        return 'bg-orange-50 border-orange-200';
      default:
        return 'bg-gray-50 border-gray-200';
    }
  };

  if (loadingInitial) {
    return (
      <div className="h-full bg-white flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-950"></div>
          <p className="text-gray-600">Loading notifications...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full bg-white flex flex-col">
      {/* Header */}
      <div className="border-b p-6">
        <div className="flex items-center gap-3 mb-4">
          <Bell size={24} className="text-blue-950" />
          <h1 className="text-2xl font-bold text-gray-900">Notifications</h1>
          {unreadCount > 0 && (
            <span className="ml-auto bg-red-500 text-white px-3 py-1 rounded-full text-sm font-semibold">
              {unreadCount}
            </span>
          )}
        </div>

        {/* Connection Status */}
        <div className="flex items-center gap-2 text-sm">
          <span
            className={`w-2 h-2 rounded-full ${
              isConnected ? 'bg-green-500 animate-pulse' : 'bg-gray-400'
            }`}
          />
          <span className="text-gray-600">
            {isConnected ? 'Live • Connected' : 'Offline • Reconnecting...'}
          </span>
        </div>
      </div>

      {/* Filters */}
      <div className="flex gap-2 px-6 py-4 border-b overflow-x-auto">
        {['all', 'unread', 'MATCH', 'APPOINTMENT', 'MESSAGE', 'CASE'].map(
          (f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-4 py-2 rounded-full whitespace-nowrap font-medium transition-all ${
                filter === f
                  ? 'bg-blue-950 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {f.charAt(0).toUpperCase() + f.slice(1)}
              {f === 'unread' && unreadCount > 0 && (
                <span className="ml-2 bg-red-500 text-white text-xs px-2 py-0.5 rounded-full">
                  {unreadCount}
                </span>
              )}
            </button>
          )
        )}
      </div>

      {/* Notifications List */}
      <div className="flex-1 overflow-y-auto">
        {filteredNotifications.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center text-gray-500">
            <AlertCircle size={48} className="mb-4 text-gray-400" />
            <p className="text-lg font-medium">
              {filter === 'unread' ? 'No unread notifications' : 'No notifications'}
            </p>
            <p className="text-sm text-gray-400 mt-2">
              {isConnected ? 'Waiting for new notifications...' : 'Reconnecting...'}
            </p>
          </div>
        ) : (
          <div className="space-y-2 p-4">
            {filteredNotifications.map((notification) => (
              <div
                key={`${notification.id}`}
                className={`flex gap-4 p-4 rounded-lg border-l-4 transition-all hover:shadow-md ${
                  getNotificationColor(notification.type)
                } ${!notification.isRead ? 'border-l-blue-500 shadow-sm' : 'border-l-gray-300'}`}
              >
                {/* Icon */}
                <div className="text-2xl flex-shrink-0">
                  {getNotificationIcon(notification.type)}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-semibold text-gray-900">
                      {notification.type}
                    </p>
                    {!notification.isRead && (
                      <span className="inline-flex h-2 w-2 rounded-full bg-blue-500"></span>
                    )}
                  </div>
                  <p className="text-sm text-gray-700 mt-1">
                    {notification.message}
                  </p>
                  <p className="text-xs text-gray-500 mt-2">
                    {new Date(notification.createdAt).toLocaleString()}
                  </p>
                </div>

                {/* Actions */}
                <div className="flex items-center gap-2 flex-shrink-0">
                  {!notification.isRead && (
                    <button
                      onClick={() => markAsRead(notification.id)}
                      className="p-2 hover:bg-blue-100 rounded-full transition-colors"
                      title="Mark as read"
                    >
                      <Check size={18} className="text-green-600" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}