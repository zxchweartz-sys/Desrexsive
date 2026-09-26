package aethereal.ui.screen;

import aethereal.config.ThemeConstructor;
import aethereal.config.ThemeInfo;
import aethereal.config.ThemeProcessor;
import aethereal.config.ThemeType;
import aethereal.core.Desrexsive;
import aethereal.core.InterfaceC0020Opcode;
import aethereal.core.Module;
import aethereal.render.AnimationUtil;
import aethereal.render.ColorUtil;
import aethereal.render.Draw2DProcessor;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import aethereal.render.ScissorUtil;
import aethereal.setting.ColorSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.Setting;
import aethereal.util.MathUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;

import java.util.function.Consumer;

/**
 * Панель «Themes» в клик-гуи (слева). Прозрачная, без обводок, сильно скруглена.
 * Список цветных тем (название + круг-индикатор цвета). Клик по строке плавно переводит
 * все цвета клиента в цвет темы и синхронизирует настройки модуля «Interface»
 * (тема оформления и глобальный цвет интерфейса).
 */
public class ThemesPanel {
    private static final float WIDTH = 125.0f;
    private static final float HEIGHT = 270.0f;
    private static final float ROW_H = 14.0f;
    private static final float ROW_GAP = 4.0f;

    private static final String[] NAMES = new String[]{
            "Тёмно-зелёная",
            "Бирюзовая",
            "Синяя",
            "Лавандовая",
            "Персиковая",
            "Небесная",
            "Тёмно-фиолетовая",
            "Оранжево-жёлтая",
            "Морская",
            "Лаймовая",
            "Салатовая",
            "Коралловая",
            "Кислотная"
    };

    private static final int[] COLORS = new int[]{
            ColorUtil.convertToARGB(0x1B, 0x4D, 0x3E, 255), // #1B4D3E Тёмно-зелёная
            ColorUtil.convertToARGB(0x40, 0xE0, 0xD0, 255), // #40E0D0 Бирюзовая
            ColorUtil.convertToARGB(0x00, 0x78, 0xD7, 255), // #0078D7 Синяя
            ColorUtil.convertToARGB(0xC7, 0xB8, 0xEA, 255), // #C7B8EA Лавандовая
            ColorUtil.convertToARGB(0xF6, 0xC6, 0xB8, 255), // #F6C6B8 Персиковая
            ColorUtil.convertToARGB(0xBD, 0xD7, 0xEE, 255), // #BDD7EE Небесная
            ColorUtil.convertToARGB(0x1A, 0x1A, 0x2E, 255), // #1A1A2E Тёмно-фиолетовая
            ColorUtil.convertToARGB(0xFF, 0xB3, 0x47, 255), // #FFB347 Оранжево-жёлтая
            ColorUtil.convertToARGB(0x4F, 0x9D, 0xA6, 255), // #4F9DA6 Морская
            ColorUtil.convertToARGB(0xCC, 0xFF, 0x00, 255), // #CCFF00 Лаймовая
            ColorUtil.convertToARGB(0x7F, 0xFF, 0x00, 255), // #7FFF00 Салатовая
            ColorUtil.convertToARGB(0xFF, 0x7F, 0x50, 255), // #FF7F50 Коралловая
            ColorUtil.convertToARGB(0xDF, 0xFF, 0x00, 255)  // #DFFF00 Кислотная
    };

    // true — тёмная тема (белый текст), false — светлая (тёмный текст)
    private static final boolean[] DARK = new boolean[]{
            true,  // Тёмно-зелёная
            false, // Бирюзовая
            true,  // Синяя
            false, // Лавандовая
            false, // Персиковая
            false, // Небесная
            true,  // Тёмно-фиолетовая
            false, // Оранжево-жёлтая
            false, // Морская
            false, // Лаймовая
            false, // Салатовая
            false, // Коралловая
            false  // Кислотная
    };

    // цели плавного перехода (статические, чтобы не сбрасывались при переоткрытии гуи)
    private static int selected = -1;
    private static int targetBackground;
    private static int targetHud;
    private static int targetText;
    private static int targetDisabled;
    private static int targetPrimary;

    private final Vector4f bounds = new Vector4f(0.0f, 0.0f, WIDTH, HEIGHT);
    private final AnimationUtil hoverAnimation = new AnimationUtil();
    private final AnimationUtil activeAnimation = new AnimationUtil();
    private float visible = 1.0f;

    public void setVisible(float visible) {
        this.visible = visible;
    }

    public void setPosition(float x, float y) {
        this.bounds.x = x;
        this.bounds.y = y;
    }

    public float height() {
        return HEIGHT;
    }

    public static float width() {
        return WIDTH;
    }

