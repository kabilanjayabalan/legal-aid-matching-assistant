import { useEffect, useState } from "react";
import { FiAlertTriangle, FiX } from "react-icons/fi";
import api from "../../services/api";

export default function MaintenanceBanner() {
  const [maintenance, setMaintenance] = useState(null);
  const [dismissed, setDismissed] = useState(false);

  // ✅ Centralized time-aware check
  const isMaintenanceActive = (m) => {
    if (!m?.enabled) return false;

    const now = new Date();
    const start = m.start ? new Date(m.start) : null;
    const end = m.end ? new Date(m.end) : null;

    if (start && now < start) return false;
    if (end && now > end) return false;

    return true;
  };

  // ✅ Fetch maintenance status
  useEffect(() => {
    let interval;

    const fetchMaintenance = async () => {
      try {
        const res = await api.get("/system/maintenance");
        setMaintenance(res.data);
      } catch {
        // silent fail — banner must NEVER break the app
      }
    };

    fetchMaintenance();

    // 🔁 Auto-refresh every 30s (handles time expiry)
    interval = setInterval(fetchMaintenance, 30000);

    return () => clearInterval(interval);
  }, []);

  // ⛔ Hide completely if inactive or dismissed
  if (!isMaintenanceActive(maintenance) || dismissed) return null;

  return (
    <div
      className="sticky top-0 left-0 right-0 z-[9999]
                 bg-gradient-to-r from-red-600 to-orange-600
                 text-white shadow-md"
    >
      <div className="flex items-center justify-between px-4 py-2">
        <div className="flex items-center gap-2 overflow-hidden">
          <FiAlertTriangle className="text-yellow-300 flex-shrink-0" />

          <span className="whitespace-nowrap animate-marquee text-sm font-medium">
            ⚠️ Scheduled Maintenance:
            {" "}
            {maintenance.start
              ? new Date(maintenance.start).toLocaleString()
              : "Soon"}
            {" "}→{" "}
            {maintenance.end
              ? new Date(maintenance.end).toLocaleString()
              : "Until further notice"}
            {maintenance.message && ` — ${maintenance.message}`}
          </span>
        </div>

        <button
          onClick={() => setDismissed(true)}
          className="ml-4 p-1 rounded hover:bg-red-700 transition"
          title="Dismiss"
        >
          <FiX />
        </button>
      </div>

      {/* ✅ Animation */}
      <style>{`
        @keyframes marquee {
          0% { transform: translateX(100%); }
          100% { transform: translateX(-100%); }
        }
        .animate-marquee {
          animation: marquee 18s linear infinite;
        }
      `}</style>
    </div>
  );
}
