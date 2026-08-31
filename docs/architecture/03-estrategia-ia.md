# KODA — Estratégia de IA

> Fase 0 (Architecture) · Status: proposta

## E. Premissa

A IA é tratada como **um colaborador produtivo e não confiável**: gera muito conteúdo útil rapidamente, e nada do que produz entra no produto sem verificação. Todo o desenho abaixo decorre disso.

## 1. Como gerar questões

A geração parte de uma especificação estruturada produzida pelo Adaptive Learning Engine — nunca de um pedido vago.

```json
{
  "topic": "javascript.promises",
  "concept": "promise_chaining",
  "difficulty": 3,
  "question_type": "code_analysis",
  "learning_objective": "Entender a ordem de execução em cadeias de promises",
  "language": "javascript",
  "estimated_time_seconds": 90,
  "avoid_similar_to": ["<hash>", "<hash>"],
  "common_mistakes_to_target": ["assumir que .then encadeado é síncrono"]
}
```

A saída é exigida em schema estruturado, com `question`, `options`, `correct_answer`, `explanation`, `distractor_rationales`, `concepts`, `difficulty`, `estimated_time_seconds` e `confidence`. O campo `confidence` é sinal de triagem, não licença para pular validação.

**Geração em lote e assíncrona.** Workers mantêm o banco abastecido por tópico e faixa de dificuldade, monitorando cobertura. O request do aluno nunca dispara uma chamada de LLM síncrona (`ARC-01`).

## 2. Como validar

Pipeline de sete estágios; qualquer reprovação descarta o candidato e registra o motivo em `ai_generations`.

```
LLM output
   ↓ 1. Schema        estrutura, tipos, campos obrigatórios, limites de tamanho
   ↓ 2. Correção      a resposta correta é de fato correta e é ÚNICA
   ↓ 3. Dificuldade   bate com a faixa pedida? heurísticas + histórico
   ↓ 4. Currículo     testa o conceito solicitado e respeita pré-requisitos
   ↓ 5. Deduplicação  4 camadas (seção 3)
   ↓ 6. Segurança     conteúdo inseguro, comandos destrutivos, injection, PII
   ↓ 7. Qualidade     scoring composto com limiar mínimo
   ↓
review_status = 'in_review'  →  aprovação humana  →  'published'
```

O **estágio 2 é o mais importante do sistema** e é o que impede o dano de `AI-01`. Onde há código, a validação é executável, não textual: parseia, compila quando aplicável e roda no sandbox com timeout, sem rede e com limite de memória. Para múltipla escolha, verifica-se que exatamente uma alternativa satisfaz o critério — questão com zero ou duas respostas corretas é rejeitada automaticamente.

Uma questão é rejeitada quando não tem resposta inequívoca, tem múltiplas respostas corretas indevidamente, não tem resposta correta, está fora do nível pedido, contém informação factualmente incorreta, contém código inválido, tem explicação que contradiz a resposta, é muito semelhante a outra, contém conteúdo inseguro ou não testa de fato o conceito alvo.

Scoring: `quality_score`, `difficulty_score`, `clarity_score`, `uniqueness_score`, `correctness_score`.

**Validação cruzada por segunda passagem.** Um modelo diferente (ou o mesmo com prompt de crítica adversarial) tenta refutar a questão: resolver, encontrar ambiguidade, apontar segunda resposta válida. Barato comparado ao custo de ensinar errado, e não substitui a revisão humana — apenas reduz o volume que chega a ela.

## 3. Como evitar repetição

Quatro camadas, do mais barato ao mais caro. Depender só de embeddings seria frágil.

| # | Camada | Mecanismo |
|---|---|---|
| 1 | Duplicata exata | Hash do payload normalizado |
| 2 | Hash canônico | Normaliza identificadores, literais, ordem das alternativas e formatação; captura "mesma questão com variáveis trocadas" |
| 3 | Similaridade semântica | `pgvector` + cosseno, escopado por tópico, com limiar calibrado |
| 4 | Histórico do usuário | `user_question_exposure` + janela de recência por conceito |

Além disso: limite de exposição do mesmo conceito em sequência, e penalização de estrutura repetida mesmo quando o conteúdo difere. A camada 2 é a que pega o modo de falha mais comum de LLM — reescrever a mesma questão com nomes diferentes.

