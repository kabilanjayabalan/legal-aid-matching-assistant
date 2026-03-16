import React, { useState, useEffect } from 'react';
import { fetchMatchesForMonitoring, fetchMatchStats, deleteMatch } from '../../../services/admin';
import { FiSearch, FiFilter, FiTrash2, FiActivity, FiCheckCircle, FiClock, FiAlertCircle, FiChevronLeft, FiChevronRight, FiUser, FiUsers, FiArrowUp, FiArrowDown } from 'react-icons/fi';

const MatchManagement = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [matches, setMatches] = useState([]);
  const [sortOrder, setSortOrder] = useState('desc'); // 'asc' or 'desc'
  const [stats, setStats] = useState({
    total: 0,
    pending: 0,
    citizenAccepted: 0,
    providerConfirmed: 0,
    rejected: 0,
    averageMatchPercentage: 0
  });
  const [deleteLoading, setDeleteLoading] = useState({});

  useEffect(() => {
    fetchStats();
  }, []);

  useEffect(() => {
    fetchMatches();
  }, [page, searchTerm, filterStatus, sortOrder]);

  const fetchStats = async () => {
    try {
      const res = await fetchMatchStats();
      setStats({
        total: res.data.totalMatches,
        pending: res.data.pending,
        citizenAccepted: res.data.citizenAccepted,
        providerConfirmed: res.data.providerConfirmed,
        rejected: res.data.rejected,
        averageMatchPercentage: res.data.averageMatchPercentage
      });
    } catch (err) {
      console.error('Failed to load match stats', err);
    }
  };

  const fetchMatches = async () => {
    try {
      setLoading(true);
      const res = await fetchMatchesForMonitoring({
        page,
        size,
        search: searchTerm || null,
        status: filterStatus === 'ALL' ? null : filterStatus
      });

      // Sort the matches by score based on sortOrder
      let sortedMatches = [...res.data.content];
      if (sortOrder === 'asc') {
        sortedMatches.sort((a, b) => (a.score || 0) - (b.score || 0));
      } else {
        sortedMatches.sort((a, b) => (b.score || 0) - (a.score || 0));
      }

      setMatches(sortedMatches);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      console.error('Failed to fetch matches', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteMatch = async (matchId) => {
    if (!window.confirm('Are you sure you want to delete this match? This action cannot be undone.')) {
      return;
    }

    try {
      setDeleteLoading(prev => ({ ...prev, [matchId]: true }));
      await deleteMatch(matchId);

      // Refresh matches and stats
      await fetchMatches();
      await fetchStats();

      alert('Match deleted successfully');
    } catch (err) {
      console.error('Failed to delete match', err);
      alert('Failed to delete match. Please try again.');
    } finally {
      setDeleteLoading(prev => ({ ...prev, [matchId]: false }));
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

  const getStatusColor = (status) => {
    switch (status) {
      case 'PROVIDER_CONFIRMED': return 'bg-green-100 text-green-800';
      case 'CITIZEN_ACCEPTED': return 'bg-blue-100 text-blue-800';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'REJECTED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusLabel = (status) => {
    switch (status) {
      case 'PROVIDER_CONFIRMED': return 'Confirmed';
      case 'CITIZEN_ACCEPTED': return 'Accepted';
      case 'PENDING': return 'Pending';
      case 'REJECTED': return 'Rejected';
      default: return status;
    }
  };

  const getProviderTypeIcon = (providerType) => {
    return providerType === 'LAWYER' ? <FiUser className="inline" /> : <FiUsers className="inline" />;
  };

  const toggleSortOrder = () => {
    setSortOrder(prevOrder => prevOrder === 'asc' ? 'desc' : 'asc');
  };

  return (
    <div className="space-y-6">
      {/* Header Stats */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Total Matches</p>
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
              <p className="text-sm text-gray-500">Confirmed</p>
              <p className="text-2xl font-bold text-green-600">{stats.providerConfirmed}</p>
            </div>
            <div className="p-3 bg-green-50 rounded-full text-green-600">
              <FiCheckCircle />
            </div>
          </div>
        </div>

        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Pending</p>
              <p className="text-2xl font-bold text-yellow-600">{stats.pending}</p>
            </div>
            <div className="p-3 bg-yellow-50 rounded-full text-yellow-600">
              <FiClock />
            </div>
          </div>
        </div>

        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Rejected</p>
              <p className="text-2xl font-bold text-red-600">{stats.rejected}</p>
            </div>
            <div className="p-3 bg-red-50 rounded-full text-red-600">
              <FiAlertCircle />
            </div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-blue-600 to-blue-800 p-4 rounded-lg shadow-sm border border-blue-700">
          <div className="flex items-center justify-between">
            <div className="text-white">
              <p className="text-sm text-blue-100">Avg. Match %</p>
              <p className="text-3xl font-bold">{stats.averageMatchPercentage}%</p>
            </div>
            <div className="p-3 bg-white/20 rounded-full text-white">
              <FiActivity className="text-xl" />
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
            placeholder="Search by case title, citizen, or provider..."
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
            <option value="PENDING">Pending</option>
            <option value="CITIZEN_ACCEPTED">Citizen Accepted</option>
            <option value="PROVIDER_CONFIRMED">Provider Confirmed</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>
      </div>

      {/* Matches Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
           <div className="relative">
             <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              {loading && (
                <div className="absolute inset-0 bg-white/70 flex items-center justify-center z-10">
                  <div className="flex items-center gap-2 text-blue-600">
                    <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
                    </svg>
                    <span className="font-medium">Loading matches...</span>
                  </div>
                </div>
              )}
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Match ID</th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Case Details</th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Citizen</th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Provider</th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    <button
                      onClick={toggleSortOrder}
                      className="flex items-center gap-1 hover:text-blue-600 transition-colors cursor-pointer"
                      title={`Sort by score ${sortOrder === 'asc' ? 'descending' : 'ascending'}`}
                    >
                      Score
                      {sortOrder === 'desc' ? (
                        <FiArrowDown className="text-blue-600" />
                      ) : (
                        <FiArrowUp className="text-blue-600" />
                      )}
                    </button>
                  </th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Status</th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Created</th>
                  <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {matches.map((match) => (
                  <tr key={match.matchId} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4">
                      <span className="font-mono text-sm text-gray-600">#{match.matchId}</span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex flex-col">
                        <span className="font-medium text-gray-900">{match.caseTitle}</span>
                        <span className="text-xs text-gray-500">Case #{match.caseId} • {match.caseCategory}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex flex-col text-sm">
                        <span className="text-gray-900">{match.citizenName || 'N/A'}</span>
                        <span className="text-xs text-gray-500">ID: {match.citizenId}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex flex-col text-sm">
                        <span className="text-gray-900 flex items-center gap-1">
                          {getProviderTypeIcon(match.providerType)}
                          {match.providerName || 'N/A'}
                        </span>
                        <span className="text-xs text-gray-500">
                          {match.providerType} • ID: {match.providerId}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <div className="w-16 bg-gray-200 rounded-full h-2">
                          <div
                            className="bg-blue-600 h-2 rounded-full transition-all duration-500"
                            style={{ width: `${match.score || 0}%` }}
                          ></div>
                        </div>
                        <span className="text-sm font-medium text-gray-700">{match.score || 0}%</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(match.status)}`}>
                        {getStatusLabel(match.status)}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-xs text-gray-500">{formatDateTime(match.createdAt)}</span>
                    </td>
                    <td className="px-6 py-4">
                      <button
                        onClick={() => handleDeleteMatch(match.matchId)}
                        disabled={deleteLoading[match.matchId]}
                        className="text-red-600 hover:text-red-800 font-medium text-sm flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {deleteLoading[match.matchId] ? (
                          <>
                            <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
                            </svg>
                            Deleting...
                          </>
                        ) : (
                          <>
                            <FiTrash2 /> Delete
                          </>
                        )}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>


            {matches.length === 0 && (
              <div className="p-8 text-center text-gray-500">
                No matches found matching your criteria.
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
    </div>
  );
};

export default MatchManagement;

