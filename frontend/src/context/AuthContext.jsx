import { createContext, useContext, useEffect, useState } from "react";

const AuthContext = createContext(null);
const STORAGE_KEY = "ovs-auth";

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? JSON.parse(saved) : null;
  });

  useEffect(() => {
    if (auth) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, [auth]);

  const value = {
    auth,
    login: (nextAuth) => setAuth(nextAuth),
    logout: () => setAuth(null)
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
