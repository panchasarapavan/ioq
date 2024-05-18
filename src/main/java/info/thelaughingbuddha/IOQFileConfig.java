package info.thelaughingbuddha;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

@Value
@Builder
public class IOQFileConfig {

    Path folder;
    Integer maxCount;

}
