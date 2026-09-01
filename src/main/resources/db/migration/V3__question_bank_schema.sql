-- KODA - Fase 3: banco de questoes (tipos extensiveis, correcao, dedup).
-- Referencias: docs/architecture/02-modelo-de-dados.md (Banco de questoes),
-- docs/architecture/03-estrategia-ia.md (dedup, avaliacao). Esta migration e
-- imutavel apos aplicada; correcoes vem em uma nova versao.
--
-- payload/correct_answer/distractor_rationales sao coluna `text` com JSON
-- serializado, nao `jsonb` com mapeamento Hibernate nativo — evita depender
-- de como Hibernate 7 mapeia JsonNode do Jackson 3, combinacao nova demais
-- pra apostar sem verificar. O parsing acontece explicitamente em Java.

-- ---------------------------------------------------------------------------
-- questions / question_versions: versoes imutaveis apos publicadas. Uma
-- tentativa referencia a versao respondida, nunca a questao (DAT-02).
-- ---------------------------------------------------------------------------
CREATE TABLE questions (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    slug          text        NOT NULL,
    topic_id      uuid        NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    question_type text        NOT NULL,
    status        text        NOT NULL DEFAULT 'PUBLISHED',
    created_at    timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT questions_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT questions_status_check CHECK (status IN ('DRAFT', 'IN_REVIEW', 'PUBLISHED', 'DISABLED', 'RETIRED'))
);

CREATE UNIQUE INDEX questions_slug_key ON questions (slug);
CREATE INDEX questions_published_type_idx ON questions (question_type) WHERE status = 'PUBLISHED';

CREATE TABLE question_versions (
    id                      uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id             uuid        NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    version                 integer     NOT NULL,
    payload                 text        NOT NULL,
    correct_answer          text        NOT NULL,
    explanation             text        NOT NULL,
    distractor_rationales   text,
    declared_difficulty     smallint    NOT NULL,
    measured_difficulty     numeric(4,3),
    estimated_time_seconds  integer,
    canonical_hash          bytea       NOT NULL,
    quality_score           numeric(4,3),
    language                text        NOT NULL DEFAULT 'pt-BR',
    created_at              timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT question_versions_difficulty_check CHECK (declared_difficulty BETWEEN 1 AND 5)
);

CREATE UNIQUE INDEX question_versions_question_version_key ON question_versions (question_id, version);

-- ---------------------------------------------------------------------------
-- question_concepts: qual concept uma versao testa.
-- ---------------------------------------------------------------------------
CREATE TABLE question_concepts (
    id                   uuid    PRIMARY KEY DEFAULT gen_random_uuid(),
    question_version_id  uuid    NOT NULL REFERENCES question_versions (id) ON DELETE CASCADE,
    concept_id           uuid    NOT NULL REFERENCES concepts (id) ON DELETE CASCADE,
    weight               numeric NOT NULL DEFAULT 1.0
);

CREATE UNIQUE INDEX question_concepts_version_concept_key ON question_concepts (question_version_id, concept_id);

-- ---------------------------------------------------------------------------
-- question_attempts: APPEND-ONLY, fonte da verdade de todo o modelo de
-- conhecimento. Sem UPDATE/DELETE no caminho da aplicacao.
-- ---------------------------------------------------------------------------
CREATE TABLE question_attempts (
    id                   uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    question_version_id  uuid        NOT NULL REFERENCES question_versions (id) ON DELETE CASCADE,
    submitted_answer     text        NOT NULL,
    is_correct           boolean     NOT NULL,
    error_type           text,
    response_time_ms     integer     NOT NULL,
    hints_used           smallint    NOT NULL DEFAULT 0,
    difficulty_at_time   smallint    NOT NULL,
    created_at           timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT question_attempts_response_time_check CHECK (response_time_ms >= 0)
);

CREATE INDEX question_attempts_user_idx ON question_attempts (user_id, created_at DESC);
CREATE INDEX question_attempts_version_idx ON question_attempts (question_version_id);

