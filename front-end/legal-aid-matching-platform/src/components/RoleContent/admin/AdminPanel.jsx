import { useEffect, useState } from "react";
import { FiSettings, FiActivity } from "react-icons/fi";
import { FaCogs, FaUsers, FaChartBar, FaClipboardList } from "react-icons/fa";
import { LuCloudUpload, LuFileSpreadsheet } from "react-icons/lu";
import UserVerification from "./UserVerification";
import DirectoryIngestion from "./DirectoryIngestion";
import UserManagement from "./UserManagement";
import AppSettings from "./AppSettings";

export default function AdminPanel() {
  const [activeTab, setActiveTab] = useState("User Verification");

  useEffect(() => {
    document.title = "Admin Panel | Legal Aid";
  }, []);

  const tabs = [
    { name: "User Verification", icon: <FaUsers className="text-lg" /> },
    { name: "User Management", icon: <FaUsers className="text-lg" /> },
    { name: "Directory Ingestion", icon: <LuCloudUpload className="text-lg" /> },
    { name: "App Settings", icon: <FaCogs className="text-lg" /> },
  ];

  return (
    <div className="w-full min-h-screen bg-gray-50">

      {/* MOBILE */}
      <div className="block lg:hidden max-w-md mx-auto p-2">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-xl font-semibold">ADMIN PANEL</h1>
          <FiSettings className="text-2xl" />
        </div>

        <div className="flex overflow-x-auto gap-3 pb-3 mb-4 no-scrollbar">
          {tabs.map((tab) => (
            <button
              key={tab.name}
              onClick={() => setActiveTab(tab.name)}
              className={`flex items-center gap-2 px-3 py-2 rounded-lg whitespace-nowrap 
                border transition-colors ${
                activeTab === tab.name
                  ? "bg-blue-950 text-white border-blue-950"
                  : "bg-white text-gray-700 border-gray-300"
              }`}
            >
              {tab.icon}
              <span className="text-sm font-medium">{tab.name}</span>
            </button>
          ))}
        </div>

        {activeTab === "User Verification" && <UserVerification />}
        {activeTab === "User Management" && <UserManagement />}
        {activeTab === "Directory Ingestion" && <DirectoryIngestion />}
          {activeTab === "App Settings" && <AppSettings />}
      </div>

      {/* DESKTOP */}
      <div className="hidden lg:block p-5">
        <h2 className="text-2xl font-semibold text-gray-900">ADMIN PANEL</h2>
        <p className="text-gray-600 mt-1 mb-6">
          Manage platform users, data ingestion, system health, and application settings.
        </p>

        <div className="flex justify-between border-b pb-3 mb-18">
          {tabs.map((tab) => (
            <button
              key={tab.name}
              onClick={() => setActiveTab(tab.name)}
              className={`flex flex-1 justify-center items-center gap-2 text-sm font-medium pb-2 transition-colors 
                focus:outline-none focus:ring-0 ${
                activeTab === tab.name
                  ? "text-blue-950 border-b-2 border-blue-950"
                  : "text-gray-600 hover:text-black"
              }`}
            >
              {tab.icon}
              {tab.name}
            </button>
          ))}
        </div>

        {activeTab === "User Verification" && <UserVerification />}
        {activeTab === "User Management" && <UserManagement />}
        {activeTab === "Directory Ingestion" && <DirectoryIngestion />}
          {activeTab === "App Settings" && <AppSettings />}
      </div>
    </div>
  );
}
