package coachless.data;

import java.nio.file.Files;
import java.nio.file.Path;

public class ProgressFileType implements FileType {
    @Override
    public boolean matches(Path file) {
        return file.getFileName().toString().equals("progress.save");
    }
 
    @Override
    public Object parse(Path file) throws Exception {
        return ProgressSave.parse(Files.readString(file));
    }
}