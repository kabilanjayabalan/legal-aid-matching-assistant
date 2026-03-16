import React from "react";

export default function CustomAlert({ message, onClose }) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">

      <div className="bg-white rounded-xl shadow-xl p-6 w-[90%] max-w-sm relative animate-fadeIn">

        {/* Close (X) Button */}
        <button
          className="absolute top-3 right-3 text-gray-500 text-xl hover:text-gray-700"
          onClick={onClose}
        >
          ×
        </button>

        {/* Message */}
        <p className="text-gray-800  text-md font-medium px-4">
          {message}
        </p>

        {/* OK Button — small and right aligned */}
        <div className="mt-6 flex justify-end">
          <button
            className="px-4 py-1 bg-blue-950 text-white rounded-md text-sm hover:bg-gray-900 transition"
            onClick={onClose}
          >
            OK
          </button>
        </div>

      </div>
    </div>
  );
}
