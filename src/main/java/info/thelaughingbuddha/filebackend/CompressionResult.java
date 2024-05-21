package info.thelaughingbuddha.filebackend;

import lombok.Value;

import java.nio.file.Path;
import java.util.List;

@Value
public class CompressionResult {

    List<Path> compressed;

    List<Path> remaining;
}
