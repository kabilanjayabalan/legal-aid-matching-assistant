import { useEffect, useState } from "react";
import { MdDelete } from "react-icons/md";

export default function SystemLogs() {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Filter states
  const [level, setLevel] = useState("");
  const [logger, setLogger] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // UI-only state (professional toggle)
  const [showFilters, setShowFilters] = useState(false);
  const [showCleanup, setShowCleanup] = useState(false);
  const [cleanupDays, setCleanupDays] = useState(30);
  const [cleanupMessage, setCleanupMessage] = useState("");

  const token = sessionStorage.getItem("accessToken");

  // Helper function to get color classes for log levels
  const getLogLevelStyles = (logLevel) => {
    switch (logLevel?.toUpperCase()) {
      case 'ERROR':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'WARN':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'INFO':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'DEBUG':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const fetchLogs = async (pageNo) => {
    const params = new URLSearchParams({
      page: pageNo,
      sortBy: "logTimestamp",
      sortDir: "desc",
    });

    if (level) params.append("level", level);
    if (logger) params.append("logger", logger);
    if (startDate) params.append("startDate", startDate);
    if (endDate) params.append("endDate", endDate);

    const res = await fetch(
      `http://localhost:8080/admin/logs?${params.toString()}`,
      {
        headers: { Authorization: `Bearer ${token}` },
      }
    );

    const data = await res.json();
    setLogs(data.content);
    setTotalPages(data.totalPages);
    setPage(data.number);
  };

  const handleCleanup = async () => {
    try {
      const res = await fetch(
        `http://localhost:8080/admin/logs/cleanup?days=${cleanupDays}`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      
      if (res.ok) {
        const data = await res.json();
        setCleanupMessage(data.message);
        fetchLogs(0); // Refresh logs
        setTimeout(() => setCleanupMessage(""), 3000);
      } else {
        setCleanupMessage("Failed to cleanup logs");
      }
    } catch (error) {
      setCleanupMessage("Error connecting to server");
    }
  };

  useEffect(() => {
    fetchLogs(0);
  }, []);

  const handleFilter = () => fetchLogs(0);

  const clearFilters = () => {
    setLevel("");
    setLogger("");
    setStartDate("");
    setEndDate("");
    fetchLogs(0);
  };

  return (
    <div className="p-2">
      {/* ================= HEADER ================= */}
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-bold">System Logs</h2>

        <div className="flex gap-2">
          <button
            onClick={() => setShowCleanup((prev) => !prev)}
            className="px-4 py-2 text-sm border rounded-lg bg-red-700 text-white flex items-center hover:bg-red-800 border-red-600"
          >
            {showCleanup ? ("Hide Cleanup"
            ) : (
                <> <MdDelete className="w-4 h-4"  /> Cleanup Logs</>)}
          </button>
          <button
            onClick={() => setShowFilters((prev) => !prev)}
            className="px-4 py-2 text-sm border rounded-lg bg-white hover:bg-gray-50"
          >
            {showFilters ? "Hide Filters" : "Show Filters"}
          </button>
        </div>
      </div>

      {/* ================= CLEANUP PANEL ================= */}
      {showCleanup && (
        <div className="bg-red-50 border border-red-800 rounded-lg p-4 mb-4 animate-fadeIn">
          <h3 className="text-sm font-bold text-white mb-2">Manual Log Cleanup</h3>
          <div className="flex flex-col sm:flex-row items-end gap-3">
            <div className="w-full sm:w-auto">
              <label className="block text-xs font-medium text-red-800 mb-1">Delete logs older than (days)</label>
              <select
                value={cleanupDays}
                onChange={(e) => setCleanupDays(e.target.value)}
                className="w-full border border-red-300 rounded px-3 py-1.5 text-sm"
              >
                <option value="7">7 Days</option>
                <option value="15">15 Days</option>
                <option value="30">30 Days</option>
                <option value="60">60 Days</option>
                <option value="90">90 Days</option>
              </select>
            </div>
            <button
              onClick={handleCleanup}
              className="px-4 py-1.5 bg-red-600 text-white rounded-md text-sm hover:bg-red-700 w-full sm:w-auto"
            >
              Delete Old Logs
            </button>
          </div>
          {cleanupMessage && (
            <p className="mt-2 text-sm font-medium text-red-700">{cleanupMessage}</p>
          )}
        </div>
      )}

      {/* ================= COLLAPSIBLE FILTERS ================= */}
      {showFilters && (
        <div className="bg-white border rounded-lg p-3 mb-4 animate-fadeIn">
          <div className="grid grid-cols-1 md:grid-cols-5 gap-3 items-end">
      
            {/* Level */}
            <div>
              <label className="block text-xs font-medium mb-1">Level</label>
                <select
                  value={level}
                  onChange={(e) => setLevel(e.target.value)}
                  className="w-full border rounded px-3 py-1.5 text-sm"
                >
                  <option value="">All</option>
                  <option value="ERROR">ERROR</option>
                  <option value="WARN">WARN</option>
                  <option value="INFO">INFO</option>
                  <option value="DEBUG">DEBUG</option>
                </select>
            </div>

            {/* Logger */}
            <div>
              <label className="block text-xs font-medium mb-1">Logger</label>
                <input
                  value={logger}
                  onChange={(e) => setLogger(e.target.value)}
                  placeholder="e.g. com.legalaid"
                  className="w-full border rounded px-3 py-1.5 text-sm"
                />
            </div>

            {/* Start Date */}
            <div>
              <label className="block text-xs font-medium mb-1">Start Date</label>
                <input
                  type="datetime-local"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  max={new Date().toISOString().slice(0, 16)}
                  className="w-full border rounded px-3 py-1.5 text-sm"
                />
            </div>

            {/* End Date */}
            <div>
              <label className="block text-xs font-medium mb-1">End Date</label>
                <input
                  type="datetime-local"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  max={new Date().toISOString().slice(0, 16)}
                  className="w-full border rounded px-3 py-1.5 text-sm"
                />
            </div>
  
            {/* Buttons */}
            <div className="flex justify-end gap-2">
              <button
                onClick={handleFilter}
                className="px-4 py-1.5 bg-blue-900 text-white rounded-md text-sm hover:bg-blue-950"
              >
                Apply
              </button>
              <button
                onClick={clearFilters}
                className="px-4 py-1.5 border rounded-md text-sm hover:bg-gray-100"
              >
                Clear
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ================= DESKTOP TABLE ================= */}
      <div className="hidden md:block overflow-x-auto border rounded-lg bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-4 py-2 text-left">ID</th>
              <th className="px-4 py-2 text-left">Timestamp</th>
              <th className="px-4 py-2 text-left">Level</th>
              <th className="px-4 py-2 text-left">Logger</th>
              <th className="px-4 py-2 text-left">Message</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id} className="border-t hover:bg-gray-50">
                <td className="px-4 py-2">{log.id}</td>
                <td className="px-4 py-2">
                  {new Date(log.logTimestamp).toLocaleString()}
                </td>
                <td className="px-4 py-2">
                  <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full border ${getLogLevelStyles(log.level)}`}>
                    {log.level}
                  </span>
                </td>
                <td className="px-4 py-2 truncate max-w-xs">
                  {log.logger}
                </td>
                <td className="px-4 py-2 truncate max-w-lg">
                  {log.message}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* ================= MOBILE CARDS ================= */}
      <div className="md:hidden space-y-3">
        {logs.map((log) => (
          <div
            key={log.id}
            className="border rounded-lg p-3 bg-white shadow-sm"
          >
            <div className="flex justify-between items-center mb-1">
              <span className="text-xs text-gray-500">#{log.id}</span>
              <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full border ${getLogLevelStyles(log.level)}`}>
                {log.level}
              </span>
            </div>

            <p className="text-xs text-gray-500">
              {new Date(log.logTimestamp).toLocaleString()}
            </p>

            <p className="text-xs font-medium truncate">{log.logger}</p>
            <p className="text-sm line-clamp-3">{log.message}</p>
          </div>
        ))}

        {logs.length === 0 && (
          <p className="text-center text-gray-500">No logs found</p>
        )}
      </div>

      {/* ================= PAGINATION ================= */}
      <div className="flex justify-between items-center mt-4">
        <button
          onClick={() => fetchLogs(page - 1)}
          disabled={page === 0}
          className="px-3 py-1 border rounded disabled:opacity-50"
        >
          Prev
        </button>

        <span className="text-sm">
          Page {page + 1} of {totalPages}
        </span>

        <button
          onClick={() => fetchLogs(page + 1)}
          disabled={page + 1 >= totalPages}
          className="px-3 py-1 border rounded disabled:opacity-50"
        >
          Next
        </button>
      </div>
    </div>
  );
}
