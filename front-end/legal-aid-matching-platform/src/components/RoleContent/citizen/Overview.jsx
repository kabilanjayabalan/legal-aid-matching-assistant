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
import { getMyMatches, acceptMatch, rejectMatch, getProviderProfile, getCaseById, getMyAppointments, createAppointment, getMyProfile } from "../../../services/api";
import { useAlert } from "../../../context/AlertContext";
import AppointmentScheduler from "./AppointmentScheduler";

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
        <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
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
        text: 'Pending',
        className: 'bg-yellow-100 text-yellow-800 border-yellow-300'
      };
    case 'accepted':
      return {
        text: 'Accepted',
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
  name,
  fullName,
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
}) {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const statusBadge = getStatusBadge(matchStatus);
  const canInteract = matchStatus === 'pending';
  const isAccepted = matchStatus === 'accepted';

  return (
    <div className="flex items-center justify-between p-4 border rounded-xl hover:shadow transition">
      <div className="flex items-center gap-4">
        {/* Avatar */}
        <div className="relative">
          <img
            src={image}
            alt={name}
            className="w-12 h-12 rounded-full object-cover"
            onError={(e) => {
              e.target.src = getDefaultImage(name, type);
            }}
          />
          {online && (
            <span className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-white rounded-full" />
          )}
        </div>

        {/* Info */}
        <div>
          <div className="flex items-center gap-2">
            <p className="text-sm font-semibold">{fullName || name}</p>
            {verified && (
              <span className="flex items-center gap-1 text-xs text-blue-600">
                <CheckCircle size={14} /> Verified
              </span>
            )}
          </div>

          <div className="flex items-center gap-2 mt-1">
            <p className="text-xs text-gray-500">
              {type} • {status}
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
                {compatibility}% Compatibility
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Actions */}
      {/* Actions Dropdown */}
      {/* Actions Dropdown */}
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
                  Accept Match
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
                  Chat
                </button>
                <button
                  onClick={() => onCreateAppointment(
                    {
                      id: providerId,
                      name: fullName || name,
                      type: type,
                      image: image,
                      compatibility: compatibility,
                      matchId: matchId,
                    }
                  )}
                  className="w-full text-left px-4 py-2 text-sm text-indigo-700 hover:bg-purple-50 rounded-lg transition flex items-center gap-2"
                >
                  <Calendar size={14} />
                  Schedule Call
                </button>
              </>
            )}
            <button
              onClick={onProfileClick}
              className="w-full text-left px-4 py-2 text-sm text-blue-950 hover:bg-indigo-50 rounded-lg transition"
            >
              View Profile
            </button>
            {caseTitle && (
              <button
                onClick={() => onCaseClick && onCaseClick(caseId)}
                className="w-full text-left px-4 py-2 text-sm text-blue-950 hover:bg-indigo-50 rounded-lg transition"
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

export default function Overview() {
  const navigate = useNavigate();
  const [matches, setMatches] = useState([]);
  const [loadingMatches, setLoadingMatches] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedProfile, setSelectedProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [selectedCase, setSelectedCase] = useState(null);
  const [caseLoading, setCaseLoading] = useState(false);
  const [showAppointmentScheduler, setShowAppointmentScheduler] = useState(false);
  const [selectedProviderForAppointment, setSelectedProviderForAppointment] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [loadingAppointments, setLoadingAppointments] = useState(true);
  const { showAlert } = useAlert();
  const [activeConversations, setActiveConversations] = useState(0);
  const [upcomingCalls, setUpcomingCalls] = useState(0);
  const [newMatchesThisWeek, setNewMatchesThisWeek] = useState(0);

  const handleCreateAppointment = (provider) => {
    setSelectedProviderForAppointment(provider);
    setShowAppointmentScheduler(true);
  };

  const handleCloseAppointmentScheduler = () => {
    setShowAppointmentScheduler(false);
    setSelectedProviderForAppointment(null);
    fetchMatches(); // Refresh matches after appointment is created/closed
    fetchAppointments(); // Refresh appointments
  };


  useEffect(() => {
    document.title = "Dashboard | Legal Aid";
    fetchMatches();
    fetchAppointments();
  }, [statusFilter]);

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
          lawyerName: `Match ID: ${appt.matchId}`, // Ideally we'd fetch provider name, but matchId works for now
          date: new Date(appt.appointmentDate).toLocaleDateString(),
          time: appt.timeSlot,
          caseTitle: appt.caseTitle || `Duration: ${appt.durationMinutes} mins`,
          caseNumber: appt.caseNumber,
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


  const fetchMatches = async () => {
    try {
      setLoadingMatches(true);
      const response = await getMyMatches();
      // Backend now returns a Spring Page<CitizenMatchDTO>; items are in .content
      const page = response.data || {};
      const matchesData = page.content || [];
      console.log(matchesData);
      const now = new Date();

      // Start of week (Monday)
      const startOfWeek = new Date(now);
      startOfWeek.setDate(now.getDate() - ((now.getDay() + 6) % 7));
      startOfWeek.setHours(0, 0, 0, 0);

      // End of week (Sunday)
      const endOfWeek = new Date(startOfWeek);
      endOfWeek.setDate(startOfWeek.getDate() + 6);
      endOfWeek.setHours(23, 59, 59, 999);

      const weeklyMatchesCount = matchesData.filter(match => {
        if (!match.createdAt) return false;

        const createdAt = new Date(match.createdAt);
        return createdAt >= startOfWeek && createdAt <= endOfWeek;
      }).length;

      setNewMatchesThisWeek(weeklyMatchesCount);

      const confirmedCount = matchesData.filter(
        m => m.status === "PROVIDER_CONFIRMED"
      ).length;

      setActiveConversations(confirmedCount);

      if (!Array.isArray(matchesData)) {
        console.error("Invalid response format - matchesData is not an array:", matchesData);
        setMatches([]);
        return;
      }
      // Filter matches based on status (CITIZEN_ACCEPTED, PROVIDER_CONFIRMED, REJECTED)
      // And apply the user selected filter
      const filteredMatches = matchesData.filter(match => {
        // Handle both string and enum status values (Spring serializes enums as strings)
        const status = match.status;
        const statusStr = typeof status === 'string' ? status : (status?.toString() || '');

        const isRelevantStatus = ['CITIZEN_ACCEPTED', 'PROVIDER_CONFIRMED', 'REJECTED'].includes(statusStr);

        if (!isRelevantStatus) return false;

        if (statusFilter === 'ALL') return true;
        if (statusFilter === 'ACCEPTED') return ['CITIZEN_ACCEPTED', 'PROVIDER_CONFIRMED'].includes(statusStr);
        if (statusFilter === 'REJECTED') return statusStr === 'REJECTED';

        return true;
      });

      // Transform matches to display format and fetch provider details
      const transformedMatchesPromises = filteredMatches.map(async (match) => {
        // Access fields directly from JSON response
        const matchId = match.matchId;
        const providerType = match.providerType;
        const providerId = match.providerId;
        const status = match.status;
        const createdAt = match.createdAt;

        const score = match.score || 0;
        const caseTitle = match.caseTitle;
        const caseId = match.caseId;

        if (!matchId || !providerType || !providerId) {
          console.warn("Invalid match data:", match);
          return null;
        }

        // Fetch provider details from directory endpoint
        let fullName = null;
        let verified = false;
        try {
          const providerResponse = await getProviderProfile(providerId, providerType);
          if (providerResponse.data) {
            fullName = providerResponse.data.name || providerResponse.data.fullName;
            verified = providerResponse.data.verified || false;
          }
        } catch (error) {
          console.warn(`Failed to fetch provider details for ${providerType} ${providerId}:`, error);
        }

        const providerName = providerType === "LAWYER"
          ? `Lawyer #${providerId}`
          : `NGO #${providerId}`;

        return {
          id: matchId, // Use matchId as id since DTO doesn't have separate id field
          matchId: matchId,
          name: providerName,
          fullName: fullName,
          type: providerType === "LAWYER" ? "Lawyer" : "NGO",
          providerType: providerType,
          providerId: providerId,
          status: providerType === "LAWYER" ? "Legal Professional" : "Legal Aid Organization",
          image: getDefaultImage(fullName || providerName, providerType),
          compatibility: score,
          verified: verified,
          online: false,
          matchStatus: mapStatus(status),
          score: score,
          createdAt: createdAt,
          caseTitle: caseTitle,
          caseId: caseId,
        };
      });

      const transformedMatches = (await Promise.all(transformedMatchesPromises))
        .filter(match => match !== null); // Remove any null entries from invalid data

      // Sort by created date (most recent first) and limit to 4
      const sortedMatches = transformedMatches
        .sort((a, b) => {
          const dateA = a.createdAt ? new Date(a.createdAt) : new Date(0);
          const dateB = b.createdAt ? new Date(b.createdAt) : new Date(0);
          return dateB - dateA;
        })
        .slice(0, 4);

      setMatches(sortedMatches);
    } catch (error) {
      console.error("Failed to fetch matches:", error);
      console.error("Error details:", {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
      });
      showAlert(
        error.response?.data?.message ||
        error.message ||
        "Failed to load matches. Please try again.",
        "error"
      );
      setMatches([]); // Set empty array on error
    } finally {
      setLoadingMatches(false);
    }
  };

  const handleAcceptMatch = async (matchId) => {
    try {
      await acceptMatch(matchId);
      showAlert("Match accepted successfully!", "success");
      fetchMatches(); // Refresh matches
    } catch (error) {
      console.error("Failed to accept match:", error);
      showAlert(error.response?.data?.message || "Failed to accept match", "error");
    }
  };

  const handleRejectMatch = async (matchId) => {
    try {
      await rejectMatch(matchId);
      showAlert("Match rejected", "success");
      fetchMatches(); // Refresh matches
    } catch (error) {
      console.error("Failed to reject match:", error);
      showAlert(error.response?.data?.message || "Failed to reject match", "error");
    }
  };

  const handleChatClick = (match) => {
    // Navigate to SecureChat with the match ID and provider info
    navigate("/dashboard/citizen/securechat", {
      state: {
        activeMatchId: match.matchId,
        newChat: {
          id: match.matchId,
          name: match.name,
          role: match.type,
          providerId: match.providerId,
          providerType: match.providerType,
        }
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

  const handleProfileClick = async (match) => {
    if (!match.providerId || !match.providerType) {
      showAlert("Profile information not available", "error");
      return;
    }

    try {
      setProfileLoading(true);
      const response = await getProviderProfile(match.providerId, match.providerType);
      setSelectedProfile({
        ...response.data,
        providerType: match.providerType,
        matchId: match.matchId,
      });
    } catch (error) {
      console.error("Failed to fetch profile:", error);
      showAlert("Failed to load profile details", "error");
    } finally {
      setProfileLoading(false);
    }
  };

  return (
    <div className="p-4 md:p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-semibold">
           DASHBOARD
        </h1>
        <p className="text-sm text-gray-600 mt-1">
          Overview of your recent legal aid activity and matching metrics.
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <StatCard
          icon={Bell}
          title="New Matches"
          value={newMatchesThisWeek > 0 ? newMatchesThisWeek : "No new matches"}
          subtitle="This week"
        />
        <StatCard
          icon={MessageSquare}
          title="Active Conversations"
          value={activeConversations}
          subtitle="With lawyers / NGOs"
        />
        <StatCard
          icon={Calendar}
          title="Scheduled Calls"
          value={upcomingCalls > 0 ? upcomingCalls : "No scheduled calls"}
          subtitle="Upcoming sessions"
        />
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Matches */}
        <div className="lg:col-span-2 bg-white rounded-xl shadow p-4 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">Recent Matches</h2>

            {/* Filter Dropdown */}
            <div className="relative inline-block text-left">
              <div className="flex items-center gap-2">
                <Filter size={16} className="text-gray-500" />
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="text-sm border-gray-300 rounded-md shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50 p-1 border"
                >
                  <option value="ALL">All Statuses</option>
                  <option value="ACCEPTED">Accepted</option>
                  <option value="REJECTED">Rejected</option>
                </select>
              </div>
            </div>
          </div>

          {loadingMatches ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="animate-spin  bg-blue-950" size={24}/>
              <span className="ml-2 text-sm text-gray-600">Loading matches...</span>
            </div>
          ) : matches.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-sm text-gray-500">No matches found matching your criteria.</p>
            </div>
          ) : (
            matches.map((match) => (
              <MatchItem
                key={match.id}
                matchId={match.matchId}
                name={match.name}
                fullName={match.fullName}
                type={match.type}
                status={match.status}
                image={match.image}
                compatibility={match.compatibility}
                verified={match.verified}
                online={match.online}
                matchStatus={match.matchStatus}
                providerId={match.providerId}
                providerType={match.providerType}
                onProfileClick={() => handleProfileClick(match)}
                onAccept={handleAcceptMatch}
                onReject={handleRejectMatch}

                onChat={() => handleChatClick(match)}
                caseTitle={match.caseTitle}
                caseId={match.caseId}
                onCaseClick={handleCaseClick}
                onCreateAppointment={handleCreateAppointment}
              />
            ))
          )}
        </div>

        {/* My Appointments */}
        <div className="bg-white rounded-xl shadow p-4">
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
                    <div className="flex-1">
                      {apt.caseNumber && (
                        <div className="mb-1 inline-block rounded-md bg-blue-50 px-2 py-0.5 text-xs font-semibold text-blue-700">
                          {apt.caseNumber}
                        </div>
                      )}
                      <h3 className="font-medium text-sm">{apt.lawyerName}</h3>
                    </div>
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

      {/* Profile Modal */}
      {selectedProfile && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-hidden relative">
            <button
              onClick={() => setSelectedProfile(null)}
              className="absolute top-4 right-4 z-10 text-gray-500 hover:text-gray-800 text-2xl leading-none bg-white rounded-full p-2 hover:bg-gray-100"
            >
              <X size={24} />
            </button>

            {profileLoading ? (
              <div className="flex items-center justify-center py-16">
                <Loader2 className="animate-spin" size={32} color={BLUE} />
                <span className="ml-3 text-gray-600">Loading profile...</span>
              </div>
            ) : (
              <div className="max-h-[90vh] overflow-y-auto p-6">
                <div className="flex items-start gap-4 mb-6">
                  <img
                    src={getDefaultImage(selectedProfile.name || selectedProfile.ngoName || "Provider", selectedProfile.providerType)}
                    alt={selectedProfile.name || selectedProfile.ngoName}
                    className="w-20 h-20 rounded-full object-cover"
                  />
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <h2 className="text-2xl font-semibold">
                        {selectedProfile.name || selectedProfile.ngoName || "Provider"}
                      </h2>
                      {selectedProfile.verified && (
                        <CheckCircle size={20} color={BLUE} />
                      )}
                    </div>
                    <p className="text-gray-600">
                      {selectedProfile.providerType === "LAWYER" ? "Legal Professional" : "Legal Aid Organization"}
                    </p>
                  </div>
                </div>

                <div className="space-y-4">
                  {selectedProfile.city && (
                    <div className="flex items-center gap-2 text-gray-700">
                      <MapPin size={18} />
                      <span>{selectedProfile.city}</span>
                    </div>
                  )}

                  {selectedProfile.contactInfo && (
                    <div className="flex items-start gap-2 text-gray-700">
                      <Phone size={18} className="mt-1" />
                      <span className="whitespace-pre-line">{selectedProfile.contactInfo}</span>
                    </div>
                  )}

                  {selectedProfile.website && (
                    <div className="flex items-center gap-2 text-gray-700">
                      <Globe size={18} />
                      <a href={selectedProfile.website} target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:underline">
                        {selectedProfile.website}
                      </a>
                    </div>
                  )}

                  {(selectedProfile.description || selectedProfile.bio || selectedProfile.expertise) && (
                    <div>
                      <h3 className="font-semibold mb-2 flex items-center gap-2">
                        <FileText size={18} />
                        About
                      </h3>
                      <p className="text-gray-700 whitespace-pre-line">
                        {selectedProfile.description || selectedProfile.bio || selectedProfile.expertise}
                      </p>
                    </div>
                  )}

                  {selectedProfile.language && (
                    <div>
                      <h3 className="font-semibold mb-2">Languages</h3>
                      <p className="text-gray-700">{selectedProfile.language}</p>
                    </div>
                  )}

                  {selectedProfile.providerType === "LAWYER" && selectedProfile.specialization && (
                    <div>
                      <h3 className="font-semibold mb-2">Specializations</h3>
                      <div className="flex flex-wrap gap-2">
                        {selectedProfile.specialization.split(',').map((spec, idx) => (
                          <span key={idx} className="px-3 py-1 bg-gray-100 rounded-full text-sm">
                            {spec.trim()}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {selectedProfile.providerType === "NGO" && selectedProfile.focusArea && (
                    <div>
                      <h3 className="font-semibold mb-2">Focus Areas</h3>
                      <div className="flex flex-wrap gap-2">
                        {selectedProfile.focusArea.split(',').map((area, idx) => (
                          <span key={idx} className="px-3 py-1 bg-gray-100 rounded-full text-sm">
                            {area.trim()}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {selectedProfile.providerType === "LAWYER" && selectedProfile.experienceYears && (
                    <div>
                      <h3 className="font-semibold mb-2">Experience</h3>
                      <p className="text-gray-700">{selectedProfile.experienceYears} years</p>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

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

      {/* Appointment Scheduler Modal */}
      {showAppointmentScheduler && selectedProviderForAppointment && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden relative">
            <button
              onClick={handleCloseAppointmentScheduler}
              className="absolute top-4 right-4 z-10 text-gray-500 hover:text-gray-800 text-2xl leading-none bg-white rounded-full p-2 hover:bg-gray-100"
            >
              <X size={24} />
            </button>
            <div className="max-h-[90vh] overflow-y-auto">
              <AppointmentScheduler
                lawyer={selectedProviderForAppointment}
                isModal={true}
                onClose={handleCloseAppointmentScheduler}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
