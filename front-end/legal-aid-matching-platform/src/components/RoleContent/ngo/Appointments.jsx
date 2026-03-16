import React, { useState, useEffect } from "react";
import {
  Calendar,
  Clock,
  MapPin,
  User,
  CheckCircle,
  X,
  Edit,
  AlertCircle,
  Loader2,
} from "lucide-react";
import {
  getMyAppointments,
  getMyProfile,
  acceptAppointment,
  cancelAppointment,
  rescheduleAppointment
} from "../../../services/api";
import { useAlert } from "../../../context/AlertContext";

const BLUE = "#6610f2";

export default function NGOAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [processingId, setProcessingId] = useState(null);
  const [showRescheduleModal, setShowRescheduleModal] = useState(false);
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [rescheduleData, setRescheduleData] = useState({
    appointmentDate: "",
    timeSlot: "",
    timeZone: "",
    durationMinutes: "",
    remind15Min: false,
    remind1Hour: false,
  });
  const { showAlert } = useAlert();

  useEffect(() => {
    fetchAppointments();
  }, []);

  const fetchAppointments = async () => {
    try {
      setLoading(true);
      setError(null);

      const userRes = await getMyProfile();
      setCurrentUser(userRes.data);

      if (userRes.data && userRes.data.id) {
        const apptsRes = await getMyAppointments(userRes.data.id.toString());
        const appts = apptsRes.data || [];

        // Filter to show only upcoming appointments (date >= today)
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const upcomingAppts = appts.filter(appt => {
          const apptDate = new Date(appt.appointmentDate);
          apptDate.setHours(0, 0, 0, 0);
          return apptDate >= today && appt.status !== "CANCELLED" && appt.status !== "COMPLETED";
        });

        // Sort: PENDING first, then by date
        upcomingAppts.sort((a, b) => {
          if (a.status === "PENDING" && b.status !== "PENDING") return -1;
          if (a.status !== "PENDING" && b.status === "PENDING") return 1;

          const dateA = new Date(a.appointmentDate);
          const dateB = new Date(b.appointmentDate);
          return dateA - dateB;
        });

        setAppointments(upcomingAppts);
      }
    } catch (err) {
      console.error("Error fetching appointments:", err);
      setError(err.response?.data?.message || "Failed to fetch appointments");
      showAlert("Failed to load appointments", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleAccept = async (appointment) => {
    try {
      setProcessingId(appointment.id);
      await acceptAppointment(appointment.matchId);
      showAlert("Appointment accepted successfully", "success");
      await fetchAppointments();
    } catch (err) {
      console.error("Error accepting appointment:", err);
      showAlert(err.response?.data?.message || "Failed to accept appointment", "error");
    } finally {
      setProcessingId(null);
    }
  };

  const handleCancel = async (appointment) => {
    if (!window.confirm("Are you sure you want to cancel this appointment?")) {
      return;
    }

    try {
      setProcessingId(appointment.id);
      await cancelAppointment(appointment.matchId);
      showAlert("Appointment cancelled successfully", "success");
      await fetchAppointments();
    } catch (err) {
      console.error("Error cancelling appointment:", err);
      showAlert(err.response?.data?.message || "Failed to cancel appointment", "error");
    } finally {
      setProcessingId(null);
    }
  };

  const handleRescheduleClick = (appointment) => {
    setSelectedAppointment(appointment);
    setRescheduleData({
      appointmentDate: appointment.appointmentDate || "",
      timeSlot: appointment.timeSlot || "",
      timeZone: appointment.timeZone || "",
      durationMinutes: appointment.durationMinutes?.toString() || "",
      remind15Min: appointment.remind15Min || false,
      remind1Hour: appointment.remind1Hour || false,
    });
    setShowRescheduleModal(true);
  };

  const handleRescheduleSubmit = async () => {
    if (!rescheduleData.appointmentDate || !rescheduleData.timeSlot || !rescheduleData.durationMinutes) {
      showAlert("Please fill in all required fields", "error");
      return;
    }

    try {
      setProcessingId(selectedAppointment.id);

      await rescheduleAppointment(selectedAppointment.matchId, {
        appointmentDate: rescheduleData.appointmentDate,
        timeSlot: rescheduleData.timeSlot,
        timeZone: rescheduleData.timeZone,
        durationMinutes: parseInt(rescheduleData.durationMinutes),
        remind15Min: rescheduleData.remind15Min,
        remind1Hour: rescheduleData.remind1Hour,
      });

      showAlert("Appointment rescheduled successfully. Waiting for client confirmation.", "success");
      setShowRescheduleModal(false);
      setSelectedAppointment(null);
      await fetchAppointments();
    } catch (err) {
      console.error("Error rescheduling appointment:", err);
      showAlert(err.response?.data?.message || "Failed to reschedule appointment", "error");
    } finally {
      setProcessingId(null);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case "CONFIRMED":
        return "bg-green-100 text-green-800";
      case "PENDING":
        return "bg-yellow-100 text-yellow-800";
      case "CANCELLED":
        return "bg-red-100 text-red-800";
      case "COMPLETED":
        return "bg-blue-100 text-blue-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    const date = new Date(dateString);
    return date.toLocaleDateString("en-US", {
      weekday: "long",
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  };

  const formatTime = (timeString) => {
    if (!timeString) return "N/A";
    return timeString;
  };

  const getAvailableSlots = () => {
    return [
      "9:00 AM",
      "10:30 AM",
      "2:00 PM",
      "3:30 PM",
      "5:00 PM",
    ];
  };

  const timeZones = [
    "Select a time zone",
    "America/New_York (EST)",
    "America/Chicago (CST)",
    "America/Denver (MST)",
    "America/Los_Angeles (PST)",
    "Asia/Kolkata (IST)",
    "Europe/London (GMT)",
  ];

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 p-4 md:p-6 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-8 h-8 animate-spin mx-auto mb-4" style={{ color: BLUE }} />
          <p className="text-gray-600">Loading appointments...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 p-4 md:p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Appointment Requests</h1>
          <p className="text-gray-600">Manage incoming appointment requests from clients</p>
        </div>

        {/* Error State */}
        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3">
            <AlertCircle className="text-red-500" size={20} />
            <p className="text-red-800">{error}</p>
          </div>
        )}

        {/* Empty State */}
        {!loading && appointments.length === 0 && (
          <div className="bg-white rounded-xl shadow-md p-12 text-center">
            <Calendar size={64} className="mx-auto mb-4 text-gray-300" />
            <h3 className="text-xl font-semibold text-gray-900 mb-2">
              No Upcoming Appointments
            </h3>
            <p className="text-gray-600">
              You don't have any appointment requests at the moment.
            </p>
          </div>
        )}

        {/* Appointments Grid */}
        {!loading && appointments.length > 0 && (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {appointments.map((appointment) => (
              <div
                key={appointment.id}
                className="bg-white rounded-xl shadow-md hover:shadow-lg transition p-6"
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    {/* CASE TITLE */}
                    <h2 className="text-lg font-semibold text-blue-950 mb-3 line-clamp-2">
                      {appointment.caseTitle || "Case Discussion"}
                    </h2>

                    {/* CLIENT NAME */}
                    <div className="flex items-center gap-2 text-sm text-gray-700 mb-2">
                      <User size={16} className="text-gray-400" />
                      <span className="font-medium">Client: {appointment.requesterName}</span>
                    </div>

                    {/* META ROW */}
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-gray-500">
                        Match ID: #{appointment.matchId}
                      </span>

                      <span
                        className={`px-3 py-1 rounded-full text-xs font-semibold tracking-wide ${getStatusColor(
                        appointment.status
                      )}`}
                      >
                        {appointment.status}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="space-y-3">
                  <div className="flex items-start gap-3">
                    <Calendar size={18} className="text-gray-500 mt-1" />
                    <div>
                      <p className="text-sm text-gray-600">Date</p>
                      <p className="font-semibold text-gray-900">
                        {formatDate(appointment.appointmentDate)}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-start gap-3">
                    <Clock size={18} className="text-gray-500 mt-1" />
                    <div>
                      <p className="text-sm text-gray-600">Time</p>
                      <p className="font-semibold text-gray-900">
                        {formatTime(appointment.timeSlot)}
                      </p>
                    </div>
                  </div>

                  {appointment.timeZone && (
                    <div className="flex items-start gap-3">
                      <MapPin size={18} className="text-gray-500 mt-1" />
                      <div>
                        <p className="text-sm text-gray-600">Time Zone</p>
                        <p className="font-semibold text-gray-900">{appointment.timeZone}</p>
                      </div>
                    </div>
                  )}

                  {appointment.durationMinutes && (
                    <div>
                      <p className="text-sm text-gray-600">Duration</p>
                      <p className="font-semibold text-gray-900">
                        {appointment.durationMinutes} minutes
                      </p>
                    </div>
                  )}
                </div>

                {/* Action Buttons */}
                <div className="mt-4 pt-4 border-t space-y-2">
                  {appointment.status === "PENDING" && (
                    <>
                      {(() => {
                        // Determine who should see action buttons
                        const shouldShowButtons = currentUser && appointment.lastModifiedBy &&
                          String(currentUser.id) !== String(appointment.lastModifiedBy);

                        // If lastModifiedBy is not set, assume citizen created it, so show buttons to provider
                        // Providers (NGOs) are always the receiver, so if current user IS the receiver, show buttons
                        const fallbackShowButtons = currentUser && appointment.receiverId &&
                          String(currentUser.id) === String(appointment.receiverId);

                        const showButtons = appointment.lastModifiedBy ? shouldShowButtons : fallbackShowButtons;

                        return showButtons ? (
                          <>
                            <button
                              onClick={() => handleAccept(appointment)}
                              disabled={processingId === appointment.id}
                              className="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg font-medium text-white transition hover:opacity-90 bg-green-600 disabled:opacity-50"
                            >
                              {processingId === appointment.id ? (
                                <Loader2 size={16} className="animate-spin" />
                              ) : (
                                <CheckCircle size={16} />
                              )}
                              Accept
                            </button>

                            <button
                              onClick={() => handleRescheduleClick(appointment)}
                              disabled={processingId === appointment.id}
                              className="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg font-medium text-white transition hover:opacity-90 bg-blue-950 disabled:opacity-50"
                            >
                              <Edit size={16} />
                              Propose New Time
                            </button>

                            <button
                              onClick={() => handleCancel(appointment)}
                              disabled={processingId === appointment.id}
                              className="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg font-medium text-white transition hover:opacity-90 bg-red-600 disabled:opacity-50"
                            >
                              <X size={16} />
                              Decline
                            </button>
                          </>
                        ) : (
                          <div className="text-center py-2 text-sm text-yellow-700 bg-yellow-50 rounded-lg">
                            Waiting for client to respond
                          </div>
                        );
                      })()}
                    </>
                  )}

                  {appointment.status === "CONFIRMED" && (
                    <button
                      onClick={() => handleCancel(appointment)}
                      disabled={processingId === appointment.id}
                      className="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg font-medium text-white transition hover:opacity-90 bg-red-600 disabled:opacity-50"
                    >
                      <X size={16} />
                      Cancel
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Reschedule Modal */}
        {showRescheduleModal && selectedAppointment && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
              {/* Modal Header */}
              <div className="sticky top-0 bg-white border-b px-6 py-5 flex items-center justify-between rounded-t-2xl">
                <div>
                  <h2 className="text-2xl font-bold text-gray-900">Propose New Time</h2>
                  <p className="text-sm text-gray-600 mt-1">
                    Suggest an alternative date and time
                  </p>
                </div>
                <button
                  onClick={() => {
                    setShowRescheduleModal(false);
                    setSelectedAppointment(null);
                  }}
                  className="text-gray-500 hover:text-gray-800 text-2xl leading-none"
                >
                  <X size={24} />
                </button>
              </div>

              {/* Modal Content */}
              <div className="p-6 space-y-6">
                {/* Date Selection */}
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    Appointment Date <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="date"
                    value={rescheduleData.appointmentDate}
                    onChange={(e) =>
                      setRescheduleData({ ...rescheduleData, appointmentDate: e.target.value })
                    }
                    min={new Date().toISOString().split("T")[0]}
                    className="w-full border rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>

                {/* Time Slot */}
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    Time Slot <span className="text-red-500">*</span>
                  </label>
                  <select
                    value={rescheduleData.timeSlot}
                    onChange={(e) =>
                      setRescheduleData({ ...rescheduleData, timeSlot: e.target.value })
                    }
                    className="w-full border rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="">Select a time slot</option>
                    {getAvailableSlots().map((slot) => (
                      <option key={slot} value={slot}>
                        {slot}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Time Zone */}
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    Time Zone
                  </label>
                  <select
                    value={rescheduleData.timeZone}
                    onChange={(e) =>
                      setRescheduleData({ ...rescheduleData, timeZone: e.target.value })
                    }
                    className="w-full border rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  >
                    {timeZones.map((tz) => (
                      <option key={tz} value={tz}>
                        {tz}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Duration */}
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    Duration (minutes) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    value={rescheduleData.durationMinutes}
                    onChange={(e) =>
                      setRescheduleData({ ...rescheduleData, durationMinutes: e.target.value })
                    }
                    min="15"
                    step="15"
                    placeholder="e.g., 30"
                    className="w-full border rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>

                {/* Reminders */}
                <div className="space-y-3">
                  <label className="block text-sm font-semibold text-gray-700">Reminders</label>
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="remind15Min"
                      checked={rescheduleData.remind15Min}
                      onChange={(e) =>
                        setRescheduleData({ ...rescheduleData, remind15Min: e.target.checked })
                      }
                      className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                    />
                    <label htmlFor="remind15Min" className="text-sm text-gray-700">
                      Remind 15 minutes before
                    </label>
                  </div>

                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="remind1Hour"
                      checked={rescheduleData.remind1Hour}
                      onChange={(e) =>
                        setRescheduleData({ ...rescheduleData, remind1Hour: e.target.checked })
                      }
                      className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                    />
                    <label htmlFor="remind1Hour" className="text-sm text-gray-700">
                      Remind 1 hour before
                    </label>
                  </div>
                </div>
              </div>

              {/* Modal Footer */}
              <div className="sticky bottom-0 bg-gray-50 px-6 py-4 flex gap-3 justify-end rounded-b-2xl border-t">
                <button
                  onClick={() => {
                    setShowRescheduleModal(false);
                    setSelectedAppointment(null);
                  }}
                  className="px-6 py-2.5 border border-gray-300 rounded-lg font-medium text-gray-700 hover:bg-gray-100 transition"
                >
                  Cancel
                </button>
                <button
                  onClick={handleRescheduleSubmit}
                  disabled={processingId === selectedAppointment.id}
                  className="px-6 py-2.5 bg-blue-950 text-white rounded-lg font-medium hover:opacity-90 transition disabled:opacity-50 flex items-center gap-2"
                >
                  {processingId === selectedAppointment.id ? (
                    <>
                      <Loader2 size={16} className="animate-spin" />
                      Proposing...
                    </>
                  ) : (
                    "Propose New Time"
                  )}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

