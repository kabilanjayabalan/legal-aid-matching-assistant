import React, { useState, useEffect } from "react";
import { FaFilter, FaChevronDown, FaChevronUp } from "react-icons/fa";
import {
  searchDirectory,
  getSpecializations,
  getFocusAreas,
  getMyProfile,
} from "../../../services/api";

const getAvatar = (name, type) => {
  const initials = name
    ? name.split(" ").map((n) => n[0]).slice(0, 2).join("").toUpperCase()
    : "??";

  const bg = type === "Lawyer" ? "#EDE9FE" : "#E0F2FE"; // soft violet / blue
  const color = "#6610f2";

  const svg = `
  <svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'>
    <rect width='100' height='100' rx='50' fill='${bg}'/>
    <text x='50%' y='55%' text-anchor='middle'
      font-size='36'
      fill='${color}'
      font-family='Arial'
      font-weight='600'>
      ${initials}
    </text>
  </svg>`;

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
};

export default function DirectorySearch() {
  /* ---------- DATA FROM BACKEND ---------- */
  const [specializations, setSpecializations] = useState([]);
  const [focusAreas, setFocusAreas] = useState([]);

  const languages = [
    "English",
    "Hindi",
    "Tamil",
    "Telugu",
    "Kannada",
    "Malayalam",
    "Bengali",
    "Gujarati",
  ];

  /* ---------- STATE ---------- */
  const [role, setRole] = useState("Lawyer");
  const [selectedAreas, setSelectedAreas] = useState([]);
  const [selectedLanguages, setSelectedLanguages] = useState([]);
  const [distance, setDistance] = useState(50);
  const [verifiedOnly, setVerifiedOnly] = useState(false);
  const [availability, setAvailability] = useState("");
  const [search, setSearch] = useState("");
  const [location, setLocation] = useState("");
  const [visiblePracticeAreasCount, setVisiblePracticeAreasCount] = useState(5);

  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedProfile, setSelectedProfile] = useState(null);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false); // Mobile filter dropdown state
  
  // Pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(12); // Items per page
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [latitude, setLatitude] = useState(null);
  const [longitude, setLongitude] = useState(null);
  const [locationError, setLocationError] = useState(null);
  const [useDistance, setUseDistance] = useState(false);
  const [locationType, setLocationType] = useState("LIVE"); // "LIVE" or "PROFILE"
  const [profileLocation, setProfileLocation] = useState(null); // {latitude, longitude}

  /* ---------- HELPERS ---------- */
  const toggleItem = (item, list, setList) => {
    setList(
      list.includes(item)
        ? list.filter((i) => i !== item)
        : [...list, item]
    );
  };

  // Single select for practice areas - only one can be selected at a time
  const selectSingleArea = (item) => {
    setSelectedAreas(
      selectedAreas.includes(item) ? [] : [item]
    );
  };
  // Fetch live location
  useEffect(() => {
  if (!navigator.geolocation) {
    setLocationError("Geolocation not supported");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      setLatitude(position.coords.latitude);
      setLongitude(position.coords.longitude);
      console.log(position.coords.longitude,position.coords.latitude)
    },
    (error) => {
      console.error("Location error:", error);
      setLocationError("Unable to fetch location");
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
    }
  );
}, []);

  // Fetch profile location
  useEffect(() => {
    const fetchProfileLocation = async () => {
      try {
        const response = await getMyProfile();
        if (response?.data) {
          const profileData = response.data;
          // Check if profile has location data
          if (profileData.latitude && profileData.longitude) {
            setProfileLocation({
              latitude: profileData.latitude,
              longitude: profileData.longitude,
            });
          }
        }
      } catch (error) {
        console.error("Error fetching profile location:", error);
      }
    };

    fetchProfileLocation();
  }, []);


  // Fetch practice areas for lawyers & NGOs from backend once
  useEffect(() => {
    const fetchPracticeAreas = async () => {
      try {
        const [specRes, focusRes] = await Promise.all([
          getSpecializations(),
          getFocusAreas(),
        ]);

        if (specRes?.data?.specializations) {
          setSpecializations(specRes.data.specializations);
        }

        if (focusRes?.data?.focusAreas) {
          setFocusAreas(focusRes.data.focusAreas);
        }
      } catch (err) {
        console.error("Error fetching practice areas:", err);
        setSpecializations([]);
        setFocusAreas([]);
      }
    };

    fetchPracticeAreas();
  }, []);

  // Reset to first page when filters change
  useEffect(() => {
    setCurrentPage(0);
  }, [role, search, location, selectedAreas, selectedLanguages, verifiedOnly,  useDistance, distance, locationType]);

  // Reset visible practice areas count when role changes
  useEffect(() => {
    setVisiblePracticeAreasCount(5);
  }, [role]);

  // Fetch directory profiles whenever filters or page changes
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);

      try {
        // Determine which coordinates to use based on locationType
        let searchLatitude = null;
        let searchLongitude = null;

        if (useDistance) {
          if (locationType === "PROFILE" && profileLocation) {
            searchLatitude = profileLocation.latitude;
            searchLongitude = profileLocation.longitude;
          } else if (locationType === "LIVE") {
            searchLatitude = latitude;
            searchLongitude = longitude;
          }
        }

        const params = {
          type: role.toUpperCase(), // "LAWYER" or "NGO"
          query: search || null,
          location: location || null,
          expertise:
            selectedAreas.length > 0 ? selectedAreas[0] : null,
          language:
            selectedLanguages.length > 0 ? selectedLanguages.join(", ") : null,
          isVerified: verifiedOnly || null,
          latitude: searchLatitude,
          longitude: searchLongitude,
          radiusKm: useDistance ? distance : null,
          locationType: useDistance ? locationType : null,
          page: currentPage,
          size: pageSize,
        };

        const response = await searchDirectory(params);
        const data = response?.data;

        let items = [];
        let pageInfo = null;

        // Case 1: backend returns a raw list
        if (Array.isArray(data)) {
          items = data;
          setTotalPages(1);
          setTotalElements(data.length);
        }
        // Case 2: backend returns a Page object
        else if (data && Array.isArray(data.content)) {
          items = data.content;
          pageInfo = data;
          setTotalPages(data.totalPages || 1);
          setTotalElements(data.totalElements || items.length);
        }
        // Case 3: backend returns BOTH (for type=BOTH)
        else if (
          data &&
          data.lawyers &&
          Array.isArray(data.lawyers.content)
        ) {
          items = data.lawyers.content;
          pageInfo = data.lawyers;
          setTotalPages(data.lawyers.totalPages || 1);
          setTotalElements(data.lawyers.totalElements || items.length);
        } else if (
          data &&
          data.ngos &&
          Array.isArray(data.ngos.content)
        ) {
          items = data.ngos.content;
          pageInfo = data.ngos;
          setTotalPages(data.ngos.totalPages || 1);
          setTotalElements(data.ngos.totalElements || items.length);
        } else {
          setTotalPages(0);
          setTotalElements(0);
        }

        setProfiles(items);
      } catch (error) {
        console.error("Error fetching directory:", error);
        setProfiles([]);
        setTotalPages(0);
        setTotalElements(0);
      } finally {
        setLoading(false);
      }
    };

    const timeoutId = setTimeout(fetchData, 500);
    return () => clearTimeout(timeoutId);
  }, [role, search, location, selectedAreas, selectedLanguages, verifiedOnly, currentPage, pageSize, distance, latitude, longitude, locationType, profileLocation, useDistance]);

  const practiceAreas =
    role === "Lawyer" ? specializations : focusAreas;

  return (
    <div className="min-h-full bg-slate-50 p-4">
      <div className="flex flex-col gap-5 lg:flex-row">
        {/* MOBILE FILTER BUTTON */}
        <button
          onClick={() => setIsFiltersOpen(!isFiltersOpen)}
          className="lg:hidden flex items-center justify-between w-full rounded-2xl bg-white p-4 shadow-sm hover:bg-gray-50 transition-colors"
        >
          <div className="flex items-center gap-2">
            <FaFilter className="text-blue-950" />
            <span className="text-sm font-semibold">Filters</span>
          </div>
          {isFiltersOpen ? (
            <FaChevronUp className="text-gray-500" />
          ) : (
            <FaChevronDown className="text-gray-500" />
          )}
        </button>

        {/* FILTERS */}
        <div className={`w-full rounded-2xl bg-white p-4 shadow-sm lg:w-[260px] ${isFiltersOpen ? 'block' : 'hidden'} lg:block`}>
          <h3 className="mb-2 text-sm font-semibold">Filters</h3>

          <div className="mb-1 mt-3 text-sm font-semibold">Role</div>
          <div className="flex flex-wrap gap-2">
            {["Lawyer", "NGO"].map((r) => (
              <span
                key={r}
                onClick={() => setRole(r)}
                className={`cursor-pointer rounded-full px-3 py-1 text-xs ${
                  role === r
                    ? "bg-blue-950 text-white"
                    : "bg-slate-100 text-slate-700"
                }`}
              >
                {r}
              </span>
            ))}
          </div>

          <div className="mb-1 mt-4 text-sm font-semibold">
            Practice Areas (Select One)
          </div>
          <div className="flex flex-wrap gap-2">
            {practiceAreas.slice(0, visiblePracticeAreasCount).map((a) => (
              <span
                key={a}
                onClick={() => selectSingleArea(a)}
                className={`cursor-pointer rounded-full px-3 py-1 text-xs ${
                  selectedAreas.includes(a)
                    ? "bg-blue-950 text-white"
                    : "bg-slate-100 text-slate-700"
                }`}
              >
                {a}
              </span>
            ))}
            {visiblePracticeAreasCount < practiceAreas.length && (
              <span
                onClick={() => setVisiblePracticeAreasCount((prev) => prev + 5)}
                className="cursor-pointer rounded-full px-3 py-1 text-xs bg-slate-100 text-slate-700 hover:bg-slate-200"
              >
                + More
              </span>
            )}
          </div>

          <div className="mt-4">
            <p className="mb-1 text-sm font-semibold">Availability</p>
            <select
              onChange={(e) => setAvailability(e.target.value)}
              className="w-full rounded-lg border px-3 py-2 text-sm"
            >
              <option value="">Select availability</option>
              <option>Available</option>
              <option>Busy</option>
            </select>
          </div>

          <div className="mt-4 flex items-center justify-between">
            <span className="text-sm font-semibold">
              Verified Status
            </span>
            <input
              type="checkbox"
              checked={verifiedOnly}
              onChange={(e) =>
                setVerifiedOnly(e.target.checked)
              }
              className="h-4 w-4 accent-blue-950"
            />
          </div>

          <div className="mt-4">
  {/* Header + toggle */}
  <div className="flex items-center justify-between mb-1">
    <span className="text-sm font-semibold">Distance</span>

    <button
      type="button"
      onClick={() => setUseDistance((prev) => !prev)}
      className={`rounded-lg px-2 py-1 text-xs font-medium transition-colors ${
        useDistance
          ? "bg-blue-950 text-white"
          : "bg-slate-100 text-slate-500 hover:bg-slate-200"
      }`}
    >
      {useDistance ? "On" : "Off"}
    </button>
  </div>

  {/* Slider */}
  <div
    className={`transition-opacity ${
      useDistance ? "opacity-100" : "opacity-40"
    }`}
  >
    <div className="mb-1 text-xs text-gray-600">
      {useDistance ? `Within ${distance} km` : "Distance filter disabled"}
    </div>

    <input
      type="range"
      min="1"
      max="100"
      value={distance}
      onChange={(e) => setDistance(e.target.value)}
      disabled={!useDistance}
      className="w-full cursor-pointer disabled:cursor-not-allowed"
    />
  </div>

  {/* Location Type Selector */}
  {useDistance && (
    <div className="mt-3">
      <div className="mb-1 text-xs font-semibold text-gray-700">Location Source</div>
      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => setLocationType("LIVE")}
          className={`flex-1 rounded-lg px-3 py-2 text-xs font-medium transition-colors ${
            locationType === "LIVE"
              ? "bg-blue-950 text-white"
              : "bg-slate-100 text-slate-700 hover:bg-slate-200"
          }`}
          disabled={!latitude || !longitude}
          title={!latitude || !longitude ? "Live location not available" : "Use your current GPS location"}
        >
          Live Location
        </button>
        <button
          type="button"
          onClick={() => setLocationType("PROFILE")}
          className={`flex-1 rounded-lg px-3 py-2 text-xs font-medium transition-colors ${
            locationType === "PROFILE"
              ? "bg-blue-950 text-white"
              : "bg-slate-100 text-slate-700 hover:bg-slate-200"
          }`}
          disabled={!profileLocation}
          title={!profileLocation ? "Profile location not set" : "Use location from your profile"}
        >
          Profile Location
        </button>
      </div>
      {locationType === "LIVE" && (!latitude || !longitude) && (
        <div className="mt-1 text-xs text-amber-600">
          ⚠ Live location unavailable
        </div>
      )}
      {locationType === "PROFILE" && !profileLocation && (
        <div className="mt-1 text-xs text-amber-600">
          ⚠ Set location in your profile
        </div>
      )}
    </div>
  )}
