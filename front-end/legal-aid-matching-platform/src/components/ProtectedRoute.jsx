import { Navigate } from "react-router-dom";

export default function ProtectedRoute({ allowedRoles, children }) {
  const accessToken = sessionStorage.getItem("accessToken");
  const role = sessionStorage.getItem("role");

  // Not logged in
  if (!accessToken) return <Navigate to="/signin" replace />;

  // Wrong role
  if (!allowedRoles.includes(role)) {
    return <Navigate to={`/dashboard/${role.toLowerCase()}`} replace />;
  }

  return children;
}
