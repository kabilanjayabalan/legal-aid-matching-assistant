import { useEffect, useState } from "react";
import { MdDelete } from "react-icons/md";
import api from "../../../services/api";
import * as XLSX from "xlsx";

const NGO_FIELDS = [
    "orgName",
    "registrationNumber",
    "focusArea",
    "city",
    "contactNumber",
    "email",
    "website"
  ];

  const LAWYER_FIELDS = [
    "fullName",
    "barRegistrationId",
    "specialization",
    "city",
    "contactNumber",
    "email",
    "type"
  ];

/* ---------- Reusable Card Section ---------- */
const Section = ({ title, desc, children }) => (
  <div className="bg-white rounded-xl border shadow-sm p-6 space-y-4">
    <div>
      <h2 className="text-lg font-semibold">{title}</h2>
      {desc && <p className="text-sm text-gray-500">{desc}</p>}
    </div>
    {children}
  </div>
);

export default function DirectoryIngestion() {
  /* ===================== STATE ===================== */
  const [source, setSource] = useState("NGO_DARPAN");
  const [useOAuth, setUseOAuth] = useState(false);
  const [apiKey, setApiKey] = useState("");
  const [mappings, setMappings] = useState([{ external: "", internal: "" }]);
  const [conflict, setConflict] = useState("SKIP");
  const [schedule, setSchedule] = useState("MANUAL");
  const [previewData, setPreviewData] = useState([]);
  const [summary, setSummary] = useState(null);
  const [csvText, setCsvText] = useState("");
  const [csvFile, setCsvFile] = useState(null);
  const [apiEndpoint, setApiEndpoint] = useState("");
  /* ===================== DERIVED DATA ===================== */
  const INTERNAL_FIELDS =
    source === "CSV"
      ? Array.from(new Set([...NGO_FIELDS, ...LAWYER_FIELDS]))
      : [];


  /* ===================== FILE PROCESSING ===================== */
  const loadFilePreviewWithStatus = async () => {
    const parsedRows = parseFileText(csvText);
    // 🔑 apply user-defined mappings
    const mappedRows = applyFieldMappings(parsedRows, mappings);
    // 2️⃣ Normalize → preview format
    const previewRows = mapRowsToPreview(mappedRows);

    // 3️⃣ Split by type for status check
    const fileNgos = previewRows.filter(r => r.type === "NGO");
    const fileLawyers = previewRows.filter(
      r => r.type === "Lawyer" || r.type === "Law Firm"
    );
    // 4️⃣ Ask backend for status
    const ngosWithStatus = await checkNgoStatus(fileNgos);
    const lawyersWithStatus = await checkLawyerStatus(fileLawyers);

    // 5️⃣ Build preview rows WITH status
    setPreviewData([
      ...ngosWithStatus,
      ...lawyersWithStatus
    ]);
  };

  // Load preview data on source change
  useEffect(() => {
    if (source === "CSV" && csvText) {
      loadFilePreviewWithStatus();
    }
  }, [source,csvText,mappings]);

  /* ===================== UTILITY FUNCTIONS ===================== */
  const isMatched = (status) => status === "Matched" || status === "MATCH";
  const parseFileText = (fileText) => {
    const lines = csvText.trim().split("\n");
    const headers = lines[0].split(",").map(h => h.trim());

    return lines.slice(1).map(line => {
      const values = line.split(",");
      const obj = {};
      headers.forEach((h, i) => {
        obj[h] = values[i]?.trim() || null;
      });
      return obj;
    });
  };
  const applyFieldMappings = (rows, mappings) => {
    return rows.map(row => {
      const mappedRow = {};
      mappings.forEach(({ external, internal }) => {
        if (external && internal && row[external] !== undefined) {
          mappedRow[internal] = row[external];
        }
      });
      // Keep original fields that weren't mapped
      Object.keys(row).forEach(key => {
        if (!mappedRow[key]) {
          mappedRow[key] = row[key];
        }
      });
      return mappedRow;
    });
  };  
  const handleFileUpload = (file) => {
    if (!file) return;
    const ext = file.name.split(".").pop().toLowerCase();

    if (ext === "csv") {
      const reader = new FileReader();
      reader.onload = (e) => setCsvText(e.target.result);
      reader.readAsText(file);
      return;
    }
    if (ext === "xlsx" || ext === "xls") {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const data = new Uint8Array(e.target.result);
          const workbook = XLSX.read(data, { type: "array" });
          const sheet = workbook.Sheets[workbook.SheetNames[0]];
          const json = XLSX.utils.sheet_to_json(sheet, { defval: "" });

          if (json.length === 0) {
            alert("The Excel file appears to be empty.");
            return;
          }

          // Convert JSON → CSV-like text with proper escaping
          const headers = Object.keys(json[0]);
          const escapeCSV = (value) => {
            if (typeof value === 'string' && (value.includes(',') || value.includes('"') || value.includes('\n'))) {
              return '"' + value.replace(/"/g, '""') + '"';
            }
            return value || "";
          };

          const csv = [
            headers.join(","),
            ...json.map(row =>
              headers.map(h => escapeCSV(row[h])).join(",")
            )
          ].join("\n");

          setCsvText(csv);
        } catch (error) {
          console.error("Error reading Excel file:", error);
          alert("Error reading the Excel file. Please check the file format.");
        }
      };
      reader.readAsArrayBuffer(file);
    }
  };

  const mapRowsToPreview = (rows) => {
    return rows.map(row => {

      // ✅ NGO detection
      if (row.registrationNumber) {
        return {
          id: row.registrationNumber,
          type: "NGO",
          name: row.orgName,
          orgName: row.orgName,
          registrationNumber: row.registrationNumber,
          focusArea: row.focusArea || null,
          city: row.city || null,
          contactNumber: row.contactNumber || null,
          email: row.email || null,
          website: row.website || null
        };
      }

      // ✅ Lawyer / Law Firm detection
      if (row.barRegistrationId) {
        return {
          id: row.barRegistrationId,
          type: row.type || "Lawyer", // fallback
          name: row.fullName,
          fullName: row.fullName,
          barRegistrationId: row.barRegistrationId,
          specialization: row.specialization || null,
          city: row.city || null,
          contactNumber: row.contactNumber || null,
          email: row.email || null
        };
      }

      // ❌ Unknown row (optional safety)
      return null;
    }).filter(Boolean); // remove invalid rows
  };

  const checkLawyerStatus = async (lawyers) => {
    const ids = lawyers.map(l => l.barRegistrationId);

    const res = await api.post("/api/directory/lawyers/check-import", ids);

    const map = {};
    res.data.forEach(r => {
      map[r.barRegistrationId] = r.status;
    });

    return lawyers.map(l => ({
      ...l,
      status: map[l.barRegistrationId] || "NEW_IMPORT"
    }));
  };

  const checkNgoStatus = async (ngos) => {
    const ids = ngos.map(n => n.registrationNumber);

    const res = await api.post("/api/directory/ngos/check-import", ids);

    const map = {};
    res.data.forEach(r => {
      map[r.registrationNumber] = r.status;
    });

    return ngos.map(n => ({
      ...n,
      status: map[n.registrationNumber] || "NEW_IMPORT"
    }));
  };

  /* ===================== HANDLERS ===================== */
  // Mapping handlers
  const addMapping = () => setMappings([...mappings, { external: "", internal: "" }]);

  const updateMapping = (index, field, value) => {
    const copy = [...mappings];
    copy[index][field] = value;
    setMappings(copy);
  };

  const removeMapping = (index) => {
    setMappings(mappings.filter((_, i) => i !== index));
  };
  
  const runImport = async () => {
    try {
      console.log("RUN IMPORT CLICKED", conflict);

      const shouldSend = (item) => {
        if (conflict === "SKIP") return item.status === "NEW_IMPORT";
        return true; // UPDATE & CREATE send all
      };

      // 1️⃣ Build raw payloads (no status yet)
      const rawLawyers = previewData
        .filter(i => i.type === "Lawyer" || i.type === "Law Firm")
        .map(i => ({
          fullName: i.fullName ?? i.name,
          barRegistrationId: i.barRegistrationId ?? i.id,
          specialization: i.specialization ?? null,
          city: i.city ?? null,
          contactNumber: i.contactNumber ?? null,
          email: i.email ?? null,
          type: i.type // 🔑 keep original type
        }));

      const rawNgos = previewData
        .filter(i => i.type === "NGO")
        .map(i => ({
          orgName: i.orgName ?? i.name,
          registrationNumber: i.registrationNumber ?? i.id,
          focusArea: i.focusArea ?? null,
          city: i.city ?? null,
          contactNumber: i.contactNumber ?? null,
          email: i.email ?? null,
          website: i.website ?? null
        }));

      // 2️⃣ Ask backend which records already exist
      const lawyersWithStatus = await checkLawyerStatus(rawLawyers);
      const ngosWithStatus = await checkNgoStatus(rawNgos);

      // 3️⃣ Rebuild previewData with REAL status
      const verifiedPreview = [
        ...lawyersWithStatus.map(item => ({
          id: item.barRegistrationId,
          type: item.type,
          name: item.fullName,
          status: item.status,
          fullName: item.fullName ?? null,
          barRegistrationId: item.barRegistrationId,
          specialization: item.specialization ?? null,
          city: item.city ?? null,
          contactNumber: item.contactNumber ?? null,
          email: item.email ?? null
        })),
        ...ngosWithStatus.map(item => ({
          id: item.registrationNumber,
          type: "NGO",
          name: item.orgName,
          status: item.status,
          orgName: item.orgName ?? null,
          registrationNumber: item.registrationNumber,
          focusArea: item.focusArea ?? null,
          city: item.city ?? null,
          contactNumber: item.contactNumber ?? null,
          email: item.email ?? null,
          website: item.website ?? null
        }))
      ];

      setPreviewData(verifiedPreview);
      // 4️⃣ Bucket records by status + type (business logic only)
      const buckets = {
        matchedLawyers: [],
        matchedNgos: [],
        newLawyers: [],
        newNgos: []
      };  
      for (const r of verifiedPreview) {
        const isLawyer = r.type === "Lawyer" || r.type === "Law Firm";

        if (r.status === "MATCH") {
          if (isLawyer) buckets.matchedLawyers.push(r);
          else buckets.matchedNgos.push(r);
        }

        if (r.status === "NEW_IMPORT") {
          if (isLawyer) buckets.newLawyers.push(r);
          else buckets.newNgos.push(r);
        }
      }

      // Build lawyers and NGOs payload (USE verifiedPreview)
      let lawyersToImport = [];
      let ngosToImport = [];

      if (conflict === "SKIP") {
        lawyersToImport = buckets.newLawyers;
        ngosToImport = buckets.newNgos;
      }

      if (conflict === "UPDATE") {
        lawyersToImport = [
          ...buckets.matchedLawyers,
          ...buckets.newLawyers
        ];
        ngosToImport = [
          ...buckets.matchedNgos,
          ...buckets.newNgos
        ];
      }
      const lawyers = lawyersToImport.map(i => ({
        fullName: i.fullName,
        barRegistrationId: i.barRegistrationId,
        specialization: i.specialization,
        city: i.city,
        contactNumber: i.contactNumber,
        email: i.email
      }));

      const ngos = ngosToImport.map(i => ({
        orgName: i.orgName,
        registrationNumber: i.registrationNumber,
        focusArea: i.focusArea,
        city: i.city,
        contactNumber: i.contactNumber,
        email: i.email,
        website: i.website
      }));


      let summary = {
        imported: 0,
        updated: 0,
        failed: 0,
        lastRun: new Date().toISOString(),
      };

      // Import lawyers
      if (lawyers.length > 0) {
        const response = await api.post(
          "/api/directory/import/lawyers",
          lawyers,
          {
            params: { mode: conflict },
          }
        );

        const data = response.data;
        summary.imported += data.importedCount ?? 0;
        summary.updated += data.updatedCount ?? 0;
        summary.failed += data.errors?.length ?? 0;
      }

      // Import NGOs
      if (ngos.length > 0) {
        const response = await api.post(
          "/api/directory/import/ngos",
          ngos,
          {
            params: { mode: conflict },
          }
        );

        const data = response.data;
        summary.imported += data.importedCount ?? 0;
        summary.updated += data.updatedCount ?? 0;
        summary.failed += data.errors?.length ?? 0;
      }

      setSummary(summary);
    } catch (error) {
      console.error("Import failed:", error);
    }
  };

  // Save Job Configuration
  const saveJob = async () => {
    try {
      const config = {
        source,
        useOAuth,
        apiKey,
        mappings,
        conflict,
        schedule
      };
      await api.post('/api/directory/config', config);
      // Show success message
    } catch (error) {
      console.error("Failed to save config:", error);
    }
  };

  /* ===================== UI ===================== */

  return (
    <div className="p-6 bg-gray-50 min-h-screen space-y-6">
      {/* PAGE HEADER */}
      <div>
        <h1 className="text-2xl font-semibold">
          Directory Ingestion — Integrate external directories (NGO Darpan, Bar Council)
        </h1>
        <p className="text-sm text-gray-500">
          Manage external data sources, configure mappings, and automate directory updates.
        </p>
      </div>

      {/* 1. SELECT DATA SOURCE */}
      <Section
        title="1. Select Data Source"
        desc="Choose where your directory data will come from."
      >
        <div className="grid grid-cols-2 gap-4 text-sm p-2">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              checked={source === "NGO_DARPAN"}
              onChange={() => setSource("NGO_DARPAN")}
            />
            NGO Darpan
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              checked={source === "BAR_COUNCIL"}
              onChange={() => setSource("BAR_COUNCIL")}
            />
            Bar Council of India
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              checked={source === "CSV"}
              onChange={() => setSource("CSV")}
            />
            CSV/Excel Upload
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              checked={source === "API"}
              onChange={() => setSource("API")}
            />
            API Endpoint
          </label>
        </div>
        {/* 🔽 CONDITIONAL INPUTS */}
        {source === "CSV" && (
          <div className="mt-4">
            <label className="block text-sm mb-1 font-medium">
              Upload CSV or Excel File
            </label>
            <input
              type="file"
              accept=".csv,.xlsx,.xls"
              onChange={(e) => {
                  const file = e.target.files[0];
                  setCsvFile(file);
                  handleFileUpload(file);
                }}
              className="w-full border rounded-md px-3 py-2"
            />
            {csvFile && (
              <p className="text-xs text-gray-500 mt-1">
                Selected: {csvFile.name}
              </p>
            )}
        </div>
      )}

      {source === "API" && (
        <div className="mt-4">
          <label className="block text-sm mb-1 font-medium">
            API Endpoint URL
          </label>
          <input
            type="text"
            value={apiEndpoint}
            onChange={(e) => setApiEndpoint(e.target.value)}
            placeholder="https://api.example.com/directory"
            className="w-full border rounded-md px-3 py-2"
          />
        </div>
      )}
      </Section>

      {/* 2. CONFIGURE AUTHENTICATION */}
      <Section
        title="2. Configure Authentication (Optional)"
        desc="Provide credentials to access the external data source."
      >
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={useOAuth}
            onChange={() => setUseOAuth(!useOAuth)}
          />
          Use OAuth 2.0
        </label>

        <div>
          <label className="block text-sm mb-1">API Key</label>
          <input
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            placeholder="Enter API Key"
            className="w-full border rounded-md px-3 py-2"
          />
          <p className="text-xs text-gray-400">
            This key will be securely stored and encrypted.
          </p>
        </div>
      </Section>

      {/* 3. MAP DATA FIELDS */}
      <Section
        title="3. Map Data Fields"
        desc="Align external data fields to the platform's internal schema."
      >
        {mappings.map((m, i) => (
          <div key={i} className="mb-2 p-2 border rounded-lg bg-gray-50">
            {/* Mobile: Stack vertically, Desktop: Grid layout */}
            <div className="grid grid-cols-1 md:grid-cols-[1fr_auto_1fr_auto] gap-3 md:gap-4 items-center">
              {/* External Field Input */}
              <div className="w-full md:w-auto">
                <label className="block text-xs text-gray-600 mb-1 md:hidden">External Field</label>
                <input
                  value={m.external}
                  onChange={(e) => updateMapping(i, "external", e.target.value)}
                  placeholder="External field name"
                  className="w-full border rounded-md px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              {/* Arrow - Hidden on mobile, shown on desktop */}
              <div className="hidden md:flex justify-center items-center">
                <span className="text-gray-400 text-lg">→</span>
              </div>

              {/* Mobile arrow indicator */}
              <div className="flex md:hidden justify-center items-center py-2">
                <span className="text-gray-400 text-sm">maps to ↓</span>
              </div>

              {/* Internal Field Select */}
              <div className="w-full md:w-auto">
                <label className="block text-xs text-gray-600 mb-1 md:hidden">Internal Field</label>
                <select
                  value={m.internal}
                  onChange={(e) => updateMapping(i, "internal", e.target.value)}
                  className="w-full border rounded-md px-3 py-2 text-sm bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="">Select Internal Field</option>
                  {INTERNAL_FIELDS.map(f => (
                    <option key={f} value={f}>
                      {f}
                    </option>
                  ))}
                </select>
              </div>

              {/* Delete Button */}
              <div className="flex justify-center md:justify-end">
                <button
                  onClick={() => removeMapping(i)}
                  className="flex items-center justify-center w-8 h-8 md:w-auto md:h-auto md:px-2 md:py-1 rounded-lg text-red-600 hover:bg-red-100 transition-colors touch-manipulation"
                  aria-label="Remove mapping"
                >
                  <MdDelete size={20} className="md:w-5 md:h-5" />
                </button>
              </div>
            </div>
          </div>
        ))}

        {/* Action Buttons - Responsive layout */}
        <div className="flex flex-col sm:flex-row justify-between items-center gap-3 mt-6">
          <button
            onClick={addMapping}
            className="w-full sm:w-auto px-4 py-2 text-blue-950 text-sm font-medium hover:bg-blue-50 rounded-lg transition-colors touch-manipulation"
          >
            + Add Another Mapping
          </button>
          <button
            onClick={loadFilePreviewWithStatus}
            className="w-full sm:w-auto px-6 py-2 bg-blue-900 text-white text-sm font-medium rounded-lg hover:bg-blue-800 transition-colors touch-manipulation"
          >
            Apply Mapping & Preview
          </button>
        </div>
      </Section>

      {/* 4. DATA PREVIEW & VALIDATION */}
      <Section
        title="4. Data Preview & Validation"
        desc="Review sample rows and check for data validity after mapping."
      >
        <table className="w-full text-sm">
          <thead className="border-b text-gray-500">
            <tr>
              <th className="py-2 text-left">ID</th>
              <th className="py-2 text-left">Name</th>
              <th className="py-2 text-left">Type</th>
              <th className="py-2 text-left">Status</th>
              <th className="py-2 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {previewData.slice(0, 5).map((r) => (
              <tr key={r.id} className="border-b last:border-0">
                <td className="py-3">{r.id}</td>
                <td className="py-3">{r.name}</td>
                <td className="py-3">{r.type}</td>
                <td className="py-3">
                  <span className={`px-3 py-1 rounded-full text-xs ${
                    r.status === "Matched"
                      ? "bg-gray-100 text-gray-700"
                      : "bg-blue-100 text-blue-600"
                  }`}>
                    {r.status}
                  </span>
                </td>
                <td className="py-3 text-right text-blue-600 cursor-pointer">
                  View Details
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <p className="text-xs text-gray-400 text-center">
          Showing 5 of {previewData.length} potential records. Full import will process all valid entries.
        </p>
      </Section>

      {/* 5. CONFLICT RESOLUTION */}
      <Section
        title="5. Conflict Resolution"
        desc="Specify how to handle duplicate records during the import process."
      >
        <div className="space-y-3 text-sm p-2">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              checked={conflict === "SKIP"}
              onChange={() => setConflict("SKIP")}
            />
            Skip existing records and Create New Records
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              checked={conflict === "UPDATE"}
              onChange={() => setConflict("UPDATE")}
            />
            Update existing records and Create new records
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              checked={conflict === "CREATE"}
              onChange={() => setConflict("CREATE")}
            />
            Create new record
          </label>
        </div>
      </Section>

      {/* 6. SCHEDULE & RUN */}
      <Section
        title="6. Schedule & Run"
        desc="Define when and how often this ingestion job should run."
      >
        <div className="space-y-3 text-sm p-2">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              checked={schedule === "MANUAL"}
              onChange={() => setSchedule("MANUAL")}
            />
            Run Manually (run once now)
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              checked={schedule === "AUTO"}
              onChange={() => setSchedule("AUTO")}
            />
            Schedule Import (automatic, recurring)
          </label>
        </div>
      </Section>

      {/* LAST RUN SUMMARY */}
      {summary && (
        <Section
          title="Last Run Summary"
          desc="Overview of the most recent directory import."
        >
          <div className="grid grid-cols-3 text-center gap-6">
            <div>
              <p className="text-2xl font-semibold">{summary.imported}</p>
              <p className="text-sm text-gray-500">Imported</p>
            </div>
            <div>
              <p className="text-2xl font-semibold text-blue-600">{summary.updated}</p>
              <p className="text-sm text-gray-500">Updated</p>
            </div>
            <div>
              <p className="text-2xl font-semibold text-red-600">{summary.failed}</p>
              <p className="text-sm text-gray-500">Failed</p>
            </div>
          </div>

          <div className="flex justify-between text-sm pt-4">
            <span className="text-gray-500">
              Last run completed on: {summary.lastRun}
            </span>
            <div className="flex gap-4 text-blue-600">
              <button className="hover:underline">View Detailed Logs</button>
              <button className="hover:underline">Contextual Help</button>
            </div>
          </div>
        </Section>
      )}

      {/* FOOTER ACTION BAR */}
      <div className="flex justify-end gap-4 pt-4">
        <button
          onClick={saveJob}
          className="px-5 py-2 rounded-lg border bg-white hover:bg-gray-50"
        >
          Save Job
        </button>
        <button
          onClick={runImport}
          className="px-6 py-2 rounded-lg bg-blue-950 text-white hover:bg-blue-700"
        >
          Run Import Now
        </button>
      </div>
    </div>
  );
}