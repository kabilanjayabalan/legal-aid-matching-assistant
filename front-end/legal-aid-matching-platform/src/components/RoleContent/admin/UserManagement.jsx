import React, { useState } from "react";
import { Search, UserCheck, UserX, Ban, CheckCircle, XCircle, Eye } from "lucide-react";
import { useEffect } from "react";
import { fetchUsers, activateUser, deactivateUser, blockUser } from "../../../services/admin";



export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);

  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [selectedUser, setSelectedUser] = useState(null);
  const [isActionModalOpen, setIsActionModalOpen] = useState(false);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [actionType, setActionType] = useState("");

  const handleSearch = (e) => {
  setSearchTerm(e.target.value);
  setPage(0);
};

const handleRoleFilter = (e) => {
  setRoleFilter(e.target.value);
  setPage(0);
};

const handleStatusFilter = (e) => {
  setStatusFilter(e.target.value);
  setPage(0);
};


  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesRole = roleFilter === "ALL" || user.role === roleFilter;
    const matchesStatus = statusFilter === "ALL" || user.status === statusFilter;
    return matchesSearch && matchesRole && matchesStatus;
  });

  const openActionModal = (user, type) => {
    setSelectedUser(user);
    setActionType(type);
    setIsActionModalOpen(true);
  };

  const closeActionModal = () => {
    setIsActionModalOpen(false);
    setSelectedUser(null);
    setActionType("");
  };

  const openDetailsModal = (user) => {
    setSelectedUser(user);
    setIsDetailsModalOpen(true);
  };

  const closeDetailsModal = () => {
    setIsDetailsModalOpen(false);
    setSelectedUser(null);
  };

  const goToPage = (p) => {
  if (p >= 0 && p < totalPages) {
    setPage(p);
  }
};

const goNext = () => {
  if (page < totalPages - 1) {
    setPage(page + 1);
  }
};

const goPrev = () => {
  if (page > 0) {
    setPage(page - 1);
  }
};

 const handleAction = async () => {
  if (!selectedUser) return;

  try {
    if (actionType === "activate") {
      await activateUser(selectedUser.id);
    } else if (actionType === "deactivate") {
      await deactivateUser(selectedUser.id);
    } else if (actionType === "block") {
      await blockUser(selectedUser.id);
    }

    await loadUsers(); // refresh list
  } catch (err) {
    console.error("Action failed", err);
  } finally {
    closeActionModal();
  }
};


  const getStatusBadge = (status) => {
    switch (status) {
      case "ACTIVE":
        return <span className="px-2 py-1 text-xs font-medium bg-green-100 text-green-800 rounded-full">Active</span>;
      case "INACTIVE":
        return <span className="px-2 py-1 text-xs font-medium bg-gray-100 text-gray-800 rounded-full">Inactive</span>;
      case "PENDING":
        return <span className="px-2 py-1 text-xs font-medium bg-yellow-100 text-yellow-800 rounded-full">Pending</span>;
      case "BLOCKED":
        return <span className="px-2 py-1 text-xs font-medium bg-red-100 text-red-800 rounded-full">Blocked</span>;
      default:
        return <span className="px-2 py-1 text-xs font-medium bg-gray-100 text-gray-800 rounded-full">{status}</span>;
    }
  };
  
  useEffect(() => {
  loadUsers();
}, [searchTerm, roleFilter, statusFilter, page]);

