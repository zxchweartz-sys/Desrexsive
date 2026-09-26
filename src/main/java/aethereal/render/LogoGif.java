package aethereal.render;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Анимированный GIF-логотип Desrexsive.
 *
 * Кадры GIF декодируются в фоне (без GL), затем загружаются в видеокарту
 * на рендер-потоке одним пакетом. Пока кадры не готовы — готовившие места
 * рисуют статичный logo.png (fallback в самом виджете).
 *
 * Использование (на рендер-потоке):
 *   LogoGif.request(); // один раз, лучше всего при старте клиента
 *   if (LogoGif.ready()) { int id = LogoGif.currentGlId(); ...рисовать id... }
 */
public final class LogoGif {

    /** Максимальный размер стороны кадра после уменьшения (текстуры в HUD мелкие). */
    private static final int TARGET_SIZE = 128;

    private static final AtomicBoolean scheduled = new AtomicBoolean(false);

    /** Кадры, декодированные в фоне (null — ещё идёт декод; пустой массив — ошибка). */
    private static volatile BufferedImage[] decodedFrames;
    /** Задержки кадров, мс. */
    private static volatile int[] frameDelays;
    /** Кадры, загруженные в GPU (null — ещё не загружены). */
    private static volatile Frame[] uploadedFrames;
    private static volatile long startTime = System.currentTimeMillis();
    private static volatile int totalMs = 1;

    private static final class Frame {
        final int glId;
        final int delayMs;

        Frame(int glId, int delayMs) {
            this.glId = glId;
            this.delayMs = delayMs;
        }
    }

    private LogoGif() {
    }

