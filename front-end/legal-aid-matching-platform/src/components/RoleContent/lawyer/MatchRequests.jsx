import React, { useState, useEffect } from "react";
import {
    FileText,
    CheckCircle,
    Clock,
    AlertCircle,
    Loader2,
    X
} from "lucide-react";
import { getMyMatchRequests, confirmMatch, getCaseById ,rejectProviderMatch} from "../../../services/api";
import { useAlert } from "../../../context/AlertContext";

const BLUE = "#6610f2";

export default function MatchRequests() {
    const [requests, setRequests] = useState([]);
    const [selectedRequest, setSelectedRequest] = useState(null);
    const [selectedCaseDetails, setSelectedCaseDetails] = useState(null);
    const [loadingCaseDetails, setLoadingCaseDetails] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [confirmingId, setConfirmingId] = useState(null);
    const { showAlert } = useAlert();

    // Fetch match requests on mount
    useEffect(() => {
        fetchMatchRequests();
    }, []);

    const fetchMatchRequests = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await getMyMatchRequests(0, 100); // Fetch up to 100 requests
            // Backend now returns a Spring Page<MatchSummaryDTO>; items are in .content
            const page = response.data || {};
            const requestsData = page.content || [];
            console.log("Fetched match requests:", requestsData);
            setRequests(requestsData);
        } catch (err) {
            console.error("Error fetching match requests:", err);
            const errorMsg = err.response?.data?.message || "Failed to fetch match requests";
            setError(errorMsg);
            showAlert(errorMsg, "error");
        } finally {
            setLoading(false);
        }
    };

    const fetchCaseDetails = async (caseId) => {
        try {
            setLoadingCaseDetails(true);
            const response = await getCaseById(caseId);
            setSelectedCaseDetails(response.data);
        } catch (err) {
            console.error("Error fetching case details:", err);
            setSelectedCaseDetails(null);
        } finally {
            setLoadingCaseDetails(false);
        }
    };

    const handleConfirmMatch = async (matchId) => {
        try {
            setConfirmingId(matchId);
            console.log("Confirming match:", matchId);
            const response = await confirmMatch(matchId);
            console.log("Match confirmed:", response.data);

            // Update local state
            setRequests(requests.filter(req => req.id !== matchId));
            setSelectedRequest(null);
            setSelectedCaseDetails(null);
            showAlert("Match confirmed successfully!", "success");
        } catch (err) {
            console.error("Error confirming match:", err);
            const errorMsg = err.response?.data?.message || "Failed to confirm match";
            showAlert(errorMsg, "error");
        } finally {
            setConfirmingId(null);
        }
    };
    const handleRejectMatch = async (matchId) => {
  try {
    setConfirmingId(matchId); // reuse loader state
    const response = await rejectProviderMatch(matchId);

    // Remove rejected request from list
    setRequests(requests.filter(req => req.id !== matchId));

    setSelectedRequest(null);
    setSelectedCaseDetails(null);

    showAlert("Match rejected successfully", "success");
  } catch (err) {
    console.error("Error rejecting match:", err);
    const errorMsg = err.response?.data?.message || "Failed to reject match";
    showAlert(errorMsg, "error");
  } finally {
    setConfirmingId(null);
  }
};


    const getStatusColor = (matchStatus) => {
        switch (matchStatus) {
            case "CITIZEN_ACCEPTED":
                return "bg-blue-100 text-blue-800";
            case "PENDING":
                return "bg-yellow-100 text-yellow-800";
            case "REJECTED":
                return "bg-red-100 text-red-800";
            case "PROVIDER_CONFIRMED":
                return "bg-green-100 text-green-800";
            default:
                return "bg-gray-100 text-gray-800";
        }
    };

    const getStatusLabel = (matchStatus) => {
        switch (matchStatus) {
            case "CITIZEN_ACCEPTED":
                return "Citizen Accepted";
            case "PENDING":
                return "Pending";
            case "REJECTED":
                return "Rejected";
            case "PROVIDER_CONFIRMED":
                return "Confirmed";
            default:
                return status;
        }
    };

    const handleCloseModal = () => {
        setSelectedRequest(null);
        setSelectedCaseDetails(null);
    };

    return (
        <div className="relative min-h-full overflow-x-hidden bg-slate-50 px-4 py-4 sm:p-4">
            <div className="mb-6">
                <h1 className="text-xl font-bold text-gray-900 sm:text-2xl">Match Requests</h1>
                <p className="mt-1 text-sm text-gray-500">Review and confirm cases sent by clients</p>
            </div>

            {/* Loading State */}
            {loading && (
                <div className="flex items-center justify-center py-12">
                    <Loader2 size={40} className="text-blue-600 animate-spin" />
                    <p className="ml-4 text-gray-600">Loading match requests...</p>
                </div>
            )}

            {/* Error State */}
            {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 flex items-start gap-3">
                    <AlertCircle size={20} className="text-red-600 mt-0.5 flex-shrink-0" />
                    <div>
                        <h3 className="font-semibold text-red-900">Error</h3>
                        <p className="text-red-700 text-sm">{error}</p>
                    </div>
                </div>
            )}

            {/* Empty State */}
            {!loading && requests.length === 0 && !error && (
                <div className="bg-white rounded-xl border border-gray-200 p-12 text-center shadow-sm">
                    <FileText size={48} className="mx-auto text-gray-400 mb-4" />
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">No Match Requests</h3>
                    <p className="text-gray-600">You don't have any pending match requests at the moment.</p>
                </div>
            )}

            {/* Match Requests List */}
            {!loading && requests.length > 0 && (
                <div className="space-y-4">
                    {requests.map((request) => (
                        <div
                            key={request.id}
                            onClick={() => {
                                setSelectedRequest(request);
                                // Fetch case details when request is selected
                                if (request.caseId) {
                                    fetchCaseDetails(request.caseId);
                                }
                            }}
                            className="cursor-pointer rounded-xl border bg-white p-4 shadow-sm transition hover:border-blue-300 hover:shadow-md"
                        >
                            <div className="flex items-start justify-between mb-3">
                                <div className="flex-1">
                                    <div className="flex items-center gap-2 mb-1">
                                        <h3 className="font-semibold text-gray-900 text-lg">
                                            {request.caseTitle || "Case"}
                                        </h3>
                                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(request.matchStatus)}`}>
                                            {getStatusLabel(request.matchStatus)}
                                        </span>
                                    </div>
                                </div>
                                <div className="text-right">
                                    <div className="text-2xl font-bold" style={{ color: BLUE }}>
                                        {request.score || 0}%
                                    </div>
                                    <p className="text-xs text-gray-500">Match Score</p>
                                </div>
                            </div>

                            <div className="space-y-2 text-sm">
                                <div className="flex items-center gap-2 text-gray-600">
                                    <Clock size={16} />
                                    <span>Received {formatDate(request.createdAt)}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Modal Popup */}
            {selectedRequest && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
                    <div className="w-full max-w-2xl overflow-hidden rounded-xl bg-white shadow-2xl">
                        {/* Modal Header */}
                        <div className="flex items-center justify-between border-b px-6 py-4">
                            <h2 className="text-lg font-bold text-gray-900">
                                Request Details
                            </h2>
                            <button
                                onClick={handleCloseModal}
                                className="rounded-full p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                            >
                                <X className="h-5 w-5" />
                            </button>
                        </div>

                        {/* Modal Body */}
                        <div className="max-h-[70vh] overflow-y-auto p-6">
                            {/* Case Title */}
                            <div className="mb-6">
                                <p className="text-sm font-semibold text-gray-600 mb-1">Case Title</p>
                                <p className="text-gray-900 font-medium">
                                    {selectedRequest.caseTitle || "N/A"}
                                </p>
                            </div>

                            {/* Loading Case Details */}
                            {loadingCaseDetails && (
                                <div className="mb-6 pb-6 border-b">
                                    <p className="text-sm text-gray-500">Loading case details...</p>
                                </div>
                            )}

                            {/* Client Info */}
                            {selectedCaseDetails && !loadingCaseDetails && (
                                <div className="mb-6 pb-6 border-b">
                                    <p className="text-sm font-semibold text-gray-600 mb-3">Client Information</p>
                                    <div className="space-y-2">
                                        <div>
                                            <p className="text-xs text-gray-500">Email</p>
                                            <p className="text-gray-900 font-medium">
                                                {selectedCaseDetails.createdByEmail || "Not provided"}
                                            </p>
                                        </div>
                                        {selectedCaseDetails.location && (
                                            <div>
                                                <p className="text-xs text-gray-500">Location</p>
                                                <p className="text-gray-900 font-medium">
                                                    {selectedCaseDetails.location}
                                                </p>
                                            </div>
                                        )}
                                        {selectedCaseDetails.city && (
                                            <div>
                                                <p className="text-xs text-gray-500">City</p>
                                                <p className="text-gray-900 font-medium">
                                                    {selectedCaseDetails.city}
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            )}

                            {/* Case Category */}
                            {selectedCaseDetails && (
                                <div className="mb-6 pb-6 border-b">
                                    <p className="text-sm font-semibold text-gray-600 mb-1">Category</p>
                                    <p className="text-gray-900 font-medium">
                                        {selectedCaseDetails.category || "General"}
                                    </p>
                                </div>
                            )}

                            {/* Match Score */}
                            <div className="mb-6 pb-6 border-b">
                                <p className="text-sm font-semibold text-gray-600 mb-2">Match Score</p>
                                <div className="flex items-center gap-3">
                                    <div className="flex-1 bg-gray-200 rounded-full h-2">
                                        <div
                                            className="bg-blue-600 h-2 rounded-full"
                                            style={{ width: `${selectedRequest.score || 0}%` }}
                                        ></div>
                                    </div>
                                    <span className="font-bold text-lg" style={{ color: BLUE }}>
                                        {selectedRequest.score || 0}%
                                    </span>
                                </div>
                            </div>

                            {/* Status */}
                            <div className="mb-6 pb-6 border-b">
                                <p className="text-sm font-semibold text-gray-600 mb-1">Status</p>
                                <span className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(selectedRequest.matchStatus)}`}>
                                    {getStatusLabel(selectedRequest.matchStatus)}
                                </span>
                            </div>

                            {/* Case Description */}
                            {selectedCaseDetails && (
                                <div className="mb-6">
                                    <p className="text-sm font-semibold text-gray-600 mb-2">Case Description</p>
                                    <p className="text-gray-700 text-sm leading-relaxed">
                                        {selectedCaseDetails.description || "No description provided"}
                                    </p>
                                </div>
                            )}
                        </div>

                        {/* Modal Footer */}
                        <div className="border-t bg-gray-50 px-6 py-4 flex justify-end gap-3">
                            <button
                                onClick={handleCloseModal}
                                className="rounded-lg border bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
                            >
                                Close
                            </button>
                            {selectedRequest.matchStatus === "CITIZEN_ACCEPTED" && (
                                <>
                                {/* Reject Match */}
                                <button
                                    onClick={() => handleRejectMatch(selectedRequest.id)}
                                    disabled={confirmingId === selectedRequest.id}
                                    className="flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {confirmingId === selectedRequest.id ? (
                                    <>
                                    <Loader2 size={16} className="animate-spin" />
                                        Rejecting...
                                    </>
                                    ) : (
                                    <>
                                    <X size={16} />
                                        Reject Match
                                    </>
                                    )}
                                </button>

                                {/* Confirm Match */}
                                <button
                                    onClick={() => handleConfirmMatch(selectedRequest.id)}
                                    disabled={confirmingId === selectedRequest.id}
                                    className="flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {confirmingId === selectedRequest.id ? (
                                        <>
                                        <Loader2 size={16} className="animate-spin" />
                                            Confirming...
                                        </>
                                    ) : (
                                        <>
                                        <CheckCircle size={16} />
                                            Confirm Match
                                        </>
                                    )}
                                </button>
                            </>
                        )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

// Helper function to format date
function formatDate(dateString) {
    if (!dateString) return "";
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return "just now";
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;

    return date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}
