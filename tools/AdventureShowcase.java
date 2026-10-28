import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * Renders the pictures of Wren Hollow, the story adventure of FlexiisGoggles 0.2.0, with the look of {@link Showcase}: its
 * buildings as the mod ships them (the prefabs, read and resolved into textured blocks by the mod's {@code PrefabPreview}) and
 * its people (dressed and posed by the mod's {@code Taken.figure}), from a checkout of the mod where that is built.
 *
 * <pre>
 * javac -d build &lt;mod&gt;/dev/art/*.java tools/Showcase.java tools/MirrorShowcase.java tools/AdventureShowcase.java
 * cd &lt;mod checkout&gt; &amp;&amp; java -cp build AdventureShowcase &lt;mod checkout&gt; &lt;Assets.zip&gt; &lt;out dir&gt; [only...]
 * </pre>
 *
 * It runs from the checkout because the mod's tools find its files relative to it.
 */
public final class AdventureShowcase {
    private static PrefabPreview game;
    private static Bunker.Assets assets;
    private static Path prefabs;

    /** The people of the valley, left to right in the lineup (their model assets): Ruth Hale, Milo, Ada, Pike, Holt. */
    static final String[] CAST = {"Flexii_Ranger_Hale", "Flexii_Milo", "Flexii_Survivor_Ada", "Flexii_Survivor_Pike", "Flexii_Survivor_Holt"};

    /** The letters lit on the Varros' wall: the first word Milo spells. */
    static final String LIT = "HELP";

    /**
     * The grass's tint. The preview stands one green ({@code BIOME}) in for every biome's tint; here grass and plants take
     * {@code GRASS} instead, which turns the game's grey grass into the green of its sunny grass (the land in the mirror's
     * pictures).
     */
    static final int BIOME = 0x6f8f3f, GRASS = 0x3c5418;

    private AdventureShowcase() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: AdventureShowcase <mod checkout> <Assets.zip> <out dir> [only...]");
            System.exit(2);
        }
        Path resources = Path.of(args[0], "src/main/resources");
        Showcase.common = resources.resolve("Common");
        Showcase.game = new ZipFile(args[1]);
        Showcase.out = Path.of(args[2]);
        Files.createDirectories(Showcase.out);
        prefabs = resources.resolve("Server/Prefabs/FlexiisGoggles");
        game = new PrefabPreview(Showcase.game, List.of(resources));
        assets = new Bunker.Assets(Showcase.game);
        List<String> only = Arrays.asList(args).subList(3, args.length);
        Map<String, Showcase.Job> jobs = new LinkedHashMap<>();
        jobs.put("house", AdventureShowcase::house);
        jobs.put("lights", AdventureShowcase::lights);
        jobs.put("cabin", AdventureShowcase::cabin);
        jobs.put("fort", AdventureShowcase::fort);
        jobs.put("cast", AdventureShowcase::cast);
        for (Map.Entry<String, Showcase.Job> job : jobs.entrySet()) {
            if (only.isEmpty() || only.contains(job.getKey())) {
                job.getValue().run();
                System.out.println("  " + job.getKey());
            }
        }
        System.out.println("pictures written to " + Showcase.out);
    }

    // ---------------------------------------------------------------- the pictures

    /** The Varro house from the front left, with its yard, porch and fence, the cellar under it showing below the ground. */
    static void house() throws IOException {
        Showcase.Scene s = scene(blocks("Adventure/Varro_House.prefab.json", "", List.of()), null);
        write(s, -32, 22, 960, 20, 1, 10, "varro-house");
    }

    /**
     * The living room of the Varro house, opened like a doll's house: the wall of letter lights, the couch facing it, the rug,
     * the floor lamp and the radio; the hall's wall, the front wall and the ceiling taken away, and with it the lantern hanging
     * from it. {@link #LIT} is lit.
     */
    static void lights() throws IOException {
        // the living room: x -8 (the west wall) to -3, z -15 (the back wall) to -6, the ground floor's boards (y 0) to y 4
        Showcase.Scene s = scene(blocks("Adventure/Varro_House.prefab.json", LIT, List.of("Deco_Lantern_Ceiling")), "x>-3&z>-6&y>4&y<0");
        write(s, 70, 26, 640, 14, 0.8, 9, "varro-lights");
    }

    /** Ruth Hale's cabin in its clearing: the porch, the chimney, the woodpile, the tripwires across the path. */
    static void cabin() throws IOException {
        Showcase.Scene s = scene(blocks("Cabin/Ranger_Cabin.prefab.json", "", List.of()), null);
        write(s, -30, 24, 640, 14, 1, 7, "ranger-cabin");
    }

    /**
     * Milo's fort against the dead tree it comes with. Its prefab brings only its own floor, the path and leaf litter, for the
     * wood's ground it is set into; here that ground is a slab under it, a block wider all round.
     */
    static void fort() throws IOException {
        Showcase.Scene s = scene(grounded(blocks("Milos_Fort.prefab.json", "", List.of()), 1), null);
        write(s, -30, 20, 640, 14, 1, 7, "milos-fort");
    }

    /**
     * Ruth Hale, Milo, Ada, Pike and Holt side by side, at rest, each turned a little and standing in the middle of a fifth of
     * the picture, so names can be set under them.
     */
    static void cast() throws IOException {
        double spacing = 1.25, turn = -20, pitch = 6;
        Showcase.Scene s = new Showcase.Scene();
        for (int i = 0; i < CAST.length; i++) {
            double x = (i - (CAST.length - 1) / 2.0) * spacing;
            for (Bunker.Quad q : Taken.figure(assets, CAST[i], null, 0, new double[]{x, 0, 0}, turn)) {
                s.face(q.p(), q.uv(), q.tex(), q.glow());
            }
        }
        // seen straight from the front, the frame exactly as wide as the five columns
        Showcase.Rect b = s.bounds(0, pitch);
        double half = CAST.length * spacing / 2, margin = 0.15;
        Showcase.Rect frame = new Showcase.Rect(-half, b.minY() - margin, half, b.maxY() + margin);
        Showcase.Frame f = s.render(0, pitch, frame, 1440 / frame.width(), 0, 1, null);
        Showcase.write(Showcase.bloom(f, 8, 1.2, 0.3), "cast");
    }

    // ---------------------------------------------------------------- scenes

    /**
     * A prefab's blocks, markers and those whose names start with one of {@code without} left out. The rift lights of the
     * letters in {@code lit} are switched on (their state {@code On}).
     */
    static List<PrefabPreview.Block> blocks(String path, String lit, List<String> without) throws IOException {
        List<PrefabPreview.Block> blocks = new ArrayList<>();
        for (PrefabPreview.Block b : PrefabPreview.read(prefabs.resolve(path))) {
            String n = b.name(), id = n.replaceFirst("^\\*", "");
            if (id.startsWith("Flexii_Marker_") || without.stream().anyMatch(id::startsWith)) {
                continue;
            }
            if (n.matches("Flexii_Rift_Light_[A-Z]") && lit.indexOf(n.charAt(n.length() - 1)) >= 0) {
                n = "*" + n + "_State_Definitions_On";
            }
            blocks.add(new PrefabPreview.Block(b.x(), b.y(), b.z(), n, b.rotation(), b.filler()));
        }
        return blocks;
    }

    /**
     * The blocks on a slab of ground under everything they set, {@code margin} wider all round: grass at y -1 (the world's
     * ground under a prefab's anchor), dirt and stone below. The blocks win where they set the slab's cells themselves.
     */
    static List<PrefabPreview.Block> grounded(List<PrefabPreview.Block> blocks, int margin) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        Set<String> taken = new HashSet<>();
        for (PrefabPreview.Block b : blocks) {
            if (!b.name().equals("Empty")) {
                minX = Math.min(minX, b.x());
                maxX = Math.max(maxX, b.x());
                minZ = Math.min(minZ, b.z());
                maxZ = Math.max(maxZ, b.z());
            }
            taken.add(b.x() + "," + b.y() + "," + b.z());
        }
        List<PrefabPreview.Block> out = new ArrayList<>(blocks);
        String[] layers = {"Soil_Grass", "Soil_Dirt", "Rock_Stone"};
        for (int x = minX - margin; x <= maxX + margin; x++) {
            for (int z = minZ - margin; z <= maxZ + margin; z++) {
                for (int i = 0; i < layers.length; i++) {
                    if (!taken.contains(x + "," + (-1 - i) + "," + z)) {
                        out.add(new PrefabPreview.Block(x, -1 - i, z, layers[i], 0, 0));
                    }
                }
            }
        }
        return out;
    }

    /**
     * Blocks as one scene in blocks, resolved by the mod's preview (cubes with the game's textures, model blocks turned as the
     * server turns them, tints as it tints them, the grass's as {@link #GRASS}); {@code cut} takes blocks away as the preview's
     * cuts do.
     */
    static Showcase.Scene scene(List<PrefabPreview.Block> blocks, String cut) {
        PrefabPreview.Grid g = game.grid(blocks, false);
        Showcase.Scene s = new Showcase.Scene();
        Map<Integer, BufferedImage> colours = new HashMap<>();
        Map<BufferedImage, Map<Integer, BufferedImage>> tinted = new IdentityHashMap<>();
        for (PrefabPreview.Quad q : game.quads(g, cut)) {
            BufferedImage tex = q.tex();
            double[][] uv = q.uv();
            if (tex == null) {
                tex = colours.computeIfAbsent(q.solid() | 0xff000000, c -> {
                    BufferedImage one = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                    one.setRGB(0, 0, c);
                    return one;
                });
                uv = new double[][]{{0, 0}, {1, 0}, {1, 1}, {0, 1}};
            } else if (q.tint() != -1) {
                BufferedImage plain = tex;
                int tint = q.tint() == BIOME ? GRASS : q.tint();
                tex = tinted.computeIfAbsent(plain, k -> new HashMap<>()).computeIfAbsent(tint, k -> tinted(plain, tint));
            }
            s.face(q.p(), uv, tex, q.glow());
        }
        return s;
    }

    /** A texture multiplied by a block's tint, as the preview multiplies it. */
    static BufferedImage tinted(BufferedImage tex, int tint) {
        BufferedImage out = new BufferedImage(tex.getWidth(), tex.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < tex.getHeight(); y++) {
            for (int x = 0; x < tex.getWidth(); x++) {
                int argb = tex.getRGB(x, y);
                out.setRGB(x, y, (argb & 0xff000000) | PrefabPreview.multiply(argb & 0xffffff, tint & 0xffffff));
            }
        }
        return out;
    }

    /** A scene seen from {@code yaw}, {@code pitch}, {@code width} pixels wide with {@code pad} free round it, with a glow. */
    static void write(Showcase.Scene s, double yaw, double pitch, int width, int pad, double light, double glow, String name) throws IOException {
        Showcase.Rect frame = s.bounds(yaw, pitch);
        Showcase.Frame f = s.render(yaw, pitch, frame, (width - 2.0 * pad) / frame.width(), pad, light, null);
        Showcase.write(Showcase.bloom(f, glow, 1.2, 0.3), name);
    }
}
