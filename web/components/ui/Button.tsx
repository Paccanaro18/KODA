"use client";

import { cn } from "@/lib/cn";
import type { ButtonHTMLAttributes, ReactNode } from "react";

type Variant = "primary" | "secondary" | "ghost" | "success" | "danger";
type Size = "sm" | "md" | "lg";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  fullWidth?: boolean;
  leadingIcon?: ReactNode;
}

const VARIANTS: Record<Variant, string> = {
  primary:
    "bg-[var(--accent)] text-[var(--text-on-accent)] hover:bg-[var(--accent-hover)] shadow-[var(--shadow-sm)]",
  secondary:
    "bg-[var(--bg-surface-raised)] text-[var(--text-primary)] border border-[var(--border-subtle)] hover:border-[var(--border-strong)]",
  ghost:
    "bg-transparent text-[var(--text-secondary)] hover:bg-[var(--bg-surface-raised)] hover:text-[var(--text-primary)]",
  success:
    "bg-[var(--success)] text-white hover:brightness-110 shadow-[var(--shadow-sm)]",
  danger:
    "bg-[var(--error)] text-white hover:brightness-110 shadow-[var(--shadow-sm)]",
};

const SIZES: Record<Size, string> = {
  // min-h garante o alvo de toque de 44px exigido em mobile, mesmo quando o
  // conteudo do botao e curto.
  sm: "text-sm px-3 py-1.5 min-h-9 rounded-[var(--radius-sm)] gap-1.5",
  md: "text-base px-5 py-2.5 min-h-11 rounded-[var(--radius-md)] gap-2",
  lg: "text-lg px-7 py-3.5 min-h-13 rounded-[var(--radius-lg)] gap-2.5 font-semibold",
};

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  fullWidth = false,
  leadingIcon,
  className,
  children,
  disabled,
  ...props
}: ButtonProps) {
  const isBlocked = disabled || loading;

  return (
    <button
      className={cn(
        "inline-flex items-center justify-center font-medium select-none",
        "transition-[transform,background-color,border-color,filter]",
        "duration-[var(--duration-fast)] ease-[var(--ease-standard)]",
        // O recuo no clique da a sensacao fisica de pressionar. E sutil de
        // proposito: 2% e perceptivel sem parecer um brinquedo.
        "active:scale-[0.98]",
        "disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100",
        VARIANTS[variant],
        SIZES[size],
        fullWidth && "w-full",
        className,
      )}
      disabled={isBlocked}
      // Comunica o estado de carregamento a leitores de tela, que nao
      // enxergam o spinner.
      aria-busy={loading || undefined}
      {...props}
    >
      {loading ? (
        <>
          <Spinner />
          <span>{children}</span>
        </>
      ) : (
        <>
          {leadingIcon}
          {children}
        </>
      )}
    </button>
  );
}

function Spinner() {
  return (
    <svg
      className="size-4 animate-spin"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        strokeWidth="3"
        opacity="0.25"
      />
      <path
        d="M12 2a10 10 0 0 1 10 10"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
      />
    </svg>
  );
}
