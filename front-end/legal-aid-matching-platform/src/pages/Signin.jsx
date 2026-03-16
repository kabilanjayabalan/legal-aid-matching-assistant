import { useState } from "react";
import { Link } from "react-router-dom";
import useAuthStore from "../store/useAuthStore";
import googleLogo from "../Images/google logo.jpg";
import githubLogo from "../Images/github-seeklogo.png";
import loginImage from "../Images/Loginimage.png";
import { useNavigate } from "react-router-dom";
import api from "../services/api";
import { useAlert } from "../context/AlertContext";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { useEffect } from "react";


const OAUTH_URL =
  process.env.REACT_APP_API_BASE?.replace(/\/$/, "") ||
  "http://localhost:8080";
function Signin() {
  const { showAlert } = useAlert();
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  const [showPassword, setShowPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false); // New state for reset password visibility
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  const [forgotEmail, setForgotEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [step, setStep] = useState(1); // 1: Email, 2: OTP, 3: New Password


  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState({ email: "", password: "" });
  useEffect(() => {
      document.title = "Login | Legal Aid";
    }, []);

  const handleGoogle = () => {
    window.location.href = `${OAUTH_URL}/oauth2/authorization/google`;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const newErrors = { email: "", password: "" };
    if (!email) newErrors.email = "Email is required.";
    if (!password) newErrors.password = "Password is required.";
    setErrors(newErrors);

    if (newErrors.email || newErrors.password) return;

    try {
      // Backend login request
      const res = await api.post("/auth/login", {
        email,
        password,
      });

      // Expected response from backend
      const { accessToken, refreshToken, email: userEmail, role } = res.data;

      // Save tokens in storage for axios interceptor
      sessionStorage.setItem("accessToken", accessToken);
      sessionStorage.setItem("refreshToken", refreshToken);
      sessionStorage.setItem("role", role);

      // Zustand login (store user data in global state)
      login({ email: userEmail }, role, accessToken, refreshToken);

      showAlert(`Login successful as ${role}`);

      // Role-based navigation
      if (role === "ADMIN") navigate("/dashboard/admin/adminpanel");
      else if (role === "LAWYER") navigate("/dashboard/lawyer/stats");
      else if (role === "NGO") navigate("/dashboard/ngo/overview");
      else if(role === "CITIZEN") navigate("/dashboard/citizen/overview");

    } catch (err) {
      const backendMsg = err.response?.data?.message || "Invalid credentials!";
      showAlert(backendMsg);
    }
  };

  const handleForgotPassword = async (e) => {
    e.preventDefault();
    try {
      await api.post("/auth/forgot-password", { email: forgotEmail });
      showAlert("OTP sent to your email!");
      setStep(2);
    } catch (err) {
      showAlert(err.response?.data?.message || "Failed to send OTP.");
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    try {
      await api.post("/auth/verify-otp", { email: forgotEmail, otp });
      showAlert("OTP verified! Please set your new password.");
      setStep(3);
    } catch (err) {
      showAlert(err.response?.data?.message || "Invalid or expired OTP.");
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    try {
      await api.post("/auth/reset-password", {
        email: forgotEmail,
        otp,
        newPassword
      });
      showAlert("Password reset successfully! Please login.");
      setShowForgotPassword(false);
      setStep(1);
      setForgotEmail("");
      setOtp("");
      setNewPassword("");
    } catch (err) {
      showAlert(err.response?.data?.message || "Failed to reset password.");
    }
  };


  return (
    <div className="min-h-screen bg-gray-50 flex items-center px-4 py-10 sm:px-6 lg:px-8 animate-fadeIn">
      <div className="max-w-7xl w-full mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center">
          {/* LEFT SIDE */}
          <div className="flex flex-col justify-center  animate-slideInLeft">
          {/* Text */}
            <div className="mb-8">
              <h1 className="text-3xl md:text-2xl font-bold tracking-tight text-blue-950 text-center font-serif">
                WELCOME TO<br/>LEGAL-AID MATCH PLATFORM
              </h1>

              <p className="mt-2 text-gray-600 text-center lg:text-left">
                Connect, collaborate, and access legal aid resources.
              </p>
            </div>

          {/* Image */}
            <div className="hidden lg:flex justify-center">
              <img
                src={loginImage}
                alt="Legal Aid Login"
                className="max-w-full h-auto rounded-2xl shadow-lg object-cover"
              />
            </div>
          </div>

          {/* Right side - Login Form */}
          <div className="w-full max-w-lg mx-auto lg:mx-0 animate-slideInRight p-2">
            {/* Brand / Header */}
            <div className="flex flex-col items-center mb-8">
              <div className="flex items-center gap-2 mb-3">
                <div className="h-10 w-10 rounded-2xl bg-blue-900 flex items-center justify-center">
                  <span className="text-white text-xl font-bold">⚖️</span>
                </div>
                <span className="text-lg font-bold text-blue-950 ">
                  LEGAL-AID MATCHING PLATFORM
                </span>
              </div>
              
            </div>

            {/* Tabs */}
            <div className="flex rounded-full bg-gray-100 p-1 mb-6 text-sm font-medium">
              <button
                type="button"
                className="w-1/2 text-center py-2 rounded-full bg-white shadow text-gray-900"
              >
                Login
              </button>
              <Link
                to="/signup"
                className="w-1/2 text-center py-2 rounded-full text-gray-600 hover:text-gray-900 transition-colors duration-200"
              >
                Register
              </Link>
            </div>

            {/* Form card */}
            <div className="bg-white rounded-3xl shadow-xl border border-gray-100 px-6 py-7 sm:px-8 sm:py-8">
              {!showForgotPassword ? (
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label
                    htmlFor="email"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Email
                  </label>
                  <div className="mt-1 relative rounded-lg shadow-sm">
                    <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-400">
                      {/* Mail icon */}
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-5 w-5"
                        viewBox="0 0 20 20"
                        fill="currentColor"
                      >
                        <path d="M2.94 6.34A2 2 0 0 1 4.9 5h10.2a2 2 0 0 1 1.96 1.34L10 10.882 2.94 6.34z" />
                        <path d="M2 8.118V13a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8.118l-7.12 4.54a1 1 0 0 1-1.08 0L2 8.118z" />
                      </svg>
                    </span>
                    <input
                      id="email"
                      name="email"
                      type="text"
                      className="block w-full rounded-lg border border-gray-300 pl-10 pr-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                      placeholder="aaaa@example.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                    />
                  </div>
                  {errors.email && (
                    <p className="mt-1 text-xs text-red-600">{errors.email}</p>
                  )}
                </div>

                <div>
                  <label
                    htmlFor="password"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Password
                  </label>
                  <div className="mt-1 relative rounded-lg shadow-sm">
                    <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-400">
                      {/* Lock icon */}
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-5 w-5"
                        viewBox="0 0 20 20"
                        fill="currentColor"
                      >
                        <path
                          fillRule="evenodd"
                          d="M10 2a4 4 0 0 0-4 4v2H5a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2h-1V6a4 4 0 0 0-4-4zm-2 6V6a2 2 0 1 1 4 0v2H8z"
                          clipRule="evenodd"
                        />
                      </svg>
                    </span>
                    <div className="relative">
                      <input
                        type={showPassword ? "text" : "password"}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Password"
                        className="w-full px-4 py-2 border rounded-md"
                      />

                      {/* Eye Icon */}
                      <span
                        className="absolute right-3 top-1/2 -translate-y-1/2 cursor-pointer text-gray-500"
                        onClick={() => setShowPassword(!showPassword)}
                        >
                        {showPassword ? <FaEyeSlash size={20} /> : <FaEye size={20} />}
                      </span>
                    </div>

                  </div>
                  {errors.password && (
                    <p className="mt-1 text-xs text-red-600">{errors.password}</p>
                  )}
                  <div className="mt-2 text-right">
                    <button
                      type="button"
                      onClick={() => setShowForgotPassword(true)}
                      className="text-xs font-medium text-blue-950 hover:text-gray-900"
                    >
                      Forgot password?
                    </button>
                  </div>
                </div>

                <div className="flex items-start gap-2 text-sm">
                  <input
                    type="checkbox"
                    id="terms"
                    checked={acceptedTerms}
                    onChange={(e) => setAcceptedTerms(e.target.checked)}
                    className="mt-1"
                  />
                  <label htmlFor="terms" className="text-gray-600">
                    I agree to the{" "}
                    <Link to="/terms" className="text-blue-800 underline">
                      Terms & Conditions
                    </Link>
                  </label>
                </div>

                <button
                  type="submit"
                  disabled={!acceptedTerms}
                  className={`mt-2 w-full inline-flex items-center justify-center rounded-lg px-4 py-2.5 text-sm font-semibold text-white shadow-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 ${
                    acceptedTerms
                      ? "bg-blue-800 hover:bg-blue-900"
                      : "bg-gray-400 cursor-not-allowed"
                  }`}
                >
                  Sign In
                </button>
              </form>
              ) : (
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold text-gray-900 text-center">Reset Password</h3>
                  {step === 1 && (
                    <form onSubmit={handleForgotPassword} className="space-y-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700">Enter your registered email</label>
                        <input
                          type="email"
                          required
                          className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-600 focus:ring-blue-600"
                          value={forgotEmail}
                          onChange={(e) => setForgotEmail(e.target.value)}
                        />
                      </div>
                      <button
                        type="submit"
                        className="w-full rounded-lg bg-blue-800 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-900"
                      >
                        Send OTP
                      </button>
                    </form>
                  )}
                  {step === 2 && (
                    <form onSubmit={handleVerifyOtp} className="space-y-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700">Enter OTP</label>
                        <input
                          type="text"
                          required
                          className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-600 focus:ring-blue-600"
                          value={otp}
                          onChange={(e) => setOtp(e.target.value)}
                        />
                      </div>
                      <button
                        type="submit"
                        className="w-full rounded-lg bg-blue-800 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-900"
                      >
                        Verify OTP
                      </button>
                    </form>
                  )}
                  {step === 3 && (
                    <form onSubmit={handleResetPassword} className="space-y-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700">New Password</label>
                        <div className="relative">
                          <input
                            type={showNewPassword ? "text" : "password"}
                            required
                            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-600 focus:ring-blue-600 pr-10"
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                          />
                          <span
                            className="absolute right-3 top-1/2 -translate-y-1/2 cursor-pointer text-gray-500"
                            onClick={() => setShowNewPassword(!showNewPassword)}
                          >
                            {showNewPassword ? <FaEyeSlash size={18} /> : <FaEye size={18} />}
                          </span>
                        </div>
                        <p className="mt-1 text-xs text-gray-500">
                          Must be at least 8 chars, include 1 uppercase, 1 lowercase, 1 number, and 1 special char.
                        </p>
                      </div>
                      <button
                        type="submit"
                        className="w-full rounded-lg bg-blue-800 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-900"
                      >
                        Reset Password
                      </button>
                    </form>
                  )}
                  <button
                    onClick={() => {
                      setShowForgotPassword(false);
                      setStep(1);
                    }}
                    className="w-full text-center text-sm text-gray-600 hover:text-gray-900"
                  >
                    Back to Login
                  </button>
                </div>
              )}

              {/* Social sign-in */}
              {!showForgotPassword && (
              <div className="mt-4">
                <div className="relative mb-3">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-gray-200" />
                  </div>
                  <div className="relative flex justify-center text-xs uppercase">
                    <span className="bg-white px-3 text-gray-400">OR</span>
                  </div>
                </div>

                <div className="flex justify-center">
                  <button
                    type="button"
                    onClick={handleGoogle}
                    disabled={!acceptedTerms}
                    className={`inline-flex items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-3 py-2 text-xs font-medium text-gray-700 shadow-sm ${
                      acceptedTerms ? "hover:bg-gray-50" : "opacity-50 cursor-not-allowed"
                    }`}
                  >
                    <img
                      src={googleLogo}
                      alt="Google logo"
                      className="h-4 w-4"
                    />
                    <span>Google</span>
                  </button>
                </div>
              </div>
              )}
            </div>
          </div>
        </div>
      </div>
      
    </div>
  );
}

export default Signin;