    public boolean onMouseClick(double mouseX, double mouseY) {
        for (int i = 0; i < NAMES.length; i++) {
            float rowY = this.bounds.y + 24.0f + 4.0f + (i * (ROW_H + ROW_GAP));
            if (MathUtil.a(mouseX, mouseY, this.bounds.x + 6.0f, rowY, WIDTH - 12.0f, ROW_H)) {
                apply(i);
                return true;
            }
        }
        return false;
    }

    private void apply(int index) {
        ThemeProcessor theme = Desrexsive.getInstance().getModuleProcessor().o();
        int color = COLORS[index];
        boolean dark = DARK[index];

        // сохраняем текущие цвета, чтобы переключение типа темы не вызвало резкий скачок
        int currentBackground = theme.a(ThemeInfo.BACKGROUND_GUI).toIntColor();
        int currentHud = theme.a(ThemeInfo.BACKGROUND_HUD).toIntColor();
        int currentText = theme.a(ThemeInfo.TEXT).toIntColor();
        int currentDisabled = theme.a(ThemeInfo.TEXT_DISABLED).toIntColor();
        int currentPrimary = theme.a(ThemeInfo.PRIMARY).toIntColor();

        // синхронизация с темой оформления модуля «Interface»
        theme.a(dark ? ThemeType.DARK : ThemeType.LIGHT);
        theme.a(ThemeInfo.BACKGROUND_GUI).fromIntColor(currentBackground);
        theme.a(ThemeInfo.BACKGROUND_HUD).fromIntColor(currentHud);
        theme.a(ThemeInfo.TEXT).fromIntColor(currentText);
        theme.a(ThemeInfo.TEXT_DISABLED).fromIntColor(currentDisabled);
        theme.a(ThemeInfo.PRIMARY).fromIntColor(currentPrimary);

        selected = index;
        targetBackground = color;
        targetHud = ColorUtil.applyAlphaToColor(color, InterfaceC0020Opcode.cY / 255.0f);
        if (dark) {
            targetText = ColorUtil.convertToARGB(255, 255, 255, 255);
            targetDisabled = ColorUtil.convertToARGB(150, 155, 165, 255);
            targetPrimary = ColorUtil.lerpColor(color, ColorUtil.convertToARGB(255, 255, 255, 255), 0.4f);
        } else {
            targetText = ColorUtil.convertToARGB(20, 21, 26, 255);
            targetDisabled = ColorUtil.convertToARGB(110, 114, 128, 255);
            targetPrimary = ColorUtil.lerpColor(color, ColorUtil.convertToARGB(0, 0, 0, 255), 0.3f);
        }
        syncInterface(dark, targetPrimary);
    }

    /**
     * Синхронизирует выбранную тему с настройками модуля «Interface»:
     * «Тема оформления» (Тёмная/Светлая) и «Глобальный цвет интерфейса».
     */
    private void syncInterface(boolean dark, int primary) {
        for (Module module : Desrexsive.getInstance().getModuleProcessor().t().e()) {
            if (!"Interface".equals(module.j())) {
                continue;
            }
            for (Setting<?> setting : module.e()) {
                if (setting instanceof ColorSetting && "Глобальный цвет интерфейса".equals(setting.i())) {
                    ((ColorSetting) setting).a(primary);
                } else if (setting instanceof ModeSetting && "Тема оформления".equals(setting.i())) {
                    ModeSetting mode = (ModeSetting) setting;
                    Consumer<String> onChange = mode.f();
                    mode.a(ignored -> {
                    });
                    mode.a(dark ? "Тёмная" : "Светлая");
                    mode.a(onChange);
                }
            }
        }
    }

    /**
     * Синхронизирует «Глобальный цвет интерфейса» модуля «Interface» с текущим
     * анимированным значением акцента, чтобы модуль не перезаписывал плавный переход.
     */
    private void syncGlobalColor(int primary) {
        for (Module module : Desrexsive.getInstance().getModuleProcessor().t().e()) {
            if ("Interface".equals(module.j())) {
                for (Setting<?> setting : module.e()) {
                    if (setting instanceof ColorSetting && "Глобальный цвет интерфейса".equals(setting.i())) {
                        ((ColorSetting) setting).a(primary);
                    }
                }
            }
        }
    }

