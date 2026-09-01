"use client";

import { cn } from "@/lib/cn";

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
  /** Mostra o balao "COMECAR" acima do no. Use apenas no proximo passo do aluno. */
  showCallout?: boolean;
  onClick?: () => void;
}

/**
 * No do mapa de aprendizado.
 *
 * Um disco solido e volumoso, nao um cartao com contorno. O aluno vai olhar
 * essa peca centenas de vezes: ela precisa parecer um botao fisico que pede
 * para ser apertado, e nao um item de lista.
 *
 * Cada estado se distingue por forma, icone e preenchimento — nao apenas por
 * cor. Um mapa que dependesse so do matiz seria ilegivel para daltonicos, e e
 * a principal peca de navegacao do produto.
 */
const STATE_STYLES: Record<SkillState, string> = {
  // Bloqueado: chapado, sem labio, sem volume. Nao e apertavel e parece nao ser.
  locked:
    "bg-[var(--bg-inset)] text-[var(--text-muted)] border-[var(--border-subtle)] shadow-none",
  available:
    "bg-[var(--action)] text-white border-[var(--lip-action)] shadow-[0_6px_0_var(--lip-action)]",
  active:
    "bg-[var(--action)] text-white border-[var(--lip-action)] shadow-[0_6px_0_var(--lip-action)]",
  completed:
    "bg-[var(--reward)] text-[#5c4300] border-[var(--lip-reward)] shadow-[0_6px_0_var(--lip-reward)]",
  mastered:
    "bg-[var(--accent)] text-white border-[var(--lip-accent)] shadow-[0_6px_0_var(--lip-accent)]",
  needsReview:
    "bg-[var(--accent-secondary)] text-white border-[var(--lip-accent-secondary)] shadow-[0_6px_0_var(--lip-accent-secondary)]",
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
  showCallout = false,
  onClick,
}: SkillNodeProps) {
  const isLocked = state === "locked";
  const showRing = state === "active" || state === "needsReview";

  return (
    <div className="flex flex-col items-center gap-2 w-32">
      {showCallout && !isLocked && (
        <Callout label={state === "needsReview" ? "Revisar" : "Comecar"} />
      )}

      <div className="relative grid place-items-center">
        {/* Anel de progresso por fora do disco. Fica atras do botao para nao
            roubar a area de clique. */}
        {showRing && <ProgressArc value={progress} />}

        <button
          type="button"
          onClick={onClick}
          disabled={isLocked}
          className={cn(
            "relative grid place-items-center size-[68px] rounded-full border-b-0 border-2",
            "transition-[transform,box-shadow,filter]",
            "duration-[var(--duration-press)] ease-[var(--ease-standard)]",
            !isLocked &&
              "cursor-pointer hover:brightness-105 active:translate-y-[6px] active:shadow-none",
            isLocked && "cursor-not-allowed",
            STATE_STYLES[state],
          )}
          // O estado vai junto do titulo no rotulo acessivel: quem usa leitor de
          // tela recebe a mesma informacao que a cor e o icone transmitem.
          aria-label={`${title} — ${STATE_LABELS[state]}`}
        >
          <StateIcon state={state} />
        </button>
      </div>

      <span
        className={cn(
          "text-xs font-extrabold text-center leading-tight uppercase tracking-[0.04em]",
          isLocked ? "text-[var(--text-muted)]" : "text-[var(--text-secondary)]",
        )}
      >
        {title}
      </span>
    </div>
  );
}

/**
 * Balao "COMECAR" acima do proximo no.
 *
 * Duolingo resolve com isso um problema real: num caminho longo, o aluno perde
 * dois segundos procurando onde parou. O balao elimina a busca — e esses dois
 * segundos, repetidos todo dia, sao atrito de verdade.
 */
function Callout({ label }: { label: string }) {
  return (
    <span className="relative motion-safe:animate-[koda-float_2.4s_ease-in-out_infinite]">
      <span className="block px-3 py-1.5 rounded-[var(--radius-md)] bg-[var(--bg-surface)] border-2 border-[var(--border-subtle)] shadow-[0_3px_0_var(--lip-neutral)] text-xs font-extrabold uppercase tracking-[0.06em] text-[var(--action)]">
        {label}
      </span>
      {/* Bico do balao, girado 45 graus e sem as duas bordas de tras. */}
      <span
        className="absolute left-1/2 -bottom-[7px] size-3 -translate-x-1/2 rotate-45 bg-[var(--bg-surface)] border-r-2 border-b-2 border-[var(--border-subtle)]"
        aria-hidden="true"
      />
    </span>
  );
}

/** Arco de progresso desenhado por fora do disco. */
function ProgressArc({ value }: { value: number }) {
  const size = 88;
  const radius = 40;
  const circumference = 2 * Math.PI * radius;
  const clamped = Math.min(100, Math.max(0, value));

  return (
    <svg
      width={size}
      height={size}
      viewBox={`0 0 ${size} ${size}`}
      className="absolute -rotate-90"
      aria-hidden="true"
    >
      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        fill="none"
        stroke="var(--bg-inset)"
        strokeWidth="6"
      />
      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        fill="none"
        stroke="var(--reward)"
        strokeWidth="6"
        strokeLinecap="round"
        strokeDasharray={circumference}
        strokeDashoffset={circumference * (1 - clamped / 100)}
        className="transition-[stroke-dashoffset] duration-[var(--duration-slow)] ease-[var(--ease-standard)]"
      />
    </svg>
  );
}

function StateIcon({ state }: { state: SkillState }) {
  const common = "size-8";

  switch (state) {
    case "locked":
      return (
        <svg viewBox="0 0 24 24" className={common} fill="none" aria-hidden="true">
          <rect
            x="5"
            y="11"
            width="14"
            height="10"
            rx="3"
            fill="currentColor"
          />
          <path
            d="M8 11V7a4 4 0 118 0v4"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
          />
        </svg>
      );
    case "completed":
      return (
        <svg viewBox="0 0 24 24" className={common} fill="none" aria-hidden="true">
          <path
            d="M5 13l4 4L19 7"
            stroke="currentColor"
            strokeWidth="3.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    case "mastered":
      return (
        <svg viewBox="0 0 24 24" className={common} fill="currentColor" aria-hidden="true">
          <path d="M12 2l2.9 6.3 6.9.8-5.1 4.7 1.4 6.8L12 17.3 5.9 20.6l1.4-6.8L2.2 9.1l6.9-.8L12 2z" />
        </svg>
      );
    case "needsReview":
      return (
        <svg viewBox="0 0 24 24" className={common} fill="none" aria-hidden="true">
          <path
            d="M4 12a8 8 0 1 1 2.3 5.7M4 12v5m0-5h5"
            stroke="currentColor"
            strokeWidth="2.6"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      );
    default:
      // Disponivel e em andamento: estrela vazada. A mesma forma do "dominado",
      // so que aberta — a progressao da forma conta a historia do no.
      return (
        <svg viewBox="0 0 24 24" className={common} fill="none" aria-hidden="true">
          <path
            d="M12 3l2.7 5.8 6.3.7-4.7 4.3 1.3 6.2L12 16.9 6.4 20l1.3-6.2L3 9.5l6.3-.7L12 3z"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinejoin="round"
          />
        </svg>
      );
  }
}
