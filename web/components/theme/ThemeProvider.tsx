"use client";

import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useSyncExternalStore,
  type ReactNode,
} from "react";

type Theme = "light" | "dark";

const STORAGE_KEY = "koda-theme";

interface ThemeContextValue {
  theme: Theme;
  toggleTheme: () => void;
  setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

/**
 * Script executado antes da primeira pintura, para evitar o flash de tema
 * errado (FOUC).
 *
 * Precisa ser sincrono e inline no <head>: qualquer solucao baseada em efeito
 * do React roda depois da hidratacao, e o usuario veria o tema claro piscar
 * antes de virar escuro. A leitura do localStorage vai em try/catch porque em
 * navegacao privada o acesso pode lancar, e uma falha aqui impediria a pagina
 * inteira de renderizar.
 */
export const themeInitScript = `
(function() {
  try {
    var stored = localStorage.getItem('${STORAGE_KEY}');
    var prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    var theme = stored || (prefersDark ? 'dark' : 'light');
    if (theme === 'dark') document.documentElement.classList.add('dark');
  } catch (e) {}
})();
`;

/*
 * O tema real mora no DOM — na classe do <html>, aplicada pelo script acima
 * antes do React existir. O React nao e dono desse estado; ele apenas o le.
 *
 * Por isso a leitura usa useSyncExternalStore em vez de useEffect + setState:
 * essa e a API feita para ler estado externo, e evita o render em cascata que
 * o padrao "efeito que sincroniza estado" provoca.
 */
const listeners = new Set<() => void>();

function subscribe(onChange: () => void): () => void {
  listeners.add(onChange);
  return () => {
    listeners.delete(onChange);
  };
}

function getSnapshot(): Theme {
  return document.documentElement.classList.contains("dark") ? "dark" : "light";
}

/** No servidor nao ha DOM; o script inline corrige antes da primeira pintura. */
function getServerSnapshot(): Theme {
  return "light";
}

function applyTheme(next: Theme): void {
  document.documentElement.classList.toggle("dark", next === "dark");
  try {
    localStorage.setItem(STORAGE_KEY, next);
  } catch {
    // Armazenamento indisponivel: o tema ainda vale para esta sessao.
  }
  listeners.forEach((listener) => listener());
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const theme = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  const setTheme = useCallback((next: Theme) => {
    applyTheme(next);
  }, []);

  const toggleTheme = useCallback(() => {
    applyTheme(getSnapshot() === "dark" ? "light" : "dark");
  }, []);

  const value = useMemo(
    () => ({ theme, toggleTheme, setTheme }),
    [theme, toggleTheme, setTheme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useTheme precisa estar dentro de <ThemeProvider>");
  }
  return context;
}
