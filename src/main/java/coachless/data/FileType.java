package coachless.data;

import java.nio.file.Path;

public interface FileType {
    boolean matches(Path file);
    Object parse(Path file) throws Exception;
}
