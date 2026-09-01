"use client";

import { useRouter } from "next/navigation";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { useAuth } from "@/components/auth/AuthProvider";
import { Sidebar } from "@/components/app/Sidebar";
import { TopBar } from "@/components/app/TopBar";
import { Card, CardTitle, CardDescription } from "@/components/ui/Card";
import { Koda } from "@/components/ui/Koda";
import { Streak } from "@/components/ui/Streak";
import { Gems } from "@/components/ui/Gems";
import { XpCounter } from "@/components/ui/Xp";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { Button } from "@/components/ui/Button";

export default function ProfilePage() {
  return (
    <RequireAuth>
      <ProfilePageContent />
    </RequireAuth>
  );
}

function ProfilePageContent() {
  const { user, logout } = useAuth();
  const router = useRouter();

  async function handleLogout() {
    await logout();
    router.push("/entrar");
  }

  return (
    <div className="min-h-screen bg-[var(--bg-canvas)]">
      <Sidebar active="perfil" />

      <div className="md:pl-60">
        <TopBar />

        <div className="mx-auto max-w-2xl px-4 pb-28 md:pb-16 py-8 space-y-4">
          <Card className="flex items-center gap-4">
            <Koda state="idle" size={64} />
            <div className="min-w-0">
              <CardTitle className="truncate">{user?.displayName || "Estudante"}</CardTitle>
              <CardDescription className="truncate">{user?.email}</CardDescription>
            </div>
          </Card>

          {/* Sequencia, gemas e XP ainda sao ficticios: dependem dos eventos
              de pratica da Fase 5, que ainda nao existem. */}
          <div className="grid grid-cols-3 gap-3">
            <Card className="flex flex-col items-center gap-1 p-4 text-center">
              <Streak days={12} />
              <span className="text-xs text-[var(--text-secondary)]">Sequencia</span>
            </Card>
            <Card className="flex flex-col items-center gap-1 p-4 text-center">
              <Gems value={430} />
              <span className="text-xs text-[var(--text-secondary)]">Gemas</span>
            </Card>
            <Card className="flex flex-col items-center gap-1 p-4 text-center">
              <XpCounter value={1240} />
              <span className="text-xs text-[var(--text-secondary)]">Experiencia</span>
            </Card>
          </div>

          <Card>
            <CardTitle className="text-base mb-3">Preferencias</CardTitle>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold">Tema</p>
                <CardDescription>Claro ou escuro</CardDescription>
              </div>
              <ThemeToggle />
            </div>
          </Card>

          <Button variant="secondary" fullWidth onClick={handleLogout}>
            Sair
          </Button>
        </div>
      </div>
    </div>
  );
}
