import { cn } from "@/lib/cn";
import type { ReactNode } from "react";
import { KodaOrb } from "./KodaOrb";

interface FeedbackProps {
  correct: boolean;
  /** O que o aluno acertou ou onde exatamente o raciocinio saiu do trilho. */
  explanation: ReactNode;
  /** Conceito exercitado, exibido como reforco pedagogico. */
  concept?: string;
  /** Dica mostrada apos um erro. */
  hint?: ReactNode;
  /** Recomendacao de proximo passo. */
  nextStep?: ReactNode;
  className?: string;
}

/**
 * Feedback pos-resposta.
 *
 * O tom no erro e deliberado: nada de "Errado" ou de linguagem de fracasso. A
 * mensagem trata o erro como descoberta, porque e exatamente isso que ele e do
 * ponto de vista do modelo de conhecimento — informacao nova sobre uma lacuna.
 * Um aluno que se sente punido para de praticar, e ai nao ha aprendizado nenhum.
 */
export function Feedback({
  correct,
  explanation,
  concept,
  hint,
  nextStep,
  className,
}: FeedbackProps) {
  return (
    <div
      className={cn(
        "flex gap-3 p-4 rounded-[var(--radius-lg)] border-2",
        "motion-safe:animate-[koda-rise_var(--duration-base)_var(--ease-standard)]",
        correct
          ? "border-[var(--success)] bg-[var(--success-soft)]"
          : "border-[var(--accent-secondary)] bg-[var(--bg-surface-raised)]",
        className,
      )}
      // Anuncia o feedback assim que aparece, sem exigir que o usuario navegue
      // ate ele. É a informacao mais importante do momento.
      role="status"
      aria-live="polite"
    >
      <KodaOrb
        state={correct ? "celebrating" : "explaining"}
        size={36}
        className="shrink-0 mt-0.5"
      />

      <div className="min-w-0 space-y-2">
        <p
          className={cn(
            "font-semibold",
            correct ? "text-[var(--success)]" : "text-[var(--text-primary)]",
          )}
        >
          {correct ? "Correto." : "Nao exatamente."}
        </p>

        <div className="text-sm text-[var(--text-secondary)] leading-relaxed">
          {explanation}
        </div>

        {concept && (
          <p className="text-sm">
            <span className="text-[var(--text-muted)]">Conceito reforcado: </span>
            <span className="font-medium">{concept}</span>
          </p>
        )}

        {hint && (
          <div className="flex gap-2 text-sm p-2.5 rounded-[var(--radius-sm)] bg-[var(--bg-inset)]">
            <LightbulbIcon />
            <div className="text-[var(--text-secondary)]">{hint}</div>
          </div>
        )}

        {nextStep && (
          <p className="text-sm">
            <span className="text-[var(--text-muted)]">Proximo passo: </span>
            <span>{nextStep}</span>
          </p>
        )}
      </div>
    </div>
  );
}

function LightbulbIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      className="size-4 shrink-0 mt-0.5 text-[var(--reward)]"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M9 18h6M10 21h4M12 3a6 6 0 00-3.5 10.9c.3.2.5.6.5 1V15h6v-.1c0-.4.2-.8.5-1A6 6 0 0012 3z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
