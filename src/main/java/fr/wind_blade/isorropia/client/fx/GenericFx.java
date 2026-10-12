package fr.wind_blade.isorropia.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Thaumcraft 6's FXGeneric, the particle every 1.12 Isorropia effect was assembled from: keyframed alpha and
 * scale spread evenly over the particle's life, a colour that drifts to a second colour, gravity, a slow-down
 * factor, Gaussian jitter, moon-phase wind and spin, and a frame run from the sprite sheet.
 *
 * <p>The sheet is the one Isorropia ships, which is the top-left sixteen cells of TC6's sheet. TC6 addressed it on
 * a 64-cell grid, so a TC6 frame maps here as {@code row * 16 + column}: its 320 is this sheet's 80 (the cyan
 * sparkle run) and its 512 is 128 (the soft wisp run). Layer 0 is drawn additively and layer 1 with ordinary
 * alpha blending, neither writing depth, as TC6's particle engine did.
 */
public class GenericFx extends SingleQuadParticle {
    public static final ResourceLocation SHEET = Isorropia.id("textures/misc/particles.png");
    /** TC6's sparkle and wisp runs, as frames of this sheet. */
    public static final int SPARKLE_FRAMES = 80;
    public static final int WISP_FRAMES = 128;
    private static final int GRID = 16;
    private static final float CELL = 1.0F / GRID;
    /** Half a texel in from each edge, so linear filtering never samples the neighbouring cell. */
    private static final float INSET = 0.5F / 256.0F;

    public static final ParticleRenderType ADDITIVE = sheet("additive", GlStateManager.DestFactor.ONE);
    public static final ParticleRenderType TRANSLUCENT = sheet("translucent", GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

    private boolean loop;
    private int startFrame;
    private int frameCount = 1;
    private int frameStep = 1;
    private float[] alphaKeys = {1.0F};
    private float[] scaleKeys = {1.0F};
    private float[] alphaFrames;
    private float[] scaleFrames;
    private float endRed;
    private float endGreen;
    private float endBlue;
    private double slowDown = 0.98F;
    private float randomX;
    private float randomY;
    private float randomZ;
    private double windX;
    private double windZ;
    private float rotationSpeed;
    private boolean translucent;
    private int delay;
    private int frame;

    public GenericFx(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);
        setSize(0.1F, 0.1F);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.0F;
    }

    public GenericFx lifetime(int ticks) {
        this.lifetime = ticks;
        return this;
    }

    public GenericFx colour(float red, float green, float blue) {
        return colours(red, green, blue, red, green, blue);
    }

    /** Starts at one colour and drifts linearly to the other over the particle's life. */
    public GenericFx colours(float red, float green, float blue, float toRed, float toGreen, float toBlue) {
        setColor(red, green, blue);
        this.endRed = toRed;
        this.endGreen = toGreen;
        this.endBlue = toBlue;
        return this;
    }

    public GenericFx alphas(float... keys) {
        this.alphaKeys = keys;
        this.alpha = keys[0];
        return this;
    }

    public GenericFx scales(float... keys) {
        this.scaleKeys = keys;
        this.quadSize = 0.1F * keys[0];
        return this;
    }

    public GenericFx frames(int start, int count, int step) {
        this.startFrame = start;
        this.frameCount = count;
        this.frameStep = step;
        this.frame = start;
        return this;
    }

    public GenericFx loop(boolean loop) {
        this.loop = loop;
        return this;
    }

    public GenericFx gravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    public GenericFx slowDown(double slowDown) {
        this.slowDown = slowDown;
        return this;
    }

    public GenericFx randomMovement(float x, float y, float z) {
        this.randomX = x;
        this.randomY = y;
        this.randomZ = z;
        return this;
    }

    /** TC6's wind: a push of a tenth of {@code strength}, turned by the moon phase. */
    public GenericFx wind(double strength) {
        int phase = level.getMoonPhase();
        double angle = phase * (40 + random.nextInt(10)) / 180.0 * Math.PI;
        this.windX = 0.1 * Math.cos(angle) * strength;
        this.windZ = -0.1 * Math.sin(angle) * strength;
        return this;
    }

