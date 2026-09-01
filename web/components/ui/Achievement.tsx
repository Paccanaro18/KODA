import { cn } from "@/lib/cn";
import type { ReactNode } from "react";

interface AchievementProps {
  title: string;
  description: string;
  unlocked?: boolean;
  icon?: ReactNode;
  className?: string;
}

/**
 * Conquista do estudante.
 *
 * Bloqueada aparece em silhueta, nao apenas esmaecida: a diferenca precisa ser
 * obvia de relance, e a silhueta ainda comunica que ha algo a conquistar ali.
 */
export function Achievement({
  title,
  description,
  unlocked = false,
  icon,
  className,
}: AchievementProps) {
  return (
    <div
      className={cn(
        "flex items-center gap-3 p-3.5 rounded-[var(--radius-lg)] border-2",
        unlocked
          ? "border-[var(--reward)] bg-[var(--reward-soft)]"
          : "border-[var(--border-subtle)] bg-[var(--bg-surface-raised)]",
        className,
      )}
    >
      <span
        className={cn(
          "shrink-0 grid place-items-center size-12 rounded-full",
          unlocked
            ? "bg-[var(--reward)] text-[#5c4300] shadow-[0_3px_0_var(--lip-reward)]"
            : "bg-[var(--bg-inset)] text-[var(--text-muted)]",
        )}
        aria-hidden="true"
      >
        {icon ?? <MedalIcon />}
      </span>

      <span className="min-w-0">
        <span
          className={cn(
            "block text-sm font-extrabold truncate",
            !unlocked && "text-[var(--text-secondary)]",
          )}
        >
          {title}
        </span>
        <span className="block text-xs text-[var(--text-secondary)]">
          {description}
        </span>
      </span>

      {/* Estado tambem em texto, nao so em cor e preenchimento. */}
      <span className="sr-only">
        {unlocked ? "Conquistado" : "Ainda nao conquistado"}
      </span>
    </div>
  );
}

function MedalIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-5" fill="currentColor" aria-hidden="true">
      <path d="M12 2l2.6 6.6L22 9.5l-5 4.9 1.2 7L12 18l-6.2 3.4L7 14.4l-5-4.9 7.4-.9L12 2z" />
    </svg>
  );
}
