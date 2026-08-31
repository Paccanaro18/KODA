"use client";

import { cn } from "@/lib/cn";
import { ProgressRing } from "./Progress";

export type SkillState =
  | "locked"
  | "available"
  | "active"
  | "completed"
  | "mastered"
  | "needsReview";

interface SkillNodeProps {
  title: string;
  state: SkillState;
  /** 0 a 100. Relevante nos estados active e needsReview. */
  progress?: number;
  onClick?: () => void;
}

/**
 * No do mapa de aprendizado.
 *
 * Cada estado se distingue por forma, icone e opacidade — nao apenas por cor.
 * Um mapa que dependesse so do matiz seria ilegivel para daltonicos, e e a
 * principal peca de navegacao do produto.
 */
const STATE_STYLES: Record<SkillState, string> = {
  locked:
    "border-dashed border-[var(--border-subtle)] bg-[var(--bg-inset)] opacity-60",
  available:
    "border-solid border-[var(--border-strong)] bg-[var(--bg-surface)] hover:border-[var(--accent)]",
  active:
    "border-solid border-[var(--accent)] bg-[var(--bg-surface)] shadow-[var(--shadow-md)]",
  completed: "border-solid border-[var(--success)] bg-[var(--bg-surface)]",
  mastered: "border-solid border-[var(--reward)] bg-[var(--bg-surface)]",
  needsReview:
    "border-solid border-[var(--accent-secondary)] bg-[var(--bg-surface)]",
};

const STATE_LABELS: Record<SkillState, string> = {
  locked: "Bloqueado",
  available: "Disponivel",
  active: "Em andamento",
  completed: "Concluido",
  mastered: "Dominado",
  needsReview: "Precisa de revisao",
};

export function SkillNode({
  title,
  state,
  progress = 0,
  onClick,
}: SkillNodeProps) {
  const isLocked = state === "locked";

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={isLocked}
      className={cn(
        "group flex flex-col items-center gap-2 w-28 p-3 border-2",
        "rounded-[var(--radius-xl)]",
        "transition-[transform,border-color,box-shadow]",
        "duration-[var(--duration-base)] ease-[var(--ease-standard)]",
        !isLocked && "hover:-translate-y-1 cursor-pointer",
        isLocked && "cursor-not-allowed",
        STATE_STYLES[state],
      )}
      // O estado vai junto do titulo no rotulo acessivel: quem usa leitor de
      // tela recebe a mesma informacao que a borda e o icone transmitem.
      aria-label={`${title} — ${STATE_LABELS[state]}`}
    >
      <span className="relative">
        <ProgressRing
          value={
            state === "completed" || state === "mastered" ? 100 : progress
          }
          size={52}
          strokeWidth={4}
          tone={
            state === "mastered"
              ? "reward"
              : state === "completed"
                ? "success"
                : "accent"
          }
        >
          <StateIcon state={state} />
        </ProgressRing>

        {/* Pulso lento sinaliza "precisa de revisao" sem exigir leitura. */}
        {state === "needsReview" && (
          <span
            className="absolute -top-0.5 -right-0.5 size-3 rounded-full bg-[var(--accent-secondary)] motion-safe:animate-[koda-pulse-soft_2s_ease-in-out_infinite]"
            aria-hidden="true"
          />
        )}
      </span>

      <span
        className={cn(
          "text-xs font-medium text-center leading-tight",
          isLocked && "text-[var(--text-muted)]",
        )}
      >
        {title}
      </span>
    </button>
  );
}

function StateIcon({ state }: { state: SkillState }) {
  const common = "size-4";

  switch (state) {
    case "locked":
      return (
        <svg viewBox="0 0 24 24" className={common} fill="none" aria-hidden="true">
          <rect
            x="5"
            y="11"
            width="14"
            height="10"
            rx="2"
            stroke="currentColor"
            strokeWidth="2"
          />
          <path
            d="M8 11V7a4 4 0 118 0v4"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
      );
    case "completed":
      return (
        <svg viewBox="0 0 24 24" className={common} fill="none" aria-hidden="true">
          <path
            d="M5 13l4 4L19 7"
            stroke="var(--success)"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "mastered":
      return (
        <svg viewBox="0 0 24 24" className={common} fill="var(--reward)" aria-hidden="true">
          <path d="M12 2l2.6 6.6L22 9.5l-5 4.9 1.2 7L12 18l-6.2 3.4L7 14.4l-5-4.9 7.4-.9L12 2z" />
        </svg>
      );
    case "needsReview":
      return (
        <svg viewBox="0 0 24 24" className={common} fill="none" aria-hidden="true">
          <path
            d="M4 12a8 8 0 1 1 2.3 5.7M4 12v5m0-5h5"
            stroke="var(--accent-secondary)"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    default:
      return null;
  }
}
