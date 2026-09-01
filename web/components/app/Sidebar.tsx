"use client";

import { cn } from "@/lib/cn";
import { Koda } from "@/components/ui/Koda";
import type { ReactNode } from "react";

interface NavItem {
  id: string;
  label: string;
  icon: ReactNode;
  href: string;
}

/**
 * Navegacao principal do produto.
 *
 * Vira barra inferior no mobile e coluna fixa no desktop — mesmos itens, mesmo
 * estado ativo, dois formatos. O item ativo se distingue por FUNDO E BORDA,
 * nao so por cor de texto: numa barra de icones pequenos, texto colorido some.
 */
const ITEMS: NavItem[] = [
  { id: "aprender", label: "Aprender", href: "/aprender", icon: <PathIcon /> },
  { id: "praticar", label: "Praticar", href: "/praticar", icon: <DumbbellIcon /> },
  { id: "conquistas", label: "Conquistas", href: "/conquistas", icon: <TrophyIcon /> },
  { id: "perfil", label: "Perfil", href: "/perfil", icon: <UserIcon /> },
];

export function Sidebar({ active = "aprender" }: { active?: string }) {
  return (
    <>
      {/* Desktop: coluna fixa */}
      <nav
        className="hidden md:flex fixed inset-y-0 left-0 z-20 w-60 flex-col gap-1 px-3 py-5 border-r-2 border-[var(--border-subtle)] bg-[var(--bg-surface)]"
        aria-label="Navegacao principal"
      >
        <a
          href="/aprender"
          className="flex items-center gap-2 px-3 pb-5 text-2xl font-extrabold tracking-[-0.02em] text-[var(--accent)]"
        >
          <Koda size={36} />
          KODA
        </a>

        {ITEMS.map((item) => (
          <NavLink key={item.id} item={item} active={item.id === active} />
        ))}
      </nav>

      {/* Mobile: barra inferior */}
      <nav
        className="md:hidden fixed inset-x-0 bottom-0 z-20 flex justify-around gap-1 px-2 py-2 border-t-2 border-[var(--border-subtle)] bg-[var(--bg-surface)]"
        aria-label="Navegacao principal"
      >
        {ITEMS.map((item) => (
          <a
            key={item.id}
            href={item.href}
            aria-current={item.id === active ? "page" : undefined}
            className={cn(
              "flex flex-col items-center gap-0.5 flex-1 min-h-12 py-1.5 rounded-[var(--radius-md)] border-2",
              item.id === active
                ? "border-[var(--accent)] bg-[var(--accent-soft)] text-[var(--accent)]"
                : "border-transparent text-[var(--text-muted)]",
            )}
          >
            <span className="size-6">{item.icon}</span>
            <span className="text-[0.6875rem] font-extrabold uppercase tracking-[0.04em]">
              {item.label}
            </span>
          </a>
        ))}
      </nav>
    </>
  );
}

function NavLink({ item, active }: { item: NavItem; active: boolean }) {
  return (
    <a
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "flex items-center gap-3 px-3 py-3 rounded-[var(--radius-md)] border-2",
        "text-sm font-extrabold uppercase tracking-[0.06em]",
        "transition-colors duration-[var(--duration-fast)]",
        active
          ? "border-[var(--accent)] bg-[var(--accent-soft)] text-[var(--accent)]"
          : "border-transparent text-[var(--text-secondary)] hover:bg-[var(--bg-surface-raised)]",
      )}
    >
      <span className="size-6 shrink-0">{item.icon}</span>
      {item.label}
    </a>
  );
}

/* --- Icones -----------------------------------------------------------------
   Traco grosso e ponta arredondada. Icone de 1.5px de traco le como interface
   de ferramenta; de 2.5px, como sinalizacao — que e o que uma barra de
   navegacao precisa ser. */

function PathIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-full" fill="none" aria-hidden="true">
      <path
        d="M6 21V9a4 4 0 018 0v6a4 4 0 008 0V3"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
      <circle cx="6" cy="21" r="2" fill="currentColor" />
    </svg>
  );
}

function DumbbellIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-full" fill="none" aria-hidden="true">
      <path
        d="M4 9v6M8 6v12M16 6v12M20 9v6M8 12h8"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
    </svg>
  );
}

function TrophyIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-full" fill="none" aria-hidden="true">
      <path
        d="M7 4h10v6a5 5 0 01-10 0V4zM7 6H4v2a3 3 0 003 3M17 6h3v2a3 3 0 01-3 3M9 20h6M12 15v5"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function UserIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-full" fill="none" aria-hidden="true">
      <circle cx="12" cy="8" r="4" stroke="currentColor" strokeWidth="2.5" />
      <path
        d="M4.5 20a7.5 7.5 0 0115 0"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
    </svg>
  );
}
