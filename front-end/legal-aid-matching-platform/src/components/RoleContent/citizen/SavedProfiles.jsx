import React, { useState, useEffect } from "react";
import {
  MapPin,
  Star,
  CheckCircle,
  Trash2,
  Loader2,
  X,
  Mail,
  Phone,
  Globe,
  Briefcase,
  Award,
  BookOpen,
  Languages,
  Calendar,
  FileText,
} from "lucide-react";
import { getSavedProfiles, unsaveProfile, getProviderProfile } from "../../../services/api";
import { useAlert } from "../../../context/AlertContext";

const BLUE = "#6610f2";

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

export default function SavedProfiles() {
  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedProfile, setSelectedProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const { showAlert } = useAlert();

  useEffect(() => {
    fetchSavedProfiles();
  }, []);

  const fetchSavedProfiles = async () => {
    try {
      setLoading(true);
      const response = await getSavedProfiles();
      const matches = response.data.content || [];

      const profilePromises = matches.map(async (match) => {
        try {
          const res = await getProviderProfile(match.providerId, match.providerType);
          const profileData = res.data;
          
          return {
            ...match,
            ...profileData,
            matchId: match.matchId,
            caseId: match.caseId,
            caseTitle: match.caseTitle,
            type: match.providerType === 'LAWYER' ? 'Lawyer' : 'NGO',
            image: profileData.imageUrl || getDefaultImage(profileData.name, match.providerType),
            areas: profileData.specialization ? profileData.specialization.split(',') : (profileData.focusArea ? profileData.focusArea.split(',') : []),
          };
        } catch (error) {
          console.error(`Failed to fetch profile for provider ${match.providerId}:`, error);
          return {
            ...match,
            matchId: match.matchId,
            caseId: match.caseId,
            caseTitle: match.caseTitle,
            name: `${match.providerType} #${match.providerId}`,
            type: match.providerType === 'LAWYER' ? 'Lawyer' : 'NGO',
            location: "Unknown",
            areas: [],
            image: getDefaultImage('?', match.providerType),
            error: true,
          };
        }
      });

      const populatedProfiles = await Promise.all(profilePromises);
      setProfiles(populatedProfiles);

    } catch (error) {
      console.error("Failed to fetch saved profiles:", error);
      showAlert("Failed to load saved profiles", "error");
      setProfiles([]);
    } finally {
      setLoading(false);
    }
  };

  const handleUnsave = async (matchId) => {
    try {
      await unsaveProfile(matchId);
      showAlert("Profile removed from saved", "success");
      
      setProfiles(prev => prev.filter(p => p.matchId !== matchId));
      if (selectedProfile?.matchId === matchId) {
        setSelectedProfile(null);
      }
    } catch (error) {
      console.error("Failed to unsave profile:", error);
      showAlert(error.response?.data?.message || "Failed to remove profile", "error");
    }
  };

  const handleProfileClick = async (match) => {
    if (!match.providerId || !match.providerType) {
      showAlert("Profile information not available", "error");
      return;
    }

    try {
      setProfileLoading(true);
      setSelectedProfile(null); // Clear previous profile
      const response = await getProviderProfile(match.providerId, match.providerType);
      setSelectedProfile({
        ...response.data,
        providerType: match.providerType,
        matchId: match.matchId,
        score: match.score,
        caseId: match.caseId,
        caseTitle: match.caseTitle,
      });
    } catch (error) {
      console.error("Failed to fetch profile:", error);
      showAlert("Failed to load profile details", "error");
    } finally {
      setProfileLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 p-4 md:p-6">
        <div className="mb-6">
          <h1 className="text-2xl font-semibold text-gray-900">Saved Profiles</h1>
          <p className="text-sm text-gray-600 mt-1">
            Manage your shortlisted lawyers and NGOs.
          </p>
        </div>
        <div className="flex items-center justify-center py-12">
          <Loader2 className="animate-spin" size={24} color={BLUE} />
          <span className="ml-2 text-gray-600">Loading saved profiles...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 p-4 md:p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">Saved Profiles</h1>
        <p className="text-sm text-gray-600 mt-1">
          Manage your shortlisted lawyers and NGOs.
        </p>
      </div>

      {profiles.length === 0 ? (
        <div className="bg-white rounded-xl shadow p-8 text-center">
          <p className="text-gray-500">No saved profiles yet.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {profiles.map((profile) => (
            <div key={profile.matchId} className="bg-white rounded-xl shadow p-6 relative">
              <button
                onClick={() => handleUnsave(profile.matchId)}
                className="absolute top-4 right-4 text-gray-400 hover:text-red-500 transition-colors"
                title="Remove from saved"
              >
                <Trash2 size={18} />
              </button>

              <div className="flex items-center gap-4 mb-4">
                <img
                  src={profile.image}
                  alt={profile.name}
                  className="w-16 h-16 rounded-full object-cover"
                  onError={(e) => {
                    e.target.src = getDefaultImage(profile.name, profile.providerType);
                  }}
                />
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold text-lg">{profile.name}</h3>
                    {profile.verified && (
                      <CheckCircle size={16} color={BLUE} />
                    )}
                  </div>
                  <p className="text-sm text-gray-500">{profile.type}</p>
                  <div className="flex items-center gap-1 mt-1">
                    <Star size={14} className="text-yellow-400 fill-yellow-400" />
                    <span className="text-xs font-medium">{profile.rating || 'N/A'}</span>
                    {profile.score > 0 && (
                      <span className="text-xs text-gray-500 ml-2">
                        ({profile.score}% match)
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <div className="space-y-2 text-sm text-gray-600 mb-4">
                <div className="flex items-center gap-2">
                  <MapPin size={14} />
                  {profile.location}
                </div>
                {profile.caseTitle && (
                  <div className="flex items-center gap-2 text-xs">
                    <FileText size={14} className="text-gray-500" />
                    <span className="font-medium">Case:</span>
                    <span className="text-gray-700">{profile.caseTitle}</span>
                  </div>
                )}
                {profile.score > 0 && (
                  <div className="flex items-center gap-2 text-xs">
                    <span className="font-medium">Match Score:</span>
                    <span style={{ color: BLUE }}>{profile.score}%</span>
                  </div>
                )}
              </div>

              <div className="flex flex-wrap gap-2 mb-4">
                {profile.areas?.slice(0, 3).map((area, idx) => (
                  <span
                    key={idx}
                    className="px-2 py-1 bg-gray-100 rounded-full text-xs text-gray-700"
                  >
                    {area}
                  </span>
                ))}
                {profile.areas?.length > 3 && (
                  <span className="px-2 py-1 bg-gray-100 rounded-full text-xs text-gray-700">
                    +{profile.areas.length - 3} more
                  </span>
                )}
              </div>

              <button 
                className="w-full py-2 rounded-lg text-white font-medium transition hover:opacity-90" 
                style={{ backgroundColor: "#162456"}}
                onClick={() => handleProfileClick(profile)}
              >
                View Full Profile
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Profile Detail Modal */}
      {(selectedProfile || profileLoading) && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-3xl max-h-[90vh] overflow-hidden relative">
            <button
              onClick={() => setSelectedProfile(null)}
              className="absolute top-4 right-4 z-10 text-gray-500 hover:text-gray-800"
            >
              <X size={24} />
            </button>
            <div className="max-h-[90vh] overflow-y-auto p-8">
              {profileLoading ? (
                <div className="flex items-center justify-center py-24">
                  <Loader2 className="animate-spin" size={32} color={BLUE} />
                  <span className="ml-3 text-lg">Loading profile...</span>
                </div>
              ) : selectedProfile && (
                <>
                  <div className="flex flex-col sm:flex-row items-start gap-6 mb-8">
                    <img
                      src={selectedProfile.imageUrl || getDefaultImage(selectedProfile.name, selectedProfile.providerType)}
                      alt={selectedProfile.name}
                      className="w-28 h-28 rounded-full object-cover border-4 border-gray-100 shadow-md"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-1">
                        <h2 className="text-3xl font-bold">{selectedProfile.name}</h2>
                        {selectedProfile.verified && (
                          <CheckCircle size={22} className="text-green-500 fill-current" title="Verified" />
                        )}
                      </div>
                      <p className="text-md text-gray-600 font-medium">{selectedProfile.providerType === 'LAWYER' ? 'Lawyer' : 'NGO'}</p>
                      <div className="flex items-center gap-2 mt-2 text-sm text-gray-500">
                        <MapPin size={16} />
                        {selectedProfile.city}, {selectedProfile.location}
                      </div>
                      {selectedProfile.caseTitle && (
                        <div className="flex items-center gap-2 mt-2 text-sm text-gray-600 bg-blue-50 px-3 py-2 rounded-lg">
                          <FileText size={16} className="text-blue-600" />
                          <span className="font-medium">Saved for Case:</span>
                          <span className="text-blue-800">{selectedProfile.caseTitle}</span>
                        </div>
                      )}
                       {selectedProfile.score > 0 && (
                        <div className="mt-3 text-lg font-bold text-purple-700">
                          Match Score: {selectedProfile.score}%
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8 text-sm">
                    {selectedProfile.email && <div className="flex items-center gap-3 bg-gray-50 p-3 rounded-lg">
                      <Mail size={18} className="text-gray-500" />
                      <a href={`mailto:${selectedProfile.email}`} className="hover:underline">{selectedProfile.email}</a>
                    </div>}
                    {selectedProfile.contactNumber && <div className="flex items-center gap-3 bg-gray-50 p-3 rounded-lg">
                      <Phone size={18} className="text-gray-500" />
                      <span>{selectedProfile.contactNumber}</span>
                    </div>}
                    {selectedProfile.website && <div className="flex items-center gap-3 bg-gray-50 p-3 rounded-lg md:col-span-2">
                      <Globe size={18} className="text-gray-500" />
                      <a href={selectedProfile.website} target="_blank" rel="noopener noreferrer" className="hover:underline">{selectedProfile.website}</a>
                    </div>}
                  </div>

                  <div className="space-y-6 mb-3">
                    {selectedProfile.providerType === 'LAWYER' && (
                      <>
                        {selectedProfile.bio && <div>
                          <h3 className="font-semibold text-lg mb-2 flex items-center gap-2"><Briefcase size={18}/> Bio</h3>
                          <p className="text-gray-700 text-sm leading-relaxed">{selectedProfile.bio}</p>
                        </div>}
                        {selectedProfile.specialization && <div>
                          <h3 className="font-semibold text-lg mb-2 flex items-center gap-2"><Award size={18}/> Specializations</h3>
                          <div className="flex flex-wrap gap-2">
                            {selectedProfile.specialization.split(',').map((spec, idx) => (
                              <span key={idx} className="px-3 py-1 bg-purple-100 text-purple-800 rounded-full text-xs font-medium">
                                {spec.trim()}
                              </span>
                            ))}
                          </div>
                        </div>}
                        <div className="grid grid-cols-2 gap-4 text-sm">
                          {selectedProfile.experienceYears && <div className="flex items-center gap-2"><Calendar size={16}/> <strong>Experience:</strong> {selectedProfile.experienceYears} years</div>}
                          {selectedProfile.barRegistrationNo && <div className="flex items-center gap-2"><BookOpen size={16}/> <strong>Bar No:</strong> {selectedProfile.barRegistrationNo}</div>}
                          {selectedProfile.language && <div className="flex items-center gap-2"><Languages size={16}/> <strong>Languages:</strong> {selectedProfile.language}</div>}
                        </div>
                      </>
                    )}
                    
                    {selectedProfile.providerType === 'NGO' && (
                      <>
                        {selectedProfile.description && <div>
                          <h3 className="font-semibold text-lg mb-2 flex items-center gap-2"><Briefcase size={18}/> Description</h3>
                          <p className="text-gray-700 text-sm leading-relaxed">{selectedProfile.description}</p>
                        </div>}
                        {selectedProfile.focusArea && <div>
                          <h3 className="font-semibold text-lg mb-2 flex items-center gap-2"><Award size={18}/> Focus Areas</h3>
                          <div className="flex flex-wrap gap-2">
                            {selectedProfile.focusArea.split(',').map((area, idx) => (
                              <span key={idx} className="px-3 py-1 bg-purple-100 text-purple-800 rounded-full text-xs font-medium">
                                {area.trim()}
                              </span>
                            ))}
                          </div>
                        </div>}
                         {selectedProfile.registrationNo && <div className="flex items-center gap-2 text-sm"><BookOpen size={16}/> <strong>Registration No:</strong> {selectedProfile.registrationNo}</div>}
                      </>
                    )}
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
