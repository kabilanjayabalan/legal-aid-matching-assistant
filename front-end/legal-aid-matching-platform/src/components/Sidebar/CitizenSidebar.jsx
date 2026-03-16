import { User, Calendar } from "lucide-react";
import { FaSearch, FaHistory, FaHandshake, FaComments } from "react-icons/fa";
import { LuFolderPlus } from "react-icons/lu";
import { AiOutlineHeart } from "react-icons/ai";
import { RiDashboardLine } from "react-icons/ri";
import { NavLink } from "react-router-dom";
import { MdManageHistory } from "react-icons/md";

export default function CitizenSidebar() {
  return (
    <aside className="w-64 bg-white border-r p-6 h-full">
      <ul className="space-y-2 text-gray-800">

        {/* Profile */}
        <li >
          <NavLink
            to="/dashboard/citizen/editprofile"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <User size={18} />
          Profile
          </NavLink>
        </li>

        {/* Submit Legal Query */}
        <li >
          <NavLink
            to="/dashboard/citizen/casesubmit"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <LuFolderPlus />
          Submit Legal Query
          </NavLink>
        </li>

        {/* Case Management */}
        <li >
          <NavLink
            to="/dashboard/citizen/casemanage"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <FaHistory />
          Case Management
          </NavLink>
        </li>

        {/* Matches */}
        <li >
          <NavLink
            to="/dashboard/citizen/matches"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <FaHandshake />
          My Matches
          </NavLink>
        </li>

        {/* Search Lawyer Directory */}
        <li >
          <NavLink
            to="/dashboard/citizen/directorysearch"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <FaSearch />
          Find Lawyers/NGOs
          </NavLink>
        </li>

        {/* Saved Lawyers / NGOs */}
        <li >
          <NavLink
            to="/dashboard/citizen/saved"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <AiOutlineHeart />
          Saved Profiles
          </NavLink>
        </li>

        {/* Secure Chat */}
        <li >
          <NavLink
            to="/dashboard/citizen/securechat"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <FaComments />
          Secure Chat
          </NavLink>
        </li>

        {/* Appointments */}
        <li >
          <NavLink
            to="/dashboard/citizen/appointments"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
            <Calendar size={18} />
            Appointments
          </NavLink>
        </li>

        {/* Dashboard → navigate example */}
        <li >
          <NavLink
            to="/dashboard/citizen/overview"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
            <RiDashboardLine />
            Citizen Dashboard
          </NavLink>
        </li>

      </ul>
    </aside>
  );
}
