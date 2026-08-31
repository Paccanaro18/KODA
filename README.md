# KODA

Plataforma de aprendizagem adaptativa para tecnologia — programação, web, backend, frontend, DevOps, cloud, Linux, redes, bancos de dados, containers, CI/CD, segurança, arquitetura, sistemas distribuídos, observabilidade, SRE e testes.

O objetivo não é entregar um site bonito com questões aleatórias. É responder, a cada momento, a uma pergunta específica:

> **O que este estudante deveria praticar agora para aprender de forma eficiente?**

## Princípio central

A decisão pedagógica é **determinística, auditável e explicável**. A IA gera conteúdo candidato; ela não decide o que o aluno estuda, não define o nível dele e não é confiável por definição.

```
Perfil do estudante
      ↓
Adaptive Learning Engine        ← determinístico
      ↓
Question Specification
      ↓
AI Gateway → LLM
      ↓
Validation Pipeline             ← correção · dificuldade · similaridade · segurança · qualidade
      ↓
Question Bank
      ↓
Aluno
```

O aluno **nunca** espera por uma chamada de LLM.

## Estado atual

**Fase 0 — Architecture.** Ainda não há código de aplicação. A documentação de arquitetura está completa e a stack está decidida (ADR-0001): **backend Spring Boot / Java 21, frontend Next.js, PostgreSQL + pgvector, Redis**.

## Documentação

| Documento | Conteúdo |
|---|---|
| [Auditoria e riscos](docs/architecture/00-auditoria.md) | Diagnóstico do projeto e registro de riscos classificado |
| [Arquitetura](docs/architecture/01-arquitetura.md) | Componentes, fronteiras e fluxo canônico |
| [Modelo de dados](docs/architecture/02-modelo-de-dados.md) | Schema inicial e decisões estruturais |
| [Estratégia de IA](docs/architecture/03-estrategia-ia.md) | Geração, validação, deduplicação, custo e falhas |
| [Design system](docs/architecture/04-design-system.md) | Identidade visual, tokens e padrões de experiência |
| [Roadmap](docs/roadmap.md) | Fases 0 a 10 com critérios objetivos de conclusão |
| [ADR-0001](docs/architecture/adr/ADR-0001-stack.md) | Escolha de stack — **aceito** |

## Definição de pronto

Nenhuma funcionalidade é considerada concluída sem estar: implementada · tipada · validada · testada · segura · observável · documentada · integrada · sem segredos expostos · sem erros críticos.
