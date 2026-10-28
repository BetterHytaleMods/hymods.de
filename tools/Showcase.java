import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Renders the pictures of hymods.de from a mod's own models and textures: any size, transparent ground,
 * glowing parts with a soft glow. Built on the mod's renderer ({@code dev/art} in the mod repository:
 * {@code Blocky} reads the models, {@code Json} parses them).
 *
 * <pre>
 * javac -d build &lt;mod&gt;/dev/art/*.java tools/*.java
 * java -cp build Showcase &lt;mod checkout&gt; &lt;Assets.zip&gt; &lt;out dir&gt;
 * </pre>
 *
 * The game's {@code Assets.zip} supplies the head the goggles are shown on and the stone of the ore blocks.
 * Nothing from it is copied into this repository; only the rendered pictures are.
 */
public final class Showcase {
    static Path common;
    static ZipFile game;
    static Path out;

    private static final String GOGGLES = "Items/FlexiisGoggles/";
    private static final String BLOCKS = "Blocks/FlexiisGoggles/";
    private static final String RESOURCES = "Resources/FlexiisGoggles/";

    /** Goggles file names and the page's names for them. */
    static final String[][] ALL_GOGGLES = {
            {"Zoom_1", "zoom-1"}, {"Zoom_2", "zoom-2"}, {"Zoom_3", "zoom-3"}, {"Zoom_4", "zoom-4"},
            {"NightVision_1", "nightvision-1"}, {"NightVision_2", "nightvision-2"}, {"NightVision_3", "nightvision-3"},
            {"NightVision_4", "nightvision-4"}, {"Ultra", "ultra"}};

    static final String[][] ALL_HELMS = {
            {"Scout_Copper", "scout-copper"}, {"Scout_Iron", "scout-iron"}, {"Scout_Thorium", "scout-thorium"},
            {"Scout_Cobalt", "scout-cobalt"}, {"Scout_Adamantite", "scout-adamantite"},
            {"Nightwatch_Copper", "nightwatch-copper"}, {"Nightwatch_Iron", "nightwatch-iron"},
            {"Nightwatch_Thorium", "nightwatch-thorium"}, {"Nightwatch_Cobalt", "nightwatch-cobalt"},
            {"Nightwatch_Adamantite", "nightwatch-adamantite"}, {"Overseer", "overseer"}};

