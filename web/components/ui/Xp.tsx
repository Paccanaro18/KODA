"use client";

import { cn } from "@/lib/cn";
import { usePrefersReducedMotion } from "@/lib/usePrefersReducedMotion";
import { useEffect, useRef, useState } from "react";

interface XpCounterProps {
  value: number;
  className?: string;
}

/**
 * Contador de XP que sobe suavemente ate o valor alvo.
 *
 * A contagem animada existe porque ver o numero subir comunica conquista de um
 * jeito que trocar "120" por "135" instantaneamente nao comunica.
 *
 * Sob prefers-reduced-motion o valor e exibido direto, sem estado intermediario:
 * a preferencia e lida durante o render, e nao "corrigida" depois por um efeito.
 */
export function XpCounter({ value, className }: XpCounterProps) {
  const prefersReducedMotion = usePrefersReducedMotion();
  const [animatedValue, setAnimatedValue] = useState(value);
  const frameRef = useRef<number | null>(null);
  const previousRef = useRef(value);

  useEffect(() => {
    const from = previousRef.current;
    const to = value;
    previousRef.current = value;

    // Sem animacao: o render ja mostra `value` diretamente.
    if (from === to || prefersReducedMotion) {
      return;
    }

    const duration = 600;
    const start = performance.now();

    const tick = (now: number) => {
      const progress = Math.min(1, (now - start) / duration);
      // easeOutCubic: rapido no inicio e desacelerando, que e como a percepcao
      // de "chegando ao valor" funciona melhor.
      const eased = 1 - Math.pow(1 - progress, 3);
      // setState aqui e legitimo: acontece no callback do requestAnimationFrame,
      // nao no corpo do efeito.
      setAnimatedValue(Math.round(from + (to - from) * eased));

      if (progress < 1) {
        frameRef.current = requestAnimationFrame(tick);
      }
    };

    frameRef.current = requestAnimationFrame(tick);

    return () => {
      if (frameRef.current !== null) cancelAnimationFrame(frameRef.current);
    };
  }, [value, prefersReducedMotion]);

  const display = prefersReducedMotion ? value : animatedValue;

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 font-extrabold tabular-nums",
        className,
      )}
    >
      <BoltIcon />
      <span>{display.toLocaleString("pt-BR")}</span>
      <span className="text-[var(--text-secondary)] font-bold text-sm">XP</span>
    </span>
  );
}

interface XpGainProps {
  amount: number;
  className?: string;
}

/** Indicador efemero de ganho, exibido logo apos uma resposta correta. */
export function XpGain({ amount, className }: XpGainProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 px-2 py-0.5 rounded-full",
        "bg-[var(--reward)] text-[#5c4300] text-sm font-extrabold",
        "motion-safe:animate-[koda-xp-float_var(--duration-celebration)_var(--ease-standard)]",
        className,
      )}
    >
      +{amount} XP
    </span>
  );
}

function BoltIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      className="size-4 text-[var(--reward)]"
      fill="currentColor"
      aria-hidden="true"
    >
      <path d="M13 2L4.5 13.5H11l-1 8.5 8.5-11.5H12l1-8.5z" />
    </svg>
  );
}
