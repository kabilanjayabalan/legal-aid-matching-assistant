import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import useAuthStore from "../store/useAuthStore";

const API_BASE = process.env.REACT_APP_API_BASE || "http://localhost:8080";

export default function OAuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);

  useEffect(() => {
    const accessToken = searchParams.get("accessToken");
    const refreshToken = searchParams.get("refreshToken");
    const email = searchParams.get("email");
    const role = searchParams.get("role");
    const isNewUser = searchParams.get("isNewUser") === "true";

    if (!accessToken || !refreshToken || !email || !role) {
      navigate("/signin");
      return;
    }

    // persist tokens
    sessionStorage.setItem("accessToken", accessToken);
    sessionStorage.setItem("refreshToken", refreshToken);
    sessionStorage.setItem("role", role);
    sessionStorage.setItem("email", email);
    sessionStorage.setItem("isAuthenticated", "true");
    sessionStorage.setItem("isNewUser", isNewUser ? "true" : "false");

    login({ email }, role, accessToken, refreshToken);

    if (isNewUser) {
      alert("Welcome! Your account was created via Google sign-in.");
    }

    // role based redirect
    const target =
      role === "ADMIN"
        ? "/dashboard/admin"
        : role === "LAWYER"
        ? "/dashboard/lawyer"
        : role === "NGO"
        ? "/dashboard/ngo"
        : "/dashboard/citizen";

    navigate(target, { replace: true });
  }, [searchParams, navigate, login]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <p className="text-lg font-semibold text-gray-800">
          Signing you in with Google...
        </p>
        <p className="text-sm text-gray-500 mt-2">
          If this takes more than a few seconds, please try again.
        </p>
      </div>
    </div>
  );
}

