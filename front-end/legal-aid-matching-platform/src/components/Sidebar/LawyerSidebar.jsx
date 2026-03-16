import { User, Calendar } from "lucide-react";
import { FaFolderOpen, FaComments } from "react-icons/fa";
import { LuFolderPlus } from "react-icons/lu";
import { FaRegHandshake } from "react-icons/fa6";
import { IoMdStats } from "react-icons/io";
import { NavLink } from "react-router-dom";

export default function LawyerSidebar() {
  return (
    <aside className="w-64 bg-white border-r p-6 h-full">
      <ul className="space-y-2 text-gray-800">

        {/* Profile */}
        <li >
          <NavLink
            to="/dashboard/lawyer/editprofile"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <User size={18} />
          Profile
          </NavLink>
        </li>

        {/* Cases Assigned */}
        <li >
          <NavLink
            to="/dashboard/lawyer/matches"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <FaFolderOpen />
          My Cases
          </NavLink>
        </li>



        {/* Match Requests */}
        <li >
          <NavLink
            to="/dashboard/lawyer/match-requests"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <FaRegHandshake />
          Match Requests
          </NavLink>
        </li>

        {/* Secure Chat */}
        <li >
          <NavLink
            to="/dashboard/lawyer/securechat"
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
            to="/dashboard/lawyer/appointments"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
            <Calendar size={18} />
            Appointments
          </NavLink>
        </li>

        {/* Lawyer Stats / Progress */}
        <li >
          <NavLink
            to="/dashboard/lawyer/stats"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <IoMdStats />
          Performance Stats
          </NavLink>
        </li>

      </ul>
    </aside>
  );
}
