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

**Fase 1 — Authentication + Core (backend).** Stack decidida em ADR-0001: Spring Boot 4 / Java 21, PostgreSQL + pgvector, Redis; frontend Next.js a partir da próxima entrega.

Já implementado: registro, login, refresh rotativo, logout, `/me`, RBAC com autorização server-side, hashing Argon2id, proteção contra brute force, rate limiting, secure headers, migrations Flyway e observabilidade via Actuator.

## Como rodar

Requisitos: JDK 21+, Docker.

```bash
cp .env.example .env      # preencha os segredos
docker compose -f infra/docker-compose.yml --env-file .env up -d
./mvnw spring-boot:run
```

Testes (sobem Postgres e Redis reais via Testcontainers — exigem Docker):

```bash
./mvnw verify
```

## API — Fase 1

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | público | Cria conta e já autentica |
| `POST` | `/api/v1/auth/login` | público | Autentica |
| `POST` | `/api/v1/auth/refresh` | cookie | Rotaciona a sessão |
| `POST` | `/api/v1/auth/logout` | autenticado | Revoga a sessão |
| `GET` | `/api/v1/auth/me` | autenticado | Dados do próprio usuário |
| `GET` | `/actuator/health` | público | Health check |

O access token vai no corpo da resposta (`Bearer`, 15 min). O refresh token **não** — ele viaja em cookie `httpOnly`, fora do alcance de JavaScript. Ver [ADR-0002](docs/architecture/adr/ADR-0002-modelo-de-sessao.md).

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
| [ADR-0002](docs/architecture/adr/ADR-0002-modelo-de-sessao.md) | Modelo de sessão e tokens — **aceito** |

## Definição de pronto

Nenhuma funcionalidade é considerada concluída sem estar: implementada · tipada · validada · testada · segura · observável · documentada · integrada · sem segredos expostos · sem erros críticos.
