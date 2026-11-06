package io.github.jean8791.batch.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileTargetTest {

    @TempDir
    Path tempDir;

    @Test
    void writesInsideBaseDirectoryCreatingSubfolders() throws Exception {
        LocalFileTarget target = new LocalFileTarget(tempDir.resolve("reports").toString());

        try (OutputStream out = target.open("2026/march.csv")) {
            out.write('x');
        }

        Path file = tempDir.resolve("reports/2026/march.csv");
        assertTrue(Files.exists(file));
        assertEquals(file.toString(), target.getLastOpenedPath());
    }

    @Test
    void rejectsNamesThatLeaveBaseDirectory() {
        LocalFileTarget target = new LocalFileTarget(tempDir.resolve("reports").toString());

        assertThrows(IllegalArgumentException.class, () -> target.open("../escaped.csv"));
        assertThrows(IllegalArgumentException.class, () -> target.open(tempDir.resolve("abs.csv").toString()));
        assertThrows(IllegalArgumentException.class, () -> target.open("."));
    }
}
