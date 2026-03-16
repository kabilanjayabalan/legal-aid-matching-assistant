import React, { useState, useEffect } from "react";
import { Calendar, Clock, CheckCircle, X, ChevronDown } from "lucide-react";
import { createAppointment, getMyAppointments, getMyProfile } from "../../../services/api";

const BLUE = "#6610f2";


export default function AppointmentScheduler({ lawyer = defaultLawyer, isModal = false, onClose }) {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedTime, setSelectedTime] = useState(null);
  const [duration, setDuration] = useState("");
  const [timeZone, setTimeZone] = useState("");
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [reminders, setReminders] = useState({
    "15mins": false,
    "1hour": false,
  });
  const [appointmentsList, setAppointmentsList] = useState([]);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [confirmedAppointment, setConfirmedAppointment] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);

  // Get current date info
  const today = new Date();
  const currentMonth = today.getMonth();
  const currentYear = today.getFullYear();

  // Fetch user and appointments on mount
  useEffect(() => {
    const fetchUserAndAppointments = async () => {
      try {
        const userRes = await getMyProfile();
        console.log("Fetched user profile:", userRes.data);
        setCurrentUser(userRes.data);

        if (userRes.data && userRes.data.id && lawyer && lawyer.id) {
          const apptsRes = await getMyAppointments(userRes.data.id);
          const filteredAppts = apptsRes.data.filter(appt => 
            appt.receiverId === lawyer.id.toString() || appt.requesterId === lawyer.id.toString()
          );
          console.log("Fetched and filtered appointments:", filteredAppts);
          const mappedAppts = filteredAppts.map(appt => ({
            id: appt.id,
            lawyerName: appt.receiverId === userRes.data.id.toString() ? "Client" : (lawyer.id.toString() === appt.receiverId ? lawyer.name : "Legal Professional"),
            date: appt.appointmentDate,
            time: appt.timeSlot,
            duration: appt.durationMinutes + " min",
            status: appt.status
          }));
          setAppointmentsList(mappedAppts);
        } else if (userRes.data && userRes.data.id && !lawyer) {
          // If not in modal mode (no specific lawyer), fetch all appointments
          const apptsRes = await getMyAppointments(userRes.data.id);
          const mappedAppts = apptsRes.data.map(appt => ({
            id: appt.id,
            lawyerName: appt.receiverId === userRes.data.id.toString() ? "Client" : "Legal Professional", // Simplified for general overview
            date: appt.appointmentDate,
            time: appt.timeSlot,
            duration: appt.durationMinutes + " min",
            status: appt.status
          }));
          setAppointmentsList(mappedAppts);
        }
      } catch (err) {
        console.error("Error fetching data:", err);
      }
    };
    fetchUserAndAppointments();
  }, [lawyer.id, lawyer.name]);

  // Generate calendar days for current month
  const getDaysInMonth = (month, year) => {
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const days = [];

    // Add empty cells for days before month starts
    for (let i = 0; i < firstDay; i++) {
      days.push(null);
    }

    // Add days of the month
    for (let day = 1; day <= daysInMonth; day++) {
      days.push(day);
    }

    return days;
  };

  const calendarDays = getDaysInMonth(currentMonth, currentYear);

  // Mock available time slots (in real app, this would be fetched based on selected date)
  const getAvailableSlots = () => {
    // Mock slots - in real app, this would come from API
    return [
      "9:00 AM",
      "10:30 AM",
      "2:00 PM",
      "3:30 PM",
      "5:00 PM",
    ];
  };

  const availableSlots = getAvailableSlots();

  // Time zones list
  const timeZones = [
    "Select a time zone",
    "America/New_York (EST)",
    "America/Chicago (CST)",
    "America/Denver (MST)",
    "America/Los_Angeles (PST)",
    "Asia/Kolkata (IST)",
    "Europe/London (GMT)",
  ];

  // Format date for display in input field
  const formatDateForInput = (date) => {
    if (!date) return "";
    const dateObj = new Date(currentYear, currentMonth, date);
    const options = { weekday: "short", year: "numeric", month: "short", day: "numeric" };
    return dateObj.toLocaleDateString("en-US", options);
  };

  // Format date for display
  const formatDate = (date) => {
    if (!date) return "";
    const dateObj = new Date(currentYear, currentMonth, date);
    const options = { weekday: "long", year: "numeric", month: "long", day: "numeric" };
    return dateObj.toLocaleDateString("en-US", options);
  };

  // Handle date selection
  const handleDateClick = (day) => {
    if (day !== null) {
      setSelectedDate(day);
      setSelectedTime(null); // Reset time when date changes
    }
  };

  // Handle reminder toggle
  const toggleReminder = (key) => {
    setReminders((prev) => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  // Handle confirm appointment
  const handleConfirm = async () => {
    if (!selectedDate || !selectedTime || !duration) {
        alert("Please select date, time, and duration.");
        return;
    }
    if (!currentUser) {
        alert("User profile not loaded. Please try again.");
        return;
    }
    if (!lawyer || !lawyer.id) {
        alert("Lawyer information missing.");
        return;
    }

    // Parse duration - handle both minutes and hours
    let durationMin = parseInt(duration.split(' ')[0]);
    if (duration.toLowerCase().includes('hr') || duration.toLowerCase().includes('hour')) {
      durationMin = durationMin * 60; // Convert hours to minutes
    }
    const formattedDate = `${currentYear}-${String(currentMonth + 1).padStart(2, "0")}-${String(selectedDate).padStart(2, "0")}`;

    const appointmentData = {
      matchId: lawyer.matchId || "manual-" + Date.now(),
      requesterId: currentUser.id.toString(),
      receiverId: lawyer.id.toString(),
      appointmentDate: formattedDate,
      timeZone: timeZone || "UTC",
      timeSlot: selectedTime,
      durationMinutes: isNaN(durationMin) ? 30 : durationMin,
      remind15Min: reminders["15mins"],
      remind1Hour: reminders["1hour"]
    };

    console.log("Sending appointment data:", appointmentData);

    try {
      const res = await createAppointment(appointmentData);
      console.log("Appointment created successfully:", res.data);

      // Format duration for display
      const durationDisplay = res.data.durationMinutes === 60 
        ? "1 hour" 
        : res.data.durationMinutes + " min";

      const newAppointment = {
        id: res.data.id,
        lawyerName: lawyer.name,
        date: res.data.appointmentDate,
        time: res.data.timeSlot,
        duration: durationDisplay,
        status: res.data.status,
      };

      setConfirmedAppointment(newAppointment);
      setAppointmentsList([...appointmentsList, newAppointment]);
      setShowConfirmation(true);
    } catch (err) {
      console.error("Failed to create appointment:", err);
      if (err.response) {
          console.error("Response data:", err.response.data);
          console.error("Response status:", err.response.status);
          alert(`Failed to schedule: ${err.response.data.message || "Unknown error"}`);
      } else {
          alert("Failed to schedule appointment. Network error or server unreachable.");
      }
    }
  };

  // Handle close modal
  const handleCloseModal = () => {
    setIsModalOpen(false);
    setShowConfirmation(false);
    setSelectedDate(null);
    setSelectedTime(null);
    setDuration("");
    setTimeZone("");
    setShowDatePicker(false);
    setReminders({ "15mins": false, "1hour": false });
    setConfirmedAppointment(null);
    // If in modal mode, notify parent to close
    if (isModal && onClose) {
      onClose();
    }
  };

  // Check if date is in the past
  const isDatePast = (day) => {
    if (day === null) return false;
    const dateObj = new Date(currentYear, currentMonth, day);
    return dateObj < new Date(today.getFullYear(), today.getMonth(), today.getDate());
  };

  // Get status badge color
  const getStatusColor = (status) => {
    switch (status) {
      case "Confirmed":
      case "CONFIRMED":
        return "bg-green-100 text-green-800";
      case "Pending":
      case "PENDING":
        return "bg-yellow-100 text-yellow-800";
      case "Cancelled":
      case "CANCELLED":
        return "bg-red-100 text-red-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  // Auto-open modal if in modal mode
  useEffect(() => {
    if (isModal) {
      setIsModalOpen(true);
    }
  }, [isModal]);

  // Close date picker when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (showDatePicker && !event.target.closest('.date-picker-container')) {
        setShowDatePicker(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showDatePicker]);

  return (
    <div className={isModal ? "p-0" : "min-h-screen bg-gray-100 p-4 md:p-6"}>
      <div className={isModal ? "w-full" : "max-w-6xl mx-auto"}>
        {/* Header - only show if not in modal mode */}
        {!isModal && (
          <div className="mb-6 flex items-center justify-between">
            <h1 className="text-2xl font-bold text-gray-900">Appointment Scheduling</h1>
            <button
              onClick={() => setIsModalOpen(true)}
              className="flex items-center gap-2 px-6 py-2 rounded-lg text-white font-medium transition hover:opacity-90"
              style={{ backgroundColor: BLUE }}
            >
              <Calendar size={18} />
              Schedule Call
            </button>
          </div>
        )}

        {/* Upcoming Appointments - only show if not in modal mode */}
        {!isModal && (
          <div className="bg-white rounded-xl shadow p-6">
            <h2 className="text-xl font-semibold mb-4">Upcoming Appointments</h2>
          {appointmentsList.length === 0 ? (
            <p className="text-gray-500 text-center py-8">No upcoming appointments</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {appointmentsList.map((appointment) => (
                <div
                  key={appointment.id}
                  className="border rounded-lg p-4 hover:shadow-md transition"
                >
                  <div className="flex items-start justify-between mb-2">
                    <h3 className="font-semibold text-gray-900">{appointment.lawyerName}</h3>
                    <span
                      className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(
                        appointment.status
                      )}`}
                    >
                      {appointment.status}
                    </span>
                  </div>
                  <div className="space-y-1 text-sm text-gray-600">
                    <div className="flex items-center gap-2">
                      <Calendar size={14} />
                      <span>{new Date(appointment.date).toLocaleDateString("en-US", {
                        weekday: "short",
                        month: "short",
                        day: "numeric",
                        year: "numeric",
                      })}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Clock size={14} />
                      <span>{appointment.time}</span>
                    </div>
                    <div className="text-xs text-gray-500">Duration: {appointment.duration}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
          </div>
        )}

        {/* Scheduling Modal */}
        {isModalOpen && (
          <div
            className={isModal ? "w-full" : "fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"}
            onClick={isModal ? undefined : handleCloseModal}
          >
            <div
              className={isModal ? "w-full bg-white" : "bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto"}
              onClick={(e) => e.stopPropagation()}
            >
              {!showConfirmation ? (
                <>
                  {/* Modal Header */}
                  <div className={`sticky top-0 bg-white border-b px-6 py-5 flex items-center justify-between ${isModal ? "" : "rounded-t-2xl"}`}>
                    <div>
                      <h2 className="text-2xl font-bold text-gray-900">Schedule an Appointment</h2>
                      <p className="text-sm text-gray-600 mt-1">
                        Propose a time to connect with {lawyer.name}
                      </p>
                    </div>
                    {!isModal && (
                      <button
                        onClick={handleCloseModal}
                        className="text-gray-500 hover:text-gray-800 text-2xl leading-none"
                      >
                        <X size={24} />
                      </button>
                    )}
                  </div>


                  {/* Modal Content */}
                  <div className="p-6 space-y-6">
                    {/* Lawyer/NGO Profile Card */}
                    {lawyer.image && (
                      <div className="bg-gray-50 rounded-lg p-4">
                        <div className="flex items-center gap-4">
                          <img
                            src={lawyer.image}
                            alt={lawyer.name}
                            className="w-16 h-16 rounded-full object-cover"
                          />
                          <div className="flex-1">
                            <h3 className="font-semibold text-gray-900">{lawyer.name}</h3>
                            <p className="text-sm text-gray-600">{lawyer.type}</p>
                            {lawyer.compatibility && (
                              <p className="text-sm text-gray-700 mt-1">
                                Match Score: <span className="font-semibold" style={{ color: BLUE }}>{lawyer.compatibility}</span>
                              </p>
                            )}
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Date Selection */}
                    <div className="date-picker-container">
                      <label className="block text-sm font-semibold text-gray-700 mb-2">Date</label>
                      <div className="relative">
                        <input
                          type="text"
                          readOnly
                          value={selectedDate ? formatDateForInput(selectedDate) : ""}
                          onClick={() => setShowDatePicker(!showDatePicker)}
                          placeholder="Select a date"
                          className="w-full border rounded-lg px-4 py-2.5 pl-12 pr-4 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 cursor-pointer"
                        />
                        <Calendar
                          size={20}
                          className="absolute right-5 top-1/2 transform -translate-y-1/2 text-gray-400 pointer-events-none"
                        />
                        {showDatePicker && (
                          <div className="absolute z-10 mt-2 bg-white border rounded-lg shadow-lg p-4 w-full max-w-sm">
                            <div className="text-center mb-4">
                              <p className="text-lg font-semibold">
                                {new Date(currentYear, currentMonth).toLocaleDateString("en-US", {
                                  month: "long",
                                  year: "numeric",
                                })}
                              </p>
                            </div>
                            <div className="grid grid-cols-7 gap-2">
                              {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => (
                                <div
                                  key={day}
                                  className="text-center text-xs font-semibold text-gray-600 py-2"
                                >
                                  {day}
                                </div>
                              ))}
                              {calendarDays.map((day, index) => (
                                <button
                                  key={index}
                                  onClick={() => {
                                    handleDateClick(day);
                                    setShowDatePicker(false);
                                  }}
                                  disabled={day === null || isDatePast(day)}
                                  className={`
                                    py-2 rounded-lg text-sm font-medium transition
                                    ${
                                      day === null
                                        ? "cursor-default"
                                        : isDatePast(day)
                                        ? "text-gray-300 cursor-not-allowed"
                                        : selectedDate === day
                                        ? "text-white"
                                        : "text-gray-700 hover:bg-gray-200"
                                    }
                                  `}
                                  style={
                                    selectedDate === day && day !== null && !isDatePast(day)
                                      ? { backgroundColor: BLUE }
                                      : {}
                                  }
                                >
                                  {day}
                                </button>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Time Zone and Proposed Time Slots - Side by Side */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      {/* Time Zone Selection */}
                      <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Time Zone</label>
                        <div className="relative">
                          <select
                            value={timeZone}
                            onChange={(e) => setTimeZone(e.target.value)}
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

                      {/* Proposed Time Slots */}
                      <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Proposed Time Slots</label>
                        <div className="grid grid-cols-2 gap-2">
                          {availableSlots.map((slot) => (
                            <button
                              key={slot}
                              onClick={() => setSelectedTime(slot)}
                              className={`
                                py-2.5 px-3 rounded-lg text-sm font-medium transition border
                                ${
                                  selectedTime === slot
                                    ? "text-white border-transparent"
                                    : "bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100"
                                }
                              `}
                              style={
                                selectedTime === slot ? { backgroundColor: BLUE } : {}
                              }
                            >
                              {slot}
                            </button>
                          ))}
                        </div>
                      </div>
                    </div>

                    {/* Call Duration */}
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-2">Call Duration</label>
                      <div className="relative">
                        <select
                          value={duration}
                          onChange={(e) => setDuration(e.target.value)}
                          className="w-full border rounded-lg px-4 py-2.5 appearance-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
                        >
                          <option value="">Select duration</option>
                          <option value="15 min">15 minutes</option>
                          <option value="30 min">30 minutes</option>
                          <option value="1 hr">1 hour</option>
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
                            checked={reminders["15mins"]}
                            onChange={() => toggleReminder("15mins")}
                            className="w-4 h-4 accent-blue-600"
                          />
                          <span className="text-sm text-gray-700">15 minutes before the call</span>
                        </label>
                        <label className="flex items-center gap-2 cursor-pointer">
                          <input
                            type="checkbox"
                            checked={reminders["1hour"]}
                            onChange={() => toggleReminder("1hour")}
                            className="w-4 h-4 accent-blue-600"
                          />
                          <span className="text-sm text-gray-700">1 hour before the call</span>
                        </label>
                      </div>
                    </div>
                  </div>

                  {/* Modal Footer */}
                  <div className={`sticky bottom-0 bg-white border-t px-6 py-4 flex items-center justify-end gap-3 ${isModal ? "" : "rounded-b-2xl"}`}>
                    <button
                      onClick={handleCloseModal}
                      className="px-6 py-2 border rounded-lg font-medium text-gray-700 hover:bg-gray-50 transition"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={handleConfirm}
                      disabled={!selectedDate || !selectedTime || !duration}
                      className={`
                        px-6 py-2 rounded-lg font-medium text-white transition
                        ${
                          !selectedDate || !selectedTime || !duration
                            ? "bg-gray-400 cursor-not-allowed"
                            : "hover:opacity-90"
                        }
                      `}
                      style={
                        selectedDate && selectedTime && duration
                          ? { backgroundColor: BLUE }
                          : {}
                      }
                    >
                      Confirm Call
                    </button>
                  </div>
                </>
              ) : (
                /* Confirmation Screen */
                <div className="p-6 text-center">
                  <div className="mb-4 flex justify-center">
                    <div
                      className="w-16 h-16 rounded-full flex items-center justify-center"
                      style={{ backgroundColor: "#d1fae5" }}
                    >
                      <CheckCircle size={32} color="#10b981" />
                    </div>
                  </div>
                  <h2 className="text-2xl font-bold text-gray-900 mb-2">
                    Appointment Confirmed!
                  </h2>
                  {confirmedAppointment && (
                    <div className="mt-6 space-y-3 text-left bg-gray-50 rounded-lg p-4">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-gray-700">With:</span>
                        <span className="text-gray-900">{confirmedAppointment.lawyerName}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Calendar size={16} className="text-gray-500" />
                        <span className="text-gray-900">
                          {new Date(confirmedAppointment.date).toLocaleDateString("en-US", {
                            weekday: "long",
                            year: "numeric",
                            month: "long",
                            day: "numeric",
                          })}
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Clock size={16} className="text-gray-500" />
                        <span className="text-gray-900">{confirmedAppointment.time}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-gray-700">Duration:</span>
                        <span className="text-gray-900">{confirmedAppointment.duration}</span>
                      </div>
                    </div>
                  )}
                  <button
                    onClick={handleCloseModal}
                    className="mt-6 px-8 py-2 rounded-lg text-white font-medium transition hover:opacity-90"
                    style={{ backgroundColor: BLUE }}
                  >
                    Close
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
