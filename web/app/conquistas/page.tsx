import { RequireAuth } from "@/components/auth/RequireAuth";
import { Sidebar } from "@/components/app/Sidebar";
import { TopBar } from "@/components/app/TopBar";
import { Achievement } from "@/components/ui/Achievement";
import { XpCounter } from "@/components/ui/Xp";

/**
 * Conquistas — dados ficticios locais, como /aprender.
 *
 * A lista real depende de eventos que ainda nao existem (sequencia, questoes
 * corretas em sequencia, unidades concluidas). A tela fica pronta pra so
 * trocar a fonte dos dados quando esses eventos existirem.
 */
interface AchievementData {
  id: string;
  title: string;
  description: string;
  unlocked: boolean;
}

const ACHIEVEMENTS: AchievementData[] = [
  { id: "a1", title: "Primeiro passo", description: "Complete sua primeira licao", unlocked: true },
  { id: "a2", title: "Uma semana", description: "Pratique 7 dias seguidos", unlocked: true },
  { id: "a3", title: "Sem erros", description: "Termine uma unidade sem errar", unlocked: true },
  { id: "a4", title: "Duas semanas", description: "Pratique 14 dias seguidos", unlocked: false },
  { id: "a5", title: "Madrugador", description: "Pratique antes das 7h por 5 dias", unlocked: false },
  { id: "a6", title: "Maratonista", description: "Complete 100 questoes em uma unidade", unlocked: false },
];

export default function AchievementsPage() {
  return (
    <RequireAuth>
      <AchievementsPageContent />
    </RequireAuth>
  );
}

function AchievementsPageContent() {
  const unlockedCount = ACHIEVEMENTS.filter((a) => a.unlocked).length;

  return (
    <div className="min-h-screen bg-[var(--bg-canvas)]">
      <Sidebar active="conquistas" />

      <div className="md:pl-60">
        <TopBar />

        <div className="mx-auto max-w-2xl px-4 pb-28 md:pb-16 py-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-xl font-extrabold">Conquistas</h1>
              <p className="text-sm text-[var(--text-secondary)]">
                {unlockedCount} de {ACHIEVEMENTS.length} conquistadas
              </p>
            </div>
            <XpCounter value={1240} />
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            {ACHIEVEMENTS.map((achievement) => (
              <Achievement
                key={achievement.id}
                title={achievement.title}
                description={achievement.description}
                unlocked={achievement.unlocked}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
