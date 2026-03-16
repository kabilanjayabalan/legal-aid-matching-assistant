import { useEffect, useState } from "react";
import {
  Bell,
  MessageSquare,
  Calendar,
  CheckCircle,
  Loader2,
  Filter,
  Send,
  X,
  MapPin,
  Phone,
  Globe,
  FileText,
  TrendingUp,
  MoreHorizontal,
  Clock,
} from "lucide-react";

import { useNavigate } from "react-router-dom";
import { getProviderDashboardStats, acceptMatch, rejectMatch, getProviderProfile, getCaseById, getMyAppointments, createAppointment, getMyProfile } from "../../../services/api";
import { useAlert } from "../../../context/AlertContext";
import AppointmentScheduler from "../citizen/AppointmentScheduler";

const BLUE = "#6610f2";

function StatCard({ icon: Icon, title, value, subtitle }) {
  return (
    <div className="bg-white rounded-xl p-4 shadow flex items-center gap-4 w-full">
      <div
        className="p-3 rounded-lg text-white flex-shrink-0 bg-blue-950"
      >
        <Icon size={24} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-500 truncate">{title}</p>
        <p className="text-medium font-semibold text-gray-900 mt-1">{value}</p>
        <p className="text-xs text-gray-400 mt-1 truncate">{subtitle}</p>
      </div>
    </div>
  );
}

// Helper function to generate avatar with first letter of name
const getDefaultImage = (name, type) => {
  const firstLetter = name ? name.charAt(0).toUpperCase() : '?';
  const colors = [
    '#6610f2', '#6f42c1', '#6c757d', '#0d6efd', '#198754',
    '#fd7e14', '#dc3545', '#20c997', '#ffc107', '#0dcaf0'
  ];
  const colorIndex = name ? name.charCodeAt(0) % colors.length : 0;
  const bgColor = colors[colorIndex];

  const svg = `
    <svg width="100" height="100" xmlns="http://www.w3.org/2000/svg">
      <circle cx="50" cy="50" r="50" fill="${bgColor}"/>
      <text x="50" y="50" font-family="Arial, sans-serif" font-size="40" font-weight="bold" fill="white" text-anchor="middle" dominant-baseline="central">${firstLetter}</text>
    </svg>
  `.trim();

  return `data:image/svg+xml;base64,${btoa(svg)}`;
};

// Map backend status to frontend status
const mapStatus = (backendStatus) => {
  switch (backendStatus) {
    case 'PENDING':
      return 'pending';
    case 'CITIZEN_ACCEPTED':
      return 'pending'; // For provider, citizen accepted is a pending request
    case 'PROVIDER_CONFIRMED':
      return 'accepted';
    case 'REJECTED':
      return 'rejected';
    default:
      return 'pending';
  }
};

// Get status badge styling
const getStatusBadge = (status) => {
  switch (status) {
    case 'pending':
      return {
        text: 'Pending Action',
        className: 'bg-yellow-100 text-yellow-800 border-yellow-300'
      };
    case 'accepted':
      return {
        text: 'Active Case',
        className: 'bg-green-100 text-green-800 border-green-300'
      };
    case 'rejected':
      return {
        text: 'Rejected',
        className: 'bg-red-100 text-red-800 border-red-300'
      };
    default:
      return {
        text: 'Unknown',
        className: 'bg-gray-100 text-gray-800 border-gray-300'
      };
  }
};

