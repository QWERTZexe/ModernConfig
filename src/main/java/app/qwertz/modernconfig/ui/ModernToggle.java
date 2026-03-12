package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.ModernConfigSettings;
import app.qwertz.modernconfig.theme.ModernConfigTheme;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ModernToggle extends AbstractWidget {
    private boolean state;
    private final Consumer<Boolean> onToggle;
    private final ModernConfigTheme theme;
    private float animationProgress = 0.0f;
    private float toggleProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();

    public ModernToggle(int x, int y, int width, int height, Component text, boolean initial, Consumer<Boolean> onToggle) {
        this(x, y, width, height, text, initial, onToggle, null);
    }

    public ModernToggle(int x, int y, int width, int height, Component text, boolean initial, Consumer<Boolean> onToggle, ModernConfigTheme theme) {
        super(x, y, width, height, text);
        this.state = initial;
        this.onToggle = onToggle;
        this.theme = theme;
        this.toggleProgress = initial ? 1.0f : 0.0f;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float) ModernConfigSettings.getAnimationDurationMs();
        lastTime = currentTime;

        // Hover animation
        if (isHovered()) {
            animationProgress = Math.min(1.0f, animationProgress + deltaTime);
        } else {
            animationProgress = Math.max(0.0f, animationProgress - deltaTime);
        }

        // Toggle animation
        if (state) {
            toggleProgress = Math.min(1.0f, toggleProgress + deltaTime * 2);
        } else {
            toggleProgress = Math.max(0.0f, toggleProgress - deltaTime * 2);
        }

        float easedHoverProgress = RenderUtil.easeOutExpo(animationProgress);
        float easedToggleProgress = RenderUtil.easeInOutQuad(toggleProgress);
        
        // Draw background
        int baseColor = 0xFF2A2A2A;
        int hoverColor = 0xFF3A3A3A;
        int currentColor = RenderUtil.interpolateColor(baseColor, hoverColor, easedHoverProgress);
        RenderUtil.drawRoundedRect(context, getX(), getY(), getWidth(), getHeight(), 4, currentColor);
        
        // Draw toggle indicator
        int toggleWidth = 40;
        int toggleHeight = 16;
        int toggleX = getX() + getWidth() - toggleWidth - 8;
        int toggleY = getY() + (getHeight() - toggleHeight) / 2;
        
        // Draw toggle background
        int offColor = 0xFF444444;
        int onColor = theme != null ? (0xFF000000 | (theme.getAccentSecondary() & 0xFFFFFF)) : 0xFF88CC88;
        int toggleBgColor = RenderUtil.interpolateColor(offColor, onColor, easedToggleProgress);
        RenderUtil.drawRoundedRect(context, toggleX, toggleY, toggleWidth, toggleHeight, toggleHeight / 2, toggleBgColor);
        
        // Draw toggle knob
        int knobSize = toggleHeight - 4;
        float knobX = toggleX + 2 + (toggleWidth - knobSize - 4) * easedToggleProgress;
        RenderUtil.drawRoundedRect(context, (int)knobX, toggleY + 2, knobSize, knobSize, knobSize / 2, 0xFFFFFFFF);

        // Draw text
        int textColor = theme != null ? theme.getTextColor() : 0xFFFFFFFF;
        float textY = getY() + (getHeight() - 8) / 2.0f;
        context.drawString(
            Minecraft.getInstance().font,
            getMessage(),
            getX() + 8,
            (int)textY,
            textColor
        );
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        state = !state;
        onToggle.accept(state);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }
}