const loadUsers = async () => {
  setLoading(true);
  try {
    const res = await fetchUsers({
      page,
      size: 10,
      search: searchTerm || null,
      role: roleFilter === "ALL" ? null : roleFilter,
      status: statusFilter === "ALL" ? null : statusFilter
    });

    const mapped = res.data.content.map(u => ({
      id: u.id,
      name: u.fullName,
      email: u.email,
      role: u.role,
      status: u.status,
      joinDate: u.createdAt.split("T")[0]
    }));

    setUsers(mapped);
    setTotalPages(res.data.totalPages);
  } catch (err) {
    console.error("Failed to load users", err);
  } finally {
    setLoading(false);
  }
};


  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
      <div className="p-6 border-b border-gray-200">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <h2 className="text-xl font-semibold text-gray-900">User Management</h2>

          <div className="flex flex-col sm:flex-row gap-3">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
              <input
                type="text"
                placeholder="Search users..."
                value={searchTerm}
                onChange={handleSearch}
                className="pl-9 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 w-full sm:w-64"
              />
            </div>

            <div className="flex gap-2">
              <select
                value={roleFilter}
                onChange={handleRoleFilter}
                className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
              >
                <option value="ALL">All Roles</option>
                <option value="CITIZEN">Citizen</option>
                <option value="LAWYER">Lawyer</option>
                <option value="NGO">NGO</option>
              </select>

              <select
                value={statusFilter}
                onChange={handleStatusFilter}
                className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
                <option value="PENDING">Pending</option>
                <option value="BLOCKED">Blocked</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm text-gray-600">
          <thead className="bg-gray-50 text-gray-700 font-medium border-b border-gray-200">
            <tr>
              <th className="px-6 py-4">User</th>
              <th className="px-6 py-4">Role</th>
              <th className="px-6 py-4">Status</th>
              <th className="px-6 py-4">Joined</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading && (
              <tr>
                <td colSpan="5" className="px-6 py-8 text-center text-gray-500">
                  Loading users...
                </td>
              </tr>
            )}
            {filteredUsers.length > 0 ? (
              filteredUsers.map((user) => (
                <tr key={user.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4">
                    <div className="flex flex-col">
                      <span className="font-medium text-gray-900">{user.name}</span>
                      <span className="text-xs text-gray-500">{user.email}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700">
                      {user.role}
                    </span>
                  </td>
                  <td className="px-6 py-4">{getStatusBadge(user.status)}</td>
                  <td className="px-6 py-4">{user.joinDate}</td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end gap-2">
                      <button
                        onClick={() => openDetailsModal(user)}
                        className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        title="View Details"
                      >
                        <Eye size={18} />
                      </button>
                      {user.status !== "ACTIVE" && (
                        <button
                          onClick={() => openActionModal(user, "activate")}
                          className="p-1.5 text-green-600 hover:bg-green-50 rounded-lg transition-colors"
                          title="Activate User"
                        >
                          <UserCheck size={18} />
                        </button>
                      )}
                      {user.status === "ACTIVE" && (
                        <button
                          onClick={() => openActionModal(user, "deactivate")}
                          className="p-1.5 text-yellow-600 hover:bg-yellow-50 rounded-lg transition-colors"
                          title="Deactivate User"
                        >
                          <UserX size={18} />
                        </button>
                      )}
                      {user.status !== "BLOCKED" && (
                        <button
                          onClick={() => openActionModal(user, "block")}
                          className="p-1.5 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                          title="Block User"
                        >
                          <Ban size={18} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="5" className="px-6 py-8 text-center text-gray-500">
                  No users found matching your criteria.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Action Confirmation Modal */}
      {isActionModalOpen && selectedUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 animate-in fade-in zoom-in duration-200">
            <div className="flex items-center gap-3 mb-4">
              {actionType === "activate" && <CheckCircle className="text-green-600 w-6 h-6" />}
              {actionType === "deactivate" && <UserX className="text-yellow-600 w-6 h-6" />}
              {actionType === "block" && <Ban className="text-red-600 w-6 h-6" />}
              <h3 className="text-lg font-semibold text-gray-900 capitalize">
                {actionType} User
              </h3>
            </div>

            <p className="text-gray-600 mb-6">
              Are you sure you want to <strong>{actionType}</strong> the user <strong>{selectedUser.name}</strong>?
              {actionType === "block" && " This will prevent them from accessing the platform."}
            </p>

            <div className="flex justify-end gap-3">
              <button
                onClick={closeActionModal}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleAction}
                className={`px-4 py-2 text-sm font-medium text-white rounded-lg transition-colors ${
                  actionType === "activate" ? "bg-green-600 hover:bg-green-700" :
                  actionType === "deactivate" ? "bg-yellow-600 hover:bg-yellow-700" :
                  "bg-red-600 hover:bg-red-700"
                }`}
              >
                Confirm
              </button>
            </div>
          </div>
        </div>
      )}

      {/* User Details Modal */}
      {isDetailsModalOpen && selectedUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-2xl w-full p-6 animate-in fade-in zoom-in duration-200 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-2xl font-semibold text-gray-900">User Details</h3>
              <button
                onClick={closeDetailsModal}
                className="text-gray-400 hover:text-gray-600 transition-colors"
              >
                <XCircle size={24} />
              </button>
            </div>

            <div className="space-y-4">
              {/* User Name */}
              <div className="bg-gray-50 p-4 rounded-lg">
                <label className="text-sm font-medium text-gray-500 block mb-1">Full Name</label>
                <p className="text-lg font-medium text-gray-900">{selectedUser.name}</p>
              </div>

              {/* Email */}
              <div className="bg-gray-50 p-4 rounded-lg">
                <label className="text-sm font-medium text-gray-500 block mb-1">Email Address</label>
                <p className="text-lg text-gray-900">{selectedUser.email}</p>
              </div>

              {/* Role and Status Row */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Role */}
                <div className="bg-gray-50 p-4 rounded-lg">
                  <label className="text-sm font-medium text-gray-500 block mb-2">Role</label>
                  <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-700">
                    {selectedUser.role}
                  </span>
                </div>

                {/* Status */}
                <div className="bg-gray-50 p-4 rounded-lg">
                  <label className="text-sm font-medium text-gray-500 block mb-2">Account Status</label>
                  {getStatusBadge(selectedUser.status)}
                </div>
              </div>

              {/* User ID */}
              <div className="bg-gray-50 p-4 rounded-lg">
                <label className="text-sm font-medium text-gray-500 block mb-1">User ID</label>
                <p className="text-lg text-gray-900 font-mono">{selectedUser.id}</p>
              </div>

              {/* Join Date */}
              <div className="bg-gray-50 p-4 rounded-lg">
                <label className="text-sm font-medium text-gray-500 block mb-1">Member Since</label>
                <p className="text-lg text-gray-900">{selectedUser.joinDate}</p>
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
              {selectedUser.status !== "ACTIVE" && (
                <button
                  onClick={() => {
                    closeDetailsModal();
                    openActionModal(selectedUser, "activate");
                  }}
                  className="px-4 py-2 text-sm font-medium text-white bg-green-600 hover:bg-green-700 rounded-lg transition-colors"
                >
                  Activate User
                </button>
              )}
              {selectedUser.status === "ACTIVE" && (
                <button
                  onClick={() => {
                    closeDetailsModal();
                    openActionModal(selectedUser, "deactivate");
                  }}
                  className="px-4 py-2 text-sm font-medium text-white bg-yellow-600 hover:bg-yellow-700 rounded-lg transition-colors"
                >
                  Deactivate User
                </button>
              )}
              {selectedUser.status !== "BLOCKED" && (
                <button
                  onClick={() => {
                    closeDetailsModal();
                    openActionModal(selectedUser, "block");
                  }}
                  className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors"
                >
                  Block User
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Pagination */}
{totalPages > 1 && (
  <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200 bg-gray-50">
    
    {/* Page Info */}
    <span className="text-sm text-gray-600">
      Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong>
    </span>

    {/* Controls */}
    <div className="flex items-center gap-1">
      <button
        onClick={goPrev}
        disabled={page === 0}
        className={`px-3 py-1.5 rounded-lg text-sm font-medium transition
          ${page === 0
            ? "text-gray-400 bg-gray-100 cursor-not-allowed"
            : "text-gray-700 bg-white hover:bg-gray-100 border border-gray-300"
          }`}
      >
        Prev
      </button>

      {[...Array(totalPages)].map((_, i) => {
        // limit visible buttons (max 5)
        if (
          i === 0 ||
          i === totalPages - 1 ||
          (i >= page - 1 && i <= page + 1)
        ) {
          return (
            <button
              key={i}
              onClick={() => goToPage(i)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition
                ${page === i
                  ? "bg-blue-600 text-white"
                  : "bg-white text-gray-700 border border-gray-300 hover:bg-gray-100"
                }`}
            >
              {i + 1}
            </button>
          );
        }

        // Ellipsis logic
        if (i === page - 2 || i === page + 2) {
          return (
            <span key={i} className="px-2 text-gray-400">
              …
            </span>
          );
        }

        return null;
      })}

      <button
        onClick={goNext}
        disabled={page === totalPages - 1}
        className={`px-3 py-1.5 rounded-lg text-sm font-medium transition
          ${page === totalPages - 1
            ? "text-gray-400 bg-gray-100 cursor-not-allowed"
            : "text-gray-700 bg-white hover:bg-gray-100 border border-gray-300"
          }`}
      >
        Next
      </button>
    </div>
  </div>
)}

    </div>
  );
}