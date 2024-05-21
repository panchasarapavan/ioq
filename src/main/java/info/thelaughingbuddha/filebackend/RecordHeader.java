package info.thelaughingbuddha.filebackend;

import io.appulse.utils.ReadBytesUtils;
import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.stream.Stream;

public class RecordHeader {

    static final int BYTES = Byte.BYTES + Long.BYTES; // marker + length

    ByteBuffer buffer = ByteBuffer.allocate(BYTES);

    RecordHeader readFrom(FileChannel channel) {
        buffer.clear();
        ReadBytesUtils.read(channel, buffer);
        return this;
    }

    @SneakyThrows
    RecordHeader skipJumps(FileChannel channel) {
        do {
            readFrom(channel);
            if (!isJump()) {
                return this;
            }
            long newPosition = getValue();
            channel.position(newPosition);
        } while (true);
    }

    @SneakyThrows
    void writeJump(FileChannel channel, long from, long to) {
        buffer.clear();
        buffer.put(Marker.JUMP.getValue());
        buffer.putLong(to);
        buffer.rewind();
        channel.write(buffer, from);
    }

    @SneakyThrows
    void writeRecord(FileChannel channel, long length) {
        buffer.clear();
        buffer.put(Marker.RECORD.getValue());
        buffer.putLong(length);
        buffer.rewind();
        channel.write(buffer);
    }

    @SneakyThrows
    void writeEnd(FileChannel channel) {
        buffer.clear();
        buffer.put(Marker.END.getValue());
        buffer.putLong(0);
        buffer.rewind();
        channel.write(buffer);
    }

    Marker getMarker() {
        byte value = buffer.get(0);
        return Marker.of(value);
    }

    long getValue() {
        return buffer.getLong(1);
    }

    int getLength() {
        return (int) getValue();
    }

    boolean isRecord() {
        if (getMarker() != Marker.RECORD) {
            return false;
        }

        if (getValue() == 0) {
            throw new IllegalStateException();
        }
        return true;
    }

    boolean isJump() {
        if (getMarker() != Marker.JUMP) {
            return false;
        }

        if (getValue() == 0) {
            throw new IllegalStateException();
        }
        return true;
    }

    boolean isEnd() {
        if (getMarker() != Marker.END) {
            return false;
        }

        if (getValue() == 0) {
            throw new IllegalStateException();
        }

        return true;
    }

    private enum Marker {
        RECORD(1),
        JUMP(2),
        END(4),
        UNDEFINED(0xFF);

        byte value;

        Marker (int value) {
            this.value = (byte) value;
        }

        byte getValue() {
            return value;
        }

        static Marker of(byte value) {
            return Stream.of(values())
                    .filter(it -> it.getValue() == value)
                    .findAny()
                    .orElse(UNDEFINED);
        }
    }
}
