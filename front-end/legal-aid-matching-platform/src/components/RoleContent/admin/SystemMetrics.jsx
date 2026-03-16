import { useEffect, useState } from "react";
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import {
  FiCpu,
  FiDatabase,
  FiClock,
  FiHardDrive,
  FiAlertTriangle,
  FiAlertCircle,
  FiInfo,
  FiActivity,
  FiServer,
  FiLayers,
  FiZap,
  FiGlobe,
  FiCheckCircle,
  FiXCircle,
} from "react-icons/fi";
import {
  getSystemHealth,
  getSystemLoadOverTime,
  getServiceActivity,
  getActuatorMetrics,
} from "../../../services/api";

export default function SystemMetrics() {
  const [metrics, setMetrics] = useState(null);
  const [actuatorMetrics, setActuatorMetrics] = useState(null);
  const [systemLoadData, setSystemLoadData] = useState([]);
  const [serviceActivityData, setServiceActivityData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Helper function to shorten service names for x-axis display
  const shortenServiceName = (fullName) => {
    if (!fullName) return "";
    
    // Remove special characters at the start (like g$, +, -)
    let cleaned = fullName.replace(/^[^a-zA-Z]+/, "");
    
    // Extract the last meaningful part after the last dot
    const parts = cleaned.split(".");
    if (parts.length > 1) {
      // Get the last part (usually the class name)
      const lastPart = parts[parts.length - 1];
      // If it's a meaningful name, use it; otherwise use second-to-last
      if (lastPart && lastPart.length > 3) {
        return lastPart;
      }
      return parts[parts.length - 2] || lastPart;
    }
    
    // If no dots, return cleaned name (max 20 chars)
    return cleaned.length > 20 ? cleaned.substring(0, 17) + "..." : cleaned;
  };

  useEffect(() => {
    const fetchSystemMetrics = async () => {
      try {
        setError(null);
        const [health, loadOverTime, serviceActivity, actuator] = await Promise.all([
          getSystemHealth(),
          getSystemLoadOverTime(),
          getServiceActivity(),
          getActuatorMetrics().catch(() => null), // Gracefully handle if actuator endpoint fails
        ]);

        setMetrics({
          cpuUsage: Math.round(health.cpuUsage ?? 0),
          memoryUsage: Math.round(health.memoryUsage ?? 0),
          uptime: health.uptimeDays ?? 0,
          diskUsage: Math.round(health.diskUsage ?? 0),
          criticalErrors: health.criticalErrors ?? 0,
          warningAlerts: health.warningAlerts ?? 0,
          infoMessages: health.infoMessages ?? 0,
        });

        if (actuator) {
          setActuatorMetrics(actuator);
        }

        setSystemLoadData(
          (loadOverTime || []).map((point) => ({
            day: point.day,
            load: point.load,
          }))
        );

        setServiceActivityData(
          (serviceActivity || []).map((item) => ({
            name: item.name,
            displayName: shortenServiceName(item.name),
            count: item.count,
          }))
        );
      } catch (err) {
        console.error("Failed to fetch system metrics:", err);
        setError("Unable to load system metrics right now.");
      } finally {
        setLoading(false);
      }
    };

    fetchSystemMetrics();

    const interval = setInterval(fetchSystemMetrics, 30000);
    return () => clearInterval(interval);
  }, []);

  const getStatusColor = (value, type = "usage") => {
    if (type === "usage") {
      if (value >= 85) return "text-red-600 bg-red-50";
      if (value >= 70) return "text-yellow-600 bg-yellow-50";
      return "text-green-600 bg-green-50";
    }
    return "text-blue-600 bg-blue-50";
  };

  const getStatusText = (value) => {
    if (value >= 85) return "High usage detected";
    if (value >= 70) return "Approaching limit";
    return "Stable operation";
  };

  const getDiskStatusText = (value) => {
    if (value >= 85) return "Critically low space";
    if (value >= 70) return "Low space";
    return "Adequate space";
  };

  const MetricCard = ({ title, value, unit, icon, status, description }) => (
    <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex justify-between items-start mb-2">
        <div>
          <p className="text-sm text-gray-600 font-medium">{title}</p>
        </div>
        <div className="text-red-500 text-xl">{icon}</div>
      </div>
      <div className="mb-2">
        <span className="text-3xl font-bold text-gray-900">{value}</span>
        <span className="text-lg text-gray-600">{unit}</span>
      </div>
      <div className={`inline-flex items-center gap-1 text-xs px-2 py-1 rounded-full ${status}`}>
        ● <span className="font-medium">{description}</span>
      </div>
    </div>
  );

  const AlertCard = ({ title, count, icon, description }) => (
    <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex justify-between items-start mb-2">
        <div>
          <p className="text-sm text-gray-600 font-medium">{title}</p>
        </div>
        <div className="text-gray-600 text-xl">{icon}</div>
      </div>
      <div className="mb-2">
        <span className="text-3xl font-bold text-gray-900">{count}</span>
      </div>
      <p className="text-xs text-gray-500">{description}</p>
    </div>
  );

  if (loading && !metrics) {
    return (
      <div className="p-4">
        <p className="text-sm text-gray-600">Loading system metrics...</p>
      </div>
    );
  }

  if (error && !metrics) {
    return (
      <div className="p-4">
        <p className="text-sm text-red-600">{error}</p>
      </div>
    );
  }

  if (!metrics) {
    return null;
  }

  return (
    <div className="p-2">
      {/* System Health Metrics */}
      <div className="mb-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
          <MetricCard
            key="cpu-usage"
            title="CPU Usage"
            value={metrics.cpuUsage}
            unit="%"
            icon={<FiCpu />}
            status={getStatusColor(metrics.cpuUsage)}
            description={getStatusText(metrics.cpuUsage)}
          />
          <MetricCard
            key="memory-usage"
            title="Memory Usage"
            value={metrics.memoryUsage}
            unit="%"
            icon={<FiDatabase />}
            status={getStatusColor(metrics.memoryUsage)}
            description={getStatusText(metrics.memoryUsage)}
          />
          <MetricCard
            key="uptime"
            title="Uptime"
            value={metrics.uptime}
            unit=" days"
            icon={<FiClock />}
            status="text-blue-600 bg-blue-50"
            description="Stable operation"
          />
          <MetricCard
            key="disk-usage"
            title="Disk Usage"
            value={metrics.diskUsage}
            unit="%"
            icon={<FiHardDrive />}
            status={getStatusColor(metrics.diskUsage)}
            description={getDiskStatusText(metrics.diskUsage)}
          />
        </div>
      </div>

      {/* Alerts and Messages */}
      <div className="mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <AlertCard
            key="critical-errors"
            title="Critical Errors"
            count={metrics.criticalErrors}
            icon={<FiAlertCircle />}
            description="incidents"
          />
          <AlertCard
            key="warning-alerts"
            title="Warning Alerts"
            count={metrics.warningAlerts}
            icon={<FiAlertTriangle />}
            description="incidents"
          />
          <AlertCard
            key="info-messages"
            title="Info Messages"
            count={metrics.infoMessages}
            icon={<FiInfo />}
            description="incidents"
          />
        </div>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* System Load Over Time */}
        <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">System Load Over Time</h3>
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={systemLoadData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis
                dataKey="day"
                tick={{ fontSize: 12 }}
                stroke="#6b7280"
              />
              <YAxis
                tick={{ fontSize: 12 }}
                stroke="#6b7280"
                label={{ value: '%', angle: -90, position: 'insideLeft', style: { fontSize: 12 } }}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#fff',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  fontSize: '12px'
                }}
              />
              <Line
                type="monotone"
                dataKey="load"
                stroke="#3b82f6"
                strokeWidth={2}
                dot={{ fill: '#3b82f6', r: 4 }}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Service Activity Breakdown */}
        <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Service Activity Breakdown</h3>
          <div
            className="overflow-y-auto overflow-x-hidden"
            style={{ maxHeight: 300 }}
          >
            <div style={{ height: Math.max(300, serviceActivityData.length * 35) }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={serviceActivityData}
                  layout="vertical"
                  margin={{ top: 5, right: 30, left: 100, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" horizontal={false} />
                  <XAxis
                    type="number"
                    tick={{ fontSize: 12, fill: '#374151' }}
                    stroke="#6b7280"
                  />
                  <YAxis
                    type="category"
                    dataKey="displayName"
                    tick={{ fontSize: 11, fill: '#374151' }}
                    stroke="#6b7280"
                    width={95}
                    interval={0}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#fff',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      fontSize: '12px'
                    }}
                    formatter={(value) => [value, 'Count']}
                    labelFormatter={(label) => {
                      const item = serviceActivityData.find(d => d.displayName === label);
                      return item ? item.name : label;
                    }}
                  />
                  <Bar
                    dataKey="count"
                    fill="#3b82f6"
                    radius={[0, 8, 8, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>


      {/* Actuator-Based Advanced Metrics */}
      {actuatorMetrics && (
        <>
          {/* Section Header */}
          <div className="mt-8 mb-4">
            <h3 className="text-xl font-bold text-gray-900 flex items-center gap-2">
              <FiActivity className="text-blue-600" />
              Advanced System Metrics
            </h3>
            <p className="text-sm text-gray-600 mt-1">
              Detailed metrics from Spring Boot Actuator for in-depth system monitoring
            </p>
          </div>

          {/* Health Status */}
          <div className="mb-6">
            <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h4 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
                  {actuatorMetrics.health?.healthy ? (
                    <FiCheckCircle className="text-green-600" />
                  ) : (
                    <FiXCircle className="text-red-600" />
                  )}
                  Application Health
                </h4>
                <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                  actuatorMetrics.health?.healthy 
                    ? "bg-green-100 text-green-800" 
                    : "bg-red-100 text-red-800"
                }`}>
                  {actuatorMetrics.health?.status || "UNKNOWN"}
                </span>
              </div>
              {actuatorMetrics.health?.components && Object.keys(actuatorMetrics.health.components).length > 0 && (
                <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                  {Object.entries(actuatorMetrics.health.components).map(([name, status]) => (
                    <div key={name} className="flex items-center gap-2 text-sm">
                      {status === "UP" ? (
                        <FiCheckCircle className="text-green-500" />
                      ) : (
                        <FiXCircle className="text-red-500" />
                      )}
                      <span className="text-gray-700 capitalize">{name}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* JVM & Thread Metrics */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            {/* JVM Information */}
            <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
              <h4 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <FiServer className="text-purple-600" />
                JVM Information
              </h4>
              <div className="space-y-3">
                <div className="flex justify-between items-center py-2 border-b border-gray-100">
                  <span className="text-sm text-gray-600">Java Version</span>
                  <span className="text-sm font-medium text-gray-900">{actuatorMetrics.jvm?.jvmVersion}</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-gray-100">
                  <span className="text-sm text-gray-600">JVM Name</span>
                  <span className="text-sm font-medium text-gray-900">{actuatorMetrics.jvm?.jvmName}</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-gray-100">
                  <span className="text-sm text-gray-600">Vendor</span>
                  <span className="text-sm font-medium text-gray-900">{actuatorMetrics.jvm?.jvmVendor}</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-gray-100">
                  <span className="text-sm text-gray-600">Uptime</span>
                  <span className="text-sm font-medium text-green-600">{actuatorMetrics.jvm?.formattedUptime}</span>
                </div>
                <div className="flex justify-between items-center py-2">
                  <span className="text-sm text-gray-600">Start Time</span>
                  <span className="text-sm font-medium text-gray-900">
                    {actuatorMetrics.jvm?.startTime ? new Date(actuatorMetrics.jvm.startTime).toLocaleString() : "N/A"}
                  </span>
                </div>
              </div>
            </div>

            {/* Thread Metrics */}
            <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
              <h4 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <FiLayers className="text-indigo-600" />
                Thread Pool Status
              </h4>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div key="live-threads" className="bg-indigo-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-indigo-700">{actuatorMetrics.threads?.liveThreads || 0}</p>
                  <p className="text-xs text-indigo-600">Live Threads</p>
                </div>
                <div key="peak-threads" className="bg-purple-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-purple-700">{actuatorMetrics.threads?.peakThreads || 0}</p>
                  <p className="text-xs text-purple-600">Peak Threads</p>
                </div>
                <div key="runnable-threads" className="bg-green-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-green-700">{actuatorMetrics.threads?.runnableThreads || 0}</p>
                  <p className="text-xs text-green-600">Runnable</p>
                </div>
                <div key="waiting-threads" className="bg-yellow-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-yellow-700">{actuatorMetrics.threads?.waitingThreads || 0}</p>
                  <p className="text-xs text-yellow-600">Waiting</p>
                </div>
              </div>
              <div className="text-xs text-gray-500 flex justify-between">
                <span>Daemon: {actuatorMetrics.threads?.daemonThreads || 0}</span>
                <span>Blocked: {actuatorMetrics.threads?.blockedThreads || 0}</span>
                <span>Total Started: {actuatorMetrics.threads?.totalStartedThreads || 0}</span>
              </div>
            </div>
          </div>

          {/* Memory & GC Metrics */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            {/* Memory Details */}
            <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
              <h4 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <FiDatabase className="text-blue-600" />
                Memory Details
              </h4>
              <div className="space-y-4">
                <div>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-gray-600">Heap Memory</span>
                    <span className="font-medium">{actuatorMetrics.memory?.heapUsedMB?.toFixed(0)} MB / {actuatorMetrics.memory?.heapMaxMB?.toFixed(0)} MB</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full ${
                        (actuatorMetrics.memory?.heapUsagePercent || 0) >= 85 ? "bg-red-500" :
                        (actuatorMetrics.memory?.heapUsagePercent || 0) >= 70 ? "bg-yellow-500" : "bg-blue-500"
                      }`}
                      style={{ width: `${Math.min(100, actuatorMetrics.memory?.heapUsagePercent || 0)}%` }}
                    />
                  </div>
                  <p className="text-xs text-gray-500 mt-1">{actuatorMetrics.memory?.heapUsagePercent?.toFixed(1)}% used</p>
                </div>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div key="non-heap-used">
                    <span className="text-gray-600">Non-Heap Used</span>
                    <p className="font-medium">{actuatorMetrics.memory?.nonHeapUsedMB?.toFixed(1)} MB</p>
                  </div>
                  <div key="heap-committed">
                    <span className="text-gray-600">Heap Committed</span>
                    <p className="font-medium">{actuatorMetrics.memory?.heapCommittedMB?.toFixed(1)} MB</p>
                  </div>
                  <div key="direct-buffers">
                    <span className="text-gray-600">Direct Buffers</span>
                    <p className="font-medium">{actuatorMetrics.memory?.directBufferMB?.toFixed(1)} MB</p>
                  </div>
                  <div key="mapped-buffers">
                    <span className="text-gray-600">Mapped Buffers</span>
                    <p className="font-medium">{actuatorMetrics.memory?.mappedBufferMB?.toFixed(1)} MB</p>
                  </div>
                </div>
              </div>
            </div>

            {/* GC Metrics */}
            <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
              <h4 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <FiZap className="text-orange-600" />
                Garbage Collection
              </h4>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div key="gc-collections" className="bg-orange-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-orange-700">{actuatorMetrics.gc?.totalCollections || 0}</p>
                  <p className="text-xs text-orange-600">Total Collections</p>
                </div>
                <div key="gc-time" className="bg-amber-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-amber-700">{(actuatorMetrics.gc?.totalTimeMs || 0).toLocaleString()}</p>
                  <p className="text-xs text-amber-600">Total Time (ms)</p>
                </div>
              </div>
              {actuatorMetrics.gc?.collectors && actuatorMetrics.gc.collectors.length > 0 && (
                <div className="space-y-2">
                  <p className="text-sm font-medium text-gray-700">Collectors:</p>
                  {actuatorMetrics.gc.collectors.map((collector, idx) => (
                    <div key={`gc-${collector.name}-${idx}`} className="flex justify-between text-sm py-1 border-b border-gray-100 last:border-0">
                      <span className="text-gray-600">{collector.name}</span>
                      <span className="text-gray-900">{collector.count} runs ({collector.timeMs}ms)</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* CPU & HTTP Metrics */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            {/* CPU Details */}
            <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
              <h4 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <FiCpu className="text-red-600" />
                CPU Details
              </h4>
              <div className="grid grid-cols-2 gap-4">
                <div key="system-cpu" className="bg-red-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-red-700">{actuatorMetrics.cpu?.systemCpuUsage?.toFixed(1) || 0}%</p>
                  <p className="text-xs text-red-600">System CPU</p>
                </div>
                <div key="process-cpu" className="bg-pink-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-pink-700">{actuatorMetrics.cpu?.processCpuUsage?.toFixed(1) || 0}%</p>
                  <p className="text-xs text-pink-600">Process CPU</p>
                </div>
                <div key="cpu-processors" className="bg-gray-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-gray-700">{actuatorMetrics.cpu?.availableProcessors || 0}</p>
                  <p className="text-xs text-gray-600">Available Processors</p>
                </div>
                <div key="cpu-load-avg" className="bg-gray-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-gray-700">{actuatorMetrics.cpu?.systemLoadAverage?.toFixed(2) || 0}</p>
                  <p className="text-xs text-gray-600">Load Average</p>
                </div>
              </div>
            </div>

            {/* HTTP Metrics */}
            <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
              <h4 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <FiGlobe className="text-teal-600" />
                HTTP Request Metrics
              </h4>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div key="http-total-requests" className="bg-teal-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-teal-700">{actuatorMetrics.http?.totalRequests?.toLocaleString() || 0}</p>
                  <p className="text-xs text-teal-600">Total Requests</p>
                </div>
                <div key="http-avg-response" className="bg-cyan-50 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-cyan-700">{actuatorMetrics.http?.avgResponseTimeMs?.toFixed(2) || 0}</p>
                  <p className="text-xs text-cyan-600">Avg Response (ms)</p>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-2 text-center text-sm">
                <div key="http-success" className="bg-green-50 p-2 rounded">
                  <p className="font-medium text-green-700">{actuatorMetrics.http?.successfulRequests || 0}</p>
                  <p className="text-xs text-green-600">Success</p>
                </div>
                <div key="http-4xx" className="bg-yellow-50 p-2 rounded">
                  <p className="font-medium text-yellow-700">{actuatorMetrics.http?.clientErrors || 0}</p>
                  <p className="text-xs text-yellow-600">4xx Errors</p>
                </div>
                <div key="http-5xx" className="bg-red-50 p-2 rounded">
                  <p className="font-medium text-red-700">{actuatorMetrics.http?.serverErrors || 0}</p>
                  <p className="text-xs text-red-600">5xx Errors</p>
                </div>
              </div>
              {(actuatorMetrics.http?.activeConnections > 0 || actuatorMetrics.http?.maxConnections > 0) && (
                <div className="mt-3 text-xs text-gray-500 flex justify-between">
                  <span>Active Connections: {actuatorMetrics.http?.activeConnections || 0}</span>
                  <span>Max Connections: {actuatorMetrics.http?.maxConnections || 0}</span>
                </div>
              )}
            </div>
          </div>

          {/* Disk & Application Info */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Disk Details */}
            <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
              <h4 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <FiHardDrive className="text-gray-600" />
                Disk Storage
              </h4>
              <div className="space-y-4">
                <div>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-gray-600">Disk Usage</span>
                    <span className="font-medium">{actuatorMetrics.disk?.usedSpaceGB?.toFixed(1)} GB / {actuatorMetrics.disk?.totalSpaceGB?.toFixed(1)} GB</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full ${
                        (actuatorMetrics.disk?.usagePercent || 0) >= 85 ? "bg-red-500" :
                        (actuatorMetrics.disk?.usagePercent || 0) >= 70 ? "bg-yellow-500" : "bg-green-500"
                      }`}
                      style={{ width: `${Math.min(100, actuatorMetrics.disk?.usagePercent || 0)}%` }}
                    />
                  </div>
                  <p className="text-xs text-gray-500 mt-1">{actuatorMetrics.disk?.usagePercent?.toFixed(1)}% used • {actuatorMetrics.disk?.freeSpaceGB?.toFixed(1)} GB free</p>
                </div>
              </div>
            </div>

            {/* Application Info */}
            <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
              <h4 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <FiInfo className="text-blue-600" />
                Application Info
              </h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between py-1 border-b border-gray-100">
                  <span className="text-gray-600">Application</span>
                  <span className="font-medium text-gray-900">{actuatorMetrics.application?.name}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-gray-100">
                  <span className="text-gray-600">Version</span>
                  <span className="font-medium text-gray-900">{actuatorMetrics.application?.version}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-gray-100">
                  <span className="text-gray-600">Spring Boot</span>
                  <span className="font-medium text-gray-900">{actuatorMetrics.application?.springBootVersion}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-gray-100">
                  <span className="text-gray-600">OS</span>
                  <span className="font-medium text-gray-900">{actuatorMetrics.application?.osName} ({actuatorMetrics.application?.osArch})</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-gray-600">OS Version</span>
                  <span className="font-medium text-gray-900">{actuatorMetrics.application?.osVersion}</span>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

