import { createContext, useContext, useState } from "react";
import CustomAlert from "../components/CustomAlert";

const AlertContext = createContext();

export const AlertProvider = ({ children }) => {
  const [alertMessage, setAlertMessage] = useState("");
  const [showAlert, setShowAlert] = useState(false);

  const showAlertFn = (msg) => {
    setAlertMessage(msg);
    setShowAlert(true);
  };

  const closeAlert = () => setShowAlert(false);

  return (
    <AlertContext.Provider value={{ showAlert: showAlertFn }}>
      {children}

      {/* GLOBAL POPUP — Renders on top of all pages */}
      {showAlert && (
        <CustomAlert message={alertMessage} onClose={closeAlert} />
      )}
    </AlertContext.Provider>
  );
};

// Custom hook to access alert anywhere
export const useAlert = () => useContext(AlertContext);
