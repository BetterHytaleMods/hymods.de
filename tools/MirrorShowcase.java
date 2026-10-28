import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * Renders the pictures of what FlexiisGoggles 0.2.0 brings (the mirror dimension, its creatures and gear, the blight's
 * Hollow King, the rift companion) with the look of {@link Showcase}, from a checkout of the mod where that is built.
 * The creatures are dressed and posed by the mod's own tools ({@code dev/art}: {@code Bunker.Assets} reads the model
 * assets, {@code Taken.figure} puts one together as the game does and poses it by one of its animations).
 *
 * <pre>
 * javac -d build &lt;mod&gt;/dev/art/*.java tools/Showcase.java tools/MirrorShowcase.java
 * cd &lt;mod checkout&gt; &amp;&amp; java -cp build MirrorShowcase &lt;mod checkout&gt; &lt;Assets.zip&gt; &lt;out dir&gt; [only...]
 * </pre>
 *
 * It runs from the checkout because the mod's tools find its files relative to it, and reads the game from
 * {@code HYTALE_ASSETS} when that is set.
 */
public final class MirrorShowcase {
    private static Bunker.Assets assets;

    private static final String GOGGLES = "Items/FlexiisGoggles/";
    private static final String BLOCKS = "Blocks/FlexiisGoggles/";
    private static final String RESOURCES = "Resources/FlexiisGoggles/";
    private static final String NEST = BLOCKS + "Nest/";
    private static final String MIRROR = "BlockTextures/Flexii_Mirror/";
    private static final String FLESH = "BlockTextures/FlexiisGoggles/Rift_Flesh_";

    /**
     * The creatures' cards: file name, model asset, animation set (null: at rest), frame, and how far the view turns. The
     * poses are the ones the mod's own photos of them use. The page's names are newer than some of the mod's ids: the
     * Hollowed is {@code Flexii_Flayed}, the riftling {@code Flexii_Pollywog}.
     */
    static final Object[][] CREATURES = {
            {"rift-hound", "Flexii_Rift_Hound", "Alerted", 21.0, -30.0},
            {"hollowed", "Flexii_Flayed", "Alerted", 24.0, -25.0},
            {"spore-wraith", "Flexii_Spore_Wraith", "Alerted", 42.0, -25.0},
            {"vine-lurker", "Flexii_Vine_Lurker", "Spawn", 44.0, -25.0},
            {"rift-bat", "Flexii_Rift_Bat", "HurtFly", 21.0, -25.0},
            {"bloomjaw", "Flexii_Bloomjaw", "Alerted", 32.0, -25.0},
            {"flesh-hulk", "Flexii_Flesh_Hulk", "Alerted", 21.0, -25.0},
            {"rift-mite", "Flexii_Rift_Mite", "Threaten", 14.0, -30.0},
            {"rift-eel", "Flexii_Rift_Eel", "Alerted", 30.0, -35.0},
            {"rift-leech", "Flexii_Rift_Leech", "Swim", 10.0, -35.0},
            {"unseen", "Flexii_Unseen", "Idle", 0.0, -20.0},
            {"hive-eye", "Flexii_Hive_Eye", "Watch", 10.0, -25.0}};

