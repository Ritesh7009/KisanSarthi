import { useState, useCallback } from 'react';
import { Language, UserRole } from '../types';
import {
  getStoredLanguage,
  setStoredLanguage,
  getStoredUser,
  setStoredUser,
} from '../utils/offlineStorage';
import { setAuthToken } from '../services/api';

export interface AuthUser {
  id?: string;
  name: string;
  phone: string;
  aadharNumber?: string;
  maskedAadhar?: string;
  district: string;
  village?: string;
  role: UserRole;
  mandiId?: string;
}

interface UseAuthSessionProps {
  onLoginSuccessCallback?: (user: AuthUser) => void;
}

const DEFAULT_DEMO_FARMER: AuthUser = {
  name: 'Ramesh Patel',
  phone: '9826014522',
  aadharNumber: '710488214522',
  maskedAadhar: 'XXXX-XXXX-4522',
  district: 'Sehore',
  village: 'Bijora',
  role: 'FARMER',
};

export function useAuthSession({ onLoginSuccessCallback }: UseAuthSessionProps = {}) {
  const [language, setLanguage] = useState<Language>(() => getStoredLanguage());
  const [role, setRole] = useState<UserRole>('FARMER');
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(() => {
    const stored = getStoredUser();
    return stored || DEFAULT_DEMO_FARMER;
  });
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);

  const handleLanguageChange = useCallback((lang: Language) => {
    setLanguage(lang);
    setStoredLanguage(lang);
  }, []);

  const handleRoleSwitch = useCallback((newRole: UserRole) => {
    setRole(newRole);
    setCurrentUser((prev) => {
      if (!prev) return null;
      const updated = { ...prev, role: newRole };
      setStoredUser(updated);
      return updated;
    });
  }, []);

  const handleLoginSuccess = useCallback((user: AuthUser) => {
    setCurrentUser(user);
    setRole(user.role);
    setStoredUser(user);
    setIsLoginModalOpen(false);

    if (onLoginSuccessCallback) {
      onLoginSuccessCallback(user);
    }
  }, [onLoginSuccessCallback]);

  const handleLogout = useCallback(() => {
    setAuthToken(null);
    setCurrentUser(null);
    setStoredUser(null);
  }, []);

  return {
    language,
    handleLanguageChange,
    role,
    handleRoleSwitch,
    currentUser,
    handleLoginSuccess,
    handleLogout,
    isLoginModalOpen,
    setIsLoginModalOpen,
  };
}
