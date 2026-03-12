package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.ModernConfigSettings;
import app.qwertz.modernconfig.theme.ModernConfigTheme;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ModernColorPicker extends AbstractWidget {
    private int currentColor;
    private final Consumer<Integer> onColorChanged;
    private Consumer<Integer> onColorComplete;
    private boolean isExpanded = false;
    private boolean isDragging = false;
    private boolean isDraggingHue = false;
    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float brightness = 1.0f;
    private float alpha = 1.0f;
    
    // Color picker dimensions
    private static final int PICKER_WIDTH = 200;
    private static final int PICKER_HEIGHT = 150;
    private static final int HUE_BAR_WIDTH = 20;
    private static final int HUE_BAR_HEIGHT = PICKER_HEIGHT;
    private static final int SWATCH_SIZE = 20;
    private static final int COLLAPSED_HEIGHT = 30;
    private static final int EXPANDED_HEIGHT = PICKER_HEIGHT + 50; // picker + padding + preview
    /** Min width so expanded picker content isn't clipped (picker + hue bar + padding + preview). */
    private static final int EXPANDED_CONTENT_WIDTH = PICKER_WIDTH + HUE_BAR_WIDTH + 50;
    
    // Performance optimization - cache rendered gradients
    private int[][] saturationBrightnessCache;
    private int[] hueBarCache;
    private float lastCachedHue = -1;
    
    // Hex input field
    private ModernString hexInput;
    private boolean isTypingHex = false;
    private final ModernConfigTheme theme;
    
    // Expand/collapse animation (0 = collapsed, 1 = expanded)
    private float expandProgress = 0.0f;
    private long lastExpandTime = System.currentTimeMillis();

    // Static list to track all color pickers for global collapse
    private static final List<ModernColorPicker> allColorPickers = new ArrayList<>();
    
    public ModernColorPicker(int x, int y, int width, int height, Component message, 
                            int currentColor, Consumer<Integer> onColorChanged) {
        this(x, y, width, height, message, currentColor, onColorChanged, null);
    }
    
    public ModernColorPicker(int x, int y, int width, int height, Component message, 
                            int currentColor, Consumer<Integer> onColorChanged, ModernConfigTheme theme) {
        super(x, y, width, height, message);
        this.currentColor = currentColor;
        this.onColorChanged = onColorChanged;
        this.theme = theme;
        updateHSVFromColor();
        
        // Initialize caches for performance (much smaller cache, scaled up when rendering)
        saturationBrightnessCache = new int[PICKER_WIDTH / 4][PICKER_HEIGHT / 4];
        hueBarCache = new int[HUE_BAR_HEIGHT / 4];
        
        // Initialize hex input
        hexInput = new ModernString(0, 0, 90, 20,
            Component.literal("Hex"), String.format("#%06X", currentColor),
            value -> onHexInputChanged(value), 7, theme);
        
        // Pre-calculate hue bar once (never changes)
        precalculateHueBar();
        
        // Register this color picker for global collapse functionality
        allColorPickers.add(this);
    }
    
    public ModernColorPicker setOnColorComplete(Consumer<Integer> onColorComplete) {
        this.onColorComplete = onColorComplete;
        return this;
    }
    
    @Override
    public int getHeight() {
        float eased = RenderUtil.easeInOutQuad(expandProgress);
        return COLLAPSED_HEIGHT + (int) ((EXPANDED_HEIGHT - COLLAPSED_HEIGHT) * eased);
    }
    
    public int getMainHeight() {
        return COLLAPSED_HEIGHT;
    }
    
    public boolean isExpanded() {
        return isExpanded;
    }
    
    private void updateHSVFromColor() {
        float[] hsv = new float[3];
        Color.RGBtoHSB(
            (currentColor >> 16) & 0xFF,
            (currentColor >> 8) & 0xFF,
            currentColor & 0xFF,
            hsv
        );
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.brightness = hsv[2];
    }
    
    private void updateColorFromHSV() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        currentColor = rgb & 0xFFFFFF;
        if (onColorChanged != null) {
            onColorChanged.accept(currentColor);
        }
        
        // Update hex input if not currently typing
        if (!isTypingHex) {
            hexInput.setValue(String.format("#%06X", currentColor));
        }
    }
    
    private void onHexInputChanged(String value) {
        isTypingHex = true;
        String hex = value.trim();
        
        // Remove # if present
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        
        // Validate hex format
        if (hex.matches("^[0-9A-Fa-f]{6}$")) {
            try {
                int newColor = Integer.parseInt(hex, 16);
                currentColor = newColor;
                updateHSVFromColor();
                
                if (onColorChanged != null) {
                    onColorChanged.accept(currentColor);
                }
            } catch (NumberFormatException e) {
                // Invalid hex, ignore
            }
        }
        
        isTypingHex = false;
    }
    
    private void precalculateHueBar() {
        for (int y = 0; y < hueBarCache.length; y++) {
            float h = (float) y / (hueBarCache.length - 1);
            hueBarCache[y] = Color.HSBtoRGB(h, 1.0f, 1.0f);
        }
    }
    
    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        int durationMs = Math.max(1, ModernConfigSettings.getAnimationDurationMs());
        float deltaTime = (currentTime - lastExpandTime) / (float) durationMs;
        lastExpandTime = currentTime;

        float oldExpandProgress = expandProgress;
        if (isExpanded) {
            expandProgress = Math.min(1.0f, expandProgress + deltaTime);
        } else {
            expandProgress = Math.max(0.0f, expandProgress - deltaTime);
        }
        if (Math.abs(oldExpandProgress - expandProgress) > 0.01f) {
            updateParentLayout();
        }

        // Draw label
        String labelText = getMessage().getString();
        int textColor = theme != null ? theme.getTextColor() : 0xFFFFFFFF;
        context.drawString(Minecraft.getInstance().font, labelText + ":", getX(), getY() - 2, textColor);
        
        // Draw color swatch button
        int swatchX = getX();
        int swatchY = getY() + 12;
        
        // Hover outline first, outside only (then swatch drawn on top so outline doesn't overlap the color)
        if (isHovered()) {
            int hoverAlpha = theme != null ? (0x50 << 24) | (theme.getAccentColor() & 0xFFFFFF) : 0x40FFFFFF;
            RenderUtil.drawRoundedRect(context, swatchX - 1, swatchY - 1, SWATCH_SIZE + 2, SWATCH_SIZE + 2, 4, hoverAlpha);
        }
        
        // Background with border (neutral black), then color on top
        RenderUtil.drawRoundedRect(context, swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, 3, 0xFF000000);
        RenderUtil.drawRoundedRect(context, swatchX + 1, swatchY + 1, SWATCH_SIZE - 2, SWATCH_SIZE - 2, 2, 0xFF000000 | currentColor);
        
        // Draw expand button (arrow)
        int expandX = swatchX + SWATCH_SIZE + 5;
        int expandY = swatchY + 2;
        String expandText = isExpanded ? "▲" : "▼";
        context.drawString(Minecraft.getInstance().font, expandText, expandX, expandY, textColor);
        
        // Position and render hex input (always visible to the right)
        int hexInputX = expandX + 20;
        int hexInputY = swatchY - 2;
        hexInput.setX(hexInputX);
        hexInput.setY(hexInputY);
        hexInput.setWidth(90);
        hexInput.render(context, mouseX, mouseY, delta);
        
        // Draw color picker with expand/collapse animation (clip to current height, same coords as container: y down)
        if (expandProgress > 0.001f) {
            int clipX = getX();
            int clipY = getY();
            int clipW = Math.max(getWidth(), EXPANDED_CONTENT_WIDTH);
            int clipH = getHeight();
            context.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);
            drawColorPicker(context, mouseX, mouseY, delta);
            context.disableScissor();
        }
    }
    
    private void drawColorPicker(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int pickerX = getX();
        int pickerY = getY() + 40;
        
        // Background
        RenderUtil.drawRoundedRect(context, pickerX - 5, pickerY - 5, PICKER_WIDTH + HUE_BAR_WIDTH + 15, PICKER_HEIGHT + 10, 5, 0xE0202020);
        
        // Draw saturation/brightness picker
        drawSaturationBrightnessPicker(context, pickerX, pickerY);
        
        // Draw hue bar
        drawHueBar(context, pickerX + PICKER_WIDTH + 5, pickerY);
        
        // Draw current color preview
        int previewX = pickerX + PICKER_WIDTH + HUE_BAR_WIDTH + 10;
        int previewY = pickerY;
        RenderUtil.drawRoundedRect(context, previewX, previewY, 30, 30, 3, 0xFF000000);
        RenderUtil.drawRoundedRect(context, previewX + 1, previewY + 1, 28, 28, 2, 0xFF000000 | currentColor);
        
        // Just show the color preview, hex input is always visible above
    }
    
    private void drawSaturationBrightnessPicker(GuiGraphics context, int startX, int startY) {
        // Cache the gradient if hue changed (much smaller cache for performance)
        if (Math.abs(lastCachedHue - hue) > 0.001f) {
            int cacheWidth = saturationBrightnessCache.length;
            int cacheHeight = saturationBrightnessCache[0].length;
            
            for (int x = 0; x < cacheWidth; x++) {
                for (int y = 0; y < cacheHeight; y++) {
                    float s = (float) x / (cacheWidth - 1);
                    float b = 1.0f - (float) y / (cacheHeight - 1);
                    saturationBrightnessCache[x][y] = Color.HSBtoRGB(hue, s, b);
                }
            }
            lastCachedHue = hue;
        }
        
        // Draw the cached gradient in 4x4 blocks (much more efficient)
        int blockSize = 4;
        int cacheWidth = saturationBrightnessCache.length;
        int cacheHeight = saturationBrightnessCache[0].length;
        
        for (int x = 0; x < cacheWidth; x++) {
            for (int y = 0; y < cacheHeight; y++) {
                int color = saturationBrightnessCache[x][y];
                int alpha = 0xFF000000;
                
                int pixelX = startX + x * blockSize;
                int pixelY = startY + y * blockSize;
                
                // Draw 4x4 blocks instead of individual pixels
                context.fill(pixelX, pixelY, pixelX + blockSize, pixelY + blockSize, alpha | color);
            }
        }
        
        // Draw selection indicator (theme accent circle)
        int indicatorX = (int) (startX + saturation * PICKER_WIDTH);
        int indicatorY = (int) (startY + (1.0f - brightness) * PICKER_HEIGHT);
        int accent = theme != null ? theme.getAccentColor() : 0xFFFFFFFF;
        RenderUtil.drawRoundedRect(context, indicatorX - 4, indicatorY - 4, 8, 8, 4, 0xFF000000);
        RenderUtil.drawRoundedRect(context, indicatorX - 3, indicatorY - 3, 6, 6, 3, 0xFF000000 | (accent & 0xFFFFFF));
    }
    
    private void drawHueBar(GuiGraphics context, int startX, int startY) {
        // Use pre-calculated hue gradient (much more efficient with larger blocks)
        int blockSize = HUE_BAR_HEIGHT / hueBarCache.length;
        for (int y = 0; y < hueBarCache.length; y++) {
            int color = hueBarCache[y];
            int alpha = 0xFF000000;
            
            int pixelY = startY + y * blockSize;
            context.fill(startX, pixelY, startX + HUE_BAR_WIDTH, pixelY + blockSize, alpha | color);
        }
        
        // Draw selection indicator (theme accent)
        int indicatorY = (int) (startY + hue * HUE_BAR_HEIGHT);
        int accent = theme != null ? theme.getAccentColor() : 0xFFFFFFFF;
        context.fill(startX - 2, indicatorY - 1, startX + HUE_BAR_WIDTH + 2, indicatorY + 1, 0xFF000000);
        context.fill(startX - 1, indicatorY, startX + HUE_BAR_WIDTH + 1, indicatorY + 1, 0xFF000000 | (accent & 0xFFFFFF));
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();
            int swatchX = getX();
            int swatchY = getY() + 12;

            if (hexInput.mouseClicked(event, doubled)) {
                return true;
            }

            if (mouseX >= swatchX && mouseX <= swatchX + SWATCH_SIZE + 20 &&
                mouseY >= swatchY && mouseY <= swatchY + SWATCH_SIZE) {
                // Collapse all other color pickers first
                collapseAllExcept(this);
                isExpanded = !isExpanded;
                // Force container to update layout when expanding/collapsing
                updateParentLayout();
                return true;
            }
            
            // Check if clicking on color picker (when expanded)
            if (isExpanded) {
                int pickerX = getX();
                int pickerY = getY() + 40;
                
                // Check saturation/brightness picker
                if (mouseX >= pickerX && mouseX <= pickerX + PICKER_WIDTH &&
                    mouseY >= pickerY && mouseY <= pickerY + PICKER_HEIGHT) {
                    isDragging = true;
                    updateSaturationBrightness(mouseX, mouseY);
                    return true;
                }
                
                // Check hue bar
                int hueBarX = pickerX + PICKER_WIDTH + 5;
                if (mouseX >= hueBarX && mouseX <= hueBarX + HUE_BAR_WIDTH &&
                    mouseY >= pickerY && mouseY <= pickerY + HUE_BAR_HEIGHT) {
                    isDraggingHue = true;
                    updateHue(mouseY);
                    return true;
                }
                
                // Check color preview area
                int previewX = pickerX + PICKER_WIDTH + HUE_BAR_WIDTH + 10;
                int previewY = pickerY;
                if (mouseX >= previewX && mouseX <= previewX + 30 &&
                    mouseY >= previewY && mouseY <= previewY + 30) {
                    return true; // Consume click on preview area
                }
                
                // Check if clicking on the background area of the color picker (the dark rectangle)
                if (mouseX >= pickerX - 5 && mouseX <= pickerX + PICKER_WIDTH + HUE_BAR_WIDTH + 45 &&
                    mouseY >= pickerY - 5 && mouseY <= pickerY + PICKER_HEIGHT + 10) {
                    return true; // Consume click on background to prevent closing
                }
                
                // If click is outside the color picker area but within our widget bounds,
                // close the picker and let the click pass through to components below
                if (mouseY > getY() + COLLAPSED_HEIGHT) {
                    isExpanded = false;
                    updateParentLayout();
                }
                
                // Let the click pass through to components below
                return false;
            }
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && (isDragging || isDraggingHue)) {
            isDragging = false;
            isDraggingHue = false;
            if (onColorComplete != null) {
                onColorComplete.accept(currentColor);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (event.button() == 0 && isDragging) {
            updateSaturationBrightness(event.x(), event.y());
            return true;
        }
        if (event.button() == 0 && isDraggingHue) {
            updateHue(event.y());
            return true;
        }
        return false;
    }
    
    private void updateSaturationBrightness(double mouseX, double mouseY) {
        int pickerX = getX();
        int pickerY = getY() + 40;
        
        saturation = Mth.clamp((float) (mouseX - pickerX) / PICKER_WIDTH, 0.0f, 1.0f);
        brightness = Mth.clamp(1.0f - (float) (mouseY - pickerY) / PICKER_HEIGHT, 0.0f, 1.0f);
        
        updateColorFromHSV();
    }
    
    private void updateHue(double mouseY) {
        int pickerY = getY() + 40;
        
        hue = Mth.clamp((float) (mouseY - pickerY) / HUE_BAR_HEIGHT, 0.0f, 1.0f);
        
        updateColorFromHSV();
    }
    
    public int getColor() {
        return currentColor;
    }
    
    public void setColor(int color) {
        this.currentColor = color & 0xFFFFFF;
        updateHSVFromColor();
    }
    
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
    
    private void updateParentLayout() {
        // Force the container to update layout by accessing the static container
        // This is needed when the color picker expands/collapses to reposition other elements
        try {
            ModernContainer container = ConfigScreen.getTLContainer();
            if (container != null) {
                container.updateLayout();
            }
        } catch (Exception e) {
            // If we can't access the container, the layout will update on next render
        }
    }
    
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (hexInput.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (hexInput.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    /**
     * Collapse all expanded color pickers except the specified one (can be null to collapse all)
     */
    public static void collapseAllExcept(ModernColorPicker except) {
        for (ModernColorPicker picker : allColorPickers) {
            if (picker != except && picker.isExpanded) {
                picker.isExpanded = false;
                picker.updateParentLayout();
            }
        }
    }
    
    /**
     * Collapse all expanded color pickers
     */
    public static void collapseAll() {
        collapseAllExcept(null);
    }
    
    /**
     * Remove this color picker from the global list (call when destroying)
     */
    public void remove() {
        allColorPickers.remove(this);
    }

    /** Clear focus from the hex input so only one input has focus at a time. */
    public void clearFocus() {
        clearFocus(true);
    }
    
    /** Clear focus with option to skip layout update (for batching). */
    public void clearFocus(boolean updateLayout) {
        if (hexInput != null) {
            hexInput.setFocused(false);
        }
        if (isExpanded) {
            isExpanded = false;
            if (updateLayout) {
                updateParentLayout();
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }
} 