    private MirrorShowcase() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: MirrorShowcase <mod checkout> <Assets.zip> <out dir> [only...]");
            System.exit(2);
        }
        Showcase.common = Path.of(args[0], "src/main/resources/Common");
        Showcase.game = new ZipFile(args[1]);
        Showcase.out = Path.of(args[2]);
        Files.createDirectories(Showcase.out);
        assets = new Bunker.Assets(Showcase.game);
        List<String> only = Arrays.asList(args).subList(3, args.length);
        Map<String, Showcase.Job> jobs = new LinkedHashMap<>();
        jobs.put("mirror", MirrorShowcase::mirror);
        jobs.put("creatures", MirrorShowcase::creatures);
        jobs.put("nest", MirrorShowcase::nest);
        jobs.put("crimsonite", MirrorShowcase::crimsonite);
        jobs.put("gear", MirrorShowcase::gear);
        jobs.put("items", MirrorShowcase::items);
        jobs.put("king", MirrorShowcase::king);
        jobs.put("companion", MirrorShowcase::companion);
        for (Map.Entry<String, Showcase.Job> job : jobs.entrySet()) {
            if (only.isEmpty() || only.contains(job.getKey())) {
                job.getValue().run();
                System.out.println("  " + job.getKey());
            }
        }
        System.out.println("pictures written to " + Showcase.out);
    }

    // ---------------------------------------------------------------- the pictures

    /**
     * The same patch of land twice: as the overworld has it, by day, and as the mirror has it, its rotten twin at night
     * (the mod's own copies of the game's textures).
     */
    static void mirror() throws IOException {
        double yaw = 35, pitch = 28;
        Showcase.Scene day = land("BlockTextures/", false);
        Showcase.Scene night = land(MIRROR, true);
        Showcase.Rect frame = day.bounds(yaw, pitch);
        double scale = 740 / frame.width();
        Showcase.write(Showcase.bloom(day.render(yaw, pitch, frame, scale, 20, 1, null), 12, 1.2, 0.3), "mirror-overworld");
        Showcase.write(Showcase.bloom(night.render(yaw, pitch, frame, scale, 20, 0.8, new double[]{1.0, 0.82, 0.9}), 12, 1.2, 0.3), "mirror-rotten");
    }

    /** Every creature on a card of its own, each as large as the card allows. */
    static void creatures() throws IOException {
        for (Object[] c : CREATURES) {
            Showcase.Scene s = figure((String) c[1], (String) c[2], (double) c[3], 0);
            Showcase.write(fit(s, (double) c[4], 10, 400, 400, 18, 1.15, 10), "creature-" + c[0]);
        }
    }

    /** A rift nest: a mound of flesh, its heart on top, pods and tendrils round it and a brood sac at its foot. */
    static void nest() throws IOException {
        Showcase.Scene s = new Showcase.Scene();
        Random rnd = new Random(5);
        // the top block of each column from x = -2 (first row) to 2 and z = -2 to 2; -1: none
        int[][] heights = {
                {-1, 0, 0, 0, -1},
                {0, 0, 1, 0, 0},
                {0, 1, 1, 1, 0},
                {0, 0, 1, 0, 0},
                {-1, 0, 0, 0, -1}};
        Set<String> solid = new HashSet<>();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                for (int y = 0; y <= heights[x][z]; y++) {
                    solid.add((x - 2) + "," + y + "," + (z - 2));
                }
            }
        }
        for (String key : solid) {
            String[] p = key.split(",");
            flesh(s, solid, Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), rnd);
        }
        block(s, NEST + "Rift_Heart", 0, 2, 0, 0);
        block(s, NEST + "Rift_Pod", 2, 1, 1, 30);
        block(s, NEST + "Rift_Pod", -1, 1, 1, -20);
        block(s, NEST + "Rift_Tendril", 0, 2, -1, 0);
        block(s, NEST + "Rift_Tendril", -2, 1, 0, 60);
        block(s, NEST + "Rift_Tendril", 1, 1, -2, 120);
        block(s, NEST + "Brood_Sac", 1, 1, 2, 0);
        double yaw = 35, pitch = 24;
        Showcase.Rect frame = s.bounds(yaw, pitch);
        Showcase.write(Showcase.bloom(s.render(yaw, pitch, frame, 900 / frame.width(), 30, 1, null), 16, 1.4, 0.35), "nest");
    }

    /** Crimsonite ore in the mirror's stone, its crystals glowing as night vision shows them; at the size of the other ores. */
    static void crimsonite() throws IOException {
        double yaw = 35, pitch = 25;
        Showcase.Scene s = new Showcase.Scene().add(Showcase.cube(), Showcase.texture(MIRROR + "Rock_Stone.png"));
        glowing(s, Showcase.model(RESOURCES + "Ore_Crimsonite.blockymodel"), Showcase.texture(RESOURCES + "Ore_Crimsonite_Texture.png"));
        // the frame the ores of 0.1 are drawn in: one stone block with its outcrops
        Showcase.Scene quartz = new Showcase.Scene().add(Showcase.cube(), Showcase.texture(MIRROR + "Rock_Stone.png"))
                .add(Showcase.model(RESOURCES + "Ore_Quartz.blockymodel"), Showcase.texture(RESOURCES + "Ore_Quartz_Texture.png"));
        Showcase.Rect frame = Showcase.union(List.of(quartz, s), yaw, pitch);
        Showcase.write(Showcase.bloom(s.render(yaw, pitch, frame, 420 / frame.width(), 20, 1, null), 12, 1.3, 0.3), "ore-crimsonite");
    }

    /** The Laser and the Thermal Goggles: worn, as icons; and the bench's sixth tier. */
    static void gear() throws IOException {
        String[][] goggles = {{"Laser", "laser"}, {"Thermal", "thermal"}};
        for (String[] g : goggles) {
            Showcase.Scene s = new Showcase.Scene().add(Showcase.front(Showcase.model(GOGGLES + "Goggles_" + g[0] + ".blockymodel")),
                    Showcase.texture(GOGGLES + "Goggles_" + g[0] + "_Texture.png"));
            Showcase.write(Showcase.square(s, 28, 14, 128, 6), "icon-goggles-" + g[1]);
        }
        double yaw = 32, pitch = 10;
        List<Showcase.Scene> heads = new ArrayList<>();
        for (String name : List.of("Laser", "Thermal", "Ultra")) {
            heads.add(Showcase.head().add(Showcase.model(GOGGLES + "Goggles_" + name + ".blockymodel"), Showcase.texture(GOGGLES + "Goggles_" + name + "_Texture.png")));
        }
        Showcase.Rect frame = Showcase.union(heads, yaw, pitch);
        for (int i = 0; i < goggles.length; i++) {
            Showcase.write(Showcase.bloom(heads.get(i).render(yaw, pitch, frame, 440 / frame.width(), 20, 1, null), 10, 1.1, 0.25), "worn-" + goggles[i][1]);
        }
        // twice as large for the laser, which stands on its own on the page
        Showcase.write(Showcase.bloom(heads.get(0).render(yaw, pitch, frame, 880 / frame.width(), 40, 1, null), 20, 1.1, 0.25), "worn-laser-960");
        // the sixth tier at the scale of the other five
        double byaw = -35, bpitch = 25;
        List<Showcase.Scene> benches = new ArrayList<>();
        for (int tier = 1; tier <= 6; tier++) {
            benches.add(new Showcase.Scene().add(Showcase.model(BLOCKS + "Optician_Bench_Tier" + tier + ".blockymodel"),
                    Showcase.texture(BLOCKS + "Optician_Bench_Tier" + tier + "_Texture.png")));
        }
        Showcase.Rect bframe = Showcase.union(benches, byaw, bpitch);
        Showcase.write(Showcase.bloom(benches.get(5).render(byaw, bpitch, bframe, 460 / bframe.width(), 16, 1, null), 9, 1.0, 0.25), "bench-tier6");
    }

    /** The new materials and items, each on its own, as the materials of 0.1 are drawn. */
    static void items() throws IOException {
        String[][] items = {
                {RESOURCES + "Crimsonite", "crimsonite", "30", "15"}, {RESOURCES + "Rift_Ichor", "rift-ichor", "30", "15"},
                {RESOURCES + "Bloomjaw_Fang", "bloomjaw-fang", "30", "15"}, {GOGGLES + "Rift_Compass", "rift-compass", "25", "35"}};
        for (String[] it : items) {
            Showcase.Scene s = new Showcase.Scene().add(Showcase.model(it[0] + ".blockymodel"), Showcase.texture(it[0] + "_Texture.png"));
            double yaw = Double.parseDouble(it[2]), pitch = Double.parseDouble(it[3]);
            Showcase.Rect frame = s.bounds(yaw, pitch);
            Showcase.Frame f = s.render(yaw, pitch, frame, 168 / Math.max(frame.width(), frame.height()), 12, 1, null);
            Showcase.write(Showcase.bloom(f, 6, 1.0, 0.25), "item-" + it[1]);
        }
    }

    /** The Hollow King, large; a root heart that binds him; the manor's clock and music box; his heart, on a card like theirs. */
    static void king() throws IOException {
        Showcase.Scene king = figure("Flexii_Hollow_King", "SporeBreath", 30, 0);
        Showcase.write(fit(king, -25, 8, 1040, 1040, 30, 1.2, 22), "hollow-king");
        Showcase.write(fit(figure("Flexii_Root_Heart", "Idle", 0, 0), -25, 12, 400, 400, 18, 1.15, 10), "root-heart");
        for (String[] b : new String[][]{{"Manor/Grandfather_Clock", "grandfather-clock"}, {"Manor/Music_Box", "music-box"}}) {
            Showcase.Scene s = new Showcase.Scene().add(Showcase.model(BLOCKS + b[0] + ".blockymodel"), Showcase.texture(BLOCKS + b[0] + "_Texture.png"));
            Showcase.write(fit(s, -30, 18, 400, 400, 18, 1.0, 9), b[1]);
        }
        Showcase.write(fit(item(RESOURCES + "Hollow_Heart"), 30, 15, 400, 400, 48, 1.0, 9), "hollow-heart");
    }

    /** The companion as it grows: its egg, the riftling, the rift pup, the rift companion, each on a card of its own. */
    static void companion() throws IOException {
        Showcase.write(fit(item(GOGGLES + "Rift_Egg"), 30, 15, 400, 400, 80, 1.0, 9), "rift-egg");
        Showcase.write(fit(figure("Flexii_Pollywog", "Idle", 0, 0), -30, 14, 400, 400, 18, 1.15, 10), "companion-riftling");
        Showcase.write(fit(figure("Flexii_Rift_Pup", null, 0, 0), -30, 10, 400, 400, 18, 1.15, 10), "companion-rift-pup");
        Showcase.write(fit(figure("Flexii_Rift_Companion", null, 0, 0), -30, 10, 400, 400, 18, 1.15, 10), "companion-rift-companion");
    }

    // ---------------------------------------------------------------- scenes

    /**
     * A patch of land cut out of the ground, five by four blocks: grass over dirt over stone, a little uneven, an oak trunk
     * standing on it. {@code dir} is where the textures are: the game's or the mirror's twins of them.
     */
    static Showcase.Scene land(String dir, boolean mirror) {
        int[][] top = {
                {2, 2, 3, 3},
                {2, 2, 2, 3},
                {1, 2, 2, 2},
                {1, 1, 2, 2},
                {1, 1, 1, 2}};
        Set<String> solid = new HashSet<>();
        for (int x = 0; x < top.length; x++) {
            for (int z = 0; z < top[x].length; z++) {
                for (int y = 0; y <= top[x][z]; y++) {
                    solid.add(x + "," + y + "," + z);
                }
            }
        }
        Showcase.Scene s = new Showcase.Scene();
        Random rnd = new Random(3);
        for (int x = 0; x < top.length; x++) {
            for (int z = 0; z < top[x].length; z++) {
                for (int y = 0; y <= top[x][z]; y++) {
                    int[] b = {x, y, z};
                    List<String> faces = Showcase.exposed(solid, b);
                    if (faces.isEmpty()) {
                        continue;
                    }
                    double dx = x * 32, dy = y * 32, dz = z * 32;
                    if (y == top[x][z]) {
                        sides(s, faces, "top", texture(dir, "Soil_Grass_Sunny.png", mirror), texture(dir, "Soil_Grass_Sunny_Side.png", mirror), dx, dy, dz);
                    } else if (y >= top[x][z] - 1) {
                        s.add(Showcase.cube(faces), texture(dir, "Soil_Dirt.png", mirror), dx, dy, dz);
                    } else {
                        s.add(Showcase.cube(faces), texture(dir, rnd.nextBoolean() ? "Rock_Stone.png" : "Rock_Stone_2.png", mirror), dx, dy, dz);
                    }
                }
            }
        }
        // a trunk of three on the high ground at the back
        for (int y = 4; y <= 6; y++) {
            List<String> faces = new ArrayList<>(List.of("front", "back", "left", "right"));
            if (y == 6) {
                faces.add("top");
            }
            sides(s, faces, "top", texture(dir, "Wood_Trunk_Oak_Top.png", mirror), texture(dir, "Wood_Trunk_Oak_Side.png", mirror), 0, y * 32, 3 * 32);
        }
        return s;
    }

    static BufferedImage texture(String dir, String name, boolean mirror) {
        return mirror ? Showcase.texture(dir + name) : Showcase.gameTexture(dir + name);
    }

    /** A block whose {@code special} face has a texture of its own and every other given face {@code rest}. */
    static void sides(Showcase.Scene s, List<String> faces, String special, BufferedImage own, BufferedImage rest, double dx, double dy, double dz) {
        List<String> others = new ArrayList<>(faces);
        if (others.remove(special)) {
            s.add(Showcase.cube(List.of(special)), own, dx, dy, dz);
        }
        if (!others.isEmpty()) {
            s.add(Showcase.cube(others), rest, dx, dy, dz);
        }
    }

    /** One block of the hive's flesh in a mound, in blocks, its faces that show, top and sides each one of three looks. */
    static void flesh(Showcase.Scene s, Set<String> solid, int x, int y, int z, Random rnd) {
        List<String> faces = Showcase.exposed(solid, new int[]{x, y, z});
        if (faces.isEmpty()) {
            return;
        }
        String[] looks = {"", "_2", "_3"};
        BufferedImage top = Showcase.texture(FLESH + "Top" + looks[rnd.nextInt(3)] + ".png");
        BufferedImage side = Showcase.texture(FLESH + "Side" + looks[rnd.nextInt(3)] + ".png");
        Showcase.Scene block = new Showcase.Scene();
        sides(block, faces, "top", top, side, 0, 0, 0);
        place(s, block, x, y, z, 0);
    }

    /** A block model (32 units a block, its origin at the middle of the block's floor) standing in the cell (x, y, z), turned. */
    static void block(Showcase.Scene s, String path, int x, int y, int z, double turn) {
        Showcase.Scene b = new Showcase.Scene().add(Showcase.model(path + ".blockymodel"), Showcase.texture(path + "_Texture.png"));
        place(s, b, x, y, z, turn);
    }

    /** Copies a scene in model units (32 a block) into one in blocks, into the cell (x, y, z), turned about its upright. */
    static void place(Showcase.Scene into, Showcase.Scene from, int x, int y, int z, double turn) {
        double c = Math.cos(Math.toRadians(turn)), sn = Math.sin(Math.toRadians(turn));
        for (Showcase.Quad q : from.quads) {
            double[][] p = new double[4][];
            for (int i = 0; i < 4; i++) {
                double px = q.p()[i][0] / 32, py = q.p()[i][1] / 32, pz = q.p()[i][2] / 32;
                p[i] = new double[]{x + 0.5 + px * c + pz * sn, y + py, z + 0.5 - px * sn + pz * c};
            }
            into.face(p, q.uv(), from.atlas(), q.glow());
        }
    }

    /** An item's model with its texture ({@code path} without the endings). */
    static Showcase.Scene item(String path) {
        return new Showcase.Scene().add(Showcase.model(path + ".blockymodel"), Showcase.texture(path + "_Texture.png"));
    }

    /** Adds a model whose every face glows. */
    static void glowing(Showcase.Scene s, Map<String, Object> model, BufferedImage texture) {
        for (Blocky.Face f : Blocky.faces(model)) {
            s.face(f.corners(), f.uv(), texture, true);
        }
    }

    /** A model asset posed by one of its animation sets at a frame (set null: at rest), as a scene in blocks. */
    static Showcase.Scene figure(String asset, String set, double frame, double yaw) throws IOException {
        Showcase.Scene s = new Showcase.Scene();
        for (Bunker.Quad q : Taken.figure(assets, asset, set == null ? null : animation(asset, set), frame, new double[]{0, 0, 0}, yaw)) {
            s.face(q.p(), q.uv(), q.tex(), q.glow());
        }
        return s;
    }

    /** The first animation of a model asset's animation set. */
    static Map<String, Object> animation(String asset, String set) throws IOException {
        Object found = Json.object(assets.asset(asset).get("AnimationSets")).get(set);
        if (found == null) {
            throw new IOException(asset + " has no animation set " + set);
        }
        Object first = Json.array(Json.object(found).get("Animations")).get(0);
        return Json.object(Json.parse(assets.text(String.valueOf(Json.object(first).get("Animation")))));
    }

    /**
     * A scene rendered into a canvas of {@code w} x {@code h} pixels, as large as fits with {@code pad} free round it,
     * centred, with a glow of about {@code glow} pixels.
     */
    static BufferedImage fit(Showcase.Scene s, double yaw, double pitch, int w, int h, int pad, double light, double glow) {
        Showcase.Rect b = s.bounds(yaw, pitch);
        double scale = Math.min((w - 2.0 * pad) / b.width(), (h - 2.0 * pad) / b.height());
        double cx = (b.minX() + b.maxX()) / 2, cy = (b.minY() + b.maxY()) / 2;
        double hw = (w - 2.0 * pad) / scale / 2, hh = (h - 2.0 * pad) / scale / 2;
        Showcase.Rect frame = new Showcase.Rect(cx - hw, cy - hh, cx + hw, cy + hh);
        Showcase.Frame f = s.render(yaw, pitch, frame, scale, pad, light, null);
        return Showcase.crop(Showcase.bloom(f, glow, 1.2, 0.3), w, h);
    }
}
