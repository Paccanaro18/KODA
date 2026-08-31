# ADR-0001 — Escolha de stack

- **Status:** **Aceito**
- **Data:** 2026-08-31
- **Decisão:** Opção A — Spring Boot (Java 21) + Next.js

## Contexto

O projeto é greenfield (ver `../00-auditoria.md`). Não há restrição de compatibilidade, então a escolha é livre — e por isso mesmo é a decisão de maior alcance do projeto: define linguagem, ecossistema, modelo de concorrência, ferramentas de teste, forma do deploy e o custo de manutenção pelos próximos anos.

Restrições reais que a stack precisa atender:

- **Domínio determinístico e fortemente testável** — o Adaptive Learning Engine é o coração e exige tipagem forte e testes densos.
- **PostgreSQL com `pgvector`** para busca de similaridade.
- **Trabalho assíncrono** — filas para pré-geração, embeddings e recalibração.
- **Resiliência de integração** — timeout, retry, backoff, circuit breaker no AI Gateway.
- **Segurança madura** — authn/authz, RBAC, rate limiting, headers.
- **Frontend rico** — a direção visual do KODA exige animação, microinterações e um design system consistente. Isso implica React na prática.
- **Autor solo** — a stack precisa ser sustentável por uma pessoa.

## Opções consideradas

### Opção A — Backend Spring Boot (Java 21) + Frontend Next.js

**A favor**
- Alinhada à fluência demonstrada do autor: `Partner-api`, `fraudengine` e `rate-limiting` são todos Spring Boot, com JPA, Redis e PostgreSQL. Fluência é velocidade real, não teórica — e reduz a chance de erro em decisões de segurança.
- Spring Security cobre authn, RBAC e autorização em nível de método diretamente (`SEC-03`).
- Resilience4j entrega timeout, retry, backoff e circuit breaker do AI Gateway sem construção artesanal.
- Micrometer + OpenTelemetry dão a observabilidade da seção 23 de forma nativa.
- Testcontainers torna teste de integração com Postgres real trivial.
- Tipagem forte e domínio explícito favorecem o Engine determinístico.

**Contra**
- Dois runtimes, duas toolchains, dois deploys.
- Ecossistema de IA em Java é menos imediato que em TypeScript/Python (contornável: as APIs são HTTP e o Gateway é abstração própria de qualquer forma).
- Requer corrigir `ENV-01` (`JAVA_HOME` apontando para JDK 21+).

### Opção B — Monorepo TypeScript (Next.js + NestJS/Fastify + Prisma)

**A favor**
- Uma linguagem só; tipos compartilhados entre frontend e backend.
- Ecossistema de IA e embeddings mais direto.
- Menor cerimônia; iteração inicial mais rápida.

**Contra**
- Menor fluência do autor no backend, o que custa mais em decisões de segurança e concorrência.
- Garantias de tipo mais fracas em runtime; exige disciplina extra de validação nas bordas.
- Resiliência e observabilidade exigem montagem manual de mais peças.

### Opção C — Backend Python (FastAPI)

Descartada. Python não está instalado no ambiente, não corresponde à trajetória do autor, e a vantagem de ecossistema de IA é pequena aqui — o KODA consome APIs HTTP de LLM, não treina modelos.

## Decisão

**Opção A — Spring Boot (Java 21) + Next.js.** Aceita em 2026-08-31.

O fator decisivo é que os requisitos mais difíceis deste sistema não são de IA; são de **correção, segurança, resiliência e testabilidade do domínio determinístico**. É exatamente onde o ecossistema Spring é mais forte e onde o autor já é produtivo. A parte de IA é uma integração HTTP encapsulada atrás do AI Gateway — abstração que precisa existir em qualquer stack, e cujo custo é praticamente idêntico nas duas opções.

Se o critério fosse velocidade de protótipo nas primeiras duas semanas, a Opção B venceria. O critério declarado, porém, é sustentar milhares de estudantes com segurança e correção.

## Consequências

- Backend: Java 21, Spring Boot 4.x, Spring Security, Spring Data JPA, Flyway, Resilience4j, Micrometer, Testcontainers, JUnit 5.
- Frontend: Next.js + TypeScript, Tailwind sobre design tokens próprios, Framer Motion para microinterações.
- Dados: PostgreSQL + `pgvector`; Redis para cache, rate limit e filas.
- Sandbox: serviço isolado em container, orquestrado via Docker.
- Repositório: monorepo com `/backend`, `/web`, `/sandbox`, `/docs`, `/infra`.
- **Ação obrigatória antes da Fase 1:** definir `JAVA_HOME` para o JDK 21+ e remover a JRE 8 do `PATH` (`ENV-01`).

## Como será testado

A decisão é validada na Fase 1: se autenticação, RBAC, migrations, observabilidade e CI ficarem de pé com os critérios de conclusão atendidos, a stack está confirmada. Caso contrário, a reversão ainda é barata — é justamente por isso que a decisão é tomada agora, e não depois.
