# Changelog

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
