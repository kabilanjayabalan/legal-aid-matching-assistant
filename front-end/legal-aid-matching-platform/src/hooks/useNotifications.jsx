import React, { useEffect, useCallback, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';

export const useNotifications = (userId) => {
  const stompClientRef = useRef(null);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    // Don't connect if userId is not available
    if (!userId) {
      setIsConnected(false);
      return;
    }

    const token = sessionStorage.getItem('accessToken');

    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws-notifications',
      connectHeaders: token
        ? { Authorization: `Bearer ${token}` }
        : {},
      reconnectDelay: 5000,
      debug: () => {},

      onConnect: () => {
        console.log('✅ Notifications WebSocket connected');
        setIsConnected(true);

        // Subscribe to personal notifications
        // Convert userId to string to ensure consistency
        const userIdStr = String(userId);
        
        client.subscribe(`/topic/notifications/${userIdStr}`, (message) => {
          try {
            const notification = JSON.parse(message.body);
            console.log('📬 Notification received:', notification);
            setNotifications((prev) => [notification, ...prev]);

            // Optional: Show browser notification
            if (Notification.permission === 'granted') {
              new Notification(`New ${notification.type} notification`, {
                body: notification.message,
                tag: notification.id,
                requireInteraction: false,
              });
            }
          } catch (error) {
            console.error('Failed to parse notification:', error);
          }
        });

        // Subscribe to unread count updates
        client.subscribe(`/user/${userIdStr}/queue/unread-count`, (message) => {
          try {
            const response = JSON.parse(message.body);
            console.log('📊 Unread count updated:', response.count);
            setUnreadCount(response.count);
          } catch (error) {
            console.error('Failed to parse unread count:', error);
          }
        });
      },

      onDisconnect: () => {
        console.log('❌ Notifications WebSocket disconnected');
        setIsConnected(false);
      },

      onStompError: (frame) => {
        console.error('⚠️ WebSocket error:', frame);
        setIsConnected(false);
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      if (stompClientRef.current?.active) {
        stompClientRef.current.deactivate();
      }
      setIsConnected(false);
      stompClientRef.current = null;
    };
  }, [userId]);

  const markAsRead = useCallback((notificationId) => {
    if (!stompClientRef.current?.active) {
      console.warn('WebSocket not connected, cannot mark as read');
      return;
    }

    console.log('📌 Marking notification as read:', notificationId);

    stompClientRef.current.publish({
      destination: '/app/notifications/mark-read',
      body: JSON.stringify({ notificationId }),
    });

    // Optimistic update
    setNotifications((prev) =>
      prev.map((n) =>
        n.id === notificationId ? { ...n, isRead: true } : n
      )
    );
  }, []);

  const acknowledgeNotification = useCallback((notificationId) => {
    if (!stompClientRef.current?.active) {
      console.warn('WebSocket not connected, cannot acknowledge');
      return;
    }

    console.log('✔️ Acknowledging notification:', notificationId);

    stompClientRef.current.publish({
      destination: '/app/notifications/ack',
      body: JSON.stringify({ notificationId }),
    });
  }, []);

  return {
    notifications,
    unreadCount,
    isConnected,
    markAsRead,
    acknowledgeNotification,
  };
};