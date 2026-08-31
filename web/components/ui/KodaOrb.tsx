"use client";

import { cn } from "@/lib/cn";

export type OrbState = "idle" | "thinking" | "explaining" | "celebrating";

interface KodaOrbProps {
  state?: OrbState;
  size?: number;
  className?: string;
}

const STATE_LABELS: Record<OrbState, string> = {
  idle: "Koda AI",
  thinking: "Koda AI esta pensando",
  explaining: "Koda AI esta explicando",
  celebrating: "Koda AI esta comemorando",
};

/**
 * Identidade visual do Koda AI.
 *
 * Deliberadamente abstrato — um nucleo geometrico com orbitas, nao um robo nem
 * um mascote com rosto. A intencao e que o Koda AI seja lido como um mentor
 * presente, e nao como um chatbot; formas geometricas envelhecem melhor e nao
 * infantilizam um publico que vai do iniciante ao engenheiro experiente.
 *
 * Os estados se diferenciam por ritmo de movimento, nao apenas por cor.
 */
export function KodaOrb({ state = "idle", size = 40, className }: KodaOrbProps) {
  const accent =
    state === "celebrating" ? "var(--reward)" : "var(--accent)";
  const secondary =
    state === "celebrating" ? "var(--reward)" : "var(--accent-secondary)";

  return (
    <span
      className={cn("relative inline-grid place-items-center", className)}
      style={{ width: size, height: size }}
      role="img"
      aria-label={STATE_LABELS[state]}
    >
      {/* Halo: intensifica no "explaining", pulsa devagar no "thinking". */}
      <span
        className={cn(
          "absolute inset-0 rounded-full blur-md",
          state === "thinking" &&
            "motion-safe:animate-[koda-pulse-soft_1.6s_ease-in-out_infinite]",
          state === "celebrating" &&
            "motion-safe:animate-[koda-pulse-soft_0.7s_ease-in-out_infinite]",
        )}
        style={{
          backgroundColor: accent,
          opacity: state === "idle" ? 0.14 : 0.28,
        }}
        aria-hidden="true"
      />

      <svg
        viewBox="0 0 48 48"
        width={size}
        height={size}
        className="relative"
        aria-hidden="true"
      >
        {/* Orbita externa: gira continuamente enquanto o Koda pensa. */}
        <g
          className={cn(
            "origin-center",
            state === "thinking" &&
              "motion-safe:animate-[koda-orb-breathe_2.4s_ease-in-out_infinite]",
          )}
          style={{ transformOrigin: "center" }}
        >
          <circle
            cx="24"
            cy="24"
            r="19"
            fill="none"
            stroke={secondary}
            strokeWidth="1.5"
            strokeDasharray="6 10"
            opacity="0.7"
          />
        </g>

        {/* Anel intermediario */}
        <circle
          cx="24"
          cy="24"
          r="13"
          fill="none"
          stroke={accent}
          strokeWidth="2"
          opacity="0.85"
        />

        {/* Nucleo: losango, ecoando a marca do streak e dando unidade a
            linguagem geometrica do KODA. */}
        <path
          d="M24 15l6 9-6 9-6-9 6-9z"
          fill={accent}
          className={cn(
            state === "explaining" &&
              "motion-safe:animate-[koda-pulse-soft_1.2s_ease-in-out_infinite]",
            state === "celebrating" &&
              "motion-safe:animate-[koda-pop_var(--duration-celebration)_var(--ease-spring)]",
          )}
        />
      </svg>
    </span>
  );
}
