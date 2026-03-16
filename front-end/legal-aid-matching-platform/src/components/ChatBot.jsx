import { useState, useRef, useEffect } from "react";
import chatbotIcon from "../Images/Ai chat bot.png";

export default function ChatBot() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const messagesEndRef = useRef(null);
  const chatRef = useRef(null);

  // Auto scroll
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Close chat when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (chatRef.current && !chatRef.current.contains(event.target)) {
        setOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const sendMessage = async () => {
    if (!input.trim()) return;

    const userMessage = { sender: "user", text: input };

    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setLoading(true);

    try {
      const res = await fetch("http://localhost:8000/chat", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ question: userMessage.text }),
      });

      const data = await res.json();

      const botMessage = {
        sender: "bot",
        text: data.answer,
      };

      setMessages((prev) => [...prev, botMessage]);
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        { sender: "bot", text: "Error connecting to AI server." },
      ]);
    }

    setLoading(false);
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter") sendMessage();
  };

  return (
    <>
      {/* Chat Icon */}
      <button
        onClick={() => setOpen(!open)}
        className="fixed bottom-6 right-6 w-16 h-16 rounded-full shadow-lg overflow-hidden"
      >
        <img
          src={chatbotIcon}
          alt="AI Chatbot"
          className="w-full h-full object-cover"
        />
      </button>

      {/* Chat Window */}
      {open && (
        <div
          ref={chatRef}
          className="fixed bottom-24 right-6 w-80 h-[450px] bg-white shadow-xl rounded-xl flex flex-col border"
        >
          {/* Header */}
          <div className="bg-blue-600 text-white p-3 rounded-t-xl font-semibold">
            Legal Assistant AI
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-3 space-y-2">
            {messages.map((msg, index) => (
              <div
                key={index}
                className={`max-w-[75%] px-3 py-2 rounded-lg text-sm ${
                  msg.sender === "user"
                    ? "ml-auto bg-blue-600 text-white"
                    : "bg-gray-200 text-gray-800"
                }`}
              >
                {msg.text}
              </div>
            ))}

            {loading && (
              <div className="bg-gray-200 text-gray-700 px-3 py-2 rounded-lg w-fit">
                Typing...
              </div>
            )}

            <div ref={messagesEndRef}></div>
          </div>

          {/* Input */}
          <div className="p-2 border-t flex gap-2">
            <input
              type="text"
              className="flex-1 border rounded-lg px-3 py-2 text-sm focus:outline-none"
              placeholder="Ask your legal question..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyPress}
            />

            <button
              onClick={sendMessage}
              className="bg-blue-600 text-white px-4 rounded-lg hover:bg-blue-700"
            >
              Send
            </button>
          </div>

          {/* Disclaimer */}
          <div className="text-xs text-gray-500 px-3 pb-2">
            AI provides general legal information only.
          </div>
        </div>
      )}
    </>
  );
}