"use client";

import { cn } from "@/lib/cn";

export type KodaState =
  | "idle"
  | "thinking"
  | "explaining"
  | "celebrating"
  /** Depois de um erro. Nunca decepcionado — atento e do lado do aluno. */
  | "encouraging";

interface KodaProps {
  state?: KodaState;
  size?: number;
  className?: string;
}

const STATE_LABELS: Record<KodaState, string> = {
  idle: "Koda",
  thinking: "Koda esta pensando",
  explaining: "Koda esta explicando",
  celebrating: "Koda esta comemorando",
  encouraging: "Koda esta te incentivando",
};

/**
 * Koda — o personagem do produto.
 *
 * Substitui o orb geometrico abstrato que existia antes. A forma abstrata
 * envelhecia bem, mas nao criava vinculo: ninguem sente que uma figura
 * geometrica esta do seu lado quando erra. O calor de um produto de pratica
 * vem em boa parte de ter alguem ali.
 *
 * E uma capivara: bicho de silhueta simples (le bem em SVG chapado e em 24px),
 * culturalmente querido no Brasil, e distante o bastante da coruja para nao
 * parecer copia. O losango da marca virou o topete — a linguagem geometrica do
 * KODA continua no personagem em vez de competir com ele.
 *
 * Os estados se distinguem por RITMO e por EXPRESSAO, nunca so por cor:
 * quem nao percebe matiz ainda le a boca, o olho e o movimento.
 */

// Cores do personagem sao literais e nao usam tokens de tema: a pelagem da
// capivara nao muda entre claro e escuro, do mesmo jeito que a marca nao muda.
// Token semantico aqui produziria uma capivara azul no dark mode.
const FUR = "#b2794f";
const FUR_SHADE = "#96603c";
const MUZZLE = "#cf9a6c";
const INK = "#40291a";

export function Koda({ state = "idle", size = 48, className }: KodaProps) {
  const bodyAnimation = {
    idle: "motion-safe:animate-[koda-float_4s_ease-in-out_infinite]",
    thinking: "motion-safe:animate-[koda-wiggle_2.6s_ease-in-out_infinite]",
    explaining: "motion-safe:animate-[koda-float_2.2s_ease-in-out_infinite]",
    celebrating:
      "motion-safe:animate-[koda-bounce_0.9s_var(--ease-bounce)_infinite]",
    encouraging: "motion-safe:animate-[koda-float_3s_ease-in-out_infinite]",
  }[state];

  const isHappy = state === "celebrating";

  return (
    <span
      className={cn("relative inline-block shrink-0", className)}
      style={{ width: size, height: size }}
      role="img"
      aria-label={STATE_LABELS[state]}
    >
      <svg
        viewBox="0 0 64 64"
        width={size}
        height={size}
        className={cn("overflow-visible origin-bottom", bodyAnimation)}
        aria-hidden="true"
      >
        {/* Topete: o losango da marca. Unico elemento do personagem que usa
            token de tema — ele pertence ao KODA, nao a capivara. */}
        <path
          d="M32 3l3.4 5.6L32 14l-3.4-5.4L32 3z"
          fill="var(--accent)"
          className={cn(
            isHappy &&
              "motion-safe:animate-[koda-pulse-soft_0.6s_ease-in-out_infinite]",
          )}
        />

        {/* Orelhas */}
        <ellipse cx="15" cy="20" rx="6.5" ry="5.5" fill={FUR_SHADE} />
        <ellipse cx="15" cy="20" rx="3.4" ry="2.8" fill={MUZZLE} />
        <ellipse cx="49" cy="20" rx="6.5" ry="5.5" fill={FUR_SHADE} />
        <ellipse cx="49" cy="20" rx="3.4" ry="2.8" fill={MUZZLE} />

        {/* Cabeca: retangulo alto e arredondado. A capivara tem cara comprida,
            e e essa proporcao que a distingue de um urso generico. */}
        <rect x="11" y="14" width="42" height="42" rx="17" fill={FUR} />

        {/* Focinho */}
        <ellipse cx="32" cy="44" rx="14" ry="10.5" fill={MUZZLE} />

        {/* Sobrancelhas: sobem no incentivo e na comemoracao. Sao o que separa
            "atento" de "inexpressivo" — o olho sozinho nao carrega isso. */}
        {(state === "encouraging" || state === "explaining") && (
          <>
            <path
              d="M17 25.5c2.5-2 6-2 8.5-.5"
              stroke={FUR_SHADE}
              strokeWidth="2.4"
              strokeLinecap="round"
              fill="none"
            />
            <path
              d="M47 25.5c-2.5-2-6-2-8.5-.5"
              stroke={FUR_SHADE}
              strokeWidth="2.4"
              strokeLinecap="round"
              fill="none"
            />
          </>
        )}

        {/* Olhos. Na comemoracao viram arcos fechados — a alegria e legivel na
            forma, sem depender de cor nem de movimento. */}
        {isHappy ? (
          <>
            <path
              d="M18.5 33c1.8-3 5.2-3 7 0"
              stroke={INK}
              strokeWidth="3"
              strokeLinecap="round"
              fill="none"
            />
            <path
              d="M38.5 33c1.8-3 5.2-3 7 0"
              stroke={INK}
              strokeWidth="3"
              strokeLinecap="round"
              fill="none"
            />
          </>
        ) : (
          <>
            <Eye cx={22} cy={32} state={state} />
            <Eye cx={42} cy={32} state={state} delay="0.15s" />
          </>
        )}

        {/* Narinas */}
        <ellipse cx="27.5" cy="41" rx="2.1" ry="1.7" fill={INK} />
        <ellipse cx="36.5" cy="41" rx="2.1" ry="1.7" fill={INK} />

        {/* Boca */}
        <Mouth state={state} />

        {/* Bolhas de raciocinio: so no "thinking", e a unica pista de que ele
            esta ocupado. Ritmo escalonado para nao piscarem em bloco. */}
        {state === "thinking" && (
          <>
            <circle
              cx="55"
              cy="12"
              r="2.4"
              fill="var(--accent-secondary)"
              className="motion-safe:animate-[koda-pulse-soft_1.2s_ease-in-out_infinite]"
            />
            <circle
              cx="60"
              cy="6"
              r="1.6"
              fill="var(--accent-secondary)"
              className="motion-safe:animate-[koda-pulse-soft_1.2s_ease-in-out_infinite]"
              style={{ animationDelay: "0.4s" }}
            />
          </>
        )}
      </svg>
    </span>
  );
}