</div>
          <div className="mb-1 mt-4 text-sm font-semibold">
            Languages
          </div>
          <div className="flex flex-wrap gap-2">
            {languages.map((l) => (
              <span
                key={l}
                onClick={() =>
                  toggleItem(
                    l,
                    selectedLanguages,
                    setSelectedLanguages
                  )
                }
                className={`cursor-pointer rounded-full px-3 py-1 text-xs ${
                  selectedLanguages.includes(l)
                    ? "bg-blue-950 text-white"
                    : "bg-slate-100 text-slate-700"
                }`}
              >
                {l}
              </span>
            ))}
          </div>
        </div>

        {/* PROFILES */}
        <div className="flex-1">
          {/* SEARCH BAR */}
          <div className="mb-4 rounded-2xl bg-white p-4 shadow-sm">
            <div className="flex flex-col gap-3 sm:flex-row">
              <input
                type="text"
                placeholder="Search"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full rounded-lg border px-4 py-2 text-sm"
              />

              <input
                type="text"
                placeholder="Location"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                className="w-full rounded-lg border px-4 py-2 text-sm"
              />

<div className="flex w-full items-center gap-2 sm:w-64">
  {/* Distance label */}
  <span
    className={`whitespace-nowrap text-xs ${
      useDistance ? "text-gray-700" : "text-gray-400"
    }`}
  >
    {useDistance ? `${distance} km` : "Distance off"}
  </span>

  {/* Distance slider */}
  <input
    type="range"
    min="1"
    max="100"
    value={distance}
    onChange={(e) => setDistance(e.target.value)}
    disabled={!useDistance}
    className="w-full cursor-pointer disabled:cursor-not-allowed disabled:opacity-40"
  />

  {/* ON / OFF toggle */}
  <button
    type="button"
    onClick={() => setUseDistance((prev) => !prev)}
    className={`rounded-lg px-2 py-1 text-xs font-medium transition-colors ${
      useDistance
        ? "bg-blue-950 text-white"
        : "bg-slate-100 text-slate-500 hover:bg-slate-200"
    }`}
  >
    {useDistance ? "On" : "Off"}
  </button>
