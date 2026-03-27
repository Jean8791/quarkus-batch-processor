# Quarkus Batch Processor

High-performance batch processing engine for Quarkus applications, designed to be embedded into other projects that already own their datasource, transaction, and runtime configuration.

This project provides a fluent API for building batch workloads using streaming database reads, chunk-based processing, retry strategies, and configurable error handling.

It is a library module, not a standalone application:

- no datasource configuration is bundled here
- no HTTP endpoints are exposed here
- no request/response layer is part of this project
- the consuming application provides `EntityManager`, transactions, and infrastructure settings

## Why this project exists

Processing large volumes of data with traditional ORM patterns often leads to problems such as:

- excessive memory consumption
- persistence context growth
- long-running transactions
- unstable throughput on large tables

Quarkus Batch Processor addresses these issues with a streaming-oriented execution model built for batch workloads.

## Main features

- Low memory footprint through streaming reads
- Chunk-based processing
- Configurable transaction boundaries
- Retry strategies for transient failures
- Record-level error handling
- Execution metrics and progress logging
- Fluent API for defining processing pipelines
- Designed for large-scale relational data processing

## When to use it

This library is a good fit for workloads such as:

- processing millions of database records
- data migration jobs
- ETL pipelines
- batch updates
- reprocessing tasks
- scheduled background jobs

## When not to use it

This project is not intended for:

- small datasets
- real-time APIs
- request-driven business flows
- short transactional application logic

It is optimized specifically for batch execution scenarios.

## High-level architecture

The engine follows a streaming pipeline:

Database 
↓ 
Scrollable results 
↓ 
Iterator 
↓ 
Processing loop 
↓ 
Chunk formation 
↓ 
Transaction execution 
↓ 
Metrics logging


This architecture prevents large result sets from being loaded fully into memory.

## Processing model

The processing flow is based on reading records sequentially and grouping them into chunks for execution.

Typical flow:

Fetch records 
↓ 
Build chunk 
↓ 
Execute processing 
↓ 
Commit or rollback according to transaction strategy 
↓ 
Log metrics


Chunk size directly affects the balance between throughput, transaction cost, and database pressure.

## Core components

### FastRecordProcessor
Main entry point for configuring batch jobs.

### FastRecordProcessorImpl
Internal engine implementation responsible for coordinating execution.

### ProcessingLoop
Execution loop that iterates over streamed records and builds chunks.

### ProcessingMetricsLogger
Responsible for logging execution metrics and progress information.

### ChunkExecutionResult
Represents the result of an individual processed chunk.

### ProcessingResult
Final execution summary returned after processing completes.

## Package structure

The codebase is organized into the following packages:

- `config` — configuration objects such as retry, error, and transaction settings
- `contract` — public contracts and callback interfaces
- `engine` — core execution engine and processing pipeline
- `result` — result objects returned after execution

## Example usage

Example of configuring a batch job:

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
                .onRecordError((customer, error) -> log.error("Error processing record", error))
                .run();
    }
}
```


The engine streams the records, processes them in chunks, and returns a final execution summary.

> Important:
> Keep this example aligned with the actual public API of the project. If method names or enum values change, update this section first.

## Memory safety

The engine is designed to avoid loading large datasets into memory all at once.

It relies on streaming access patterns and controlled processing cycles to provide:

- stable memory usage
- predictable execution behavior
- safer handling of very large tables

## Metrics

During execution, the engine can log metrics such as:

- total processed records
- total errors
- retry count
- total execution time
- chunk progress

These metrics help monitor throughput and operational behavior during long-running jobs.

## Requirements

- Java 17+
- Quarkus 3+
- Hibernate ORM

## Integration Model

This module assumes the host project already defines:

- Quarkus datasource configuration
- transaction manager / `UserTransaction`
- JPA entities and mappings
- any scheduling, triggering, or HTTP exposure around batch execution

The processor itself only focuses on efficient record iteration and chunk execution.

## Documentation

Additional documentation is available in the [`docs`](docs) directory:

- [Architecture overview](docs/architecture-overview.md)
- [Package organization](docs/package-organization.md)
- [Processing flow](docs/processing-flow.md)

## Roadmap

Potential future improvements include:

- parallel chunk execution
- reactive database support
- Kafka-based input sources
- observability integration
- backpressure support
- more advanced retry policies

## License

MIT License
