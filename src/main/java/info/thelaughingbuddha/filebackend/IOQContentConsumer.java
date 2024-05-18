package info.thelaughingbuddha.filebackend;

import java.nio.channels.FileChannel;

/**
 *
 *  An operation that accepts the {@code length} and {@code channel} and returns no result
 */
public interface IOQContentConsumer {

    void accept(Integer length, FileChannel channel) throws Exception;
}