`duplicate_rate` é métrica monitorada. Subida indica saturação do tópico e dispara ajuste da estratégia de geração.

## 4. Como controlar dificuldade

A dificuldade declarada pelo modelo é apenas um *prior* (`AI-03`). A dificuldade real é **medida**: um job recalcula `measured_difficulty` a partir da taxa de acerto observada, ponderada pelo nível dos alunos que responderam. Divergência grande entre declarada e medida é sinal de má calibração e alimenta a revisão de prompt.

A progressão de dificuldade do aluno é decidida pelo Engine determinístico, mirando uma faixa de acerto produtiva — nem trivial, nem frustrante — e nunca pelo LLM.

## 5. Como avaliar respostas

- **Tipos objetivos** (múltipla escolha, verdadeiro/falso, ordenação, matching, saída de código): correção determinística no servidor. A IA não participa. O gabarito nunca é enviado ao cliente antes da submissão.
- **Código**: execução em sandbox contra casos de teste. O veredito vem dos testes, não do modelo.
- **Resposta curta / discursiva**: a IA auxilia, com rubrica estruturada, e o resultado é tratado como estimativa. Divergência com a confiança baixa vai para revisão. Uma avaliação por IA nunca reprova sozinha de forma definitiva.
- **Explicação e dica**: a IA gera, e são conteúdo exibido — logo passam pelos mesmos filtros de segurança e sanitização.

A classificação de erro (`conceptual`, `syntax`, `careless`, `misunderstanding`, `knowledge_gap`, `guess`) combina heurísticas determinísticas — tempo de resposta muito baixo sugere chute, erro repetido no mesmo conceito sugere lacuna — com apoio da IA para os casos ambíguos.

## 6. Como controlar custo

`COST-01` é risco crítico e o controle é arquitetural, não incidental.

- **Reuso é o mecanismo principal**: uma questão validada serve milhares de alunos. Gerar por clique seria insustentável.
- Pré-geração assíncrona guiada por cobertura, não por demanda instantânea.
- Cache de embeddings e de explicações.
- Roteamento por tarefa: modelo econômico para classificação e triagem, modelo forte para geração e crítica.
- Orçamento por usuário, por dia e global, com circuit breaker ao estourar.
- `input_tokens`, `output_tokens`, `cost_usd` e `latency_ms` gravados por geração em `ai_generations` — custo observável por tópico, por prompt e por modelo.
- Deduplicação antes de gerar: se o tópico já está bem coberto, não se gera.

## 7. Como versionar prompts

Prompts são artefatos de código: versionados no repositório, com identificador semântico gravado em cada geração (`prompt_version`). Isso permite correlacionar queda de qualidade a uma mudança específica e reverter. O mesmo vale para `model` — trocar de modelo é uma mudança rastreável, com comparação de qualidade em conjunto de avaliação antes de promover.

## 8. Como tratar falhas do modelo

| Falha | Tratamento |
|---|---|
| Timeout | Retry com backoff exponencial e teto de tentativas |
| Saída fora do schema | Uma tentativa de correção; persistindo, descarta e registra |
| Indisponibilidade do provedor | Circuit breaker abre; geração pausa. **O aluno não é afetado** — o banco já tem questões |
| Degradação de qualidade | Queda em `quality_score` agregado dispara alerta e pode congelar a publicação automática |
| Orçamento estourado | Geração suspensa; alerta ao admin |
| Conteúdo inseguro recorrente | Bloqueio do prompt/versão e revisão manual |

A propriedade que torna tudo isso tolerável: **falha de IA degrada a taxa de reposição do banco, não a experiência do aluno.**

## 9. Segurança da IA

- Conteúdo do usuário e conteúdo de terceiros entram nos prompts **como dado delimitado**, nunca como instrução.
- Nenhum segredo, credencial, token ou dado pessoal entra em prompt.
- A saída do modelo é sempre não confiável — inclusive quando parece uma instrução.
- Se a IA usar ferramentas: `LLM → Tool Gateway → autorização → validação → rate limit → ferramenta`. Sem acesso direto a banco, filesystem ou infraestrutura.
- Defesas contra prompt injection direto e indireto, jailbreak, tentativa de extrair o system prompt, exfiltração de dados, abuso de ferramentas e geração de comandos destrutivos.
- Logs de prompt com redaction; nunca registrar dados sensíveis.
