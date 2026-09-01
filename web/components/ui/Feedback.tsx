import { cn } from "@/lib/cn";
import type { ReactNode } from "react";
import { Koda } from "./Koda";

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
  /** Botao de avancar. Fica a direita no desktop e no rodape no mobile. */
  action?: ReactNode;
  /**
   * Ancora a barra no rodape da janela, como painel que sobe. E a forma usada
   * dentro do exercicio; fora dele o painel e apenas um bloco no fluxo.
   */
  docked?: boolean;
  className?: string;
}

/**
 * Feedback pos-resposta.
 *
 * O tom no erro e deliberado: nada de "Errado" ou de linguagem de fracasso. A
 * mensagem trata o erro como descoberta, porque e exatamente isso que ele e do
 * ponto de vista do modelo de conhecimento — informacao nova sobre uma lacuna.
 * Um aluno que se sente punido para de praticar, e ai nao ha aprendizado nenhum.
 *
 * O painel ocupa a largura toda e tem cor de fundo propria: o momento em que o
 * aluno descobre se acertou e o pico emocional do exercicio, e um cartaozinho
 * discreto no meio da pagina desperdica esse pico.
 */
export function Feedback({
  correct,
  explanation,
  concept,
  hint,
  nextStep,
  action,
  docked = false,
  className,
}: FeedbackProps) {
  return (
    <div
      className={cn(
        "border-t-2",
        correct
          ? "bg-[var(--success-soft)] border-[var(--success)]"
          : "bg-[var(--error-soft)] border-[var(--error)]",
        docked
          ? "fixed inset-x-0 bottom-0 z-30 motion-safe:animate-[koda-slide-up_var(--duration-base)_var(--ease-standard)]"
          : "rounded-[var(--radius-lg)] border-2 motion-safe:animate-[koda-rise_var(--duration-base)_var(--ease-standard)]",
        className,
      )}
      // Anuncia o feedback assim que aparece, sem exigir que o usuario navegue
      // ate ele. E a informacao mais importante do momento.
      role="status"
      aria-live="polite"
    >
      <div className="mx-auto max-w-3xl w-full px-5 py-5 flex flex-col gap-4 sm:flex-row sm:items-center">
        <Koda
          state={correct ? "celebrating" : "encouraging"}
          size={64}
          className="shrink-0"
        />

        <div className="min-w-0 flex-1 space-y-2">
          <p
            className={cn(
              "text-xl font-extrabold",
              correct
                ? "text-[var(--lip-success)]"
                : "text-[var(--lip-error)]",
            )}
          >
            {correct ? "Boa! Acertou." : "Quase la."}
          </p>

          <div className="text-sm text-[var(--text-secondary)] leading-relaxed">
            {explanation}
          </div>

          {concept && (
            <p className="text-sm">
              <span className="text-[var(--text-muted)]">Conceito reforcado: </span>
              <span className="font-bold">{concept}</span>
            </p>
          )}

          {hint && (
            <div className="flex gap-2 text-sm p-3 rounded-[var(--radius-md)] bg-[var(--bg-surface)]/70">
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

        {action && <div className="shrink-0 sm:self-center">{action}</div>}
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
