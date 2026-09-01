"use client";

import { useState, type ReactNode } from "react";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { Button } from "@/components/ui/Button";
import { Card, CardTitle, CardDescription } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Progress, ProgressRing } from "@/components/ui/Progress";
import { AnswerOption, type AnswerState } from "@/components/ui/AnswerOption";
import { SkillNode } from "@/components/ui/SkillNode";
import { XpCounter, XpGain } from "@/components/ui/Xp";
import { Streak } from "@/components/ui/Streak";
import { Koda } from "@/components/ui/Koda";
import { Hearts } from "@/components/ui/Hearts";
import { Gems } from "@/components/ui/Gems";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Achievement } from "@/components/ui/Achievement";
import { Feedback } from "@/components/ui/Feedback";

/**
 * Referencia viva do design system do KODA.
 *
 * Nao e uma tela do produto: e o inventario de componentes e estados, que
 * permite verificar consistencia, dark/light e acessibilidade antes de existir
 * qualquer pagina real. As telas da Fase 2 em diante se montam a partir daqui.
 */
export default function DesignSystemPage() {
  return (
    <main className="min-h-screen px-6 py-10 max-w-5xl mx-auto">
      <header className="flex items-start justify-between gap-4 mb-12">
        <div className="flex items-center gap-3">
          <Koda state="idle" size={56} />
          <div>
            <h1 className="text-3xl">KODA</h1>
            <p className="text-[var(--text-secondary)] text-sm">
              Design system — componentes e estados
            </p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <a
            href="/aprender"
            className="text-sm font-extrabold uppercase tracking-[0.06em] text-[var(--accent)] hover:underline"
          >
            Ver a trilha
          </a>
          <ThemeToggle />
        </div>
      </header>

      <div className="space-y-12">
        <ColorSection />
        <ButtonSection />
        <FeedbackSection />
        <ExerciseSection />
        <SkillMapSection />
        <GamificationSection />
        <KodaAiSection />
        <CodeSection />
      </div>

      <footer className="mt-16 pt-6 border-t border-[var(--border-subtle)] text-sm text-[var(--text-muted)]">
        Todos os estados acima respeitam{" "}
        <code className="font-mono">prefers-reduced-motion</code> e comunicam
        significado sem depender apenas de cor.
      </footer>
    </main>
  );
}

function Section({
  title,
  hint,
  children,
}: {
  title: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <section>
      <h2 className="text-xl mb-1">{title}</h2>
      {hint && (
        <p className="text-sm text-[var(--text-secondary)] mb-4">{hint}</p>
      )}
      <div className={hint ? "" : "mt-4"}>{children}</div>
    </section>
  );
}

function ColorSection() {
  const tokens = [
    ["--bg-canvas", "canvas"],
    ["--bg-surface", "surface"],
    ["--bg-surface-raised", "raised"],
    ["--accent", "accent"],
    ["--accent-secondary", "secondary"],
    ["--reward", "reward"],
    ["--success", "success"],
    ["--error", "error"],
  ];

  return (
    <Section
      title="Tokens de cor"
      hint="Nenhum componente declara cor literal. Trocar o tema reescreve apenas esta camada."
    >
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {tokens.map(([token, name]) => (
          <div key={token} className="space-y-1.5">
            <div
              className="h-14 rounded-[var(--radius-md)] border border-[var(--border-subtle)]"
              style={{ backgroundColor: `var(${token})` }}
            />
            <p className="text-xs font-mono text-[var(--text-secondary)]">
              {name}
            </p>
          </div>
        ))}
      </div>
    </Section>
  );
}

function ButtonSection() {
  return (
    <Section title="Button" hint="Variantes e estados, incluindo loading e disabled.">
      <div className="flex flex-wrap gap-3 items-center">
        <Button variant="action">Continuar</Button>
        <Button variant="secondary">Revisar</Button>
        <Button variant="ghost">Pular</Button>
        <Button variant="primary">Verificar</Button>
        <Button variant="danger">Encerrar</Button>
        <Button loading>Carregando</Button>
        <Button disabled>Indisponivel</Button>
      </div>
      <div className="flex flex-wrap gap-3 items-center mt-4">
        <Button size="sm">Pequeno</Button>
        <Button size="md">Medio</Button>
        <Button size="lg">Grande</Button>
      </div>
    </Section>
  );
}

function FeedbackSection() {
  return (
    <Section
      title="Feedback"
      hint="O erro nunca e tratado como fracasso, e sim como descoberta de uma lacuna."
    >
      <div className="space-y-4">
        <Feedback
          correct
          explanation="Voce identificou que uma Promise nao e o valor final da operacao, mas a representacao de um resultado que ficara disponivel depois."
          concept="Promise chaining"
          nextStep="Praticar tratamento de erro com catch()."
        />
        <Feedback
          correct={false}
          explanation="O container sobe e encerra imediatamente porque o processo principal termina. Um container vive enquanto seu PID 1 estiver rodando."
          concept="Ciclo de vida de containers"
          hint="Compare a saida de docker ps com a de docker ps -a."
          nextStep="Revisar processos em primeiro plano no Docker."
        />
      </div>
    </Section>
  );
}

