import React, { useEffect, useState } from "react";
import { getMyCases, updateCase } from "../../../services/api";
import api from "../../../services/api";
import { LuMoveLeft } from "react-icons/lu";
import { FiSearch, FiEdit2, FiX, FiCheck } from "react-icons/fi";

export default function CaseManagement() {
  /* ---------- STATE ---------- */
  const [cases, setCases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedCase, setSelectedCase] = useState(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({});
  const [timeline, setTimeline] = useState([]);
  const [timelineLoading, setTimelineLoading] = useState(false);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [withdrawReason, setWithdrawReason] = useState("Withdrawn by citizen");


  /* ---------- FETCH CASES ---------- */
  const fetchCases = async () => {
    try {
      const response = await getMyCases();
      setCases(response.data);
    } catch (error) {
      console.error("Failed to fetch cases:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCases();
  }, []);

  const handleWithdrawCase = async () => {
  try {
    await api.post(`/cases/${selectedCase.id}/close`, {
      reason: withdrawReason,
    });

    // Update UI
    setShowWithdrawModal(false);
    fetchCases();

    // Refresh selected case
    const updated = await api.get(`/cases/${selectedCase.id}`);
    setSelectedCase(updated.data);
  } catch (error) {
    console.error("Failed to withdraw case", error);
    alert("Failed to withdraw case. Please try again.");
  }
};


  /* ---------- FILTER ---------- */
  const filteredCases = cases.filter(
    (c) =>
      (c.title.toLowerCase().includes(search.toLowerCase()) ||
        c.description.toLowerCase().includes(search.toLowerCase())) &&
      (statusFilter === "" || c.status === statusFilter)
  );

  const formatStatus = (status) =>
    status ? status.replace(/_/g, " ") : "";

  /* ---------- DOWNLOAD EVIDENCE FILE ---------- */
  const handleDownload = async (file) => {
    try {
      const response = await api.get(`/cases/evidence/${file.fileId}`, {
        responseType: "blob",
      });
      
      // Create blob URL and trigger download
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", file.fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Failed to download file:", error);
      alert("Failed to download file. Please try again.");
    }
  };

  /* ---------- EDIT HANDLERS ---------- */
  const handleEditClick = () => {
    setEditForm({
      title: selectedCase.title,
      description: selectedCase.description,
      category: selectedCase.category,
      location: selectedCase.location,
      city: selectedCase.city,
      latitude: selectedCase.latitude,
      longitude: selectedCase.longitude,
      contactInfo: selectedCase.contactInfo,
      preferredLanguage: selectedCase.preferredLanguage,
      parties: selectedCase.parties,
      isUrgent: selectedCase.isUrgent,
      expertiseTags: selectedCase.expertiseTags || [],
    });
    setIsEditing(true);
  };

  const handleEditChange = (e) => {
    const { name, value, type, checked } = e.target;
    setEditForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSaveEdit = async () => {
    try {
      const response = await updateCase(selectedCase.id, editForm);
      setSelectedCase(response.data);
      setIsEditing(false);
      fetchCases(); // Refresh list
    } catch (error) {
      console.error("Failed to update case:", error);
      alert("Failed to update case. Please try again.");
    }
  };

  return (
    <div className="relative min-h-full overflow-x-hidden bg-slate-50 px-4 py-4 sm:p-4">
      {/* ================= CASE DETAIL VIEW ================= */}
      {selectedCase ? (
        <>
          {/* Back Button */}
          <button
            onClick={() => {
              setSelectedCase(null);
              setIsEditing(false);
            }}
            className="mb-2 inline-flex items-center gap-2 rounded-lg border bg-white px-4 py-2 text-sm font-semibold text-gray-700 shadow-sm transition hover:border-gray-300 hover:bg-gray-50"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-100">
              <LuMoveLeft className="text-gray-700" />
            </span>
            Back to Dashboard
          </button>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            {/* LEFT – CASE DETAILS */}
            <div className="lg:col-span-2 space-y-4 rounded-xl border bg-white p-6 shadow-sm">
              {selectedCase.caseNumber && (
                <div className="mb-2">
                  <span className="inline-block rounded-lg bg-blue-100 px-3 py-1.5 text-sm font-bold text-blue-800">
                    Case #{selectedCase.caseNumber}
                  </span>
                  {selectedCase.caseType && (
                    <span className="ml-2 text-xs text-gray-500">
                      Type: {selectedCase.caseType}
                    </span>
                  )}
                </div>
              )}
              <div className="flex items-center justify-between">
                {isEditing ? (
                  <input
                    name="title"
                    value={editForm.title || ""}
                    onChange={handleEditChange}
                    className="w-full rounded-md border p-2 text-xl font-bold text-gray-900"
                  />
                ) : (
                  <h2 className="text-xl font-bold text-gray-900">
                    {selectedCase.title}
                  </h2>
                )}
                
                {!isEditing && selectedCase.status !== "CLOSED" && (
                  <button
                    onClick={handleEditClick}
                    className="ml-4 flex items-center gap-2 rounded-lg border bg-gray-50 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-100"
                  >
                    <FiEdit2 className="h-4 w-4" />
                    Edit
                  </button>
                )}
              </div>

              <div className="flex flex-wrap items-center gap-3">
                <span className="text-sm text-gray-500">
                  Category:{" "}
                  {isEditing ? (
                    <input
                      name="category"
                      value={editForm.category || ""}
                      onChange={handleEditChange}
                      className="rounded-md border p-1 text-sm"
                    />
                  ) : (
                    <b>{selectedCase.category?.trim() || "-"}</b>
                  )}
                </span>

                <span
                  className={`rounded-full px-3 py-1 text-xs font-semibold ${
                    selectedCase.status === "OPEN"
                      ? "bg-indigo-100 text-indigo-800"
                      : selectedCase.status === "IN_PROGRESS"
                      ? "bg-amber-100 text-amber-800"
                      : "bg-emerald-100 text-emerald-800"
                  }`}
                >
                  {formatStatus(selectedCase.status)}
                </span>
                {selectedCase.status !== "CLOSED" && (
  <button
    onClick={() => setShowWithdrawModal(true)}
    className="ml-3 rounded-lg bg-red-50 px-3 py-1.5 text-sm font-semibold text-red-700 hover:bg-red-100"
  >
    Withdraw Case
  </button>
)}

              </div>

              <div>
                <p className="text-sm font-semibold text-gray-700">
                  Description
                </p>
                {isEditing ? (
                  <textarea
                    name="description"
                    value={editForm.description || ""}
                    onChange={handleEditChange}
                    rows={4}
                    className="mt-1 w-full rounded-md border p-2 text-sm text-gray-600"
                  />
                ) : (
                  <p className="mt-1 text-sm text-gray-600">
                    {selectedCase.description?.trim() || "-"}
                  </p>
                )}
              </div>

              <div className="grid grid-cols-1 gap-4 text-sm md:grid-cols-2">
                <Info 
                  label="Location" 
                  value={selectedCase.location} 
                  isEditing={isEditing}
                  editValue={editForm.location}
                  name="location"
                  onChange={handleEditChange}
                />
                <Info
                  label="City"
                  value={selectedCase.city}
                  isEditing={isEditing}
                  editValue={editForm.city}
                  name="city"
                  onChange={handleEditChange}
                />
                <Info
                  label="Preferred Language"
                  value={selectedCase.preferredLanguage}
                  isEditing={isEditing}
                  editValue={editForm.preferredLanguage}
                  name="preferredLanguage"
                  onChange={handleEditChange}
                />
                <Info
                  label="Contact Info"
                  value={selectedCase.contactInfo}
                  isEditing={isEditing}
                  editValue={editForm.contactInfo}
                  name="contactInfo"
                  onChange={handleEditChange}
                />
                <Info
                  label="Involved Parties"
                  value={selectedCase.parties}
                  isEditing={isEditing}
                  editValue={editForm.parties}
                  name="parties"
                  onChange={handleEditChange}
                />
                 <div className="flex items-center gap-2">
                    <p className="font-semibold text-gray-700">Urgent:</p>
                    {isEditing ? (
                        <input
                            type="checkbox"
                            name="isUrgent"
                            checked={editForm.isUrgent || false}
                            onChange={handleEditChange}
                            className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                        />
                    ) : (
                        <p className="text-gray-600">{selectedCase.isUrgent ? "Yes" : "No"}</p>
                    )}
                </div>
              </div>

              {/* Evidence Files */}
              {selectedCase.evidenceFiles && selectedCase.evidenceFiles.length > 0 && (
                <div>
                  <p className="text-sm font-semibold text-gray-700 mb-2">
                    Evidence Files
                  </p>
                  <div className="space-y-2">
                    {selectedCase.evidenceFiles.map((file) => (
                      <div
                        key={file.fileId}
                        className="flex items-center justify-between rounded-lg border bg-gray-50 p-3 text-sm"
                      >
                        <span className="text-gray-700">{file.fileName}</span>
                        <button
                          onClick={() => handleDownload(file)}
                          className="rounded bg-blue-950 px-3 py-1 text-xs font-semibold text-white transition hover:bg-blue-900"
                        >
                          Download
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="flex gap-6 border-t pt-2 text-xs text-gray-500">
                <span>
                  Created:{" "}
                  {new Date(selectedCase.createdAt).toLocaleString()}
                </span>
                <span>
                  Updated:{" "}
                  {new Date(selectedCase.updatedAt).toLocaleString()}
                </span>
              </div>

              {isEditing && (
                <div className="mt-4 flex justify-end gap-3">
                  <button
                    onClick={() => setIsEditing(false)}
                    className="flex items-center gap-2 rounded-lg border bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                  >
                    <FiX className="h-4 w-4" />
                    Cancel
                  </button>
                  <button
                    onClick={handleSaveEdit}
                    className="flex items-center gap-2 rounded-lg bg-blue-950 px-4 py-2 text-sm font-medium text-white hover:bg-blue-900"
                  >
                    <FiCheck className="h-4 w-4" />
                    Save Changes
                  </button>
                </div>
              )}
            </div>
            {/* RIGHT – SIDEBAR */}
            <div className="space-y-4">
              {/* ================= CASE TIMELINE ================= */}
              <div className="h-fit rounded-xl border bg-white p-4 shadow-sm">
                <h3 className="mb-3 text-sm font-bold text-gray-800">
                  Case Timeline
                </h3>

                <ol className="relative ml-3">
                {timeline.map((t, idx) => {
                  const isLast = idx === timeline.length - 1;
                return (
                  <li key={idx} className="relative ml-6 pb-6">
                    {/* Vertical line (ONLY if not last item) */}
                    {!isLast && (
                      <span className="absolute -left-[24px] top-3 h-full w-px bg-gray-200" />
                    )}
                    {/* Dot */}
                    <span
                      className={`absolute -left-[30px] top-1 h-3 w-3 rounded-full border-2 border-white ${
                      t.status === "OPEN"
                        ? "bg-indigo-500"
                        : t.status === "IN_PROGRESS"
                        ? "bg-amber-500"
                        : "bg-emerald-500"
                      }`}
                    />
                    <p className="text-sm font-semibold text-gray-900">
                      {t.label}
                    </p>
                    <p className="text-xs text-gray-500">
                      {new Date(t.timestamp).toLocaleString()}
                    </p>
                    {t.performedBy && (
                      <p className="text-xs text-gray-600">
                        By: {t.performedBy}
                      </p>
                    )}
                    {t.reason && (
                      <p className="mt-1 text-xs text-gray-600 italic">
                        Reason: {t.reason}
                      </p>
                    )}
                  </li>
                );
              })}
              </ol>
            </div>
            {/* Assigned Lawyer */}
            {selectedCase.status === "IN_PROGRESS" &&
              selectedCase.assignedToEmail && (
                <div className="h-fit rounded-xl border border-indigo-100 bg-indigo-50 p-3 shadow-sm">
                  <h4 className="mb-2 text-sm font-bold text-indigo-900">
                    Assigned Legal Support
                  </h4>
                  <p className="text-sm text-indigo-800">
                    <b>Email:</b> {selectedCase.assignedToEmail}
                  </p>
                </div>
              )}
              </div>
          </div>
        </>
      ) : (
        /* ================= DASHBOARD ================= */
        <>
          {/* Header */}
          <div className="mb-6">
            <h2 className="text-xl font-bold text-gray-900 sm:text-2xl">
              Case Management
            </h2>
            <p className="mt-1 text-sm text-gray-500">
              Track and manage your submitted legal cases
            </p>
          </div>

          {/* Toolbar */}
          <div className="mb-6 flex flex-col gap-3 sm:flex-row">
            {/* Search */}
            <div className="relative w-full">
              <FiSearch className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search by title or description..."
                className="h-14 w-full rounded-lg border border-blue-950 pl-10 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-blue-950"
              />
            </div>

            {/* Status Filter */}
            <div className="relative w-full sm:w-48">
              <button
                onClick={() => setIsOpen(!isOpen)}
                className="flex w-full items-center justify-between rounded-lg border bg-white px-4 py-3 text-sm"
              >
                <span>
                  {statusFilter
                    ? statusFilter.replace("_", " ")
                    : "All Status"}
                </span>
                <span className="text-gray-400">▾</span>
              </button>

              {isOpen && (
                <div className="absolute z-20 mt-1 w-full rounded-lg border bg-white shadow-lg">
                  {[
                    { label: "All Status", value: "" },
                    { label: "Open", value: "OPEN" },
                    { label: "In Progress", value: "IN_PROGRESS" },
                    { label: "Closed", value: "CLOSED" },
                  ].map((opt) => (
                    <div
                      key={opt.label}
                      onClick={() => {
                        setStatusFilter(opt.value);
                        setIsOpen(false);
                      }}
                      className="cursor-pointer px-4 py-2 text-sm hover:bg-gray-100"
                    >
                      {opt.label}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Case Grid */}
          {loading ? (
            <p className="text-gray-500">Loading cases...</p>
          ) : (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
              {filteredCases.map((c) => (
                <div
                  key={c.id}
                  onClick={async () => {
                    setSelectedCase(c);
                    setTimeline([]);
                    setTimelineLoading(true);

                    try {
                      const res = await api.get(`/cases/${c.id}/timeline`);
                      setTimeline(res.data);
                    } catch (err) {
                      console.error("Failed to fetch timeline", err);
                    } finally {
                      setTimelineLoading(false);
                    }
                  }}
                  className="cursor-pointer rounded-xl border bg-white p-4 shadow-sm transition hover:border-blue-300 hover:shadow-md"
                >
                  {c.caseNumber && (
                    <div className="mb-2 inline-block rounded-md bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">
                      Case #{c.caseNumber}
                    </div>
                  )}
                  <h3 className="truncate font-semibold text-gray-900">
                    {c.title}
                  </h3>

                  <p className="mt-1 text-xs text-gray-500">
                    Category: {c.category?.trim() || "-"}
                  </p>

                  <span
                    className={`mt-3 inline-block rounded-full px-3 py-1 text-xs font-semibold ${
                      c.status === "OPEN"
                        ? "bg-indigo-100 text-indigo-800"
                        : c.status === "IN_PROGRESS"
                        ? "bg-amber-100 text-amber-800"
                        : "bg-emerald-100 text-emerald-800"
                    }`}
                  >
                    {formatStatus(c.status)}
                  </span>
                </div>
                
              ))}

              {filteredCases.length === 0 && (
                <p className="text-sm text-gray-500">
                  No cases found.
                </p>
              )}
            </div>
          )}
        </>
      )}
  {showWithdrawModal && (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
    <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-lg">
      <h3 className="text-lg font-bold text-gray-900">
        Withdraw Case
      </h3>

      <p className="mt-2 text-sm text-gray-600">
        Are you sure you want to withdraw this case?
      </p>

      {/* Reason options */}
      <div className="mt-4 space-y-2">
        {[
          "Withdrawn by citizen",
          "Issue resolved personally",
          "Incorrect case submitted"
        ].map((reason) => (
          <label
            key={reason}
            className="flex items-center gap-2 text-sm text-gray-700"
          >
            <input
              type="radio"
              name="withdrawReason"
              value={reason}
              checked={withdrawReason === reason}
              onChange={() => setWithdrawReason(reason)}
            />
            {reason}
          </label>
        ))}
      </div>

      {/* Actions */}
      <div className="mt-6 flex justify-end gap-3">
        <button
          onClick={() => setShowWithdrawModal(false)}
          className="rounded-lg border px-4 py-2 text-sm"
        >
          Cancel
        </button>

        <button
          onClick={handleWithdrawCase}
          className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700"
        >
          Confirm Withdraw
        </button>
      </div>
    </div>
  </div>
)}

    </div>
  );

}

/* ---------- REUSABLE INFO ROW ---------- */
function Info({ label, value, isEditing, editValue, name, onChange }) {
  return (
    <div>
      <p className="font-semibold text-gray-700">{label}</p>
      {isEditing ? (
        <input
          name={name}
          value={editValue || ""}
          onChange={onChange}
          className="w-full rounded-md border p-1 text-sm text-gray-600"
        />
      ) : (
        <p className="text-gray-600">{value?.trim() || "-"}</p>
      )}
    </div>
  );
}
