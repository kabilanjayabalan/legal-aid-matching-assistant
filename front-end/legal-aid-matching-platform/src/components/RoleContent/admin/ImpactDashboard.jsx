import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { FiMapPin, FiBarChart2, FiTrendingUp, FiUsers, FiSettings } from "react-icons/fi";
import { FaChartBar, FaMap, FaUsers, FaGavel, FaHandshake, FaCalendarCheck, FaCheckCircle } from "react-icons/fa";
import CaseCategoriesChart from "./CaseCategoriesChart";
import GrowthTrendsChart from "./GrowthTrendsChart";
import RoleDistributionChart from "./RoleDistributionChart";
import { getOverviewAnalytics ,getImpactAnalytics } from "../../../services/analytics";


// Fix for default markers in react-leaflet
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const createCustomIcon = (type, count) => {
  const getColor = () => {
    switch (type) {
      case 'cases': return '#1e40af';     // blue
      case 'lawyers': return '#059669';   // green
      case 'ngos': return '#dc2626';      // red
      case 'citizens': return '#ca8a04';  // yellow
      case 'total': return '#374151';     // gray (ALL)
      default: return '#374151';
    }
  };

  const size = Math.min(Math.max(20 + count * 3, 26), 48);

  return L.divIcon({
    html: `
      <div style="
        background-color: ${getColor()};
        width: ${size}px;
        height: ${size}px;
        border-radius: 50%;
        border: 3px solid white;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-weight: bold;
        font-size: 12px;
      ">
        ${count}
      </div>
    `,
    className: 'custom-div-icon',
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2]
  });
};



