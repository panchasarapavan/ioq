package info.thelaughingbuddha.filebackend;


import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;

import java.nio.channels.FileChannel;
import java.nio.file.Path;

@Value
@Builder
public class IOQContent {
    Path file;
    long offset;
    int length;

    @SneakyThrows
    public void open(IOQContentConsumer ioqContentConsumer) {
        try (FileChannel channel = FileChannel.open(file, READ, WRITE)) {
            channel.position(offset);
            ioqContentConsumer.accept(length, channel);
        }
    }
}
