"use client";

import { useState } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { Sidebar } from "@/components/app/Sidebar";
import { TopBar } from "@/components/app/TopBar";
import { AnswerOption, type AnswerState } from "@/components/ui/AnswerOption";
import { Feedback } from "@/components/ui/Feedback";
import { CodeBlock } from "@/components/ui/CodeBlock";
import { Progress } from "@/components/ui/Progress";
import { Button } from "@/components/ui/Button";
import { Card, CardTitle, CardDescription } from "@/components/ui/Card";
import { Koda } from "@/components/ui/Koda";

/**
 * Pratica avulsa por topico ainda nao existe — depende do grafo de conceitos
 * da Fase 2. O que existe aqui e o outro metade da tela: o loop de responder
 * questao em si, que nao depende de curriculo nenhum pra ser construido.
 *
 * O banco de questoes e ficticio e local, como as UNITS de /aprender: cobre
 * os mesmos topicos da Unidade 1 pra tela nao introduzir um assunto novo.
 */
interface Question {
  id: string;
  concept: string;
  prompt: string;
  code?: string;
  options: { id: string; label: string; mono?: boolean }[];
  correctOptionId: string;
  explanation: string;
  hint: string;
}

const QUESTIONS: Question[] = [
  {
    id: "q1",
    concept: "Tipos primitivos",
    prompt: "Qual e o tipo do valor 3.14?",
    options: [
      { id: "a", label: "inteiro" },
      { id: "b", label: "decimal" },
      { id: "c", label: "texto" },
      { id: "d", label: "booleano" },
    ],
    correctOptionId: "b",
    explanation:
      "3.14 tem casas decimais, entao e um numero de ponto flutuante — o tipo decimal.",
    hint: "Um numero inteiro nunca tem virgula ou ponto.",
  },
  {
    id: "q2",
    concept: "Condicionais",
    prompt: "O que este trecho imprime?",
    code: 'let idade = 16;\n\nif (idade >= 18) {\n  console.log("maior");\n} else {\n  console.log("menor");\n}',
    options: [
      { id: "a", label: "maior" },
      { id: "b", label: "menor" },
      { id: "c", label: "Nao imprime nada" },
      { id: "d", label: "Erro" },
    ],
    correctOptionId: "b",
    explanation: "16 nao e maior ou igual a 18, entao o fluxo cai no senao e imprime \"menor\".",
    hint: "Compare 16 com 18 antes de olhar pros ramos do if/else.",
  },
  {
    id: "q3",
    concept: "Lacos",
    prompt: "Quantas vezes este laco executa?",
    code: "for (let i = 0; i <= 4; i++) {\n  console.log(i);\n}",
    options: [
      { id: "a", label: "4" },
      { id: "b", label: "5" },
      { id: "c", label: "0" },
      { id: "d", label: "Infinitas" },
    ],
    correctOptionId: "b",
    explanation: "De 0 ate 4 incluindo as duas pontas sao 5 valores: 0, 1, 2, 3, 4.",
    hint: "Conte os numeros de 0 a 4, incluindo o 0 e o 4.",
  },
  {
    id: "q4",
    concept: "Funcoes",
    prompt: "O que esta funcao retorna para dobro(5)?",
    code: "function dobro(x) {\n  return x * 2;\n}\n\ndobro(5);",
    options: [
      { id: "a", label: "5" },
      { id: "b", label: "10" },
      { id: "c", label: "25" },
      { id: "d", label: "Nada" },
    ],
    correctOptionId: "b",
    explanation: "A funcao multiplica o parametro por 2, entao dobro(5) retorna 10.",
    hint: "Substitua x por 5 na expressao x * 2.",
  },
  {
    id: "q5",
    concept: "Escopo",
    prompt: "Depois de rodar este codigo, qual o valor de x fora da funcao?",
    code: "let x = 1;\n\nfunction muda() {\n  let x = 2;\n}\n\nmuda();",
    options: [
      { id: "a", label: "1" },
      { id: "b", label: "2" },
      { id: "c", label: "0" },
      { id: "d", label: "Erro" },
    ],
    correctOptionId: "a",
    explanation:
      "O x dentro de muda() e uma variavel local nova — ela nao altera o x de fora. Do lado de fora, x continua 1.",
    hint: "Uma atribuicao dentro de uma funcao cria uma variavel local, a menos que ela seja declarada global.",
  },
];

const XP_PER_CORRECT = 10;
const STARTING_HEARTS = 5;

type Phase = "intro" | "question" | "summary" | "out-of-hearts";

export default function PracticePage() {
  return (
    <RequireAuth>
      <PracticePageContent />
    </RequireAuth>
  );
}

