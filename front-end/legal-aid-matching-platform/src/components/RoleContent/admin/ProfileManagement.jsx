import { useEffect, useState } from "react";
import { FaSave } from "react-icons/fa";
import { useAlert } from "../../../context/AlertContext";

export default function ProfileManagement() {
  const { showAlert } = useAlert();
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState("ALL");
  const [lawyers, setLawyers] = useState([]);
  const [ngos, setNgos] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [editMode, setEditMode] = useState(false);

  const token = sessionStorage.getItem("accessToken");

  // Load unverified profiles
async function loadProfiles(currentPage = page) {
  setLoading(true);

  try {
    let url = `http://localhost:8080/admin/lawyer-ngo?page=${currentPage}&size=${size}`;

    if (activeTab === "LAWYER") {
      url += "&role=LAWYER&pendingOnly=true";
    } else if (activeTab === "NGO") {
      url += "&role=NGO&pendingOnly=true";
    }
    // ALL → no filters

    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
    });

    const data = await res.json();

    setLawyers(data.content.filter(u => u.role === "LAWYER"));
    setNgos(data.content.filter(u => u.role === "NGO"));

    setTotalPages(data.totalPages || 0);

  } catch (err) {
    console.error(err);
  } finally {
    setLoading(false);
  }
}

  useEffect(() => {
    document.title = "Profile Management | Legal Aid";
    loadProfiles(page);
  }, [page , activeTab]);

  const users =
    activeTab === "LAWYER"
      ? lawyers
      : activeTab === "NGO"
      ? ngos
      : [...lawyers, ...ngos];


  // ---------------- VERIFY ----------------
  async function verifyUser(username) {
    await fetch(`http://localhost:8080/admin/verify/${username}/true`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}` },
    });

    showAlert("User Verified ✔");
    loadProfiles(page);
  }

  // ---------------- REJECT ----------------
  async function rejectUser(username) {
    await fetch(`http://localhost:8080/admin/verify/${username}/false`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}` },
    });

    showAlert("User Rejected ❌");
    loadProfiles(page);
  }

  // ---------------- ADMIN UPDATE ----------------
  async function updateByAdmin() {
    const username = selectedUser.username;

    let payload = {};

    if (selectedUser.role === "LAWYER") {
      payload = {
        name: selectedUser.name,
        expertise: selectedUser.expertise,
        location: selectedUser.location,
        contactInfo: selectedUser.contactInfo,
        language: selectedUser.language,
      };
    }

    if (selectedUser.role === "NGO") {
      payload = {
        organization: selectedUser.organization,
        location: selectedUser.location,
        contactInfo: selectedUser.contactInfo,
        language: selectedUser.language,
      };
    }

    await fetch(`http://localhost:8080/admin/lawyers-ngos/${username}`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    showAlert("Profile Updated Successfully ✔");
    setEditMode(false); // lock fields again
  }

  return (
    <div className="p-2 sm:p-4">
      <h1 className="text-2xl sm:text-3xl font-bold mb-4 sm:mb-6">Profile Management</h1>

      {/* TABS */}
      <div className="flex gap-2 sm:gap-4 mb-4 sm:mb-6">
        <button
          onClick={() => {
           setActiveTab("ALL"); setPage(0); }}
          className={`px-3 sm:px-4 py-2 rounded-t-lg font-semibold text-sm sm:text-base ${
            activeTab === "ALL"
              ? "bg-blue-950 text-white"
              : "bg-gray-200 text-gray-700"
          }`}
        >
          All
        </button>
        <button
          onClick={() => setActiveTab("LAWYER")}
          className={`px-3 sm:px-4 py-2 rounded-t-lg font-semibold text-sm sm:text-base ${
            activeTab === "LAWYER"
              ? "bg-blue-950 text-white"
              : "bg-gray-200 text-gray-700"
          }`}
        >
          Lawyers
        </button>

        <button
          onClick={() => setActiveTab("NGO")}
          className={`px-3 sm:px-4 py-2 rounded-t-lg font-semibold text-sm sm:text-base ${
            activeTab === "NGO"
              ? "bg-blue-950 text-white"
              : "bg-gray-200 text-gray-700"
          }`}
        >
          NGOs
        </button>
      </div>

      {/* TABLE */}
      <div className="shadow-lg rounded-lg bg-white">
        {/* Mobile Card View */}
        <div className="block md:hidden">
          {loading ? (
            <p className="text-center p-4">Loading...</p>
              ) : users.length === 0 ? (
            <p colSpan="4" className="text-center p-6 text-gray-500 font-medium">
                      {activeTab === "LAWYER"
                        ? "No lawyers pending verification"
                        : activeTab === "NGO"
                        ? "No NGOs pending verification"
                        : "No profiles found"}
                    </p>
        ) : (
          users.map((u, i) => (
            <div key={i} className="p-4 border-b last:border-b-0 hover:bg-gray-50">
              <div className="space-y-2">
                <div>
                  <p className="font-semibold text-gray-900">{u.fullName}</p>
                  <p className="text-sm text-gray-600">@{u.username}</p>
                </div>
                <p className="text-sm text-gray-600">{u.email}</p>
                <button
                  onClick={() => {
                    setSelectedUser(u);
                    setEditMode(false);
                  }}
                  className="w-full mt-2 px-3 py-2 bg-blue-950 text-white rounded hover:bg-gray-900 text-sm"
                >
                  View Profile
                </button>
              </div>
            </div>
          )))}
        </div>

        {/* Desktop Table View */}
        <div className="hidden md:block overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-gray-100 border-b">
              <tr>
                <th className="p-3">Full Name</th>
                <th className="p-3">Username</th>
                <th className="p-3">Email</th>
                <th className="p-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="4" className="text-center p-4">
                    Loading...
                  </td>
                </tr>
                ) : users.length === 0 ? (
                  <tr>
                    <td colSpan="4" className="text-center p-6 text-gray-500 font-medium">
                      {activeTab === "LAWYER"
                        ? "No lawyers pending verification"
                        : activeTab === "NGO"
                        ? "No NGOs pending verification"
                        : "No profiles found"}
                    </td>
                  </tr>
                ) : (
              users.map((u, i) => (
                <tr key={i} className="border-b hover:bg-gray-50">
                  <td className="p-3">{u.fullName}</td>
                  <td className="p-3">{u.username}</td>
                  <td className="p-3">{u.email}</td>
                  <td className="p-3">
                    <button
                      onClick={() => {
                        setSelectedUser(u);
                        setEditMode(false);
                      }}
                      className="px-3 py-1 bg-blue-950 text-white rounded hover:bg-gray-900"
                    >
                      View
                    </button>
                  </td>
                </tr>
              )))}
            </tbody>
          </table>
          <div className="flex justify-between items-center mt-6 mb-2 px-2">
            <button
              onClick={() => setPage(p => Math.max(p - 1, 0))}
              disabled={page === 0}
              className="px-3 py-1 bg-blue-900 text-white rounded disabled:bg-gray-300"
            >
              Prev
            </button>

            <span className="text-sm font-semibold">
              Page {page + 1} of {totalPages || 1}
            </span>

            <button
              onClick={() => setPage(p => Math.min(p + 1, totalPages - 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1 bg-blue-900 text-white rounded disabled:bg-gray-300"
            >
              Next
            </button>
          </div>

        </div>
      </div>

      {/* VIEW MODAL */}
      {selectedUser && (
        <div className="fixed inset-0 bg-black/40 flex justify-center items-center p-2 sm:p-4 pt-16 sm:pt-20 pb-12 sm:pb-16">
          <div className="bg-white p-4 sm:p-6 rounded-xl shadow-xl w-full max-w-sm sm:max-w-4xl max-h-[80vh] sm:max-h-[75vh] overflow-y-auto relative">

            {/* CLOSE */}
            <button
              className="absolute top-2 right-2 sm:top-4 sm:right-4 text-xl sm:text-2xl hover:bg-gray-100 w-8 h-8 rounded-full flex items-center justify-center z-10"
              onClick={() => setSelectedUser(null)}
            >
              ×
            </button>

            <h2 className="text-xl sm:text-2xl font-bold mb-4 sm:mb-6 pr-8">Profile Details</h2>

            {/* LAWYER FIELDS */}
            {selectedUser.role === "LAWYER" && (
              <div className="space-y-3 sm:space-y-4">
                {/* Basic Information */}
                <div>
                  <h3 className="text-sm sm:text-base font-semibold mb-2 text-blue-900 border-b pb-1">Basic Information</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2">
                    <EditableField
                      label="Full Name"
                      value={selectedUser.fullName}
                      editable={false}
                    />
                    <EditableField
                      label="Username"
                      value={selectedUser.username}
                      editable={false}
                    />
                    <EditableField
                      label="Email"
                      value={selectedUser.email}
                      editable={false}
                    />
                    <EditableField
                      label="Created At"
                      value={new Date(selectedUser.createdAt).toLocaleDateString()}
                      editable={false}
                    />
                  </div>
                </div>

                {/* Professional Information */}
                <div>
                  <h3 className="text-sm sm:text-base font-semibold mb-2 text-blue-900 border-b pb-1">Professional Information</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 mb-2">
                    <EditableField
                      label="Lawyer Name"
                      value={selectedUser.name}
                      editable={editMode}
                      onChange={(e) =>
                        setSelectedUser({
                          ...selectedUser,
                          name: e.target.value,
                        })
                      }
                    />
                    <EditableField
                      label="Bar Registration No"
                      value={selectedUser.barRegistrationNo}
                      editable={false}
                    />
                    <EditableField
                      label="Specialization"
                      value={selectedUser.specialization}
                      editable={false}
                    />
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <EditableField
                      label="Experience Years"
                      value={selectedUser.experienceYears}
                      editable={false}
                    />
                    <EditableField
                      label="Expertise"
                      value={selectedUser.expertise}
                      editable={editMode}
                      onChange={(e) =>
                        setSelectedUser({
                          ...selectedUser,
                          expertise: e.target.value,
                        })
                      }
                    />
                  </div>
                </div>

                {/* Contact & Location */}
                <div>
                  <h3 className="text-sm sm:text-base font-semibold mb-2 text-blue-900 border-b pb-1">Contact & Location</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 mb-2">
                    <EditableField
                      label="Language"
                      value={selectedUser.language}
                      editable={editMode}
                      onChange={(e) =>
                        setSelectedUser({
                          ...selectedUser,
                          language: e.target.value,
                        })
                      }
                    />
                    <EditableField
                      label="City"
                      value={selectedUser.city}
                      editable={false}
                    />
                    <EditableField
                      label="Contact Info"
                      value={selectedUser.contactInfo}
                      editable={editMode}
                      onChange={(e) =>
                        setSelectedUser({
                          ...selectedUser,
                          contactInfo: e.target.value,
                        })
                      }
                    />
                  </div>
                  <div className="grid grid-cols-1 gap-2">
                    <EditableField
                      label="Location"
                      value={selectedUser.location}
                      editable={editMode}
                      onChange={(e) =>
                        setSelectedUser({
                          ...selectedUser,
                          location: e.target.value,
                        })
                      }
                    />
                    <EditableField
                      label="Bio"
                      value={selectedUser.bio}
                      editable={false}
                      isTextarea={true}
                    />
                  </div>
                </div>
              </div>
            )}

            {/* NGO FIELDS */}
            {selectedUser.role === "NGO" && (
              <div className="space-y-3 sm:space-y-4">
                {/* Basic Information */}
                <div>
                  <h3 className="text-sm sm:text-base font-semibold mb-2 text-green-900 border-b pb-1">Basic Information</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2">
                    <EditableField
                      label="Full Name"
                      value={selectedUser.fullName}
                      editable={false}
                    />
                    <EditableField
                      label="Username"
                      value={selectedUser.username}
                      editable={false}
                    />
                    <EditableField
                      label="Email"
                      value={selectedUser.email}
                      editable={false}
                    />
                    <EditableField
                      label="Created At"
                      value={new Date(selectedUser.createdAt).toLocaleDateString()}
                      editable={false}
                    />
                  </div>
                </div>

                {/* Organization Information */}
                <div>
                  <h3 className="text-sm sm:text-base font-semibold mb-2 text-green-900 border-b pb-1">Organization Information</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 mb-2">
                    <EditableField
                      label="Organization"
                      value={selectedUser.organization}
                      editable={editMode}
                      onChange={(e) =>
                        setSelectedUser({
                          ...selectedUser,
                          organization: e.target.value,
                        })
                      }
                    />
                    <EditableField
                      label="NGO Name"
                      value={selectedUser.ngoName}
                      editable={false}
                    />
                    <EditableField
                      label="Registration No"
                      value={selectedUser.registrationNo}
                      editable={false}
                    />
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <EditableField
                      label="Website"
                      value={selectedUser.website}
                      editable={false}
                    />
                    <EditableField
                      label="Description"
                      value={selectedUser.description}
                      editable={false}
                      isTextarea={true}
                    />
                  </div>
                </div>

                {/* Contact & Location */}
                <div>
                  <h3 className="text-sm sm:text-base font-semibold mb-2 text-green-900 border-b pb-1">Contact & Location</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 mb-2">
                    <EditableField
                      label="Language"
                      value={selectedUser.language}
                      editable={editMode}
                      onChange={(e) =>
                        setSelectedUser({
                          ...selectedUser,
                          language: e.target.value,
                        })
                      }
                    />
                    <EditableField
                      label="City"
                      value={selectedUser.city}
                      editable={false}
                    />
                    <EditableField
                      label="Contact Info"
                      value={selectedUser.contactInfo}
                      editable={editMode}
                      onChange={(e) =>
                        setSelectedUser({
                          ...selectedUser,
                          contactInfo: e.target.value,
                        })
                      }
                    />
                  </div>
                  <div className="grid grid-cols-1 gap-2">
                    <EditableField
                      label="Location"
                      value={selectedUser.location}
                      editable={editMode}
                      onChange={(e) =>
                        setSelectedUser({
                          ...selectedUser,
                          location: e.target.value,
                        })
                      }
                    />
                  </div>
                </div>
              </div>
            )}

            {/* BUTTONS */}
            <div className="mt-6 flex flex-col sm:flex-row justify-between gap-3 sm:gap-0 px-2">
              <button
                disabled={selectedUser?.verified !== null}
                onClick={() => rejectUser(selectedUser.username)}
                className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 text-sm sm:text-base"
              >
                Reject
              </button>

              <button
                disabled={selectedUser?.verified !== null}
                onClick={() => verifyUser(selectedUser.username)}
                className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 text-sm sm:text-base"
              >
                Verify
              </button>
            </div>

            {/* UPDATE BUTTON */}
            {!editMode ? (
              <button
                onClick={() => setEditMode(true)}
                className="mt-4 w-full bg-blue-900 text-white py-2 rounded hover:bg-blue-950 text-sm sm:text-base"
              >
                Update Profile (Admin)
              </button>
            ) : (
              <button
                onClick={updateByAdmin}
                className="mt-4 w-full bg-green-700 text-white py-2 rounded hover:bg-green-800 flex items-center justify-center gap-2 text-sm sm:text-base"
              ><FaSave/>
                Save Changes
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

/** EDITABLE FIELD COMPONENT */
function EditableField({ label, value, editable, onChange, isTextarea = false }) {
  return (
    <div className="mb-2">
      <p className="font-medium text-xs text-gray-700 mb-1">{label}</p>

      {editable ? (
        isTextarea ? (
          <textarea
            className="w-full border p-2 rounded resize-none text-sm"
            rows="2"
            value={value || ""}
            onChange={onChange}
          />
        ) : (
          <input
            className="w-full border p-2 rounded text-sm"
            value={value || ""}
            onChange={onChange}
          />
        )
      ) : (
        <p className="border p-2 rounded bg-gray-100 text-sm min-h-[32px] flex items-center">
          {value || "—"}
        </p>
      )}
    </div>
  );
}
