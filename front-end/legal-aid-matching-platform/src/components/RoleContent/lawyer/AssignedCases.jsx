import React, { useState, useEffect } from "react";
import { getMyAssignedCases, getCaseById } from "../../../services/api";
import api from "../../../services/api";
import { LuMoveLeft } from "react-icons/lu";
import { FiSearch, FiFileText, FiAlertCircle, FiDownload } from "react-icons/fi";

export default function AssignedCases({ role }) {
    const [cases, setCases] = useState([]);
    const [selectedCase, setSelectedCase] = useState(null);
    const [selectedCaseDetails, setSelectedCaseDetails] = useState(null);
    const [loadingCaseDetails, setLoadingCaseDetails] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [search, setSearch] = useState("");
    const [timeline, setTimeline] = useState([]);
    const [timelineLoading, setTimelineLoading] = useState(false);
    const [showCloseModal, setShowCloseModal] = useState(false);
    const [closeReason, setCloseReason] = useState("Case resolved");

    useEffect(() => {
        fetchAssignedCases();
    }, [role]);

    const fetchAssignedCases = async () => {
        try {
            setLoading(true);
            const response = await getMyAssignedCases(0, 100);
            const page = response.data || {};
            const casesData = page.content || [];
            
            const mappedCases = casesData.map(caseObj => ({
                id: caseObj.caseId,
                caseId: caseObj.caseId,
                title: caseObj.title,
                category: caseObj.category || "",
                createdAt: caseObj.createdAt,
                assignedTo: role,
                status: caseObj.status,
                caseObj: caseObj 
            }));

            setCases(mappedCases);
            setError(null);
        } catch (err) {
            console.error("Error fetching assigned cases:", err);
            setError(err.response?.data?.message || "Failed to fetch assigned cases");
            setCases([]);
        } finally {
            setLoading(false);
        }
    };

const handleViewDetails = async (c) => {
    setSelectedCase(c);
    setTimeline([]);
    setTimelineLoading(true);

    try {
        const [caseRes, timelineRes] = await Promise.all([
            getCaseById(c.caseId),
            api.get(`/cases/${c.caseId}/timeline`)
        ]);

        setSelectedCaseDetails(caseRes.data);
        setTimeline(timelineRes.data);
    } catch (err) {
        console.error("Error fetching case details:", err);
        setSelectedCaseDetails(null);
    } finally {
        setTimelineLoading(false);
        setLoadingCaseDetails(false);
    }
};

const handleCloseCase = async () => {
    try {
        await api.post(`/cases/${selectedCase.caseId}/close`, {
            reason: closeReason
        });

        setShowCloseModal(false);

        // Refresh details
        const updated = await getCaseById(selectedCase.caseId);
        setSelectedCaseDetails(updated.data);

        const timelineRes = await api.get(`/cases/${selectedCase.caseId}/timeline`);
        setTimeline(timelineRes.data);

        fetchAssignedCases();
    } catch (err) {
        console.error("Failed to close case", err);
        alert("Failed to close case. Please try again.");
    }
};


    const handleBack = () => {
        setSelectedCase(null);
        setSelectedCaseDetails(null);
    };

    const handleDownload = async (file) => {
        try {
            const response = await api.get(`/cases/evidence/${file.fileId}`, {
                responseType: "blob",
            });

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

    const formatStatus = (status) =>
        status ? status.replace(/_/g, " ") : "";

    const filteredCases = cases.filter(
        (c) =>
          c.title.toLowerCase().includes(search.toLowerCase())
    );

    return (
        <div className="relative min-h-full overflow-x-hidden bg-slate-50 px-4 py-4 sm:p-4">
            {selectedCase ? (
                /* ================= CASE DETAIL VIEW ================= */
                <>
                    <button
                        onClick={handleBack}
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
                            {loadingCaseDetails ? (
                                <p className="text-gray-500">Loading details...</p>
                            ) : selectedCaseDetails ? (
                                <>
                                    {selectedCaseDetails.caseNumber && (
                                        <div className="mb-2">
                                            <span className="inline-block rounded-lg bg-blue-100 px-3 py-1.5 text-sm font-bold text-blue-800">
                                                Case #{selectedCaseDetails.caseNumber}
                                            </span>
                                            {selectedCaseDetails.caseType && (
                                                <span className="ml-2 text-xs text-gray-500">
                                                    Type: {selectedCaseDetails.caseType}
                                                </span>
                                            )}
                                        </div>
                                    )}
                                    <div className="flex items-center justify-between">
                                        <h2 className="text-xl font-bold text-gray-900">
                                            {selectedCaseDetails.title}
                                        </h2>
                                    </div>

                                    <div className="flex flex-wrap items-center gap-3">                                        
                                        <span className="text-sm text-gray-500">
                                            Category: <b>{selectedCaseDetails.category?.trim() || "-"}</b>
                                        </span>

                                        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                            selectedCaseDetails.status === "OPEN"
                                            ? "bg-indigo-100 text-indigo-800"
                                            : selectedCaseDetails.status === "IN_PROGRESS"
                                            ? "bg-amber-100 text-amber-800"
                                            : "bg-emerald-100 text-emerald-800"
                                        }`}>
                                            {formatStatus(selectedCaseDetails.status)}
                                        </span>
                                        {selectedCaseDetails.status !== "CLOSED" && (
                                            <button
                                                onClick={() => setShowCloseModal(true)}
                                                className="rounded-lg bg-red-50 px-3 py-1.5 text-sm font-semibold text-red-700 hover:bg-red-100"
                                            >
                                                Close Case
                                            </button>
                                        )}
                                    </div>

                                    <div>
                                        <p className="text-sm font-semibold text-gray-700">
                                            Description
                                        </p>
                                        <p className="mt-1 text-sm text-gray-600">
                                            {selectedCaseDetails.description?.trim() || "-"}
                                        </p>
                                    </div>

                                    <div className="grid grid-cols-1 gap-4 text-sm md:grid-cols-2">
                                        <Info label="Location" value={selectedCaseDetails.location} />
                                        <Info label="City" value={selectedCaseDetails.city} />
                                        <Info label="Client Email" value={selectedCaseDetails.createdByEmail} />
                                        <Info label="Case ID" value={`#${selectedCaseDetails.id}`} />
                                    </div>

                                    {/* Evidence Files */}
                                    {selectedCaseDetails.evidenceFiles && selectedCaseDetails.evidenceFiles.length > 0 && (
                                        <div>
                                            <p className="text-sm font-semibold text-gray-700 mb-2">
                                                Evidence Files
                                            </p>
                                            <div className="space-y-2">
                                                {selectedCaseDetails.evidenceFiles.map((file) => (
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
                                            Created: {new Date(selectedCaseDetails.createdAt).toLocaleString()}
                                        </span>
                                        <span>
                                            Updated: {new Date(selectedCaseDetails.updatedAt).toLocaleString()}
                                        </span>
                                    </div>
                                </>
                            ) : (
                                <p className="text-red-500">Failed to load case details.</p>
                            )}
                        </div>

                        {selectedCaseDetails && (
                        <div className="space-y-4">
                            {/* ================= CASE TIMELINE ================= */}
                            <div className="h-fit rounded-xl border bg-white p-4 shadow-sm">
                                <h3 className="mb-3 text-sm font-bold text-gray-800">
                                    Case Timeline
                                </h3>

                                {timelineLoading ? (
                                    <p className="text-sm text-gray-500">Loading timeline...</p>
                                ) : (
                                    <ol className="relative ml-3">
                                    {timeline.map((t, idx) => {
                                        const isLast = idx === timeline.length - 1;
                                        return (
                                            <li key={idx} className="relative ml-6 pb-6">
                                            {!isLast && (
                                                <span className="absolute -left-[24px] top-3 h-full w-px bg-gray-200" />
                                            )}
                                          <span
                                            className={`absolute -left-[30px] top-1 h-3 w-3 rounded-full border-2 border-white ${
                                            t.status === "OPEN"
                                                ? "bg-indigo-500"
                                                : t.status === "IN_PROGRESS"
                                                ? "bg-amber-500"
                                                : "bg-emerald-500"
                                            }`}
                                          />

                                          <p className="text-sm font-semibold text-gray-900">{t.label}</p>
                                          <p className="text-xs text-gray-500">
                                            {new Date(t.timestamp).toLocaleString()}
                                          </p>

                                        {t.reason && (
                                            <p className="mt-1 text-xs text-gray-600 italic">
                                                Reason: {t.reason}
                                            </p>
                                        )}
                                        </li>
                                        );
                                    })}
                                    </ol>
                                )}
                            </div>

                            {/* ================= CLIENT INFO ================= */}
                            <div className="h-fit rounded-xl border border-indigo-100 bg-indigo-50 p-3 shadow-sm">
                                <h4 className="mb-2 text-sm font-bold text-indigo-900">
                                    Client Information
                                </h4>
                                <p className="text-sm text-indigo-800">
                                    <b>Email:</b> {selectedCaseDetails.createdByEmail}
                                </p>
                            </div>
                        </div>
                    )}
                    </div>
                </>
            ) : (
                /* ================= DASHBOARD ================= */
                <>
                    <div className="mb-6">
                        <h1 className="text-xl font-bold text-gray-900 sm:text-2xl">
                            {role === 'LAWYER' ? 'Assigned Legal Cases' : 'Assigned NGO Cases'}
                        </h1>
                        <p className="mt-1 text-sm text-gray-500">
                            Manage and track your assigned cases
                        </p>
                    </div>

                    {/* Toolbar */}
                    <div className="mb-6 flex flex-col gap-3 sm:flex-row">
                        <div className="relative w-full">
                            <FiSearch className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                            <input
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                placeholder="Search by title..."
                                className="h-14 w-full rounded-lg border border-blue-950 pl-10 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-blue-950"
                            />
                        </div>
                    </div>

                    {/* Loading State */}
                    {loading && (
                        <div className="flex justify-center py-8">
                            <p className="text-gray-500">Loading assigned cases...</p>
                        </div>
                    )}

                    {/* Error State */}
                    {error && (
                        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">
                            <div className="flex items-center gap-2">
                                <FiAlertCircle />
                                <p>{error}</p>
                            </div>
                        </div>
                    )}

                    {/* Empty State */}
                    {!loading && filteredCases.length === 0 && !error && (
                        <div className="rounded-xl border bg-white p-8 text-center shadow-sm">
                            <FiFileText className="mx-auto h-10 w-10 text-gray-300" />
                            <h3 className="mt-2 text-sm font-medium text-gray-900">No cases found</h3>
                        </div>
                    )}

                    {/* Cases Grid */}
                    {!loading && filteredCases.length > 0 && (
                        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
                            {filteredCases.map((c) => (
                                <div
                                    key={c.id}
                                    onClick={() => handleViewDetails(c)}
                                    className="cursor-pointer rounded-xl border bg-white p-4 shadow-sm transition hover:border-blue-300 hover:shadow-md"
                                >
                                    {c.caseObj?.caseNumber && (
                                        <div className="mb-2 inline-block rounded-md bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">
                                            Case #{c.caseObj.caseNumber}
                                        </div>
                                    )}
                                    <h3 className="truncate font-semibold text-gray-900">
                                        {c.title}
                                    </h3>

                                    <p className="mt-1 text-xs text-gray-500">
                                        Category: {c.category?.trim() || "-"}
                                    </p>
                                    
                                    <div className="mt-3 flex items-center justify-between">
                                        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                             c.status === "IN_PROGRESS"
                                            ? "bg-indigo-100 text-indigo-800"
                                            : "bg-emerald-100 text-emerald-800"
                                        }`}>    {formatStatus(c.status)}
                                        </span>
                                        <span className="text-xs text-gray-400">
                                            {new Date(c.createdAt).toLocaleDateString()}
                                        </span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </>
            )}
            {showCloseModal && (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
    <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-lg">
      <h3 className="text-lg font-bold text-gray-900">
        Close Case
      </h3>

      <p className="mt-2 text-sm text-gray-600">
        Please confirm case closure.
      </p>

      <div className="mt-4 space-y-2">
        {[
          "Case resolved",
          "Settlement reached",
          "Legal assistance completed"
        ].map((reason) => (
          <label key={reason} className="flex items-center gap-2 text-sm">
            <input
              type="radio"
              checked={closeReason === reason}
              onChange={() => setCloseReason(reason)}
            />
            {reason}
          </label>
        ))}
      </div>

      <div className="mt-6 flex justify-end gap-3">
        <button
          onClick={() => setShowCloseModal(false)}
          className="rounded-lg border px-4 py-2 text-sm"
        >
          Cancel
        </button>

        <button
          onClick={handleCloseCase}
          className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700"
        >
          Confirm Close
        </button>
      </div>
    </div>
  </div>
        )}
        </div>
    );
}

function Info({ label, value }) {
  return (
    <div>
      <p className="font-semibold text-gray-700">{label}</p>
      <p className="text-gray-600">{value?.trim() || "-"}</p>
    </div>
  );
}
