import React, { useState, useEffect } from "react";
import {
    MapPin,
    Mail,
    Phone,
    MessageSquare,
    Calendar,
    CheckCircle,
    FileText,
    Loader2,
    AlertCircle,
    Heart,
    Filter,
} from "lucide-react";
import AppointmentScheduler from "./AppointmentScheduler";
import { useNavigate } from "react-router-dom";
import { getMyCases, generateMatches, acceptMatch, rejectMatch, saveProfile, unsaveProfile, getSavedProfiles } from "../../../services/api";
import { useAlert } from "../../../context/AlertContext";

const BLUE = "#6610f2";

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

// Map backend MatchCard to frontend profile format
const mapMatchCardToProfile = (matchCard, index) => {
    const areas = matchCard.expertise
        ? matchCard.expertise.split(',').map(a => a.trim()).filter(a => a)
        : [];

    return {
        id: matchCard.matchId || `temp-${index}`,
        matchId: matchCard.matchId,
        matchStatus: matchCard.matchStatus,
        caseStatus: matchCard.caseStatus,
        type: matchCard.providerType === "LAWYER" ? "Lawyer" : "NGO",
        providerType: matchCard.providerType,
        verified: matchCard.verified || false,
        rating: 4.5, // Default rating since backend doesn't provide this
        image: getDefaultImage(matchCard.name, matchCard.providerType),
        name: matchCard.name,
        location: matchCard.city || "Location not specified",
        email: "", // Not provided in MatchCard
        phone: "", // Not provided in MatchCard
        areas: areas.length > 0 ? areas : [matchCard.expertise || "General Legal Services"],
        availability: "Available for consultations",
        compatibility: `${matchCard.score}%`,
        score: matchCard.score,
        experience: matchCard.expertise || "Experienced legal professional",
        activity: [
            "Active on platform",
            matchCard.verified ? "Verified professional" : "Unverified",
        ],
        documents: [],
        source: matchCard.source,
        canInteract: matchCard.canInteract,
    };
};

