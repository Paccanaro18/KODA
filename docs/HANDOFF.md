# KODA — Handoff de Sessão

> Escrito em 2026-08-31 para que uma nova sessão entre em contexto sem reler o histórico.
> **Leia este arquivo primeiro.** Ele resume estado, decisões e armadilhas já descobertas.

---

## 1. O que é o KODA

Plataforma de aprendizagem adaptativa para tecnologia (programação, DevOps, cloud, Linux, redes, bancos, containers, CI/CD, segurança, arquitetura, SRE, testes).

O objetivo não é um site com questões aleatórias. É responder continuamente:

> **O que este estudante deveria praticar agora para aprender de forma eficiente?**

### Princípio inegociável

A decisão pedagógica é **determinística, auditável e explicável**. A IA gera conteúdo candidato; **não** decide o que o aluno estuda, **não** define o nível dele, e **não** é confiável por definição.

```
Perfil do estudante
      ↓
Adaptive Learning Engine        ← determinístico, puro, sem I/O
      ↓
Question Specification
      ↓
AI Gateway → LLM
      ↓
Validation Pipeline (7 estágios)
      ↓
Question Bank
      ↓
Aluno
```

**O aluno nunca espera por uma chamada de LLM.** Geração é assíncrona, por fila, alimentando um banco de questões.

---

## 2. Onde tudo vive

| Item | Caminho |
|---|---|
| Projeto | `C:\Developer\koda` (**não** existe pasta chamada "KODA") |
| Repositório | `github.com/Paccanaro18/KODA` (público) |
| Backend | `src/main/java/com/paccanaro/koda/` |
| Frontend | `web/` |
| Infra local | `infra/docker-compose.yml` |
| Docs | `docs/` |

Histórico: começou em `C:\Developer\fakeduolingo` (esse diretório ainda existe com uma cópia antiga dos docs — **ignore**). O usuário moveu para `koda`, reaproveitando um scaffold Spring Boot vazio que era do projeto `fraudengine`.

---

## 3. Estado atual

**Fase 0 (Architecture) e Fase 1 (Authentication + Core) completas e verificadas.**

- Backend: 19/19 testes de integração verdes contra Postgres e Redis reais.
- Frontend: design system completo; lint, tipos e build de produção verdes.
- CI no GitHub Actions verde.

**Próximo passo: Fase 2 — Curriculum** (grafo de conceitos com pré-requisitos, validador que quebra o CI em ciclos, mapa de aprendizado usando os `SkillNode` que já existem).

Roadmap completo com critérios objetivos por fase: `docs/roadmap.md`.

---

## 4. Stack (ADR-0001, aceito)

- **Backend:** Java 21, Spring Boot **4.1.1**, Spring Security, JPA, Flyway, Redis, Actuator
- **Frontend:** Next.js 16.3.4, React 19.2.8, **Tailwind v4**, TypeScript
- **Dados:** PostgreSQL (imagem `pgvector/pgvector:pg17`) + Redis
- **Testes:** JUnit 5 + Testcontainers **2.x**

Escolhida por alinhamento com a fluência do usuário (dev Spring Boot) e porque os requisitos difíceis do sistema são correção, segurança, resiliência e testabilidade — não IA.

---

## 5. ⚠️ Armadilhas já descobertas (não repita)

### Spring Boot 4 modularizou as autoconfigurações

Adicionar a biblioteca **não basta**; é preciso o módulo de integração do Boot.

- `flyway-core` sozinho **nunca executa**. Precisa de `org.springframework.boot:spring-boot-flyway`.
  Sintoma: zero linhas de log do Flyway + Hibernate falhando com `Schema validation: missing table`.
- Isso derrubou os 19 testes de uma vez. O mesmo padrão vale para outras integrações.

### Outras mudanças do Boot 4 / ecossistema

| Coisa | Valor correto |
|---|---|
| Jackson | **Jackson 3** — pacote `tools.jackson.databind`, não `com.fasterxml.jackson.databind` |
| Starter web | `spring-boot-starter-webmvc` (não `-web`) |
| Starters de teste | Granulares: `spring-boot-starter-webmvc-test`, `-data-jpa-test` (não `spring-boot-starter-test`) |
| MockMvc | `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` |
| Testcontainers 2.x | Módulos prefixados: `testcontainers-postgresql`, `testcontainers-junit-jupiter` |
| `PostgreSQLContainer` | `org.testcontainers.postgresql.PostgreSQLContainer` |
| BouncyCastle | **Não gerenciado** pelo BOM. Versão fixa `1.84` via `bc-jdk18on-bom` (necessário para Argon2id) |

**Como verificar sem adivinhar:** `mvnw dependency:tree`, e `jar tf` no jar em `~/.m2` para achar o pacote real de uma classe.

### Tailwind v4 — cascata de layers

Regras **fora** de qualquer `@layer` vencem regras dentro de `@layer`. Um `* { border-color: ... }` solto anula todas as utilities de cor de borda. Reset global tem que ir em `@layer base`.

