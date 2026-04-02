# Quarkus Batch Processor

Motor de processamento batch para aplicações Quarkus, pensado para ser incorporado em outros projetos que já possuem datasource, transação e configuração de runtime definidos.

O objetivo do projeto é oferecer uma API fluente para processamento de grandes volumes de registros com leitura em streaming, execução em chunks, retry configurável e tratamento de erro controlado.

Este módulo é uma biblioteca, não uma aplicação standalone:

- não define datasource próprio
- não expõe endpoints HTTP
- não contém camada de requisição/resposta
- depende do projeto consumidor para fornecer `EntityManager`, transações e configuração de infraestrutura

## Por que este projeto existe

Processar grandes volumes com padrões ORM tradicionais costuma gerar problemas como:

- consumo excessivo de memória
- crescimento do contexto de persistência
- transações longas
- throughput instável em tabelas grandes

O `quarkus-batch-processor` foi criado para reduzir esses problemas com um modelo de execução orientado a streaming e chunks.

## Principais recursos

- baixo uso de memória por leitura progressiva
- processamento em chunks
- fronteiras transacionais configuráveis
- políticas de retry para falhas transitórias
- tratamento de erro por registro
- métricas e logs de progresso
- API fluente para definição do processamento
- foco em processamento relacional de grande volume

## Quando usar

Este projeto faz sentido para cenários como:

- processamento de milhões de registros
- migração de dados
- pipelines de ETL
- atualizações em lote
- reprocessamento de dados
- jobs agendados em background

## Quando não usar

Este projeto não foi desenhado para:

- datasets pequenos
- APIs em tempo real
- fluxos de negócio orientados a request
- lógicas transacionais curtas

Ele é otimizado para execução batch.

## Arquitetura em alto nível

O fluxo principal do motor é:

Banco de dados
↓
`ScrollableResults`
↓
Iterador
↓
Loop de processamento
↓
Formação de chunk
↓
Execução transacional
↓
Métricas e logs

Essa arquitetura evita carregar o resultado inteiro em memória.

## Modelo de processamento

Os registros são lidos sequencialmente e agrupados em chunks antes da execução.

Fluxo típico:

Ler registros
↓
Montar chunk
↓
Executar processamento
↓
Commit ou rollback conforme a estratégia
↓
Registrar métricas

O tamanho do chunk impacta diretamente o equilíbrio entre throughput, custo transacional e pressão no banco.

## Componentes principais

### `FastRecordProcessor`

Ponto de entrada principal para configuração de jobs batch.

### `FastRecordProcessorImpl`

Implementação interna responsável por coordenar a execução.

### `ProcessingLoop`

Loop central que percorre os registros e forma os chunks.

### `ProcessingMetricsLogger`

Responsável pelos logs de métricas e progresso.

### `ChunkExecutionResult`

Representa o resultado de um chunk processado.

### `ProcessingResult`

Resumo final retornado ao término da execução.

## Estrutura de pacotes

O código está organizado nos seguintes pacotes principais:

- `config` — configurações de retry, erro e transação
- `contract` — contratos públicos e pontos de extensão
- `engine` — implementação interna do motor
- `result` — objetos de resultado retornados ao consumidor
- `report` — geração de relatórios em formatos como CSV e XLSX

## Exemplo de uso

```java
import br.com.codenest.config.ErrorStrategy;
import br.com.codenest.config.TransactionMode;
import br.com.codenest.engine.FastRecordProcessor;
import br.com.codenest.result.ProcessingResult;
import jakarta.inject.Inject;

class CustomerJob {

    @Inject
    FastRecordProcessor processor;

    void run() {
        ProcessingResult result = processor
                .source(CustomerEntity.class)
                .query("select c from CustomerEntity c where c.active = true")
                .chunkSize(1000)
                .transactionMode(TransactionMode.CHUNK)
                .onError(ErrorStrategy.CONTINUE_ON_ERROR)
                .onProcess(this::recalculateScore)
                .onRecordError((customer, error) -> log.error("Erro ao processar registro", error))
                .run();
    }
}
```

O motor lê os dados em streaming, processa em chunks e devolve um resumo final da execução.

## Exemplo prático: query nativa para CSV em diretório local

Se você quiser exportar uma query nativa para CSV sem adicionar novos métodos na API da biblioteca, pode usar `nativeQuery(...)` diretamente e gravar o arquivo no seu ambiente.

```java
import br.com.codenest.contract.RowMapper;
import br.com.codenest.engine.FastRecordProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@ApplicationScoped
class CustomerNativeCsvJob {

    @Inject
    FastRecordProcessor processor;

    public Path exportActiveCustomers() throws Exception {
        Path output = Path.of("/tmp/reports/clientes-ativos.csv");
        Files.createDirectories(output.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("ID,NOME,EMAIL");
            writer.newLine();

            RowMapper<CustomerCsvRow> nativeMapper = row -> new CustomerCsvRow(
                    String.valueOf(row[0]),
                    String.valueOf(row[1]),
                    String.valueOf(row[2])
            );

            processor.source(CustomerCsvRow.class)
                    .nativeQuery("""
                        select
                            c.id,
                            c.name,
                            c.email
                        from customers c
                        where c.status = :status
                        order by c.id
                    """)
                    .params(Map.of("status", "ACTIVE"))
                    .rowMapper(nativeMapper)
                    .chunkSize(500)
                    .onProcess(row -> {
                        try {
                            writer.write(row.id() + "," + row.name() + "," + row.email());
                            writer.newLine();
                        } catch (Exception e) {
                            throw new RuntimeException("Erro ao escrever linha do CSV", e);
                        }
                    })
                    .run();
        }

        return output;
    }

    private record CustomerCsvRow(String id, String name, String email) {
    }
}
```

Nesse exemplo, o arquivo será criado em `/tmp/reports/clientes-ativos.csv`.

## Segurança de memória

O projeto foi desenhado para evitar carregamento completo de grandes datasets em memória.

Na prática, isso busca garantir:

- uso de memória mais estável
- comportamento previsível em execuções longas
- operação mais segura sobre tabelas muito grandes

## Métricas

Durante a execução, o motor pode registrar métricas como:

- total de registros processados
- total de erros
- total de retries
- tempo total de execução
- progresso por chunk

Essas informações ajudam no monitoramento operacional de jobs longos.

## Requisitos

- Java 17+
- Quarkus 3+
- Hibernate ORM

## Modelo de integração

Este módulo assume que o projeto hospedeiro já define:

- configuração de datasource do Quarkus
- gerenciador transacional / `UserTransaction`
- entidades e mapeamentos JPA
- qualquer agendamento, orquestração ou exposição HTTP em torno do batch

O foco desta biblioteca é exclusivamente melhorar a execução do processamento.

## Documentação adicional

Mais detalhes estão disponíveis no diretório [`docs`](docs):

- [Visão geral da arquitetura](docs/architecture-overview.md)
- [Organização dos pacotes](docs/package-organization.md)
- [Fluxo de processamento](docs/processing-flow.md)

## Roadmap

Possíveis evoluções futuras:

- execução paralela de chunks
- suporte reativo a banco
- entrada baseada em Kafka
- integração com observabilidade
- suporte a backpressure
- políticas de retry mais avançadas

## Licença

MIT License
