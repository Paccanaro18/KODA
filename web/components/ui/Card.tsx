import { cn } from "@/lib/cn";
import type { HTMLAttributes } from "react";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** Eleva a superficie e habilita resposta ao hover. Use apenas quando o card for clicavel. */
  interactive?: boolean;
  /** Destaca o card como acao principal da tela. */
  highlighted?: boolean;
}

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
        "bg-[var(--bg-surface)] border border-[var(--border-subtle)]",
        "rounded-[var(--radius-lg)] p-5",
        "transition-[transform,box-shadow,border-color]",
        "duration-[var(--duration-base)] ease-[var(--ease-standard)]",
        interactive &&
          "cursor-pointer hover:-translate-y-0.5 hover:shadow-[var(--shadow-md)] hover:border-[var(--border-strong)]",
        highlighted && "border-[var(--accent)] shadow-[var(--shadow-md)]",
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
    <h3 className={cn("text-lg font-semibold", className)} {...props}>
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