/**
 * Olho com piscada.
 *
 * `transformBox: fill-box` faz o `transform-origin: center` valer o centro do
 * proprio olho. Sem isso o SVG usa a origem do viewBox e a piscada vira um
 * olho escorregando pela cara.
 */
function Eye({
  cx,
  cy,
  state,
  delay,
}: {
  cx: number;
  cy: number;
  state: KodaState;
  delay?: string;
}) {
  return (
    <g
      className="motion-safe:animate-[koda-blink_5.5s_ease-in-out_infinite]"
      style={{
        transformBox: "fill-box",
        transformOrigin: "center",
        animationDelay: delay,
      }}
    >
      <circle cx={cx} cy={cy} r="4.6" fill={INK} />
      {/* O brilho e o que da vida ao olho. Sem ele, dois circulos pretos leem
          como botao, nao como olhar. */}
      <circle cx={cx + 1.6} cy={cy - 1.6} r="1.5" fill="#ffffff" />
      {state === "thinking" && (
        <circle cx={cx + 2.4} cy={cy - 2.6} r="0.7" fill="#ffffff" />
      )}
    </g>
  );
}

function Mouth({ state }: { state: KodaState }) {
  switch (state) {
    case "celebrating":
      // Boca aberta, sorriso largo.
      return (
        <path
          d="M26 47c1.5 4 10.5 4 12 0z"
          fill={INK}
          stroke={INK}
          strokeWidth="2"
          strokeLinejoin="round"
        />
      );
    case "explaining":
      // Boca entreaberta: ele esta falando.
      return <ellipse cx="32" cy="48" rx="3.6" ry="2.8" fill={INK} />;
    case "encouraging":
      // Sorriso pequeno e torto, de "tudo bem, vamos de novo".
      return (
        <path
          d="M28 47.5c2 2 5 2.2 7.5.2"
          stroke={INK}
          strokeWidth="2.2"
          strokeLinecap="round"
          fill="none"
        />
      );
    default:
      return (
        <path
          d="M28 47c2.4 2.4 5.6 2.4 8 0"
          stroke={INK}
          strokeWidth="2.2"
          strokeLinecap="round"
          fill="none"
        />
      );
  }
}
