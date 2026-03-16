import React, { useEffect, useState } from "react";
import { FaEdit, FaSave } from "react-icons/fa";

export default function CitizenEditProfile() {
  const token = sessionStorage.getItem("accessToken");

  const [form, setForm] = useState({
    fullName: "",
    contactInfo: "",
    location: "",
    latitude: "",
    longitude: ""
  });

  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [responseData, setResponseData] = useState(null);
  const [showPopup, setShowPopup] = useState(false);
  const [contactInfoError, setContactInfoError] = useState("");
  const [locationSuggestions, setLocationSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  // ---------------- LOAD PROFILE ----------------
  useEffect(() => {
    document.title = "Profile | Legal Aid";
    const loadProfile = async () => {
      try {
        // Validate user
        await fetch("http://localhost:8080/profile/me", {
          headers: { Authorization: `Bearer ${token}` },
        });

        // Load citizen profile using PUT as GET
        const profileRes = await fetch("http://localhost:8080/profile/update/citizen", {
          method: "PUT",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({}),
        });

        const profile = await profileRes.json();

        setForm({
          fullName: profile.user.fullName || "",
          contactInfo: profile.contactInfo || "",
          location: profile.location || "",
          latitude: profile.latitude || "",
          longitude: profile.longitude || ""
        });

        setLoading(false);
      } catch (err) {
        console.error("Error loading citizen profile:", err);
      }
    };

    loadProfile();
  }, []);

  // ---------------- INPUT HANDLER ----------------
  const validateContactInfo = (contactInfo) => {
    // Regex for phone number (must start with 6,7,8,9 and be exactly 10 digits) or email
    const phoneRegex = /^[6-9]\d{9}$/;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!contactInfo) {
      return "Contact info is required";
    }

    if (!phoneRegex.test(contactInfo) && !emailRegex.test(contactInfo)) {
      return "Please enter a valid phone number (10 digits starting with 6,7,8,9) or email address";
    }

    return "";
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });

    // Validate contact info in real-time
    if (name === "contactInfo") {
      const error = validateContactInfo(value);
      setContactInfoError(error);
    }
  };
  // ---------------- FETCH LAT/LNG ----------------
  const fetchLocationSuggestions = async (query) => {
    if (!query || query.length < 3) {
      setLocationSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    try {
      const res = await fetch(
        `http://localhost:8080/api/location/search?q=${encodeURIComponent(query)}`
      );

      const data = await res.json();
      setLocationSuggestions(data.slice(0, 5)); // Show top 5 suggestions
      setShowSuggestions(data.length > 0);
    } catch (err) {
      console.error("Location suggestions fetch error:", err);
      setLocationSuggestions([]);
      setShowSuggestions(false);
    }
  };

  const selectLocationSuggestion = (suggestion) => {
    setForm((prev) => ({
      ...prev,
      location: suggestion.display_name,
      latitude: suggestion.lat,
      longitude: suggestion.lon
    }));
    setShowSuggestions(false);
    setLocationSuggestions([]);
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

  // ---------------- SUBMIT ----------------
  const handleSubmit = (e) => {
    e.preventDefault();

    // Validate contact info before submission
    const contactError = validateContactInfo(form.contactInfo);
    if (contactError) {
      setContactInfoError(contactError);
      return;
    }

    fetch("http://localhost:8080/profile/update/citizen", {
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

        <div className="flex flex-col md:flex-row items-start gap-6">
          
          {/* LEFT — IMAGE */}
          <div className="flex flex-col items-center">
            <img
              src={`https://ui-avatars.com/api/?name=${encodeURIComponent(form.fullName || "User")}&background=random`}
              alt="Profile"
              className="w-30 h-28 rounded-full object-cover border-4 border-indigo-500 shadow"
            />
          </div>

          {/* RIGHT — TEXT + BUTTON */}
          <div className="w-full">
            <div className="flex items-start w-full">
              <div className="flex-1">
                <h1 className="text-2xl font-bold text-gray-800">Your Profile</h1>

                <span className="px-3 py-1 mt-2 inline-block rounded-full bg-indigo-100 text-indigo-700 text-sm font-medium">
                  Role: Citizen
                </span>

                <p className="text-gray-500 mt-2">
                  Update your personal details and contact information.
                </p>
              </div>

              {/* EDIT → SAVE BUTTON */}
              <div className="ml-auto">
                {!isEditing ? (
                  <button
                    onClick={() => setIsEditing(true)}
                    className="px-4 py-2 bg-blue-950 text-white rounded-lg shadow hover:bg-gray-900 transition flex items-center gap-2"
                  >
                    <FaEdit /> Edit Profile
                  </button>
                ) : (
                  <button
                    onClick={handleSubmit}
                    className="px-4 py-2 bg-green-600 text-white rounded-lg shadow hover:bg-green-700 transition flex items-center gap-2"
                  >
                    <FaSave /> Save Changes
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>

        <hr className="my-6" />

        {/* FORM */}
        <form className="grid grid-cols-1 md:grid-cols-2 gap-5" onSubmit={handleSubmit}>
          
          <Input label="Full Name" name="fullName" value={form.fullName} onChange={handleChange} isEditing={isEditing} />

          <div>
            <Input
              label="Contact Info"
              name="contactInfo"
              value={form.contactInfo}
              onChange={handleChange}
              isEditing={isEditing}
            />
            {contactInfoError && (
              <p className="text-red-500 text-sm mt-1">{contactInfoError}</p>
            )}
          </div>

          <div className="relative col-span-2">
            <label className="block text-sm font-semibold mb-1">Location</label>

              <input
                name="location"
                type="text"
                value={form.location}
                disabled={!isEditing}
                onChange={(e) => {
                  handleChange(e);
                  fetchLocationSuggestions(e.target.value);
                }}
                className={`w-full border rounded-lg px-3 py-2 ${
                  !isEditing ? "bg-gray-200" : ""
                }`}
              />

              {/* Location Suggestions Dropdown */}
              {isEditing && showSuggestions && locationSuggestions.length > 0 && (
                <div className="absolute top-full left-0 right-0 bg-white border border-gray-300 rounded-lg shadow-lg z-10 max-h-60 overflow-y-auto">
                  {locationSuggestions.map((suggestion, index) => (
                    <div
                      key={index}
                      className="p-3 hover:bg-gray-100 cursor-pointer border-b last:border-b-0"
                      onClick={() => selectLocationSuggestion(suggestion)}
                    >
                      <div className="text-sm font-medium">{suggestion.display_name}</div>
                    </div>
                  ))}
                </div>
              )}

              <button
                type="button"
                disabled={!isEditing}
                className="text-xs px-3 py-1 rounded bg-indigo-600 text-white mt-2"
                onClick={() => {
                  if (!navigator.geolocation) {
                    alert("Geolocation not supported");
                    return;
                  }

                  navigator.geolocation.getCurrentPosition(
                    (pos) => {
                      const lat = pos.coords.latitude;
                      const lon = pos.coords.longitude;

                      setForm((prev) => ({
                        ...prev,
                        latitude: lat,
                        longitude: lon
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

            {/* LAT / LNG DISPLAY */}
            <div className="bg-gray-100 p-3 rounded-lg mt-2">
              <p className="text-sm text-gray-600">
                <strong>Latitude:</strong> {form.latitude || "—"}
              </p>
              <p className="text-sm text-gray-600">
                <strong>Longitude:</strong> {form.longitude || "—"}
              </p>
            </div>
          </div>
        </form>

      </div>

      {/* POPUP */}
      {showPopup && responseData && (
        <CitizenPopup responseData={responseData} onClose={() => setShowPopup(false)} />
      )}
    </div>
  );
}

/* ----- INPUT COMPONENT ----- */
function Input({ label, name, value, type = "text", onChange, isEditing }) {
  return (
    <div>
      <label className="block text-sm font-semibold text-gray-700 mb-1">{label}</label>
      <input
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        disabled={!isEditing}
        className={`w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-950 ${
          !isEditing ? "bg-gray-200 cursor-not-allowed" : ""
        }`}
      />
    </div>
  );
}

/* ----- POPUP COMPONENT ----- */
function CitizenPopup({ responseData, onClose }) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white p-6 rounded-2xl shadow-2xl w-[90%] max-w-md">

        <h2 className="text-xl font-bold text-gray-800 mb-4">✔ Profile Updated</h2>

        <div className="space-y-2">
          <PopupDetail label="Full Name" value={responseData.fullName} />
          <PopupDetail label="Contact Info" value={responseData.contactInfo} />
          <PopupDetail label="Location" value={responseData.location} />
          <PopupDetail label="Latitude" value={responseData.latitude} />
          <PopupDetail label="Longitude" value={responseData.longitude} />

          <PopupDetail label="Email" value={responseData.user?.email} />
          <PopupDetail label="Role" value={responseData.user?.role} />
        </div>

        <button
          className="mt-6 w-full bg-blue-950 text-white py-2 rounded-lg hover:bg-gray-900"
          onClick={onClose}
        >
          Close
        </button>
      </div>
    </div>
  );
}

function PopupDetail({ label, value }) {
  if (!value) return null;
  return (
    <div className="flex justify-between bg-gray-100 p-2 rounded-lg">
      <span className="font-semibold text-gray-700">{label}</span>
      <span className="text-gray-900">{value}</span>
    </div>
  );
}
