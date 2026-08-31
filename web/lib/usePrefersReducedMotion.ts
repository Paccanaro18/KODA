"use client";

import { useSyncExternalStore } from "react";

const QUERY = "(prefers-reduced-motion: reduce)";

function subscribe(onChange: () => void): () => void {
  const mediaQuery = window.matchMedia(QUERY);
  mediaQuery.addEventListener("change", onChange);
  return () => mediaQuery.removeEventListener("change", onChange);
}

function getSnapshot(): boolean {
  return window.matchMedia(QUERY).matches;
}

/** No servidor assume-se movimento permitido; o cliente corrige na hidratacao. */
function getServerSnapshot(): boolean {
  return false;
}

/**
 * Le a preferencia de movimento reduzido do sistema.
 *
 * Usa useSyncExternalStore porque a media query e estado externo ao React —
 * ler com useEffect + setState provocaria render em cascata e violaria a
 * regra react-hooks/set-state-in-effect.
 *
 * O CSS ja neutraliza transicoes e animacoes declarativas via
 * @media (prefers-reduced-motion). Este hook cobre o que o CSS nao alcanca:
 * animacao imperativa em JavaScript, como a contagem de XP via
 * requestAnimationFrame.
 */
export function usePrefersReducedMotion(): boolean {
  return useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
}
