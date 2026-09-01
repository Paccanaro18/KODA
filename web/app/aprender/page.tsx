"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { Sidebar } from "@/components/app/Sidebar";
import { TopBar } from "@/components/app/TopBar";
import { Button } from "@/components/ui/Button";
import { Card, CardTitle, CardDescription } from "@/components/ui/Card";
import { Progress } from "@/components/ui/Progress";
import { SkillNode, type SkillState } from "@/components/ui/SkillNode";
import { Streak } from "@/components/ui/Streak";
import { Koda } from "@/components/ui/Koda";
import { Badge } from "@/components/ui/Badge";

/**
 * Trilha de aprendizado — primeira tela real do produto.
 *
 * Os dados aqui sao FICTICIOS e locais. A trilha de verdade vem do grafo de
 * conceitos da Fase 2, e a ordem dos nos e decisao do Adaptive Learning Engine,
 * nunca desta tela: aqui e so a superficie que desenha o que o motor decidiu.
 *
 * O caminho serpenteia em vez de descer reto. Nao e enfeite — uma lista
 * vertical alinhada le como backlog de tarefas, e o desvio lateral e o que faz
 * ler como percurso. A diferenca muda a disposicao de continuar.
 */

interface Lesson {
  id: string;
  title: string;
  state: SkillState;
  progress?: number;
}

interface Unit {
  id: string;
  name: string;
  subtitle: string;
  tone: "accent" | "action" | "reward";
  lessons: Lesson[];
}

const UNITS: Unit[] = [
  {
    id: "u1",
    name: "Unidade 1 — Fundamentos da linguagem",
    subtitle: "Tipos, controle de fluxo e funcoes",
    tone: "action",
    lessons: [
      { id: "l1", title: "Tipos primitivos", state: "mastered" },
      { id: "l2", title: "Condicionais", state: "completed" },
      { id: "l3", title: "Lacos", state: "completed" },
      { id: "l4", title: "Funcoes", state: "needsReview", progress: 40 },
      { id: "l5", title: "Escopo", state: "active", progress: 65 },
      { id: "l6", title: "Recursao", state: "available" },
    ],
  },
  {
    id: "u2",
    name: "Unidade 2 — Estruturas de dados",
    subtitle: "Listas, mapas e complexidade",
    tone: "accent",
    lessons: [
      { id: "l7", title: "Arrays", state: "locked" },
      { id: "l8", title: "Listas ligadas", state: "locked" },
      { id: "l9", title: "Mapas", state: "locked" },
      { id: "l10", title: "Complexidade", state: "locked" },
    ],
  },
];

const UNIT_TONES = {
  action: "bg-[var(--action)] shadow-[0_5px_0_var(--lip-action)] text-white",
  accent: "bg-[var(--accent)] shadow-[0_5px_0_var(--lip-accent)] text-white",
  reward:
    "bg-[var(--reward)] shadow-[0_5px_0_var(--lip-reward)] text-[#5c4300]",
} as const;

/**
 * Deslocamento lateral de cada no, em px.
 *
 * O ciclo tem 8 posicoes e volta ao inicio: o caminho ondula sem nunca sair da
 * coluna central, entao continua legivel em tela estreita sem media query.
 */
const PATH_OFFSETS = [0, 52, 76, 52, 0, -52, -76, -52];

/**
 * O deslocamento e resolvido AQUI, fora do componente, e nao durante o render.
 *
 * A conta e a mesma, mas um contador incrementado dentro do JSX e reatribuicao
 * depois do render — o lint do React 19 barra, e com razao: a posicao de cada
 * no e um dado derivado do curriculo, nao um efeito colateral de desenhar.
 */
const LESSON_OFFSETS: Record<string, number> = {};
UNITS.flatMap((unit) => unit.lessons).forEach((lesson, index) => {
  LESSON_OFFSETS[lesson.id] = PATH_OFFSETS[index % PATH_OFFSETS.length];
});

export default function LearnPage() {
  return (
    <RequireAuth>
      <LearnPageContent />
    </RequireAuth>
  );
}

