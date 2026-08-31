import { cn } from "@/lib/cn";
import type { HTMLAttributes, ReactNode } from "react";

type Tone = "neutral" | "accent" | "success" | "error" | "reward" | "info";

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: Tone;
  icon?: ReactNode;
}

const TONES: Record<Tone, string> = {
  neutral:
    "bg-[var(--bg-surface-raised)] text-[var(--text-secondary)] border-[var(--border-subtle)]",
  accent: "bg-[var(--accent-soft)] text-[var(--accent)] border-transparent",
  success: "bg-[var(--success-soft)] text-[var(--success)] border-transparent",
  error: "bg-[var(--error-soft)] text-[var(--error)] border-transparent",
  reward: "bg-[var(--reward-soft)] text-[var(--reward)] border-transparent",
  info: "bg-[var(--bg-inset)] text-[var(--accent-secondary)] border-transparent",
};

export function Badge({
  tone = "neutral",
  icon,
  className,
  children,
  ...props
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 border",
        "px-2 py-0.5 rounded-full text-xs font-medium whitespace-nowrap",
        TONES[tone],
        className,
      )}
      {...props}
    >
      {icon}
      {children}
    </span>
  );
}
