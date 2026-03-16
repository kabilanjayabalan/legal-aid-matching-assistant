import { useState, useEffect } from "react";
import { FiAlertTriangle, FiX } from "react-icons/fi";
import  api  from "../../../services/api";

export default function AppSettings() {
  const [emailNotifications, setEmailNotifications] = useState(true);
  const [maintenanceMode, setMaintenanceMode] = useState(false);
  const [maintenanceStart, setMaintenanceStart] = useState("");
  const [maintenanceEnd, setMaintenanceEnd] = useState("");

const toLocalInput = (value) => {
  if (!value) return "";
  return value.slice(0, 16);
};

useEffect(() => {
  fetchMaintenance();
}, []);

const fetchMaintenance = async () => {
  try {
    const res = await api.get("/system/maintenance");
    setMaintenanceMode(res.data.enabled);
    setMaintenanceStart(toLocalInput(res.data.start));
    setMaintenanceEnd(toLocalInput(res.data.end));
  } catch (e) {
    console.error("Failed to load maintenance status");
  }
};

const saveMaintenanceSettings = async () => {
  if (maintenanceMode && (!maintenanceStart || !maintenanceEnd)) {
    alert("Please select both start and end time");
    return;
  }

  try {
    await api.post("/admin/system/maintenance", {
      enabled: maintenanceMode,
      start: maintenanceMode ? maintenanceStart : null,
      end: maintenanceMode ? maintenanceEnd : null,
      message: "Scheduled maintenance in progress"
    });

    alert("Maintenance settings updated successfully");
    await fetchMaintenance();
  } catch (e) {
    console.error(e.response?.data || e.message);
    alert("Failed to update maintenance settings");
  }
};
const handleMaintenanceModeChange = async (e) => {
  const isChecked = e.target.checked;
  setMaintenanceMode(isChecked);

  // 🔴 Admin turning OFF manually
  if (!isChecked) {
    setMaintenanceStart("");
    setMaintenanceEnd("");

    try {
      await api.post("/admin/system/maintenance", {
        enabled: false,
        start: null,
        end: null,
        message: null
      });

      await fetchMaintenance();
    } catch (err) {
      alert("Failed to disable maintenance");
      setMaintenanceMode(true); // rollback UI
    }
  }
};

  const formatMaintenanceTime = (dateTime) => {
    if (!dateTime) return "";
    const date = new Date(dateTime);
    return date.toLocaleDateString("en-US", {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleBackupDatabase = () => {
    // Simulate database backup
    alert("Database backup initiated successfully!");
  };

  const handleClearCache = () => {
    // Simulate cache clearing
    if (window.confirm("Are you sure you want to clear the cache? This action cannot be undone.")) {
      alert("Cache cleared successfully!");
    }
  };

  return (
    <>
      <div className="p-6 bg-white rounded-lg shadow-sm">
        <h3 className="text-lg font-semibold mb-6 text-gray-900">Application Settings</h3>

        <div className="space-y-8">
          {/* System Preferences Section */}
          <div className="p-6 border border-gray-200 rounded-lg bg-gray-50">
            <h4 className="font-semibold mb-4 text-gray-800 flex items-center gap-2">
              <span className="w-2 h-2 bg-blue-600 rounded-full"></span>
              System Preferences
            </h4>

            <div className="space-y-4">
              {/* Email Notifications */}
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <label className="text-sm font-medium text-gray-700">Email Notifications</label>
                  <p className="text-xs text-gray-500 mt-1">Receive notifications for important system events</p>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    className="sr-only peer"
                    checked={emailNotifications}
                    onChange={(e) => setEmailNotifications(e.target.checked)}
                  />
                  <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                </label>
              </div>

              {/* Maintenance Mode */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <label className="text-sm font-medium text-gray-700">Maintenance Mode</label>
                    <p className="text-xs text-gray-500 mt-1">Schedule system maintenance with custom timeline</p>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      className="sr-only peer"
                      checked={maintenanceMode}
                      onChange={handleMaintenanceModeChange}
                    />
                    <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-red-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-red-600"></div>
                  </label>
                </div>

                {/* Maintenance Timeline Inputs */}
                {maintenanceMode && (
                  <div className="ml-4 p-4 bg-red-50 border border-red-200 rounded-lg animate-fadeIn">
                    <h5 className="text-sm font-medium text-red-800 mb-3 flex items-center gap-2">
                      <FiAlertTriangle className="w-4 h-4" />
                      Maintenance Schedule
                    </h5>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-medium text-red-700 mb-2">
                          Start Date & Time
                        </label>
                        <input
                          type="datetime-local"
                          value={maintenanceStart}
                          onChange={(e) => setMaintenanceStart(e.target.value)}
                          className="w-full px-3 py-2 text-sm border border-red-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-red-500"
                          min={new Date().toISOString().slice(0, 16)}
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-medium text-red-700 mb-2">
                          End Date & Time
                        </label>
                        <input
                          type="datetime-local"
                          value={maintenanceEnd}
                          onChange={(e) => setMaintenanceEnd(e.target.value)}
                          className="w-full px-3 py-2 text-sm border border-red-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-red-500"
                          min={maintenanceStart || new Date().toISOString().slice(0, 16)}
                        />
                      </div>
                    </div>
                       <button
                        onClick={saveMaintenanceSettings}
                        disabled={maintenanceMode && (!maintenanceStart || !maintenanceEnd)}
                        className={`mt-4 px-4 py-2 text-sm font-semibold rounded-lg ${
                          maintenanceMode && (!maintenanceStart || !maintenanceEnd)
                            ? "bg-gray-400 cursor-not-allowed"
                            : "bg-red-600 hover:bg-red-700 text-white"
                        }`}
                        >
                          Save Maintenance Settings
                       </button>


                    {maintenanceStart && maintenanceEnd && (
                      <div className="mt-3 p-2 bg-red-100 rounded border border-red-200">
                        <p className="text-xs text-red-800">
                          <strong>Preview:</strong> Maintenance scheduled from {formatMaintenanceTime(maintenanceStart)} to {formatMaintenanceTime(maintenanceEnd)}
                        </p>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Database Settings Section */}
          <div className="p-6 border border-gray-200 rounded-lg bg-gray-50">
            <h4 className="font-semibold mb-4 text-gray-800 flex items-center gap-2">
              <span className="w-2 h-2 bg-green-600 rounded-full"></span>
              Database Settings
            </h4>

            <div className="space-y-4">
              <div className="flex flex-wrap gap-3">
                <button
                  onClick={handleBackupDatabase}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
                >
                  Backup Database
                </button>
                <button
                  onClick={handleClearCache}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-sm font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
                >
                  Clear Cache
                </button>
              </div>

              <div className="text-xs text-gray-600 bg-blue-50 p-3 rounded-lg border border-blue-200">
                <p><strong>Note:</strong> Database backups are performed automatically every 24 hours. Manual backup creates an additional restore point.</p>
              </div>
            </div>
          </div>

          {/* System Information Section */}
          <div className="p-6 border border-gray-200 rounded-lg bg-gray-50">
            <h4 className="font-semibold mb-4 text-gray-800 flex items-center gap-2">
              <span className="w-2 h-2 bg-purple-600 rounded-full"></span>
              System Information
            </h4>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-gray-600">Application Version:</span>
                  <span className="font-medium">v1.2.3</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Database Version:</span>
                  <span className="font-medium">PostgreSQL 14.2</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Last Backup:</span>
                  <span className="font-medium text-green-600">2 hours ago</span>
                </div>
              </div>
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-gray-600">Server Uptime:</span>
                  <span className="font-medium">15 days, 8 hours</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Active Sessions:</span>
                  <span className="font-medium">127</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">System Status:</span>
                  <span className="font-medium text-green-600">Operational</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Custom CSS for marquee animation */}
      <style jsx>{`
        @keyframes marquee {
          0% { transform: translateX(100%); }
          100% { transform: translateX(-100%); }
        }
        .animate-marquee {
          animation: marquee 20s linear infinite;
        }
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(-10px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .animate-fadeIn {
          animation: fadeIn 0.3s ease-out;
        }
      `}</style>
    </>
  );
}
