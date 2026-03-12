package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.ModernConfigSettings;
import app.qwertz.modernconfig.theme.ModernConfigTheme;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ModernButton extends AbstractWidget {
    private final Runnable onClick;
    private final ModernConfigTheme theme;
    private float animationProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();

    public ModernButton(int x, int y, int width, int height, Component text, Runnable onClick) {
        this(x, y, width, height, text, onClick, null);
    }

    public ModernButton(int x, int y, int width, int height, Component text, Runnable onClick, ModernConfigTheme theme) {
        super(x, y, width, height, text);
        this.onClick = onClick;
        this.theme = theme;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float) ModernConfigSettings.getAnimationDurationMs();
        lastTime = currentTime;

        if (isHovered()) {
            animationProgress = Math.min(1.0f, animationProgress + deltaTime);
        } else {
            animationProgress = Math.max(0.0f, animationProgress - deltaTime);
        }

        float easedProgress = RenderUtil.easeOutExpo(animationProgress);
        
        int baseColor = 0xFF2A2A2A;
        int hoverColor = 0xFF3A3A3A;
        int currentColor = RenderUtil.interpolateColor(baseColor, hoverColor, easedProgress);
        
        RenderUtil.drawRoundedRect(context, getX(), getY(), getWidth(), getHeight(), 4, currentColor);
        
        // Draw outline (accent tint when themed and hovered)
        int outlineLo = 0x33FFFFFF;
        int outlineHi = 0x77FFFFFF;
        if (theme != null && easedProgress > 0) {
            int accent = theme.getAccentColor();
            outlineHi = (0x77 << 24) | (accent & 0xFFFFFF);
        }
        int outlineColor = RenderUtil.interpolateColor(outlineLo, outlineHi, easedProgress);
        RenderUtil.drawRoundedRect(context, getX(), getY(), getWidth(), 1, 0, outlineColor); // top
        RenderUtil.drawRoundedRect(context, getX(), getY() + getHeight() - 1, getWidth(), 1, 0, outlineColor); // bottom
        RenderUtil.drawRoundedRect(context, getX(), getY(), 1, getHeight(), 0, outlineColor); // left
        RenderUtil.drawRoundedRect(context, getX() + getWidth() - 1, getY(), 1, getHeight(), 0, outlineColor); // right

        int textColor = theme != null ? theme.getTextColor() : 0xFFFFFFFF;
        float textY = getY() + (getHeight() - 8) / 2.0f;
        context.drawString(
            Minecraft.getInstance().font,
            getMessage(),
            getX() + (getWidth() - Minecraft.getInstance().font.width(getMessage())) / 2,
            (int)textY,
            textColor
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0 && event.x() >= getX() && event.x() <= getX() + getWidth() &&
            event.y() >= getY() && event.y() <= getY() + getHeight()) {
            onClick.run();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }
} 