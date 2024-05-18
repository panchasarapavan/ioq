package info.thelaughingbuddha.filebackend;

import com.sun.istack.internal.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public final class FileManager implements AutoCloseable {

    private final AtomicInteger index;

    private final Queue<Path> queue;

    private final Path folder;

    private final String prefix;

    private final String suffix;

    private final Pattern fileNamePattern;

    private final Pattern fileIndexPattern;

    @Builder
    FileManager(@NotNull Path folder, String prefix, String suffix) {
        index = new AtomicInteger(0);
        this.folder = folder;
        this.prefix = ofNullable(prefix)
                .map(String::trim)
                .orElse("");
        this.suffix = ofNullable(suffix)
                .map(String::trim)
                .orElse("");

        final String fileNameRegex = String.format(Locale.ENGLISH, "^%s\\d+%s$", this.prefix, this.suffix);
        fileNamePattern = Pattern.compile(fileNameRegex);

        final String fileIndexRegex = String.format(Locale.ENGLISH, "^%s(?<index>\\d+)%s$", this.prefix, this.suffix);
        fileIndexPattern = Pattern.compile(fileIndexRegex);

        queue = getFilesFromFileSystem();
        if (!queue.isEmpty()) {
            Path[] array = queue.toArray(new Path[0]);
            Path lastPath = array[array.length - 1];
            int lastIndex = getIndex(lastPath);
            index.set(lastIndex + 1);
        }
    }

    @Override
    public void close() throws Exception {
        queue.clear();
    }

    Queue<Path> getFilesFromQueue() {
        return queue;
    }

    @SneakyThrows
    Queue<Path> getFilesFromFileSystem() {
        final File file = folder.toFile();

        final String[] fileList = file.list();
        if (fileList != null && fileList.length == 0) {
            return new LinkedList<>();
        }

        Predicate<Path> namePatternPredicate = path ->
                ofNullable(path)
                        .map(Path::getFileName)
                        .filter(Objects::nonNull)
                        .map(Path::toString)
                        .map(fileNamePattern::matcher)
                        .map(Matcher::matches)
                        .orElse(false);

        return Files.list(folder)
                .filter(Files::isRegularFile)
                .filter(namePatternPredicate)
                .sorted(Comparator.comparing(this::getIndex))
                .collect(Collectors.toCollection(LinkedList::new));


    }

    int getIndex(@NonNull Path path) {
        return of(path)
                .map(Path::getFileName)
                .filter(Objects::nonNull)
                .map(Path::toString)
                .map(fileIndexPattern::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group("index"))
                .map(Integer::valueOf)
                .orElseThrow(() -> {
                    String msg = String.format(ENGLISH, "File '%s' doesn't have index group", path.toString());
                    return new IllegalArgumentException(msg);
                });
    }

    Path getFile(int fileIndex) {
        String fileName = String.format(ENGLISH, "%s%d%s", prefix, fileIndex, suffix);
        return folder.resolve(fileName);
    }

    Optional<Path> findFile(int fileIndex) {
        return ofNullable(getFile(fileIndex)).filter(Files::exists);
    }

    @SneakyThrows
    Path createNextFile() {
        Path result;
        do {
            int nextIndex = index.getAndIncrement();
            result = getFile(nextIndex);
        } while (Files.exists(result));

        Files.createFile(result);
        queue.add(result);
        return result;
    }

    Path poll() {
        return queue.poll();
    }

    Path peek() {
        return queue.peek();
    }

    @SneakyThrows
    void remove(@NotNull Path... paths) {
        for (Path path: paths) {
            Files.deleteIfExists(path);
            queue.remove(path);
        }
    }

    void remove(@NotNull Collection<Path> paths) {
        Path[] pathsArray = paths.toArray(new Path[0]);
        remove(pathsArray);
    }

    void clear() {
        remove(queue);
        index.set(0);
    }
}
