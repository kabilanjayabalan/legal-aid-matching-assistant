import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { LuMoveLeft } from "react-icons/lu";
import { FiHome } from "react-icons/fi";
import logo from "../logo.svg";

export default function NotFound() {
  const navigate = useNavigate();

  useEffect(() => {
    document.title = "404 - Page Not Found | Legal Aid";
  }, []);

  return (
    <div className="h-screen bg-gray-50 flex flex-col items-center justify-center px-4 overflow-hidden">
      <div className="max-w-xl w-full text-center">
        {/* Logo */}
        <div className="flex justify-center mb-4">
          <img src={logo} alt="Legal Aid Logo" className="h-12 w-12" />
        </div>

        {/* 404 Error */}
        <div className="mb-4">
          <h1 className="text-6xl md:text-7xl font-extrabold text-blue-950 mb-2">404</h1>
          <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-2">
            Page Not Found
          </h2>
          <p className="text-base text-gray-600">
            The page you're looking for doesn't exist or has been moved.
          </p>
        </div>

        {/* Illustration */}
        <div className="mb-6">
          <div className="relative inline-block">
            <div className="absolute inset-0 bg-blue-200 rounded-full filter blur-2xl opacity-30 animate-pulse"></div>
            <div className="relative bg-blue-100 rounded-full p-8">
              <svg
                className="w-20 h-20 text-blue-900 mx-auto"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-3 justify-center items-center mb-4">
          <button
            onClick={() => navigate("/")}
            className="px-6 py-2.5 bg-blue-900 text-white rounded-lg font-semibold hover:bg-blue-950 transition-all shadow-md hover:shadow-lg flex items-center gap-2 transform hover:-translate-y-1"
          >
            <FiHome size={18} />
            Go to Homepage
          </button>
          <button
            onClick={() => navigate(-1)}
            className="px-6 py-2.5 border-2 border-blue-900 text-blue-900 rounded-lg font-semibold hover:bg-blue-50 transition-all flex items-center gap-2"
          >
            <LuMoveLeft size={18} />
            Go Back
          </button>
        </div>

        {/* Helpful Links */}
        <div className="pt-4 border-t border-gray-200">
          <p className="text-xs text-gray-500 mb-2">Quick links:</p>
          <div className="flex flex-wrap justify-center gap-3">
            <button
              onClick={() => navigate("/signin")}
              className="text-blue-900 hover:text-blue-950 hover:underline text-xs font-medium"
            >
              Sign In
            </button>
            <button
              onClick={() => navigate("/signup")}
              className="text-blue-900 hover:text-blue-950 hover:underline text-xs font-medium"
            >
              Sign Up
            </button>
            <button
              onClick={() => navigate("/terms")}
              className="text-blue-900 hover:text-blue-950 hover:underline text-xs font-medium"
            >
              Terms
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

