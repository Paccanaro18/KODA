import { RequireAuth } from "@/components/auth/RequireAuth";
import { Sidebar } from "@/components/app/Sidebar";
import { TopBar } from "@/components/app/TopBar";
import { Koda } from "@/components/ui/Koda";

/**
 * Pratica avulsa — ainda nao existe.
 *
 * Fica de placeholder ate a Fase 2 (grafo de conceitos) dar a esta tela o que
 * ela precisa: uma lista de topicos que o aluno ja tocou, pra reforcar fora da
 * ordem da trilha. Sem isso a tela so teria os mesmos nos de /aprender de
 * novo, o que nao serve a ninguem.
 */
export default function PracticePage() {
  return (
    <RequireAuth>
      <PracticePageContent />
    </RequireAuth>
  );
}

function PracticePageContent() {
  return (
    <div className="min-h-screen bg-[var(--bg-canvas)]">
      <Sidebar active="praticar" />

      <div className="md:pl-60">
        <TopBar />

        <div className="mx-auto max-w-5xl px-4 py-20 flex flex-col items-center gap-4 text-center">
          <Koda state="thinking" size={80} />
          <h1 className="text-xl font-extrabold">Ainda estamos preparando isso</h1>
          <p className="max-w-sm text-sm text-[var(--text-secondary)]">
            A pratica avulsa por topico chega junto com o mapa de conceitos.
            Por enquanto, siga a trilha em Aprender.
          </p>
        </div>
      </div>
    </div>
  );
}
