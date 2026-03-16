import { Calendar } from "lucide-react";
import { FaUsers, FaComments } from "react-icons/fa";
import { LuFileSpreadsheet } from "react-icons/lu";
import { FaRegHandshake } from "react-icons/fa6";
import { IoMdStats } from "react-icons/io";
import { NavLink } from "react-router-dom";

export default function NGOSidebar() {
  return (
    <aside className="w-64 bg-white border-r p-6 h-full">
      <ul className="space-y-2 text-gray-800">

        {/* NGO Profile */}
        <li >
          <NavLink
            to="/dashboard/ngo/editprofile"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <FaUsers />
          NGO Profile
          </NavLink>
        </li>

        {/* Case Intake */}
        <li >
          <NavLink
            to="/dashboard/ngo/assignedcases"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <LuFileSpreadsheet />
          Case Intake
          </NavLink>
        </li>

        {/* Matching Requests */}
        <li >
          <NavLink
            to="/dashboard/ngo/match-requests"
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
            to="/dashboard/ngo/securechat"
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
            to="/dashboard/ngo/appointments"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
            <Calendar size={18} />
            Appointments
          </NavLink>
        </li>

        {/* NGO Impact Stats */}
        <li >
          <NavLink
            to="/dashboard/ngo/overview"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg 
              ${isActive ? "bg-blue-950 text-white" : "text-gray-700 hover:bg-gray-100"}`
            }
          >
          <IoMdStats />
          Impact Analytics
          </NavLink>
        </li>

      </ul>
    </aside>
  );
}
