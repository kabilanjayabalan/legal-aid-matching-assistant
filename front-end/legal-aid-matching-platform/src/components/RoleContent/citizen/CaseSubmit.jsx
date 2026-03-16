import { useState, useEffect ,useRef } from "react";
import axios from "axios";
import { useAlert } from "../../../context/AlertContext";



const EXPERTISE_TAGS_BY_CATEGORY = {
  FAMILY: [
    "Divorce",
    "Child Custody",
    "Maintenance / Alimony",
    "Domestic Violence",
    "Adoption",
    "Marriage Registration"
  ],

  PROPERTY: [
    "Land Dispute",
    "Property Ownership Issues",
    "Illegal Encroachment",
    "Inheritance",
    "Sale Deed Issues",
    "Tenancy Disputes"
  ],

  EMPLOYMENT: [
    "Wrongful Termination",
    "Unpaid Wages",
    "Workplace Harassment",
    "Employment Contract Disputes",
    "Industrial Disputes"
  ],

  CRIMINAL: [
    "Fraud",
    "Cyber Crime",
    "Theft",
    "Assault",
    "Bail Matters",
    "FIR Registration Issues"
  ],

  CIVIL: [
    "Contract Dispute",
    "Recovery of Money",
    "Consumer Complaints",
    "Civil Injunction",
    "Compensation Claims"
  ]
};


