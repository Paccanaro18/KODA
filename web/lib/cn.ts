import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Combina classes condicionais resolvendo conflitos do Tailwind.
 *
 * Sem o merge, `cn("px-4", "px-6")` deixaria as duas classes no elemento e o
 * resultado dependeria da ordem no CSS gerado — o que quebra a capacidade de
 * um componente sobrescrever o estilo padrao via `className`.
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
