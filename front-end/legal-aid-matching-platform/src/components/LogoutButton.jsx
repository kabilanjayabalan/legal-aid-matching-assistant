import { LuLogOut } from "react-icons/lu";
import useAuthStore from "../store/useAuthStore";

export default function LogoutButton() {
  const logout = useAuthStore((state) => state.logout);

  const handleLogout = () => {
    sessionStorage.clear();
    logout();
    window.location.href = "/signin";
  };

  return (
    <button
      onClick={handleLogout}
      className="px-4 py-2 bg-blue-900 text-white rounded-lg hover:bg-blue-950 
        flex items-center gap-2">
        <LuLogOut /> Logout
    </button>
  );
}
