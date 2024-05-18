package info.thelaughingbuddha.exception;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;
import java.util.Locale;


@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class CorruptedDataException extends ReadingFromDiskException {

    private static String getDefaultMessage(Path path, long offset) {
        return String.format(Locale.ENGLISH, "Corrupted data in file '%s' at offset '%d'",
                path.toAbsolutePath(), offset);
    }

    private final long offset;

    public CorruptedDataException(Path file, long offset) {
        this(file, offset, getDefaultMessage(file, offset));
    }

    public CorruptedDataException(Path file, long offset, String message) {
        super(file, message);
        this.offset = offset;
    }

    public CorruptedDataException(Path file, long offset, Throwable throwable) {
        this(file, offset, getDefaultMessage(file, offset), throwable);
    }

    public CorruptedDataException(Path file, long offset, String message, Throwable throwable) {
        super(file, message, throwable);
        this.offset = offset;
    }

}
