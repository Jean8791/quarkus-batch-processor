# Changelog

## [Unreleased]

### Added

- Reproducible benchmark in `benchmark/` (separate Maven project, not part of the published artifact): naive `getResultList()` versus the library, for reads and XLSX export, on real PostgreSQL. Run it with `./benchmark/run.sh`; results are in `benchmark/results/` and summarised in the README.

### Fixed

- On PostgreSQL, the default usage (`run()` outside a transaction) loaded the whole result set into memory, because the connection was in autocommit and the driver ignores the fetch size in that mode. The processor now turns autocommit off on the cursor's own connection while it reads and restores it afterwards; a connection that already belongs to the caller's transaction is left untouched. In the benchmark, with 1 million rows and `-Xmx256m`, reads went from `OutOfMemoryError` to about 12 MB retained, and the XLSX export to about 20 MB. Read the [Benchmark](README.md#benchmark) for the method and the limits.

## [0.1.0] - 2026-11-06

First public release.

### Added

- Streaming batch processing with `FastRecordProcessor`: HQL or native queries, chunks, retries, error strategies and progress logs.
- `TransactionMode.CHUNK`, with one JTA transaction per chunk.
- CSV and XLSX export through `ProcessorBackedReportService`.
- Jandex index, so the processor is discovered as a CDI bean without extra configuration.
- Sources and Javadoc jars, published on JitPack.

### Fixed

- `TransactionMode.CHUNK` no longer commits a transaction opened by the caller; `run()` now fails fast when called inside one.
- A failed rollback no longer hides the original error; it is attached as a suppressed exception.
- The database cursor is closed when a run fails halfway.
- `LocalFileTarget` rejects file names that leave its base folder.
- XLSX export writes `BigDecimal` and other numbers as numeric cells.

[0.1.0]: https://github.com/Jean8791/quarkus-batch-processor/releases/tag/v0.1.0