-- ---------------------------------------------------------------------------
-- user_question_exposure: dedup camada 4. A sessao de pratica prioriza o que
-- o usuario viu menos.
-- ---------------------------------------------------------------------------
CREATE TABLE user_question_exposure (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    question_id   uuid        NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    times_seen    integer     NOT NULL DEFAULT 1,
    last_seen_at  timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX user_question_exposure_user_question_key ON user_question_exposure (user_id, question_id);

-- ---------------------------------------------------------------------------
-- Seed: 30 questoes curadas a mao (3 por concept, 10 concepts da Fase 2),
-- variando entre os 5 tipos. Todas nascem PUBLISHED — nao ha fluxo de revisao
-- humana ainda.
-- ---------------------------------------------------------------------------
INSERT INTO questions (slug, topic_id, question_type)
SELECT q.slug, t.id, q.question_type
FROM topics t
JOIN (VALUES
    ('tipos-primitivos-1',   'fundamentos-da-linguagem', 'single_choice'),
    ('tipos-primitivos-2',   'fundamentos-da-linguagem', 'true_false'),
    ('tipos-primitivos-3',   'fundamentos-da-linguagem', 'fill_in_blank'),
    ('condicionais-1',       'fundamentos-da-linguagem', 'single_choice'),
    ('condicionais-2',       'fundamentos-da-linguagem', 'true_false'),
    ('condicionais-3',       'fundamentos-da-linguagem', 'fill_in_blank'),
    ('lacos-1',              'fundamentos-da-linguagem', 'single_choice'),
    ('lacos-2',              'fundamentos-da-linguagem', 'true_false'),
    ('lacos-3',              'fundamentos-da-linguagem', 'ordering'),
    ('funcoes-1',            'fundamentos-da-linguagem', 'single_choice'),
    ('funcoes-2',            'fundamentos-da-linguagem', 'true_false'),
    ('funcoes-3',            'fundamentos-da-linguagem', 'multiple_select'),
    ('escopo-1',             'fundamentos-da-linguagem', 'single_choice'),
    ('escopo-2',             'fundamentos-da-linguagem', 'true_false'),
    ('escopo-3',             'fundamentos-da-linguagem', 'fill_in_blank'),
    ('recursao-1',           'fundamentos-da-linguagem', 'single_choice'),
    ('recursao-2',           'fundamentos-da-linguagem', 'true_false'),
    ('recursao-3',           'fundamentos-da-linguagem', 'ordering'),
    ('arrays-1',             'estruturas-de-dados',      'single_choice'),
    ('arrays-2',             'estruturas-de-dados',      'true_false'),
    ('arrays-3',             'estruturas-de-dados',      'multiple_select'),
    ('listas-ligadas-1',     'estruturas-de-dados',      'single_choice'),
    ('listas-ligadas-2',     'estruturas-de-dados',      'true_false'),
    ('listas-ligadas-3',     'estruturas-de-dados',      'fill_in_blank'),
    ('mapas-1',              'estruturas-de-dados',      'single_choice'),
    ('mapas-2',              'estruturas-de-dados',      'true_false'),
    ('mapas-3',              'estruturas-de-dados',      'fill_in_blank'),
    ('complexidade-1',       'estruturas-de-dados',      'single_choice'),
    ('complexidade-2',       'estruturas-de-dados',      'true_false'),
    ('complexidade-3',       'estruturas-de-dados',      'ordering')
) AS q(slug, topic_slug, question_type) ON t.slug = q.topic_slug;

INSERT INTO question_versions
    (question_id, version, payload, correct_answer, explanation, distractor_rationales, declared_difficulty, estimated_time_seconds, canonical_hash, language)
SELECT q.id, 1, v.payload, v.correct_answer, v.explanation, v.distractor_rationales, v.declared_difficulty, v.estimated_time_seconds,
       digest(v.payload, 'sha256'), 'pt-BR'
FROM questions q
JOIN (VALUES

    ('tipos-primitivos-1',
     '{"prompt":"Qual e o tipo do valor 3.14?","options":[{"id":"a","label":"inteiro"},{"id":"b","label":"decimal"},{"id":"c","label":"texto"},{"id":"d","label":"booleano"}]}',
     '{"optionId":"b"}',
     'Tipos primitivos: 3.14 tem casas decimais, entao e um numero de ponto flutuante — o tipo decimal.',
     '{"a":"Inteiro nao tem casas decimais.","c":"Texto vem entre aspas, nao e o caso aqui.","d":"Booleano so tem os valores verdadeiro ou falso."}',
     1, 20),

    ('tipos-primitivos-2',
     '{"prompt":"O valor \"10\" (entre aspas) e do tipo numerico."}',
     '{"value":false}',
     'Qualquer valor entre aspas e texto, mesmo que parece um numero. Para virar numero, precisa de conversao explicita.',
     NULL, 1, 15),

    ('tipos-primitivos-3',
     '{"prompt":"Complete: o tipo do valor true e ______."}',
     '{"text":"booleano","acceptable":["boolean"]}',
     'true e false sao os dois unicos valores do tipo booleano.',
     NULL, 1, 15),

    ('condicionais-1',
     '{"prompt":"O que este trecho imprime?","code":"let idade = 16;\n\nif (idade >= 18) {\n  console.log(\"maior\");\n} else {\n  console.log(\"menor\");\n}","options":[{"id":"a","label":"maior"},{"id":"b","label":"menor"},{"id":"c","label":"Nao imprime nada"},{"id":"d","label":"Erro"}]}',
     '{"optionId":"b"}',
     '16 nao e maior ou igual a 18, entao o fluxo cai no else e imprime "menor".',
     '{"a":"So aconteceria se idade fosse maior ou igual a 18.","c":"Um dos dois ramos sempre executa.","d":"O codigo e valido, nao ha erro."}',
     1, 30),

    ('condicionais-2',
     '{"prompt":"O bloco else e obrigatorio em toda estrutura if."}',
     '{"value":false}',
     'O else e opcional. Sem ele, se a condicao for falsa, o bloco if simplesmente nao executa e o programa segue adiante.',
     NULL, 1, 15),

    ('condicionais-3',
     '{"prompt":"Complete: a palavra-chave que testa uma condicao alternativa a um if anterior, sem ser o ultimo caso, e ______ (em ingles)."}',
     '{"text":"else if","acceptable":["elseif","else-if"]}',
     'else if encadeia uma nova condicao quando o if anterior for falso.',
     NULL, 2, 20),

    ('lacos-1',
     '{"prompt":"Quantas vezes este laco executa?","code":"for (let i = 0; i <= 4; i++) {\n  console.log(i);\n}","options":[{"id":"a","label":"4"},{"id":"b","label":"5"},{"id":"c","label":"0"},{"id":"d","label":"Infinitas"}]}',
     '{"optionId":"b"}',
     'De 0 ate 4 incluindo as duas pontas sao 5 valores: 0, 1, 2, 3, 4.',
     '{"a":"Esqueceu de contar o 0 ou o 4.","c":"O laco tem uma condicao que permite pelo menos uma execucao.","d":"i comeca em 0 e cresce ate passar de 4, entao ele termina."}',
     1, 25),

    ('lacos-2',
     '{"prompt":"Um laco while pode nunca executar o bloco de dentro, se a condicao ja comecar falsa."}',
     '{"value":true}',
     'O while testa a condicao antes de cada execucao, incluindo a primeira. Se ja comecar falsa, o bloco nunca roda.',
     NULL, 2, 15),

    ('lacos-3',
     '{"prompt":"Ordene os passos de execucao deste laco, do primeiro ao ultimo: for (let i = 0; i < 3; i++) { console.log(i); }","items":[{"id":"1","label":"Inicializa i com 0"},{"id":"2","label":"Testa se i e menor que 3"},{"id":"3","label":"Executa o corpo do laco"},{"id":"4","label":"Incrementa i"}]}',
     '{"order":["1","2","3","4"]}',
     'O for inicializa uma vez, depois repete: testa a condicao, executa o corpo, incrementa — nessa ordem, ate a condicao falhar.',
     NULL, 2, 30),

    ('funcoes-1',
     '{"prompt":"O que esta funcao retorna para dobro(5)?","code":"function dobro(x) {\n  return x * 2;\n}\n\ndobro(5);","options":[{"id":"a","label":"5"},{"id":"b","label":"10"},{"id":"c","label":"25"},{"id":"d","label":"Nada"}]}',
     '{"optionId":"b"}',
     'A funcao multiplica o parametro por 2, entao dobro(5) retorna 10.',
     '{"a":"Isso seria retornar x sem multiplicar.","c":"Isso seria 5 ao quadrado, nao o dobro.","d":"A funcao tem um return explicito."}',
     1, 20),

    ('funcoes-2',
     '{"prompt":"Uma funcao sem a instrucao return explicita ainda assim retorna um valor: undefined."}',
     '{"value":true}',
     'Se a execucao chega ao fim da funcao sem um return, o valor retornado e undefined automaticamente.',
     NULL, 2, 15),

    ('funcoes-3',
     '{"prompt":"Quais das opcoes abaixo sao formas validas de passar dados para dentro de uma funcao?","options":[{"id":"a","label":"Um parametro na definicao da funcao"},{"id":"b","label":"Um argumento na chamada da funcao"},{"id":"c","label":"Escrever o valor direto no corpo da funcao, fora de qualquer parametro"},{"id":"d","label":"Chamar a funcao duas vezes seguidas"}]}',
     '{"optionIds":["a","b"]}',
     'Parametros (na definicao) e argumentos (na chamada) sao as duas metades do mesmo mecanismo de passar dados. As outras opcoes nao sao formas de passar dado nenhum.',
     NULL, 2, 30),

    ('escopo-1',
     '{"prompt":"Depois de rodar este codigo, qual o valor de x fora da funcao?","code":"let x = 1;\n\nfunction muda() {\n  let x = 2;\n}\n\nmuda();","options":[{"id":"a","label":"1"},{"id":"b","label":"2"},{"id":"c","label":"0"},{"id":"d","label":"Erro"}]}',
     '{"optionId":"a"}',
     'O x dentro de muda() e uma variavel local nova — ela nao altera o x de fora. Do lado de fora, x continua 1.',
     '{"b":"Isso so aconteceria se a funcao alterasse o x externo, nao criasse um novo.","c":"Nada no codigo zera x.","d":"O codigo e valido, nao ha erro."}',
     2, 25),

    ('escopo-2',
     '{"prompt":"Uma variavel declarada dentro de um bloco if com let so existe dentro daquele bloco."}',
     '{"value":true}',
     'let (e const) tem escopo de bloco: a variavel so vive entre as chaves onde foi declarada.',
     NULL, 2, 15),

    ('escopo-3',
     '{"prompt":"Complete: uma variavel que pode ser acessada de qualquer parte do codigo, dentro ou fora de funcoes, tem escopo ______."}',
     '{"text":"global","acceptable":[]}',
     'Escopo global significa visivel em todo o programa, ao contrario do escopo local, restrito a uma funcao ou bloco.',
     NULL, 1, 15),

    ('recursao-1',
     '{"prompt":"O que esta funcao recursiva calcula para fatorial(3)?","code":"function fatorial(n) {\n  if (n <= 1) return 1;\n  return n * fatorial(n - 1);\n}\n\nfatorial(3);","options":[{"id":"a","label":"3"},{"id":"b","label":"6"},{"id":"c","label":"9"},{"id":"d","label":"1"}]}',
     '{"optionId":"b"}',
     'fatorial(3) = 3 vezes fatorial(2) = 3 vezes 2 vezes fatorial(1) = 3 vezes 2 vezes 1 = 6.',
     '{"a":"Isso seria so o valor de n, sem multiplicar pelos anteriores.","c":"A recursao multiplica por numeros decrescentes, nao pelo mesmo n tres vezes.","d":"Esse e o caso base, nao o resultado final."}',
     3, 40),

    ('recursao-2',
     '{"prompt":"Uma funcao recursiva sem um caso base nunca para de chamar a si mesma."}',
     '{"value":true}',
     'O caso base e a condicao que interrompe a recursao. Sem ele, as chamadas continuam ate estourar a pilha de chamadas.',
     NULL, 2, 15),

    ('recursao-3',
     '{"prompt":"Ordene as chamadas de fatorial(3) pela ordem em que elas terminam (retornam um valor), da primeira a ultima:","items":[{"id":"1","label":"fatorial(1) retorna"},{"id":"2","label":"fatorial(2) retorna"},{"id":"3","label":"fatorial(3) retorna"}]}',
     '{"order":["1","2","3"]}',
     'A chamada mais profunda (fatorial(1), o caso base) e a primeira a retornar. Cada chamada espera a de baixo terminar antes de calcular o proprio resultado, entao elas terminam de dentro pra fora.',
     NULL, 3, 35),

    ('arrays-1',
     '{"prompt":"Qual o valor de numeros[1] neste array?","code":"const numeros = [10, 20, 30];","options":[{"id":"a","label":"10"},{"id":"b","label":"20"},{"id":"c","label":"30"},{"id":"d","label":"undefined"}]}',
     '{"optionId":"b"}',
     'Arrays comecam a contar do indice 0. numeros[0] e 10, numeros[1] e 20.',
     '{"a":"Esse seria numeros[0].","c":"Esse seria numeros[2].","d":"O indice 1 existe nesse array de 3 posicoes."}',
     1, 20),

    ('arrays-2',
     '{"prompt":"O primeiro elemento de um array esta sempre no indice 1."}',
     '{"value":false}',
     'O primeiro elemento esta no indice 0. Contar a partir de 1 e um erro comum de quem vem de outras convencoes.',
     NULL, 1, 15),

    ('arrays-3',
     '{"prompt":"Quais das afirmacoes sobre arrays estao corretas?","options":[{"id":"a","label":"Um array pode crescer dinamicamente conforme itens sao adicionados"},{"id":"b","label":"Todo array precisa ter um tamanho fixo definido na criacao"},{"id":"c","label":"Elementos de um array sao acessados por indice numerico"},{"id":"d","label":"Arrays so podem guardar numeros"}]}',
     '{"optionIds":["a","c"]}',
     'Arrays em linguagens como JavaScript crescem dinamicamente e sao acessados por indice. Eles podem guardar qualquer tipo de valor, nao so numeros.',
     NULL, 2, 30),

    ('listas-ligadas-1',
     '{"prompt":"Numa lista ligada, cada elemento (no) guarda, alem do proprio valor, o que mais?","options":[{"id":"a","label":"O indice numerico da posicao"},{"id":"b","label":"Uma referencia para o proximo no"},{"id":"c","label":"Uma copia de todos os outros nos"},{"id":"d","label":"Nada alem do valor"}]}',
     '{"optionId":"b"}',
     'O que define uma lista ligada e cada no apontar para o proximo — nao ha indice numerico como em um array.',
     '{"a":"Isso e caracteristica de array, nao de lista ligada.","c":"Guardar todos os outros nos desperdicaria memoria sem necessidade.","d":"Sem a referencia ao proximo, os nos ficariam desconectados."}',
     2, 25),

    ('listas-ligadas-2',
     '{"prompt":"Acessar o elemento do meio de uma lista ligada e tao rapido quanto acessar o elemento do meio de um array."}',
     '{"value":false}',
     'Array acessa qualquer posicao diretamente pelo indice. Lista ligada precisa percorrer no a no a partir do inicio ate chegar la — e mais lento.',
     NULL, 2, 20),

    ('listas-ligadas-3',
     '{"prompt":"Complete: o ultimo no de uma lista ligada geralmente aponta para ______, indicando o fim da lista."}',
     '{"text":"nulo","acceptable":["null","nada","none"]}',
     'Uma referencia nula no ultimo no e o sinal convencional de que a lista terminou ali.',
     NULL, 2, 20),

    ('mapas-1',
     '{"prompt":"Num mapa (dicionario) que guarda idades por nome, como se busca a idade de Ana?","code":"const idades = { Ana: 30, Bruno: 25 };","options":[{"id":"a","label":"idades[0]"},{"id":"b","label":"idades.get(1)"},{"id":"c","label":"idades[\"Ana\"]"},{"id":"d","label":"idades.primeiro()"}]}',
     '{"optionId":"c"}',
     'Mapas sao acessados pela chave, nao por posicao numerica. idades["Ana"] busca o valor associado a chave "Ana".',
     '{"a":"Indice numerico e conceito de array, nao de mapa.","b":"Nao existe posicao numerica 1 num mapa por chave de texto.","d":"Esse metodo nao existe; mapas nao tem primeiro elemento garantido."}',
     2, 25),

    ('mapas-2',
     '{"prompt":"Num mapa, cada chave so pode apontar para um unico valor por vez."}',
     '{"value":true}',
     'Se voce atribuir um novo valor a uma chave que ja existe, o valor antigo e substituido — a chave continua apontando pra um valor so.',
     NULL, 2, 15),

    ('mapas-3',
     '{"prompt":"Complete: a estrutura que guarda pares de chave e valor, e permite buscar um valor diretamente pela chave, se chama ______."}',
     '{"text":"mapa","acceptable":["dicionario","map","hash map","hashmap"]}',
     'Mapa (tambem chamado dicionario ou hash map) e a estrutura de dados feita pra associar chaves a valores.',
     NULL, 1, 15),

    ('complexidade-1',
     '{"prompt":"Qual a complexidade de tempo de buscar um elemento pelo indice num array?","options":[{"id":"a","label":"O(1)"},{"id":"b","label":"O(n)"},{"id":"c","label":"O(n^2)"},{"id":"d","label":"O(log n)"}]}',
     '{"optionId":"a"}',
     'Acessar por indice e direto, o tempo nao depende do tamanho do array — por isso e O(1), tempo constante.',
     '{"b":"O(n) seria percorrer o array procurando um valor, nao acessar por indice.","c":"O(n^2) apareceria em lacos aninhados, nao num acesso direto.","d":"O(log n) e tipico de busca binaria em dados ordenados, nao de acesso por indice."}',
     3, 30),

    ('complexidade-2',
     '{"prompt":"Um algoritmo O(n) fica mais lento, em proporcao direta, conforme o tamanho da entrada cresce."}',
     '{"value":true}',
     'O(n) significa que o tempo cresce linearmente com o tamanho da entrada — dobrar a entrada dobra, aproximadamente, o tempo.',
     NULL, 2, 20),

    ('complexidade-3',
     '{"prompt":"Ordene estas complexidades da mais rapida (menor crescimento) para a mais lenta (maior crescimento):","items":[{"id":"1","label":"O(1)"},{"id":"2","label":"O(log n)"},{"id":"3","label":"O(n)"},{"id":"4","label":"O(n^2)"}]}',
     '{"order":["1","2","3","4"]}',
     'Constante e sempre a mais rapida, depois logaritmica, depois linear, depois quadratica — essa e a ordem de crescimento do tempo conforme a entrada aumenta.',
     NULL, 3, 35)

) AS v(slug, payload, correct_answer, explanation, distractor_rationales, declared_difficulty, estimated_time_seconds)
    ON q.slug = v.slug;

INSERT INTO question_concepts (question_version_id, concept_id)
SELECT qv.id, c.id
FROM question_versions qv
JOIN questions q ON q.id = qv.question_id
JOIN (VALUES
    ('tipos-primitivos-1', 'tipos-primitivos'), ('tipos-primitivos-2', 'tipos-primitivos'), ('tipos-primitivos-3', 'tipos-primitivos'),
    ('condicionais-1', 'condicionais'), ('condicionais-2', 'condicionais'), ('condicionais-3', 'condicionais'),
    ('lacos-1', 'lacos'), ('lacos-2', 'lacos'), ('lacos-3', 'lacos'),
    ('funcoes-1', 'funcoes'), ('funcoes-2', 'funcoes'), ('funcoes-3', 'funcoes'),
    ('escopo-1', 'escopo'), ('escopo-2', 'escopo'), ('escopo-3', 'escopo'),
    ('recursao-1', 'recursao'), ('recursao-2', 'recursao'), ('recursao-3', 'recursao'),
    ('arrays-1', 'arrays'), ('arrays-2', 'arrays'), ('arrays-3', 'arrays'),
    ('listas-ligadas-1', 'listas-ligadas'), ('listas-ligadas-2', 'listas-ligadas'), ('listas-ligadas-3', 'listas-ligadas'),
    ('mapas-1', 'mapas'), ('mapas-2', 'mapas'), ('mapas-3', 'mapas'),
    ('complexidade-1', 'complexidade'), ('complexidade-2', 'complexidade'), ('complexidade-3', 'complexidade')
) AS mapping(question_slug, concept_slug) ON mapping.question_slug = q.slug
JOIN concepts c ON c.slug = mapping.concept_slug;
