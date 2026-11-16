package io.github.jean8791.benchmark;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * One case per JVM. Arguments: {@code <scenario: read|xlsx> <impl: naive|library|library-in-tx|library-tx> <rows>}.
 * Prints one CSV line to stdout, prefixed with {@code RESULT,}.
 */
@QuarkusMain
public class BenchmarkMain implements QuarkusApplication {

    private static final long WARMUP_ROWS = 20_000;

    @Inject
    Scenarios scenarios;

    @Override
    public int run(String... args) throws Exception {
        String scenario = args[0];
        String impl = args[1];
        long rows = Long.parseLong(args[2]);

        Path directory = Files.createTempDirectory("bench-xlsx");
        String status = "OK";
        long millis = -1;
        long count = -1;
        try (HeapMonitor heap = new HeapMonitor()) {
            execute(scenario, impl, WARMUP_ROWS, directory);
            System.gc();
            long baselineMb = heap.usedMb();
            heap.reset();

            long start = System.nanoTime();
            try {
                count = execute(scenario, impl, rows, directory);
            } catch (OutOfMemoryError oom) {
                status = "OOM";
            } catch (RuntimeException failure) {
                status = isOutOfMemory(failure) ? "OOM" : "ERROR";
            }
            millis = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("RESULT,%s,%s,%d,%s,%d,%d,%d,%d,%d%n", scenario, impl, rows, status, millis,
                    heap.peakRetainedMb(), heap.peakUsedMb(), count,
                    baselineMb);
        } finally {
            deleteQuietly(directory);
        }
        return 0;
    }

    private long execute(String scenario, String impl, long rows, Path directory) throws Exception {
        if (scenario.equals("read")) {
            return switch (impl) {
                case "naive" -> scenarios.readNaive(rows);
                case "library" -> scenarios.readLibrary(rows);
                case "library-in-tx" -> scenarios.readLibraryInsideTx(rows);
                case "library-tx" -> scenarios.readLibraryChunkTx(rows);
                default -> throw new IllegalArgumentException("unknown impl " + impl);
            };
        }
        return switch (impl) {
            case "naive" -> scenarios.xlsxNaive(rows, directory);
            case "library" -> scenarios.xlsxLibrary(rows, directory);
            case "library-in-tx" -> scenarios.xlsxLibraryInsideTx(rows, directory);
            default -> throw new IllegalArgumentException("unknown impl " + impl);
        };
    }

    private boolean isOutOfMemory(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof OutOfMemoryError) {
                return true;
            }
        }
        return false;
    }

    private void deleteQuietly(Path directory) {
        try (Stream<Path> files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (Exception ignored) {
            // temp files only
        }
    }
}
