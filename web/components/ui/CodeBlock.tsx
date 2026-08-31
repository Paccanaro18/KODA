import { cn } from "@/lib/cn";

interface CodeBlockProps {
  code: string;
  language?: string;
  showLineNumbers?: boolean;
  /** Linhas destacadas, 1-indexadas. Use para apontar onde esta o bug. */
  highlightLines?: number[];
  className?: string;
}

/**
 * Bloco de codigo para enunciados e explicacoes.
 *
 * Sem syntax highlighting por enquanto: adicionar um highlighter e uma decisao
 * de dependencia e de bundle que merece ser tomada junto com o tipo de questao
 * que a exigir (Fase 3), nao antecipada aqui. O que ja existe e o essencial —
 * fonte monoespacada legivel, numeracao e destaque de linha.
 */
export function CodeBlock({
  code,
  language,
  showLineNumbers = false,
  highlightLines = [],
  className,
}: CodeBlockProps) {
  const lines = code.replace(/\n$/, "").split("\n");
  const highlighted = new Set(highlightLines);

  return (
    <div
      className={cn(
        "rounded-[var(--radius-md)] border border-[var(--border-subtle)]",
        "bg-[var(--bg-inset)] overflow-hidden",
        className,
      )}
    >
      {language && (
        <div className="px-4 py-2 border-b border-[var(--border-subtle)] text-xs font-mono text-[var(--text-muted)]">
          {language}
        </div>
      )}

      {/* O scroll horizontal fica CONTIDO neste container: a pagina nunca
          rola lateralmente por causa de uma linha longa de codigo. */}
      <div className="overflow-x-auto">
        <pre className="p-4 text-sm font-mono leading-relaxed min-w-full w-max">
          <code>
            {lines.map((line, index) => {
              const lineNumber = index + 1;
              const isHighlighted = highlighted.has(lineNumber);

              return (
                <span
                  key={lineNumber}
                  className={cn(
                    "block px-2 -mx-2 rounded-sm",
                    isHighlighted &&
                      "bg-[var(--accent-soft)] border-l-2 border-[var(--accent)] pl-2 -ml-2",
                  )}
                >
                  {showLineNumbers && (
                    <span
                      className="inline-block w-8 mr-4 text-right text-[var(--text-muted)] select-none"
                      aria-hidden="true"
                    >
                      {lineNumber}
                    </span>
                  )}
                  {line || " "}
                </span>
              );
            })}
          </code>
        </pre>
      </div>
    </div>
  );
}
