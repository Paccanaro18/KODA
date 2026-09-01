"use client";

import { useEffect, useMemo, useState } from "react";
import { cn } from "@/lib/cn";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { useAuth } from "@/components/auth/AuthProvider";
import * as api from "@/lib/api";
import type { CurriculumTopic } from "@/lib/api";
import { Sidebar } from "@/components/app/Sidebar";
import { TopBar } from "@/components/app/TopBar";
import { Button } from "@/components/ui/Button";
import { Card, CardTitle, CardDescription } from "@/components/ui/Card";
import { Progress } from "@/components/ui/Progress";
import { SkillNode } from "@/components/ui/SkillNode";
import { Streak } from "@/components/ui/Streak";
import { Koda } from "@/components/ui/Koda";
import { Badge } from "@/components/ui/Badge";

/**
 * Trilha de aprendizado — primeira tela real do produto.
 *
 * Os dados vem de GET /api/v1/curriculum/map (Fase 2). A ordem dos nos e
 * decisao do backend (pre-requisito, por enquanto — o Adaptive Learning
 * Engine de verdade e Fase 4); esta tela e so a superficie que desenha o que
 * chegou.
 */

const TOPIC_TONES = ["action", "accent", "reward"] as const;
type TopicTone = (typeof TOPIC_TONES)[number];

const TONE_STYLES: Record<TopicTone, string> = {
  action: "bg-[var(--action)] shadow-[0_5px_0_var(--lip-action)] text-white",
  accent: "bg-[var(--accent)] shadow-[0_5px_0_var(--lip-accent)] text-white",
  reward:
    "bg-[var(--reward)] shadow-[0_5px_0_var(--lip-reward)] text-[#5c4300]",
};

/**
 * Deslocamento lateral de cada no, em px.
 *
 * O ciclo tem 8 posicoes e volta ao inicio: o caminho ondula sem nunca sair da
 * coluna central, entao continua legivel em tela estreita sem media query.
 */
const PATH_OFFSETS = [0, 52, 76, 52, 0, -52, -76, -52];

export default function LearnPage() {
  return (
    <RequireAuth>
      <LearnPageContent />
    </RequireAuth>
  );
}

function LearnPageContent() {
  const { getAccessToken } = useAuth();
  const [topics, setTopics] = useState<CurriculumTopic[] | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const accessToken = getAccessToken();
    if (!accessToken) return;

    api
      .curriculumMap(accessToken)
      .then((map) => {
        if (!cancelled) setTopics(map.topics);
      })
      .catch(() => {
        if (!cancelled) setError(true);
      });

    return () => {
      cancelled = true;
    };
  }, [getAccessToken]);

  // O deslocamento e derivado da lista de conceitos, nao um efeito colateral
  // de desenhar — por isso useMemo, nao um contador incrementado no JSX.
  const offsetById = useMemo(() => {
    const offsets: Record<string, number> = {};
    (topics ?? [])
      .flatMap((topic) => topic.concepts)
      .forEach((concept, index) => {
        offsets[concept.id] = PATH_OFFSETS[index % PATH_OFFSETS.length];
      });
    return offsets;
  }, [topics]);

  // O no com o balao "COMECAR" e o primeiro que o aluno pode agir agora. Sem
  // isso ele perde tempo procurando onde parou toda vez que abre o app.
  const focusedConceptId = useMemo(() => {
    const actionable = (topics ?? [])
      .flatMap((topic) => topic.concepts)
      .find((concept) => concept.state === "available" || concept.state === "active" || concept.state === "needsReview");
    return actionable?.id ?? null;
  }, [topics]);

  return (
    <div className="min-h-screen bg-[var(--bg-canvas)]">
      <Sidebar active="aprender" />

      <div className="md:pl-60">
        <TopBar />

        {error && <ErrorState />}

        {!error && topics === null && <LoadingState />}

        {!error && topics !== null && (
          <div className="mx-auto max-w-5xl px-4 pb-28 md:pb-16 flex gap-8">
            <main className="flex-1 min-w-0">
              {topics.map((topic, index) => (
                <section key={topic.id} className="mb-4">
                  <TopicBanner topic={topic} tone={TOPIC_TONES[index % TOPIC_TONES.length]} />

                  <div className="flex flex-col items-center gap-5 py-8">
                    {topic.concepts.map((concept) => (
                      <div
                        key={concept.id}
                        style={{
                          transform: `translateX(${offsetById[concept.id]}px)`,
                        }}
                      >
                        <SkillNode
                          title={concept.title}
                          state={concept.state}
                          progress={concept.progress}
                          showCallout={concept.id === focusedConceptId}
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
        )}
      </div>
    </div>
  );
}

function LoadingState() {
  return (
    <div className="grid place-items-center py-24">
      <Koda state="thinking" size={64} />
    </div>
  );
}

function ErrorState() {
  return (
    <div className="flex flex-col items-center gap-3 py-24 text-center px-4">
      <Koda state="encouraging" size={64} />
      <p className="max-w-xs text-sm text-[var(--text-secondary)]">
        Nao consegui carregar sua trilha agora. Tente recarregar a pagina.
      </p>
    </div>
  );
}

function TopicBanner({ topic, tone }: { topic: CurriculumTopic; tone: TopicTone }) {
  return (
    <div
      className={cn(
        "flex items-center justify-between gap-4 px-5 py-4 rounded-[var(--radius-xl)]",
        TONE_STYLES[tone],
      )}
    >
      <div className="min-w-0">
        <h2 className="text-lg font-extrabold truncate">{topic.name}</h2>
        {topic.description && <p className="text-sm opacity-90 truncate">{topic.description}</p>}
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
        Voce chegou ao fim do que esta liberado. Termine a unidade atual para o
        Koda abrir a proxima.
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
