"use client";

import { cn } from "@/lib/cn";
import type { ButtonHTMLAttributes, ReactNode } from "react";

type Variant = "action" | "primary" | "secondary" | "ghost" | "danger";
type Size = "sm" | "md" | "lg";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  fullWidth?: boolean;
  leadingIcon?: ReactNode;
}

/**
 * Botao com labio.
 *
 * A faixa escura embaixo e `box-shadow`, nunca `border-bottom`. Borda entra na
 * altura da caixa: remove-la no :active encolheria o botao em 4px e empurraria
 * tudo que vem depois dele. Sombra nao ocupa espaco, entao o afundar e puro
 * movimento — e o layout nao se mexe.
 *
 * O par translate/shadow tem que somar sempre a mesma distancia: o botao desce
 * exatamente a altura do labio que perdeu. E isso que faz o dedo sentir que
 * encostou no fundo, em vez de ver um elemento deslizando.
 */
const VARIANTS: Record<Variant, string> = {
  // Verde: avancar, confirmar, comecar. A cor mais viva do sistema, reservada
  // para o movimento que faz o aluno progredir.
  action:
    "bg-[var(--action)] text-white shadow-[0_4px_0_var(--lip-action)] hover:bg-[var(--action-hover)] active:shadow-[0_0_0_var(--lip-action)] active:translate-y-[4px]",
  // Indigo da marca: acoes de identidade e navegacao principal.
  primary:
    "bg-[var(--accent)] text-white shadow-[0_4px_0_var(--lip-accent)] hover:bg-[var(--accent-hover)] active:shadow-[0_0_0_var(--lip-accent)] active:translate-y-[4px]",
  secondary:
    "bg-[var(--bg-surface)] text-[var(--text-secondary)] border-2 border-[var(--border-subtle)] shadow-[0_4px_0_var(--lip-neutral)] hover:bg-[var(--bg-surface-raised)] active:shadow-[0_0_0_var(--lip-neutral)] active:translate-y-[4px]",
  // Sem labio de proposito: o fantasma nao e um objeto, e um atalho.
  ghost:
    "bg-transparent text-[var(--text-secondary)] hover:bg-[var(--bg-surface-raised)] hover:text-[var(--text-primary)] active:translate-y-[1px]",
  danger:
    "bg-[var(--error)] text-white shadow-[0_4px_0_var(--lip-error)] hover:brightness-105 active:shadow-[0_0_0_var(--lip-error)] active:translate-y-[4px]",
};

const SIZES: Record<Size, string> = {
  // min-h garante o alvo de toque de 44px exigido em mobile, mesmo quando o
  // conteudo do botao e curto.
  sm: "text-xs px-4 py-2 min-h-10 rounded-[var(--radius-sm)] gap-1.5",
  md: "text-sm px-6 py-3 min-h-12 rounded-[var(--radius-md)] gap-2",
  lg: "text-base px-8 py-4 min-h-14 rounded-[var(--radius-md)] gap-2.5",
};

export function Button({
  variant = "action",
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
        // Caixa alta com tracking aberto. Em rotulo curto de acao ela le como
        // voz — "CONTINUAR" soa dito, "Continuar" soa escrito.
        "inline-flex items-center justify-center select-none",
        "font-extrabold uppercase tracking-[0.06em]",
        "transition-[transform,background-color,box-shadow,filter]",
        "duration-[var(--duration-press)] ease-[var(--ease-standard)]",
        VARIANTS[variant],
        SIZES[size],
        fullWidth && "w-full",
        // Desabilitado perde o labio: deixa de ser objeto pressionavel e vira
        // superficie morta. A informacao chega pela forma, nao so pela opacidade.
        isBlocked &&
          "cursor-not-allowed bg-[var(--bg-inset)] text-[var(--text-muted)] border-transparent shadow-none hover:bg-[var(--bg-inset)] hover:brightness-100 active:translate-y-0 active:shadow-none",
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
