package info.thelaughingbuddha.filebackend;

import lombok.SneakyThrows;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public class CompressedFileIteratorSingleFile implements Iterator<IOQContent>, AutoCloseable {

    private Path file;
    private FileChannel channel;
    private RecordHeader recordHeader;
    private IOQContent lastReturned;
    private IOQContent next;

    @SneakyThrows
    void init(Path path) {
        if (channel != null) {
            close();
        }
        file = path;
        channel = FileChannel.open(path, READ, WRITE);
        if (recordHeader == null) {
            recordHeader = new RecordHeader();
        }
    }

    @Override
    public void close() throws Exception {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    @SneakyThrows
    public boolean hasNext() {
        if (channel == null) {
            return false;
        }

        if (next != null) {
            return false;
        }

        do {
            if (recordHeader.isEnd()) {
                return false;
            } else if (recordHeader.isRecord()) {
                break;
            } else if (recordHeader.isJump()) {
                long newPosition = recordHeader.getValue();
                channel.position(newPosition);
            }
        } while (true);

        next = IOQContent.builder()
                .file(file)
                .offset(channel.position())
                .length(recordHeader.getLength())
                .build();

        long nextHeaderPosition = channel.position() + recordHeader.getLength();
        channel.position(nextHeaderPosition);
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
        long oldHeaderStartPosition = lastReturned.getOffset() - RecordHeader.BYTES;
        recordHeader.writeJump(channel, oldHeaderStartPosition, channel.position());
        lastReturned = null;
    }
}
