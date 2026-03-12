package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.ConfigOption;
import app.qwertz.modernconfig.config.ModernConfigSettings;
import app.qwertz.modernconfig.theme.ModernConfigTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ModernCategory extends AbstractWidget {
    private final Component description;
    private final ResourceLocation icon;
    private final List<Object> elements = new ArrayList<>(); // Can contain ConfigOption<?> or ModernCategory
    private float hoverProgress = 0.0f;
    private float arrowAnimProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();
    private final Consumer<ModernCategory> onClick;
    private final ModernConfigTheme theme;

    public ModernCategory(int x, int y, int width, int height, Component title, Component description, Consumer<ModernCategory> onClick) {
        super(x, y, width, height, title);
        this.description = description;
        this.icon = null;
        this.theme = null;
        this.onClick = onClick;
    }

    /** With optional theme for this mod's config submenus. */
    public ModernCategory(int x, int y, int width, int height, Component title, Component description, ModernConfigTheme theme, Consumer<ModernCategory> onClick) {
        super(x, y, width, height, title);
        this.description = description;
        this.icon = null;
        this.theme = theme;
        this.onClick = onClick;
    }

    public ModernCategory(int x, int y, int width, int height, Component title, Component description, ResourceLocation icon, Consumer<ModernCategory> onClick) {
        super(x, y, width, height, title);
        this.description = description;
        this.icon = icon;
        this.theme = null;
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
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float) ModernConfigSettings.getAnimationDurationMs();
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
            int accent = theme != null ? theme.getAccentColor() : 0x4A90E2;
            int glowColor = (glowAlpha << 24) | (accent & 0xFFFFFF);
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

        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;

        // Calculate text start position considering icon
        int textStartX = getX() + 16;
        int descStartX = getX() + 16;
        if (icon != null) {
            textStartX += 42; // Make room for icon
            descStartX += 42;
        }

        // Title (theme text color)
        int titleColor = theme != null ? theme.getTextColor() : 0xFFFFFFFF;
        int titleY = getY() + 8; // Moved up slightly
      //  if (icon != null) {
        //    titleY = titleY + 8;
      //  }
        context.drawString(textRenderer, getMessage(), textStartX, titleY, titleColor);

        // Draw icon if present
        if (icon != null) {
            int iconSize = 48;
            int iconX = getX() + 6;
            int iconY = getY() + 6; // + (height - iconSize);
            context.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
        }

        // Description (theme secondary text color) – up to 2 lines, then "..." on second line if needed
        int descriptionColor = theme != null ? theme.getTextColorSecondary() : 0xFFAAAAAA;
        int descriptionY = titleY + textRenderer.lineHeight + 2;
        int lineHeight = textRenderer.lineHeight + 2;
        int maxDescWidth = width - 80;
        String fullDesc = description.getString();
        String[] lines = wrapDescriptionToTwoLines(textRenderer, fullDesc, maxDescWidth);
        for (int i = 0; i < lines.length; i++) {
            context.drawString(textRenderer, lines[i], descStartX, descriptionY + i * lineHeight, descriptionColor);
        }

        // Count items in category (moved to right side near arrow)
        int itemCount = elements.size();
        if (itemCount > 0) {
            String countText = itemCount + " item" + (itemCount == 1 ? "" : "s");
            int countColor = theme != null ? theme.getTextColorSecondary() : 0xFF888888;
            int countWidth = textRenderer.width(countText);
            context.drawString(textRenderer, countText, getX() + width - 60 - countWidth, getY() + height/2 - textRenderer.lineHeight/2, countColor);
        }

        // Enhanced arrow design
        drawEnhancedArrow(context, easedProgress, arrowEased);
    }

    private void drawEnhancedArrow(GuiGraphics context, float hoverProgress, float animProgress) {
        int arrowBaseX = getX() + width - 40;
        int arrowBaseY = getY() + height / 2;
        
        int accent = theme != null ? theme.getAccentColor() : 0x4A90E2;
        // Arrow colors with animation
        int arrowAlpha = (int)(128 + hoverProgress * 127);
        int arrowColor = (arrowAlpha << 24) | interpolateColor(0xAAAAAA, accent, hoverProgress);
        
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
        context.hLine(arrowX - arrowSize/2, arrowX, arrowY - arrowSize/2, arrowColor);
        context.hLine(arrowX - arrowSize/2, arrowX, arrowY + arrowSize/2, arrowColor);
        
        // Arrow tip
        for (int i = 0; i < arrowSize/2; i++) {
            int tipAlpha = (int)(arrowAlpha * (1.0f - i * 0.3f / (arrowSize/2)));
            int tipColor = (tipAlpha << 24) | (arrowColor & 0xFFFFFF);
            context.hLine(arrowX - i, arrowX - i + 1, arrowY - arrowSize/2 + i, tipColor);
            context.hLine(arrowX - i, arrowX - i + 1, arrowY + arrowSize/2 - i, tipColor);
        }

        // Add subtle glow effect around arrow when hovered
        if (hoverProgress > 0) {
            int glowAlpha = (int)(hoverProgress * 40);
            int glowColor = (glowAlpha << 24) | (accent & 0xFFFFFF);
            
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

    /** Wrap description into 1 or 2 lines; only add "..." if the second line would still overflow. */
    private static String[] wrapDescriptionToTwoLines(Font textRenderer, String text, int maxDescWidth) {
        if (text == null || text.isEmpty()) {
            return new String[] { "" };
        }
        if (textRenderer.width(text) <= maxDescWidth) {
            return new String[] { text };
        }
        // Find break for first line: prefer last space before we exceed maxDescWidth
        int breakAt = 0;
        int lastSpace = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ' ') {
                lastSpace = i;
            }
            if (textRenderer.width(text.substring(0, i + 1)) > maxDescWidth) {
                breakAt = lastSpace > 0 ? lastSpace : i;
                break;
            }
            breakAt = i + 1;
        }
        String line1 = text.substring(0, breakAt).trim();
        String rest = text.substring(breakAt).trim();
        if (rest.isEmpty()) {
            return new String[] { line1 };
        }
        if (textRenderer.width(rest) <= maxDescWidth) {
            return new String[] { line1, rest };
        }
        // Second line still too long – truncate with "..."
        String line2 = rest;
        while (line2.length() > 0 && textRenderer.width(line2 + "...") > maxDescWidth) {
            line2 = line2.substring(0, line2.length() - 1);
        }
        line2 += "...";
        return new String[] { line1, line2 };
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
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
        if (description != null) {
            builder.add(NarratedElementType.HINT, description);
        }
    }
} 