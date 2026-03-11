package app.qwertz.modernconfig.config;

/**
 * End-user settings for the ModernConfig UI. Read from the "modernconfig" config, "settings" category.
 */
public final class ModernConfigSettings {
    private static final String MOD_ID = "modernconfig";
    private static final String CATEGORY = "settings";

    /** Default animation duration in milliseconds. */
    public static final int DEFAULT_ANIMATION_DURATION = 200;

    /** Get whether animations are enabled (open/close, hover, expand, etc.). */
    public static boolean isAnimationsEnabled() {
        ConfigOption<?> opt = ConfigManager.getOption(MOD_ID, CATEGORY, "enable_animations");
        if (opt != null && opt.getValue() instanceof Boolean b) {
            return b;
        }
        return true;
    }

    /** What to show on mod top-level config: "Close" = close screen, "Back" = go to ModernConfig main menu. */
    public static String getModTopExitButton() {
        ConfigOption<?> opt = ConfigManager.getOption(MOD_ID, CATEGORY, "mod_top_exit_button");
        if (opt != null && opt.getValue() instanceof String s) {
            return "Back".equals(s) ? "Back" : "Close";
        }
        return "Close";
    }

    /** Get whether to show the credit line below the config panel. */
    public static boolean isShowCredit() {
        ConfigOption<?> opt = ConfigManager.getOption(MOD_ID, CATEGORY, "show_credit");
        if (opt != null && opt.getValue() instanceof Boolean b) {
            return b;
        }
        return true;
    }

    /** Get animation duration in milliseconds (1 when disabled for instant snap, else Fast=100, Normal=200, Slow=350, Super slow=600). */
    public static int getAnimationDurationMs() {
        if (!isAnimationsEnabled()) {
            return 1; // near-instant so one frame completes the animation
        }
        ConfigOption<?> opt = ConfigManager.getOption(MOD_ID, CATEGORY, "animation_speed");
        if (opt != null && opt.getValue() instanceof String s) {
            return switch (s) {
                case "Fast" -> 100;
                case "Slow" -> 350;
                case "Super slow" -> 600;
                default -> DEFAULT_ANIMATION_DURATION;
            };
        }
        return DEFAULT_ANIMATION_DURATION;
    }
}
