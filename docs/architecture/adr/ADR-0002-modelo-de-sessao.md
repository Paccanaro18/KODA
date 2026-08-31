# ADR-0002 — Modelo de sessão e tokens

- **Status:** Aceito
- **Data:** 2026-08-31
- **Contexto de fase:** Phase 1 — Authentication + Core

## Contexto

O KODA precisa autenticar estudantes numa API stateless consumida por um frontend Next.js, e mais tarde por workers e possivelmente um app móvel. As restrições relevantes:

- `SEC-03` — autorização sempre server-side, com escopo por dono.
- `SEC-07` — XSS é risco `HIGH`; a sessão não pode ser roubável por JavaScript.
- CSRF precisa ser tratado sem depender de um token sincronizador em cada request.
- Revogação precisa ser possível: logout real, e resposta a roubo de credencial.
- As fases seguintes trazem workers assíncronos, o que desfavorece sessão presa a um servidor.

## Opções consideradas

### Opção A — Sessão server-side em Redis, cookie de sessão

**A favor:** revogação instantânea e trivial; nada de criptografia própria; o estado da sessão pode crescer sem inchar o token.

**Contra:** cada request autenticado vira uma ida ao Redis; a disponibilidade do Redis passa a ser requisito de autenticação, não apenas de rate limit. Acopla a autenticação a um serviço com estado, o que atrapalha os workers das fases 5 e 6.

### Opção B — JWT de longa duração no `localStorage`

**A favor:** o mais simples de implementar; nenhum estado no servidor.

**Contra:** **inaceitável.** Qualquer XSS lê o `localStorage` e rouba a sessão inteira, e um JWT de longa duração não é revogável — o atacante mantém acesso até o token expirar naturalmente. Descartada por conflito direto com `SEC-07`.

### Opção C — Access token JWT curto + refresh token opaco rotativo em cookie httpOnly

**A favor:** o access token é validado por assinatura, sem consultar o banco — barato e sem estado. Sua vida curta (15 min) limita a janela de dano. O refresh token fica em cookie `httpOnly`, fora do alcance de JavaScript, então um XSS não o rouba. Sendo opaco e persistido como hash, é revogável de verdade. A rotação a cada uso permite **detectar roubo**: se um token já rotacionado reaparece, alguém tem uma cópia.

**Contra:** mais peças móveis; exige tabela de refresh tokens e limpeza periódica; a revogação do access token não é imediata — ele vale até expirar.

## Decisão

**Opção C.**

O ponto que decide é a combinação de duas propriedades que as outras opções não entregam juntas: sessão inacessível a JavaScript (contra `SEC-07`) e revogação real com detecção de roubo. O custo — uma tabela e um job de limpeza — é pequeno perto disso.

A janela de até 15 minutos em que um access token revogado continua válido é um risco aceito conscientemente. Para operações sensíveis que vierem a existir (mudança de senha, exclusão de conta, ações de admin), a mitigação é reverificação explícita no momento da ação, não encurtar o token para todos.

## Consequências

- `refresh_tokens` guarda **SHA-256** do token, nunca o valor em claro: um dump do banco não entrega sessões ativas.
- Cookie com `httpOnly`, `Secure`, `SameSite=Strict` e `Path=/api/v1/auth`.
- `SameSite=Strict` é o que fecha o CSRF no endpoint de refresh — é por isso que a proteção CSRF do Spring pôde ser desligada numa API que, fora esse cookie, é puramente bearer.
- Reuso de token rotacionado revoga **toda** a árvore de sessões do usuário. Isso derruba também o dono legítimo, e é intencional: naquele ponto não há como distinguir os dois.
- O `subject` do JWT é o id do usuário, nunca o e-mail — o token circula por logs e proxies e não deve carregar dado pessoal.
- Pendente para a Fase 9: job de limpeza de tokens expirados e notificação ao usuário quando um roubo for detectado.

## Como foi testado

`AuthFlowIntegrationTest` cobre a rotação, a invalidação do token anterior, a detecção de reuso revogando a árvore inteira, o logout revogando a sessão, e a garantia de que o refresh token nunca aparece no corpo da resposta.