export default function CaseSubmission() {
  const [step, setStep] = useState(1);
  const { showAlert } = useAlert();
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    category: "",
    location: "",
    city: "",
    latitude: "",
    longitude: "",
    expertiseTags: [],
    preferredLanguage: "",
    parties: "",
    evidenceFiles: [],
    isUrgent: false,
    contactInfo: "",
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [validationErrors, setValidationErrors] = useState({});
  const [citySuggestions, setCitySuggestions] = useState([]);
  const [citySelected, setCitySelected] = useState(false);
  const [usingCurrentLocation, setUsingCurrentLocation] = useState(false);
  const debounceRef = useRef(null);

  // Regex patterns for validation
  const validationPatterns = {
    title: /^[a-zA-Z0-9\s.,'-]{5,100}$/, // 5-100 chars, alphanumeric with basic punctuation
    description: /^[\s\S]{20,1000}$/, // 20-1000 chars, any characters
    location: /^[a-zA-Z\s,.-]{3,100}$/, // 3-100 chars, letters and basic punctuation
    contactInfo: {
      phone: /^[6-9]\d{9}$/, // Indian phone number starting with 6-9, exactly 10 digits
      email: /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/ // Valid email format
    },
    parties: /^[\s\S]{10,500}$/ // 10-500 chars for parties description
  };

  const availableTags =
  EXPERTISE_TAGS_BY_CATEGORY[formData.category] || [];

  /* ================= LOAD DRAFT ================= */
  useEffect(() => {
    document.title = "Case Submission | Legal Aid";

    const savedDraft = localStorage.getItem("caseDraft");
    if (savedDraft) {
      try {
        setFormData(JSON.parse(savedDraft));
      } catch {
        localStorage.removeItem("caseDraft");
      }
    }
  }, []);

  /* ================= HELPERS ================= */
  const update = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    // Clear validation error for this field when user starts typing
    if (validationErrors[field]) {
      setValidationErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  // Validation function for each step
  const validateStep = (stepNumber) => {
    const errors = {};

    if (stepNumber === 1) {
      // Validate title
      if (!formData.title.trim()) {
        errors.title = "Case title is required";
      } else if (!validationPatterns.title.test(formData.title)) {
        errors.title = "Title must be 5-100 characters with letters, numbers, and basic punctuation only";
      }

      // Validate description
      if (!formData.description.trim()) {
        errors.description = "Case description is required";
      } else if (!validationPatterns.description.test(formData.description)) {
        errors.description = "Description must be 20-1000 characters";
      }

      // Validate category
      if (!formData.category) {
        errors.category = "Case category is required";
      }

      // Validate contact info
      if (!formData.contactInfo.trim()) {
        errors.contactInfo = "Contact information is required";
      } else {
        const isValidPhone = validationPatterns.contactInfo.phone.test(formData.contactInfo);
        const isValidEmail = validationPatterns.contactInfo.email.test(formData.contactInfo);

        if (!isValidPhone && !isValidEmail) {
          errors.contactInfo = "Enter a valid email or 10-digit phone number starting with 6-9";
        }
      }
    }

    if (stepNumber === 2) {
      // Validate location
      if (!formData.location.trim()) {
        errors.location = "Case location is required";
      } else if (!validationPatterns.location.test(formData.location)) {
        errors.location = "Location must be 3-100 characters with letters and basic punctuation";
      }

      // City is optional but nice to have
      if (!formData.city.trim()) {
        errors.city = "City selection is recommended for better matching";
      }
    }

    return errors;
  };

  const next = () => {
    const errors = validateStep(step);

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      showAlert("Please fill the important details before continuing", "error");
      return;
    }

    setValidationErrors({});
    setStep((s) => Math.min(s + 1, 4));
  };

  const prev = () => {
    setValidationErrors({});
    setStep((s) => Math.max(s - 1, 1));
  };

  const handleFileUpload = (files) => {
    const maxSize = 10 * 1024 * 1024; // 10MB
    const validFiles = [];
    const oversized = [];

    files.forEach((file) => {
      if (file.size > maxSize) {
        oversized.push(file);
      } else {
        validFiles.push(file);
      }
    });

    if (oversized.length > 0) {
      showAlert(
        `File(s) too large: ${oversized.map((f) => f.name).join(", ")}. Max 10MB per file.`,
        "error"
      );
    }

    if (validFiles.length > 0) {
      update("evidenceFiles", [...formData.evidenceFiles, ...validFiles]);
    }
  };
  const searchCity = (query) => {
  if (!query || query.length < 3) {
    setCitySuggestions([]);
    return;
  }

  if (debounceRef.current) clearTimeout(debounceRef.current);

  debounceRef.current = setTimeout(async () => {
    const res = await fetch(
      `http://localhost:8080/api/location/search?q=${encodeURIComponent(query)}`
    );
    const data = await res.json();
    setCitySuggestions(data);
  }, 400);
};

const fetchLatLng = async (query) => {
  if (!query || query.length < 3) return;

  const res = await fetch(
    `http://localhost:8080/api/location/search?q=${encodeURIComponent(query)}`
  );
  const data = await res.json();

  if (data.length > 0) {
    setFormData((prev) => ({
      ...prev,
      city: data[0].display_name,
      latitude: data[0].lat,
      longitude: data[0].lon,
    }));
  }
};

const reverseGeocode = async (lat, lon) => {
  const res = await fetch(
    `http://localhost:8080/api/location/reverse?lat=${lat}&lon=${lon}`
  );
  const data = await res.json();

  if (data?.display_name) {
    setFormData((prev) => ({
      ...prev,
      city: data.display_name,
    }));
  }
};


  /* ================= SAVE DRAFT ================= */
  const saveDraft = () => {
    localStorage.setItem("caseDraft", JSON.stringify(formData));
    alert("Draft saved");
  };

  /* ================= SUBMIT ================= */
  const submitCase = async () => {
    setError("");

    // Validate all required fields
    if (
      !formData.title ||
      !formData.description ||
      !formData.category ||
      !formData.location ||
      !formData.contactInfo
    ) {
      setError("All required fields must be filled");
      showAlert("Please fill all required fields", "error");
      return;
    }

    // Validate with regex patterns
    if (!validationPatterns.title.test(formData.title)) {
      setError("Invalid case title format");
      showAlert("Case title must be 5-100 characters with valid characters", "error");
      return;
    }

    if (!validationPatterns.description.test(formData.description)) {
      setError("Invalid case description format");
      showAlert("Case description must be 20-1000 characters", "error");
      return;
    }

    if (!validationPatterns.location.test(formData.location)) {
      setError("Invalid location format");
      showAlert("Location must be 3-100 characters", "error");
      return;
    }

    // Validate Contact Info (Phone Number or Email)
    const isValidPhone = validationPatterns.contactInfo.phone.test(formData.contactInfo);
    const isValidEmail = validationPatterns.contactInfo.email.test(formData.contactInfo);

    if (!isValidPhone && !isValidEmail) {
        setError("Contact info must be a valid email or a 10-digit phone number starting with 6-9.");
        showAlert("Please enter valid contact information", "error");
        return;
    }

    try {
      setLoading(true);

      const formDataToSend = new FormData();
      formDataToSend.append("title", formData.title);
      formDataToSend.append("description", formData.description);
      formDataToSend.append("category", formData.category);
      formDataToSend.append("location", formData.location);
      formDataToSend.append("contactInfo", formData.contactInfo);
      formDataToSend.append("isUrgent", formData.isUrgent.toString());

      if (formData.expertiseTags.length > 0) {
        formData.expertiseTags.forEach(tag => formDataToSend.append("expertiseTags", tag));
      }
      if (formData.preferredLanguage) {
        formDataToSend.append("preferredLanguage", formData.preferredLanguage);
      }
      if (formData.parties) {
        formDataToSend.append("parties", formData.parties);
      }
      if( formData.city) {
        formDataToSend.append("city", formData.city);
      }
      if (formData.latitude) {
        formDataToSend.append("latitude", formData.latitude);
      }
      if (formData.longitude) {
        formDataToSend.append("longitude", formData.longitude);
      }


      // Append files
      if (formData.evidenceFiles.length > 0) {
        formData.evidenceFiles.forEach(file => {
          formDataToSend.append("evidenceFiles", file);
        });
      }

      console.log("Submitting form data");

      await axios.post(
        "http://localhost:8080/cases",
        formDataToSend,
        {
          headers: {
            Authorization: `Bearer ${sessionStorage.getItem("accessToken")}`,
            "Content-Type": "multipart/form-data"
          }
        }
      );
    showAlert("Case submitted successfully");
    sessionStorage.removeItem("caseDraft");
    // reset form
    setFormData({
      title: "",
      description: "",
      category: "",
      location: "",
      city: "",
      latitude: "",
      longitude: "",
      expertiseTags: [],
      preferredLanguage: "",
      parties: "",
      evidenceFiles: [],
      isUrgent: false,
      contactInfo: "",
      });

  // reset stepper
    setStep(1);
  } catch (err) {
    console.error(err.response?.data || err.message);
    setError("Failed to submit case");
  } finally {
    setLoading(false);
  }
};

  return (
    <div className="p-4 max-w-5xl mx-auto">
      <h1 className="text-2xl font-semibold mb-6">
        Case Submission Interface
      </h1>

      {/* ================= STEPPER ================= */}
      <div className="mb-6">
        <div className="flex justify-between items-center mb-2">
          {["Details", "Location & Parties", "Evidence", "Review"].map(
            (label, index) => {
              const stepNum = index + 1;
              return (
                <div key={label} className="flex flex-col items-center w-full">
                  <div
                    className={`w-8 h-8 flex items-center justify-center rounded-full text-sm font-semibold
                      ${
                        step >= stepNum
                          ? "bg-blue-950 text-white"
                          : "bg-gray-200 text-gray-600"
                      }`}
                  >
                    {stepNum}
                  </div>
                  <span className="text-sm  h-10 flex items-center text-gray-700">{label}</span>
                </div>
              );
            }
          )}
        </div>

        <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
          <div
            className="h-full bg-blue-950 transition-all"
            style={{ width: `${(step / 4) * 100}%` }}
          />
        </div>
      </div>

      {/* ================= CONTENT ================= */}
      <div className="bg-white border rounded-xl p-4 shadow-sm">

        {/* STEP 1 */}
        {step === 1 && (
          <>
            <h2 className="text-xl font-semibold mb-3">Case Details</h2>

            <label className="block mb-1 text-sm font-medium">
              Case Title <span className="text-red-500">*</span>
            </label>
            <input
              value={formData.title}
              onChange={(e) => update("title", e.target.value)}
              className={`w-full border rounded-md p-2 mb-1 text-sm ${
                validationErrors.title ? "border-red-500" : ""
              }`}
              placeholder="Enter case title (5-100 characters)"
            />
            {validationErrors.title && (
              <p className="text-red-500 text-xs mb-3">{validationErrors.title}</p>
            )}
            {!validationErrors.title && <div className="mb-3"></div>}

            <label className="block mb-1 text-sm font-medium">
              Case Summary (plain language) <span className="text-red-500">*</span>
            </label>
            <textarea
              rows={5}
              value={formData.description}
              onChange={(e) => update("description", e.target.value)}
              className={`w-full border rounded-md p-2 mb-1 text-sm ${
                validationErrors.description ? "border-red-500" : ""
              }`}
              placeholder="Briefly describe your situation (20-1000 characters)"
            />
            {validationErrors.description && (
              <p className="text-red-500 text-xs mb-3">{validationErrors.description}</p>
            )}
            {!validationErrors.description && <div className="mb-3"></div>}

            <label className="block mb-1 text-sm font-medium">
              Case Type / Category <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.category}
              onChange={(e) => {
                update("category", e.target.value);
                update("expertiseTags", []); // reset old tags
              }}
              className={`w-full border rounded-lg p-2 mb-1 text-sm ${
                validationErrors.category ? "border-red-500" : ""
              }`}
            >
              <option value="">Select case type</option>
              <option value="FAMILY">Family</option>
              <option value="PROPERTY">Property</option>
              <option value="EMPLOYMENT">Employment</option>
              <option value="CRIMINAL">Criminal</option>
              <option value="CIVIL">Civil</option>
            </select>
            {validationErrors.category && (
              <p className="text-red-500 text-xs mb-3">{validationErrors.category}</p>
            )}
            {!validationErrors.category && <div className="mb-3"></div>}

            <label className="block mb-2 text-sm font-medium">
              Expertise Tags
            </label>

            {availableTags.length === 0 ? (
              <div className="mb-4 rounded-xl border border-dashed border-gray-300 bg-gray-50 p-2 text-sm text-gray-600">
                <p>Select a case type above to unlock relevant expertise tags</p>
              </div>
            ) : (
              <div className="flex flex-wrap gap-2 mb-3">
                {availableTags.map((tag) => {
                  const selected = formData.expertiseTags.includes(tag);
                  return (
                  <button
                    key={tag}
                    type="button"
                    onClick={() =>
                    update(
                      "expertiseTags",
                      selected
                      ? formData.expertiseTags.filter((t) => t !== tag)
                      : [...formData.expertiseTags, tag]
                    )
                } 
                className={`px-3 py-1 rounded-full border text-sm ${
                  selected
                    ? "bg-blue-950 text-white border-blue-600"
                    : "bg-gray-100 border-gray-300"
                }`}
              >
                {tag}
              </button>
            );
            })}
          </div>
        )}
            <label className="block mb-1 text-sm font-medium">
              Preferred Language
            </label>
            <select
              value={formData.preferredLanguage}
              onChange={(e) => update("preferredLanguage", e.target.value)}
              className="w-full border rounded-lg p-2 mb-3 text-sm"
            >
              <option value="">Select preferred language</option>
              <option value="English">English</option>
              <option value="Hindi">Hindi</option>
              <option value="Telugu">Telugu</option>
              <option value="Tamil">Tamil</option>
              <option value="Kannada">Kannada</option>
            </select>
            <label className="block mb-1 text-sm font-medium">
              Contact Information <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.contactInfo}
              onChange={(e) => {
                const val = e.target.value;
                // If empty, allow
                if (val === "") {
                    update("contactInfo", val);
                    return;
                }

                // If it looks like an email (contains letters or @), allow normal typing
                if (/[a-zA-Z@]/.test(val)) {
                    update("contactInfo", val);
                    return;
                }

                // If it's purely digits (phone number logic)
                if (/^\d+$/.test(val)) {
                    // Check first digit constraint
                    const firstDigit = parseInt(val.charAt(0));
                    if (firstDigit < 6) {
                        // If first digit is invalid (0-5), don't update state (reject input)
                        return;
                    }
                    
                    // Check length constraint
                    if (val.length <= 10) {
                        update("contactInfo", val);
                    }
                }
              }}
              className={`w-full border rounded-lg p-2 mb-1 text-sm ${
                validationErrors.contactInfo ? "border-red-500" : ""
              }`}
              placeholder="Phone number or email"
            />
            {validationErrors.contactInfo && (
              <p className="text-red-500 text-xs mb-3">{validationErrors.contactInfo}</p>
            )}
            {!validationErrors.contactInfo && <div className="mb-3"></div>}
            <label className="flex items-center gap-2 mb-4">
              <input
                type="checkbox"
                checked={formData.isUrgent}
                onChange={(e) => update("isUrgent", e.target.checked)}
              />
            <span className="text-sm">Mark as urgent</span>
            </label>
          </>
        )}

        {/* STEP 2 */}
        {step === 2 && (
          <>
            <h2 className="text-xl font-semibold mb-3">
              Location & Parties
            </h2>

            <label className="block mb-1 text-sm font-medium">
              Case Location <span className="text-red-500">*</span>
            </label>
            <input
              value={formData.location}
              onChange={(e) => update("location", e.target.value)}
              className={`w-full border rounded-lg p-2 mb-1 text-sm ${
                validationErrors.location ? "border-red-500" : ""
              }`}
              placeholder="City / District (e.g., Mumbai, Delhi)"
            />
            {validationErrors.location && (
              <p className="text-red-500 text-xs mb-3">{validationErrors.location}</p>
            )}
            {!validationErrors.location && <div className="mb-3"></div>}
            <label className="block mb-1 text-sm font-medium">
              Select Your City <span className="text-yellow-600">(Recommended)</span>
            </label>

            <div className="relative mb-2">
              <input
                value={formData.city}
                onChange={(e) => {
                update("city", e.target.value);
                searchCity(e.target.value);
                setCitySelected(false);
                setUsingCurrentLocation(false);
                }}
                onBlur={() => {
                  setTimeout(() => {
                    if (!citySelected && !usingCurrentLocation && formData.city.length >= 3) {
                      fetchLatLng(formData.city);
                    }
                  setCitySuggestions([]);
                  }, 300);
                }}
                className={`w-full border rounded-lg p-2 text-sm ${
                  validationErrors.city ? "border-yellow-400" : ""
                }`}
                placeholder="Start typing city name"
              />
            {validationErrors.city && (
              <p className="text-yellow-600 text-xs mt-1">{validationErrors.city}</p>
            )}

            {citySuggestions.length > 0 && (
              <ul className="absolute z-50 w-full bg-white border rounded-lg shadow max-h-48 overflow-auto">
                {citySuggestions.map((c, i) => (
                  <li
                    key={i}
                    className="px-3 py-2 hover:bg-blue-50 cursor-pointer"
                    onMouseDown={(e) => {
                      e.preventDefault();
                      setCitySelected(true);
                      setFormData((prev) => ({
                        ...prev,
                        city: c.display_name,
                        latitude: c.lat,
                        longitude: c.lon,
                    }));
                    setCitySuggestions([]);
                  }}
              >
                {c.display_name}
              </li>
            ))}
          </ul>
          )}
          </div>

          {/* OR USE MY LOCATION */}
          <div className="flex items-center gap-2 mb-3">
            <span className="text-xs text-gray-400">OR</span>
              <button
                type="button"
                className="text-xs px-3 py-1 bg-blue-950 text-white rounded"
                onClick={() => {
                navigator.geolocation.getCurrentPosition(
                  (pos) => {
                    setUsingCurrentLocation(true);
                    setCitySelected(true);

                    update("latitude", pos.coords.latitude);
                    update("longitude", pos.coords.longitude);

                    reverseGeocode(
                      pos.coords.latitude,
                      pos.coords.longitude
                    );
                  },
                  () => alert("Location permission denied")
                );
                }}
            >
              📍 Use My Location
            </button>
            </div>

            {/* READ-ONLY LAT / LNG */}
            <div className="bg-gray-100 p-3 rounded-lg text-sm pb-1 mb-3">
              <p><strong>Latitude:</strong> {formData.latitude || "—"}</p>
              <p><strong>Longitude:</strong> {formData.longitude || "—"}</p>
            </div>


            <label className="block mb-1 text-sm font-medium">
              Parties Involved
            </label>
            <textarea
              rows={4}
              value={formData.parties}
              onChange={(e) => update("parties", e.target.value)}
              className="w-full border rounded-md p-2 text-sm"
              placeholder="Describe the people or organizations involved"
            />
          </>
        )}

        {/* STEP 3 */}
        {step === 3 && (
          <>
            <h2 className="text-xl font-semibold mb-4">Evidence</h2>

            <label className="block mb-2 text-sm font-medium">
              Upload Supporting Documents
            </label>
            
            {/* Drag and Drop Zone */}
            <div
              onDragOver={(e) => {
                e.preventDefault();
                e.stopPropagation();
                e.currentTarget.classList.add("border-blue-500", "bg-blue-50");
              }}
              onDragLeave={(e) => {
                e.preventDefault();
                e.stopPropagation();
                e.currentTarget.classList.remove("border-blue-500", "bg-blue-50");
              }}
              onDrop={(e) => {
                e.preventDefault();
                e.stopPropagation();
                e.currentTarget.classList.remove("border-blue-500", "bg-blue-50");
                
                const files = Array.from(e.dataTransfer.files);
                handleFileUpload(files);
              }}
              className="border-2 border-dashed border-gray-300 rounded-xl p-8 text-center transition-all hover:border-blue-400 hover:bg-gray-50 cursor-pointer"
              onClick={() => document.getElementById("file-upload-input")?.click()}
            >
              <input
                id="file-upload-input"
                type="file"
                multiple
                accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                onChange={(e) => {
                  const files = Array.from(e.target.files);
                  handleFileUpload(files);
                  e.target.value = ""; // Reset input to allow re-uploading same file
                }}
                className="hidden"
              />
              
              <div className="flex flex-col items-center">
                <svg
                  className="w-12 h-12 text-gray-400 mb-3"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                  />
                </svg>
                <p className="text-gray-700 font-medium mb-1">
                  Drag and drop files here, or click to browse
                </p>
                <p className="text-sm text-gray-500">
                  Supported formats: PDF, DOC, DOCX, JPG, JPEG, PNG (Max 10MB per file)
                </p>
              </div>
            </div>

            {/* File List */}
            {formData.evidenceFiles.length > 0 && (
              <div className="mt-4 space-y-2">
                <p className="text-sm font-medium text-gray-700 mb-2">
                  Uploaded Files ({formData.evidenceFiles.length})
                </p>
                <div className="space-y-2">
                  {formData.evidenceFiles.map((file, index) => (
                    <div
                      key={index}
                      className="flex items-center justify-between p-3 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition-colors"
                    >
                      <div className="flex items-center gap-3 flex-1 min-w-0">
                        <div className="flex-shrink-0">
                          {file.type.startsWith("image/") ? (
                            <svg className="w-8 h-8 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                          ) : (
                            <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                            </svg>
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-gray-900 truncate">
                            {file.name}
                          </p>
                          <p className="text-xs text-gray-500">
                            {(file.size / 1024 / 1024).toFixed(2)} MB
                          </p>
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          const newFiles = formData.evidenceFiles.filter((_, i) => i !== index);
                          update("evidenceFiles", newFiles);
                        }}
                        className="ml-3 p-1.5 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors flex-shrink-0"
                        aria-label="Remove file"
                      >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}

        {/* STEP 4 */}
        {step === 4 && (
        <>
          <h2 className="text-2xl font-semibold mb-3 text-gray-800">
            Review & Submit
          </h2>

          <div className="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden">

          {/* Section: Case Summary */}
          <div className="px-3 py-3 border-b bg-gray-50">
            <h3 className="text-lg font-medium text-gray-700">
              Case Summary
            </h3>
          </div>

          <div className="py-3 px-4  grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-4 text-sm">

            <div>
              <p className="text-gray-500 font-medium">Title</p>
              <p className="text-gray-900">{formData.title || "—"}</p>
            </div>

            <div>
              <p className="text-gray-500 font-medium">Category</p>
              <p className="text-gray-900">{formData.category || "—"}</p>
            </div>

            <div className="md:col-span-2">
              <p className="text-gray-500 font-medium">Description</p>
              <p className="text-gray-900 leading-relaxed">
                {formData.description || "—"}
              </p>
            </div>

            <div>
              <p className="text-gray-500 font-medium">Location</p>
              <p className="text-gray-900">{formData.location || "—"}</p>
            </div>

            <div>
              <p className="text-gray-500 font-medium">City</p>
              <p className="text-gray-900">{formData.city || "—"}</p>
            </div>

            <div>
              <p className="text-gray-500 font-medium">Preferred Language</p>
              <p className="text-gray-900">{formData.preferredLanguage || "—"}</p>
            </div>

            <div>
              <p className="text-gray-500 font-medium">Urgency</p>
                <span
                  className={`inline-block px-2 py-1 rounded-full text-xs font-medium ${
                  formData.isUrgent
                    ? "bg-red-100 text-red-700"
                    : "bg-green-100 text-green-700"
                  }`}
                >{formData.isUrgent ? "Urgent" : "Not Urgent"}
                </span>
            </div>

            <div className="md:col-span-2">
              <p className="text-gray-500 font-medium">Expertise Tags</p>
              <p className="text-gray-900">
                {formData.expertiseTags.length > 0
                  ? formData.expertiseTags.join(", ")
                  : "—"}
              </p>
            </div>

            <div>
              <p className="text-gray-500 font-medium">Parties Involved</p>
              <p className="text-gray-900">{formData.parties || "—"}</p>
            </div>

            <div>
              <p className="text-gray-500 font-medium">Contact Information</p>
              <p className="text-gray-900">{formData.contactInfo || "—"}</p>
            </div>

          </div>

          {/* Section: Evidence */}
          <div className="px-3 py-2 border-t bg-gray-50">
            <h3 className="text-lg font-medium text-gray-700">
              Evidence Files
            </h3>
          </div>

          <div className="p-6">
            {formData.evidenceFiles.length > 0 ? (
              <ul className="list-disc list-inside text-sm text-gray-800 space-y-1">
                {formData.evidenceFiles.map((file, i) => (
                  <li key={i}>{file.name}</li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-gray-500">No files uploaded</p>
            )}
          </div>
          </div>
        </>
        )}

        {error && <p className="text-red-600 mt-3">{error}</p>}
        {/* ================= ACTION BAR ================= */}
        <div className="flex flex-col gap-4 sm:flex-row sm:justify-between sm:items-center mt-6">
          <div className="flex flex-col sm:flex-row gap-3">
            <button
              onClick={prev}
              disabled={step === 1}
              className="w-full sm:w-auto px-5 py-2 border rounded-lg disabled:opacity-50"
              >
              Back
            </button>

            <button
              onClick={saveDraft}
              type="button"
              className="w-full sm:w-auto px-5 py-2 bg-gray-100 border rounded-lg"
              >
              Save Draft
            </button>
          </div>

          {step <  4 ? (
            <button
              onClick={next}
              className="w-full sm:w-auto px-5 py-2 bg-blue-950 text-white rounded-lg"
              >
              Continue
            </button>
          ) : (
            <button
              onClick={submitCase}
              disabled={loading}
              className="w-full sm:w-auto px-5 py-2 bg-blue-950 text-white rounded-lg disabled:opacity-50"
            >
              {loading ? "Submitting..." : "Submit Case"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
