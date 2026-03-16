import React, { useEffect, useRef, useState, useCallback } from "react";
import { FaComments } from "react-icons/fa";
import { Client } from "@stomp/stompjs";
import Avatar from "../../common/Avatar";
import { Send, MessageSquare, Phone, X, Mail, MapPin, Briefcase, Award, Building, Paperclip, Image, Video, File, Download } from "lucide-react";
import AppointmentScheduler from "./AppointmentScheduler";

import {
  getMyUserId,
  getMyChats,
  getChatHistory,
  checkUserOnline,
  getMyMatches,
  getProviderProfile,
  getLawyerProfileById,
  getNgoProfileById,
  uploadChatFile,
  downloadChatFile,
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
  const [showScheduler, setShowScheduler] = useState(false);
  const [selectedChatProfile, setSelectedChatProfile] = useState(null);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [selectedUserProfile, setSelectedUserProfile] = useState(null);
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [uploadingFile, setUploadingFile] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [fileUrls, setFileUrls] = useState({}); // Cache file URLs
  const [loadingFiles, setLoadingFiles] = useState({}); // Track loading state
  const [selectedFile, setSelectedFile] = useState(null); // File selected for preview
  const [filePreviewMessage, setFilePreviewMessage] = useState(""); // Message to send with file

  const stompClientRef = useRef(null);
  const messagesEndRef = useRef(null);
  const conversationsRef = useRef(conversations);
  const fileInputRef = useRef(null);

  useEffect(() => {
    conversationsRef.current = conversations;
  }, [conversations]);

  /* ===================== FILE SIZE VALIDATION ===================== */

  const formatFileSize = (bytes) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + " " + sizes[i];
  };

  const MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024; // 2GB

  const validateFile = (file) => {
    if (file.size > MAX_FILE_SIZE) {
      alert(`File size exceeds maximum limit of 2GB. Your file is ${formatFileSize(file.size)}`);
      return false;
    }
    return true;
  };

  const getFileType = (fileName, fileType) => {
    const extension = fileName.split(".").pop()?.toLowerCase();
    if (["jpg", "jpeg", "png", "gif", "webp"].includes(extension) || fileType?.startsWith("image/")) {
      return "image";
    }
    if (["mp4", "webm", "ogg", "mov", "avi"].includes(extension) || fileType?.startsWith("video/")) {
      return "video";
    }
    return "document";
  };

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
        const matchesResponse = await getMyMatches(0, 100);
        const page = matchesResponse.data || {};
        const matchesData = page.content || [];

        // Filter to only accepted matches (PROVIDER_CONFIRMED) for adding as new conversations
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
            lastMessage: c.lastMessage || "Start Conversation",
            lastMessageTime: formatConversationTime(c.lastMessageAt),
            // Will be populated from match data below
            providerId: null,
            providerType: null,
            matchId: c.matchId,
          });
        });

        // Populate providerId and providerType for ALL existing chats from matchesData
        existingChats.forEach((chat) => {
          const matchInfo = matchesData.find(m => m.matchId === chat.matchId);
          if (matchInfo) {
            const existing = chatMap.get(chat.matchId);
            if (existing) {
              existing.providerId = matchInfo.providerId;
              existing.providerType = matchInfo.providerType;
            }
          }
        });

        // For all accepted matches, fetch the provider name and update the conversation
        const profilePromises = allAcceptedMatches.map(match =>
          getProviderProfile(match.providerId, match.providerType)
            .then(res => ({ ...match, providerName: res.data.name }))
            .catch(() => ({ ...match, providerName: `ID: ${match.providerId}` }))
        );

        const matchesWithNames = await Promise.all(profilePromises);

        matchesWithNames.forEach((match) => {
          const existingChat = chatMap.get(match.matchId) || {};
          chatMap.set(match.matchId, {
            ...existingChat,
            id: match.matchId,
            name: match.providerName, // <-- Override the name
            role: match.providerType === "LAWYER" ? "LAWYER" : "NGO",
            email: existingChat.email,
            caseTitle: existingChat.caseTitle,
            isOnline: existingChat.isOnline || false,
            lastMessage: existingChat.lastMessage || "",
            lastMessageTime: existingChat.lastMessageTime || formatConversationTime(match.createdAt),
            providerId: match.providerId, // Store providerId for scheduling
            providerType: match.providerType, // Store providerType for scheduling
            matchId: match.matchId, // Store matchId for scheduling
          });
        });

        // Handle navigation state - add new chat if provided
        if (location.state?.newChat) {
          const newChat = location.state.newChat;
          if (!chatMap.has(newChat.id)) {
            // Try to find match info for this chat
            const matchInfo = matchesData.find(m => m.matchId === newChat.id);

            chatMap.set(newChat.id, {
              id: newChat.id,
              name: newChat.name,
              role: newChat.role,
              email: newChat.email,
              caseTitle: newChat.caseTitle,
              isOnline: false,
              lastMessage: "",
              lastMessageTime: "",
              providerId: matchInfo?.providerId || newChat.providerId,
              providerType: matchInfo?.providerType || newChat.providerType,
              matchId: newChat.id,
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
  const openScheduler = (conversation) => {
    // Ensure we have the required fields for AppointmentScheduler
    if (!conversation.providerId || !conversation.providerType) {
      console.error("Provider information missing in conversation:", conversation);
      alert("Unable to schedule appointment. Provider information is missing.");
      return;
    }

    setSelectedChatProfile({
      id: conversation.providerId,
      matchId: conversation.matchId || conversation.id,
      name: conversation.name,
      type: conversation.providerType,
    });
    setShowScheduler(true);
  };
  const openProfile = async (conversation) => {
    try {
      const res = await getProviderProfile(
        conversation.providerId,
        conversation.providerType
      );

      setSelectedChatProfile({
        ...res.data,
        providerType: conversation.providerType,
        matchId: conversation.matchId,
      });
    } catch (err) {
      console.error("Failed to load profile", err);
    }
  };

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
        if (update && update.isOnline !== c.isOnline) {
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
        res.data.map((m) => {
          const fileAttachment = m.fileAttachments && m.fileAttachments.length > 0 ? m.fileAttachments[0] : null;
          const fileType = fileAttachment ? getFileType(fileAttachment.fileName, fileAttachment.fileType) : null;
          
          return {
            id: m.createdAt,
            text: m.message,
            sender: m.senderId === myUserId ? "me" : "other",
            senderId: m.senderId,
            timestamp: formatTime(m.createdAt),
            fileAttachment: fileAttachment,
            fileType: fileType,
          };
        })
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

          const fileAttachment = chat.fileAttachments && chat.fileAttachments.length > 0 ? chat.fileAttachments[0] : null;
          const fileType = fileAttachment ? getFileType(fileAttachment.fileName, fileAttachment.fileType) : null;

          setChatHistory((prev) => [
            ...prev,
            {
              id: chat.createdAt,
              text: chat.message,
              sender: "other",
              senderId: chat.senderId,
              timestamp: formatTime(chat.createdAt),
              fileAttachment: fileAttachment,
              fileType: fileType,
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

  /* ===================== FILE UPLOAD ===================== */

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!validateFile(file)) {
      e.target.value = "";
      return;
    }

    if (!isStompConnected || !activeChat) {
      alert("Please wait for connection to be established");
      e.target.value = "";
      return;
    }

    // Show preview modal (like WhatsApp)
    setSelectedFile(file);
    setFilePreviewMessage(messageInput.trim() || "");
    e.target.value = "";
  };

  const handleCancelFilePreview = () => {
    // Clean up object URL if it was created
    if (selectedFile) {
      const objectUrl = URL.createObjectURL(selectedFile);
      URL.revokeObjectURL(objectUrl);
    }
    setSelectedFile(null);
    setFilePreviewMessage("");
  };

  const handleConfirmFileSend = async () => {
    if (!selectedFile || !isStompConnected || !activeChat) return;

    setUploadingFile(true);
    setUploadProgress(0);

    try {
      // Upload file
      const response = await uploadChatFile(activeChat, selectedFile, filePreviewMessage.trim() || "");
      const savedMessage = response.data;

      // Update chat history optimistically
      const fileType = getFileType(savedMessage.fileAttachments?.[0]?.fileName || selectedFile.name, savedMessage.fileAttachments?.[0]?.fileType || selectedFile.type);
      
      setChatHistory((prev) => [
        ...prev,
        {
          id: savedMessage.createdAt,
          text: savedMessage.message,
          sender: "me",
          senderId: savedMessage.senderId,
          timestamp: formatTime(savedMessage.createdAt),
          fileAttachment: savedMessage.fileAttachments?.[0],
          fileType: fileType,
        },
      ]);

      // Update sidebar
      setConversations((prev) =>
        prev.map((c) =>
          c.id === activeChat
            ? {
              ...c,
              lastMessage: savedMessage.message,
              lastMessageTime: formatConversationTime(savedMessage.createdAt),
            }
            : c
        )
      );

      setMessageInput("");
      // Clean up object URL
      if (selectedFile) {
        const objectUrl = URL.createObjectURL(selectedFile);
        URL.revokeObjectURL(objectUrl);
      }
      setSelectedFile(null);
      setFilePreviewMessage("");
    } catch (error) {
      console.error("File upload failed:", error);
      alert("Failed to upload file. Please try again.");
    } finally {
      setUploadingFile(false);
      setUploadProgress(0);
    }
  };

  const getFileUrl = async (fileId) => {
    // Check cache first
    if (fileUrls[fileId]) {
      return fileUrls[fileId];
    }

    // Set loading state
    setLoadingFiles((prev) => ({ ...prev, [fileId]: true }));

    try {
      const response = await downloadChatFile(fileId);
      const contentType =
        response?.headers?.["content-type"] || "application/octet-stream";
      const url = window.URL.createObjectURL(
        new Blob([response.data], { type: contentType })
      );
      setFileUrls((prev) => ({ ...prev, [fileId]: url }));
      setLoadingFiles((prev) => {
        const newState = { ...prev };
        delete newState[fileId];
        return newState;
      });
      return url;
    } catch (error) {
      console.error("Failed to load file:", error);
      setLoadingFiles((prev) => {
        const newState = { ...prev };
        delete newState[fileId];
        return newState;
      });
      return null;
    }
  };

  // Component to display file with loading state
  const FileDisplay = ({ fileAttachment, fileType, sender }) => {
    const [url, setUrl] = React.useState(null);
    const [loading, setLoading] = React.useState(true);

    React.useEffect(() => {
      if (fileAttachment && (fileType === "image" || fileType === "video")) {
        getFileUrl(fileAttachment.id).then((fileUrl) => {
          setUrl(fileUrl);
          setLoading(false);
        });
      } else {
        setLoading(false);
      }
    }, [fileAttachment?.id, fileType]);

    if (loading) {
      return (
        <div className="flex items-center justify-center p-4">
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-950"></div>
        </div>
      );
    }

    if (fileType === "image" && url) {
      return (
        <div className="rounded-lg overflow-hidden max-w-full">
          <img
            src={url}
            alt={fileAttachment.fileName}
            className="max-w-full max-h-64 object-contain cursor-pointer"
            onClick={() => window.open(url, "_blank")}
          />
        </div>
      );
    }

    if (fileType === "video" && url) {
      return (
        <div className="rounded-lg overflow-hidden max-w-full relative">
          <video
            src={url}
            controls
            className="max-w-full max-h-64"
          >
            Your browser does not support the video tag.
          </video>
        </div>
      );
    }

    if (fileType === "document") {
      return (
        <div
          className={`flex items-center gap-2 p-2 rounded-lg cursor-pointer hover:opacity-80 ${sender === "me"
            ? "bg-blue-900"
            : "bg-gray-100"
            }`}
          onClick={() => handleDownloadFile(fileAttachment.id, fileAttachment.fileName)}
        >
          <File className="w-5 h-5" />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium truncate">{fileAttachment.fileName}</p>
            <p className="text-xs opacity-70">{formatFileSize(fileAttachment.fileSize)}</p>
          </div>
          <Download className="w-4 h-4" />
        </div>
      );
    }

    return null;
  };

  const handleDownloadFile = async (fileId, fileName) => {
    try {
      const url = await getFileUrl(fileId);
      if (!url) {
        alert("Failed to download file. Please try again.");
        return;
      }
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error("File download failed:", error);
      alert("Failed to download file. Please try again.");
    }
  };

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
      let profileData;

      // For citizens viewing lawyer/NGO profiles
      if (conversation.providerId && conversation.providerType) {
        if (conversation.providerType === "LAWYER") {
          const response = await getLawyerProfileById(conversation.providerId);
          profileData = {
            ...response.data,
            type: "LAWYER",
            email: conversation.email,
            caseTitle: conversation.caseTitle,
          };
        } else if (conversation.providerType === "NGO") {
          const response = await getNgoProfileById(conversation.providerId);
          profileData = {
            ...response.data,
            type: "NGO",
            email: conversation.email,
            caseTitle: conversation.caseTitle,
          };
        }
      } else {
        // Fallback to basic conversation data
        profileData = {
          name: conversation.name,
          email: conversation.email,
          role: conversation.role,
          caseTitle: conversation.caseTitle,
          type: conversation.role,
        };
      }

      setSelectedUserProfile(profileData);
    } catch (error) {
      console.error("Failed to load profile:", error);
      // Set basic profile data even if fetch fails
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
        <div className="p-3 border-b">
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
          <div className="bg-white border-b p-3 flex items-center justify-between">
            <div
              className="flex items-center gap-3 cursor-pointer"
              onClick={() => handleViewProfile(activeConversation)}
            >
              <Avatar name={activeConversation.name} size="12" />
              <div>
                <h3 className="font-semibold">
                  {activeConversation.name}
                </h3>
                <span className={`text-xs ${activeConversation.isOnline ? "text-green-600 font-medium" : "text-gray-500"}`}>
                  {activeConversation.isOnline ? "Online" : "Offline"}
                </span>
              </div>
            </div>
            {/* Right: Actions */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => openScheduler(activeConversation)}
                title="Schedule Call"
                className="p-2 rounded-full hover:bg-gray-100 text-gray-600 hover:text-blue-950"
              >
                <Phone size={18} />
              </button>
            </div>
          </div>

            <div className="flex-1 overflow-y-auto p-4 bg-gray-50">
              {chatHistory.map((m) => (
                <div
                  key={m.id}
                  className={`flex mb-2 ${m.sender === "me"
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
                    {/* File Attachment Display */}
                    {m.fileAttachment && (
                      <div className="mb-2">
                        <FileDisplay
                          fileAttachment={m.fileAttachment}
                          fileType={m.fileType}
                          sender={m.sender}
                          senderId={m.senderId}
                        />
                      </div>
                    )}
                    {/* Message Text */}
                    {m.text && (
                      <p className="text-sm">{m.text}</p>
                    )}
                    <p className="text-xs mt-1 opacity-70">
                      {m.timestamp}
                    </p>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            <div className="bg-white border-t p-3 flex gap-3 items-center">
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileSelect}
                className="hidden"
                accept="image/*,video/*,.pdf,.doc,.docx,.txt,.xls,.xlsx,.ppt,.pptx"
              />
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={!isStompConnected || uploadingFile}
                className={`p-2 rounded-lg ${isStompConnected && !uploadingFile
                  ? "text-blue-950 hover:bg-gray-100"
                  : "text-gray-400 cursor-not-allowed"
                  }`}
                title="Attach file"
              >
                <Paperclip className="w-5 h-5" />
              </button>
              {uploadingFile && (
                <div className="flex items-center gap-2 text-sm text-gray-500">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-950"></div>
                  <span>Uploading...</span>
                </div>
              )}
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
                disabled={uploadingFile}
              />
              <button
                onClick={handleSendMessage}
                disabled={!isStompConnected || uploadingFile || !messageInput.trim()}
                className={`px-4 py-2 rounded-lg text-white ${isStompConnected && !uploadingFile && messageInput.trim()
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
      {showScheduler && selectedChatProfile && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden relative">

          {/* Close button */}
          <button
            onClick={() => setShowScheduler(false)}
            className="absolute top-4 right-4 z-10 text-gray-500 hover:text-gray-800 text-2xl leading-none bg-white rounded-full p-2 hover:bg-gray-100"
          >
            ×
          </button>

          {/* SAME scroll pattern as matches */}
          <div className="max-h-[90vh] overflow-y-auto p-6">
            <AppointmentScheduler
            lawyer={{
              id: selectedChatProfile.id,
              matchId: selectedChatProfile.matchId,
              name: selectedChatProfile.name,
              type: selectedChatProfile.type,
            }}
            isModal={true}
            onClose={() => setShowScheduler(false)}
            />
          </div>

        </div>
      </div>
    )}

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

      {/* FILE PREVIEW MODAL (WhatsApp-like) */}
      {selectedFile && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
            {/* Modal Header */}
            <div className="flex items-center justify-between p-4 border-b">
              <h3 className="text-lg font-semibold text-gray-900">Send File</h3>
              <button
                onClick={handleCancelFilePreview}
                className="text-gray-500 hover:text-gray-700 transition-colors"
              >
                <X size={24} />
              </button>
            </div>

            {/* File Preview */}
            <div className="p-4">
              {getFileType(selectedFile.name, selectedFile.type) === "image" && (
                <div className="mb-4 rounded-lg overflow-hidden">
                  <img
                    src={URL.createObjectURL(selectedFile)}
                    alt={selectedFile.name}
                    className="max-w-full max-h-64 object-contain mx-auto"
                  />
                </div>
              )}
              {getFileType(selectedFile.name, selectedFile.type) === "video" && (
                <div className="mb-4 rounded-lg overflow-hidden">
                  <video
                    src={URL.createObjectURL(selectedFile)}
                    controls
                    className="max-w-full max-h-64 mx-auto"
                  >
                    Your browser does not support the video tag.
                  </video>
                </div>
              )}
              {getFileType(selectedFile.name, selectedFile.type) === "document" && (
                <div className="mb-4 flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                  <File className="w-8 h-8 text-blue-950" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">{selectedFile.name}</p>
                    <p className="text-xs text-gray-500">{formatFileSize(selectedFile.size)}</p>
                  </div>
                </div>
              )}

              {/* File Info */}
              <div className="mb-4">
                <p className="text-sm text-gray-600 mb-1">File: {selectedFile.name}</p>
                <p className="text-xs text-gray-500">Size: {formatFileSize(selectedFile.size)}</p>
              </div>

              {/* Message Input */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Add a caption (optional)
                </label>
                <textarea
                  value={filePreviewMessage}
                  onChange={(e) => setFilePreviewMessage(e.target.value)}
                  className="w-full border rounded-lg px-3 py-2 text-sm resize-none"
                  rows={3}
                  placeholder="Type a message..."
                />
              </div>
            </div>

            {/* Modal Footer */}
            <div className="flex justify-end gap-3 p-4 border-t bg-gray-50">
              <button
                onClick={handleCancelFilePreview}
                className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmFileSend}
                disabled={uploadingFile}
                className={`px-4 py-2 rounded-lg text-white ${uploadingFile
                  ? "bg-gray-400 cursor-not-allowed"
                  : "bg-blue-950 hover:bg-blue-900"
                  } transition-colors`}
              >
                {uploadingFile ? (
                  <span className="flex items-center gap-2">
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                    Sending...
                  </span>
                ) : (
                  "Send"
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
