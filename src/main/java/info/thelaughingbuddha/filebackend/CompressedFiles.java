package info.thelaughingbuddha.filebackend;

import com.sun.istack.internal.NotNull;
import info.thelaughingbuddha.CompressedFileConfig;
import info.thelaughingbuddha.exception.CorruptedDataException;
import io.appulse.utils.Bytes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class CompressedFiles implements Iterable<IOQContent>, AutoCloseable {

    private static final byte[] CLEAR = new byte[8192];

    FileManager files;

    long maxFileSizeInBytes;

    Function<CorruptedDataException, Boolean> corruptionHandler;


    @Builder
    CompressedFiles(@NotNull String queueName,
                    @NotNull CompressedFileConfig config,
                    Boolean restoreFromDisk,
                    Function<CorruptedDataException, Boolean> corruptionHandler) {
        Boolean restoreFromDiskValue = ofNullable(restoreFromDisk)
                .orElse(Boolean.TRUE);

        final Function<CorruptedDataException, Boolean> corruptionHandelerFunction =
                ofNullable(corruptionHandler).orElse((e) -> Boolean.TRUE);

        files = FileManager.builder()
                .folder(config.getFolder())
                .prefix(queueName + "-")
                .suffix(".compressed")
                .build();

        if (!restoreFromDiskValue) {
            files.clear();
        }

        maxFileSizeInBytes = config.getMaxSizeInBytes();
        this.corruptionHandler = corruptionHandelerFunction;
    }



    @Override
    public void close() throws Exception {
        files.close();
    }

    @Override
    public Iterator<IOQContent> iterator() {
        return new CompressedFileIteratorMultipleFiles(files.getFilesFromQueue());
    }

    int peekContentPart(@NotNull Bytes bytes) {
        return
    }


    @Override
    public void forEach(Consumer<? super IOQContent> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<IOQContent> spliterator() {
        return Iterable.super.spliterator();
    }

    @FunctionalInterface
    interface RecordReader {
        int read(FileChannel channel, RecordHeader header, Bytes buffer) throws IOException;
    }

    @Value
    @AllArgsConstructor
    static class ReadResult {

        long readed;

        boolean removeFile;

        boolean hasReaded() {
            return readed > 0;
        }

        static ReadResult endOfFile() {
            return new ReadResult(0, true);
        }

        static ReadResult continueReading(long readed) {
            return new ReadResult(readed, false);
        }

    }
}
