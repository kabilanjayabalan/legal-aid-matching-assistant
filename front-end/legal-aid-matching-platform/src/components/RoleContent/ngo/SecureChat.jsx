import React, { useEffect, useRef, useState, useCallback } from "react";
import { FaComments } from "react-icons/fa";
import { Client } from "@stomp/stompjs";
import Avatar from "../../common/Avatar";
import { MessageSquare, Send, X, Mail, Phone, MapPin, Briefcase, Award, Building } from "lucide-react";

import {
  getMyUserId,
  getMyChats,
  getChatHistory,
  checkUserOnline,
  getMyAssignedCases,
  getCaseById,
} from "../../../services/api";
import { useLocation } from "react-router-dom";

/* ===================== UTILS ===================== */

const formatTime = (iso) => {
  if (!iso) return "";
  return new Date(iso).toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
};
const formatConversationTime = (iso) => {
  if (!iso) return "";

  const date = new Date(iso);
  const now = new Date();

  const startOfToday = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate()
  );

  const startOfMessageDay = new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate()
  );

  const diffDays =
    (startOfToday - startOfMessageDay) / (1000 * 60 * 60 * 24);

  // Today → show time
  if (diffDays === 0) {
    return date.toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  // Yesterday
  if (diffDays === 1) {
    return "Yesterday";
  }

  // Within last week → show weekday
  if (diffDays < 7) {
    return date.toLocaleDateString([], { weekday: "short" });
  }

  // Older → show date
  return date.toLocaleDateString([], {
    day: "2-digit",
    month: "short",
  });
};


/* ===================== COMPONENT ===================== */

