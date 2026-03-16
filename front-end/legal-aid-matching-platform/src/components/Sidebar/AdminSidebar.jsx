import { Shield } from "lucide-react";
import { FaSearch, FaHome } from "react-icons/fa";
import { IoMdStats } from "react-icons/io";
import { FaFolderOpen } from "react-icons/fa6";
import { RiAdminLine } from "react-icons/ri";
import { FiActivity } from "react-icons/fi";
import { NavLink } from "react-router-dom";

export default function AdminSidebar() {
  return (
    <aside className="w-64 bg-white border-r p-6 h-full">
      <ul className="space-y-2 text-gray-800">

        {/* Profile Management */}
        <li>
          <NavLink
            to="/dashboard/admin/profilemanagement"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
            <Shield size={18} />
            Profile Management
          </NavLink>
        </li>



        {/* Directory */}
        <li>
          <NavLink
            to="/dashboard/admin/directorysearch"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
            <FaSearch />
            Directory
          </NavLink>
        </li>

        {/* Case Operations */}
        <li>
          <NavLink
            to="/dashboard/admin/caseoperations"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
            <FaFolderOpen />
            Case Operations
          </NavLink>
        </li>

        {/* Impact Dashboard */}
        <li>
          <NavLink
            to="/dashboard/admin/impactdashboard"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
            <IoMdStats />
            Impact Dashboard
          </NavLink>
        </li>

        {/* System Monitoring */}
        <li>
          <NavLink
            to="/dashboard/admin/systemmonitoring"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
            <FiActivity />
            System Monitoring
          </NavLink>
        </li>

        {/* Admin Panel */}
        <li>
          <NavLink
            to="/dashboard/admin/adminpanel"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
            <RiAdminLine />
            Admin Panel
          </NavLink>
        </li>

      </ul>
    </aside>
  );
}
