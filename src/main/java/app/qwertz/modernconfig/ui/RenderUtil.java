package app.qwertz.modernconfig.ui;

import net.minecraft.client.gui.DrawContext;

public class RenderUtil {
    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        // Draw main rectangle
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + width, y + height - radius, color);
        
        // Draw corners
        for (int i = 0; i < radius * 2; i++) {
            for (int j = 0; j < radius * 2; j++) {
                float dx = i - radius;
                float dy = j - radius;
                if (dx * dx + dy * dy <= radius * radius) {
                    // Top left
                    context.fill(x + i, y + j, x + i + 1, y + j + 1, color);
                    // Top right
                    context.fill(x + width - radius * 2 + i, y + j, x + width - radius * 2 + i + 1, y + j + 1, color);
                    // Bottom left
                    context.fill(x + i, y + height - radius * 2 + j, x + i + 1, y + height - radius * 2 + j + 1, color);
                    // Bottom right
                    context.fill(x + width - radius * 2 + i, y + height - radius * 2 + j, x + width - radius * 2 + i + 1, y + height - radius * 2 + j + 1, color);
                }
            }
        }
    }

    public static void drawBlurredBackground(DrawContext context, int x, int y, int width, int height, float alpha) {
        int color1 = (int)(alpha * 255) << 24 | 0x101010;
        int color2 = (int)(alpha * 255) << 24 | 0x202020;
        context.fillGradient(x, y, x + width, y + height, color1, color2);
    }

    public static float easeOutExpo(float x) {
        return x == 1 ? 1 : 1 - (float)Math.pow(2, -10 * x);
    }

    public static float easeInOutQuad(float x) {
        return x < 0.5 ? 2 * x * x : 1 - (float)Math.pow(-2 * x + 2, 2) / 2;
    }

    public static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(x - 1, 3) + c1 * (float)Math.pow(x - 1, 2);
    }

    public static int interpolateColor(int color1, int color2, float progress) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 + (a2 - a1) * progress);
        int r = (int)(r1 + (r2 - r1) * progress);
        int g = (int)(g1 + (g2 - g1) * progress);
        int b = (int)(b1 + (b2 - b1) * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    public static int applyAlpha(int color, float alpha) {
        int originalAlpha = (color >> 24) & 0xFF;
        int newAlpha = (int)(originalAlpha * alpha);
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }
} 