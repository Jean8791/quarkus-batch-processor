package br.com.codenest.report.targets;

import br.com.codenest.report.contract.ReportOutputTarget;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalFileTarget implements ReportOutputTarget {

    private final Path baseDirectory;
    private Path lastOpenedPath;

    public LocalFileTarget(String baseDirectory) {
        this.baseDirectory = Path.of(baseDirectory);
    }

    @Override
    public OutputStream open(String fileName) throws IOException {
        Files.createDirectories(baseDirectory);
        lastOpenedPath = baseDirectory.resolve(fileName);
        return Files.newOutputStream(lastOpenedPath);
    }

    public String getLastOpenedPath() {
        return lastOpenedPath != null ? lastOpenedPath.toString() : null;
    }
}