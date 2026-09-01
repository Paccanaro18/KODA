import { cn } from "@/lib/cn";

interface GemsProps {
  value: number;
  className?: string;
}

/**
 * Moeda do produto.
 *
 * Separada do XP de proposito, e a distincao e pedagogica, nao cosmetica: XP
 * mede esforco e nunca e gasto; gema e recompensa gastavel. Fundir as duas
 * transformaria progresso em saldo, e ai o aluno passa a otimizar a moeda em
 * vez do aprendizado.
 */
export function Gems({ value, className }: GemsProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 font-extrabold tabular-nums text-[var(--accent-secondary)]",
        className,
      )}
      title={`${value} gemas`}
    >
      <GemMark />
      <span>{value.toLocaleString("pt-BR")}</span>
      <span className="sr-only">gemas</span>
    </span>
  );
}

function GemMark() {
  return (
    <svg viewBox="0 0 24 24" className="size-6" fill="none" aria-hidden="true">
      {/* Corpo da gema */}
      <path
        d="M12 3l7 5.5-7 12.5L5 8.5 12 3z"
        fill="var(--accent-secondary)"
        stroke="var(--lip-accent-secondary)"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
      {/* Faceta clara: a gema so parece talhada se uma face pegar mais luz que
          a outra. Sem isso e um losango azul. */}
      <path d="M12 3l7 5.5-7 12.5V3z" fill="#ffffff" fillOpacity="0.18" />
      <path
        d="M5 8.5h14"
        stroke="var(--lip-accent-secondary)"
        strokeWidth="1.4"
      />
    </svg>
  );
}
