import { useState } from "react";
import { Outlet } from "react-router-dom";
import Header from "../../Header";
import { ROLE_COMPONENTS } from "../../roleConfig";
import Footer from "../../Footer";
import { useEffect,useRef } from "react";

export default function Dashboard() {
  const role = sessionStorage.getItem("role") || "ADMIN";
  const roleComponents = ROLE_COMPONENTS[role];

  // Sidebar states for animation
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isClosing, setIsClosing] = useState(false);
  const titleSet = useRef(false);

  useEffect(() => {
    if (!titleSet.current) {
      document.title = "Dashboard | Legal Aid";
      titleSet.current = true;
    }
  }, []);

  // Smooth slide-out animation
  const closeSidebar = () => {
    setIsClosing(true);
    setTimeout(() => {
      setIsSidebarOpen(false);
      setIsClosing(false);
    }, 300); // must match animation duration in index.css
  };

  if (!roleComponents)
    return <div className="p-6 text-red-600">Invalid role: {role}</div>;

  return (
    <div className="min-h-screen flex flex-col">
      
      {/* HEADER */}
      <Header onMenuClick={() => setIsSidebarOpen(true)} />


      <div className="flex flex-1 min-h-0">

        {/* Desktop Sidebar */}
        <div className="hidden lg:block">
          {roleComponents.sidebar}
        </div>


        {/* Mobile Sidebar Overlay */}
        {isSidebarOpen && (
          <div
            className="fixed inset-0 bg-black bg-opacity-40 z-50 lg:hidden"
            onClick={closeSidebar}
          >
            {/* The sliding sidebar */}
            <div
              className={`
                absolute left-0 top-0 h-full w-64 bg-white shadow-xl 
                ${isClosing ? "animate-slideOut" : "animate-slideIn"}
              `}
              onClick={(e) => e.stopPropagation()} // prevent closing when clicking sidebar
            >
              {/* Close Button Row */}
              <div className="flex justify-start p-4">
                <button
                  className="text-gray-700 font-medium"
                  onClick={closeSidebar}
                  >
                    X
                </button>
              </div>

              {/* Sidebar Content */}
              <div className=" pt-2">
                  {roleComponents.sidebar}
              </div>
            </div>
          </div>
        )}


        {/* MAIN CONTENT */}
        <main className="flex-1 bg-gray-100 overflow-auto p-2">
          <Outlet />
        </main>
      </div>


      <Footer/>

    </div>
  );
}