function PracticePageContent() {
  const [phase, setPhase] = useState<Phase>("intro");
  const [index, setIndex] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [resolved, setResolved] = useState(false);
  const [hearts, setHearts] = useState(STARTING_HEARTS);
  const [correctCount, setCorrectCount] = useState(0);

  const question = QUESTIONS[index];
  const isLast = index === QUESTIONS.length - 1;
  const isCorrectSelected = resolved && selected === question.correctOptionId;

  function start() {
    setIndex(0);
    setSelected(null);
    setResolved(false);
    setHearts(STARTING_HEARTS);
    setCorrectCount(0);
    setPhase("question");
  }

  function check() {
    if (!selected || resolved) return;
    setResolved(true);
    if (selected === question.correctOptionId) {
      setCorrectCount((c) => c + 1);
    } else {
      setHearts((h) => h - 1);
    }
  }

  function goNext() {
    // Vidas acabaram exatamente nesta resposta: encerra antes de avancar, em
    // vez de deixar o aluno tentar a proxima questao sem vida nenhuma.
    if (hearts <= 0) {
      setPhase("out-of-hearts");
      return;
    }
    if (isLast) {
      setPhase("summary");
      return;
    }
    setIndex((i) => i + 1);
    setSelected(null);
    setResolved(false);
  }

  function optionState(optionId: string): AnswerState {
    if (!resolved) return selected === optionId ? "selected" : "default";
    if (optionId === question.correctOptionId) {
      return optionId === selected ? "correct" : "revealed";
    }
    return optionId === selected ? "incorrect" : "default";
  }

  return (
    <div className="min-h-screen bg-[var(--bg-canvas)]">
      <Sidebar active="praticar" />

      <div className="md:pl-60">
        <TopBar hearts={hearts} />

        {phase === "intro" && <IntroCard onStart={start} />}

        {phase === "question" && (
          <div className="mx-auto max-w-2xl px-4 pb-44 md:pb-16 py-6">
            <Progress
              value={(index / QUESTIONS.length) * 100}
              label={`Questao ${index + 1} de ${QUESTIONS.length}`}
              className="mb-8"
            />

            <h1 className="text-xl font-extrabold mb-4">{question.prompt}</h1>

            {question.code && (
              <CodeBlock code={question.code} language="javascript" className="mb-5" />
            )}

            <div className="flex flex-col gap-3">
              {question.options.map((option, i) => (
                <AnswerOption
                  key={option.id}
                  marker={String.fromCharCode(65 + i)}
                  label={option.label}
                  mono={option.mono}
                  state={optionState(option.id)}
                  onClick={() => !resolved && setSelected(option.id)}
                />
              ))}
            </div>

            {!resolved && (
              <div className="mt-6 flex justify-end">
                <Button onClick={check} disabled={!selected}>
                  Verificar
                </Button>
              </div>
            )}

            {resolved && (
              <Feedback
                docked
                correct={isCorrectSelected}
                explanation={question.explanation}
                concept={question.concept}
                hint={!isCorrectSelected ? question.hint : undefined}
                action={
                  <Button onClick={goNext}>{isLast ? "Terminar" : "Continuar"}</Button>
                }
              />
            )}
          </div>
        )}

        {phase === "summary" && (
          <SummaryCard
            correctCount={correctCount}
            total={QUESTIONS.length}
            xp={correctCount * XP_PER_CORRECT}
            onRestart={start}
          />
        )}

        {phase === "out-of-hearts" && <OutOfHeartsCard onRestart={start} />}
      </div>
    </div>
  );
}

function IntroCard({ onStart }: { onStart: () => void }) {
  return (
    <div className="mx-auto max-w-sm px-4 py-16">
      <Card className="flex flex-col items-center gap-4 text-center">
        <Koda state="explaining" size={72} />
        <div>
          <CardTitle>Sessao rapida</CardTitle>
          <CardDescription className="mt-1">
            {QUESTIONS.length} questoes sobre Fundamentos da linguagem. Erre
            demais e a sessao acaba — igual ao caminho principal.
          </CardDescription>
        </div>
        <Button onClick={onStart} fullWidth>
          Comecar
        </Button>
      </Card>
    </div>
  );
}

function SummaryCard({
  correctCount,
  total,
  xp,
  onRestart,
}: {
  correctCount: number;
  total: number;
  xp: number;
  onRestart: () => void;
}) {
  return (
    <div className="mx-auto max-w-sm px-4 py-16">
      <Card className="flex flex-col items-center gap-4 text-center">
        <Koda state="celebrating" size={80} />
        <div>
          <CardTitle>Sessao concluida</CardTitle>
          <CardDescription className="mt-1">
            {correctCount} de {total} corretas — +{xp} XP
          </CardDescription>
        </div>
        <Button onClick={onRestart} fullWidth>
          Praticar de novo
        </Button>
      </Card>
    </div>
  );
}

function OutOfHeartsCard({ onRestart }: { onRestart: () => void }) {
  return (
    <div className="mx-auto max-w-sm px-4 py-16">
      <Card className="flex flex-col items-center gap-4 text-center">
        <Koda state="encouraging" size={80} />
        <div>
          <CardTitle>Acabaram as vidas</CardTitle>
          <CardDescription className="mt-1">
            Sem problema — errar faz parte. Espere elas voltarem ou comece uma
            nova sessao.
          </CardDescription>
        </div>
        <Button onClick={onRestart} fullWidth>
          Tentar de novo
        </Button>
      </Card>
    </div>
  );
}
