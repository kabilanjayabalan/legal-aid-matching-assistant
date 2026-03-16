import React, { useEffect, useRef, useState } from "react";
import { FaEdit, FaSave } from "react-icons/fa";

export default function NGOEditProfile() {
  const token = sessionStorage.getItem("accessToken");
  const debounceRef = useRef(null);

  const [form, setForm] = useState({
    organization: "",
    contactInfo: "",
    location: "",
    ngoName: "",
    registrationNo: "",
    city: "",
    website: "",
    description: "",
    language: "",
    latitude: "",
    longitude: ""
  });

  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [responseData, setResponseData] = useState(null);
  const [showPopup, setShowPopup] = useState(false);

  // NEW (same as Lawyer)
  const [citySuggestions, setCitySuggestions] = useState([]);
  const [citySelected, setCitySelected] = useState(false);
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
          "http://localhost:8080/profile/update/ngo",
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
          organization: profile.organization || "",
          contactInfo: profile.contactInfo || "",
          location: profile.location || "",
          ngoName: profile.ngoName || "",
          registrationNo: profile.registrationNo || "",
          city: profile.city || "",
          website: profile.website || "",
          description: profile.description || "",
          language: profile.language || "",
          latitude: profile.latitude || "",
          longitude: profile.longitude || ""
        });

        setLoading(false);
      } catch (err) {
        console.error("Error loading NGO profile:", err);
      }
    };

    loadProfile();
  }, [token]);

  // ---------------- LOCATION SEARCH ----------------
  const searchCity = (query) => {
    if (!query || query.length < 3) {
      setCitySuggestions([]);
      return;
    }

    if (debounceRef.current) clearTimeout(debounceRef.current);

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

  // ---------------- INPUT HANDLER ----------------
  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  // ---------------- SUBMIT ----------------
  const handleSubmit = (e) => {
    e.preventDefault();

    fetch("http://localhost:8080/profile/update/ngo", {
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

          {/* IMAGE — NOT REMOVED */}
          <div className="flex flex-col items-center">
            <img
              src={`https://ui-avatars.com/api/?name=${encodeURIComponent(form.ngoName || "User")}&background=random`}
              alt="NGO Avatar"
              className="w-35 h-32 rounded-full object-cover border-4 border-indigo-500 shadow"
            />
          </div>

          <div className="flex-1">
            <h1 className="text-2xl font-bold text-gray-800">NGO Profile</h1>
            <span className="px-3 py-1 mt-2 inline-block rounded-full bg-indigo-100 text-indigo-700 text-sm font-medium">
              Role: NGO
            </span>
            <p className="text-gray-500 mt-2">
              Update your organization details, registration info, website, and description.
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
          <Input label="Organization" name="organization" value={form.organization} onChange={handleChange} isEditing={isEditing} />
          <Input label="Contact Info" name="contactInfo" value={form.contactInfo} onChange={handleChange} isEditing={isEditing} />
          <Input label="NGO Name" name="ngoName" value={form.ngoName} onChange={handleChange} isEditing={isEditing} />
          <Input label="Registration No" name="registrationNo" value={form.registrationNo} onChange={handleChange} isEditing={isEditing} />
          <Input label="City" name="city" value={form.city} onChange={handleChange} isEditing={isEditing} />
          <Input label="Website" name="website" value={form.website} onChange={handleChange} isEditing={isEditing} />
          <Input label="Language" name="language" value={form.language} onChange={handleChange} isEditing={isEditing} />

          {/* LOCATION (same logic as Lawyer) */}
          <div className="relative col-span-2">
            <label className="block text-sm font-semibold mb-1">Location</label>
            <input
              name="location"
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
                  if (!citySelected && !usingCurrentLocation) {
                    fetchLatLng(form.location);
                  }
                  setCitySuggestions([]);
                }, 300);
              }}
              className={`w-full border rounded-lg px-3 py-2 ${!isEditing ? "bg-gray-200" : ""}`}
            />

            <div className="flex items-center gap-2 mt-2">
              <span className="text-xs text-gray-400">OR</span>
              <button
                type="button"
                disabled={!isEditing}
                className="text-xs px-3 py-1 rounded bg-indigo-600 text-white"
                onClick={() => {
                  navigator.geolocation.getCurrentPosition(
                    (pos) => {
                      setUsingCurrentLocation(true);
                      setCitySelected(true);

                      setForm((prev) => ({
                        ...prev,
                        latitude: pos.coords.latitude,
                        longitude: pos.coords.longitude,
                      }));

                      reverseGeocode(
                        pos.coords.latitude,
                        pos.coords.longitude
                      );
                    },
                    () => alert("Location permission denied")
                  );
                }}
              >
                📍 Use My Location
              </button>
            </div>

            <div className="bg-gray-100 p-4 rounded-lg mt-3">
              <p className="text-sm"><strong>Latitude:</strong> {form.latitude || "—"}</p>
              <p className="text-sm"><strong>Longitude:</strong> {form.longitude || "—"}</p>
            </div>

            {citySuggestions.length > 0 && isEditing && (
              <ul className="absolute z-50 w-full bg-white border rounded-lg shadow mt-1 max-h-48 overflow-auto">
                {citySuggestions.map((loc, idx) => (
                  <li
                    key={idx}
                    className="px-3 py-2 hover:bg-indigo-100 cursor-pointer"
                    onMouseDown={(e) => {
                      e.preventDefault();
                      setCitySelected(true);
                      setForm({
                        ...form,
                        location: loc.display_name,
                        latitude: loc.lat,
                        longitude: loc.lon
                      });
                      setCitySuggestions([]);
                    }}
                  >
                    {loc.display_name}
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* DESCRIPTION — NOT REMOVED */}
          <div className="col-span-2">
            <label className="block text-sm font-semibold mb-1">Description</label>
            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              disabled={!isEditing}
              className={`w-full border rounded-lg px-3 py-2 ${!isEditing ? "bg-gray-200" : ""}`}
              rows="3"
            />
          </div>
        </form>
      </div>
    </div>
  );
}

/* INPUT */
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
        className={`w-full border rounded-lg px-3 py-2 ${!isEditing ? "bg-gray-200" : ""}`}
      />
    </div>
  );
}