export default function ProfilesPage() {
    const [cases, setCases] = useState([]);
    const [selectedCaseId, setSelectedCaseId] = useState("");
    const [profiles, setProfiles] = useState([]);
    const [selectedProfile, setSelectedProfile] = useState(null);
    const [showScheduler, setShowScheduler] = useState(false);
    const [loading, setLoading] = useState(true);
    const [loadingMatches, setLoadingMatches] = useState(false);
    const [error, setError] = useState("");
    const [savedMatchIds, setSavedMatchIds] = useState(new Set());
    const [showRegisteredOnly, setShowRegisteredOnly] = useState(false);
    const { showAlert } = useAlert();
    const navigate = useNavigate();
    const [selectedMatchId, setSelectedMatchId] = useState(null);
    const [sensitivity, setSensitivity] = useState(40);
    const [debouncedSensitivity, setDebouncedSensitivity] = useState(sensitivity);

    useEffect(() => {
        const t = setTimeout(() => {
            setDebouncedSensitivity(sensitivity);
        }, 400);

        return () => clearTimeout(t);
    }, [sensitivity]);

    useEffect(() => {
        setSelectedProfile(null);
        setSelectedMatchId(null);
    }, [debouncedSensitivity]);


    useEffect(() => {
        const fetchSavedProfiles = async () => {
            try {
                const response = await getSavedProfiles();
                const page = response.data || {};
                const savedMatches = page.content || [];
                const savedIds = new Set(savedMatches.map(match => match.matchId));
                setSavedMatchIds(savedIds);
            } catch (err) {
                console.error("Failed to fetch saved profiles:", err);
            }
    };

        fetchSavedProfiles();
    }, []);

    // Fetch cases on mount
    useEffect(() => {
        const fetchCases = async () => {
            try {
                setLoading(true);
                setError("");
                const response = await getMyCases();
                const casesData = response.data || [];
                setCases(casesData);

                if (casesData.length > 0&& !selectedCaseId) {
                    setSelectedCaseId(casesData[0].id);
                }
            } catch (err) {
                console.error("Failed to fetch cases:", err);
                setError("Failed to load your cases. Please try again.");
                showAlert("Failed to load cases", "error");
            } finally {
                setLoading(false);
            }
        };

        fetchCases();
    }, []);

    // Fetch matches when case is selected
    useEffect(() => {
        if (!selectedCaseId) {
            setProfiles([]);
            setSelectedProfile(null);
            return;
        }

        const fetchMatches = async () => {
            try {
                if (profiles.length === 0) {
                    setLoadingMatches(true); // only first load
                }

                setError("");
                
                const matchesResponse = await generateMatches(
                    Number(selectedCaseId) ,debouncedSensitivity
                );
                const matchCards = matchesResponse.data?.results || [];

                const mappedProfiles = matchCards.map((card, index) =>
                    mapMatchCardToProfile(card, index)
                );

                // Sort profiles by compatibility score in descending order
                const sortedProfiles = mappedProfiles.sort((a, b) => {
                    const scoreA = a.score || 0;
                    const scoreB = b.score || 0;
                    return scoreB - scoreA; // Descending order
                });

                setProfiles(sortedProfiles);
                // Auto-select first profile if available (highest compatibility)
                if (sortedProfiles.length > 0) {
                const preserved = selectedMatchId
                        ? sortedProfiles.find(p => p.matchId === selectedMatchId)
                        : null;

                    const registeredProfiles = sortedProfiles.filter(
                        p => p.source === "REGISTERED"
                    );

                    const bestRegistered = registeredProfiles.sort(
                        (a, b) => (b.score || 0) - (a.score || 0)
                    )[0];
                    setSelectedProfile(
                        preserved || bestRegistered || sortedProfiles[0]
                    );
                }
            } catch (err) {
                console.error("Failed to fetch matches:", err);
                setError("Failed to load matches. Please try again.");
                showAlert("Failed to load matches", "error");
                setProfiles([]);
                setSelectedProfile(null);
            } finally {
                setLoadingMatches(false);
            }
        };

        fetchMatches();
    }, [selectedCaseId, debouncedSensitivity]);

        // Handle selection when filter changes
    useEffect(() => {
        if (showRegisteredOnly) {
            const filtered = profiles.filter(p => p.source === "REGISTERED");
            if (selectedProfile && !filtered.some(p => p.id === selectedProfile.id)) {return;}
                
            setSelectedProfile(filtered.length > 0 ? filtered[0] : null);
        }
    }, [showRegisteredOnly, profiles]);

    const displayedProfiles = showRegisteredOnly
        ? profiles.filter(p => p.source === "REGISTERED")
        : profiles;
    const matchStatus = selectedProfile?.matchStatus;
    const caseStatus = selectedProfile?.caseStatus;

    let acceptText = "Accept Match";
    let acceptDisabled = false;

    
    if (caseStatus === "CLOSED") {// Case resolved
        acceptText = "Case Resolved";
        acceptDisabled = true;
    }  
    else if (caseStatus === "IN_PROGRESS") {// Case assigned
        if (matchStatus === "PROVIDER_CONFIRMED") {
            acceptText = "Match Confirmed";   
        } else {
            acceptText = "Already Assigned";
        }
        acceptDisabled = true;
    }   
    else if (matchStatus === "CITIZEN_ACCEPTED") {// Citizen already accepted
        acceptText = "Accepted";
        acceptDisabled = true;
    }else if (matchStatus === "REJECTED") {// Match rejected
        acceptText = "Rejected";
        acceptDisabled = true;
    }
    const handleAcceptMatch = async (matchId) => {
        if (!matchId) {
            showAlert("Cannot accept this match", "error");
            return;
        }

        try {
            await acceptMatch(matchId);
            showAlert("Match accepted successfully!", "success");

                        setProfiles(prev =>
                prev.map(p =>
                p.matchId === matchId
                    ? { ...p, canInteract: true, accepted: true }
                    : p
                )
            );

            setSelectedProfile(prev =>
                prev && prev.matchId === matchId
                ? { ...prev, canInteract: true, accepted: true }
                : prev
            );
        } catch (err) {
            console.error("Failed to accept match:", err);
            showAlert(err.response?.data?.message || "Failed to accept match", "error");
        }
    };

    const handleRejectMatch = async (matchId) => {
        if (!matchId) {
            showAlert("Cannot reject this match", "error");
            return;
        }

        try {
            await rejectMatch(matchId);
            showAlert("Match rejected", "success");
            // Remove rejected match from list
            setProfiles(prev => prev.filter(p => p.matchId !== matchId));
            // Select another profile if current one was rejected
            if (selectedProfile?.matchId === matchId) {
                const remaining = profiles.filter(p => p.matchId !== matchId);
                setSelectedProfile(remaining.length > 0 ? remaining[0] : null);
            }
        } catch (err) {
            console.error("Failed to reject match:", err);
            showAlert(err.response?.data?.message || "Failed to reject match", "error");
        }
    };

    const handleMessageClick = () => {
        navigate("/dashboard/citizen/securechat", {
            state: {
                newChat: {
                    id: selectedProfile.id,
                    name: selectedProfile.name,
                    role: selectedProfile.type,
                    isOnline: true, // Assuming online for now
                },
            },
        });
    };

        const handleAddToShortlist = async () => {
        if (!selectedProfile?.matchId) {
            showAlert("Cannot save this profile - no match ID available", "error");
            return;
        }

        const matchId = selectedProfile.matchId;
        const isCurrentlySaved = savedMatchIds.has(matchId);

        try {
            if (isCurrentlySaved) {
                await unsaveProfile(matchId);
                setSavedMatchIds(prev => {
                    const newSet = new Set(prev);
                    newSet.delete(matchId);
                    return newSet;
                });
                showAlert("Profile removed from saved", "success");
            } else {
                await saveProfile(matchId);
                setSavedMatchIds(prev => new Set(prev).add(matchId));
                showAlert("Profile saved to shortlist", "success");
            }
        } catch (err) {
            console.error("Failed to save/unsave profile:", err);
            showAlert(err.response?.data?.message || "Failed to update saved status", "error");
        }
    };

    const isSaved = selectedProfile?.matchId ? savedMatchIds.has(selectedProfile.matchId) : false;

    return (
        <div className="min-h-screen bg-gray-100 p-4 md:p-6">
            <h1 className="text-2xl font-semibold mb-4">Find Legal Help</h1>

            {loading ? (
                <div className="flex items-center justify-center py-8">
                    <Loader2 className="animate-spin" size={26} color={BLUE} />
                    <span className="ml-3 text-base font-medium text-gray-600">Loading your cases…</span>
                </div>
            ) : cases.length === 0 ? (
                <div className="bg-white rounded-2xl shadow-xl p-8 text-center">
                    <AlertCircle className="mx-auto mb-4" size={60} color="#9ca3af" />
                    <h3 className="text-lg font-semibold text-gray-800 mb-2">No Cases Found</h3>
                    <p className="text-gray-500">You don't have any cases yet. Please submit your first case to find legal help.</p>
                </div>
            ) : (
                <>
                    <div className="mb-6">
                        <label className="block text-sm font-semibold text-gray-700 mb-3">
                            Select Case
                        </label>
                        <div className="flex gap-4 items-center">
                            <div className="relative flex-1">
                                <div className="absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none">
                                    <FileText size={18} color="#6b7280" />
                                </div>
                                <select
                                    value={selectedCaseId || ""}
                                    onChange={(e) => setSelectedCaseId(e.target.value)}
                                    className="w-full pl-12 pr-10 py-3.5 bg-white border-2 border-gray-200 rounded-xl text-gray-800 font-medium shadow-sm hover:border-purple-300 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all duration-200 appearance-none cursor-pointer"
                                    style={{
                                        backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E")`,
                                        backgroundRepeat: 'no-repeat',
                                        backgroundPosition: 'right 0.75rem center',
                                        backgroundSize: '1.25rem'
                                    }}
                                >
                                    {cases.map((caseItem) => (
                                        <option key={caseItem.id} value={caseItem.id}>
                                            {caseItem.title} - {caseItem.status}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <button
                                onClick={() => setShowRegisteredOnly(!showRegisteredOnly)}
                                className={`flex items-center gap-2 px-4 py-3.5 rounded-xl font-medium transition-colors border-2 ${
                                    showRegisteredOnly
                                        ? "bg-purple-100 border-purple-500 text-purple-700"
                                        : "bg-white border-gray-200 text-gray-700 hover:border-purple-300"
                                }`}
                                title="Show only registered professionals"
                            >
                                <Filter size={18} />
                                <span className="whitespace-nowrap">{showRegisteredOnly ? "Registered Only" : "All Sources"}</span>
                            </button>
                        </div>
                    </div>

                     {error && (
                        <div className="mb-4 p-4 bg-red-50 border-l-4 border-red-400 rounded-lg text-red-700 text-sm flex items-start gap-3 shadow-sm">
                            <AlertCircle size={20} className="mt-0.5 flex-shrink-0" />
                            <span>{error}</span>
                        </div>
                    )}

                    {displayedProfiles.length === 0 ? (
                        <div className="bg-white rounded-xl shadow p-6 text-center">  
                            <AlertCircle className="mx-auto mb-2" size={48} color="#9ca3af" />
                                                        <p className="text-gray-600">
                                {profiles.length > 0 
                                    ? "No registered professionals found for this case." 
                                    : "No matches found for this case."}
                            </p>
                            <p className="text-sm text-gray-500 mt-1">
                                {profiles.length > 0 
                                    ? "Try turning off the filter to see more results." 
                                    : "Try generating matches or check back later."}
                            </p>
                        </div>
                    ) : (
                        <div className="relative">
                            {loadingMatches && (
                                <div className="absolute inset-0 bg-white/60 z-20 flex items-center justify-center rounded-xl">
                                    <Loader2 className="animate-spin" size={28} color={BLUE} />
                                    <span className="ml-3 font-medium text-gray-600">
                                        Finding matches...
                                    </span>
                                </div>
                            )}  
                            <div className="flex gap-4 overflow-x-auto pb-4 mb-6">
                                {displayedProfiles.map((profile) => (
                                    <div
                                        key={profile.id}
                                        onClick={() => {setSelectedProfile(profile)
                                            setSelectedMatchId(profile.matchId);}
                                        }
                                        className={`min-w-[180px] cursor-pointer bg-white rounded-xl shadow p-4 text-center border transition ${
                                            selectedProfile?.id === profile.id
                                                ? "border-2"
                                                : "hover:border-gray-300"
                                        }`}
                                        style={{
                                            borderColor: selectedProfile?.id === profile.id ? BLUE : undefined,
                                        }}
                                    >
                                        <img
                                            src={profile.image}
                                            alt={profile.name}
                                            className="w-16 h-16 mx-auto rounded-full object-cover mb-2"
                                            onError={(e) => {
                                                e.target.src = getDefaultImage(profile.name, profile.providerType);
                                            }}
                                        />
                                        <p className="font-medium text-sm truncate">{profile.name}</p>
                                        <p className="text-xs text-gray-500">{profile.type}</p>
                                        <div className="flex justify-center items-center gap-1 mt-1">
                                            <span className="text-xs font-semibold" style={{ color: BLUE }}>
                                                {profile.compatibility}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>

                            {selectedProfile ? (
                                <main className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                                    <div className="lg:col-span-2 bg-white rounded-xl shadow p-6">
                                        <div className="flex items-start gap-4">
                                            <img
                                                src={selectedProfile.image}
                                                alt={selectedProfile.name}
                                                className="w-20 h-20 rounded-full object-cover"
                                                onError={(e) => {
                                                    e.target.src = getDefaultImage(selectedProfile.name, selectedProfile.providerType);
                                                }}
                                            />
                                            <div className="flex-1">
                                                <div className="flex items-center gap-2">
                                                    <h2 className="text-xl font-semibold">
                                                        {selectedProfile.name}
                                                    </h2>
                                                    {selectedProfile.verified && (
                                                        <CheckCircle size={18} color={BLUE} />
                                                    )}
                                                </div>

                                                <div className="flex items-center gap-2 text-sm text-gray-500 mt-1">
                                                    <MapPin size={14} />
                                                    {selectedProfile.location}
                                                </div>

                                                {selectedProfile.email && (
                                                    <div className="flex items-center gap-2 text-sm mt-1">
                                                        <Mail size={14} />
                                                        {selectedProfile.email}
                                                    </div>
                                                )}
                                                {selectedProfile.phone && (
                                                    <div className="flex items-center gap-2 text-sm">
                                                        <Phone size={14} />
                                                        {selectedProfile.phone}
                                                    </div>
                                                )}

                                                <div className="flex items-center gap-2 mt-2">
                                                    <span className="text-sm font-medium text-gray-600">Compatibility:</span>
                                                    <span className="text-lg font-bold" style={{ color: BLUE }}>
                                                        {selectedProfile.compatibility}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>

                                        <div className="mt-6">
                                            <h3 className="font-medium mb-2">Practice Areas</h3>
                                            <div className="flex flex-wrap gap-2">
                                                {selectedProfile.areas && selectedProfile.areas.length > 0 ? (
                                                    selectedProfile.areas.map((area, idx) => (
                                                        <span
                                                            key={idx}
                                                            className="px-3 py-1 bg-gray-100 rounded-full text-sm"
                                                        >
                                                            {area}
                                                        </span>
                                                    ))
                                                ) : (
                                                    <span className="text-sm text-gray-500">No specific areas listed</span>
                                                )}
                                            </div>
                                        </div>

                                        <div className="mt-6">
                                            <h3 className="font-medium">Availability</h3>
                                            <p className="text-sm text-gray-600">
                                                {selectedProfile.availability}
                                            </p>
                                        </div>

                                        <div
                                            className="mt-6 rounded-xl p-4 flex items-center justify-between"
                                            style={{ backgroundColor: "#f3e8ff" }}
                                        >
                                            <p className="text-sm text-gray-700">
                                                Compatibility score based on your case
                                            </p>
                                            <div className="text-2xl font-bold" style={{ color: BLUE }}>
                                                {selectedProfile.compatibility}
                                            </div>
                                        </div>

                                        <div className="mt-6">
                                            <h3 className="font-medium mb-2">Expertise & Experience</h3>
                                            <p className="text-sm text-gray-600 leading-relaxed">
                                                {selectedProfile.experience}
                                            </p>
                                        </div>                                      
                                    </div>

                                    {/* Actions Panel */}
                                    <div className="bg-white rounded-xl shadow p-6 space-y-6">
                                        <div className="space-y-3">
                                            <button
                                                onClick={handleAddToShortlist}
                                                className={`w-full border py-2 rounded-lg flex items-center justify-center gap-2 transition-all duration-200 ${
                                                    isSaved ? "text-red-500 border-red-200 bg-red-50" : "text-gray-700 hover:bg-gray-50"
                                                }`}
                                            >
                                                <Heart
                                                    size={16}
                                                    className={`transition-all duration-300 ${isSaved ? "fill-red-500 scale-110" : "scale-100"}`}
                                                />
                                                {isSaved ? "Saved to Shortlist" : "Add to Shortlist"}
                                            </button>
                                            {selectedProfile.matchId && (
                                                <>
                                                    <button
                                                    onClick={handleMessageClick}
                                                        className="w-full text-white py-2 rounded-lg flex items-center justify-center gap-2"
                                                        style={{ backgroundColor: BLUE }}
                                                    >
                                                        <MessageSquare size={16} /> Message
                                                    </button>
                                                    <button
                                                        onClick={() => setShowScheduler(true)}
                                                        className="w-full border py-2 rounded-lg flex items-center justify-center gap-2 hover:bg-gray-50 transition"
                                                    >
                                                        <Calendar size={16} /> Schedule Call
                                                    </button>
                                                    {/* <p> Status {selectedProfile.matchStatus} | Case Status {selectedProfile.caseStatus}</p> */}
                                                    <button
                                                        type="button"
                                                        onClick={(e) =>handleAcceptMatch(selectedProfile.matchId)}
                                                        className={`w-full text-white py-2 rounded-lg flex items-center justify-center gap-2 hover:opacity-90 transition 
                                                        ${acceptDisabled
                                                                    ? "bg-blue-950 text-white cursor-not-allowed"
                                                                    : "bg-green-600 text-white hover:opacity-90"}`}
                                                    >
                                                        <CheckCircle size={16} />{acceptText}
                                                    </button>
                                                    <button
                                                        type="button"
                                                        onClick={() => handleRejectMatch(selectedProfile.matchId)}
                                                        className="w-full border border-red-300 text-red-600 py-2 rounded-lg flex items-center justify-center gap-2 hover:bg-red-50 transition"
                                                    >
                                                        Reject Match
                                                    </button>
                                                </>
                                            )}
                                        </div>
                                        

                                        {/* Recent Activity */}
                                        {selectedProfile.activity && selectedProfile.activity.length > 0 && (
                                            <div>
                                                <h3 className="font-medium mb-2">Recent Activity</h3>
                                                <ul className="text-sm text-gray-600 space-y-1">
                                                    {selectedProfile.activity.map((item, idx) => (
                                                        <li key={idx}>{item}</li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}

                                        {/* Documents */}
                                        {selectedProfile.documents && selectedProfile.documents.length > 0 && (
                                            <div>
                                                <h3 className="font-medium mb-2">Documents</h3>
                                                <div className="space-y-2 text-sm">
                                                    {selectedProfile.documents.map((doc, idx) => (
                                                        <div key={idx} className="flex justify-between items-center">
                                                            <div className="flex items-center gap-2">
                                                                <FileText size={14} />
                                                                <span>{doc}</span>
                                                            </div>
                                                            <button style={{ color: BLUE }}>Download</button>
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </div>                                    
                                </main>
                            ) : (
                                <div className="bg-white rounded-xl shadow p-6 text-center">
                                    <p className="text-gray-600">Select a match to view details</p>
                                </div>
                            )}
                        </div>
                    )}
                </>
            )}

            {/* Appointment Scheduler Modal */}
            {showScheduler && selectedProfile && (
                <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
                    <div className="bg-white rounded-2xl shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden relative">
                        <button
                            onClick={() => setShowScheduler(false)}
                            className="absolute top-4 right-4 z-10 text-gray-500 hover:text-gray-800 text-2xl leading-none"
                        >
                            ×
                        </button>
                        <div className="max-h-[90vh] overflow-y-auto">
                            <AppointmentScheduler
                                lawyer={{
                                    id: selectedProfile.id,
                                    matchId: selectedProfile.matchId,
                                    name: selectedProfile.name,
                                    type: selectedProfile.type,
                                    image: selectedProfile.image,
                                    compatibility: selectedProfile.compatibility,
                                }}
                                isModal={true}
                                onClose={() => setShowScheduler(false)}
                            />
                        </div>
                    </div>
                </div>
            )}
            {/* Matching Preferences */}
            <div className="m-6 bg-gray-50 rounded-xl p-6 space-y-4 border border-gray-200">
                <h2 className="text-lg font-semibold">Matching Preferences</h2>
                    <p className="text-sm text-gray-600"> Adjust how Legal Aid Connect finds and recommends matches for you. </p>
                    {/* Sensitivity */}
                    <div className="space-y-2">
                        <label className="block text-sm font-medium">Matching Sensitivity</label>
                            <input type="range" min="0" max="100" step="10" value={sensitivity} 
                                   onChange={(e) => setSensitivity(Number(e.target.value))}
                                   className="w-full accent-[#6610f2]" />
                            <p className="text-sm text-gray-500">
                                Current setting: <span className="font-medium">{sensitivity}%</span>. Higher
                                values prioritize more relevant matches.
                            </p>
                    </div>
                </div>
        </div>
    );
}
