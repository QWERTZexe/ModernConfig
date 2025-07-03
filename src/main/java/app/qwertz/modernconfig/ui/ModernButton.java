package app.qwertz.modernconfig.ui;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.MinecraftClient;

public class ModernButton extends ClickableWidget {
    private final Runnable onClick;
    private float animationProgress = 0.0f;
    private static final int ANIMATION_DURATION = 200; // milliseconds
    private long lastTime = System.currentTimeMillis();

    public ModernButton(int x, int y, int width, int height, Text text, Runnable onClick) {
        super(x, y, width, height, text);
        this.onClick = onClick;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float)ANIMATION_DURATION;
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
        
        // Draw outline
        int outlineColor = RenderUtil.interpolateColor(0x33FFFFFF, 0x77FFFFFF, easedProgress);
        RenderUtil.drawRoundedRect(context, getX(), getY(), getWidth(), 1, 0, outlineColor); // top
        RenderUtil.drawRoundedRect(context, getX(), getY() + getHeight() - 1, getWidth(), 1, 0, outlineColor); // bottom
        RenderUtil.drawRoundedRect(context, getX(), getY(), 1, getHeight(), 0, outlineColor); // left
        RenderUtil.drawRoundedRect(context, getX() + getWidth() - 1, getY(), 1, getHeight(), 0, outlineColor); // right

        float textY = getY() + (getHeight() - 8) / 2.0f;
        context.drawTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            getMessage(),
            getX() + (getWidth() - MinecraftClient.getInstance().textRenderer.getWidth(getMessage())) / 2,
            (int)textY,
            0xFFFFFFFF
        );
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onClick.run();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
} 