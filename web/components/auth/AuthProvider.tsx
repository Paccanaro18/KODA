"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import * as api from "@/lib/api";
import type { Me } from "@/lib/api";

type Status = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  status: Status;
  user: Me | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName?: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * O access token nunca sai deste ref: nada de localStorage nem de estado que
 * outro componente possa ler diretamente (SEC-07, ADR-0002). Ele so existe em
 * memoria, e some ao fechar a aba — por isso o mount tenta um refresh silencioso
 * usando o cookie httpOnly, que e o que faz a sessao sobreviver a um F5.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<Status>("loading");
  const [user, setUser] = useState<Me | null>(null);
  const accessTokenRef = useRef<string | null>(null);

  const settle = useCallback((tokens: api.AuthTokens, profile: api.Me) => {
    accessTokenRef.current = tokens.accessToken;
    setUser(profile);
    setStatus("authenticated");
  }, []);

  useEffect(() => {
    let cancelled = false;

    api
      .refresh()
      .then((tokens) => api.me(tokens.accessToken).then((profile) => ({ tokens, profile })))
      .then(({ tokens, profile }) => {
        if (!cancelled) settle(tokens, profile);
      })
      .catch(() => {
        if (!cancelled) {
          accessTokenRef.current = null;
          setUser(null);
          setStatus("unauthenticated");
        }
      });

    return () => {
      cancelled = true;
    };
  }, [settle]);

  const login = useCallback(
    async (email: string, password: string) => {
      const tokens = await api.login({ email, password });
      const profile = await api.me(tokens.accessToken);
      settle(tokens, profile);
    },
    [settle],
  );

  const register = useCallback(
    async (email: string, password: string, displayName?: string) => {
      const tokens = await api.register({ email, password, displayName });
      const profile = await api.me(tokens.accessToken);
      settle(tokens, profile);
    },
    [settle],
  );

  const logout = useCallback(async () => {
    try {
      await api.logout();
    } finally {
      // Limpa o lado do cliente mesmo se a chamada falhar: uma sessao que o
      // usuario acha encerrada nunca pode continuar parecendo ativa na tela.
      accessTokenRef.current = null;
      setUser(null);
      setStatus("unauthenticated");
    }
  }, []);

  return (
    <AuthContext.Provider value={{ status, user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth precisa estar dentro de <AuthProvider>");
  }
  return context;
}
