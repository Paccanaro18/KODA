"use client";

import { cn } from "@/lib/cn";
import type { ButtonHTMLAttributes } from "react";

export type AnswerState =
  | "default"
  | "selected"
  | "correct"
  | "incorrect"
  /** A alternativa certa, revelada depois que o aluno errou. */
  | "revealed";

interface AnswerOptionProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, "children"> {
  state?: AnswerState;
  /** Marcador da alternativa: "A", "B", "1"... */
  marker?: string;
  label: string;
  /** Renderiza o texto em fonte monoespacada, para alternativas com codigo. */
  mono?: boolean;
}

/**
 * Alternativa de multipla escolha.
 *
 * Cada alternativa e um objeto pressionavel de verdade: tem labio, afunda no
 * clique e continua afundada enquanto estiver selecionada. Esse "ficar
 * apertado" e o que dispensa qualquer marcacao extra de selecao.
 */
const STATES: Record<AnswerState, string> = {
  default:
    "border-[var(--border-subtle)] bg-[var(--bg-surface)] shadow-[0_4px_0_var(--lip-neutral)] hover:bg-[var(--bg-surface-raised)] active:shadow-[0_0_0_var(--lip-neutral)] active:translate-y-[4px]",
  // Selecionada permanece no fundo: nao ha labio, e ela ja esta deslocada.
  selected:
    "border-[var(--accent)] bg-[var(--accent-soft)] text-[var(--accent)] translate-y-[4px] shadow-none",
  correct:
    "border-[var(--success)] bg-[var(--success-soft)] text-[var(--lip-success)] translate-y-[4px] shadow-none",
  incorrect:
    "border-[var(--error)] bg-[var(--error-soft)] text-[var(--lip-error)] translate-y-[4px] shadow-none",
  revealed:
    "border-[var(--success)] bg-[var(--success-soft)] text-[var(--lip-success)] translate-y-[4px] shadow-none",
};

const MARKER_STATES: Record<AnswerState, string> = {
  default:
    "border-[var(--border-strong)] text-[var(--text-muted)] bg-[var(--bg-surface)]",
  selected: "border-[var(--accent)] bg-[var(--accent)] text-white",
  correct: "border-[var(--success)] bg-[var(--success)] text-white",
  incorrect: "border-[var(--error)] bg-[var(--error)] text-white",
  revealed: "border-[var(--success)] bg-[var(--success)] text-white",
};

export function AnswerOption({
  state = "default",
  marker,
  label,
  mono = false,
  className,
  disabled,
  ...props
}: AnswerOptionProps) {
  const isResolved =
    state === "correct" || state === "incorrect" || state === "revealed";

  return (
    <button
      type="button"
      className={cn(
        "group w-full flex items-center gap-3 text-left font-bold",
        // min-h-16 mantem o alvo de toque confortavel em mobile.
        "px-4 py-4 min-h-16 border-2 rounded-[var(--radius-md)]",
        "transition-[transform,background-color,border-color,box-shadow]",
        "duration-[var(--duration-press)] ease-[var(--ease-standard)]",
        disabled && "cursor-not-allowed opacity-60",
        STATES[state],
        className,
      )}
      disabled={disabled || isResolved}
      // O estado precisa chegar a quem usa leitor de tela, nao so a quem ve a cor.
      aria-pressed={state === "selected"}
      {...props}
    >
      {marker && (
        <span
          className={cn(
            "shrink-0 grid place-items-center size-8 rounded-[var(--radius-sm)] border-2",
            "text-sm font-extrabold transition-colors duration-[var(--duration-fast)]",
            MARKER_STATES[state],
          )}
          aria-hidden="true"
        >
          {marker}
        </span>
      )}

      <span className={cn("flex-1", mono && "font-mono text-sm font-semibold")}>
        {label}
      </span>

      {/*
        Icone alem da cor. Comunicar acerto e erro apenas por verde e vermelho
        exclui quem tem daltonismo — o simbolo carrega o mesmo significado.
        O texto em sr-only garante o equivalente para leitores de tela.
      */}
      {state === "correct" && <StatusIcon kind="correct" />}
      {state === "revealed" && <StatusIcon kind="revealed" />}
      {state === "incorrect" && <StatusIcon kind="incorrect" />}
    </button>
  );
}

function StatusIcon({ kind }: { kind: "correct" | "incorrect" | "revealed" }) {
  const isPositive = kind !== "incorrect";
  const srText =
    kind === "correct"
      ? "Resposta correta"
      : kind === "revealed"
        ? "Esta era a resposta correta"
        : "Resposta incorreta";

  return (
    <>
      <span className="sr-only">{srText}</span>
      <span
        className={cn(
          "shrink-0 grid place-items-center size-7 rounded-full text-white",
          "motion-safe:animate-[koda-pop_var(--duration-base)_var(--ease-spring)]",
          isPositive ? "bg-[var(--success)]" : "bg-[var(--error)]",
        )}
        aria-hidden="true"
      >
        {isPositive ? (
          <svg viewBox="0 0 24 24" className="size-4" fill="none">
            <path
              d="M5 13l4 4L19 7"
              stroke="currentColor"
              strokeWidth="3.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        ) : (
          <svg viewBox="0 0 24 24" className="size-4" fill="none">
            <path
              d="M6 6l12 12M18 6L6 18"
              stroke="currentColor"
              strokeWidth="3.5"
              strokeLinecap="round"
            />
          </svg>
        )}
      </span>
    </>
  );
}
