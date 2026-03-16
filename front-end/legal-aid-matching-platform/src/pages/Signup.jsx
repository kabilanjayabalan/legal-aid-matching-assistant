import { Link ,useNavigate} from "react-router-dom";
import googleLogo from "../Images/google logo.jpg";
import githubLogo from "../Images/github-seeklogo.png";
import loginImage from "../Images/Loginimage.png";
import api from "../services/api";
import { useState } from "react";
import { useAlert } from "../context/AlertContext";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { useEffect } from "react";

const OAUTH_URL =
  process.env.REACT_APP_API_BASE?.replace(/\/$/, "") ||
  "http://localhost:8080";

export default function Signup() {
  const { showAlert } = useAlert();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [acceptedTerms, setAcceptedTerms] = useState(false);

  const [inputs, setInputs] = useState({
    username: "",
    email: "",
    password: "",
    fullName: "",
    role: "citizen",
    referenceId: "",
  });

  const [errors, setErrors] = useState({
    username: "",
    email: "",
    password: "",
    fullName: "",
  });

  const [loading, setLoading] = useState(false);
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [currentStep, setCurrentStep] = useState(1);

  useEffect(() => {
      document.title = "Register | Legal Aid";
    }, []);

  const handleGoogle = () => {
    setShowRoleModal(true);
  };

  const handleRoleSelect = (role) => {
    const normalized = role.toUpperCase();
    document.cookie = `oauth_role=${normalized}; path=/; max-age=300`;
    window.location.href = `${OAUTH_URL}/oauth2/authorization/google`;
  };

  const handleInput = (event) => {
    const { name, value } = event.target;
    // Clear role-specific fields when role changes
    if (name === "role") {
      setCurrentStep(1); // Reset to step 1 when role changes
      if (value === "citizen") {
        setInputs({
          ...inputs,
          [name]: value,
          referenceId: "",
          // Clear lawyer fields
          barRegistrationNo: "",
          specialization: "",
          experienceYears: "",
          city: "",
          bio: "",
          language: "",
          contactInfo: "",
          // Clear NGO fields
          ngoName: "",
          registrationNo: "",
          website: "",
          description: "",
        });
      } else if (value === "lawyer") {
        setInputs({
          ...inputs,
          [name]: value,
          // Clear NGO fields
          ngoName: "",
          registrationNo: "",
          website: "",
          description: "",
        });
      } else if (value === "ngo") {
        setInputs({
          ...inputs,
          [name]: value,
          // Clear lawyer fields
          barRegistrationNo: "",
          specialization: "",
          experienceYears: "",
          bio: "",
        });
      } else {
        setInputs({ ...inputs, [name]: value });
      }
    } else {
      setInputs({ ...inputs, [name]: value });
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const newErrors = { username: "", email: "", password: "", fullName: "" };

    if (!inputs.username) newErrors.username = "Username is required.";
    if (!inputs.email) newErrors.email = "Email is required.";
    if (!inputs.password) newErrors.password = "Password is required.";
    if (!inputs.fullName) newErrors.fullName = "Full name is required.";

    setErrors(newErrors);

    // If any validation error exists, STOP submit
    if (Object.values(newErrors).some((err) => err !== "")) return;

    const roleForBackend = inputs.role.toUpperCase();

    try {
      setLoading(true);

      const registrationData = {
        username: inputs.username,
        email: inputs.email,
        password: inputs.password,
        fullName: inputs.fullName,
        role: roleForBackend,
        referenceId: inputs.referenceId || null,
      };

      // Add lawyer-specific fields if role is LAWYER
      if (roleForBackend === "LAWYER") {
        registrationData.barRegistrationNo = inputs.barRegistrationNo || null;
        registrationData.specialization = inputs.specialization || null;
        registrationData.experienceYears = inputs.experienceYears ? parseInt(inputs.experienceYears) : null;
        registrationData.city = inputs.city || null;
        registrationData.bio = inputs.bio || null;
        registrationData.language = inputs.language || null;
        registrationData.contactInfo = inputs.contactInfo || null;
      }

      // Add NGO-specific fields if role is NGO
      if (roleForBackend === "NGO") {
        registrationData.ngoName = inputs.ngoName || null;
        registrationData.registrationNo = inputs.registrationNo || null;
        registrationData.city = inputs.city || null;
        registrationData.website = inputs.website || null;
        registrationData.description = inputs.description || null;
        registrationData.language = inputs.language || null;
        registrationData.contactInfo = inputs.contactInfo || null;
      }

      await api.post("/auth/register", registrationData);

      showAlert("Registration successful! Please login.");
      navigate("/signin");

    } catch (err) {
      showAlert(err.response?.data?.message || "Registration failed!");
    } finally {
      setLoading(false);
    }
  };

  const nextStep = () => {
    // Validate step 1 fields before proceeding for lawyer/ngo
    if (currentStep === 1 && (inputs.role === "lawyer" || inputs.role === "ngo")) {
      const newErrors = { username: "", email: "", password: "", fullName: "" };

      if (!inputs.username) newErrors.username = "Username is required.";
      if (!inputs.email) newErrors.email = "Email is required.";
      if (!inputs.password) newErrors.password = "Password is required.";
      if (!inputs.fullName) newErrors.fullName = "Full name is required.";

      setErrors(newErrors);

      // If any validation error exists, STOP
      if (Object.values(newErrors).some((err) => err !== "")) return;

      setCurrentStep(2);
    }
  };

  const prevStep = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };


  return (
    <>
    <div className="min-h-screen bg-gray-50 flex items-center px-4 py-10 sm:px-6 lg:px-8 animate-fadeIn">
      <div className="max-w-7xl w-full mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center">
          {/* LEFT SIDE */}
          <div className="flex flex-col justify-center  animate-slideInLeft">
          
            {/* Brand / Header */}
            <div className="flex flex-col items-center mb-8">
              <div className="flex items-center gap-2 mb-3">
                <div className="h-10 w-10 rounded-2xl bg-blue-900 flex items-center justify-center">
                  <span className="text-white text-xl font-semibold">⚖️</span>
                </div>
                <span className="text-lg font-bold text-blue-950 font-serif">
                  LEGAL-AID MATCHING PLATFORM
                </span>
                
              </div>
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

          {/* Right side - Signup Form */}
          <div className="w-full max-w-lg mx-auto lg:mx-0 animate-slideInRight p-2">
            

            {/* Tabs */}
            <div className="flex rounded-full bg-gray-100 p-1 mb-6 text-sm font-medium">
              <Link
                to="/signin"
                className="w-1/2 text-center py-2 rounded-full text-gray-600 hover:text-gray-900 transition-colors duration-200"
              >
                Login
              </Link>
              <button
                type="button"
                className="w-1/2 text-center py-2 rounded-full bg-white shadow text-gray-900"
              >
                Register
              </button>
            </div>

            {/* Form card */}
            <div className="bg-white rounded-3xl shadow-xl border border-gray-100 px-6 py-7 sm:px-8 sm:py-8">
              {/* Progress Indicator for Lawyer/NGO */}
              {(inputs.role === "lawyer" || inputs.role === "ngo") && (
                <div className="mb-6">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center">
                      <div className={`flex items-center justify-center w-8 h-8 rounded-full ${currentStep >= 1 ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-600'} font-semibold text-sm`}>
                        1
                      </div>
                      <span className={`ml-2 text-sm font-medium ${currentStep >= 1 ? 'text-blue-600' : 'text-gray-500'}`}>
                        Basic Info
                      </span>
                    </div>
                    <div className="flex-1 h-1 mx-4 bg-gray-200 rounded">
                      <div className={`h-full rounded transition-all duration-300 ${currentStep >= 2 ? 'bg-blue-600 w-full' : 'bg-gray-200 w-0'}`}></div>
                    </div>
                    <div className="flex items-center">
                      <div className={`flex items-center justify-center w-8 h-8 rounded-full ${currentStep >= 2 ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-600'} font-semibold text-sm`}>
                        2
                      </div>
                      <span className={`ml-2 text-sm font-medium ${currentStep >= 2 ? 'text-blue-600' : 'text-gray-500'}`}>
                        Profile Details
                      </span>
                    </div>
                  </div>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                {/* Step 1: Basic Information (always shown for citizens, step 1 for lawyer/ngo) */}
                {(inputs.role === "citizen" || currentStep === 1) && (
                  <>
                    <div>
                      <label
                        htmlFor="fullName"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Full Name
                      </label>
                  <input
                    id="fullName"
                    name="fullName"
                    type="text"
                    className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                    placeholder="Enter your Name"
                    value={inputs.fullName}
                    onChange={handleInput}
                  />
                  {errors.name && (
                    <p className="mt-1 text-xs text-red-600">{errors.name}</p>
                  )}
                </div>

                <div>
                  <label
                    htmlFor="username"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Username
                  </label>
                  <input
                    id="username"
                    name="username"
                    type="text"
                    className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                    placeholder="Enter your Username"
                    value={inputs.username}
                    onChange={handleInput}
                  />
                  {errors.username && (
                    <p className="mt-1 text-xs text-red-600">{errors.username}</p>
                  )}
                </div>

                <div>
                  <label
                    htmlFor="email"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Email
                  </label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                    placeholder="aaaa@example.com"
                    value={inputs.email}
                    onChange={handleInput}
                  />
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
                  <div className="relative">
                    <input
                      id="password"
                      name="password"
                      type={showPassword ? "text" : "password"}
                      className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                      placeholder="Enter your password"
                      value={inputs.password}
                      onChange={handleInput}
                    />

                    {/* Eye Icon */}
                    <span
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 cursor-pointer"
                      onClick={() => setShowPassword((prev) => !prev)}
                      >
                      {showPassword ? <FaEyeSlash size={18} /> : <FaEye size={18} />}
                    </span>
                  </div>

                  {errors.password && (
                    <p className="mt-1 text-xs text-red-600">{errors.password}</p>
                  )}
                </div>

                <div>
                  <label
                    htmlFor="role"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Role
                  </label>
                  <select
                    id="role"
                    name="role"
                    value={inputs.role}
                    onChange={handleInput}
                    className="mt-1 block w-full rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm focus:border-blue-600 focus:ring-blue-600 "
                  >
                    <option value="citizen">Citizen</option>
                    <option value="lawyer">Lawyer</option>
                    <option value="ngo">NGO</option>
                  </select>
                </div>

                {(inputs.role === "lawyer" || inputs.role === "ngo") && (
                  <div>
                    <label
                      htmlFor="referenceId"
                      className="block text-sm font-medium text-gray-700"
                    >
                      {inputs.role === "lawyer" ? "Bar Registration ID" : "Registration Number"}
                      <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                    </label>
                    <input
                      id="referenceId"
                      name="referenceId"
                      type="text"
                      className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                      placeholder={
                        inputs.role === "lawyer"
                          ? "Enter your Bar Registration ID to claim your profile"
                          : "Enter your NGO Registration Number to claim your profile"
                      }
                      value={inputs.referenceId}
                      onChange={handleInput}
                    />
                    <p className="mt-1 text-xs text-gray-500">
                      {inputs.role === "lawyer"
                        ? "If you're registered in our directory, enter your Bar Registration ID to auto-approve and pre-fill your profile."
                        : "If you're registered in our directory, enter your Registration Number to auto-approve and pre-fill your profile."}
                    </p>
                  </div>
                )}

                {/* Next button for Step 1 (only for lawyer/ngo) */}
                {(inputs.role === "lawyer" || inputs.role === "ngo") && currentStep === 1 && (
                  <button
                    type="button"
                    onClick={nextStep}
                    className="mt-2 w-full inline-flex items-center justify-center rounded-lg px-4 py-2.5 text-sm font-semibold text-white shadow-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 bg-blue-800 hover:bg-blue-900"
                  >
                    Next: Profile Details →
                  </button>
                )}
                  </>
                )}

                {/* Step 2: Profile-specific fields for Lawyer/NGO */}
                {((inputs.role === "lawyer" || inputs.role === "ngo") && currentStep === 2) && (
                  <>
                {/* Lawyer-specific fields */}
                {inputs.role === "lawyer" && (
                  <>
                    <div>
                      <label
                        htmlFor="barRegistrationNo"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Bar Registration Number
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="barRegistrationNo"
                        name="barRegistrationNo"
                        type="text"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="Enter your bar registration number"
                        value={inputs.barRegistrationNo}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="specialization"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Specialization
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="specialization"
                        name="specialization"
                        type="text"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="e.g., Criminal Law, Family Law, Corporate Law"
                        value={inputs.specialization}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="experienceYears"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Years of Experience
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="experienceYears"
                        name="experienceYears"
                        type="number"
                        min="0"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="Enter years of experience"
                        value={inputs.experienceYears}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="city"
                        className="block text-sm font-medium text-gray-700"
                      >
                        City
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="city"
                        name="city"
                        type="text"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="Enter your city"
                        value={inputs.city}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="bio"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Bio
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <textarea
                        id="bio"
                        name="bio"
                        rows="3"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="Tell us about yourself and your practice"
                        value={inputs.bio}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="language"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Languages
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="language"
                        name="language"
                        type="text"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="e.g., English, Hindi, Tamil"
                        value={inputs.language}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="contactInfo"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Contact Information
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="contactInfo"
                        name="contactInfo"
                        type="text"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="Phone number or additional contact details"
                        value={inputs.contactInfo}
                        onChange={handleInput}
                      />
                    </div>
                  </>
                )}

                {/* NGO-specific fields */}
                {inputs.role === "ngo" && (
                  <>
                    <div>
                      <label
                        htmlFor="ngoName"
                        className="block text-sm font-medium text-gray-700"
                      >
                        NGO/Organization Name
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="ngoName"
                        name="ngoName"
                        type="text"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="Enter your organization name"
                        value={inputs.ngoName}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="registrationNo"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Registration Number
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="registrationNo"
                        name="registrationNo"
                        type="text"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="Enter your organization registration number"
                        value={inputs.registrationNo}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="city"
                        className="block text-sm font-medium text-gray-700"
                      >
                        City
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="city"
                        name="city"
                        type="text"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="Enter your city"
                        value={inputs.city}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="website"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Website
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="website"
                        name="website"
                        type="url"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="https://example.com"
                        value={inputs.website}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="description"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Description
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <textarea
                        id="description"
                        name="description"
                        rows="3"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="Describe your organization and its mission"
                        value={inputs.description}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="language"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Languages
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="language"
                        name="language"
                        type="text"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="e.g., English, Hindi, Tamil"
                        value={inputs.language}
                        onChange={handleInput}
                      />
                    </div>

                    <div>
                      <label
                        htmlFor="contactInfo"
                        className="block text-sm font-medium text-gray-700"
                      >
                        Contact Information
                        <span className="text-gray-500 font-normal ml-1">(Optional)</span>
                      </label>
                      <input
                        id="contactInfo"
                        name="contactInfo"
                        type="text"
                        className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-400 focus:border-blue-600 focus:ring-blue-600"
                        placeholder="Phone number or additional contact details"
                        value={inputs.contactInfo}
                        onChange={handleInput}
                      />
                    </div>
                  </>
                )}
                  </>
                )}

                {/* Terms & Conditions and Submit - Show for citizens or step 2 for lawyer/ngo */}
                {(inputs.role === "citizen" || currentStep === 2) && (
                  <>
                {/* Back button for Step 2 */}
                {(inputs.role === "lawyer" || inputs.role === "ngo") && currentStep === 2 && (
                  <button
                    type="button"
                    onClick={prevStep}
                    className="mt-2 w-full inline-flex items-center justify-center rounded-lg px-4 py-2.5 text-sm font-semibold text-gray-700 bg-gray-100 hover:bg-gray-200 shadow-md focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-1"
                  >
                    ← Back to Basic Info
                  </button>
                )}

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
                  disabled={loading || !acceptedTerms}
                  className={`mt-2 w-full inline-flex items-center justify-center rounded-lg px-4 py-2.5 text-sm font-semibold text-white shadow-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 ${
                    loading || !acceptedTerms
                      ? "bg-gray-400 cursor-not-allowed"
                      : "bg-blue-800 hover:bg-blue-900"
                  }`}
                >
                  {loading ? "Creating account..." : "Sign Up"}
                </button>
                  </>
                )}
              </form>

              {/* Social sign-in */}
              <div className="mt-4">
                <div className="relative mb-3">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-gray-200" />
                  </div>
                  <div className="relative flex justify-center text-xs uppercase">
                    <span className="bg-white px-3 text-gray-400">OR</span>
                  </div>
                </div>

                <div className="flex justify-center mx-auto">
                  <div className="w-1/2">
                    <button
                      type="button"
                      onClick={handleGoogle}
                      disabled={!acceptedTerms}
                      className={`w-full inline-flex items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-3 py-2 text-xs font-medium text-gray-700 shadow-sm ${
                        acceptedTerms ? "hover:bg-gray-50" : "opacity-50 cursor-not-allowed"
                      }`}
                    >
                      <img src={googleLogo} alt="Google logo" className="h-4 w-4" />
                      <span>Google</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    {showRoleModal && (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
        <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Continue with Google</h3>
          <p className="text-sm text-gray-600 mb-4">Choose your role to finish signup.</p>
          <div className="grid gap-3">
            <button
              type="button"
              onClick={() => handleRoleSelect("CITIZEN")}
              className="w-full rounded-lg border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-800 hover:border-blue-600 hover:text-blue-700"
            >
              Citizen
            </button>
            <button
              type="button"
              onClick={() => handleRoleSelect("LAWYER")}
              className="w-full rounded-lg border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-800 hover:border-blue-600 hover:text-blue-700"
            >
              Lawyer
            </button>
            <button
              type="button"
              onClick={() => handleRoleSelect("NGO")}
              className="w-full rounded-lg border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-800 hover:border-blue-600 hover:text-blue-700"
            >
              NGO
            </button>
          </div>
          <button
            type="button"
            onClick={() => setShowRoleModal(false)}
            className="mt-4 w-full rounded-lg px-4 py-2 text-sm font-semibold text-gray-600 hover:text-gray-800"
          >
            Cancel
          </button>
        </div>
      </div>
    )}
    </>
  );
}