function ExerciseSection() {
  const [selected, setSelected] = useState<number | null>(null);
  const [checked, setChecked] = useState(false);
  const correctIndex = 0;

  const options = [
    "docker ps",
    "docker ls",
    "docker containers",
    "docker list",
  ];

  const stateFor = (index: number): AnswerState => {
    if (!checked) return selected === index ? "selected" : "default";
    if (index === correctIndex) {
      return selected === correctIndex ? "correct" : "revealed";
    }
    return selected === index ? "incorrect" : "default";
  };

  return (
    <Section
      title="AnswerOption"
      hint="Interativo: escolha uma alternativa e verifique. Acerto e erro tem icone, nao apenas cor."
    >
      <Card>
        <div className="flex items-center justify-between mb-3">
          <Badge tone="accent">Docker Fundamentals</Badge>
          <span className="text-sm text-[var(--text-secondary)]">3 / 12</span>
        </div>

        <Progress value={25} className="mb-5" />

        <p className="text-lg font-medium mb-4">
          Qual comando lista os containers em execucao?
        </p>

        <div className="space-y-2.5">
          {options.map((option, index) => (
            <AnswerOption
              key={option}
              marker={String.fromCharCode(65 + index)}
              label={option}
              mono
              state={stateFor(index)}
              onClick={() => setSelected(index)}
            />
          ))}
        </div>

        <div className="flex gap-3 mt-5">
          <Button
            onClick={() => setChecked(true)}
            disabled={selected === null || checked}
          >
            Verificar
          </Button>
          {checked && (
            <Button
              variant="ghost"
              onClick={() => {
                setChecked(false);
                setSelected(null);
              }}
            >
              Reiniciar
            </Button>
          )}
        </div>
      </Card>
    </Section>
  );
}

function SkillMapSection() {
  return (
    <Section
      title="SkillNode"
      hint="Os seis estados do mapa. Cada um difere em forma, icone e opacidade — nao so em cor."
    >
      <div className="flex flex-wrap gap-4">
        <SkillNode title="Variaveis" state="mastered" />
        <SkillNode title="Condicoes" state="completed" />
        <SkillNode title="Loops" state="active" progress={82} />
        <SkillNode title="Funcoes" state="needsReview" progress={61} />
        <SkillNode title="Arrays" state="available" />
        <SkillNode title="Estruturas" state="locked" />
      </div>
    </Section>
  );
}

function GamificationSection() {
  const [xp, setXp] = useState(1240);
  const [gain, setGain] = useState(false);

  return (
    <Section
      title="Progresso e gamificacao"
      hint="XP deriva de evidencia de dominio, nunca de quantidade de cliques."
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardTitle>Sua evolucao</CardTitle>
          <CardDescription>Sequencia e experiencia acumulada</CardDescription>

          <div className="flex flex-wrap items-center gap-5 mt-4">
            <XpCounter value={xp} />
            <Streak days={12} />
            <Streak days={3} atRisk size="sm" />
          </div>

          <div className="flex flex-wrap items-center gap-5 mt-4">
            <Gems value={430} />
            <Hearts value={3} max={5} />
          </div>

          <div className="flex items-center gap-3 mt-4">
            <Button
              size="sm"
              variant="secondary"
              onClick={() => {
                setXp((current) => current + 15);
                setGain(true);
                window.setTimeout(() => setGain(false), 600);
              }}
            >
              Simular acerto
            </Button>
            {gain && <XpGain amount={15} />}
          </div>
        </Card>

        <Card>
          <CardTitle>Dominio por topico</CardTitle>
          <CardDescription>Estimativa, nao contagem de acertos</CardDescription>

          <div className="space-y-3 mt-4">
            <Progress value={92} label="Variaveis" showValue tone="success" />
            <Progress value={64} label="Objetos" showValue />
            <Progress value={38} label="Promises" showValue tone="reward" />
          </div>

          <div className="flex gap-4 mt-5">
            <ProgressRing value={68} />
            <ProgressRing value={100} tone="success" />
            <ProgressRing value={31} tone="reward" />
          </div>
        </Card>

        <Card className="sm:col-span-2">
          <CardTitle>Conquistas</CardTitle>
          <div className="grid gap-2.5 sm:grid-cols-3 mt-4">
            <Achievement
              title="Docker Explorer"
              description="10 desafios de containers"
              unlocked
            />
            <Achievement
              title="Bug Hunter"
              description="25 bugs identificados"
              unlocked
            />
            <Achievement
              title="Cloud Builder"
              description="Trilha de cloud completa"
            />
          </div>
        </Card>
      </div>
    </Section>
  );
}

function KodaAiSection() {
  return (
    <Section
      title="Koda"
      hint="O personagem do produto. Os estados se distinguem por expressao e por ritmo do movimento, nunca por cor."
    >
      <div className="flex flex-wrap gap-8">
        {(
          ["idle", "thinking", "explaining", "celebrating", "encouraging"] as const
        ).map((state) => (
          <div key={state} className="flex flex-col items-center gap-2">
            <Koda state={state} size={72} />
            <span className="text-xs font-mono text-[var(--text-secondary)]">
              {state}
            </span>
          </div>
        ))}
      </div>
    </Section>
  );
}

function CodeSection() {
  return (
    <Section
      title="CodeBlock"
      hint="Linha destacada aponta onde esta o problema. O scroll horizontal fica contido no bloco."
    >
      <CodeBlock
        language="javascript"
        showLineNumbers
        highlightLines={[3]}
        code={`const promise = fetch('/api/questions');
console.log(promise);
const data = promise.json();
console.log(data);`}
      />
      <div className="mt-3">
        <Badge tone="info">Identifique o bug</Badge>
      </div>
    </Section>
  );
}