function MatchItem({
  matchId,
  name, // This will be Case Title for provider view
  fullName, // Not used for provider view usually
  type,
  status,
  image,
  compatibility,
  verified = true,
  online = false,
  onProfileClick,
  onAccept,
  onReject,
  onChat,
  matchStatus,
  providerId,
  providerType,
  caseTitle,
  caseId,
  onCaseClick,
  onCreateAppointment,
  isAssignedCase = false
}) {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const statusBadge = getStatusBadge(matchStatus);
  
  // For provider:
  // Pending = CITIZEN_ACCEPTED (Needs provider action)
  // Accepted = PROVIDER_CONFIRMED (Active case)
  const canInteract = matchStatus === 'pending' && !isAssignedCase;
  const isAccepted = matchStatus === 'accepted' || isAssignedCase;

  return (
    <div className="flex items-center justify-between p-4 border rounded-xl hover:shadow transition">
      <div className="flex items-center gap-4">
        {/* Avatar/Icon */}
        <div className="relative">
           <div className="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center text-blue-600 font-bold text-lg">
              {caseTitle ? caseTitle.charAt(0).toUpperCase() : '#'}
           </div>
        </div>

        {/* Info */}
        <div>
          <div className="flex items-center gap-2">
            <p className="text-sm font-semibold">{caseTitle || `Case #${caseId}`}</p>
          </div>

          <div className="flex items-center gap-2 mt-1">
            <p className="text-xs text-gray-500">
              Match ID: {matchId}
            </p>
            <span className={`px-2 py-0.5 text-xs font-medium rounded-full border ${statusBadge.className}`}>
              {statusBadge.text}
            </span>
          </div>

          {/* Compatibility */}
          {typeof compatibility === 'number' && (
            <div className="flex items-center gap-1 mt-1">
              <TrendingUp size={12} style={{ color: "#6610f2" }} />
              <span className="text-xs font-semibold" style={{ color: BLUE }}>
                {compatibility}% Match Score
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Actions */}
      <div className="relative">
        <button
          className="p-2 rounded-lg hover:bg-gray-100 transition text-gray-400 hover:text-gray-600"
          title="More Actions"
          onClick={() => setIsDropdownOpen(!isDropdownOpen)}
          onBlur={() => setTimeout(() => setIsDropdownOpen(false), 200)}
        >
          <MoreHorizontal size={20} />
        </button>

        {isDropdownOpen && (
          <div className="absolute right-0 top-full mt-2 w-48 bg-white border rounded-xl shadow-lg z-10 flex flex-col p-1">
            {canInteract && (
              <>
                <button
                  onClick={() => onAccept && onAccept(matchId)}
                  className="w-full text-left px-4 py-2 text-sm text-green-700 hover:bg-green-50 rounded-lg transition"
                >
                  Confirm Match
                </button>
                <button
                  onClick={() => onReject && onReject(matchId)}
                  className="w-full text-left px-4 py-2 text-sm text-red-700 hover:bg-red-50 rounded-lg transition"
                >
                  Reject Match
                </button>
              </>
            )}
            {isAccepted && (
              <>
                <button
                  onClick={() => onChat && onChat(matchId)}
                  className="w-full text-left px-4 py-2 text-sm text-indigo-700 hover:bg-blue-50 rounded-lg transition flex items-center gap-2"
                >
                  <Send size={14} />
                  Chat with Citizen
                </button>
              </>
            )}
            {caseId && (
              <button
                onClick={() => onCaseClick && onCaseClick(caseId)}
                className="w-full text-left px-4 py-2 text-sm text-grey-700 hover:bg-indigo-50 rounded-lg transition"
              >
                View Case Details
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default function Stats() {
  const navigate = useNavigate();
  const [matchRequests, setMatchRequests] = useState([]);
  const [assignedCases, setAssignedCases] = useState([]);
  const [loadingStats, setLoadingStats] = useState(true);
  
  // Stats counts
  const [matchRequestsCount, setMatchRequestsCount] = useState(0);
  const [assignedCasesCount, setAssignedCasesCount] = useState(0);

  const [selectedCase, setSelectedCase] = useState(null);
  const [caseLoading, setCaseLoading] = useState(false);
  const [appointments, setAppointments] = useState([]);
  const [loadingAppointments, setLoadingAppointments] = useState(true);
  const { showAlert } = useAlert();
  const [upcomingCalls, setUpcomingCalls] = useState(0);
  const [resolvedCasesCount, setResolvedCasesCount] = useState(0);


  useEffect(() => {
    document.title = "Dashboard | Legal Aid";
    fetchDashboardStats();
    fetchAppointments();
  }, []);

  const fetchAppointments = async () => {
    try {
      setLoadingAppointments(true);
      const userRes = await getMyProfile();

      if (userRes.data && userRes.data.id) {
        const apptsRes = await getMyAppointments(userRes.data.id.toString());
        const appts = apptsRes.data || [];

        // Filter to show only upcoming appointments (date >= today)
        const today = new Date();
         today.setHours(0, 0, 0, 0);

         const upcomingAppts = appts.filter(appt => {
          if (appt.status === "CANCELLED" || appt.status === "COMPLETED") {
            return false;
          }
          const apptDate = new Date(appt.appointmentDate);
          apptDate.setHours(0, 0, 0, 0);
          return apptDate >= today;
        });

        // Sort by date (earliest first)
        upcomingAppts.sort((a, b) => {
          const dateA = new Date(a.appointmentDate);
          const dateB = new Date(b.appointmentDate);
          return dateA - dateB;
        });
        setUpcomingCalls(upcomingAppts.length);

        // Map to display format
        const formattedAppts = upcomingAppts.map(appt => ({
          id: appt.id,
          lawyerName: `Match ID: ${appt.matchId}`, 
          date: new Date(appt.appointmentDate).toLocaleDateString(),
          time: appt.timeSlot,
          caseTitle: `Duration: ${appt.durationMinutes} mins`,
          status: appt.status
        }));

        setAppointments(formattedAppts);
      }
    } catch (error) {
      console.error("Failed to fetch appointments:", error);
    } finally {
      setLoadingAppointments(false);
    }
  };


  const fetchDashboardStats = async () => {
    try {
      setLoadingStats(true);
      const response = await getProviderDashboardStats();
      const data = response.data;

      setMatchRequestsCount(data.matchRequestsCount || 0);
      setAssignedCasesCount(data.assignedCasesCount || 0);

      // Process Match Requests
      const requests = (data.recentMatchRequests || []).map(match => ({
        id: match.matchId,
        matchId: match.matchId,
        matchStatus: mapStatus(match.status),
        score: match.score || 0,
        createdAt: match.createdAt,
        caseTitle: match.caseTitle,
        caseId: match.caseId,
        // Provider specific fields not needed here as we are the provider
      }));
      setMatchRequests(requests);

      // Process Assigned Cases
      // Note: Assigned cases DTO structure is different (CaseSummaryDTO)
      // We might need to fetch match details if we want to chat, or just link to case
      const assigned = (data.recentAssignedCases || []).map(c => ({
        id: c.caseId,
        caseId: c.caseId,
        caseTitle: c.title,
        category: c.category,
        status: c.status,
        createdAt: c.createdAt,
        // We don't have matchId here directly from CaseSummaryDTO usually, 
        // but for "Recent Assigned Cases" list we might want to show them.
        // If chat is needed, we need matchId. 
        // For now, we'll just display case info.
      }));
      setAssignedCases(assigned);
      const closedCount = assigned.filter(c => c.status === "CLOSED").length;
      setResolvedCasesCount(closedCount);

    } catch (error) {
      console.error("Failed to fetch dashboard stats:", error);
      showAlert("Failed to load dashboard statistics", "error");
    } finally {
      setLoadingStats(false);
    }
  };

  const handleConfirmMatch = async (matchId) => {
    try {
      // For provider, "Accept" means confirm
      // We need to call the confirm endpoint
      // Assuming confirmMatch is imported from api.js and points to /matches/{id}/confirm
      const { confirmMatch } = require("../../../services/api"); 
      await confirmMatch(matchId);
      showAlert("Match confirmed successfully!", "success");
      fetchDashboardStats(); // Refresh stats
    } catch (error) {
      console.error("Failed to confirm match:", error);
      showAlert(error.response?.data?.message || "Failed to confirm match", "error");
    }
  };

  const handleRejectMatch = async (matchId) => {
    try {
      // For provider reject
      const { rejectProviderMatch } = require("../../../services/api");
      await rejectProviderMatch(matchId);
      showAlert("Match rejected", "success");
      fetchDashboardStats(); // Refresh stats
    } catch (error) {
      console.error("Failed to reject match:", error);
      showAlert(error.response?.data?.message || "Failed to reject match", "error");
    }
  };

  const handleChatClick = (match) => {
    // Navigate to SecureChat with the match ID
    navigate("/dashboard/lawyer/securechat", {
      state: {
        activeMatchId: match.matchId,
        // We might not have full details here, but SecureChat should handle loading by ID
      }
    });
  };

  const handleCaseClick = async (id) => {
    try {
      setCaseLoading(true);
      const response = await getCaseById(id);
      setSelectedCase(response.data);
    } catch (error) {
      console.error("Failed to fetch case details:", error);
      showAlert("Failed to load case details", "error");
    } finally {
      setCaseLoading(false);
    }
  };

  return (
    <div className="p-4 md:p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-semibold">
          Lawyer Dashboard
        </h1>
        <p className="text-sm text-gray-600 mt-1">
          Overview of your match requests and assigned cases.
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          icon={Bell}
          title="Match Requests"
          value={matchRequestsCount}
          subtitle="Pending your action"
        />
        <StatCard
          icon={CheckCircle}
          title="Assigned Cases"
          value={assignedCasesCount}
          subtitle="Active cases"
        />
        <StatCard
          icon={Calendar}
          title="Scheduled Calls"
          value={upcomingCalls}
          subtitle="Upcoming sessions"
        />
        {/* Placeholder for other stats */}
        <StatCard
          icon={TrendingUp}
          title="Resolved Cases"
          value={resolvedCasesCount}
          subtitle="Last 30 days"
        />
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left Column: Recent Activity */}
        <div className="lg:col-span-2 space-y-6">
            
            {/* Recent Match Requests */}
            <div className="bg-white rounded-xl shadow p-4 space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="font-semibold">Recent Match Requests</h2>
                <button 
                    onClick={() => navigate("/dashboard/lawyer/matches")}
                    className="text-sm text-blue-600 hover:underline"
                >
                    View All
                </button>
              </div>

              {loadingStats ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="animate-spin" size={24} color={BLUE} />
                </div>
              ) : matchRequests.length === 0 ? (
                <div className="text-center py-8">
                  <p className="text-sm text-gray-500">No pending match requests.</p>
                </div>
              ) : (
                matchRequests.map((match) => (
                  <MatchItem
                    key={match.id}
                    matchId={match.matchId}
                    caseTitle={match.caseTitle}
                    caseId={match.caseId}
                    compatibility={match.score}
                    matchStatus={match.matchStatus}
                    onAccept={handleConfirmMatch}
                    onReject={handleRejectMatch}
                    onCaseClick={handleCaseClick}
                  />
                ))
              )}
            </div>

            {/* Recent Assigned Cases */}
            <div className="bg-white rounded-xl shadow p-4 space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="font-semibold">Recent Assigned Cases</h2>
                <button 
                    onClick={() => navigate("/dashboard/lawyer/cases")}
                    className="text-sm text-blue-600 hover:underline"
                >
                    View All
                </button>
              </div>

              {loadingStats ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="animate-spin" size={24} color={BLUE} />
                </div>
              ) : assignedCases.length === 0 ? (
                <div className="text-center py-8">
                  <p className="text-sm text-gray-500">No assigned cases yet.</p>
                </div>
              ) : (
                <div className="space-y-3">
                    {assignedCases.map((c) => (
                        <div key={c.caseId} className="p-4 border rounded-xl hover:shadow transition flex justify-between items-center">
                            <div>
                                <h3 className="font-semibold text-sm">{c.caseTitle}</h3>
                                <p className="text-xs text-gray-500">{c.category} • {new Date(c.createdAt).toLocaleDateString()}</p>
                            </div>
                            <button
                                onClick={() => handleCaseClick(c.caseId)}
                                className="text-sm text-blue-600 hover:bg-blue-50 px-3 py-1 rounded-lg transition"
                            >
                                View Details
                            </button>
                        </div>
                    ))}
                </div>
              )}
            </div>

        </div>

        {/* Right Column: Appointments */}
        <div className="bg-white rounded-xl shadow p-4 h-fit">
          <h2 className="font-semibold mb-4">Upcoming Events</h2>

          {loadingAppointments ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="animate-spin" size={24} color={BLUE} />
            </div>
          ) : appointments.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-sm text-gray-500">No upcoming appointments.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {appointments.map((apt) => (
                <div key={apt.id} className="p-3 border rounded-lg hover:bg-gray-50 transition">
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="font-medium text-sm">{apt.lawyerName}</h3>
                    <span className={`text-xs px-2 py-0.5 rounded-full ${
                      apt.status === 'CONFIRMED' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                    }`}>
                      {apt.status}
                    </span>
                  </div>
                  <div className="space-y-1 text-xs text-gray-600">
                    <div className="flex items-center gap-2">
                      <Calendar size={12} />
                      <span>{apt.date}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Clock size={12} />
                      <span>{apt.time}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <FileText size={12} />
                      <span className="truncate">{apt.caseTitle}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Case Modal */}
      {selectedCase && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-hidden relative">
            <button
              onClick={() => setSelectedCase(null)}
              className="absolute top-4 right-4 z-10 text-gray-500 hover:text-gray-800 text-2xl leading-none bg-white rounded-full p-2 hover:bg-gray-100"
            >
              <X size={24} />
            </button>

            {caseLoading ? (
              <div className="flex items-center justify-center py-16">
                <Loader2 className="animate-spin" size={32} color={BLUE} />
                <span className="ml-3 text-gray-600">Loading case details...</span>
              </div>
            ) : (
              <div className="max-h-[90vh] overflow-y-auto p-6">
                <h2 className="text-2xl font-semibold mb-4">{selectedCase.title}</h2>

                <div className="space-y-4">
                  <div className="flex flex-wrap gap-2 mb-4">
                    <span className="px-3 py-1 bg-gray-100 rounded-full text-sm font-medium">
                      {selectedCase.category}
                    </span>
                    <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">
                      {selectedCase.status}
                    </span>
                  </div>

                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">Description</h3>
                    <p className="text-gray-700 whitespace-pre-line">{selectedCase.description}</p>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {selectedCase.location && (
                      <div>
                        <h3 className="font-semibold text-gray-900 mb-1">Location</h3>
                        <p className="text-gray-700">{selectedCase.location}</p>
                      </div>
                    )}
                    {selectedCase.preferredLanguage && (
                      <div>
                        <h3 className="font-semibold text-gray-900 mb-1">Language</h3>
                        <p className="text-gray-700">{selectedCase.preferredLanguage}</p>
                      </div>
                    )}
                  </div>
                  {selectedCase.contactInfo && (
                    <div>
                      <h3 className="font-semibold text-gray-900 mb-1">Contact Info</h3>
                      <p className="text-gray-700">{selectedCase.contactInfo}</p>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