    private Showcase() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: Showcase <mod checkout> <Assets.zip> <out dir> [only...]");
            System.exit(2);
        }
        common = Path.of(args[0], "src/main/resources/Common");
        game = new ZipFile(args[1]);
        out = Path.of(args[2]);
        Files.createDirectories(out);
        List<String> only = Arrays.asList(args).subList(3, args.length);
        Map<String, Job> jobs = new LinkedHashMap<>();
        jobs.put("hero", Showcase::hero);
        jobs.put("icons", Showcase::icons);
        jobs.put("worn", Showcase::worn);
        jobs.put("helms", Showcase::helms);
        jobs.put("benches", Showcase::benches);
        jobs.put("materials", Showcase::materials);
        jobs.put("ores", Showcase::ores);
        jobs.put("caves", Showcase::caves);
        jobs.put("social", Showcase::social);
        for (Map.Entry<String, Job> job : jobs.entrySet()) {
            if (only.isEmpty() || only.contains(job.getKey())) {
                job.getValue().run();
                System.out.println("  " + job.getKey());
            }
        }
        System.out.println("pictures written to " + out);
    }

    interface Job {
        void run() throws IOException;
    }

    // ---------------------------------------------------------------- the pictures

    /** The UltraGoggles, large and glowing: front parts only, as on the icon. */
    static void hero() throws IOException {
        Scene s = new Scene().add(front(model(GOGGLES + "Goggles_Ultra.blockymodel")), texture(GOGGLES + "Goggles_Ultra_Texture.png"));
        double yaw = 24, pitch = 12;
        Rect frame = s.bounds(yaw, pitch);
        Frame f = s.render(yaw, pitch, frame, 880 / frame.width(), 120, 1, null);
        write(bloom(f, 34, 2.0, 0.45), "hero-ultragoggles");
        // smaller, for the mod's card on the start page
        Frame card = s.render(yaw, pitch, frame, 540 / frame.width(), 50, 1, null);
        write(bloom(card, 18, 2.0, 0.45), "card-ultragoggles");
    }

    /** Icons of the goggles for the tables: front parts only, as in the game's inventory. */
    static void icons() throws IOException {
        for (String[] g : ALL_GOGGLES) {
            Scene s = new Scene().add(front(model(GOGGLES + "Goggles_" + g[0] + ".blockymodel")), texture(GOGGLES + "Goggles_" + g[0] + "_Texture.png"));
            write(square(s, 28, 14, 128, 6), "icon-goggles-" + g[1]);
        }
    }

    /** Every goggles on a head, all at the same scale. */
    static void worn() throws IOException {
        double yaw = 32, pitch = 10;
        List<Scene> scenes = new ArrayList<>();
        for (String[] g : ALL_GOGGLES) {
            scenes.add(head().add(model(GOGGLES + "Goggles_" + g[0] + ".blockymodel"), texture(GOGGLES + "Goggles_" + g[0] + "_Texture.png")));
        }
        Rect frame = union(scenes, yaw, pitch);
        for (int i = 0; i < scenes.size(); i++) {
            Frame f = scenes.get(i).render(yaw, pitch, frame, 440 / frame.width(), 20, 1, null);
            write(bloom(f, 10, 1.1, 0.25), "worn-" + ALL_GOGGLES[i][1]);
        }
        // twice as large for the two that stand on their own on the page
        for (int i : new int[]{3, 8}) {
            Frame f = scenes.get(i).render(yaw, pitch, frame, 880 / frame.width(), 40, 1, null);
            write(bloom(f, 20, 1.1, 0.25), "worn-" + ALL_GOGGLES[i][1] + "-960");
        }
    }

    /** The helms, all at the same scale. */
    static void helms() throws IOException {
        double yaw = -40, pitch = 18;
        List<Scene> scenes = new ArrayList<>();
        for (String[] h : ALL_HELMS) {
            scenes.add(new Scene().add(model(GOGGLES + "Helm_" + h[0] + ".blockymodel"), texture(GOGGLES + "Helm_" + h[0] + "_Texture.png")));
        }
        Rect frame = union(scenes, yaw, pitch);
        for (int i = 0; i < scenes.size(); i++) {
            Frame f = scenes.get(i).render(yaw, pitch, frame, 300 / frame.width(), 14, 1, null);
            write(bloom(f, 7, 1.0, 0.2), "helm-" + ALL_HELMS[i][1]);
        }
    }

    /** The bench's five tiers at one scale (for the row) and the last one large. */
    static void benches() throws IOException {
        double yaw = -35, pitch = 25;
        List<Scene> scenes = new ArrayList<>();
        for (int tier = 1; tier <= 5; tier++) {
            scenes.add(new Scene().add(model(BLOCKS + "Optician_Bench_Tier" + tier + ".blockymodel"), texture(BLOCKS + "Optician_Bench_Tier" + tier + "_Texture.png")));
        }
        Rect frame = union(scenes, yaw, pitch);
        for (int i = 0; i < scenes.size(); i++) {
            Frame f = scenes.get(i).render(yaw, pitch, frame, 460 / frame.width(), 16, 1, null);
            write(bloom(f, 9, 1.0, 0.25), "bench-tier" + (i + 1));
        }
        Scene top = scenes.get(4);
        Rect own = top.bounds(yaw, pitch);
        write(bloom(top.render(yaw, pitch, own, 1040 / own.width(), 40, 1, null), 22, 1.2, 0.3), "bench-tier5-large");
    }

    /** Glass, quartz, lumenite and the four lenses, each on its own. */
    static void materials() throws IOException {
        String[][] items = {
                {"Lens_Glass", "lens-glass", "25", "18"}, {"Lens_Polished", "lens-polished", "25", "18"},
                {"Lens_Crystal", "lens-crystal", "25", "18"}, {"Lens_Void", "lens-void", "25", "18"},
                {"Glass", "glass", "35", "35"}, {"Quartz", "quartz", "30", "15"}, {"Lumenite", "lumenite", "30", "15"}};
        for (String[] it : items) {
            Scene s = new Scene().add(model(RESOURCES + it[0] + ".blockymodel"), texture(RESOURCES + it[0] + "_Texture.png"));
            double yaw = Double.parseDouble(it[2]), pitch = Double.parseDouble(it[3]);
            Rect frame = s.bounds(yaw, pitch);
            Frame f = s.render(yaw, pitch, frame, 168 / Math.max(frame.width(), frame.height()), 12, 1, null);
            write(bloom(f, 6, 1.0, 0.25), "item-" + it[1]);
        }
    }

    /** The ore blocks: quartz ore, and lumenite ore as everyone sees it and as night vision shows it. */
    static void ores() throws IOException {
        double yaw = 35, pitch = 25;
        Scene quartz = stone("Rock_Stone").add(model(RESOURCES + "Ore_Quartz.blockymodel"), texture(RESOURCES + "Ore_Quartz_Texture.png"));
        Scene hidden = stone("Rock_Stone");
        Scene revealed = stone("Rock_Stone").add(model(RESOURCES + "Ore_Lumenite.blockymodel"), texture(RESOURCES + "Ore_Lumenite_Texture.png"));
        Rect frame = union(List.of(quartz, revealed), yaw, pitch);
        double scale = 420 / frame.width();
        write(bloom(quartz.render(yaw, pitch, frame, scale, 20, 1, null), 9, 1.0, 0.25), "ore-quartz");
        write(bloom(hidden.render(yaw, pitch, frame, scale, 20, 1, null), 9, 1.0, 0.25), "ore-lumenite-hidden");
        write(bloom(revealed.render(yaw, pitch, frame, scale, 20, 1, null), 12, 1.3, 0.3), "ore-lumenite-revealed");
    }

    /** A corner of a cave with quartz ore: once in the dark and once lit up, as night vision shows it. */
    static void caves() throws IOException {
        double yaw = 40, pitch = 28;
        Scene cave = cave();
        Rect frame = cave.bounds(yaw, pitch);
        double scale = 740 / frame.width();
        write(bloom(cave.render(yaw, pitch, frame, scale, 20, 0.28, new double[]{0.62, 0.72, 1.0}), 12, 1.2, 0.3), "cave-dark");
        write(bloom(cave.render(yaw, pitch, frame, scale, 20, 1, null), 12, 1.2, 0.3), "cave-lit");
    }

    /** Link previews, 1200x630: the UltraGoggles on the site's colours with a name. */
    static void social() throws IOException {
        Scene s = new Scene().add(front(model(GOGGLES + "Goggles_Ultra.blockymodel")), texture(GOGGLES + "Goggles_Ultra_Texture.png"));
        double yaw = 24, pitch = 12;
        Rect frame = s.bounds(yaw, pitch);
        BufferedImage art = bloom(s.render(yaw, pitch, frame, 460 / frame.width(), 70, 1, null), 20, 2.0, 0.45);
        write(card(art, "FlexiisGoggles", "Hytale server mod"), "og-flexiisgoggles");
        write(card(art, "HYMODS", "Hytale server mods"), "og-hymods");
    }

    static BufferedImage card(BufferedImage art, String title, String line) {
        int w = 1200, h = 630;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setPaint(new GradientPaint(0, 0, new Color(0x10131a), w, h, new Color(0x2b3442)));
        g.fillRect(0, 0, w, h);
        float cx = 880, cy = 315;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), 360, new float[]{0f, 1f},
                new Color[]{new Color(87, 240, 255, 52), new Color(87, 240, 255, 0)}));
        g.fillRect(0, 0, w, h);
        g.drawImage(art, Math.round(cx - art.getWidth() / 2f), Math.round(cy - art.getHeight() / 2f), null);
        int x = 80;
        g.setFont(font(Font.BOLD, 68));
        g.setColor(new Color(0xf2f6fb));
        g.drawString(title, x, 296);
        g.setColor(new Color(0xd4af37));
        g.fillRect(x, 326, 64, 6);
        g.setFont(font(Font.PLAIN, 32));
        g.setColor(new Color(0xb9c3d1));
        g.drawString(line, x, 386);
        g.setColor(new Color(0x57f0ff));
        g.setFont(font(Font.BOLD, 28));
        g.drawString("hymods.de", x, 552);
        g.dispose();
        return img;
    }

    /** The first of a few clean sans-serif families that is installed. */
    static Font font(int style, int size) {
        List<String> installed = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String family : List.of("Inter Display", "Inter", "Helvetica Neue", "Helvetica", "Arial")) {
            if (installed.contains(family)) {
                return new Font(family, style, size);
            }
        }
        return new Font(Font.SANS_SERIF, style, size);
    }

    // ---------------------------------------------------------------- scenes

    /** The player's head (from the game) as a plain display head, ready for something worn. */
    static Scene head() {
        Map<String, Object> player = gameModel("Characters/Player.blockymodel");
        Map<String, Object> head = find(Json.array(player.get("nodes")), "Head");
        if (head == null) {
            throw new IllegalStateException("no Head in Player.blockymodel");
        }
        // the face is a model of its own in the game; give the head a plain front
        Map<String, Object> layout = Json.object(Json.object(head.get("shape")).get("textureLayout"));
        layout.putIfAbsent("front", face(64, 64));
        List<Object> kids = Json.array(head.get("children"));
        kids.removeIf(o -> !"Neck".equals(Json.object(o).get("name")));
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("nodes", new ArrayList<>(List.of(head)));
        return new Scene().add(model, mannequin());
    }

    /** A one-colour texture with a little grain, for the display head. */
    static BufferedImage mannequin() {
        BufferedImage img = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(7);
        int base = 0x545e6e;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, 0xff000000 | Art.shade(base, 0.97 + 0.06 * rnd.nextDouble()));
            }
        }
        return img;
    }

    /** One stone block (32 units, origin at the bottom centre) with the game's stone texture. */
    static Scene stone(String texture) {
        return new Scene().add(cube(), gameTexture("BlockTextures/" + texture + ".png"));
    }

    /** Stone blocks forming the corner of a cave (a floor, a back wall, a side wall) with quartz ore in them. */
    static Scene cave() {
        String[] stones = {"Rock_Stone", "Rock_Stone", "Rock_Stone_2", "Rock_Stone_3"};
        Random rnd = new Random(11);
        Scene s = new Scene();
        BufferedImage quartzTex = texture(RESOURCES + "Ore_Quartz_Texture.png");
        // x to the right, z towards the viewer: a floor, a wall at the back (z = -1) and one on the left (x = -1)
        List<int[]> list = new ArrayList<>();
        for (int x = -1; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    boolean floor = y == 0, back = z == -1, left = x == -1;
                    if (floor || back || left) {
                        list.add(new int[]{x, y, z});
                    }
                }
            }
        }
        int[][] blocks = list.toArray(new int[0][]);
        // ore blocks and the side that faces into the cave; crystals on the cut faces would look odd
        Object[][] ores = {{new int[]{0, 1, -1}, "front"}, {new int[]{2, 2, -1}, "front"}, {new int[]{-1, 1, 0}, "right"}, {new int[]{1, 0, 0}, "top"}};
        java.util.Set<String> solid = new java.util.HashSet<>();
        for (int[] b : blocks) {
            solid.add(b[0] + "," + b[1] + "," + b[2]);
        }
        for (int[] b : blocks) {
            double dx = b[0] * 32, dy = b[1] * 32, dz = b[2] * 32;
            String side = null;
            for (Object[] o : ores) {
                if (Arrays.equals((int[]) o[0], b)) {
                    side = (String) o[1];
                }
            }
            String tex = stones[rnd.nextInt(stones.length)];
            s.add(cube(exposed(solid, b)), gameTexture("BlockTextures/" + (side != null ? "Rock_Stone" : tex) + ".png"), dx, dy, dz);
            if (side != null) {
                s.add(outcrops(model(RESOURCES + "Ore_Quartz.blockymodel"), side), quartzTex, dx, dy, dz);
            }
        }
        return s;
    }

    /**
     * Only the crystals of an ore overlay that grow out of one side of the block. The overlay's crystal
     * groups are numbered by side: 0-3 top, 4-6 front, 7-8 back, 9-10 right, 11-12 left.
     */
    static Map<String, Object> outcrops(Map<String, Object> ore, String side) {
        int[] range = switch (side) {
            case "top" -> new int[]{0, 3};
            case "front" -> new int[]{4, 6};
            case "back" -> new int[]{7, 8};
            case "right" -> new int[]{9, 10};
            case "left" -> new int[]{11, 12};
            default -> throw new IllegalArgumentException(side);
        };
        prune(Json.array(ore.get("nodes")), name -> {
            if (!name.startsWith("Crystal_")) {
                return false;
            }
            int group = Integer.parseInt(name.split("_")[1]);
            return group < range[0] || group > range[1];
        });
        return ore;
    }

    /** The faces of a block that no neighbour covers. */
    static List<String> exposed(java.util.Set<String> solid, int[] b) {
        List<String> faces = new ArrayList<>();
        Object[][] sides = {{"front", 0, 0, 1}, {"back", 0, 0, -1}, {"left", -1, 0, 0}, {"right", 1, 0, 0}, {"top", 0, 1, 0}, {"bottom", 0, -1, 0}};
        for (Object[] side : sides) {
            String key = (b[0] + (int) side[1]) + "," + (b[1] + (int) side[2]) + "," + (b[2] + (int) side[3]);
            if (!solid.contains(key)) {
                faces.add((String) side[0]);
            }
        }
        return faces;
    }

    static Map<String, Object> cube() {
        return cube(List.of("front", "back", "left", "right", "top", "bottom"));
    }

    /** A 32-unit block with the given faces, each showing the whole 32x32 texture. */
    static Map<String, Object> cube(List<String> faces) {
        Map<String, Object> layout = new LinkedHashMap<>();
        for (String face : faces) {
            layout.put(face, face(0, 0));
        }
        Map<String, Object> shape = new LinkedHashMap<>();
        shape.put("type", "box");
        shape.put("offset", xyz(0, 16, 0));
        shape.put("settings", Map.of("size", xyz(32, 32, 32)));
        shape.put("textureLayout", layout);
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("name", "Block");
        node.put("position", xyz(0, 0, 0));
        node.put("shape", shape);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("nodes", new ArrayList<>(List.of(node)));
        return model;
    }

    static Map<String, Object> xyz(double x, double y, double z) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", x);
        m.put("y", y);
        m.put("z", z);
        return m;
    }

    static Map<String, Object> face(double u, double v) {
        Map<String, Object> f = new LinkedHashMap<>();
        Map<String, Object> off = new LinkedHashMap<>();
        off.put("x", u);
        off.put("y", v);
        f.put("offset", off);
        f.put("mirror", new LinkedHashMap<>(Map.of("x", false, "y", false)));
        f.put("angle", 0.0);
        return f;
    }

    /** The goggles without what runs round the head, as their icons show them. */
    static Map<String, Object> front(Map<String, Object> model) {
        Predicate<String> back = n -> n.startsWith("Strap_") || n.startsWith("Back_") || n.startsWith("Temple_");
        prune(Json.array(model.get("nodes")), back);
        return model;
    }

    static void prune(List<Object> nodes, Predicate<String> drop) {
        nodes.removeIf(o -> drop.test(String.valueOf(Json.object(o).get("name"))));
        for (Object o : nodes) {
            Object kids = Json.object(o).get("children");
            if (kids != null) {
                prune(Json.array(kids), drop);
            }
        }
    }

    static Map<String, Object> find(List<Object> nodes, String name) {
        for (Object o : nodes) {
            Map<String, Object> n = Json.object(o);
            if (name.equals(n.get("name"))) {
                return n;
            }
            Object kids = n.get("children");
            if (kids != null) {
                Map<String, Object> f = find(Json.array(kids), name);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- reading

    static Map<String, Object> model(String path) {
        try {
            return Json.object(Json.parse(Files.readString(common.resolve(path))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static BufferedImage texture(String path) {
        try {
            return argb(ImageIO.read(common.resolve(path).toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<String, Object> gameModel(String path) {
        try (InputStream in = gameFile(path)) {
            return Json.object(Json.parse(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static BufferedImage gameTexture(String path) {
        try (InputStream in = gameFile(path)) {
            return argb(ImageIO.read(in));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static InputStream gameFile(String path) throws IOException {
        ZipEntry entry = game.getEntry("Common/" + path);
        if (entry == null) {
            throw new IOException("not in Assets.zip: Common/" + path);
        }
        return game.getInputStream(entry);
    }

    static BufferedImage argb(BufferedImage img) {
        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.getGraphics().drawImage(img, 0, 0, null);
        return copy;
    }

    static void write(BufferedImage img, String name) throws IOException {
        ImageIO.write(img, "png", out.resolve(name + ".png").toFile());
    }

    // ---------------------------------------------------------------- rendering

    /** A rectangle in view space: x to the right, y up. */
    record Rect(double minX, double minY, double maxX, double maxY) {
        double width() {
            return maxX - minX;
        }

        double height() {
            return maxY - minY;
        }

        Rect union(Rect o) {
            return new Rect(Math.min(minX, o.minX), Math.min(minY, o.minY), Math.max(maxX, o.maxX), Math.max(maxY, o.maxY));
        }
    }

    static Rect union(List<Scene> scenes, double yaw, double pitch) {
        Rect r = null;
        for (Scene s : scenes) {
            Rect b = s.bounds(yaw, pitch);
            r = r == null ? b : r.union(b);
        }
        return r;
    }

    /** A picture and, separately, its glowing (fullbright) parts. */
    record Frame(BufferedImage image, BufferedImage glow) {
    }

    /** One textured face: corners, texture pixels, glowing or not, and the texture rectangle it may sample. */
    record Quad(double[][] p, double[][] uv, boolean glow, int u0, int v0, int u1, int v1) {
    }

    /** Models placed together, their textures stacked into one. */
    static final class Scene {
        final List<Quad> quads = new ArrayList<>();
        private final List<BufferedImage> sheets = new ArrayList<>();
        private final Map<BufferedImage, Integer> shared = new java.util.IdentityHashMap<>();
        private int height;
        private BufferedImage atlas;

        Scene add(Map<String, Object> model, BufferedImage texture) {
            return add(model, texture, 0, 0, 0);
        }

        /** Adds a model with its texture, moved by (dx, dy, dz) model units. */
        Scene add(Map<String, Object> model, BufferedImage texture, double dx, double dy, double dz) {
            int v0 = height;
            sheets.add(texture);
            height += texture.getHeight();
            atlas = null;
            for (Blocky.Face f : Blocky.faces(model)) {
                double[][] p = new double[4][];
                double[][] uv = new double[4][];
                double minU = Double.MAX_VALUE, minV = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
                for (int i = 0; i < 4; i++) {
                    p[i] = new double[]{f.corners()[i][0] + dx, f.corners()[i][1] + dy, f.corners()[i][2] + dz};
                    uv[i] = new double[]{f.uv()[i][0], f.uv()[i][1] + v0};
                    minU = Math.min(minU, uv[i][0]);
                    maxU = Math.max(maxU, uv[i][0]);
                    minV = Math.min(minV, uv[i][1]);
                    maxV = Math.max(maxV, uv[i][1]);
                }
                quads.add(quad(p, uv, f.fullbright(), texture, v0, minU, minV, maxU, maxV));
            }
            return this;
        }

        /**
         * Adds one face that already sits where it belongs, with its own texture ({@code uv} in that texture's pixels):
         * corners top-left, top-right, bottom-right, bottom-left. Faces that share a texture share its sheet.
         */
        Scene face(double[][] corners, double[][] texels, BufferedImage texture, boolean glow) {
            Integer at = shared.get(texture);
            if (at == null) {
                at = height;
                shared.put(texture, at);
                sheets.add(texture);
                height += texture.getHeight();
                atlas = null;
            }
            int v0 = at;
            double[][] p = new double[4][];
            double[][] uv = new double[4][];
            double minU = Double.MAX_VALUE, minV = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                p[i] = corners[i].clone();
                uv[i] = new double[]{texels[i][0], texels[i][1] + v0};
                minU = Math.min(minU, uv[i][0]);
                maxU = Math.max(maxU, uv[i][0]);
                minV = Math.min(minV, uv[i][1]);
                maxV = Math.max(maxV, uv[i][1]);
            }
            quads.add(quad(p, uv, glow, texture, v0, minU, minV, maxU, maxV));
            return this;
        }

        /** A face that samples only its own rectangle of its own texture, which starts at {@code v0} in the atlas. */
        private static Quad quad(double[][] p, double[][] uv, boolean glow, BufferedImage texture, int v0, double minU, double minV, double maxU, double maxV) {
            int tw = texture.getWidth() - 1, th = texture.getHeight() - 1;
            int u0 = Math.clamp((int) Math.floor(minU + 1e-6), 0, tw);
            int u1 = Math.clamp((int) Math.ceil(maxU - 1e-6) - 1, u0, tw);
            int t0 = Math.clamp((int) Math.floor(minV + 1e-6) - v0, 0, th);
            int t1 = Math.clamp((int) Math.ceil(maxV - 1e-6) - 1 - v0, t0, th);
            return new Quad(p, uv, glow, u0, v0 + t0, u1, v0 + t1);
        }

        BufferedImage atlas() {
            if (atlas == null) {
                int w = 1;
                for (BufferedImage s : sheets) {
                    w = Math.max(w, s.getWidth());
                }
                atlas = new BufferedImage(w, Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
                int v = 0;
                for (BufferedImage s : sheets) {
                    atlas.getGraphics().drawImage(s, 0, v, null);
                    v += s.getHeight();
                }
            }
            return atlas;
        }

        Rect bounds(double yaw, double pitch) {
            View view = new View(yaw, pitch);
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (Quad q : quads) {
                for (double[] c : q.p()) {
                    double[] v = view.apply(c);
                    minX = Math.min(minX, v[0]);
                    maxX = Math.max(maxX, v[0]);
                    minY = Math.min(minY, v[1]);
                    maxY = Math.max(maxY, v[1]);
                }
            }
            return new Rect(minX, minY, maxX, maxY);
        }

        /**
         * Renders the scene as the mod's icons are rendered (same light, same shading), with {@code frame}
         * mapped at {@code scale} pixels per unit and {@code pad} free pixels around it. {@code light}
         * scales the light on everything that does not glow; {@code tint}, if given, colours it.
         */
        Frame render(double yaw, double pitch, Rect frame, double scale, int pad, double light, double[] tint) {
            final int ss = 4;
            int w = (int) Math.ceil(frame.width() * scale) + 2 * pad, h = (int) Math.ceil(frame.height() * scale) + 2 * pad;
            int sw = w * ss, sh = h * ss;
            int[] color = new int[sw * sh];
            int[] glow = new int[sw * sh];
            float[] depth = new float[sw * sh];
            Arrays.fill(depth, Float.NEGATIVE_INFINITY);
            BufferedImage tex = atlas();
            int tw = tex.getWidth(), th = tex.getHeight();
            int[] texels = tex.getRGB(0, 0, tw, th, null, 0, tw);
            View view = new View(yaw, pitch);
            double[] lightDir = Art.norm(new double[]{-0.35, 0.85, 0.4});
            double k = scale * ss;
            double ox = pad * ss - frame.minX() * k, oy = pad * ss + frame.maxY() * k;
            for (Quad q : quads) {
                double[][] v = new double[4][];
                for (int i = 0; i < 4; i++) {
                    v[i] = view.apply(q.p()[i]);
                }
                double[] n = Art.norm(Art.cross(Art.sub(v[1], v[0]), Art.sub(v[3], v[0])));
                double shade = q.glow() ? 1.12 : (0.55 + 0.5 * Math.abs(Art.dot(n, lightDir))) * light;
                int[][] tris = {{0, 1, 2}, {0, 2, 3}};
                for (int[] t : tris) {
                    double[][] s = new double[3][];
                    double[][] tuv = new double[3][];
                    for (int i = 0; i < 3; i++) {
                        double[] p = v[t[i]];
                        s[i] = new double[]{ox + p[0] * k, oy - p[1] * k, p[2]};
                        tuv[i] = q.uv()[t[i]];
                    }
                    double area = Art.edge(s[0], s[1], s[2][0], s[2][1]);
                    if (Math.abs(area) < 1e-9) {
                        continue;
                    }
                    int x0 = (int) Math.max(0, Math.floor(Math.min(s[0][0], Math.min(s[1][0], s[2][0]))));
                    int x1 = (int) Math.min(sw - 1, Math.ceil(Math.max(s[0][0], Math.max(s[1][0], s[2][0]))));
                    int y0 = (int) Math.max(0, Math.floor(Math.min(s[0][1], Math.min(s[1][1], s[2][1]))));
                    int y1 = (int) Math.min(sh - 1, Math.ceil(Math.max(s[0][1], Math.max(s[1][1], s[2][1]))));
                    for (int y = y0; y <= y1; y++) {
                        for (int x = x0; x <= x1; x++) {
                            double px = x + 0.5, py = y + 0.5;
                            double w0 = Art.edge(s[1], s[2], px, py) / area;
                            double w1 = Art.edge(s[2], s[0], px, py) / area;
                            double w2 = Art.edge(s[0], s[1], px, py) / area;
                            if (w0 < 0 || w1 < 0 || w2 < 0) {
                                continue;
                            }
                            float z = (float) (w0 * s[0][2] + w1 * s[1][2] + w2 * s[2][2]);
                            int idx = y * sw + x;
                            if (z <= depth[idx]) {
                                continue;
                            }
                            double u = w0 * tuv[0][0] + w1 * tuv[1][0] + w2 * tuv[2][0];
                            double vv = w0 * tuv[0][1] + w1 * tuv[1][1] + w2 * tuv[2][1];
                            int tx = Math.clamp((int) Math.floor(u), q.u0(), q.u1());
                            int ty = Math.clamp((int) Math.floor(vv), q.v0(), q.v1());
                            int argb = texels[ty * tw + tx];
                            if ((argb >>> 24) < 128) {
                                continue;
                            }
                            depth[idx] = z;
                            int rgb = Art.shade(argb & 0xffffff, shade);
                            if (tint != null && !q.glow()) {
                                rgb = mul(rgb, tint);
                            }
                            color[idx] = 0xff000000 | rgb;
                            glow[idx] = q.glow() ? 0xff000000 | rgb : 0;
                        }
                    }
                }
            }
            return new Frame(downsample(color, w, h, ss), downsample(glow, w, h, ss));
        }
    }

    /** Yaw around y, then pitch around x, as the mod's icons. */
    record View(double cyaw, double syaw, double cp, double sp) {
        View(double yaw, double pitch) {
            this(Math.cos(Math.toRadians(yaw)), Math.sin(Math.toRadians(yaw)), Math.cos(Math.toRadians(pitch)), Math.sin(Math.toRadians(pitch)));
        }

        double[] apply(double[] p) {
            double vx = p[0] * cyaw - p[2] * syaw;
            double vz = p[0] * syaw + p[2] * cyaw;
            return new double[]{vx, p[1] * cp - vz * sp, p[1] * sp + vz * cp};
        }
    }

    static int mul(int rgb, double[] f) {
        int r = (int) Math.min(255, Math.round(((rgb >> 16) & 0xff) * f[0]));
        int g = (int) Math.min(255, Math.round(((rgb >> 8) & 0xff) * f[1]));
        int b = (int) Math.min(255, Math.round((rgb & 0xff) * f[2]));
        return (r << 16) | (g << 8) | b;
    }

    /** Averages ss x ss samples into each pixel, weighting colour by coverage. */
    static BufferedImage downsample(int[] color, int w, int h, int ss) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int sw = w * ss;
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double r = 0, g = 0, b = 0, a = 0;
                for (int dy = 0; dy < ss; dy++) {
                    for (int dx = 0; dx < ss; dx++) {
                        int c = color[(y * ss + dy) * sw + x * ss + dx];
                        double ca = (c >>> 24) / 255.0;
                        r += ((c >> 16) & 0xff) * ca;
                        g += ((c >> 8) & 0xff) * ca;
                        b += (c & 0xff) * ca;
                        a += ca;
                    }
                }
                row[x] = a <= 0 ? 0 : ((int) Math.round(a / (ss * ss) * 255) << 24)
                        | ((int) Math.round(r / a) << 16) | ((int) Math.round(g / a) << 8) | (int) Math.round(b / a);
            }
            img.setRGB(0, y, w, 1, row, 0, w);
        }
        return img;
    }

    /** Fits a scene into a square of {@code size} pixels with {@code margin} free around it, without glow. */
    static BufferedImage square(Scene s, double yaw, double pitch, int size, int margin) {
        Rect b = s.bounds(yaw, pitch);
        double side = Math.max(b.width(), b.height());
        double cx = (b.minX() + b.maxX()) / 2, cy = (b.minY() + b.maxY()) / 2;
        Rect frame = new Rect(cx - side / 2, cy - side / 2, cx + side / 2, cy + side / 2);
        double scale = (size - 2.0 * margin) / side;
        Frame f = s.render(yaw, pitch, frame, scale, margin, 1, null);
        return crop(f.image(), size, size);
    }

    static BufferedImage crop(BufferedImage img, int w, int h) {
        BufferedImage c = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        c.getGraphics().drawImage(img, 0, 0, null);
        return c;
    }

    /**
     * The picture over its own glow: the glowing parts, blurred by about {@code radius} pixels and
     * scaled by {@code strength}, go beneath the picture, and a share {@code over} of it on top.
     */
    static BufferedImage bloom(Frame f, double radius, double strength, double over) {
        BufferedImage img = f.image(), glow = f.glow();
        int w = img.getWidth(), h = img.getHeight(), n = w * h;
        int[] gp = glow.getRGB(0, 0, w, h, null, 0, w);
        int[] ip = img.getRGB(0, 0, w, h, null, 0, w);
        float[][] g = new float[4][n];
        boolean any = false;
        for (int i = 0; i < n; i++) {
            float a = (gp[i] >>> 24) / 255f;
            any |= a > 0;
            g[0][i] = ((gp[i] >> 16) & 0xff) / 255f * a;
            g[1][i] = ((gp[i] >> 8) & 0xff) / 255f * a;
            g[2][i] = (gp[i] & 0xff) / 255f * a;
            g[3][i] = a;
        }
        if (!any) {
            return img;
        }
        // three box blurs make a soft, nearly gaussian glow
        int box = Math.max(1, (int) Math.round(radius / 1.8));
        for (float[] channel : g) {
            for (int pass = 0; pass < 3; pass++) {
                blur(channel, w, h, box, true);
                blur(channel, w, h, box, false);
            }
        }
        int[] outPx = new int[n];
        for (int i = 0; i < n; i++) {
            float ia = (ip[i] >>> 24) / 255f;
            float ir = ((ip[i] >> 16) & 0xff) / 255f * ia, ig = ((ip[i] >> 8) & 0xff) / 255f * ia, ib = (ip[i] & 0xff) / 255f * ia;
            float ga = Math.min(1f, (float) (g[3][i] * strength));
            float gr = Math.min(ga, (float) (g[0][i] * strength)), gg = Math.min(ga, (float) (g[1][i] * strength)), gb = Math.min(ga, (float) (g[2][i] * strength));
            // picture over glow
            float oa = ia + ga * (1 - ia);
            float or = ir + gr * (1 - ia), og = ig + gg * (1 - ia), ob = ib + gb * (1 - ia);
            // a little of the glow on top, as light spilling over
            float k = (float) over;
            or = Math.min(oa, or + gr * k * (1 - or));
            og = Math.min(oa, og + gg * k * (1 - og));
            ob = Math.min(oa, ob + gb * k * (1 - ob));
            if (oa <= 0.002f) {
                outPx[i] = 0;
                continue;
            }
            int a8 = Math.round(oa * 255);
            outPx[i] = (a8 << 24) | (Math.round(Math.min(1, or / oa) * 255) << 16) | (Math.round(Math.min(1, og / oa) * 255) << 8) | Math.round(Math.min(1, ob / oa) * 255);
        }
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, w, h, outPx, 0, w);
        return result;
    }

    /** One box blur pass along rows or columns (running sum, edges count as empty). */
    static void blur(float[] c, int w, int h, int r, boolean rows) {
        int len = rows ? w : h, lines = rows ? h : w;
        float[] line = new float[len];
        float norm = 1f / (2 * r + 1);
        for (int l = 0; l < lines; l++) {
            for (int i = 0; i < len; i++) {
                line[i] = c[rows ? l * w + i : i * w + l];
            }
            float sum = 0;
            for (int i = -r; i <= r; i++) {
                if (i >= 0 && i < len) {
                    sum += line[i];
                }
            }
            for (int i = 0; i < len; i++) {
                c[rows ? l * w + i : i * w + l] = sum * norm;
                int add = i + r + 1, sub = i - r;
                if (add < len) {
                    sum += line[add];
                }
                if (sub >= 0) {
                    sum -= line[sub];
                }
            }
        }
    }
}
