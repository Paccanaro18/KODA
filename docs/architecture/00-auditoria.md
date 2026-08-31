# KODA — Auditoria Inicial e Registro de Riscos

> Data: 2026-08-31 · Fase 0 (Architecture) · Status: concluída

## A. Diagnóstico

### Resultado central

**O projeto é greenfield absoluto.** Não existe código, stack, arquitetura, banco, autenticação, API, infraestrutura ou dívida técnica a auditar.

| Item | Estado verificado |
|---|---|
| Diretório local `C:\Developer\fakeduolingo` | Vazio — 0 arquivos, 0 subdiretórios |
| Repositório `github.com/Paccanaro18/KODA` | Existe e é acessível; **zero refs** (`git ls-remote` retorna vazio com exit 0) |
| Git local | Não inicializado |
| Stack / dependências | Inexistentes |
| Banco de dados | Inexistente |
| Autenticação | Inexistente |
| APIs | Inexistentes |
| Infraestrutura / IaC | Inexistente |
| Testes / CI | Inexistentes |

Consequência prática: **não há dívida técnica herdada nem restrição de compatibilidade.** Todas as decisões estruturais estão abertas e o custo de acertá-las agora é o mais baixo que jamais será. Em contrapartida, não existe fundação alguma — autenticação, modelo de dados e o Adaptive Learning Engine precisam ser projetados antes de qualquer tela.

### Ambiente de desenvolvimento verificado

| Ferramenta | Versão | Observação |
|---|---|---|
| Node.js | 24.19.0 | OK |
| npm | 11.17.0 | OK |
| Docker | 29.7.2 | OK — habilita Postgres, Redis e o sandbox local |
| Git | 2.55.0 | OK |
| Java no `PATH` | **1.8.0_501 (JRE)** | ⚠️ risco `ENV-01` |
| JDK 26 | `~/.jdks/openjdk-26.0.2` | Instalado, porém fora do `PATH` |
| `JAVA_HOME` | **não definido** | ⚠️ risco `ENV-01` |
| pnpm | ausente | Instalar se a stack escolhida for TypeScript |
| Python | ausente | Não é requisito da stack proposta |
| GitHub CLI (`gh`) | ausente | Opcional — os commits funcionam com `git` puro |

### Contexto do autor (informa a escolha de stack)

Os projetos vizinhos em `C:\Developer` são consistentemente **Java + Spring Boot**:

- `Partner-api` — Maven + Spring Boot
- `fraudengine` — Spring Boot 4.1.1, Java 21, JPA, Redis, PostgreSQL, Validation, Lombok
- `rate-limiting` — Spring Boot + Docker Compose

Isso é um dado de engenharia, não trivia: a velocidade real de entrega e a qualidade das decisões de segurança são maiores na stack em que o autor já é fluente. Ver `adr/ADR-0001-stack.md`.

---

## B. Riscos

Severidade: `CRITICAL` (bloqueia produção ou causa dano irreversível) · `HIGH` (dano material provável) · `MEDIUM` (degrada qualidade ou custo) · `LOW` (incômodo gerenciável).

### Segurança

| ID | Risco | Sev. | Mitigação |
|---|---|---|---|
| SEC-01 | **Execução de código do estudante** (exercícios de programação, SQL, terminal). Sem isolamento real, isso é RCE no servidor. | CRITICAL | Sandbox fora do processo: container efêmero, sem rede, filesystem read-only, `--cap-drop ALL`, `--pids-limit`, limite de CPU/memória, timeout duro, usuário não-root, perfil seccomp. Nunca `eval` ou `Runtime.exec` no processo da API. |
| SEC-02 | **Indirect prompt injection** — código ou texto do usuário entra no prompt e reescreve as instruções do modelo. | CRITICAL | Conteúdo do usuário nunca entra no bloco de instruções; entra delimitado como dado. Saída do LLM é sempre não confiável e validada por schema. O LLM não tem acesso a banco, filesystem ou rede — apenas via Tool Gateway com autorização por chamada. |
| SEC-03 | **IDOR** em progresso, tentativas e perfis de aprendizagem. | HIGH | Toda query escopada pelo `user_id` do token, no servidor. Autorização em nível de método, nunca no frontend. Teste de autorização obrigatório por endpoint. |
| SEC-04 | Questão gerada por IA com conteúdo inseguro, incorreto ou ofensivo chega ao aluno. | HIGH | Pipeline de validação obrigatório, `review_status` explícito e kill switch por questão e por lote de geração. |
| SEC-05 | Vazamento de segredos (chave do provedor de LLM) em código, log ou frontend. | HIGH | Segredos apenas via variável de ambiente / secret manager, nunca no cliente. Logs com redaction. Scanner de segredos no CI. |
| SEC-06 | Brute force e credential stuffing no login. | HIGH | Rate limit por IP e por conta, backoff progressivo, hashing Argon2id, mensagens de erro sem enumeração de usuário. |
| SEC-07 | XSS via conteúdo de questão (enunciado e explicação renderizados). | HIGH | Renderizar como texto ou Markdown sanitizado com allowlist; nunca injetar HTML cru; CSP restritiva. |
| SEC-08 | SSRF a partir do backend ao buscar recursos referenciados por conteúdo. | MEDIUM | Não buscar URLs vindas de conteúdo gerado. Se for necessário: allowlist de domínios e bloqueio de faixas privadas. |

