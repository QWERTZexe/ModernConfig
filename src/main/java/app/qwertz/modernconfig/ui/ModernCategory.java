package app.qwertz.modernconfig.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.font.TextRenderer;
import app.qwertz.modernconfig.config.ConfigOption;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModernCategory extends ClickableWidget {
    private final Text description;
    private final List<Object> elements = new ArrayList<>(); // Can contain ConfigOption<?> or ModernCategory
    private float hoverProgress = 0.0f;
    private float arrowAnimProgress = 0.0f;
    private static final int HOVER_ANIMATION_DURATION = 200; // milliseconds
    private long lastTime = System.currentTimeMillis();
    private final Consumer<ModernCategory> onClick;

    public ModernCategory(int x, int y, int width, int height, Text title, Text description, Consumer<ModernCategory> onClick) {
        super(x, y, width, height, title);
        this.description = description;
        this.onClick = onClick;
    }

    public void addElement(Object element) {
        if (element instanceof ModernCategory || element instanceof ConfigOption<?>) {
            elements.add(element);
        }
    }

    public List<Object> getElements() {
        return elements;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onClick.accept(this);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float)HOVER_ANIMATION_DURATION;
        lastTime = currentTime;

        if (isHovered()) {
            hoverProgress = Math.min(1.0f, hoverProgress + deltaTime * 3);
            arrowAnimProgress = Math.min(1.0f, arrowAnimProgress + deltaTime * 2);
        } else {
            hoverProgress = Math.max(0.0f, hoverProgress - deltaTime * 3);
            arrowAnimProgress = Math.max(0.0f, arrowAnimProgress - deltaTime * 2);
        }

        float easedProgress = RenderUtil.easeOutExpo(hoverProgress);
        float arrowEased = RenderUtil.easeOutBack(arrowAnimProgress);

        // Enhanced background with gradient-like effect
        int baseColor = 0xFF202020;
        int hoverColor = 0xFF2A2A2A;
        int backgroundColor = interpolateColor(baseColor, hoverColor, easedProgress);
        
        // Add subtle glow effect when hovered
        if (hoverProgress > 0) {
            int glowAlpha = (int)(easedProgress * 20);
            int glowColor = (glowAlpha << 24) | 0x4A90E2;
            RenderUtil.drawRoundedRect(context, getX() - 1, getY() - 1, width + 2, height + 2, 9, glowColor);
        }
        
        RenderUtil.drawRoundedRect(context, getX(), getY(), width, height, 8, backgroundColor);

        // Enhanced outline with multiple layers
        int outlineAlpha = (int)(64 + easedProgress * 96);
        int outlineColor = (outlineAlpha << 24) | 0xFFFFFF;
        
        // Main outline
        RenderUtil.drawRoundedRect(context, getX(), getY(), width, 1, 0, outlineColor);
        RenderUtil.drawRoundedRect(context, getX(), getY() + height - 1, width, 1, 0, outlineColor);
        RenderUtil.drawRoundedRect(context, getX(), getY(), 1, height, 0, outlineColor);
        RenderUtil.drawRoundedRect(context, getX() + width - 1, getY(), 1, height, 0, outlineColor);

        // Inner highlight when hovered
        if (hoverProgress > 0) {
            int highlightAlpha = (int)(easedProgress * 32);
            int highlightColor = (highlightAlpha << 24) | 0xFFFFFF;
            RenderUtil.drawRoundedRect(context, getX() + 1, getY() + 1, width - 2, 1, 0, highlightColor);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        // Title in white
        int titleColor = 0xFFFFFFFF; // Pure white, no hover interpolation
        int titleY = getY() + 8; // Moved up slightly
        context.drawTextWithShadow(textRenderer, getMessage(), getX() + 16, titleY, titleColor);

        // Description in light grey
        int descriptionColor = 0xFFAAAAAA; // Light grey, no hover interpolation
        int descriptionY = titleY + textRenderer.fontHeight + 2; // Closer to title
        
        // Wrap description text if too long
        String descText = description.getString();
        int maxDescWidth = width - 60; // More space for text, arrow is at the right
        if (textRenderer.getWidth(descText) > maxDescWidth) {
            while (textRenderer.getWidth(descText + "...") > maxDescWidth && descText.length() > 0) {
                descText = descText.substring(0, descText.length() - 1);
            }
            descText += "...";
        }
        
        context.drawTextWithShadow(textRenderer, descText, getX() + 16, descriptionY, descriptionColor);

        // Count items in category (moved to right side near arrow)
        int itemCount = elements.size();
        if (itemCount > 0) {
            String countText = itemCount + " item" + (itemCount == 1 ? "" : "s");
            int countColor = 0xFF888888;
            int countWidth = textRenderer.getWidth(countText);
            context.drawTextWithShadow(textRenderer, countText, getX() + width - 60 - countWidth, getY() + height/2 - textRenderer.fontHeight/2, countColor);
        }

        // Enhanced arrow design
        drawEnhancedArrow(context, easedProgress, arrowEased);
    }

    private void drawEnhancedArrow(DrawContext context, float hoverProgress, float animProgress) {
        int arrowBaseX = getX() + width - 40;
        int arrowBaseY = getY() + height / 2;
        
        // Arrow colors with animation
        int arrowAlpha = (int)(128 + hoverProgress * 127);
        int arrowColor = (arrowAlpha << 24) | interpolateColor(0xAAAAAA, 0x4A90E2, hoverProgress);
        
        // Arrow size and animation
        int baseSize = 8;
        float animatedSize = baseSize * (0.8f + 0.4f * animProgress);
        int arrowSize = (int)animatedSize;
        
        // Arrow position with subtle movement animation
        int arrowX = (int)(arrowBaseX + animProgress * 3);
        int arrowY = arrowBaseY;

        // Draw arrow shaft
        int shaftLength = 12;
        int shaftY = arrowY;
        RenderUtil.drawRoundedRect(context, arrowX - shaftLength, shaftY - 1, shaftLength, 2, 1, arrowColor);
        
        // Draw arrow head with three lines for a more defined look
        // Main arrow lines
        context.drawHorizontalLine(arrowX - arrowSize/2, arrowX, arrowY - arrowSize/2, arrowColor);
        context.drawHorizontalLine(arrowX - arrowSize/2, arrowX, arrowY + arrowSize/2, arrowColor);
        
        // Arrow tip
        for (int i = 0; i < arrowSize/2; i++) {
            int tipAlpha = (int)(arrowAlpha * (1.0f - i * 0.3f / (arrowSize/2)));
            int tipColor = (tipAlpha << 24) | (arrowColor & 0xFFFFFF);
            context.drawHorizontalLine(arrowX - i, arrowX - i + 1, arrowY - arrowSize/2 + i, tipColor);
            context.drawHorizontalLine(arrowX - i, arrowX - i + 1, arrowY + arrowSize/2 - i, tipColor);
        }

        // Add subtle glow effect around arrow when hovered
        if (hoverProgress > 0) {
            int glowAlpha = (int)(hoverProgress * 40);
            int glowColor = (glowAlpha << 24) | 0x4A90E2;
            
            // Glow around arrow
            RenderUtil.drawRoundedRect(context, 
                arrowX - shaftLength - 2, 
                arrowY - arrowSize/2 - 2, 
                shaftLength + arrowSize + 4, 
                arrowSize + 4, 
                4, 
                glowColor
            );
        }
    }

    private int interpolateColor(int color1, int color2, float factor) {
        factor = Math.max(0, Math.min(1, factor));
        
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int)(a1 + (a2 - a1) * factor);
        int r = (int)(r1 + (r2 - r1) * factor);
        int g = (int)(g1 + (g2 - g1) * factor);
        int b = (int)(b1 + (b2 - b1) * factor);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
        if (description != null) {
            builder.put(NarrationPart.HINT, description);
        }
    }
} 