</div>

              <div className="flex gap-2">
                <button className="rounded-lg bg-slate-100 px-3 py-2 text-xs">
                  List
                </button>
                <button className="rounded-lg bg-slate-100 px-3 py-2 text-xs">
                  Map
                </button>
              </div>
            </div>
          </div>

          <h3 className="mb-3 text-sm font-semibold">
            Matching Profiles ({totalElements})
          </h3>

          {loading ? (
            <p className="text-sm text-gray-500">Loading...</p>
          ) : (
            <>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {profiles.map((p, index) => (
                  <div
                    key={`${p.id}-${p.source || 'unknown'}-${index}`}
                    className="rounded-2xl bg-white p-4 shadow-sm"
                  >
                    {(() => {
                      const displayName =
                        role === "Lawyer"
                          ? p.fullName || "Lawyer"
                          : p.orgName || "NGO";

                      return (
                        <>
                          <img
                            src={getAvatar(displayName.replace("Adv.", "").trim(), role)}
                            alt={displayName}
                            className="w-16 h-16 rounded-full mx-auto mb-2"
                          />
                          <div className="mt-2 text-sm font-semibold text-center">
                            {displayName}
                          </div>
                        </>
                      );
                    })()}

                    <div className="mt-1 text-xs text-gray-500 text-center">
                      {role}
                    </div>
                    <div className="text-xs text-gray-500 text-center">
                      {p.specialization ||
                        p.focusArea ||
                        p.area}
                    </div>

                    <button
                      onClick={() => setSelectedProfile(p)}
                      className="mt-3 w-full rounded-lg bg-blue-950 py-2 text-sm text-white"
                    >
                      View Profile
                    </button>
                  </div>
                ))}
              </div>

              {/* Pagination Controls */}
              {totalPages > 1 && (
                <div className="mt-6 flex items-center justify-center gap-2">
                  <button
                    onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                    disabled={currentPage === 0 || loading}
                    className={`rounded-lg px-4 py-2 text-sm font-medium ${
                      currentPage === 0 || loading
                        ? "cursor-not-allowed bg-slate-100 text-slate-400"
                        : "bg-white text-slate-700 shadow-sm hover:bg-slate-50"
                    }`}
                  >
                    Previous
                  </button>

                  <div className="flex items-center gap-1">
                    {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                      let pageNum;
                      if (totalPages <= 5) {
                        pageNum = i;
                      } else if (currentPage < 3) {
                        pageNum = i;
                      } else if (currentPage > totalPages - 4) {
                        pageNum = totalPages - 5 + i;
                      } else {
                        pageNum = currentPage - 2 + i;
                      }

                      return (
                        <button
                          key={pageNum}
                          onClick={() => setCurrentPage(pageNum)}
                          disabled={loading}
                          className={`rounded-lg px-3 py-2 text-sm font-medium ${
                            currentPage === pageNum
                              ? "bg-blue-950 text-white"
                              : "bg-white text-slate-700 shadow-sm hover:bg-slate-50"
                          } ${loading ? "cursor-not-allowed opacity-50" : ""}`}
                        >
                          {pageNum + 1}
                        </button>
                      );
                    })}
                  </div>

                  <button
                    onClick={() =>
                      setCurrentPage((prev) =>
                        Math.min(totalPages - 1, prev + 1)
                      )
                    }
                    disabled={currentPage >= totalPages - 1 || loading}
                    className={`rounded-lg px-4 py-2 text-sm font-medium ${
                      currentPage >= totalPages - 1 || loading
                        ? "cursor-not-allowed bg-slate-100 text-slate-400"
                        : "bg-white text-slate-700 shadow-sm hover:bg-slate-50"
                    }`}
                  >
                    Next
                  </button>
                </div>
              )}

              {totalPages > 1 && (
                <div className="mt-2 text-center text-xs text-gray-500">
                  Page {currentPage + 1} of {totalPages} • Showing{" "}
                  {profiles.length} of {totalElements} results
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {selectedProfile && (
        <div
          onClick={() => setSelectedProfile(null)}
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-3"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-xl rounded-2xl bg-white p-4 shadow-xl"
          >
            {/* HEADER */}
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-semibold">
                {role === "Lawyer"
                  ? selectedProfile.fullName ||
                    "Lawyer Profile"
                  : selectedProfile.orgName || "NGO Profile"}
              </h3>
              <button
                onClick={() => setSelectedProfile(null)}
                className="text-xl leading-none text-gray-500 hover:text-gray-800"
              >
                ×
              </button>
            </div>

            {/* CONTENT GRID */}
            <div className="grid grid-cols-1 gap-4 text-sm sm:grid-cols-2">
              {/* LAWYER VIEW */}
              {role === "Lawyer" && (
                <>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      Bar Registration ID
                    </p>
                    <p className="mt-1">
                      {selectedProfile.barRegistrationId ||
                        "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      Specialization
                    </p>
                    <p className="mt-1">
                      {selectedProfile.specialization ||
                        "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      City
                    </p>
                    <p className="mt-1">
                      {selectedProfile.city || "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      Contact Number
                    </p>
                    <p className="mt-1">
                      {selectedProfile.contactNumber ||
                        "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      Email
                    </p>
                    <p className="mt-1 break-all">
                      {selectedProfile.email || "-"}
                    </p>
                  </div>
                </>
              )}

              {/* NGO VIEW */}
              {role === "NGO" && (
                <>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      Email
                    </p>
                    <p className="mt-1 break-all">
                      {selectedProfile.email || "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      Registration Number
                    </p>
                    <p className="mt-1">
                      {selectedProfile.registrationNumber ||
                        "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      Focus Area
                    </p>
                    <p className="mt-1">
                      {selectedProfile.focusArea || "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      City
                    </p>
                    <p className="mt-1">
                      {selectedProfile.city || "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      Contact Number
                    </p>
                    <p className="mt-1">
                      {selectedProfile.contactNumber ||
                        "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      Email
                    </p>
                    <p className="mt-1 break-all">
                      {selectedProfile.email || "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-gray-500">
                      Website
                    </p>
                    <p className="mt-1 break-all">
                      {selectedProfile.website || "-"}
                    </p>
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
