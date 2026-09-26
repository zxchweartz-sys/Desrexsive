package aethereal.config;

import aethereal.core.InterfaceC0020Opcode;

public enum ThemeInfo {
    PRIMARY(
            new ThemeConstructor("primary", InterfaceC0020Opcode.aS, InterfaceC0020Opcode.bh, 255, 75),
            new ThemeConstructor("primary", InterfaceC0020Opcode.aS, InterfaceC0020Opcode.bh, 255, 75),
            new ThemeConstructor("primary", InterfaceC0020Opcode.aS, InterfaceC0020Opcode.bh, 255, 75),
            new ThemeConstructor("primary", InterfaceC0020Opcode.aS, InterfaceC0020Opcode.bh, 255, 75),
            new ThemeConstructor("primary", InterfaceC0020Opcode.aS, InterfaceC0020Opcode.bh, 255, 75)
    ),
    BACKGROUND_HUD(
            new ThemeConstructor("background_hud", 6, 6, 11, InterfaceC0020Opcode.cY),
            new ThemeConstructor("background_hud", 11, 11, 22, InterfaceC0020Opcode.cY),
            new ThemeConstructor("background_hud", 26, 27, 37, InterfaceC0020Opcode.cY),
            new ThemeConstructor("background_hud", 240, 242, 245, InterfaceC0020Opcode.cY),
            new ThemeConstructor("background_hud", 251, 252, 254, InterfaceC0020Opcode.cY)
    ),
    BACKGROUND_GUI(
            new ThemeConstructor("background_gui", 3, 3, 4, 255),
            new ThemeConstructor("background_gui", 8, 8, 8, 255),
            new ThemeConstructor("background_gui", 38, 39, 48, 255),
            new ThemeConstructor("background_gui", 253, 254, 255, 255),
            new ThemeConstructor("background_gui", 255, 255, 255, 255)
    ),
    OUTLINE_SMALL(
            new ThemeConstructor("outline_small", 255, 255, 255, 6),
            new ThemeConstructor("outline_small", 255, 255, 255, 5),
            new ThemeConstructor("outline_small", 255, 255, 255, 6),
            new ThemeConstructor("outline_small", 17, 18, 22, 5),
            new ThemeConstructor("outline_small", 17, 18, 22, 6)
    ),
    OUTLINE_MEDIUM(
            new ThemeConstructor("outline_medium", 255, 255, 255, 8),
            new ThemeConstructor("outline_medium", 255, 255, 255, 10),
            new ThemeConstructor("outline_medium", 255, 255, 255, 10),
            new ThemeConstructor("outline_medium", 17, 18, 22, 5),
            new ThemeConstructor("outline_medium", 17, 18, 22, 7)
    ),
    TEXT(
            new ThemeConstructor("typography_text", 255, 255, 255, 255),
            new ThemeConstructor("typography_text", 255, 255, 255, 255),
            new ThemeConstructor("typography_text", 250, 250, 252, 255),
            new ThemeConstructor("typography_text", 17, 18, 22, 255),
            new ThemeConstructor("typography_text", 17, 18, 22, 255)
    ),
    TEXT_DISABLED(
            new ThemeConstructor("typography_disabled", 58, 61, 72, 255),
            new ThemeConstructor("typography_disabled", 67, 70, 81, 255),
            new ThemeConstructor("typography_disabled", 126, 130, 148, 255),
            new ThemeConstructor("typography_disabled", InterfaceC0020Opcode.bv, InterfaceC0020Opcode.aD, InterfaceC0020Opcode.C, 255),
            new ThemeConstructor("typography_disabled", 115, 119, 138, 255)
    );

    private final ThemeConstructor black;
    private final ThemeConstructor dark;
    private final ThemeConstructor gray;
    private final ThemeConstructor light;
    private final ThemeConstructor white;

    ThemeInfo(ThemeConstructor black, ThemeConstructor dark, ThemeConstructor gray, ThemeConstructor light, ThemeConstructor white) {
        this.black = black;
        this.dark = dark;
        this.gray = gray;
        this.light = light;
        this.white = white;
    }

    public ThemeConstructor a(ThemeType theme) {
        switch (theme) {
            case BLACK:
                return this.black;
            case GRAY:
                return this.gray;
            case WHITE:
                return this.white;
            case LIGHT:
                return this.light;
            default:
                return this.dark;
        }
    }

    public ThemeConstructor a() {
        return this.light;
    }
}