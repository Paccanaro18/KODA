import { cn } from "@/lib/cn";

interface HeartsProps {
  /** Vidas restantes. */
  value: number;
  /** Total de vidas. Acima disso o componente passa a mostrar so o numero. */
  max?: number;
  /** Some com os corações individuais e mostra apenas icone + contagem. */
  compact?: boolean;
  className?: string;
}

/**
 * Vidas da sessao.
 *
 * Um limite de erros so funciona como tensao se o aluno enxergar o custo ANTES
 * de responder. Numero puro nao produz isso; um coracao que apaga, sim — a
 * perda vira uma imagem, e nao uma conta.
 *
 * O coracao vazio fica CONTORNADO, nunca apenas mais claro: a diferenca entre
 * cheio e vazio precisa sobreviver a qualquer tipo de daltonismo, e forma
 * sempre sobrevive.
 */
export function Hearts({ value, max = 5, compact = false, className }: HeartsProps) {
  const remaining = Math.max(0, Math.min(max, value));

  if (compact || max > 6) {
    return (
      <span
        className={cn(
          "inline-flex items-center gap-1.5 font-extrabold tabular-nums text-[var(--error)]",
          className,
        )}
        title={`${remaining} de ${max} vidas`}
      >
        <HeartMark filled={remaining > 0} />
        <span>{remaining}</span>
        <span className="sr-only">de {max} vidas restantes</span>
      </span>
    );
  }

  return (
    <span
      className={cn("inline-flex items-center gap-1", className)}
      title={`${remaining} de ${max} vidas`}
    >
      {Array.from({ length: max }, (_, index) => (
        <HeartMark key={index} filled={index < remaining} />
      ))}
      <span className="sr-only">{remaining} de {max} vidas restantes</span>
    </span>
  );
}

function HeartMark({ filled }: { filled: boolean }) {
  return (
    <svg viewBox="0 0 24 24" className="size-6" fill="none" aria-hidden="true">
      <path
        d="M12 20.5S3.5 15 3.5 9.2A4.7 4.7 0 0 1 12 6.5a4.7 4.7 0 0 1 8.5 2.7c0 5.8-8.5 11.3-8.5 11.3z"
        fill={filled ? "var(--error)" : "none"}
        stroke={filled ? "var(--lip-error)" : "var(--border-strong)"}
        strokeWidth="2"
        strokeLinejoin="round"
      />
    </svg>
  );
}
