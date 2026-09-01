import { cn } from "@/lib/cn";
import type { HTMLAttributes } from "react";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** Da labio ao card e habilita resposta ao hover. Use apenas quando for clicavel. */
  interactive?: boolean;
  /** Destaca o card como acao principal da tela. */
  highlighted?: boolean;
}

/**
 * Superficie de conteudo.
 *
 * Borda de 2px em vez de 1px: com raio grande, o fio de 1px some e o card
 * parece um retangulo esmaecido. A borda grossa e o que da contorno de objeto.
 *
 * Card clicavel ganha o mesmo labio dos botoes — se afunda quando pressionado,
 * o usuario aprende sem instrucao que aquilo e apertavel.
 */
export function Card({
  interactive = false,
  highlighted = false,
  className,
  children,
  ...props
}: CardProps) {
  return (
    <div
      className={cn(
        "bg-[var(--bg-surface)] border-2 border-[var(--border-subtle)]",
        "rounded-[var(--radius-xl)] p-5",
        "transition-[transform,box-shadow,border-color,background-color]",
        "duration-[var(--duration-fast)] ease-[var(--ease-standard)]",
        interactive &&
          "cursor-pointer shadow-[0_4px_0_var(--lip-neutral)] hover:bg-[var(--bg-surface-raised)] active:shadow-[0_0_0_var(--lip-neutral)] active:translate-y-[4px]",
        highlighted &&
          "border-[var(--accent)] shadow-[0_4px_0_var(--lip-accent)]",
        className,
      )}
      {...props}
    >
      {children}
    </div>
  );
}

export function CardTitle({
  className,
  children,
  ...props
}: HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h3 className={cn("text-lg font-extrabold", className)} {...props}>
      {children}
    </h3>
  );
}

export function CardDescription({
  className,
  children,
  ...props
}: HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p
      className={cn("text-sm text-[var(--text-secondary)]", className)}
      {...props}
    >
      {children}
    </p>
  );
}
