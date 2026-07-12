package org.evo.reels.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class WriterManager {
    public static void writeResults(String resultPath, String content) throws IOException {
        Path path = Path.of(resultPath);
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
