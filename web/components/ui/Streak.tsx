import { cn } from "@/lib/cn";

interface StreakProps {
  days: number;
  /** Marca o dia de hoje como ainda nao praticado. */
  atRisk?: boolean;
  size?: "sm" | "md";
  className?: string;
}

/**
 * Sequencia de dias praticados.
 *
 * A identidade e um losango com nucleo — geometrico e proprio do KODA, em vez
 * do emoji de chama que virou lugar-comum em produtos educacionais.
 */
export function Streak({
  days,
  atRisk = false,
  size = "md",
  className,
}: StreakProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 font-semibold tabular-nums",
        size === "sm" ? "text-sm" : "text-base",
        className,
      )}
      title={
        atRisk
          ? "Pratique hoje para manter sua sequencia"
          : `Sequencia de ${days} dias`
      }
    >
      <StreakMark active={!atRisk} size={size} />
      <span>{days}</span>
      <span className="font-normal text-[var(--text-secondary)] text-sm">
        {days === 1 ? "dia" : "dias"}
      </span>
    </span>
  );
}

function StreakMark({
  active,
  size,
}: {
  active: boolean;
  size: "sm" | "md";
}) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={cn(size === "sm" ? "size-4" : "size-5")}
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M12 2l7 10-7 10-7-10 7-10z"
        // Sequencia em risco fica apenas contornada; ativa fica preenchida.
        // A diferenca de forma sobrevive a qualquer tipo de daltonismo.
        fill={active ? "var(--reward)" : "none"}
        stroke={active ? "var(--reward)" : "var(--text-muted)"}
        strokeWidth="2"
        strokeLinejoin="round"
      />
      {active && <circle cx="12" cy="12" r="2.5" fill="var(--bg-canvas)" />}
    </svg>
  );
}