### Arquitetura

| ID | Risco | Sev. | Mitigação |
|---|---|---|---|
| ARC-01 | **IA no caminho crítico da resposta** — o aluno clica e espera o LLM gerar. Latência de segundos, falha visível ao usuário e custo por clique. | CRITICAL | Banco de questões pré-geradas com geração assíncrona por fila. O request do aluno **nunca** chama o LLM de forma síncrona. |
| ARC-02 | Acoplamento entre o Adaptive Engine e a geração de questões. | HIGH | O Engine emite uma *especificação* e não conhece o LLM. O Question Engine serve do banco; se não houver questão adequada, enfileira geração e serve o melhor candidato disponível. |
| ARC-03 | Lógica pedagógica espalhada por controllers. | MEDIUM | Domínio isolado, puro e testável, sem I/O, com portas para persistência. |

### Escalabilidade e custo

| ID | Risco | Sev. | Mitigação |
|---|---|---|---|
| COST-01 | **Custo de LLM cresce sem teto** conforme a base de usuários. | CRITICAL | Reuso agressivo — uma questão boa serve milhares de alunos. Cache, orçamento por usuário e por dia, modelo barato para tarefas simples, métricas de token e custo por geração, circuit breaker por orçamento. |
| SCA-01 | Busca de similaridade semântica O(n) sobre todo o banco. | HIGH | `pgvector` com índice HNSW, escopo por tópico e pré-filtro determinístico (hash canônico) antes do vetorial. |
| SCA-02 | Sandbox como gargalo e vetor de DoS. | HIGH | Fila dedicada, pool de workers, quota por usuário e timeout curto. |

### Dados

| ID | Risco | Sev. | Mitigação |
|---|---|---|---|
| DAT-01 | Perder o histórico de tentativas invalida todo o modelo de conhecimento. | HIGH | `question_attempts` é append-only e é a fonte da verdade. `skill_scores` é projeção reconstruível a partir dele. |
| DAT-02 | Editar uma questão altera o significado de tentativas passadas. | HIGH | Questões versionadas e imutáveis após publicação. A tentativa referencia a **versão**, não a questão. |
| DAT-03 | Migrations destrutivas em produção. | MEDIUM | Migrations versionadas e forward-only. Nunca editar uma migration já aplicada. |

### IA e qualidade pedagógica

| ID | Risco | Sev. | Mitigação |
|---|---|---|---|
| AI-01 | O LLM gera questão com resposta errada ou ambígua e o produto **ensina errado**. É o pior dano possível aqui. | CRITICAL | Validação multicamada, revisão humana obrigatória antes de publicar e sinal de qualidade vindo dos próprios alunos — taxa de acerto anômala derruba a questão automaticamente. |
| AI-02 | Repetição de questões destrói a percepção de aprendizado. | HIGH | Pipeline de deduplicação em quatro camadas. |
| AI-03 | A dificuldade declarada pelo LLM não é confiável. | HIGH | A dificuldade declarada é apenas um *prior*. A dificuldade real é medida empiricamente pela taxa de acerto e recalibrada. |

### UX e manutenibilidade

| ID | Risco | Sev. | Mitigação |
|---|---|---|---|
| UX-01 | A gamificação vira métrica de vaidade e o aluno não aprende. | MEDIUM | XP derivado de evidência de domínio, não de cliques. Revisão espaçada vale XP. |
| UX-02 | O aluno não entende por que recebeu determinada questão e perde a confiança no sistema. | MEDIUM | Toda seleção carrega um `selection_reason` legível, exibido na interface. |
| MNT-01 | Tipos de questão hardcoded exigem reescrita a cada novo tipo. | HIGH | Registry de tipos — cada tipo implementa render, validate e score. |
| ENV-01 | O `PATH` aponta para a JRE 8 e `JAVA_HOME` não está definido; o build Java falha ou usa o runtime errado. | MEDIUM | Definir `JAVA_HOME` para o JDK 21+ antes da Fase 1, caso a stack escolhida seja Java. |

---

## Conclusão da auditoria

Não há nada a preservar, migrar ou refatorar. A recomendação é seguir direto para a definição de stack (`adr/ADR-0001-stack.md`) e para a Fase 1 do roadmap, tratando `SEC-01`, `SEC-02`, `ARC-01`, `COST-01` e `AI-01` como restrições de projeto desde o primeiro commit — todos os cinco são caros ou impossíveis de corrigir depois que o sistema já está em produção.
