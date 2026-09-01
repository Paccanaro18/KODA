import { cn } from "@/lib/cn";
import type { ReactNode } from "react";

interface ProgressProps {
  /** 0 a 100. Valores fora da faixa sao truncados. */
  value: number;
  label?: string;
  /** Mostra a porcentagem numerica ao lado do rotulo. */
  showValue?: boolean;
  tone?: "accent" | "success" | "reward";
  className?: string;
}

const TONES = {
  accent: "var(--accent)",
  success: "var(--success)",
  reward: "var(--reward)",
} as const;

export function Progress({
  value,
  label,
  showValue = false,
  tone = "accent",
  className,
}: ProgressProps) {
  const clamped = Math.min(100, Math.max(0, value));

  return (
    <div className={cn("w-full", className)}>
      {(label || showValue) && (
        <div className="flex justify-between items-baseline mb-1.5">
          {label && (
            <span className="text-sm text-[var(--text-secondary)]">{label}</span>
          )}
          {showValue && (
            <span className="text-sm font-semibold tabular-nums">
              {Math.round(clamped)}%
            </span>
          )}
        </div>
      )}

      {/* Barra alta (16px). O fio de 8px lia como enfeite de dashboard; nesta
          altura ela vira o placar da sessao, que e o papel que tem de fato. */}
      <div
        className="h-4 w-full rounded-full bg-[var(--bg-inset)] overflow-hidden"
        // A barra e semanticamente um progressbar: leitores de tela anunciam o
        // valor. Sem isso, o progresso existiria apenas como cor na tela.
        role="progressbar"
        aria-valuenow={Math.round(clamped)}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={label ?? "Progresso"}
      >
        <div
          className="relative h-full rounded-full transition-[width] duration-[var(--duration-slow)] ease-[var(--ease-standard)]"
          style={{ width: `${clamped}%`, backgroundColor: TONES[tone] }}
        >
          {/* Brilho interno: uma faixa clara no terco de cima. E o que faz a
              barra parecer um tubo preenchido em vez de um retangulo colorido. */}
          <span
            className="absolute inset-x-1.5 top-[3px] h-[4px] rounded-full bg-white/35"
            aria-hidden="true"
          />
        </div>
      </div>
    </div>
  );
}

interface ProgressRingProps {
  value: number;
  size?: number;
  strokeWidth?: number;
  tone?: "accent" | "success" | "reward";
  /** Conteudo central. Se omitido, mostra a porcentagem. */
  children?: ReactNode;
  className?: string;
}

export function ProgressRing({
  value,
  size = 64,
  strokeWidth = 6,
  tone = "accent",
  children,
  className,
}: ProgressRingProps) {
  const clamped = Math.min(100, Math.max(0, value));
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  // O traco e desenhado inteiro e "escondido" pelo offset — e assim que o
  // stroke-dasharray produz um arco proporcional ao valor.
  const offset = circumference - (clamped / 100) * circumference;

  return (
    <div
      className={cn("relative inline-grid place-items-center", className)}
      style={{ width: size, height: size }}
      role="progressbar"
      aria-valuenow={Math.round(clamped)}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <svg width={size} height={size} className="-rotate-90" aria-hidden="true">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="var(--bg-inset)"
          strokeWidth={strokeWidth}
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={TONES[tone]}
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          className="transition-[stroke-dashoffset] duration-[var(--duration-slow)] ease-[var(--ease-standard)]"
        />
      </svg>

      <span className="absolute text-sm font-semibold tabular-nums">
        {children ?? `${Math.round(clamped)}%`}
      </span>
    </div>
  );
}
