# KODA — Identidade Visual e Design System

> Fase 0 (Architecture) · Status: proposta · Precede qualquer tela

Regra de precedência, aplicada em toda dúvida: **clareza > quantidade de elementos** · **feedback > animação** · **identidade própria > semelhança com referências**.

## Conceito

Tecnologia + conhecimento + evolução. A sensação alvo é *"estou jogando algo fluido, e estou realmente ficando melhor como engenheiro"*. Moderno, técnico, inteligente, premium, amigável e motivador — nunca infantil, porque o público vai do iniciante absoluto ao engenheiro experiente.

Nenhum ativo, mascote, ícone, layout ou paleta de terceiros é copiado. O que se reaproveita são **princípios de UX** — progresso visível, lições curtas, feedback imediato, recompensa, mapa de aprendizado, celebração — não linguagem visual alheia.

## Design tokens

Cores vivem exclusivamente em tokens. Nenhum componente declara cor literal — essa é a regra que mantém a consistência e faz o dark mode funcionar de verdade.

### Cor — escala de marca

```
brand    50  #EEF0FF   ← superfícies suaves (light)
        100  #E0E3FF
        200  #C7CCFF
        300  #A5AAFF
        400  #837FFF
        500  #635BFF   ← PRIMARY
        600  #524AE0
        700  #423BB8
        800  #332D8F
        900  #241F66
```

```
cyan     400  #4DE0F5
         500  #22D3EE   ← SECONDARY
         600  #0EA5C4
amber    400  #FBBF24
         500  #F59E0B   ← ACCENT / recompensa
green    500  #22C55E   ← SUCCESS
red      500  #EF4444   ← ERROR
```

### Tokens semânticos

O componente consome **apenas** o token semântico, nunca a escala bruta.

| Token | Dark | Light |
|---|---|---|
| `--bg-canvas` | `#0B1020` | `#FBFBFD` |
| `--bg-surface` | `#131A2E` | `#FFFFFF` |
| `--bg-surface-raised` | `#1B2440` | `#F5F6FA` |
| `--border-subtle` | `#252F4A` | `#E4E7F0` |
| `--text-primary` | `#F2F4FB` | `#0F1425` |
| `--text-secondary` | `#A3ACC8` | `#5A6484` |
| `--text-muted` | `#6B7594` | `#8A93AD` |
| `--accent-primary` | `brand.400` | `brand.500` |
| `--accent-secondary` | `cyan.400` | `cyan.600` |
| `--reward` | `amber.400` | `amber.500` |
| `--success` | `green.500` | `#16A34A` |
| `--error` | `red.500` | `#DC2626` |
| `--focus-ring` | `cyan.400` | `brand.500` |

Dark mode é experiência de primeira classe, não inversão: superfícies escuras com hierarquia própria, contraste calibrado e código com legibilidade excelente. Light mode mantém a identidade índigo/ciano — não vira dashboard corporativo genérico.

### Estados do mapa de aprendizado

Cada estado precisa ser distinguível **sem depender de cor** — forma, ícone e opacidade carregam o significado junto com o matiz. Requisito de acessibilidade, não enfeite.

| Estado | Tratamento |
|---|---|
| `locked` | Superfície rebaixada, contorno tracejado, ícone de cadeado, sem elevação |
| `available` | Superfície normal, contorno sólido sutil, ícone do conceito |
| `active` | Contorno em `accent-primary`, anel de progresso, leve elevação e glow discreto |
| `completed` | Contorno em `success`, ícone de check preenchido |
| `mastered` | Contorno em `reward`, ícone com selo, brilho contido |
| `needs-review` | Contorno em `accent-secondary`, ícone de renovação, pulso lento |

### Tipografia

- Interface: **Inter** — fallback `system-ui, -apple-system, "Segoe UI", sans-serif`
- Código: **JetBrains Mono** — fallback `ui-monospace, "Cascadia Code", Consolas, monospace`

Escala: `xs 12 · sm 14 · base 16 · lg 18 · xl 20 · 2xl 24 · 3xl 30 · 4xl 38 · 5xl 48`.
Pesos: 400 corpo · 500 rótulo · 600 título · 700 destaque.
Corpo de texto em `line-height 1.6`; títulos em `1.2` com leve tracking negativo.

### Espaçamento, raio e elevação

Espaçamento em base 4: `1 2 3 4 6 8 12 16 20 24 32`.
Raio: `sm 6 · md 10 · lg 14 · xl 20 · full 9999`.
Elevação sutil e em três níveis apenas. Sem excesso de sombra, gradiente, glassmorphism, neon ou borda decorativa.

### Motion

```
duration:  instant 80ms · fast 140ms · base 220ms · slow 320ms · celebration 600ms
easing:    standard cubic-bezier(.2,0,0,1) · spring (stiffness 320, damping 26)
```

Animar preferencialmente `opacity` e `transform` — são as propriedades que o compositor resolve sem relayout. Animação melhora a experiência; nunca a atrasa.

**`prefers-reduced-motion` é obrigatório**: elimina translação, escala e partículas, preservando mudanças de opacidade e todo o feedback informativo. Nenhuma informação pode existir apenas na animação.

## Componentes base

Antes das telas, constrói-se a biblioteca: `Button` · `Card` · `Badge` · `Progress` · `ProgressRing` · `Toast` · `Modal` · `Tabs` · `Navigation` · `Question` · `AnswerOption` · `CodeBlock` · `CodeEditor` · `XP` · `Streak` · `Achievement` · `SkillNode` · `SkillMap` · `KodaAI`.

Todos com estados explícitos: `default · hover · active · focus · disabled · loading · success · error · locked · completed`.

`focus` nunca é removido — é anel visível de 2px em `--focus-ring` com offset.

## Padrões de experiência

**As três perguntas** que a interface responde a qualquer momento: onde estou · o que faço agora · por que isso me ajuda. O próximo passo é sempre o elemento visualmente dominante.

**Dashboard** — central de aprendizagem, não painel administrativo. Hierarquia: saudação → continuar jornada (ação principal, dominante) → recomendados → evolução (XP, streak, skills). Números servem à ação; não a substituem.

**Exercício** — tela limpa com a pergunta como elemento principal. Cabeçalho com tópico e barra de progresso, enunciado, alternativas generosas ao toque. O aluno entra e começa imediatamente.

**Feedback correto** — seleção → confirmação → check animado → contador de XP subindo suavemente → celebração breve → próxima. O aluno precisa sentir que realizou algo.

**Feedback incorreto** — seleção → sinalização → explicação → dica → continuar. A mensagem comunica *"você descobriu algo que ainda precisa aprender"*, nunca fracasso. Sem punição visual agressiva.

**Level up** — momento especial e curto: glow, partículas discretas, progressão visual. Celebra sem sequestrar o usuário.

**Koda AI** — identidade abstrata: um orb geométrico com movimento próprio, não um robô genérico. Estados animados para pensando, explicando, dando dica e comemorando. Deve parecer **mentor pessoal**, não chatbot.

## Acessibilidade

Contraste mínimo AA (4.5:1 em texto, 3:1 em elementos de interface), navegação completa por teclado, focus states visíveis, marcação semântica com ARIA onde necessário, alvos de toque de no mínimo 44px, suporte a `prefers-reduced-motion` e nenhum estado comunicado só por cor.

## Mobile

Mobile-first de fato. Navegação simples, botões grandes, cards adaptáveis, código legível com scroll horizontal contido no próprio bloco — a página nunca rola horizontalmente.
