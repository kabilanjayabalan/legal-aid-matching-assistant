import React, { createContext, useState, useContext } from 'react';

const SavedProfilesContext = createContext();

// Mock initial saved profiles
const INITIAL_SAVED_PROFILES = [
  {
    id: 101,
    type: "Lawyer",
    verified: true,
    rating: 4.9,
    image: "https://randomuser.me/api/portraits/men/32.jpg",
    name: "Robert Fox, Esq.",
    location: "Chicago, IL",
    email: "robert.fox@law.com",
    phone: "+1 (312) 555-0199",
    areas: ["Criminal Defense", "Civil Rights"],
  },
];

export const SavedProfilesProvider = ({ children }) => {
  const [savedProfiles, setSavedProfiles] = useState(INITIAL_SAVED_PROFILES);

  const addProfile = (profile) => {
    setSavedProfiles((prev) => {
      if (prev.some((p) => p.id === profile.id)) return prev;
      return [profile, ...prev];
    });
  };

  const removeProfile = (id) => {
    setSavedProfiles((prev) => prev.filter((p) => p.id !== id));
  };

  const isProfileSaved = (id) => {
    return savedProfiles.some((p) => p.id === id);
  };

  return (
    <SavedProfilesContext.Provider value={{ savedProfiles, addProfile, removeProfile, isProfileSaved }}>
      {children}
    </SavedProfilesContext.Provider>
  );
};

export const useSavedProfiles = () => useContext(SavedProfilesContext);
