import React, { useEffect, useRef, useState } from "react";
import { FaEdit, FaSave } from "react-icons/fa";

export default function LawyerEditProfile() {
  const token = sessionStorage.getItem("accessToken");
  const debounceRef = useRef(null);

  const [form, setForm] = useState({
    name: "",
    expertise: "",
    location: "",
    contactInfo: "",
    barRegistrationNo: "",
    specialization: "",
    experienceYears: "",
    city: "",
    bio: "",
    language: "",
    latitude: "",
    longitude: ""
  });

  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [responseData, setResponseData] = useState(null);
  const [showPopup, setShowPopup] = useState(false);
  const [citySuggestions, setCitySuggestions] = useState([]);
  const [citySelected, setCitySelected] = useState(false);

  //  ONLY NEW STATE
  const [usingCurrentLocation, setUsingCurrentLocation] = useState(false);

  // ---------------- LOAD PROFILE ----------------
  useEffect(() => {
    document.title = "Profile | Legal Aid";

    const loadProfile = async () => {
      try {
        await fetch("http://localhost:8080/profile/me", {
          headers: { Authorization: `Bearer ${token}` },
        });

        const profileRes = await fetch(
          "http://localhost:8080/profile/update/lawyer",
          {
            method: "PUT",
            headers: {
              Authorization: `Bearer ${token}`,
              "Content-Type": "application/json",
            },
            body: JSON.stringify({}),
          }
        );

        const profile = await profileRes.json();

        setForm({
          name: profile.name || "",
          expertise: profile.expertise || "",
          location: profile.location || "",
          contactInfo: profile.contactInfo || "",
          barRegistrationNo: profile.barRegistrationNo || "",
          specialization: profile.specialization || "",
          experienceYears: profile.experienceYears || "",
          city: profile.city || "",
          bio: profile.bio || "",
          language: profile.language || "",
          latitude: profile.latitude || "",
          longitude: profile.longitude || ""
        });

        setLoading(false);
      } catch (err) {
        console.error("Error loading profile:", err);
      }
    };

    loadProfile();
  }, [token]);

  // ---------------- FETCH LAT/LNG ----------------
  const fetchLatLng = async (query) => {
    if (!query || query.length < 3) return;

    try {
      const res = await fetch(
        `http://localhost:8080/api/location/search?q=${encodeURIComponent(query)}`
      );

      const data = await res.json();

      if (data.length > 0) {
        setForm((prev) => ({
          ...prev,
          location: data[0].display_name,
          latitude: data[0].lat,
          longitude: data[0].lon
        }));
      }
    } catch (err) {
      console.error("Lat/Lng fetch error:", err);
    }
  };
  const reverseGeocode = async (lat, lon) => {
  try {
    const res = await fetch(
      `http://localhost:8080/api/location/reverse?lat=${lat}&lon=${lon}`
    );

    const data = await res.json();

    if (data?.display_name) {
      setForm((prev) => ({
        ...prev,
        location: data.display_name
      }));
    }
  } catch (err) {
    console.error("Reverse geocode error:", err);
  }
};


  // ---------------- AUTOCOMPLETE SEARCH ----------------
  const searchCity = (query) => {
    if (!query || query.length < 3) {
      setCitySuggestions([]);
      return;
    }

    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    debounceRef.current = setTimeout(async () => {
      try {
        const res = await fetch(
          `http://localhost:8080/api/location/search?q=${encodeURIComponent(query)}`
        );

        const data = await res.json();
        setCitySuggestions(data);
      } catch (err) {
        console.error("City autocomplete error:", err);
      }
    }, 400);
  };

  // ---------------- INPUT HANDLER ----------------
  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  // ---------------- SUBMIT ----------------
  const handleSubmit = (e) => {
    e.preventDefault();

    fetch("http://localhost:8080/profile/update/lawyer", {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(form),
    })
      .then((res) => res.json())
      .then((data) => {
        setResponseData(data);
        setShowPopup(true);
        setIsEditing(false);
      })
      .catch((err) => console.error("Submit error:", err));
  };

  if (loading) return <p className="p-6">Loading profile...</p>;

  return (
    <div className="min-h-screen bg-gray-100 p-2">
      <div className="max-w-4xl mx-auto bg-white shadow-lg rounded-xl p-6">

        {/* HEADER */}
        <div className="flex flex-col md:flex-row items-start gap-6">
          <img
            src={`https://ui-avatars.com/api/?name=${encodeURIComponent(form.name || "User")}&background=random`}
            alt="Profile"
            className="w-30 h-28 rounded-full object-cover border-4 border-indigo-500 shadow"
          />

          <div className="flex-1">
            <h1 className="text-2xl font-bold text-gray-800">Lawyer Profile</h1>
            <span className="px-3 py-1 mt-2 inline-block rounded-full bg-indigo-100 text-indigo-700 text-sm font-medium">
              Role: Lawyer
            </span>
            <p className="text-gray-500 mt-2">
              Update your professional, bar details, and personal information.
            </p>
          </div>

          {!isEditing ? (
            <button
              onClick={() => setIsEditing(true)}
              className="px-4 py-2 bg-blue-950 text-white rounded-lg shadow flex items-center gap-2"
            >
              <FaEdit /> Edit Profile
            </button>
          ) : (
            <button
              onClick={handleSubmit}
              className="px-4 py-2 bg-green-600 text-white rounded-lg shadow flex items-center gap-2"
            >
              <FaSave /> Save Changes
            </button>
          )}
        </div>

        <hr className="my-6" />

        {/* FORM */}
        <form className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <Input label="Full Name" name="name" value={form.name} onChange={handleChange} isEditing={isEditing} />
          <Input label="Expertise" name="expertise" value={form.expertise} onChange={handleChange} isEditing={isEditing} />
          <Input label="City" name="city" value={form.city} onChange={handleChange} isEditing={isEditing} />
          <Input label="Contact Info" name="contactInfo" value={form.contactInfo} onChange={handleChange} isEditing={isEditing} />
          <Input label="Bar Registration No" name="barRegistrationNo" value={form.barRegistrationNo} onChange={handleChange} isEditing={isEditing} />
          <Input label="Specialization" name="specialization" value={form.specialization} onChange={handleChange} isEditing={isEditing} />
          <Input label="Experience (Years)" name="experienceYears" type="number" value={form.experienceYears} onChange={handleChange} isEditing={isEditing} />
          <Input label="Language" name="language" value={form.language} onChange={handleChange} isEditing={isEditing} />

          {/* LOCATION */}
          <div className="relative">
            <label className="block text-sm font-semibold mb-1">Location</label>

            <input
              name="location"
              type="text"
              value={form.location}
              disabled={!isEditing}
              onChange={(e) => {
                handleChange(e);
                searchCity(e.target.value);
                setCitySelected(false);
                setUsingCurrentLocation(false);
              }}
              onBlur={() => {
                setTimeout(() => {
                  if (
                    !citySelected &&
                    !usingCurrentLocation &&
                    form.location.length >= 3
                  ) {
                    fetchLatLng(form.location);
                  }
                  setCitySuggestions([]);
                }, 300);
              }}
              className={`w-full border rounded-lg px-3 py-2 ${
                !isEditing ? "bg-gray-200" : ""
              }`}
            />

            {/* 🔹 OR + USE MY LOCATION */}
            <div className="flex items-center gap-2 mt-2">
              <span className="text-xs text-gray-400">OR</span>
              <button
                type="button"
                disabled={!isEditing}
                className="text-xs px-3 py-1 rounded bg-indigo-600 text-white"
                onClick={() => {
                  if (!navigator.geolocation) {
                    alert("Geolocation not supported");
                    return;
                  }

                  navigator.geolocation.getCurrentPosition(
                    (pos) => {
                      const lat = pos.coords.latitude;
                      const lon = pos.coords.longitude;

                      setUsingCurrentLocation(true);
                      setCitySelected(true);

                      setForm((prev) => ({
                        ...prev,
                        latitude: pos.coords.latitude,
                        longitude: pos.coords.longitude,
                        location: prev.location || ""
                      }));
                      reverseGeocode(lat, lon);
                    },
                    () => alert("Location permission denied"),
                    { enableHighAccuracy: true }
                  );
                }}
              >
                📍 Use My Location
              </button>
            </div>

            {/* LAT / LNG DISPLAY */}
            <div className="col-span-2 bg-gray-100 p-4 rounded-lg mt-3">
              <p className="text-sm text-gray-600">
                <strong>Latitude:</strong> {form.latitude || "—"}
              </p>
              <p className="text-sm text-gray-600">
                <strong>Longitude:</strong> {form.longitude || "—"}
              </p>
            </div>

            {citySuggestions.length > 0 && isEditing && (
              <ul className="absolute z-50 w-full bg-white border rounded-lg shadow mt-1 max-h-48 overflow-auto">
                {citySuggestions.map((location, index) => (
                  <li
                    key={index}
                    className="px-3 py-2 hover:bg-indigo-100 cursor-pointer"
                    onMouseDown={(e) => {
                      e.preventDefault();
                      setCitySelected(true);
                      setUsingCurrentLocation(false);

                      setForm({
                        ...form,
                        location: location.display_name,
                        latitude: location.lat,
                        longitude: location.lon
                      });
                      setCitySuggestions([]);
                    }}
                  >
                    {location.display_name}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </form>
      </div>

      {showPopup && responseData && (
        <Popup responseData={responseData} onClose={() => setShowPopup(false)} />
      )}
    </div>
  );
}

/* INPUT COMPONENT */
function Input({ label, name, value, type = "text", onChange, isEditing }) {
  return (
    <div>
      <label className="block text-sm font-semibold mb-1">{label}</label>
      <input
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        disabled={!isEditing}
        className={`w-full border rounded-lg px-3 py-2 ${
          !isEditing ? "bg-gray-200" : ""
        }`}
      />
    </div>
  );
}

/* POPUP COMPONENT */
function Popup({ responseData, onClose }) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white p-6 rounded-xl w-[90%] max-w-lg shadow-2xl">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-green-600 flex items-center gap-2">
            <span className="text-2xl">✔</span> Profile Updated Successfully
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl"
          >
            ×
          </button>
        </div>

        <div className="space-y-3 max-h-96 overflow-y-auto">
          {/* Personal Info */}
          <div className="bg-gray-50 p-4 rounded-lg">
            <h3 className="font-semibold text-gray-700 mb-2">Personal Information</h3>
            <div className="grid grid-cols-1 gap-2 text-sm">
              <div><span className="font-medium">Name:</span> {responseData.name}</div>
              <div><span className="font-medium">Contact:</span> {responseData.contactInfo}</div>
              <div><span className="font-medium">City:</span> {responseData.city}</div>
              <div><span className="font-medium">Location:</span> {responseData.location}</div>
              {responseData.language && (
                <div><span className="font-medium">Language:</span> {responseData.language}</div>
              )}
            </div>
          </div>

          {/* Professional Info */}
          <div className="bg-indigo-50 p-4 rounded-lg">
            <h3 className="font-semibold text-indigo-700 mb-2">Professional Details</h3>
            <div className="grid grid-cols-1 gap-2 text-sm">
              <div><span className="font-medium">Bar Registration:</span> {responseData.barRegistrationNo}</div>
              <div><span className="font-medium">Expertise:</span> {responseData.expertise}</div>
              <div><span className="font-medium">Specialization:</span> {responseData.specialization}</div>
              {responseData.experienceYears && (
                <div><span className="font-medium">Experience:</span> {responseData.experienceYears} years</div>
              )}
            </div>
          </div>

          {/* Location Coordinates */}
          {(responseData.latitude || responseData.longitude) && (
            <div className="bg-blue-50 p-4 rounded-lg">
              <h3 className="font-semibold text-blue-700 mb-2">Location Coordinates</h3>
              <div className="grid grid-cols-2 gap-2 text-sm">
                <div><span className="font-medium">Latitude:</span> {responseData.latitude || "—"}</div>
                <div><span className="font-medium">Longitude:</span> {responseData.longitude || "—"}</div>
              </div>
            </div>
          )}

          {/* Status */}
          <div className="bg-green-50 p-4 rounded-lg">
            <h3 className="font-semibold text-green-700 mb-2">Account Status</h3>
            <div className="flex items-center gap-4 text-sm">
              <div>
                <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                  responseData.isAvailable 
                    ? "bg-green-200 text-green-800" 
                    : "bg-red-200 text-red-800"
                }`}>
                  {responseData.isAvailable ? "Available" : "Not Available"}
                </span>
              </div>
              {responseData.verified !== null && (
                <div>
                  <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                    responseData.verified 
                      ? "bg-blue-200 text-blue-800" 
                      : "bg-yellow-200 text-yellow-800"
                  }`}>
                    {responseData.verified ? "Verified" : "Pending Verification"}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>

        <button
          className="mt-6 w-full bg-blue-950 text-white py-2.5 rounded-lg font-medium hover:bg-blue-900 transition"
          onClick={onClose}
        >
          Close
        </button>
      </div>
    </div>
  );
}