    private void animate(ThemeConstructor constructor, int target, float factor) {
        int current = constructor.toIntColor();
        if (current != target) {
            constructor.fromIntColor(ColorUtil.lerpColor(current, target, Math.min(1.0f, factor)));
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        Draw2DProcessor draw = Desrexsive.getInstance().getModuleProcessor().i();
        ThemeProcessor theme = Desrexsive.getInstance().getModuleProcessor().o();
        float x = this.bounds.x;
        float y = this.bounds.y;
        if (this.visible <= 0.001f) {
            return;
        }

        if (selected > 0 && selected >= NAMES.length) {
            selected = -1;
        }
        if (selected == -1) {
            int currentBackground = theme.a(ThemeInfo.BACKGROUND_GUI).toIntColor();
            for (int i = 0; i < COLORS.length; i++) {
                if (currentBackground == COLORS[i]) {
                    apply(i);
                    break;
                }
            }
        }

        if (selected >= 0) {
            float factor = 1.0f - (float) Math.exp(-delta * 5.5f);
            animate(theme.a(ThemeInfo.BACKGROUND_GUI), targetBackground, factor);
            animate(theme.a(ThemeInfo.BACKGROUND_HUD), targetHud, factor);
            animate(theme.a(ThemeInfo.TEXT), targetText, factor);
            animate(theme.a(ThemeInfo.TEXT_DISABLED), targetDisabled, factor);
            animate(theme.a(ThemeInfo.PRIMARY), targetPrimary, factor);
            syncGlobalColor(theme.a(ThemeInfo.PRIMARY).toIntColor());
        }

        int background = ColorUtil.combineColorWithAlpha(ColorUtil.lerpColor(theme.a(ThemeInfo.BACKGROUND_GUI).toIntColor(), theme.a(ThemeInfo.PRIMARY).toIntColor(), theme.a(ThemeInfo.PRIMARY).getAlphaFloat() / 4.0f), InterfaceC0020Opcode.aN);
        draw.a(matrices, x, y, WIDTH, HEIGHT, 16.0f, background, this.visible, background, 16.0f);

        float headerCenter = y + 12.0f;
        int headerColor = ColorUtil.lerpColor(theme.a(ThemeInfo.TEXT).toIntColor(), ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), 1.0f), 0.25f);
        Fonts.c.a(matrices, "Themes", x + 6.0f + 4.0f, Fonts.c.a("Themes", 9.0f, headerCenter), 9.0f, ColorUtil.applyAlphaToColor(headerColor, this.visible));
        float iconSize = 8.0f;
        float iconX = ((x + WIDTH) - 6.0f - 4.0f) - iconSize;
        draw.a(matrices, iconX, headerCenter - (iconSize / 2.0f), iconSize, iconSize, 4.0f, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.85f * this.visible));

        float rowsY = y + 24.0f + 4.0f;
        float view = ((y + HEIGHT) - 6.0f) - rowsY + 4.0f;
        float bottom = rowsY + view;
        ScissorUtil.a(matrices, x, rowsY, WIDTH, view);
        for (int i = 0; i < NAMES.length; i++) {
            float rowY = rowsY + (i * (ROW_H + ROW_GAP));
            if (rowY + ROW_H <= rowsY || rowY >= bottom) {
                continue;
            }
            boolean active = selected == i;
            boolean hover = ((float) mouseY) >= rowsY && ((float) mouseY) <= bottom && MathUtil.a(mouseX, mouseY, x + 6.0f, rowY, WIDTH - 12.0f, ROW_H);
            this.hoverAnimation.a(0.0f, 1.0f, 0.25f, EasingList.i, delta);
            this.hoverAnimation.a(hover);
            this.activeAnimation.a(0.0f, 1.0f, 0.5f, EasingList.i, delta);
            this.activeAnimation.a(active);
            float hoverValue = this.hoverAnimation.c();
            float activeValue = this.activeAnimation.c();
            float fade = (float) Math.pow(MathUtil.a(MathUtil.b((bottom - rowY) / 16.0f, 0.0f, 1.0f)), 1.0d);
            draw.a(matrices, x + 6.0f, rowY, WIDTH - 12.0f, ROW_H, 8.0f, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.PRIMARY).toIntColor(), 0.039215688f * activeValue * fade * this.visible));
            draw.a(matrices, x + 6.0f, rowY, WIDTH - 12.0f, ROW_H, 8.0f, ColorUtil.applyAlphaToColor(ColorUtil.convertToARGB(255, 255, 255, 255), 0.023529412f * hoverValue * fade * this.visible));
            Fonts.c.a(matrices, NAMES[i], x + 6.0f + 4.0f, (rowY + 7.0f - (Fonts.c.a(7.0f) / 2.0f)) - 0.5f, 7.0f, ColorUtil.applyAlphaToColor(theme.a(ThemeInfo.TEXT).toIntColor(), fade * this.visible));
            float circleX = ((x + WIDTH) - 6.0f - 4.0f) - 12.0f;
            float circleY = rowY + 7.0f - 6.0f;
            draw.a(matrices, circleX, circleY, 12.0f, 12.0f, 6.0f, ColorUtil.applyAlphaToColor(COLORS[i], 0.85f * fade * this.visible));
        }
        ScissorUtil.a(matrices);
    }
}