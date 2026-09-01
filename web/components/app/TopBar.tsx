import { Streak } from "@/components/ui/Streak";
import { Gems } from "@/components/ui/Gems";
import { Hearts } from "@/components/ui/Hearts";
import { ThemeToggle } from "@/components/theme/ThemeToggle";

interface TopBarProps {
  streakDays?: number;
  gems?: number;
  hearts?: number;
  maxHearts?: number;
}

/**
 * Barra superior com os recursos da sessao: sequencia, gemas e vidas.
 *
 * Compartilhada por todas as telas do produto — extraida de /aprender porque
 * repetir a mesma barra em cada pagina divergiria com o tempo.
 */
export function TopBar({
  streakDays = 12,
  gems = 430,
  hearts = 4,
  maxHearts = 5,
}: TopBarProps) {
  return (
    <header className="sticky top-0 z-10 border-b-2 border-[var(--border-subtle)] bg-[var(--bg-canvas)]/95 backdrop-blur">
      <div className="mx-auto max-w-5xl px-4 h-16 flex items-center justify-between gap-4">
        <span className="md:hidden text-xl font-extrabold text-[var(--accent)]">
          KODA
        </span>

        {/* No mobile a barra fica com o essencial da sessao: sequencia, gemas e
            vidas. Vidas viram contagem, e o tema sai — nao ha largura para os
            cinco coracoes e o botao sem empurrar a barra para fora da tela. */}
        <div className="flex items-center gap-3 sm:gap-6 ml-auto">
          <Streak days={streakDays} size="sm" className="sm:text-base" />
          <Gems value={gems} />
          <Hearts value={hearts} max={maxHearts} className="hidden sm:inline-flex" />
          <Hearts value={hearts} max={maxHearts} compact className="sm:hidden" />
          <ThemeToggle className="hidden sm:inline-flex" />
        </div>
      </div>
    </header>
  );
}
