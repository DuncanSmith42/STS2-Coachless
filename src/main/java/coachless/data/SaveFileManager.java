package coachless.data;

import javax.swing.Timer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;



public final class SaveFileManager {

    //One folder the user gives us, plus what we got out of it
    public static final class Entry {
        public final Path path;
        public final FileType type;
        public ProgressSave data;
        public String error;
        long modified = Long.MIN_VALUE;

        Entry(Path path, FileType type) { this.path = path; this.type = type; }
    }

    public static final class Folder {
        public final Path path;
        public final List<Entry> files = new ArrayList<>();
        public String error;

        Folder(Path path) { this.path = path; }
    }

    private static final String PREF_KEY = "folders";
    private static final int MAX_DEPTH = 6;          
    private static final int MAX_VISITED = 20000;    
    private static final int RESCAN_EVERY = 5;

    private final Preferences prefs = Preferences.userNodeForPackage(SaveFileManager.class);
    private final List<FileType> types;
    private final List<Folder> folders = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private int tick;

    public SaveFileManager(FileType... types) {
        this.types = List.of(types);
        for (String p : prefs.get(PREF_KEY, "").split("\n")) {
            if (p.isBlank()) continue;
            try {
                Folder f = new Folder(Paths.get(p));
                rescan(f);
                folders.add(f);
            } catch (RuntimeException ignored) {}
        }
        Timer timer = new Timer(2000, ev -> poll());
        timer.setCoalesce(true);
        timer.start();
    }

    public List<Folder> folders() { return Collections.unmodifiableList(folders); }

    public <T> List<T> valid(Class<T> kind) {
        List<T> out = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        for (Folder f : folders) {
            for (Entry e : f.files) {
                if (kind.isInstance(e.data) && seen.add(e.path)) out.add(kind.cast(e.data));
            }
        }
        return out;
    }

    public List<ProgressSave> validSaves() { return valid(ProgressSave.class); }

    public void addListener(Runnable r) { listeners.add(r); }

    public void addFolder(Path path) {
        Path p = path.toAbsolutePath().normalize();
        for (Folder f : folders) if (f.path.equals(p)) return;
        Folder f = new Folder(p);
        rescan(f);
        folders.add(f);
        persist();
        fire();
    }

    public void remove(Folder f) {
        if (folders.remove(f)) {
            persist();
            fire();
        }
    }

    //Internals
    private void poll() {
        boolean changed = false;
        boolean rescan = ++tick % RESCAN_EVERY == 0;
        for (Folder f : folders) {
            for (Entry e : f.files) {
                try {
                    long m = Files.getLastModifiedTime(e.path).toMillis();
                    if (m != e.modified) {
                        reload(e);
                        changed = true;
                    }
                } catch (IOException ex) {
                    rescan = true;
                }
            }
        }
        if (rescan) {
            for (Folder f : folders) changed |= rescan(f);
        }
        if (changed) fire();
    }

    private boolean rescan(Folder f) {
        Map<Path, Entry> old = new HashMap<>();
        for (Entry e : f.files) old.put(e.path, e);
 
        List<Entry> found = new ArrayList<>();
        String err = null;
        if (!Files.isDirectory(f.path)) {
            err = "Folder not found";
        } else {
            try (Stream<Path> walk = Files.walk(f.path, MAX_DEPTH)) {
                Iterator<Path> it = walk.limit(MAX_VISITED).iterator();
                while (it.hasNext()) {
                    Path p = it.next();
                    if (!Files.isRegularFile(p)) continue;
                    Entry e = old.get(p);
                    if (e == null) {
                        FileType t = typeOf(p);
                        if (t == null) continue;
                        e = new Entry(p, t);
                        reload(e);
                    }
                    found.add(e);
                }
            } catch (IOException | UncheckedIOException ex) {
                boolean changed = !Objects.equals("Can't read folder", f.error);
                f.error = "Can't read folder";
                return changed;
            }
        }
 
        found.sort(Comparator.comparing(e -> e.path));
        Set<Path> after = new HashSet<>();
        for (Entry e : found) after.add(e.path);
        boolean changed = !old.keySet().equals(after) || !Objects.equals(err, f.error);
 
        f.files.clear();
        f.files.addAll(found);
        f.error = err;
        return changed;
    }

        private FileType typeOf(Path p) {
        for (FileType t : types) if (t.matches(p)) return t;
        return null;
    }

    private void reload(Entry e) {
        try {
            e.modified = Files.getLastModifiedTime(e.path).toMillis();
            e.data = ProgressSave.parse(Files.readString(e.path));
            e.error = null;
        } catch (Exception ex) {
            e.data = null;
            e.error = ex instanceof IOException ? "Can't read file" : String.valueOf(ex.getMessage());
        }
    }

    private void persist() {
        StringBuilder sb = new StringBuilder();
        for (Folder f : folders) sb.append(f.path).append('\n');
        prefs.put(PREF_KEY, sb.toString());
    }

    private void fire() {
        for (Runnable r : new ArrayList<>(listeners)) r.run();
    }
}