    /** Начинает фоновую распаковку GIF. Безопасно вызывать в любом количестве. */
    public static void request() {
        if (scheduled.compareAndSet(false, true)) {
            Thread thread = new Thread(LogoGif::decode, "Desrexsive-LogoGif");
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Готовы ли кадры к отрисовке. Вызывать только на рендер-потоке —
     * первый вызов после декода загружает текстуры в GPU.
     */
    public static boolean ready() {
        BufferedImage[] frames = getDecoded();
        if (frames == null || frames.length == 0) {
            return false;
        }
        return uploadIfNeeded(frames);
    }

    /** GL-ид текущего кадра по времени, или -1 если лого не готово. Рендер-поток. */
    public static int currentGlId() {
        if (!ready()) {
            return -1;
        }
        Frame[] cached = uploadedFrames;
        if (cached == null || cached.length == 0) {
            return -1;
        }
        long elapsed = (System.currentTimeMillis() - startTime) % Math.max(1, totalMs);
        int accumulated = 0;
        for (Frame frame : cached) {
            accumulated += frame.delayMs;
            if (elapsed < accumulated) {
                return frame.glId;
            }
        }
        return cached[cached.length - 1].glId;
    }

    private static BufferedImage[] getDecoded() {
        request();
        return decodedFrames;
    }

    private static synchronized boolean uploadIfNeeded(BufferedImage[] frames) {
        if (uploadedFrames != null) {
            return true;
        }
        Frame[] result = new Frame[frames.length];
        int[] delays = frameDelays;
        int total = 0;
        try {
            for (int i = 0; i < frames.length; i++) {
                NativeImage image = toNativeImage(frames[i]);
                if (image == null) {
                    uploadedFrames = new Frame[0];
                    return false;
                }
                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                texture.bindTexture();
                texture.setFilter(true, false);
                int glId = texture.getGlId();
                int delay = delays != null && i < delays.length ? delays[i] : 60;
                result[i] = new Frame(glId, Math.max(10, delay));
                total += result[i].delayMs;
            }
            uploadedFrames = result;
            totalMs = Math.max(1, total);
            return true;
        } catch (Throwable throwable) {
            uploadedFrames = new Frame[0];
            return false;
        }
    }

    private static NativeImage toNativeImage(BufferedImage image) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);
            return NativeImage.read(new ByteArrayInputStream(buffer.toByteArray()));
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static void decode() {
        try {
            List<BufferedImage> images = new ArrayList<>();
            List<Integer> delays = new ArrayList<>();
            InputStream stream = LogoGif.class.getResourceAsStream("/assets/desrexsive/pictures/Desrexsive_Logo_3D_Glow.gif");
            if (stream == null) {
                decodedFrames = new BufferedImage[0];
                return;
            }
            try (ImageInputStream input = ImageIO.createImageInputStream(stream)) {
                ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
                reader.setInput(input, false, true);
                int count = reader.getNumImages(true);
                if (count <= 0) {
                    decodedFrames = new BufferedImage[0];
                    return;
                }
                int canvasWidth = reader.getWidth(0);
                int canvasHeight = reader.getHeight(0);
                IIOMetadataNode streamRoot = toNode(reader.getStreamMetadata());
                IIOMetadataNode screen = node(streamRoot, "LogicalScreenDescriptor");
                if (screen != null) {
                    String width = screen.getAttribute("screenWidth");
                    String height = screen.getAttribute("screenHeight");
                    if (width != null && !width.isEmpty() && height != null && !height.isEmpty()) {
                        int parsedWidth = Integer.parseInt(width);
                        int parsedHeight = Integer.parseInt(height);
                        if (parsedWidth > 0 && parsedHeight > 0) {
                            canvasWidth = parsedWidth;
                            canvasHeight = parsedHeight;
                        }
                    }
                }
                BufferedImage master = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                for (int i = 0; i < count; i++) {
                    BufferedImage frame = reader.read(i);
                    int left = 0;
                    int top = 0;
                    int delayMs = 60;
                    int disposal = 1;
                    IIOMetadataNode frameRoot = toNode(reader.getImageMetadata(i));
                    IIOMetadataNode control = node(frameRoot, "GraphicControlExtension");
                    if (control != null) {
                        String value = control.getAttribute("delayTime");
                        if (value != null && !value.isEmpty()) {
                            delayMs = Math.max(10, Integer.parseInt(value) * 10);
                        }
                        value = control.getAttribute("disposalMethod");
                        if (value != null && !value.isEmpty()) {
                            disposal = Integer.parseInt(value);
                        }
                    }
                    IIOMetadataNode descriptor = node(frameRoot, "ImageDescriptor");
                    if (descriptor != null) {
                        String value = descriptor.getAttribute("imageLeftPosition");
                        if (value != null && !value.isEmpty()) {
                            left = Integer.parseInt(value);
                        }
                        value = descriptor.getAttribute("imageTopPosition");
                        if (value != null && !value.isEmpty()) {
                            top = Integer.parseInt(value);
                        }
                    }
                    Graphics2D painter = master.createGraphics();
                    painter.drawImage(frame, left, top, null);
                    painter.dispose();
                    BufferedImage snapshot = copy(master);
                    images.add(downscale(snapshot));
                    delays.add(delayMs);
                    if (disposal == 2) {
                        Graphics2D clearer = master.createGraphics();
                        clearer.setComposite(AlphaComposite.Clear);
                        clearer.fillRect(left, top, frame.getWidth(), frame.getHeight());
                        clearer.dispose();
                    }
                }
            }
            int[] delayArray = new int[delays.size()];
            for (int i = 0; i < delayArray.length; i++) {
                delayArray[i] = delays.get(i);
            }
            frameDelays = delayArray;
            decodedFrames = images.toArray(new BufferedImage[0]);
        } catch (Throwable throwable) {
            decodedFrames = new BufferedImage[0];
        }
    }

    private static IIOMetadataNode toNode(IIOMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
        } catch (Throwable throwable) {
            try {
                return (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_stream_1.0");
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static IIOMetadataNode node(IIOMetadataNode root, String name) {
        if (root == null) {
            return null;
        }
        for (int i = 0; i < root.getLength(); i++) {
            IIOMetadataNode child = (IIOMetadataNode) root.item(i);
            if (child.getNodeName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = copy.createGraphics();
        painter.drawImage(source, 0, 0, null);
        painter.dispose();
        return copy;
    }

    private static BufferedImage downscale(BufferedImage source) {
        int sourceWidth = Math.max(1, source.getWidth());
        int sourceHeight = Math.max(1, source.getHeight());
        int width = Math.min(TARGET_SIZE, sourceWidth);
        int height = Math.max(1, (int) Math.round((double) sourceHeight * width / sourceWidth));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = scaled.createGraphics();
        painter.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        painter.drawImage(source, 0, 0, width, height, null);
        painter.dispose();
        return scaled;
    }
}