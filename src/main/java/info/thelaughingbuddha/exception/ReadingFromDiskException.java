package info.thelaughingbuddha.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;

import static java.util.Locale.ENGLISH;


@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class ReadingFromDiskException extends RuntimeException {

    private static String getDefaultMessage (Path file) {
        return String.format(ENGLISH, "Error during reading file's '%s' content", file.toAbsolutePath());
    }

    private final Path file;

    public ReadingFromDiskException(Path file) {
        this(file, getDefaultMessage(file));
    }

    public ReadingFromDiskException(Path file, String message) {
        super(message);
        this.file = file;
    }

    public ReadingFromDiskException(Path file, Throwable throwable) {
        super(getDefaultMessage(file), throwable);
        this.file = file;
    }

    public ReadingFromDiskException(Path file, String message, Throwable throwable) {
        super(message, throwable);
        this.file = file;
    }


}
