import React, { useState, useRef, useEffect } from "react";
import { FaPaperPlane, FaRobot, FaUser } from "react-icons/fa";
import { Sparkles } from "lucide-react";
import api from "../../services/api";

export default function AiChatPage() {
  const [messages, setMessages] = useState([
    {
      sender: "bot",
      text: "Hello! I am your Legal Assistant AI. How can I help you with your legal questions today?",
    },
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const messagesEndRef = useRef(null);

  // Auto scroll to bottom
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const sendMessage = async () => {
    if (!input.trim()) return;

    const userMessage = { sender: "user", text: input };

    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setLoading(true);

    try {
      const res = await api.post("/api/ai/chat", { question: userMessage.text });
      const data = res.data;

      const botMessage = {
        sender: "bot",
        text: data.answer || "Sorry, I received an empty response.",
      };

      setMessages((prev) => [...prev, botMessage]);
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        { sender: "bot", text: "Error connecting to AI server. Please try again later." },
      ]);
    }

    setLoading(false);
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter") {
      sendMessage();
    }
  };

  return (
    <div className="flex flex-col h-[calc(100vh-140px)] bg-white rounded-xl shadow-lg border border-gray-100 overflow-hidden m-6">
      {/* Header */}
      <div className="bg-blue-950 text-white p-4 flex items-center gap-3">
        <Sparkles className="text-yellow-400" size={24} />
        <div>
          <h2 className="text-lg font-semibold">Legal Assistant AI</h2>
          <p className="text-sm text-blue-200">Powered by advanced legal models</p>
        </div>
      </div>

      {/* Chat Area */}
      <div className="flex-1 overflow-y-auto p-6 space-y-4 bg-gray-50">
        {messages.map((msg, index) => (
          <div
            key={index}
            className={`flex ${msg.sender === "user" ? "justify-end" : "justify-start"}`}
          >
            <div
              className={`flex gap-3 max-w-[80%] ${
                msg.sender === "user" ? "flex-row-reverse" : "flex-row"
              }`}
            >
              {/* Avatar */}
              <div
                className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
                  msg.sender === "user" ? "bg-blue-600 text-white" : "bg-blue-100 text-blue-800"
                }`}
              >
                {msg.sender === "user" ? <FaUser /> : <FaRobot size={20} />}
              </div>

              {/* Message Bubble */}
              <div
                className={`px-4 py-3 rounded-2xl shadow-sm text-sm md:text-base ${
                  msg.sender === "user"
                    ? "bg-blue-600 text-white rounded-tr-none"
                    : "bg-white text-gray-800 border border-gray-100 rounded-tl-none"
                }`}
                style={{ whiteSpace: "pre-wrap" }}
              >
                {msg.text}
              </div>
            </div>
          </div>
        ))}

        {loading && (
          <div className="flex justify-start">
            <div className="flex gap-3 max-w-[80%]">
              <div className="w-10 h-10 rounded-full bg-blue-100 text-blue-800 flex items-center justify-center flex-shrink-0">
                <FaRobot size={20} />
              </div>
              <div className="px-4 py-3 rounded-2xl shadow-sm bg-white border border-gray-100 rounded-tl-none flex items-center gap-2">
                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></span>
                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: "0.2s" }}></span>
                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: "0.4s" }}></span>
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="p-4 bg-white border-t border-gray-200">
        <div className="flex gap-3 items-center max-w-4xl mx-auto">
          <input
            type="text"
            className="flex-1 bg-gray-100 border border-transparent rounded-full px-6 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
            placeholder="Ask your legal question..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyPress}
            disabled={loading}
          />
          <button
            onClick={sendMessage}
            disabled={loading || !input.trim()}
            className="w-12 h-12 rounded-full bg-blue-600 text-white flex items-center justify-center hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0"
          >
            <FaPaperPlane />
          </button>
        </div>
        <p className="text-center text-xs text-gray-400 mt-3">
          AI provides general legal information only and cannot replace professional legal counsel.
        </p>
      </div>
    </div>
  );
}
