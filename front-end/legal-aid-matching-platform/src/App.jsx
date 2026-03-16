import React, { Suspense, lazy, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, useLocation } from "react-router-dom";
import { SavedProfilesProvider } from "./context/SavedProfilesContext";
import LegalLoader from "./components/LegalLoader";
import MaintenanceBanner from "./components/system/MaintenanceBanner";
import ChatBot from "./components/ChatBot";

// Lazy-loaded components
const TermsAndConditions = lazy(() => import("./pages/TermsAndConditions"));
const PrivacyPolicy = lazy(() => import("./pages/PrivacyPolicy"));
const Signin = lazy(() => import("./pages/Signin"));
const Signup = lazy(() => import("./pages/Signup"));
const Dashboard = lazy(() => import("./components/RoleContent/lawyer/Dashboard"));
const OAuthCallback = lazy(() => import("./components/OAuthCallback"));
const ProtectedRoute = lazy(() => import("./components/ProtectedRoute"));
const AdminPanel = lazy(() => import("./components/RoleContent/admin/AdminPanel"));
const ImpactDashboard = lazy(() => import("./components/RoleContent/admin/ImpactDashboard"));
const SystemMonitoring = lazy(() => import("./components/RoleContent/admin/SystemMonitoring"));
const Overview = lazy(() => import("./components/RoleContent/citizen/Overview"));
const Stats = lazy(() => import("./components/RoleContent/lawyer/Stats"));
const NgoDashboard = lazy(() => import("./components/RoleContent/ngo/NgoDashboard"));
const LawyerProfile = lazy(() => import("./components/RoleContent/lawyer/LawyerEditProfile"));
const NGOEditProfile = lazy(() => import("./components/RoleContent/ngo/NGOEditProfile"));
const CitizenEditProfile = lazy(() => import("./components/RoleContent/citizen/CitizenEditProfile"));
const CaseSubmit = lazy(() => import("./components/RoleContent/citizen/CaseSubmit"));
const CaseManage = lazy(() => import("./components/RoleContent/citizen/CaseManage"));
const Matches = lazy(() => import("./components/RoleContent/citizen/Matches"));
const ProfileManagement = lazy(() => import("./components/RoleContent/admin/ProfileManagement"));
const DirectorySearch = lazy(() => import("./components/RoleContent/citizen/DirectorySearch"));
const SecureChat = lazy(() => import("./components/RoleContent/citizen/SecureChat"));
const LawyerSecureChat = lazy(() => import("./components/RoleContent/lawyer/SecureChat"));
const SavedProfiles = lazy(() => import("./components/RoleContent/citizen/SavedProfiles"));
const HomePage = lazy(() => import("./pages/HomePage"));
const NotFound = lazy(() => import("./pages/NotFound"));
const AssignedCases = lazy(() => import("./components/RoleContent/lawyer/AssignedCases"));
const NGOAssignedCases = lazy(() => import("./components/RoleContent/lawyer/AssignedCases"));
const MatchRequests = lazy(() => import("./components/RoleContent/lawyer/MatchRequests"));
const NGOMatchRequests = lazy(() => import("./components/RoleContent/ngo/MatchRequests"));
const Appointments = lazy(() => import("./components/RoleContent/citizen/Appointments"));
const LawyerAppointments = lazy(() => import("./components/RoleContent/lawyer/Appointments"));
const NGOAppointments = lazy(() => import("./components/RoleContent/ngo/Appointments"));
const NotificationsPage = lazy(() => import("./components/NotificationsPage"));
const CaseOperations = lazy(() => import("./components/RoleContent/admin/CaseOperations"));

function AnimatedRoutes() {
  const location = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location]);

  return (
    <Suspense fallback={<LegalLoader />}>
      <Routes location={location} key={location.pathname}>
        <Route path="/" element={<HomePage />} />
        <Route path="/signin" element={<Signin />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/terms" element={<TermsAndConditions />} />
        <Route path="/privacy" element={<PrivacyPolicy />} />
        <Route path="/oauth2/callback" element={<OAuthCallback />} />

        <Route
          path="/dashboard/admin"
          element={
            <ProtectedRoute allowedRoles={["ADMIN"]}>
              <Dashboard />
            </ProtectedRoute>
          }
        >
          <Route index element={<AdminPanel />} />
          <Route path="adminpanel" element={<AdminPanel />} />
          <Route path="profilemanagement" element={<ProfileManagement />} />
          <Route path="impactdashboard" element={<ImpactDashboard />} />
          <Route path="caseoperations" element={<CaseOperations />} />
          <Route path="systemmonitoring" element={<SystemMonitoring />} />
          <Route path="directorysearch" element={<DirectorySearch />} />
          <Route path="notifications" element={<NotificationsPage />} />
        </Route>

        <Route
          path="/dashboard/lawyer"
          element={
            <ProtectedRoute allowedRoles={["LAWYER"]}>
              <Dashboard />
            </ProtectedRoute>
          }
        >
          <Route index element={<Stats />} />
          <Route path="editprofile" element={<LawyerProfile />} />
          <Route path="stats" element={<Stats />} />
          <Route path="securechat" element={<LawyerSecureChat />} />
          <Route path="matches" element={<AssignedCases role="LAWYER" />} />
          <Route path="match-requests" element={<MatchRequests />} />
          <Route path="appointments" element={<LawyerAppointments />} />
          <Route path="notifications" element={<NotificationsPage />} />
        </Route>

        <Route
          path="/dashboard/ngo"
          element={
            <ProtectedRoute allowedRoles={["NGO"]}>
              <Dashboard />
            </ProtectedRoute>
          }
        >
          <Route index element={<NgoDashboard />} />
          <Route path="editprofile" element={<NGOEditProfile />} />
          <Route path="overview" element={<NgoDashboard />} />
          <Route path="assignedcases" element={<NGOAssignedCases />} />
          <Route path="matches" element={<AssignedCases role="NGO" />} />
          <Route path="match-requests" element={<NGOMatchRequests />} />
          <Route path="securechat" element={<LawyerSecureChat />} />
          <Route path="appointments" element={<NGOAppointments />} />
          <Route path="notifications" element={<NotificationsPage />} />
        </Route>

        <Route
          path="/dashboard/citizen"
          element={
            <ProtectedRoute allowedRoles={["CITIZEN"]}>
              <Dashboard />
            </ProtectedRoute>
          }
        >
          <Route index element={<Overview />} />
          <Route path="editprofile" element={<CitizenEditProfile />} />
          <Route path="overview" element={<Overview />} />
          <Route path="casesubmit" element={<CaseSubmit />} />
          <Route path="casemanage" element={<CaseManage />} />
          <Route path="matches" element={<Matches />} />
          <Route path="directorysearch" element={<DirectorySearch />} />
          <Route path="securechat" element={<SecureChat />} />
          <Route path="saved" element={<SavedProfiles />} />
          <Route path="appointments" element={<Appointments />} />
          <Route path="notifications" element={<NotificationsPage />} />
        </Route>

        <Route path="*" element={<NotFound />} />
      </Routes>
    </Suspense>
  );
}

function App() {
  return (
    <SavedProfilesProvider>
      <Router>
        <MaintenanceBanner />
        <AnimatedRoutes />
      </Router>
      <ChatBot />
    </SavedProfilesProvider>
  );
}

export default App;
