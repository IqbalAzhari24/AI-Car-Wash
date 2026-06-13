import React, { createContext, useContext, useState } from 'react';
import { jwtDecode } from 'jwt-decode';

interface User {
  id: string;
  role: string;
}

interface JwtPayload {
  sub: string;
  role: string;
  exp: number;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (token: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Validate the stored token synchronously so a hard refresh of a protected
// route doesn't bounce an authenticated user to /login before the first render.
const readStoredSession = (): { token: string; user: User } | null => {
  const storedToken = localStorage.getItem('token');
  if (!storedToken) return null;
  try {
    const decoded = jwtDecode<JwtPayload>(storedToken);
    if (decoded.exp * 1000 > Date.now()) {
      return { token: storedToken, user: { id: decoded.sub, role: decoded.role } };
    }
  } catch (e) {
    // fall through to cleanup
  }
  localStorage.removeItem('token');
  return null;
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [session] = useState(readStoredSession);
  const [user, setUser] = useState<User | null>(session?.user ?? null);
  const [token, setToken] = useState<string | null>(session?.token ?? null);

  const login = (newToken: string) => {
    try {
      const decoded = jwtDecode<JwtPayload>(newToken);
      localStorage.setItem('token', newToken);
      setToken(newToken);
      setUser({ id: decoded.sub, role: decoded.role });
    } catch(e) {
      console.error("Invalid token", e);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