export default function SecureChat() {
  const location = useLocation();
  const [myUserId, setMyUserId] = useState(null);
  const [conversations, setConversations] = useState([]);
  const [activeChat, setActiveChat] = useState(null);
  const [chatHistory, setChatHistory] = useState([]);
  const [messageInput, setMessageInput] = useState("");
  const [isStompConnected, setIsStompConnected] = useState(false);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [selectedUserProfile, setSelectedUserProfile] = useState(null);
  const [loadingProfile, setLoadingProfile] = useState(false);

  const stompClientRef = useRef(null);
  const messagesEndRef = useRef(null);
  const conversationsRef = useRef(conversations);

  useEffect(() => {
    conversationsRef.current = conversations;
  }, [conversations]);

  /* ===================== LOAD MY USER ===================== */

  useEffect(() => {
    getMyUserId()
      .then((res) => setMyUserId(res.data))
      .catch(() => { });
  }, []);

  /* ===================== LOAD CHAT LIST ===================== */

  useEffect(() => {
    const loadConversations = async () => {
      try {
        // Load existing chats from backend
        const chatsResponse = await getMyChats();
        const existingChats = Array.isArray(chatsResponse.data) ? chatsResponse.data : [];

        // Load accepted matches to add to conversations
        // Fetch a reasonable number (100 matches should cover most cases)
        const matchesResponse = await getMyAssignedCases(0, 100);
        const page = matchesResponse.data || {};
        const matchesData = page.content || [];

        // Filter to only accepted matches (PROVIDER_CONFIRMED)
        const allAcceptedMatches = matchesData.filter(match =>
          match.status === 'PROVIDER_CONFIRMED'
        );

        // Create conversation map from existing chats
        const chatMap = new Map();
        existingChats.forEach((c) => {
          chatMap.set(c.matchId, {
            id: c.matchId,
            name: c.name,
            role: c.role,
            email: c.email,
            caseTitle: c.caseTitle,
            isOnline: false,
            lastMessage: c.lastMessage || "Start Conversation...",
            lastMessageTime: formatConversationTime(c.lastMessageAt),
          });
        });

        // For all accepted matches, fetch the case title and update the conversation
        const profilePromises = allAcceptedMatches.map(match =>
          getCaseById(match.caseId)
            .then(res => ({ ...match, name: res.data.title }))
            .catch(() => ({ ...match, name: `ID: ${match.caseId}` }))
        );

        const matchesWithNames = await Promise.all(profilePromises);

        matchesWithNames.forEach((match) => {
          const existingChat = chatMap.get(match.matchId) || {};
          chatMap.set(match.matchId, {
            ...existingChat, // keep last message, etc.
            id: match.matchId,
            name: match.name, // <-- This is the important part, override the name
            role: "CITIZEN",
            email: existingChat.email || null, // preserve email if present
            caseTitle: existingChat.caseTitle,
            isOnline: existingChat.isOnline || false,
            lastMessage: existingChat.lastMessage || "",
            lastMessageTime: existingChat.lastMessageTime || formatConversationTime(match.createdAt),
          });
        });

        // Handle navigation state - add new chat if provided
        if (location.state?.newChat) {
          const newChat = location.state.newChat;
          if (!chatMap.has(newChat.id)) {
            chatMap.set(newChat.id, {
              id: newChat.id,
              name: newChat.name,
              role: newChat.role,
              email: newChat.email,
              caseTitle: newChat.caseTitle,
              isOnline: false,
              lastMessage: "",
              lastMessageTime: "",
            });
          }
        }

        // Set active chat from navigation state
        if (location.state?.activeMatchId) {
          setActiveChat(location.state.activeMatchId);
        }

        setConversations(Array.from(chatMap.values()));
      } catch (error) {
        console.error("Failed to load conversations:", error);
      }
    };

    loadConversations();
  }, [location.state]);

  /* ===================== PRESENCE POLLING ===================== */

  const checkPresence = useCallback(async () => {
    const currentConversations = conversationsRef.current;
    if (!currentConversations || !currentConversations.length) return;

    const updates = await Promise.all(
      currentConversations.map(async (c) => {
        if (!c.email) return null;
        try {
          const res = await checkUserOnline(c.email);
          return { id: c.id, isOnline: res.data };
        } catch {
          return null;
        }
      })
    );

    setConversations((prev) =>
      prev.map((c) => {
        const update = updates.find((u) => u && u.id === c.id);
        if (update && c.isOnline !== undefined && update.isOnline !== c.isOnline) {
          return { ...c, isOnline: update.isOnline };
        }
        return c;
      })
    );
  }, []);

  useEffect(() => {
    const interval = setInterval(checkPresence, 10000);
    return () => clearInterval(interval);
  }, [checkPresence]);

  useEffect(() => {
    if (conversations.length > 0) {
      checkPresence();
    }
  }, [conversations.length, checkPresence]);

  /* ===================== LOAD CHAT HISTORY ===================== */

  useEffect(() => {
    if (!activeChat || !myUserId) return;

    getChatHistory(activeChat).then((res) => {
      if (!Array.isArray(res.data)) return;

      setChatHistory(
        res.data.map((m) => ({
          id: m.createdAt,
          text: m.message,
          sender: m.senderId === myUserId ? "me" : "other",
          timestamp: formatTime(m.createdAt),
        }))
      );
    });
  }, [activeChat, myUserId]);

  /* ===================== 🔥 NATIVE WEBSOCKET CONNECT ===================== */

  useEffect(() => {
    if (!activeChat || !myUserId) return;

    setIsStompConnected(false);

    const token = sessionStorage.getItem("accessToken");

    const client = new Client({
      brokerURL: "ws://localhost:8080/ws-chat", // ✅ native WebSocket
      connectHeaders: token
        ? { Authorization: `Bearer ${token}` }
        : {},
      reconnectDelay: 5000,
      debug: () => { },

      onConnect: () => {
        setIsStompConnected(true);

        client.subscribe(`/topic/chat/${activeChat}`, (msg) => {
          const chat = JSON.parse(msg.body);

          // 🔥 IGNORE MY OWN MESSAGE (already optimistically added)
          if (chat.senderId === myUserId) return;

          setChatHistory((prev) => [
            ...prev,
            {
              id: chat.createdAt,
              text: chat.message,
              sender: "other",
              timestamp: formatTime(chat.createdAt),
            },
          ]);
          // Sidebar update for OTHER user messages
          setConversations((prev) =>
            prev.map((c) =>
              c.id === activeChat
                ? {
                  ...c,
                  lastMessage: chat.message,
                  lastMessageTime: formatTime(chat.createdAt),
                }
                : c
            )
          );
        });
      },

      onDisconnect: () => setIsStompConnected(false),
      onStompError: () => setIsStompConnected(false),
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      setIsStompConnected(false);
      client.deactivate();
      stompClientRef.current = null;
    };
  }, [activeChat, myUserId]);

  /* ===================== AUTOSCROLL ===================== */

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [chatHistory]);

  /* ===================== SEND MESSAGE ===================== */

  const handleSendMessage = () => {
    if (!messageInput.trim() || !isStompConnected) return;

    const message = messageInput.trim();
    const now = new Date().toISOString();

    // 🔥 1. Optimistically update sidebar
    setConversations((prev) =>
      prev.map((c) =>
        c.id === activeChat
          ? {
            ...c,
            lastMessage: message,
            lastMessageTime: formatConversationTime(now),
          }
          : c
      )
    );
    // 🔥 2. Optimistically update chat window
    setChatHistory((prev) => [
      ...prev,
      {
        id: now,
        text: message,
        sender: "me",
        timestamp: formatTime(now),
      },
    ]);

    // 🔥 3. Send via WebSocket
    stompClientRef.current.publish({
      destination: `/app/chat.send/${activeChat}`,
      body: JSON.stringify({ message }),
    });

    setMessageInput("");
  };

  /* ===================== HANDLE PROFILE VIEW ===================== */

  const handleViewProfile = async (conversation) => {
    if (!conversation) return;

    setLoadingProfile(true);
    setShowProfileModal(true);

    try {
      // For NGOs viewing citizen profiles, we primarily show basic info
      // since citizens don't have detailed profiles like lawyers/NGOs
      const profileData = {
        name: conversation.name,
        email: conversation.email,
        role: conversation.role,
        caseTitle: conversation.caseTitle,
        type: conversation.role || "CITIZEN",
      };

      setSelectedUserProfile(profileData);
    } catch (error) {
      console.error("Failed to load profile:", error);
      setSelectedUserProfile({
        name: conversation.name,
        email: conversation.email,
        role: conversation.role,
        caseTitle: conversation.caseTitle,
        type: conversation.role,
      });
    } finally {
      setLoadingProfile(false);
    }
  };

  const closeProfileModal = () => {
    setShowProfileModal(false);
    setSelectedUserProfile(null);
  };

  // 🔥 THIS MUST BE HERE
  const activeConversation = conversations.find(
    (c) => c.id === activeChat
  );

  /* ===================== UI (UNCHANGED) ===================== */

  return (
    <div className="h-[calc(100vh-140px)] bg-slate-50 flex overflow-hidden -m-2">
      {/* SIDEBAR */}
      <div
        className={`w-full md:w-80 lg:w-96 bg-white border-r flex flex-col ${activeChat ? "hidden md:flex" : "flex"
          }`}
      >
        <div className="p-4 border-b">
          <h2 className="text-lg font-semibold text-blue-950">
            Messages
          </h2>
        </div>

        <div className="flex-1 overflow-y-auto">
          {conversations.length === 0 ? (
                      <div className="flex flex-col items-center justify-center h-full text-center px-6 text-gray-500">
                        <MessageSquare size={36} className="mb-3 text-gray-400" />
                          <p className="text-sm font-medium">No conversations yet</p>
                          <p className="text-xs text-gray-400 mt-1">
                            Accept a match to start chatting with a lawyer or NGO
                          </p>
                      </div>
                    ) : (
                      conversations.map((c) => (
            <div
              key={c.id}
              onClick={() => setActiveChat(c.id)}
              className={`p-4 border-b cursor-pointer ${activeChat === c.id
                ? "bg-blue-50 border-l-4 border-l-blue-950"
                : "hover:bg-gray-50"
                }`}
            >
              <div className="flex gap-3">
                <div
                  className="relative cursor-pointer"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleViewProfile(c);
                  }}
                >
                  <Avatar name={c.name} size="12" />
                  {c.isOnline && (
                    <span className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-white" />
                  )}
                </div>

                <div className="flex-1">
                  <div className="flex justify-between">
                    <div className="min-w-0">
                    <h3 className="text-sm font-semibold truncate">
                      {c.name}
                    </h3>
                    {c.caseTitle && (
                      <p className="text-xs text-gray-500 truncate">
                        Case: {c.caseTitle}
                      </p>
                  )}
                    </div>
                    <span className="text-xs text-gray-500">
                      {c.lastMessageTime}
                    </span>
                  </div>

                  <p className="text-xs text-gray-600 truncate">
                    {c.lastMessage}
                  </p>
                </div>
              </div>
            </div>
          ))
        )}
        </div>
      </div>

      {/* CHAT WINDOW */}
      <div className="flex-1 flex flex-col">
        {activeConversation ? (
          <>
            <div className="bg-white border-b p-4 flex items-center gap-3">
              <div
                className="cursor-pointer"
                onClick={() => handleViewProfile(activeConversation)}
              >
                <Avatar name={activeConversation.name} size="12" />
              </div>
              <div>
                <h3 className="font-semibold">
                  {activeConversation.name}
                </h3>
                <span className={`text-xs ${activeConversation.isOnline ? "text-green-600 font-medium" : "text-gray-500"}`}>
                  {activeConversation.isOnline ? "Online" : "Offline"}
                </span>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 bg-gray-50">
              {chatHistory.map((m) => (
                <div
                  key={m.id}
                  className={`flex ${m.sender === "me"
                    ? "justify-end"
                    : "justify-start"
                    }`}
                >
                  <div
                    className={`max-w-[60%] rounded-2xl px-4 py-2 ${m.sender === "me"
                      ? "bg-blue-950 text-white"
                      : "bg-white shadow"
                      }`}
                  >
                    <p className="text-sm">{m.text}</p>
                    <p className="text-xs mt-1 opacity-70">
                      {m.timestamp}
                    </p>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            <div className="bg-white border-t p-4 flex gap-3">
              <input
                value={messageInput}
                onChange={(e) => setMessageInput(e.target.value)}
                onKeyDown={(e) =>
                  e.key === "Enter" && handleSendMessage()
                }
                className="flex-1 border rounded-lg px-4 py-2"
                placeholder={
                  isStompConnected
                    ? "Type a message..."
                    : "Connecting..."
                }
              />
              <button
                onClick={handleSendMessage}
                disabled={!isStompConnected}
                className={`px-4 py-2 rounded-lg text-white ${isStompConnected
                  ? "bg-blue-950"
                  : "bg-gray-400 cursor-not-allowed"
                  }`}
              >
                <Send className="w-4 h-4" />
              </button>
            </div>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center gap-3">
            <FaComments className="text-gray-400 text-4xl" />
            <p className="text-gray-600 text-lg font-medium">
              Select a conversation to start chatting
            </p>
          </div>
        )}
      </div>

      {/* PROFILE MODAL */}
      {showProfileModal && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
          onClick={closeProfileModal}
        >
          <div
            className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="flex items-center justify-between p-6 border-b">
              <h2 className="text-2xl font-bold text-blue-950">Profile Details</h2>
              <button
                onClick={closeProfileModal}
                className="text-gray-500 hover:text-gray-700 transition-colors"
              >
                <X size={24} />
              </button>
            </div>

            {/* Modal Content */}
            <div className="p-6">
              {loadingProfile ? (
                <div className="flex items-center justify-center py-12">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-950"></div>
                </div>
              ) : selectedUserProfile ? (
                <div className="space-y-6">
                  {/* Profile Header */}
                  <div className="flex items-center gap-4">
                    <Avatar
                      name={selectedUserProfile.name || selectedUserProfile.fullName || "Unknown"}
                      size="20"
                    />
                    <div>
                      <h3 className="text-xl font-bold text-gray-900">
                        {selectedUserProfile.name || selectedUserProfile.fullName || "Unknown User"}
                      </h3>
                      <p className="text-sm text-gray-600 capitalize">
                        {selectedUserProfile.type || selectedUserProfile.role}
                      </p>
                    </div>
                  </div>

                  {/* Case Information */}
                  {selectedUserProfile.caseTitle && (
                    <div className="bg-blue-50 p-4 rounded-lg">
                      <h4 className="font-semibold text-blue-950 mb-2">Current Case</h4>
                      <p className="text-sm text-gray-700">{selectedUserProfile.caseTitle}</p>
                    </div>
                  )}

                  {/* Contact Information */}
                  <div className="space-y-3">
                    <h4 className="font-semibold text-gray-900 flex items-center gap-2">
                      <Mail size={18} className="text-blue-950" />
                      Contact Information
                    </h4>

                    {selectedUserProfile.email && (
                      <div className="flex items-center gap-3 text-sm">
                        <Mail size={16} className="text-gray-400" />
                        <span className="text-gray-700">{selectedUserProfile.email}</span>
                      </div>
                    )}

                    {selectedUserProfile.phone && (
                      <div className="flex items-center gap-3 text-sm">
                        <Phone size={16} className="text-gray-400" />
                        <span className="text-gray-700">{selectedUserProfile.phone}</span>
                      </div>
                    )}

                    {selectedUserProfile.address && (
                      <div className="flex items-center gap-3 text-sm">
                        <MapPin size={16} className="text-gray-400" />
                        <span className="text-gray-700">{selectedUserProfile.address}</span>
                      </div>
                    )}
                  </div>

                  {/* Lawyer-specific information */}
                  {selectedUserProfile.type === "LAWYER" && (
                    <>
                      {selectedUserProfile.specialization && (
                        <div className="space-y-2">
                          <h4 className="font-semibold text-gray-900 flex items-center gap-2">
                            <Briefcase size={18} className="text-blue-950" />
                            Specialization
                          </h4>
                          <p className="text-sm text-gray-700">{selectedUserProfile.specialization}</p>
                        </div>
                      )}

                      {selectedUserProfile.experience && (
                        <div className="space-y-2">
                          <h4 className="font-semibold text-gray-900 flex items-center gap-2">
                            <Award size={18} className="text-blue-950" />
                            Experience
                          </h4>
                          <p className="text-sm text-gray-700">{selectedUserProfile.experience} years</p>
                        </div>
                      )}

                      {selectedUserProfile.barCouncilNumber && (
                        <div className="space-y-2">
                          <h4 className="font-semibold text-gray-900">Bar Council Number</h4>
                          <p className="text-sm text-gray-700">{selectedUserProfile.barCouncilNumber}</p>
                        </div>
                      )}

                      {selectedUserProfile.bio && (
                        <div className="space-y-2">
                          <h4 className="font-semibold text-gray-900">About</h4>
                          <p className="text-sm text-gray-700">{selectedUserProfile.bio}</p>
                        </div>
                      )}
                    </>
                  )}

                  {/* NGO-specific information */}
                  {selectedUserProfile.type === "NGO" && (
                    <>
                      {selectedUserProfile.organizationName && (
                        <div className="space-y-2">
                          <h4 className="font-semibold text-gray-900 flex items-center gap-2">
                            <Building size={18} className="text-blue-950" />
                            Organization
                          </h4>
                          <p className="text-sm text-gray-700">{selectedUserProfile.organizationName}</p>
                        </div>
                      )}

                      {selectedUserProfile.focusAreas && (
                        <div className="space-y-2">
                          <h4 className="font-semibold text-gray-900 flex items-center gap-2">
                            <Briefcase size={18} className="text-blue-950" />
                            Focus Areas
                          </h4>
                          <p className="text-sm text-gray-700">{selectedUserProfile.focusAreas}</p>
                        </div>
                      )}

                      {selectedUserProfile.registrationNumber && (
                        <div className="space-y-2">
                          <h4 className="font-semibold text-gray-900">Registration Number</h4>
                          <p className="text-sm text-gray-700">{selectedUserProfile.registrationNumber}</p>
                        </div>
                      )}

                      {selectedUserProfile.description && (
                        <div className="space-y-2">
                          <h4 className="font-semibold text-gray-900">Description</h4>
                          <p className="text-sm text-gray-700">{selectedUserProfile.description}</p>
                        </div>
                      )}
                    </>
                  )}

                  {/* Availability */}
                  {selectedUserProfile.availability && (
                    <div className="space-y-2">
                      <h4 className="font-semibold text-gray-900">Availability</h4>
                      <p className="text-sm text-gray-700">{selectedUserProfile.availability}</p>
                    </div>
                  )}
                </div>
              ) : (
                <div className="text-center py-8 text-gray-500">
                  No profile data available
                </div>
              )}
            </div>

            {/* Modal Footer */}
            <div className="flex justify-end gap-3 p-6 border-t bg-gray-50">
              <button
                onClick={closeProfileModal}
                className="px-4 py-2 bg-blue-950 text-white rounded-lg hover:bg-blue-900 transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
