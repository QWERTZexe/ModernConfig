package app.qwertz.modernconfig.theme;

public final class ModernConfigTheme {

    public static final ModernConfigTheme DEFAULT = new ModernConfigTheme(
        0x4A90E2,
        0x202020,
        0x40FFFFFF,
        0x88CC88,
        0xFFFFFF,
        0xAAAAAA
    );

    public static final ModernConfigTheme YELLOW = new ModernConfigTheme(
        0xE6B800,
        0x252520,
        0x50E6B800,
        0xB8860B,
        0xFFF8E7,
        0xE6C76C
    );

    public static final ModernConfigTheme GREEN = new ModernConfigTheme(
        0x4ADE80,
        0x1E2520,
        0x504ADE80,
        0x22C55E,
        0xE8FFF0,
        0x86EFAC
    );

    public static final ModernConfigTheme PURPLE = new ModernConfigTheme(
        0xA78BFA,
        0x251E28,
        0x50A78BFA,
        0x8B5CF6,
        0xF5F0FF,
        0xC4B5FD
    );

    public static final ModernConfigTheme CYAN = new ModernConfigTheme(
        0x22D3EE,
        0x1E2528,
        0x5022D3EE,
        0x06B6D4,
        0xE0FCFF,
        0x67E8F9
    );

    public static final ModernConfigTheme ROSE = new ModernConfigTheme(
        0xFB7185,
        0x282025,
        0x50FB7185,
        0xF43F5E,
        0xFFF0F3,
        0xFDA4AF
    );

    public static final ModernConfigTheme ORANGE = new ModernConfigTheme(
        0xFB923C,
        0x282520,
        0x50FB923C,
        0xF97316,
        0xFFF7ED,
        0xFDBA74
    );

    private final int accentColor;
    private final int containerBackground;
    private final int containerOutline;
    private final int accentSecondary; // e.g. toggle "on", selected option
    private final int textColor;
    private final int textColorSecondary;

    private ModernConfigTheme(int accentColor, int containerBackground, int containerOutline, int accentSecondary, int textColor, int textColorSecondary) {
        this.accentColor = accentColor;
        this.containerBackground = containerBackground;
        this.containerOutline = containerOutline;
        this.accentSecondary = accentSecondary;
        this.textColor = 0xFF000000 | (textColor & 0xFFFFFF);
        this.textColorSecondary = 0xFF000000 | (textColorSecondary & 0xFFFFFF);
    }

    public int getAccentColor() {
        return accentColor;
    }

    public int getContainerBackground() {
        return containerBackground;
    }

    public int getContainerOutline() {
        return containerOutline;
    }

    public int getAccentSecondary() {
        return accentSecondary;
    }

    /** Primary text color (labels, titles). */
    public int getTextColor() {
        return textColor;
    }

    /** Secondary/muted text color (descriptions). */
    public int getTextColorSecondary() {
        return textColorSecondary;
    }

    public static ModernConfigTheme custom(int accentColor) {
        int outline = (0x40 << 24) | (accentColor & 0xFFFFFF);
        int secondary = deriveSecondary(accentColor);
        return new ModernConfigTheme(accentColor, 0x202020, outline, secondary, 0xFFFFFF, 0xAAAAAA);
    }

    public static ModernConfigTheme custom(int accentColor, int containerBackground, int containerOutline, int accentSecondary, int textColor, int textColorSecondary) {
        return new ModernConfigTheme(accentColor, containerBackground, containerOutline, accentSecondary, textColor, textColorSecondary);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static int deriveSecondary(int accent) {
        int r = (accent >> 16) & 0xFF;
        int g = (accent >> 8) & 0xFF;
        int b = accent & 0xFF;
        // Slightly lighter/more saturated for "on" states
        r = Math.min(255, r + 40);
        g = Math.min(255, g + 40);
        b = Math.min(255, b + 40);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static final class Builder {
        private int accentColor = 0x4A90E2;
        private Integer containerBackground = null;
        private Integer containerOutline = null;
        private Integer accentSecondary = null;
        private int textColor = 0xFFFFFF;
        private int textColorSecondary = 0xAAAAAA;

        public Builder accentColor(int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public Builder containerBackground(int containerBackground) {
            this.containerBackground = containerBackground;
            return this;
        }

        public Builder containerOutline(int containerOutline) {
            this.containerOutline = containerOutline;
            return this;
        }

        public Builder accentSecondary(int accentSecondary) {
            this.accentSecondary = accentSecondary;
            return this;
        }

        public Builder textColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder textColorSecondary(int textColorSecondary) {
            this.textColorSecondary = textColorSecondary;
            return this;
        }

        public ModernConfigTheme build() {
            int bg = containerBackground != null ? containerBackground : 0x202020;
            int outline = containerOutline != null ? containerOutline : ((0x40 << 24) | (accentColor & 0xFFFFFF));
            int secondary = accentSecondary != null ? accentSecondary : deriveSecondary(accentColor);
            return new ModernConfigTheme(accentColor, bg, outline, secondary, textColor, textColorSecondary);
        }
    }
}
