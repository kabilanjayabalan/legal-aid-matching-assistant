import React, { useState, useEffect } from 'react';
import { fetchCasesForMonitoring ,fetchCaseStats} from '../../../services/admin';
import { FiSearch, FiFilter, FiEye, FiActivity, FiCheckCircle, FiClock, FiAlertCircle, FiChevronLeft,  FiChevronRight } from 'react-icons/fi';

const CaseMonitoring = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [cases, setCases] = useState([]);
  const [selectedCase, setSelectedCase] = useState(null);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [stats, setStats] = useState({
    total: 0,
    open: 0,
    inProgress: 0,
    closed: 0
  });
  useEffect(() => {
    fetchStats();
  }, []);

  useEffect(() => {
    fetchCases();
  }, [page, searchTerm, filterStatus]);

  const fetchStats = async () => {
    try {
      const res = await fetchCaseStats();

      setStats({
        total: res.data.totalCases,
        open: res.data.openCases,
        inProgress: res.data.inProgressCases,
        closed: res.data.closedCases
      });

    } catch (err) {
      console.error('Failed to load case stats', err);
    }
  };

  const fetchCases = async () => {
    try {
      setLoading(true);

      const res = await fetchCasesForMonitoring({
        page,
        size,
        search: searchTerm || null,
        status: filterStatus === 'ALL' ? null : filterStatus
      });

      setCases(res.data.content);
      setTotalPages(res.data.totalPages);

    } catch (err) {
      console.error('Failed to fetch cases', err);
    } finally {
      setLoading(false);
    }
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

  const openDetailsModal = (caseItem) => {
    setSelectedCase(caseItem);
    setIsDetailsModalOpen(true);
  };

  const closeDetailsModal = () => {
    setIsDetailsModalOpen(false);
    setSelectedCase(null);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'CLOSED': return 'bg-green-100 text-green-800';
      case 'IN PROGRESS': return 'bg-blue-100 text-blue-800';
      case 'OPEN': return 'bg-yellow-100 text-yellow-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'HIGH': return 'text-red-600 bg-red-50';
      case 'MEDIUM': return 'text-orange-600 bg-orange-50';
      case 'LOW': return 'text-green-600 bg-green-50';
      default: return 'text-gray-600 bg-gray-50';
    }
  };

  const getMatchStatusColor = (matchStatus) => {
    switch (matchStatus) {
      case 'Matched': return 'bg-green-100 text-green-800';
      case 'Match Pending': return 'bg-blue-100 text-blue-800';
      case 'Searching': return 'bg-yellow-100 text-yellow-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getAssignedToStyle = (assignedTo, matchStatus) => {
    if (matchStatus === 'Matched') {
      return 'text-green-700 font-medium';
    } else if (matchStatus === 'Match Pending') {
      return 'text-blue-700 font-medium';
    }
    return 'text-gray-500 italic';
  };

  const filteredCases = cases;
  return (
    <div className="space-y-6">
      {/* Header Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Total Cases</p>
              <p className="text-2xl font-bold transition-all duration-300">{stats.total}</p>
            </div>
            <div className="p-3 bg-blue-50 rounded-full text-blue-600">
              <FiActivity />
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Closed Cases</p>
              <p className="text-2xl font-bold text-gray-900">{stats.closed}</p>
            </div>
            <div className="p-3 bg-purple-50 rounded-full text-purple-600">
              <FiCheckCircle />
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Opened Cases</p>
              <p className="text-2xl font-bold text-gray-900">{stats.open}</p>
            </div>
            <div className="p-3 bg-yellow-50 rounded-full text-yellow-600">
              <FiClock />
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Active Cases</p>
              <p className="text-2xl font-bold text-gray-900">{stats.inProgress}</p>
            </div>
            <div className="p-3 bg-red-50 rounded-full text-red-600">
              <FiAlertCircle />
            </div>
          </div>
        </div>
      </div>

      {/* Filters and Search */}
      <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 flex flex-col md:flex-row gap-4 justify-between items-center">
        <div className="relative w-full md:w-96">
          <FiSearch className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Search by Case Title, or Description..."
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setPage(0);
            }}
          />
        </div>
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
            <option value="OPEN">Open</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="CLOSED">Closed</option>
          </select>
        </div>
      </div>

      {/* Cases Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          
          <div className="relative">
            <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            {loading && (
              <div className="absolute inset-0 bg-white/70 flex items-center justify-center z-10">
                <div className="flex items-center gap-2 text-blue-600">
                  <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none" >
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
                  </svg>
                  <span className="font-medium">Loading cases...</span>
                </div>
              </div>
            )}
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Case Details</th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Parties</th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Progress</th>
                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredCases.map((c) => (
                <tr key={c.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4">
                    <div className="flex flex-col">
                      <span className="font-medium text-gray-900">{c.title}</span>
                      <span className="text-xs text-gray-500">{c.id} • {c.category}</span>
                      <span className={`text-xs mt-1 px-2 py-0.5 rounded-full w-fit ${getPriorityColor(c.priority)}`}>
                        {c.priority} Priority
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex flex-col text-sm">
                      <span className="text-gray-900">Citizen: {c.citizen}</span>
                      <span className={getAssignedToStyle(c.assignedTo, c.matchStatus)}>
                        Assigned: {c.assignedTo}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex flex-col gap-1">
                      <span className={`px-3 py-1 text-xs font-medium rounded-full text-center min-w-[120px] ${getStatusColor(c.status)}`}>
                        {c.status}
                      </span>
                      <span className={`px-3 py-1 text-xs font-medium rounded-full text-center min-w-[120px] ${getMatchStatusColor(c.matchStatus)}`}>
                        {c.matchStatus}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="w-full max-w-xs">
                      <div className="flex flex-col text-xs mb-1 items-center">
                        <span className="text-gray-600 font-medium">{c.progress}%</span>
                        <span className="text-gray-400">{formatDateTime(c.lastUpdate)}</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div 
                          className="bg-blue-600 h-2 rounded-full transition-all duration-500" 
                          style={{ width: `${c.progress}%` }}
                        ></div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <button
                      onClick={() => openDetailsModal(c)}
                      className="text-blue-600 hover:text-blue-800 hover:bg-blue-50 px-3 py-1.5 rounded-lg font-medium text-sm flex items-center gap-1 transition-colors"
                      title="View Case Details"
                    >
                      <FiEye /> View Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>

          {filteredCases.length === 0 && (
          <div className="p-8 text-center text-gray-500">
            No cases found matching your criteria.
          </div>
        )}
        </div>
          <div className="flex items-center justify-between px-6 py-4 border-t bg-gray-50">
            <span className="text-sm text-gray-600">
              Page {page + 1} of {totalPages}
            </span>

            <div className="flex gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
                className="px-3 py-1 border rounded-lg disabled:opacity-50 flex items-center gap-1"
              >
                <FiChevronLeft /> Prev
              </button>

              <button
                disabled={page + 1 >= totalPages}
                onClick={() => setPage(p => p + 1)}
                className="px-3 py-1 border rounded-lg disabled:opacity-50 flex items-center gap-1"
              >
                Next <FiChevronRight />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Case Details Modal */}
      {isDetailsModalOpen && selectedCase && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-3xl w-full p-6 animate-in fade-in zoom-in duration-200 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-2xl font-semibold text-gray-900">Case Details</h3>
              <button
                onClick={closeDetailsModal}
                className="text-gray-400 hover:text-gray-600 transition-colors"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="space-y-4">
              {/* Case Title */}
              <div className="bg-gray-50 p-4 rounded-lg">
                <label className="text-sm font-medium text-gray-500 block mb-1">Case Title</label>
                <p className="text-lg font-medium text-gray-900">{selectedCase.title}</p>
              </div>

              {/* Case ID and Category */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-gray-50 p-4 rounded-lg">
                  <label className="text-sm font-medium text-gray-500 block mb-1">Case ID</label>
                  <p className="text-lg text-gray-900 font-mono">{selectedCase.id}</p>
                </div>
                <div className="bg-gray-50 p-4 rounded-lg">
                  <label className="text-sm font-medium text-gray-500 block mb-1">Category</label>
                  <p className="text-lg text-gray-900">{selectedCase.category}</p>
                </div>
              </div>

              {/* Status and Priority */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-gray-50 p-4 rounded-lg">
                  <label className="text-sm font-medium text-gray-500 block mb-2">Status</label>
                  <span className={`px-3 py-1 text-sm font-medium rounded-full inline-block min-w-[120px] text-center ${getStatusColor(selectedCase.status)}`}>
                    {selectedCase.status}
                  </span>
                </div>
                <div className="bg-gray-50 p-4 rounded-lg">
                  <label className="text-sm font-medium text-gray-500 block mb-2">Priority</label>
                  <span className={`px-3 py-1 text-sm font-medium rounded-full inline-block ${getPriorityColor(selectedCase.priority)}`}>
                    {selectedCase.priority} Priority
                  </span>
                </div>
              </div>

              {/* Parties Involved */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-gray-50 p-4 rounded-lg">
                  <label className="text-sm font-medium text-gray-500 block mb-1">Citizen</label>
                  <p className="text-lg text-gray-900">{selectedCase.citizen}</p>
                </div>
                <div className="bg-gray-50 p-4 rounded-lg">
                  <label className="text-sm font-medium text-gray-500 block mb-1">Assigned To</label>
                  <p className={`text-lg ${getAssignedToStyle(selectedCase.assignedTo, selectedCase.matchStatus)}`}>
                    {selectedCase.assignedTo}
                  </p>
                  <span className={`mt-2 px-3 py-1 text-xs font-medium rounded-full inline-block min-w-[120px] text-center ${getMatchStatusColor(selectedCase.matchStatus)}`}>
                    {selectedCase.matchStatus}
                  </span>
                </div>
              </div>

              {/* Description */}
              {selectedCase.description && (
                <div className="bg-gray-50 p-4 rounded-lg">
                  <label className="text-sm font-medium text-gray-500 block mb-1">Description</label>
                  <p className="text-gray-900">{selectedCase.description}</p>
                </div>
              )}

              {/* Progress Bar */}
              <div className="bg-gray-50 p-4 rounded-lg">
                <label className="text-sm font-medium text-gray-500 block mb-2">Progress</label>
                <div className="w-full">
                  <div className="flex justify-between text-sm mb-2">
                    <span className="text-gray-900 font-medium">{selectedCase.progress}% Complete</span>
                    <span className="text-gray-500">Last Updated: {formatDateTime(selectedCase.lastUpdate)}</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-3">
                    <div
                      className="bg-blue-600 h-3 rounded-full transition-all duration-500"
                      style={{ width: `${selectedCase.progress}%` }}
                    ></div>
                  </div>
                </div>
              </div>

              {/* Dates */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {selectedCase.createdAt && (
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <label className="text-sm font-medium text-gray-500 block mb-1">Created Date</label>
                    <p className="text-lg text-gray-900">{formatDateTime(selectedCase.createdAt)}</p>
                  </div>
                )}
                {selectedCase.lastUpdate && (
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <label className="text-sm font-medium text-gray-500 block mb-1">Last Updated</label>
                    <p className="text-lg text-gray-900">{formatDateTime(selectedCase.lastUpdate)}</p>
                  </div>
                )}
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex justify-end gap-3 mt-6 pt-6 border-t border-gray-200">
              <button
                onClick={closeDetailsModal}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CaseMonitoring;
