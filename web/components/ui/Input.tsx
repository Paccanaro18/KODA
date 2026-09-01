import { cn } from "@/lib/cn";
import { useId, type InputHTMLAttributes } from "react";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

/**
 * Campo de formulario com rotulo e erro.
 *
 * O erro muda a borda E aparece como texto — nunca so a cor, pelo mesmo
 * motivo do resto do sistema (ver Feedback, SkillNode).
 */
export function Input({ label, error, className, id, ...props }: InputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = error ? `${inputId}-error` : undefined;

  return (
    <div className="w-full">
      <label
        htmlFor={inputId}
        className="block mb-1.5 text-sm font-extrabold text-[var(--text-secondary)]"
      >
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={!!error}
        aria-describedby={errorId}
        className={cn(
          "w-full min-h-12 px-4 rounded-[var(--radius-md)] border-2 bg-[var(--bg-surface)]",
          "text-base placeholder:text-[var(--text-muted)]",
          "transition-colors duration-[var(--duration-fast)]",
          "focus:outline-none focus:border-[var(--accent)]",
          error ? "border-[var(--error)]" : "border-[var(--border-subtle)]",
          className,
        )}
        {...props}
      />
      {error && (
        <p id={errorId} className="mt-1.5 text-sm font-semibold text-[var(--error)]">
          {error}
        </p>
      )}
    </div>
  );
}