    /** A starting turn as a fraction of a full circle, then a spin in degrees per tick, as TC6's two-argument form. */
    public GenericFx spin(float startTurns, float degreesPerTick) {
        this.roll = this.oRoll = (float) (startTurns * Math.PI * 2.0);
        this.rotationSpeed = degreesPerTick * Mth.DEG_TO_RAD;
        return this;
    }

    /** Layer 1 of TC6's engine: ordinary alpha blending instead of additive. */
    public GenericFx translucent() {
        this.translucent = true;
        return this;
    }

    /** Holds the particle back unseen for a few ticks, as TC6's addEffectWithDelay did. */
    public GenericFx delay(int ticks) {
        this.delay = ticks;
        return this;
    }

    @Override
    public void tick() {
        if (delay > 0) {
            delay--;
            return;
        }
        if (alphaFrames == null) {
            alphaFrames = spread(alphaKeys, lifetime);
            scaleFrames = spread(scaleKeys, lifetime);
        }
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        oRoll = roll;
        roll += (float) Math.PI * rotationSpeed * 2.0F;
        yd -= 0.04 * gravity;
        move(xd, yd, zd);
        xd *= slowDown;
        yd *= slowDown;
        zd *= slowDown;
        xd += random.nextGaussian() * randomX;
        yd += random.nextGaussian() * randomY;
        zd += random.nextGaussian() * randomZ;
        xd += windX;
        zd += windZ;
        if (onGround && slowDown != 1.0) {
            xd *= 0.7F;
            zd *= 0.7F;
        }
        frame = loop ? startFrame + age / frameStep % frameCount
                : (int) (startFrame + Math.min(frameCount * (age / (float) lifetime), frameCount - 1));
        alpha = alphaFrames[Math.min(age, alphaFrames.length - 1)];
        quadSize = 0.1F * scaleFrames[Math.min(age, scaleFrames.length - 1)];
    }

    /** TC6's calculateFrames: the keys laid out evenly from the first tick to the last. */
    private static float[] spread(float[] keys, int lifetime) {
        float[] frames = new float[lifetime + 1];
        float step = (keys.length - 1) / (float) lifetime;
        float at = 0.0F;
        for (int tick = 0; tick <= lifetime; tick++) {
            int index = Mth.floor(at);
            float next = index < keys.length - 1 ? keys[index + 1] - keys[index] : 0.0F;
            frames[tick] = keys[Math.min(index, keys.length - 1)] + next * (at - index);
            at += step;
        }
        return frames;
    }

    @Override
    public void render(VertexConsumer vertices, Camera camera, float partialTick) {
        if (delay > 0) {
            return;
        }
        // The colour drift is per frame in TC6, so it is applied here and put back afterwards.
        float red = rCol;
        float green = gCol;
        float blue = bCol;
        float life = Mth.clamp((age + partialTick) / lifetime, 0.0F, 1.0F);
        rCol = red + (endRed - red) * life;
        gCol = green + (endGreen - green) * life;
        bCol = blue + (endBlue - blue) * life;
        super.render(vertices, camera, partialTick);
        rCol = red;
        gCol = green;
        bCol = blue;
    }

    @Override
    protected float getU0() {
        return (frame % GRID) * CELL + INSET;
    }

    @Override
    protected float getU1() {
        return (frame % GRID + 1) * CELL - INSET;
    }

    @Override
    protected float getV0() {
        return (frame / GRID) * CELL + INSET;
    }

    @Override
    protected float getV1() {
        return (frame / GRID + 1) * CELL - INSET;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return translucent ? TRANSLUCENT : ADDITIVE;
    }

    private static ParticleRenderType sheet(String name, GlStateManager.DestFactor destination) {
        return new ParticleRenderType() {
            @Override
            public void begin(BufferBuilder builder, TextureManager textures) {
                RenderSystem.depthMask(false);
                RenderSystem.setShaderTexture(0, SHEET);
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, destination);
                RenderSystem.setShader(GameRenderer::getParticleShader);
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            @Override
            public void end(Tesselator tesselator) {
                tesselator.end();
                RenderSystem.depthMask(true);
                RenderSystem.defaultBlendFunc();
            }

            @Override
            public String toString() {
                return "isorropia:generic_" + name;
            }
        };
    }
}
