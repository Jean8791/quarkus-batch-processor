# Organização dos Pacotes

Este documento explica como o projeto está organizado em nível de pacote e qual responsabilidade pertence a cada parte do código.

O objetivo dessa estrutura é manter a API pública enxuta, isolar a implementação interna e facilitar a evolução do projeto ao longo do tempo.

## Visão geral

O código está organizado em alguns grupos principais:

- `config`
- `contract`
- `engine`
- `result`
- `report`

Cada pacote tem um papel claro dentro da arquitetura.

## `config`

Este pacote contém tipos de configuração que definem como o processamento se comporta.

Responsabilidades típicas:

- configuração de comportamento transacional
- definição de estratégia de erro
- definição de política de retry

Exemplos de conceitos que pertencem aqui:

- modo transacional
- estratégia de erro
- estratégia de retry

### O que deve ficar aqui

Tipos que descrevem opções de execução e políticas de processamento.

### O que não deve ficar aqui

- lógica de execução
- acesso a banco
- orquestração
- objetos de resultado

## `contract`

Este pacote contém os contratos públicos usados pelos consumidores da biblioteca e pela engine por meio de abstrações estáveis.

Responsabilidades típicas:

- interfaces de processamento
- contratos de callback
- pontos de extensão
- contratos de mapeamento de resultado

Exemplos:

- contrato do processor
- listener de chunk
- handler de erro por registro
- `rowMapper`
- abstração de transação

### O que deve ficar aqui

Interfaces e callbacks que definem como o código externo interage com a engine.

### O que não deve ficar aqui

- implementações concretas da engine
- lógica interna de orquestração
- agregação interna de métricas
- detalhes package-private de processamento

## `engine`

Este pacote contém a implementação interna do motor batch.

Ele é o núcleo do projeto e coordena a execução.

Responsabilidades típicas:

- implementação do processor
- iteração em streaming
- formação e execução de chunks
- coordenação transacional
- propagação interna de erros
- logging de progresso

Exemplos:

- implementação do ponto de entrada
- loop de processamento
- logger de métricas
- resultado de execução de chunk
- exceções específicas da engine

### O que deve ficar aqui

Classes concretas que implementam o workflow e a mecânica da execução.

### O que não deve ficar aqui

- exemplos de documentação
- enums genéricos de configuração pública
- contratos exclusivamente públicos
- DTOs finais voltados ao consumidor

## `result`

Este pacote contém os objetos retornados ao final do processamento.

Seu papel é expor estruturas de saída estáveis sem vazar detalhes internos da engine.

Responsabilidades típicas:

- resumo final da execução
- contadores agregados
- informações de tempo de execução

### O que deve ficar aqui

Objetos de resultado consumidos pelo código da aplicação após o job terminar.

### O que não deve ficar aqui

- estado intermediário da engine
- detalhes do loop de processamento
- implementação transacional
- infraestrutura de logging

## `report`

Este pacote contém a infraestrutura de geração de relatórios apoiada pela biblioteca.

Seu objetivo é permitir a produção de arquivos como CSV e XLSX sem misturar essa lógica ao núcleo do processamento batch.

Responsabilidades típicas:

- contratos de escrita de relatório
- formatos suportados
- definição de colunas e layout
- geração de arquivos
- integração do processor com exportação

### O que deve ficar aqui

Tipos relacionados à geração, escrita e saída de relatórios.

### O que não deve ficar aqui

- lógica central da engine batch
- configuração transacional do processor
- DTOs gerais de resultado do motor

## API pública versus implementação interna

Uma forma útil de pensar a estrutura é esta:

### Pacotes voltados ao consumidor

Os pacotes mais relevantes para quem usa a biblioteca são:

- `config`
- `contract`
- `result`
- `report`

Eles definem como o consumidor configura, executa e consome o processamento.

### Pacote de implementação interna

O pacote que contém os detalhes internos é:

- `engine`

Essa separação reduz acoplamento acidental entre o consumidor e a implementação.

## Direção das dependências

O fluxo conceitual de dependência é:

Código da aplicação
↓
`contract` / `config`
↓
`engine`
↓
`result`

Na prática:

- a aplicação configura o processamento por meio dos contratos públicos
- a engine executa o workflow
- o resultado final é devolvido como objeto estável

O código externo deve evitar depender diretamente dos detalhes internos da `engine`, exceto quando isso fizer parte explícita do desenho da biblioteca.

## Benefícios dessa organização

Essa estrutura traz algumas vantagens:

- separação mais clara de responsabilidades
- onboarding mais simples para novos contribuidores
- evolução mais segura da implementação interna
- menor acoplamento entre API e engine
- melhor manutenção à medida que o projeto cresce

## Diretrizes de manutenção

Ao adicionar código novo, siga estas regras:

- coloque políticas e comportamentos em `config`
- coloque abstrações públicas e pontos de extensão em `contract`
- coloque processamento interno e orquestração em `engine`
- coloque saídas finais consumidas externamente em `result`
- coloque geração de relatório em `report`

Se uma classe estiver fazendo mais de um desses papéis, ela provavelmente precisa ser dividida.

## Documentação relacionada

Para mais contexto, veja:

- [Visão geral da arquitetura](architecture-overview.md)
- [Fluxo de processamento](processing-flow.md)
