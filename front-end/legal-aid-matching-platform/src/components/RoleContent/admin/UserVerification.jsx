import { useEffect, useState } from "react";

export default function UserVerification() {
  const [users, setUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [role, setRole] = useState("");
  const [location,setLocation] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [showFilters, setShowFilters] = useState(false);
  const token =
    sessionStorage.getItem("accessToken");

  useEffect(() => {
    fetchUsers();
  }, [page]);

const fetchUsers = async () => {
  try {
    setLoading(true);

    const params = new URLSearchParams();
    params.append("page", page);
    params.append("size", size);

    if (role) params.append("role", role);
    if (statusFilter) params.append("status", statusFilter);

    const res = await fetch(
      `http://localhost:8080/admin/users/lawyers-ngos?${params.toString()}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    const data = await res.json();
    setUsers(data.content || []);
    setTotalPages(data.totalPages || 0);
  } catch (err) {
    console.error(err);
  } finally {
    setLoading(false);
  }
};



  const approveUser = async (username) => {
    await fetch(`http://localhost:8080/admin/approve/${username}`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    fetchUsers();
    setSelectedUser(null);
  };

  const rejectUser = async (username) => {
    await fetch(`http://localhost:8080/admin/reject/${username}`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    fetchUsers();
    setSelectedUser(null);
  };

  const formatDate = (date) => date.split("T")[0];

  const getStatus = (approved) => {
    if (approved === true) return "Approved";
    if (approved === false) return "Rejected";
    return "Pending";
  };

  const handleFilter = () => {
    setPage(0);
    fetchUsers();
  };

  const clearFilters = () => {
    setRole("");
    setStatusFilter("");
    setPage(0);
  };

  /* ======================================================
     MOBILE VIEW — DETAILS PAGE
  ====================================================== */
  if (selectedUser) {
    const status = getStatus(selectedUser.approved);

    return (
      <div className="fixed inset-0 z-50 flex justify-center items-center 
                      bg-black/40 backdrop-blur-sm p-4">
        <div className="w-full max-w-sm bg-white p-4 rounded-xl overflow-y-auto max-h-[95vh]">

          <h2 className="text-xl font-bold mb-2">User Details</h2>

          <div className="bg-gray-100 p-3 rounded-lg mb-2">
            <p className="text-gray-500 text-sm">Full Name</p>
            <p className="text-md font-medium">{selectedUser.fullName}</p>
          </div>

          <div className="bg-gray-100 p-3 rounded-lg mb-2">
            <p className="text-gray-500 text-sm">Role</p>
            <p className="text-md font-medium">{selectedUser.role}</p>
          </div>

          <div className="bg-gray-100 p-3 rounded-lg mb-2">
            <p className="text-gray-500 text-sm">Email</p>
            <p className="text-md font-medium">{selectedUser.email}</p>
          </div>

          <div className="bg-gray-100 p-3 rounded-lg mb-2">
            <p className="text-gray-500 text-sm">Submitted Date</p>
            <p className="text-md font-medium">
              {formatDate(selectedUser.createdAt)}
            </p>
          </div>

          <div className="bg-gray-100 p-3 rounded-lg mb-2">
            <p className="text-gray-500 text-sm">Status</p>
            <span
              className={`px-3 py-1 rounded-full text-sm ${
                status === "Approved"
                  ? "bg-green-100 text-green-700"
                  : status === "Rejected"
                  ? "bg-red-100 text-red-700"
                  : "bg-yellow-100 text-yellow-700"
              }`}
            >
              {status}
            </span>
          </div>

          {status === "Pending" ? (
            <div className="flex gap-3 mt-4">
              <button
                onClick={() => approveUser(selectedUser.username)}
                className="flex-1 py-2 bg-blue-950 text-white rounded-lg"
              >
                Approve
              </button>
              <button
                onClick={() => rejectUser(selectedUser.username)}
                className="flex-1 py-2 bg-red-600 text-white rounded-lg"
              >
                Reject
              </button>
            </div>
          ) : (
            <button
              onClick={() => setSelectedUser(null)}
              className="mt-5 w-full py-2 bg-blue-950 text-white rounded-lg"
            >
              Go Back
            </button>
          )}
        </div>
      </div>
    );
  }

  /* ======================================================
     MOBILE LIST + DESKTOP TABLE (UNCHANGED)
  ====================================================== */

  return (
    <>
      {/* MOBILE */}
      <div className="block lg:hidden w-full mt-4 p-2">
        <h2 className="text-lg font-semibold mb-3">Verification Requests</h2>

        {users && users.length > 0 ? (
          users.map((u, index) => (
            <div
              key={index}
              className="bg-white p-4 mb-3 rounded-xl shadow border"
            >
              <p className="font-medium text-base">{u.fullName}</p>
              <p className="text-sm text-gray-500">{u.role}</p>
              <p className="text-sm text-gray-500">{u.email}</p>

              <button
                onClick={() => setSelectedUser(u)}
                className="mt-3 w-full py-2 bg-blue-950 text-white rounded-lg"
              >
                View Details
              </button>
            </div>
          ))
        ) : (
          <p className="text-gray-500 text-center py-4">
            {loading ? "Loading..." : "No verification requests found"}
          </p>
        )}
      </div>

      {/* DESKTOP */}
      <div className="hidden lg:block">
        <div className="flex justify-between items-center mb-4 mt-2">
          <div>
        <h3 className="text-xl font-medium text-gray-800 mb-1">
          User Verification Queue
        </h3>
        <p className="text-gray-600 mb-4">
          Review and approve/reject profiles for Lawyers and NGOs.
        </p>
        </div>
        <button
          onClick={() => setShowFilters((prev) => !prev)}
          className="px-4 py-2 text-sm border rounded-lg bg-white hover:bg-gray-50"
        >
          {showFilters ? "Hide Filters" : "Show Filters"}
        </button>
        </div>
        {showFilters && (
        <div className="bg-white border rounded-lg p-3 mb-4 animate-fadeIn">
          <div className="grid grid-cols-1 md:grid-cols-5 gap-3 items-end">
      
            {/* Level */}
            <div>
              <label className="block text-xs font-medium mb-1">Role</label>
                <select
                  value={role}
                  onChange={(e) =>{
                    setRole(e.target.value);
                    setPage(0);
                  }}
                  className="w-full border rounded px-3 py-1.5 text-sm"
                >
                  <option value="">All</option>
                  <option value="LAWYER">LAWYER</option>
                  <option value="NGO">NGO</option>
                </select>
            </div>
            <div>
              <label className="block text-xs font-medium mb-1">Status</label>
                <select
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value);
                    setPage(0);
                  }}
                  className="w-full border rounded px-3 py-1.5 text-sm"
                >
                  <option value="">All</option>
                  <option value="PENDING">Pending</option>
                  <option value="APPROVED">Approved</option>
                  <option value="REJECTED">Rejected</option>
                </select>
            </div>

            {/* Location */}

            {/* <div>
              <label className="block text-xs font-medium mb-1">Location</label>
              
                <input
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  placeholder="Search by Location"
                  className="w-full border rounded px-3 py-1.5 text-sm"
                />
            </div> */}
  
            {/* Buttons */}
            <div className="flex justify-end gap-2">
              <button
                onClick={handleFilter}
                className="px-4 py-1.5 bg-blue-900 text-white rounded-md text-sm hover:bg-blue-950"
              >
                Apply
              </button>
              <button
                onClick={clearFilters}
                className="px-4 py-1.5 border rounded-md text-sm hover:bg-gray-100"
              >
                Clear
              </button>
            </div>
          </div>
        </div>
      )}
        <div className="overflow-x-auto rounded-lg border border-gray-200 shadow-sm">
          {loading && (
              <div className="p-6 text-center text-gray-500">
                Loading users...
              </div>
            )}
            {!loading && users.length === 0 && (
              <div className="p-6 text-center text-gray-500">
                No verification requests found.
              </div>
            )}
          {users.length > 0 && (
          <table className="w-full table-auto">
            
            <thead className="bg-gray-100 border-b">
              <tr className="text-left text-gray-700 font-medium">
                <th className="py-3 px-4">Name</th>
                <th className="py-3 px-4">Role</th>
                <th className="py-3 px-4">Email</th>
                <th className="py-3 px-4">Submitted Date</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Actions</th>
              </tr>
            </thead>

            <tbody>
              {users && users.length > 0 ? (
                users.map((u, index) => {
                  const status = getStatus(u.approved);

                  return (
                    <tr
                      key={index}
                      className="border-b hover:bg-gray-50 transition"
                    >
                      <td className="py-3 px-4">{u.fullName}</td>
                      <td className="py-3 px-4">{u.role}</td>
                      <td className="py-3 px-4">{u.email}</td>
                      <td className="py-3 px-4">
                        {formatDate(u.createdAt)}
                      </td>

                      <td className="py-3 px-4">
                        <span
                          className={`px-3 py-1 text-sm rounded-full ${
                            status === "Approved"
                              ? "bg-green-100 text-green-700"
                              : status === "Rejected"
                              ? "bg-red-100 text-red-700"
                              : "bg-yellow-100 text-yellow-700"
                          }`}
                        >
                          {status}
                        </span>
                      </td>

                      <td className="py-3 px-4 flex gap-3">
                        {status === "Pending" ? (
                          <>
                            <button
                              onClick={() => approveUser(u.username)}
                              className="px-3 py-2 bg-blue-950 text-white rounded-md hover:bg-gray-950"
                            >
                              Approve
                            </button>

                            <button
                              onClick={() => rejectUser(u.username)}
                              className="px-3 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
                            >
                              Reject
                            </button>
                          </>
                        ) : (
                          <button
                            className="px-3 py-2 bg-blue-950 text-white rounded-md hover:bg-gray-950"
                            onClick={() => setSelectedUser(u)}
                          >
                            View Details
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan="6" className="py-8 text-center text-gray-500">
                    {loading ? "Loading..." : "No verification requests found"}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          )}
          <div className="flex items-center justify-between px-4 py-3 bg-gray-50 border-t">
            <span className="text-sm text-gray-600">
              Page {page + 1} of {totalPages}
            </span>

            <div className="flex gap-2">
              <button
                disabled={page === 0 || loading}
                onClick={() => setPage(p => p - 1)}
                className="px-3 py-1 border rounded disabled:opacity-50"
              >
                Prev
              </button>

              <button
                disabled={page + 1 >= totalPages || loading}
                onClick={() => setPage(p => p + 1)}
                className="px-3 py-1 border rounded disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
