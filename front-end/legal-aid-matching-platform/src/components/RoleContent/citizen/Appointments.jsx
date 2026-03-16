import React, { useState, useEffect } from "react";
import {
  Calendar,
  Clock,
  MapPin,
  User,
  Edit,
  X,
  CheckCircle,
  AlertCircle,
  Loader2,
  ChevronDown,
} from "lucide-react";
import { getMyAppointments, getMyProfile, rescheduleAppointment, acceptAppointment, cancelAppointment } from "../../../services/api";
import { useAlert } from "../../../context/AlertContext";

const BLUE = "#6610f2";

export default function Appointments() {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [reschedulingId, setReschedulingId] = useState(null);
  const [showRescheduleModal, setShowRescheduleModal] = useState(false);
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [rescheduleData, setRescheduleData] = useState({
    appointmentDate: "",
    timeSlot: "",
    timeZone: "",
    durationMinutes: "",
    caseTitle:"",
    receiverId: "",
    requesterId: "",
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

        // Sort by date (earliest first)
        upcomingAppts.sort((a, b) => {
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

  const handleRescheduleClick = (appointment) => {
    setSelectedAppointment(appointment);
    setRescheduleData({
      appointmentDate: appointment.appointmentDate || "",
      timeSlot: appointment.timeSlot || "",
      timeZone: appointment.timeZone || "",
      durationMinutes: appointment.durationMinutes?.toString() || "",
      caseTitle: appointment.caseTitle || "",
      receiverId: appointment.receiverId || "",
      requesterId: appointment.requesterId || "",
      remind15Min: appointment.remind15Min || false,
      remind1Hour: appointment.remind1Hour || false,
    });
    setShowRescheduleModal(true);
  };

  const handleAccept = async (appointment) => {
    try {
      setReschedulingId(appointment.id);
      await acceptAppointment(appointment.matchId);
      showAlert("Appointment accepted successfully", "success");
      await fetchAppointments();
    } catch (err) {
      console.error("Error accepting appointment:", err);
      showAlert(err.response?.data?.message || "Failed to accept appointment", "error");
    } finally {
      setReschedulingId(null);
    }
  };

  const handleCancel = async (appointment) => {
    if (!window.confirm("Are you sure you want to cancel this appointment?")) {
      return;
    }

    try {
      setReschedulingId(appointment.id);
      await cancelAppointment(appointment.matchId);
      showAlert("Appointment cancelled successfully", "success");
      await fetchAppointments();
    } catch (err) {
      console.error("Error cancelling appointment:", err);
      showAlert(err.response?.data?.message || "Failed to cancel appointment", "error");
    } finally {
      setReschedulingId(null);
    }
  };

  const handleRescheduleSubmit = async () => {
    if (!rescheduleData.appointmentDate || !rescheduleData.timeSlot || !rescheduleData.durationMinutes) {
      showAlert("Please fill in all required fields", "error");
      return;
    }

    try {
      setReschedulingId(selectedAppointment.matchId);
      
      const appointmentData = {
        appointmentDate: rescheduleData.appointmentDate,
        timeSlot: rescheduleData.timeSlot,
        timeZone: rescheduleData.timeZone || "UTC",
        durationMinutes: parseInt(rescheduleData.durationMinutes),
        remind15Min: rescheduleData.remind15Min,
        remind1Hour: rescheduleData.remind1Hour,
      };

      await rescheduleAppointment(selectedAppointment.matchId, appointmentData);
      
      showAlert("Appointment rescheduled successfully. Waiting for provider confirmation.", "success");
      setShowRescheduleModal(false);
      setSelectedAppointment(null);
      fetchAppointments(); // Refresh the list
    } catch (err) {
      console.error("Error rescheduling appointment:", err);
      showAlert(err.response?.data?.message || "Failed to reschedule appointment", "error");
    } finally {
      setReschedulingId(null);
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

  // Get available time slots
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
          <h1 className="text-3xl font-bold text-gray-900 mb-2">My Appointments</h1>
          <p className="text-gray-600">View and manage your upcoming appointments</p>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg mb-6 flex items-center gap-2">
            <AlertCircle size={20} />
            <span>{error}</span>
          </div>
        )}

        {/* Appointments List */}
        {appointments.length === 0 ? (
          <div className="bg-white rounded-xl shadow p-12 text-center">
            <Calendar size={48} className="mx-auto mb-4 text-gray-400" />
            <h3 className="text-xl font-semibold text-gray-900 mb-2">No Upcoming Appointments</h3>
            <p className="text-gray-600">You don't have any upcoming appointments scheduled.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {appointments.map((appointment) => (
              <div
                key={appointment.id}
                className="bg-white rounded-xl shadow-md hover:shadow-lg transition p-6"
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    {/* CASE NUMBER */}
                    {appointment.caseNumber && (
                      <div className="mb-2 inline-block rounded-md bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">
                        Case #{appointment.caseNumber}
                      </div>
                    )}

                    {/* CASE TITLE */}
                    <h2 className="text-lg font-semibold text-blue-950 mb-3 line-clamp-2">
                      {appointment.caseTitle || "Case Discussion"}
                    </h2>

                    {/* PARTICIPANTS */}
                    <div className="flex items-center gap-2 text-sm text-gray-700 mb-2">
                      <User size={16} className="text-gray-400" />
                      <span className="font-medium">{appointment.requesterName || "You"}</span>
                      <span className="text-gray-400">↔</span>
                      <span className="font-medium">{appointment.receiverName || "Provider"}</span>
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

                <div className="mt-4 pt-4 border-t space-y-2">
                  {appointment.status === "PENDING" && (
                    <>
                      {(() => {
                        // Determine who should see action buttons
                        const shouldShowButtons = currentUser && appointment.lastModifiedBy &&
                          String(currentUser.id) !== String(appointment.lastModifiedBy);

                        // If lastModifiedBy is not set, assume citizen created it, so show buttons to provider
                        // Citizens are always the requester, so if current user is NOT the requester, show buttons
                        const fallbackShowButtons = currentUser && appointment.requesterId &&
                          String(currentUser.id) !== String(appointment.requesterId);

                        const showButtons = appointment.lastModifiedBy ? shouldShowButtons : fallbackShowButtons;

                        return showButtons ? (
                          // Other person proposed - show action buttons
                          <>
                            <button
                              onClick={() => handleAccept(appointment)}
                              disabled={reschedulingId === appointment.id}
                              className="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg font-medium text-white transition hover:opacity-90 bg-green-600 disabled:opacity-50"
                            >
                              {reschedulingId === appointment.id ? (
                                <Loader2 size={16} className="animate-spin" />
                              ) : (
                                <CheckCircle size={16} />
                              )}
                              Accept
                            </button>

                            <button
                              onClick={() => handleRescheduleClick(appointment)}
                              disabled={reschedulingId === appointment.id}
                              className="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg font-medium text-white transition hover:opacity-90 bg-blue-950 disabled:opacity-50"
                            >
                              <Edit size={16} />
                              Propose New Time
                            </button>

                            <button
                              onClick={() => handleCancel(appointment)}
                              disabled={reschedulingId === appointment.id}
                              className="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg font-medium text-white transition hover:opacity-90 bg-red-600 disabled:opacity-50"
                            >
                              <X size={16} />
                              Decline
                            </button>
                          </>
                        ) : (
                          // Current user last proposed - show waiting message
                          <div className="text-center py-2 text-sm text-yellow-700 bg-yellow-50 rounded-lg">
                            Waiting for provider to respond
                          </div>
                        );
                      })()}
                    </>
                  )}

                  {appointment.status === "CONFIRMED" && (
                    <>
                      <button
                        onClick={() => handleRescheduleClick(appointment)}
                        disabled={reschedulingId === appointment.id}
                        className="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg font-medium text-white transition hover:opacity-90 bg-blue-950 disabled:opacity-50"
                      >
                        <Edit size={16} />
                        Reschedule
                      </button>

                      <button
                        onClick={() => handleCancel(appointment)}
                        disabled={reschedulingId === appointment.id}
                        className="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg font-medium text-white transition hover:opacity-90 bg-red-600 disabled:opacity-50"
                      >
                        <X size={16} />
                        Cancel
                      </button>
                    </>
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
                  <h2 className="text-2xl font-bold text-gray-900">Reschedule Appointment</h2>
                  <p className="text-sm text-gray-600 mt-1">
                    Update the date and time for your appointment
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

                {/* Time Zone and Time Slot */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-2">
                      Time Zone
                    </label>
                    <div className="relative">
                      <select
                        value={rescheduleData.timeZone}
                        onChange={(e) =>
                          setRescheduleData({ ...rescheduleData, timeZone: e.target.value })
                        }
                        className="w-full border rounded-lg px-4 py-2.5 appearance-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
                      >
                        {timeZones.map((tz) => (
                          <option key={tz} value={tz === "Select a time zone" ? "" : tz}>
                            {tz}
                          </option>
                        ))}
                      </select>
                      <ChevronDown
                        size={18}
                        className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 pointer-events-none"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-2">
                      Time Slot <span className="text-red-500">*</span>
                    </label>
                    <div className="grid grid-cols-2 gap-2">
                      {getAvailableSlots().map((slot) => (
                        <button
                          key={slot}
                          onClick={() =>
                            setRescheduleData({ ...rescheduleData, timeSlot: slot })
                          }
                          className={`py-2.5 px-3 rounded-lg text-sm font-medium transition border ${
                            rescheduleData.timeSlot === slot
                              ? "bg-blue-950 text-white border-blue-950"
                              : "bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100"
                          }`}
                        >
                          {slot}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Duration */}
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    Duration (minutes) <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <select
                      value={rescheduleData.durationMinutes}
                      onChange={(e) =>
                        setRescheduleData({ ...rescheduleData, durationMinutes: e.target.value })
                      }
                      className="w-full border rounded-lg px-4 py-2.5 appearance-none focus:ring-2 focus:ring-blue-950 focus:border-blue-950 bg-white"
                    >
                      <option value="">Select duration</option>
                      <option value="15">15 minutes</option>
                      <option value="30">30 minutes</option>
                      <option value="60">1 hour</option>
                    </select>
                    <ChevronDown
                      size={18}
                      className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 pointer-events-none"
                    />
                  </div>
                </div>

                {/* Reminders */}
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">Reminders</label>
                  <div className="space-y-2">
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={rescheduleData.remind15Min}
                        onChange={(e) =>
                          setRescheduleData({ ...rescheduleData, remind15Min: e.target.checked })
                        }
                        className="w-4 h-4 accent-blue-600"
                      />
                      <span className="text-sm text-gray-700">15 minutes before the call</span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={rescheduleData.remind1Hour}
                        onChange={(e) =>
                          setRescheduleData({ ...rescheduleData, remind1Hour: e.target.checked })
                        }
                        className="w-4 h-4 accent-blue-600"
                      />
                      <span className="text-sm text-gray-700">1 hour before the call</span>
                    </label>
                  </div>
                </div>
              </div>

              {/* Modal Footer */}
              <div className="sticky bottom-0 bg-white border-t px-6 py-4 flex items-center justify-end gap-3 rounded-b-2xl">
                <button
                  onClick={() => {
                    setShowRescheduleModal(false);
                    setSelectedAppointment(null);
                  }}
                  className="px-6 py-2 border rounded-lg font-medium text-gray-700 hover:bg-gray-50 transition"
                >
                  Cancel
                </button>
                <button
                  onClick={handleRescheduleSubmit}
                  disabled={
                    !rescheduleData.appointmentDate ||
                    !rescheduleData.timeSlot ||
                    !rescheduleData.durationMinutes ||
                    reschedulingId === selectedAppointment.matchId
                  }
                  className={`px-6 py-2 rounded-lg font-medium text-white transition ${
                    !rescheduleData.appointmentDate ||
                    !rescheduleData.timeSlot ||
                    !rescheduleData.durationMinutes ||
                    reschedulingId === selectedAppointment.matchId
                      ? "bg-gray-400 cursor-not-allowed"
                      : "hover:opacity-90"
                  }`}
                  style={
                    rescheduleData.appointmentDate &&
                    rescheduleData.timeSlot &&
                    rescheduleData.durationMinutes &&
                    reschedulingId !== selectedAppointment.matchId
                      ? { backgroundColor: "#162456" }
                      : {}
                  }
                >
                  {reschedulingId === selectedAppointment.matchId ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin inline-block mr-2" />
                      Rescheduling...
                    </>
                  ) : (
                    "Reschedule Appointment"
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

