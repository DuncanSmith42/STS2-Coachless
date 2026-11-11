package coachless.ui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MapArt {
    private MapArt() {}

    private static final Map<String, List<String>> NODE_NAMES = Map.of(
            "monster", List.of("map_monster", "monster", "fight", "combat"),
            "elite", List.of("map_elite", "elite"),
            "rest_site", List.of("map_rest", "rest_site", "rest", "campfire"),
            "shop", List.of("map_shop", "shop"),
            "treasure", List.of("map_chest", "treasure", "chest"),
            "unknown", List.of("map_unknown", "unknown", "event", "question"),
            "ancient", List.of("ancient_node_neow", "ancient", "start", "neow"),
            "boss", List.of("boss"));

    
    private static final List<String> DIRS = List.of("/images/map_nodes/");

    private static final Map<String, BufferedImage> RAW = new HashMap<>();
    private static final Map<String, BufferedImage> SCALED = new HashMap<>();

    static BufferedImage node(String type) {
        return first(NODE_NAMES.getOrDefault(type, List.of(type)));
    }

    // Picture for one specific boss
    static BufferedImage boss(String bossId) {
        if (bossId != null && !bossId.isEmpty()) {
            String n = bossId.substring(bossId.indexOf('.') + 1).toLowerCase();
            BufferedImage img = first(List.of(n + "_icon", n, n.replaceAll("_boss$", "")));
            if (img != null) return img;
        }
        return node("boss");
    }

    // static BufferedImage nodeBackdrop() {
    //     return first(List.of("map_node_background", "node_background", "background"));
    // }

    // static BufferedImage background() {
    //     return first(List.of("map_background", "bg"));
    // }

    static BufferedImage ancient(String ancientId) {
        if (ancientId != null && !ancientId.isEmpty()) {
            String n = ancientId.substring(ancientId.indexOf('.') + 1).toLowerCase();
            BufferedImage img = first(List.of("ancient_node_" + n, n));
            if (img != null) return img;
        }
        return node("ancient");
    }

    static BufferedImage fit(BufferedImage src, int size) {
        size = Math.max(1, size);
        String key = System.identityHashCode(src) + ":" + size;
        BufferedImage out = SCALED.get(key);
        if (out != null) return out;
        double s = Math.min((double) size / src.getWidth(), (double) size / src.getHeight());
        int w = Math.max(1, (int) Math.round(src.getWidth() * s));
        int h = Math.max(1, (int) Math.round(src.getHeight() * s));
        BufferedImage cur = src;
        while (cur.getWidth() / 2 >= w && cur.getHeight() / 2 >= h) {
            cur = resize(cur, cur.getWidth() / 2, cur.getHeight() / 2);
        }
        out = resize(cur, w, h);
        SCALED.put(key, out);
        return out;
    }

    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage first(List<String> names) {
        for (String n : names) {
            BufferedImage img = load(n);
            if (img != null) return img;
        }
        return null;
    }

    private static BufferedImage load(String name) {
        if (RAW.containsKey(name)) return RAW.get(name);
        BufferedImage img = null;
        for (String dir : DIRS) {
            try (InputStream in = MapArt.class.getResourceAsStream(dir + name + ".png")) {
                if (in != null) img = ImageIO.read(in);
            } catch (Exception ignored) {
                // unreadable picture: treat as missing
            }
            if (img != null) break;
        }
        RAW.put(name, img);
        return img;
    }
}