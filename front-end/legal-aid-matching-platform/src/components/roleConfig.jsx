import AdminSidebar from "../components/Sidebar/AdminSidebar";
import CitizenSidebar from "./Sidebar/CitizenSidebar";
import LawyerSidebar from "./Sidebar/LawyerSidebar";
import NGOSidebar from "./Sidebar/NgoSidebar";

export const ROLE_COMPONENTS = {
  ADMIN: {
    sidebar: <AdminSidebar />,
  },
  LAWYER: {
    sidebar: <LawyerSidebar />,
  },
  NGO: {
    sidebar: <NGOSidebar />,
  },
  CITIZEN: {
    sidebar: <CitizenSidebar />,
  },
};
