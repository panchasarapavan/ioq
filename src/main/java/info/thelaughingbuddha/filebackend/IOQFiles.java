package info.thelaughingbuddha.filebackend;

import com.sun.istack.internal.NotNull;
import info.thelaughingbuddha.IOQFileConfig;
import info.thelaughingbuddha.exception.CorruptedDataException;
import io.appulse.utils.Bytes;
import io.appulse.utils.ReadBytesUtils;
import io.appulse.utils.WriteBytesUtils;
import lombok.Builder;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static info.thelaughingbuddha.Constants.IO_FILE_EXTENSION;
import static java.util.Optional.ofNullable;

public class IOQFiles implements AutoCloseable, Iterable<IOQContent> {

    private final FileManager files;

    private final int maxCount;

    private final Function<CorruptedDataException, Boolean> corruptionHandler;

    @Builder
    IOQFiles(@NotNull String queueName,
             @NotNull IOQFileConfig config,
             Boolean restoreFromDisk,
             Function<CorruptedDataException, Boolean> corruptionHandler) {
        final Boolean restoreFromDiskValue = ofNullable(restoreFromDisk).orElse(Boolean.TRUE);
        final Function<CorruptedDataException, Boolean> corruptionHandelerFunction =
                ofNullable(corruptionHandler).orElse((e) -> Boolean.TRUE);

        files = FileManager.builder()
                .folder(config.getFolder())
                .prefix(queueName + "-")
                .suffix(IO_FILE_EXTENSION)
                .build();

        if (!restoreFromDiskValue) {
            files.clear();
        }

        maxCount = config.getMaxCount();
        this.corruptionHandler = corruptionHandelerFunction;
    }

    @Override
    public Iterator<IOQContent> iterator() {
        return new IOQFilesIterator();
    }

    void write(@NotNull Bytes buffer) {
        Path file = files.createNextFile();
        WriteBytesUtils.write(file, buffer);
    }

    @SneakyThrows
    int pollTo(@NotNull Bytes buffer) {
        return readTo(buffer, files::poll, files::remove);
    }

    @SneakyThrows
    int peakTo(@NotNull Bytes buffer) {
        return readTo(buffer, files::poll, null);
    }

    boolean isLimitExceeded() {
        return files.getFilesFromQueue().size() > maxCount;
    }

    @SneakyThrows
    long diskSize() {
        long result = 0;
        for (Path file : getFiles()) {
            result += Files.size(file);
        }
        return result;
    }

    Collection<Path> getFiles() {
        return files.getFilesFromQueue();
    }

    void remove(Collection<Path> paths) {
        files.remove(paths);
    }

    private int readTo(Bytes buffer, Supplier<Path> supplier, Consumer<Path> actionAfter) {
        int writerIndex = buffer.writerIndex();
        int readerIndex = buffer.readerIndex();

        do {
            Path file = supplier.get();
            try {
                if (file == null) {
                    return 0;
                }

                int size = (int) Files.size(file);
                if (!buffer.isWritable(size)) {
                    int newCapacity = buffer.writerIndex() + size;
                    buffer.capacity(newCapacity);
                }

                int read = ReadBytesUtils.read(file, buffer);
                if (read > 0 && actionAfter != null) {
                    actionAfter.accept(file);
                }
                return read;
            } catch (Exception e) {
                CorruptedDataException corruptedDataException = new CorruptedDataException(file, 0, e);
                corruptionHandler.apply(corruptedDataException);
                files.remove(file);
            }
            buffer.writerIndex(writerIndex);
            buffer.readerIndex(readerIndex);
        } while (true);
    }

    @Override
    public void close() throws Exception {
        files.close();
    }

    private class IOQFilesIterator implements Iterator<IOQContent> {

        final Iterator<Path> paths = files.getFilesFromQueue().iterator();

        IOQContent lastReturned;

        IOQContent next;

        @Override
        @SneakyThrows
        public boolean hasNext() {
            if (next != null) {
                return false;
            }

            if (!paths.hasNext()) {
                return false;
            }

            Path path = paths.next();
            next = IOQContent.builder()
                    .file(path)
                    .offset(0)
                    .length((int) Files.size(path))
                    .build();

            return true;
        }

        @Override
        public IOQContent next() {
            if (next != null || hasNext()) {
                lastReturned = next;
                next = null;
                return lastReturned;
            }
            throw new NoSuchElementException();
        }

        @Override
        @SneakyThrows
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            paths.remove();
            Files.deleteIfExists(lastReturned.getFile());
            lastReturned = null;
        }
    }
}