Também: não faça `--shadow-sm: var(--shadow-sm)` dentro de `@theme inline` — é referência circular.

### React 19 — lint bloqueia `setState` síncrono em effect

Ler estado externo (classe do DOM, media query) com `useEffect` + `setState` é erro de lint (`react-hooks/set-state-in-effect`). Use **`useSyncExternalStore`**. Já aplicado em `ThemeProvider` e no hook `usePrefersReducedMotion`.

### Ambiente Windows

- `JAVA_HOME` não está definido e o `PATH` aponta para **JRE 8**. Compilar exige:
  `JAVA_HOME="/c/Users/pacsss/.jdks/openjdk-26.0.2"` antes do `mvnw`.
- `npm install` é **muito lento** aqui (minutos; provavelmente Defender). Rode em background.
- Se `npm` der `ENOTEMPTY`, há processo node órfão travando arquivos: mate os processos e reinstale limpo.
- WSL2 foi instalado nesta máquina; Docker Desktop funciona (necessário para Testcontainers).
- Heredocs grandes no Bash tool quebram no parser — use a ferramenta Write para arquivos de conteúdo.
- O preview headless do browser **não invalida estilos** de elementos já montados nem repinta após scroll. Screenshots saem em branco e `getComputedStyle` retorna valores obsoletos. Não confunda isso com bug do produto — verificado criando elemento novo, que estiliza corretamente.

---

## 6. Decisões de segurança já tomadas

### Sessão (ADR-0002, aceito)

Access token JWT curto (15 min) + refresh token **opaco** rotativo em cookie `httpOnly` + `Secure` + `SameSite=Strict` + `Path=/api/v1/auth`.

- O refresh token **nunca** vai no corpo da resposta (um XSS o roubaria do localStorage).
- É persistido como **SHA-256**; dump do banco não entrega sessões ativas.
- Rotaciona a cada uso. Reuso de token já rotacionado = sinal de roubo → revoga **toda** a árvore de sessões do usuário (derruba o dono legítimo também; é intencional, não há como distinguir).
- `SameSite=Strict` é o que fecha o CSRF — por isso o CSRF do Spring pôde ser desligado.
- `subject` do JWT é o id do usuário, nunca o e-mail (token circula por logs e proxies).

### Bug sutil já corrigido — não reintroduza

`AuthService.refresh()` é `@Transactional`. A revogação por detecção de reuso era desfeita pelo **rollback** da `ApiException` lançada logo em seguida — a defesa contra roubo nunca persistia. Corrigido com `SessionRevocationService` usando `@Transactional(REQUIRES_NEW)`, em bean separado para atravessar o proxy do Spring.

### Outras defesas ativas

- Argon2id via `DelegatingPasswordEncoder` (prefixo `{id}` permite migrar de algoritmo sem invalidar senhas).
- Login: resposta e latência **idênticas** para e-mail inexistente e senha errada (hash sempre verificado contra um dummy gerado em runtime) — impede enumeração de contas.
- Brute force: contadores separados por conta e por IP no Redis; e-mail vira hash (dump do cache não revela contas).
- Rate limit por IP; **`X-Forwarded-For` deliberadamente ignorado** (é forjável). Atrás de proxy, configurar `server.forward-headers-strategy=framework`.
- `authorizeHttpRequests` com **default deny**; `@EnableMethodSecurity` habilitado.
- Secure headers: CSP, HSTS, frame-deny, referrer-policy.
- `GlobalExceptionHandler`: código estável + mensagem genérica ao cliente; detalhe técnico só no log.

---

## 7. Modelo de dados — decisões estruturais

Detalhe completo em `docs/architecture/02-modelo-de-dados.md`. As três que governam tudo:

1. **`question_attempts` é append-only e é a fonte da verdade.** Todo o modelo de conhecimento é projeção reconstruível a partir dele.
2. **Questões são versionadas e imutáveis após publicação.** A tentativa referencia a *versão*, não a questão — senão editar uma questão corrompe retroativamente o histórico.
3. **Currículo e conteúdo são separados.** Currículo é curado e versionado no repo; questões são geradas e validadas. Confundir permitiria à IA redefinir o que é ensinado.

Também: `mastery` (estimativa de domínio, decai com o tempo) **nunca** se confunde com XP (esforço). Progressão pedagógica: `unseen → recognizes → understands → applies → solves → masters`.

Migration atual: `V1__initial_schema.sql` (`users`, `profiles`, `refresh_tokens`). Migrations são **forward-only**; nunca editar uma já aplicada.

---

## 8. API existente (Fase 1)

| Método | Rota | Acesso |
|---|---|---|
| `POST` | `/api/v1/auth/register` | público |
| `POST` | `/api/v1/auth/login` | público |
| `POST` | `/api/v1/auth/refresh` | cookie |
| `POST` | `/api/v1/auth/logout` | autenticado |
| `GET` | `/api/v1/auth/me` | autenticado |
| `GET` | `/actuator/health` | público |
| `/actuator/**`, `/api/v1/admin/**` | | apenas `ADMIN` |