function LearnPageContent() {
  // O no em foco e o unico com balao "COMECAR". Sem isso o aluno perde tempo
  // procurando onde parou toda vez que abre o app.
  const [focusedLesson] = useState("l5");

  return (
    <div className="min-h-screen bg-[var(--bg-canvas)]">
      <Sidebar active="aprender" />

      <div className="md:pl-60">
        <TopBar />

        <div className="mx-auto max-w-5xl px-4 pb-28 md:pb-16 flex gap-8">
          <main className="flex-1 min-w-0">
            {UNITS.map((unit) => (
              <section key={unit.id} className="mb-4">
                <UnitBanner unit={unit} />

                <div className="flex flex-col items-center gap-5 py-8">
                  {unit.lessons.map((lesson) => (
                    <div
                      key={lesson.id}
                      style={{
                        transform: `translateX(${LESSON_OFFSETS[lesson.id]}px)`,
                      }}
                    >
                      <SkillNode
                        title={lesson.title}
                        state={lesson.state}
                        progress={lesson.progress}
                        showCallout={lesson.id === focusedLesson}
                      />
                    </div>
                  ))}
                </div>
              </section>
            ))}

            <EndOfPath />
          </main>

          {/* Coluna de contexto. Some abaixo de xl: nela nao ha nada que o
              aluno precise para praticar, e roubar largura do caminho custaria
              mais do que ela entrega. */}
          <aside className="hidden xl:block w-72 shrink-0 space-y-4 py-6">
            <DailyQuest />
            <StreakCard />
            <LeagueCard />
          </aside>
        </div>
      </div>
    </div>
  );
}

function UnitBanner({ unit }: { unit: Unit }) {
  return (
    <div
      className={cn(
        "flex items-center justify-between gap-4 px-5 py-4 rounded-[var(--radius-xl)]",
        UNIT_TONES[unit.tone],
      )}
    >
      <div className="min-w-0">
        <h2 className="text-lg font-extrabold truncate">{unit.name}</h2>
        <p className="text-sm opacity-90 truncate">{unit.subtitle}</p>
      </div>

      <Button
        variant="secondary"
        size="sm"
        className="shrink-0 bg-white/15 text-current border-white/30 shadow-[0_4px_0_rgb(0_0_0/0.18)] hover:bg-white/25"
      >
        Guia
      </Button>
    </div>
  );
}

/** Fim do caminho: o Koda espera ali como recompensa de percurso. */
function EndOfPath() {
  return (
    <div className="flex flex-col items-center gap-3 py-10 text-center">
      <Koda state="idle" size={72} />
      <p className="max-w-xs text-sm text-[var(--text-secondary)]">
        Voce chegou ao fim do que esta liberado. Termine a Unidade 1 para o Koda
        abrir a proxima.
      </p>
    </div>
  );
}

function DailyQuest() {
  return (
    <Card>
      <div className="flex items-center justify-between mb-3">
        <CardTitle className="text-base">Missao do dia</CardTitle>
        <Badge tone="reward">+40 XP</Badge>
      </div>
      <CardDescription className="mb-3">
        Acerte 10 questoes seguidas sem usar dica.
      </CardDescription>
      <Progress value={60} tone="reward" showValue label="6 de 10" />
    </Card>
  );
}

function StreakCard() {
  return (
    <Card>
      <CardTitle className="text-base mb-1">Sequencia</CardTitle>
      <CardDescription className="mb-3">
        Voce praticou 12 dias seguidos.
      </CardDescription>
      <div className="flex items-center justify-between">
        <Streak days={12} />
        <Badge tone="success">Em dia</Badge>
      </div>
    </Card>
  );
}

function LeagueCard() {
  return (
    <Card>
      <CardTitle className="text-base mb-1">Liga Indigo</CardTitle>
      <CardDescription className="mb-3">
        Voce esta em 7o entre 30. Top 10 sobe de liga.
      </CardDescription>
      <Button variant="secondary" size="sm" fullWidth>
        Ver classificacao
      </Button>
    </Card>
  );
}