// Map component with responsive handling
function ResponsiveMap({ data, selectedType }) {
  const map = useMap();

  useEffect(() => {
    const handleResize = () => {
      map.invalidateSize();
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [map]);

  return null;
}

export default function ImpactDashboard() {
  const [activeTab, setActiveTab] = useState("Analytics");
  const [selectedType, setSelectedType] = useState('all');
  const [isMobile, setIsMobile] = useState(false);
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalLawyers: 0,
    totalNGOs: 0,
    totalCases: 0,
    totalMatches: 0,
    activeAppointments: 0,
    resolvedCases: 0
  });

  useEffect(() => {
    document.title = "Impact Dashboard | Legal Aid";
    fetchStats();
    fetchImpactData();

    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkMobile();
    window.addEventListener('resize', checkMobile);

    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  const fetchStats = async () => {
    try {
      const res = await getOverviewAnalytics();
      setStats(res.data);
    } catch (error) {
      console.error("Failed to fetch overview analytics:", error);
    }
  };

  const [impactData, setImpactData] = useState({
    lawyers: [],
    ngos: [],
    citizens: [],
    cases: [],
    total: []
  });

  const fetchImpactData = async () => {
    try {
      const res = await getImpactAnalytics();

      setImpactData({
        lawyers: res.data.lawyersByLocation || [],
        ngos: res.data.ngosByLocation || [],
        citizens: res.data.citizensByLocation || [],
        cases: res.data.casesByLocation || [],
        total: res.data.totalByLocation || []
      });
    } catch (err) {
      console.error("Failed to fetch impact analytics:", err);
    }
  };

  const tabs = [
    { name: "Analytics", icon: <FaChartBar className="text-lg" /> },
    { name: "Map Visualization", icon: <FaMap className="text-lg" /> },
  ];

  const kpiCards = [
    { title: "Total Users", value: stats.totalUsers.toLocaleString(), icon: <FaUsers className="text-blue-600" />, description: "+12% this month" },
    { title: "Total Lawyers", value: stats.totalLawyers.toLocaleString(), icon: <FaGavel className="text-purple-600" />, description: "Verified professionals" },
    { title: "Total NGOs", value: stats.totalNGOs.toLocaleString(), icon: <FaHandshake className="text-green-600" />, description: "Active partners" },
    { title: "Total Cases", value: stats.totalCases.toLocaleString(), icon: <FaCalendarCheck className="text-orange-600" />, description: "Submitted to date" },
    { title: "Total Matches", value: stats.totalMatches.toLocaleString(), icon: <FaHandshake className="text-indigo-600" />, description: "Successful connections" },
    { title: "Active Appointments", value: stats.activeAppointments.toLocaleString(), icon: <FaCalendarCheck className="text-red-600" />, description: "Scheduled this week" },
    { title: "Resolved Cases", value: stats.resolvedCases.toLocaleString(), icon: <FaCheckCircle className="text-teal-600" />, description: "Successfully closed" },
  ];

  const filterTypes = [
    { value: 'all', label: 'All Data', color: 'bg-gray-600' },
    { value: 'cases', label: 'Cases', color: 'bg-blue-950' },
    { value: 'lawyers', label: 'Lawyers', color: 'bg-emerald-600' },
    { value: 'ngos', label: 'NGOs', color: 'bg-red-600' },
    { value: 'citizens', label: 'Citizens', color: 'bg-yellow-600' }
  ];
  
const getMarkersForType = (type) => {
  let source = [];

  switch (type) {
    case 'lawyers':
      source = impactData.lawyers.map(i => ({ ...i, markerType: 'lawyers' }));
      break;

    case 'ngos':
      source = impactData.ngos.map(i => ({ ...i, markerType: 'ngos' }));
      break;

    case 'citizens':
      source = impactData.citizens.map(i => ({ ...i, markerType: 'citizens' }));
      break;

    case 'cases':
      source = impactData.cases.map(i => ({ ...i, markerType: 'cases' }));
      break;

    case 'all':
    default:
      source = impactData.total.map(i => ({
        latitude: i.latitude,
        longitude: i.longitude,
        count: i.total,
        breakdown: i.breakdown,
        markerType: 'total'
      }));
  }

  return source
    .filter(i => i.latitude && i.longitude)
    .map(i => ({
      ...i,
      latitude: Number(i.latitude),
      longitude: Number(i.longitude)
    }));
};

  return (
    <div className="w-full min-h-screen bg-gray-50">
      {/* Header Section */}
      <div className="p-5 pb-1">
        <h1 className="text-2xl md:text-3xl font-bold text-gray-900 mb-2">
          IMPACT DASHBOARD
        </h1>
        <p className="text-gray-600 mb-2">
          Visualize platform analytics and geographical distribution of cases, lawyers, and NGOs across regions.
        </p>
      </div>

      {/* KPI Cards Section */}
      <div className="px-5">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {kpiCards.map((card, index) => (
            <div key={index} className="bg-white rounded-xl shadow p-4 flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-500">{card.title}</p>
                <p className="text-2xl font-bold text-gray-900 mt-1">{card.value}</p>
                {card.description && (
                  <p className="text-xs text-gray-400 mt-1">{card.description}</p>
                )}
              </div>
              <div className="p-3 bg-gray-50 rounded-full">
                {card.icon}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* MOBILE */}
      <div className="block lg:hidden max-w-md mx-auto p-2 pt-4">
        <div className="flex justify-end items-center mb-4">
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

        {activeTab === "Analytics" && (
          <div className="space-y-6">
            <GrowthTrendsChart />
            <CaseCategoriesChart />
            <RoleDistributionChart />
          </div>
        )}

        {activeTab === "Map Visualization" && (
          <div className="space-y-6">
            {/* Map Visualization Widget */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
              {/* Widget Header */}
              <div className="p-4 border-b border-gray-200">
                <div className="flex flex-col gap-4">
                  <div className="flex items-center gap-2">
                    <FiMapPin className="text-blue-950 text-xl" />
                    <h2 className="text-lg font-semibold text-gray-900">
                      Location Analytics
                    </h2>
                  </div>

                  {/* Filter Buttons */}
                  <div className="flex flex-wrap gap-2">
                    {filterTypes.map((filter) => (
                      <button
                        key={filter.value}
                        onClick={() => setSelectedType(filter.value)}
                        className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
                          selectedType === filter.value
                            ? `${filter.color} text-white`
                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                      >
                        {filter.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Map Container */}
              <div className="relative" style={{ height: '400px' }}>
                <MapContainer
                  center={[20.5937, 78.9629]}
                  zoom={4}
                  style={{ height: '100%', width: '100%' }}
                  scrollWheelZoom={true}
                  touchZoom={true}
                  doubleClickZoom={true}
                  dragging={true}
                >
                  <TileLayer
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                  />

                  <ResponsiveMap data={impactData} selectedType={selectedType} />

                  {getMarkersForType(selectedType).map((location, idx) => (
  <Marker
    key={`${location.latitude}-${location.longitude}-${idx}`}
    position={[location.latitude, location.longitude]}
    icon={createCustomIcon(location.markerType, location.count)}
  >
    <Popup>
      <div className="text-sm space-y-1">
        <p className="font-semibold capitalize">
          {location.markerType === 'total'
            ? 'All Entities'
            : location.markerType}
        </p>

        <p>
          Total: <strong>{location.count}</strong>
        </p>

        {location.breakdown && (
          <div className="pt-1 border-t text-xs space-y-0.5">
            <p>👨‍⚖ Lawyers: {location.breakdown.lawyers}</p>
            <p>🏢 NGOs: {location.breakdown.ngos}</p>
            <p>👤 Citizens: {location.breakdown.citizens}</p>
            <p>📁 Cases: {location.breakdown.cases}</p>
          </div>
        )}

        <p className="text-gray-400 text-xs pt-1">
          Lat: {location.latitude.toFixed(4)},
          Lng: {location.longitude.toFixed(4)}
        </p>
      </div>
    </Popup>
  </Marker>
))}

                </MapContainer>
              </div>

              {/* Legend */}
              <div className="p-4 bg-gray-50 border-t border-gray-200">
                <div className="flex flex-wrap items-center gap-4 text-sm">
                  <span className="font-medium text-gray-700">Legend:</span>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 bg-blue-950 rounded-full"></div>
                    <span>Cases</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 bg-emerald-600 rounded-full"></div>
                    <span>Lawyers</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 bg-red-600 rounded-full"></div>
                    <span>NGOs</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 bg-yellow-600 rounded-full"></div>
                    <span>Citizens</span>
                  </div>
                  <span className="text-gray-500 ml-2">
                    • Marker size indicates quantity • Click markers for details
                  </span>
                </div>
              </div>
            </div>

            {/* Key Insights */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Key Insights</h3>
              <div className="grid grid-cols-1 gap-4">
                <div className="p-4 bg-blue-50 rounded-lg">
                  <h4 className="font-medium text-blue-950 mb-2">High-Demand Regions</h4>
                  <p className="text-sm text-gray-700">
                    Mumbai and Delhi show the highest case volumes, indicating strong demand for legal aid services in major metropolitan areas.
                  </p>
                </div>
                <div className="p-4 bg-emerald-50 rounded-lg">
                  <h4 className="font-medium text-emerald-600 mb-2">Coverage Analysis</h4>
                  <p className="text-sm text-gray-700">
                    Southern cities like Bangalore and Chennai have better lawyer-to-case ratios, while northern regions show potential for expansion.
                  </p>
                </div>
                <div className="p-4 bg-red-50 rounded-lg">
                  <h4 className="font-medium text-red-600 mb-2">Partnership Growth</h4>
                  <p className="text-sm text-gray-700">
                    NGO partnerships are strongest in tier-1 cities, with opportunities for growth in tier-2 cities like Jaipur and Lucknow.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* DESKTOP */}
      <div className="hidden lg:block px-5 pt-4 pb-5">
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

        {activeTab === "Analytics" && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <GrowthTrendsChart />
            <CaseCategoriesChart />
            <RoleDistributionChart />
          </div>
        )}

        {activeTab === "Map Visualization" && (
          <div className="space-y-6">
            {/* Map Visualization Widget */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
              {/* Widget Header */}
              <div className="p-4 border-b border-gray-200">
                <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                  <div className="flex items-center gap-2">
                    <FiMapPin className="text-blue-950 text-xl" />
                    <h2 className="text-lg font-semibold text-gray-900">
                      Location Analytics
                    </h2>
                  </div>

                  {/* Filter Buttons */}
                  <div className="flex flex-wrap gap-2">
                    {filterTypes.map((filter) => (
                      <button
                        key={filter.value}
                        onClick={() => setSelectedType(filter.value)}
                        className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
                          selectedType === filter.value
                            ? `${filter.color} text-white`
                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                      >
                        {filter.label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Map Container */}
              <div className="relative" style={{ height: '500px' }}>
                <MapContainer
                  center={[20.5937, 78.9629]}
                  zoom={5}
                  style={{ height: '100%', width: '100%' }}
                  scrollWheelZoom={true}
                  touchZoom={true}
                  doubleClickZoom={true}
                  dragging={true}
                >
                  <TileLayer
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                  />

                  <ResponsiveMap data={impactData} selectedType={selectedType} />

                  {getMarkersForType(selectedType).map((location, idx) => (
  <Marker
    key={`${location.latitude}-${location.longitude}-${idx}`}
    position={[location.latitude, location.longitude]}
    icon={createCustomIcon(location.markerType, location.count)}
  >
    <Popup>
      <div className="text-sm space-y-1">
        <p className="font-semibold capitalize">
          {location.markerType === 'total'
            ? 'All Entities'
            : location.markerType}
        </p>

        <p>
          Total: <strong>{location.count}</strong>
        </p>

        {location.breakdown && (
          <div className="pt-1 border-t text-xs space-y-0.5">
            <p>👨‍⚖ Lawyers: {location.breakdown.lawyers}</p>
            <p>🏢 NGOs: {location.breakdown.ngos}</p>
            <p>👤 Citizens: {location.breakdown.citizens}</p>
            <p>📁 Cases: {location.breakdown.cases}</p>
          </div>
        )}

        <p className="text-gray-400 text-xs pt-1">
          Lat: {location.latitude.toFixed(4)},
          Lng: {location.longitude.toFixed(4)}
        </p>
      </div>
    </Popup>
  </Marker>
))}

                </MapContainer>
              </div>

              {/* Legend */}
              <div className="p-4 bg-gray-50 border-t border-gray-200">
                <div className="flex flex-wrap items-center gap-4 text-sm">
                  <span className="font-medium text-gray-700">Legend:</span>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 bg-blue-950 rounded-full"></div>
                    <span>Cases</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 bg-emerald-600 rounded-full"></div>
                    <span>Lawyers</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 bg-red-600 rounded-full"></div>
                    <span>NGOs</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 bg-yellow-600 rounded-full"></div>
                    <span>Citizens</span>
                  </div>
                  <span className="text-gray-500 ml-2">
                    • Marker size indicates quantity • Click markers for details
                  </span>
                </div>
              </div>
            </div>

            {/* Key Insights */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Key Insights</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                <div className="p-4 bg-blue-50 rounded-lg">
                  <h4 className="font-medium text-blue-950 mb-2">High-Demand Regions</h4>
                  <p className="text-sm text-gray-700">
                    Mumbai and Delhi show the highest case volumes, indicating strong demand for legal aid services in major metropolitan areas.
                  </p>
                </div>
                <div className="p-4 bg-emerald-50 rounded-lg">
                  <h4 className="font-medium text-emerald-600 mb-2">Coverage Analysis</h4>
                  <p className="text-sm text-gray-700">
                    Southern cities like Bangalore and Chennai have better lawyer-to-case ratios, while northern regions show potential for expansion.
                  </p>
                </div>
                <div className="p-4 bg-red-50 rounded-lg">
                  <h4 className="font-medium text-red-600 mb-2">Partnership Growth</h4>
                  <p className="text-sm text-gray-700">
                    NGO partnerships are strongest in tier-1 cities, with opportunities for growth in tier-2 cities like Jaipur and Lucknow.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