---

## 9. Design system (frontend)

`docs/architecture/04-design-system.md` tem a direção completa. Essencial:

- **Tokens em duas camadas** em `web/app/globals.css`: escala bruta (`--color-brand-500`) e semântica (`--accent`). Componentes consomem **apenas** a semântica.
- Paleta: índigo `#635BFF` (primary), ciano `#22D3EE` (secondary), âmbar `#F59E0B` (recompensa), verde (sucesso), vermelho (erro), fundo escuro `#0B1020`.
- Fontes: Inter (UI) + JetBrains Mono (código), via `next/font`.
- Dark mode é cidadão de primeira classe, com hierarquia própria de superfícies — **não** é o claro invertido.
- **Nenhum estado depende só de cor.** Acerto/erro têm ícone; `locked` tem borda tracejada; `mastered` tem estrela; streak em risco fica vazado. Requisito de acessibilidade.
- `prefers-reduced-motion` respeitado no CSS e na animação imperativa de XP.
- **Koda AI** é um orb geométrico abstrato (núcleo em losango que ecoa a marca do streak), com estados diferenciados por ritmo de movimento. Não é robô nem mascote com rosto.
- Componentes em `web/components/ui/`: `Button`, `Card`, `Badge`, `Progress`, `ProgressRing`, `AnswerOption`, `SkillNode`, `Xp`, `Streak`, `Achievement`, `CodeBlock`, `Feedback`, `KodaOrb`.
- A rota `/` é a **referência viva** do design system — não é tela do produto. Nenhuma tela do produto existe ainda, por decisão: o sistema vem antes das telas.

**Tom do feedback:** o erro nunca é tratado como fracasso. `Feedback` diz "Não exatamente", nunca "Errado". Um aluno que se sente punido para de praticar.

---

## 10. Como rodar e verificar

```bash
# Backend (exige Docker para os testes)
cd C:\Developer\koda
JAVA_HOME="/c/Users/pacsss/.jdks/openjdk-26.0.2" KODA_JWT_SECRET="segredo-local-de-teste-com-mais-de-32-bytes-ok" ./mvnw -B verify

# Infra local
docker compose -f infra/docker-compose.yml --env-file .env up -d

# Frontend
cd web && npm run dev     # design system em http://localhost:3000
npm run lint && npm run build
```

CI (`.github/workflows/ci.yml`): 4 jobs — build/testes Java, frontend (npm ci + lint + tsc + build), gitleaks (binário v8.30.1 com allowlist em `.gitleaks.toml`), Trivy (`aquasecurity/trivy-action@v0.36.0`).

Falhas de CI já resolvidas: `mvnw` precisava do bit `100755` (Windows não preserva); tags do trivy-action usam prefixo `v`; gitleaks detectava as senhas de teste (allowlist é por **valor**, não por caminho — liberar `src/test/**` cegaria o scanner).

---

## 11. Como o usuário quer trabalhar

- **Fala português.** Responder em português.
- **Ele executa os commits.** Entregar os comandos git prontos, em blocos ```bash separados (um comando por bloco), com a mensagem de commit já redigida. Não rodar `git commit` nem `git push`.
- Valoriza verificação real: rodar o build/teste e reportar o resultado de fato, nunca afirmar que passa sem ter visto.
- Não inventar versões de dependência nem APIs — verificar no registry, no BOM ou no jar.

---

## 12. Documentação do projeto

| Arquivo | Conteúdo |
|---|---|
| `docs/architecture/00-auditoria.md` | Diagnóstico + 24 riscos classificados (5 CRITICAL) |
| `docs/architecture/01-arquitetura.md` | Componentes, fronteiras, fluxo canônico |
| `docs/architecture/02-modelo-de-dados.md` | Schema e decisões estruturais |
| `docs/architecture/03-estrategia-ia.md` | Geração, validação em 7 estágios, dedup em 4 camadas, custo, falhas |
| `docs/architecture/04-design-system.md` | Identidade visual e tokens |
| `docs/roadmap.md` | Fases 0–10 com critérios objetivos |
| `docs/architecture/adr/ADR-0001-stack.md` | Escolha de stack |
| `docs/architecture/adr/ADR-0002-modelo-de-sessao.md` | Modelo de sessão e tokens |

### Riscos CRITICAL a manter em mente

| ID | Risco |
|---|---|
| `SEC-01` | Execução de código do aluno sem sandbox = RCE. Container efêmero, sem rede, FS read-only, cap-drop, limites, timeout. Nunca no processo da API |
| `SEC-02` | Indirect prompt injection — conteúdo do usuário nunca no bloco de instruções; saída do LLM sempre validada |
| `ARC-01` | IA no caminho crítico. Geração é assíncrona; o aluno nunca espera o LLM |
| `COST-01` | Custo de LLM sem teto. Reuso é o mecanismo principal: uma questão validada serve milhares de alunos |
| `AI-01` | Questão com resposta errada ou ambígua — **o produto ensina errado**. É o pior dano possível |
