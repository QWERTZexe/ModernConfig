package app.qwertz.modernconfig.ui;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.MinecraftClient;

import java.util.function.Consumer;

public class ModernToggle extends ClickableWidget {
    private boolean state;
    private final Consumer<Boolean> onToggle;
    private float animationProgress = 0.0f;
    private float toggleProgress = 0.0f;
    private static final int ANIMATION_DURATION = 200; // milliseconds
    private long lastTime = System.currentTimeMillis();

    public ModernToggle(int x, int y, int width, int height, Text text, boolean initial, Consumer<Boolean> onToggle) {
        super(x, y, width, height, text);
        this.state = initial;
        this.onToggle = onToggle;
        this.toggleProgress = initial ? 1.0f : 0.0f;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float)ANIMATION_DURATION;
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
        int onColor = 0xFF88CC88;
        int toggleBgColor = RenderUtil.interpolateColor(offColor, onColor, easedToggleProgress);
        RenderUtil.drawRoundedRect(context, toggleX, toggleY, toggleWidth, toggleHeight, toggleHeight / 2, toggleBgColor);
        
        // Draw toggle knob
        int knobSize = toggleHeight - 4;
        float knobX = toggleX + 2 + (toggleWidth - knobSize - 4) * easedToggleProgress;
        RenderUtil.drawRoundedRect(context, (int)knobX, toggleY + 2, knobSize, knobSize, knobSize / 2, 0xFFFFFFFF);

        // Draw text
        float textY = getY() + (getHeight() - 8) / 2.0f;
        context.drawTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            getMessage(),
            getX() + 8,
            (int)textY,
            0xFFFFFFFF
        );
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        state = !state;
        onToggle.accept(state);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
