import React, { useState, useEffect } from 'react';
import { fetchAppointmentsForAdmin ,fetchAppointmentsStats} from '../../../services/admin';
import LegalLoader from '../../LegalLoader';
import {
  FiCalendar,
  FiClock,
  FiCheckCircle,
  FiXCircle,
  FiAlertCircle,
  FiChevronLeft,
  FiChevronRight,
  FiFilter,
  FiUsers,
  FiFileText
} from 'react-icons/fi';

const AppointmentsTracker = () => {
  const [appointments, setAppointments] = useState([]);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [stats, setStats] = useState({
    total: 0,
    confirmed: 0,
    completed: 0,
    upcoming : 0,
    pending : 0,
    cancelled: 0
  });
  useEffect(() => {
    fetchStats();
  }, []);

  useEffect(() => {
    fetchAppointments();
  }, [page, filterStatus]);

  const fetchStats = async () => {
      try {
        const res = await fetchAppointmentsStats();
  
        setStats({
          total: res.data.total,
          confirmed: res.data.confirmed,
          completed: res.data.completed,
          upcoming : res.data.upcoming,
          pending : res.data.pending,
          cancelled : res.data.cancelled
        });
  
      } catch (err) {
        console.error('Failed to load case stats', err);
      }
    };

  const fetchAppointments = async () => {
    try {
      setLoading(true);
      const res = await fetchAppointmentsForAdmin({
        page,
        size,
        status: filterStatus === 'ALL' ? null : filterStatus
      });

      setAppointments(res.data.content);
      setTotalPages(res.data.totalPages);

    } catch (err) {
      console.error('Failed to fetch appointments', err);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  };

  const formatDateTime = (isoDate) => {
    if (!isoDate) return '';
    return new Date(isoDate).toLocaleString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'COMPLETED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'RESCHEDULED':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'CONFIRMED':
        return <FiCheckCircle className="text-blue-600" />;
      case 'COMPLETED':
        return <FiCheckCircle className="text-green-600" />;
      case 'CANCELLED':
        return <FiXCircle className="text-red-600" />;
      case 'RESCHEDULED':
        return <FiAlertCircle className="text-yellow-600" />;
      default:
        return <FiClock className="text-gray-600" />;
    }
  };

  const isUpcoming = (appointmentDate) => {
    return new Date(appointmentDate) >= new Date();
  };

  const formatDuration = (minutes) => {
    if (!minutes) return '';
    if (minutes === 60) return '1 hour';
    return `${minutes} mins`;
  };

  return (
    <div className="space-y-6">
      {/* Header Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4">
        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition">
          <p className="text-sm text-gray-500 whitespace-nowrap">
            Total Appointments
          </p>
          <div className="mt-2 flex items-center justify-between">
            <p className="text-2xl font-bold text-gray-900">
              {stats.total}
            </p>
            <div className="p-1 bg-blue-50 rounded-full text-blue-600">
              <FiCalendar size={20} />
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition">
          <p className="text-sm text-gray-500 whitespace-nowrap">
            Confirmed
          </p>
          <div className="mt-2 flex items-center justify-between">
            <p className="text-2xl font-bold text-gray-900">
              {stats.confirmed}
            </p>
            <div className="p-1 bg-orange-50 rounded-full text-orange-600">
              <FiCheckCircle size={20} />
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition">
          <p className="text-sm text-gray-500 whitespace-nowrap">
            Upcoming
          </p>
          <div className="mt-2 flex items-center justify-between">
            <p className="text-2xl font-bold text-gray-900">
              {stats.upcoming}
            </p>
            <div className="p-1 bg-indigo-50 rounded-full text-indigo-600">
              <FiClock size={20} />
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition">
          <p className="text-sm text-gray-500 whitespace-nowrap">
            Completed
          </p>
          <div className="mt-2 flex items-center justify-between">
            <p className="text-2xl font-bold text-gray-900">
              {stats.completed}
            </p>
            <div className="p-1 bg-green-50 rounded-full text-green-600">
              <FiCheckCircle size={20} />
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition">
          <p className="text-sm text-gray-500 whitespace-nowrap">
            Pending
          </p>
          <div className="mt-2 flex items-center justify-between">
            <p className="text-2xl font-bold text-gray-900">
              {stats.pending}
            </p>
            <div className="p-1 bg-yellow-50 rounded-full text-yellow-600">
              <FiAlertCircle size={20} />
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition">
          <p className="text-sm text-gray-500 whitespace-nowrap">
            Cancelled
          </p>
          <div className="mt-2 flex items-center justify-between">
            <p className="text-2xl font-bold text-gray-900">
              {stats.cancelled}
            </p>
            <div className="p-1 bg-red-50 rounded-full text-red-600">
              <FiXCircle size={20} />
            </div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 flex flex-col md:flex-row gap-4 justify-between items-center">
        <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
          <FiCalendar className="text-blue-600" />
          Appointment Tracker
        </h3>
        <div className="flex items-center gap-2 w-full md:w-auto">
          <FiFilter className="text-gray-500" />
          <select
            className="border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={filterStatus}
            onChange={(e) => {
              setFilterStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="ALL">All Statuses</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="RESCHEDULED">Rescheduled</option>
          </select>
        </div>
      </div>

      {/* Appointments Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
            <div className="relative">
                      <div className="overflow-x-auto">
{/*           <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden relative"> */}
            {loading && (
              <div className="absolute inset-0 bg-white/90 flex items-center justify-center z-10">
                <LegalLoader />
              </div>
            )}

            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    Appointment Details
                  </th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    Participants
                  </th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    Date & Time
                  </th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    Created
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {appointments.map((appointment) => (
                  <tr key={appointment.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex flex-col">
                        <span className="font-medium text-gray-900 flex items-center gap-2">
                          <FiFileText className="text-blue-600" />
                          {appointment.caseTitle || 'No Case Linked'}
                        </span>
                        <span className="text-xs text-gray-500 mt-1">
                          ID: {appointment.id} • Match: {appointment.matchId}
                        </span>
                        <span className="text-xs text-gray-600 mt-1 flex items-center gap-1">
                          <FiClock size={12} />
                          {formatDuration(appointment.durationMinutes)} • {appointment.timeZone}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex flex-col gap-2">
                        <div className="flex items-center gap-2 text-sm">
                          <FiUsers className="text-gray-400" size={14} />
                          <div>
                            <p className="text-gray-900 font-medium">{appointment.requesterName}</p>
                            <p className="text-xs text-gray-500">Requester ID : {appointment.requesterId}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                          <FiUsers className="text-gray-400" size={14} />
                          <div>
                            <p className="text-gray-900 font-medium"> {appointment.receiverName}</p>
                            <p className="text-xs text-gray-500">Receiver ID : {appointment.receiverId}</p>
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex flex-col">
                        <span className="text-sm font-medium text-gray-900 flex items-center gap-2">
                          <FiCalendar className="text-blue-600" size={14} />
                          {formatDate(appointment.appointmentDate)}
                        </span>
                        <span className="text-xs text-gray-600 mt-1 flex items-center gap-1">
                          <FiClock size={12} />
                          {appointment.timeSlot}
                        </span>
                        {isUpcoming(appointment.appointmentDate) && (
                          <span className="text-xs text-green-600 font-medium mt-1">
                            Upcoming
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        {getStatusIcon(appointment.status)}
                        <span className={`px-3 py-1 text-xs font-medium rounded-full border ${getStatusColor(appointment.status)}`}>
                          {appointment.status}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-xs text-gray-500">
                        {formatDateTime(appointment.createdAt)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>



            {appointments.length === 0 && !loading && (
              <div className="p-8 text-center text-gray-500">
                <FiCalendar className="mx-auto text-4xl mb-2 text-gray-300" />
                <p>No appointments found matching your criteria.</p>
              </div>
            )}
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between px-6 py-4 border-t bg-gray-50">
            <span className="text-sm text-gray-600">
              Page {page + 1} of {totalPages || 1}
            </span>

            <div className="flex gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
                className="px-4 py-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-white transition-colors flex items-center gap-1"
              >
                <FiChevronLeft /> Prev
              </button>

              <button
                disabled={page + 1 >= totalPages}
                onClick={() => setPage(p => p + 1)}
                className="px-4 py-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-white transition-colors flex items-center gap-1"
              >
                Next <FiChevronRight />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AppointmentsTracker;

