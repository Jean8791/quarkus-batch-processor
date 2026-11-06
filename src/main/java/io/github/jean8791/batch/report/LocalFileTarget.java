package io.github.jean8791.batch.report;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes reports to a folder on the local file system, creating it when needed.
 *
 * <p>File names cannot leave the base folder: names such as {@code ../secret.csv}
 * or absolute paths are rejected.
 */
public class LocalFileTarget implements ReportOutputTarget {

    private final Path baseDirectory;
    private Path lastOpenedPath;

    /**
     * Creates the target.
     *
     * @param baseDirectory folder that receives the files
     */
    public LocalFileTarget(String baseDirectory) {
        this.baseDirectory = Path.of(baseDirectory).toAbsolutePath().normalize();
    }

    @Override
    public OutputStream open(String fileName) throws IOException {
        Path target = baseDirectory.resolve(fileName).normalize();
        if (!target.startsWith(baseDirectory) || target.equals(baseDirectory)) {
            throw new IllegalArgumentException("File name must stay inside the base directory: " + fileName);
        }
        Files.createDirectories(target.getParent());
        lastOpenedPath = target;
        return Files.newOutputStream(target);
    }

    /**
     * @return full path of the last file opened, or {@code null} if none was opened
     */
    public String getLastOpenedPath() {
        return lastOpenedPath != null ? lastOpenedPath.toString() : null;
    }
}
