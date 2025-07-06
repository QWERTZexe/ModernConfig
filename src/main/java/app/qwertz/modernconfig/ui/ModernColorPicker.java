package app.qwertz.modernconfig.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModernColorPicker extends ClickableWidget {
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
    
    // Performance optimization - cache rendered gradients
    private int[][] saturationBrightnessCache;
    private int[] hueBarCache;
    private float lastCachedHue = -1;
    
    // Hex input field
    private ModernString hexInput;
    private boolean isTypingHex = false;
    
    // Static list to track all color pickers for global collapse
    private static final List<ModernColorPicker> allColorPickers = new ArrayList<>();
    
    public ModernColorPicker(int x, int y, int width, int height, Text message, 
                            int currentColor, Consumer<Integer> onColorChanged) {
        super(x, y, width, height, message);
        this.currentColor = currentColor;
        this.onColorChanged = onColorChanged;
        updateHSVFromColor();
        
        // Initialize caches for performance (much smaller cache, scaled up when rendering)
        saturationBrightnessCache = new int[PICKER_WIDTH / 4][PICKER_HEIGHT / 4];
        hueBarCache = new int[HUE_BAR_HEIGHT / 4];
        
        // Initialize hex input
        hexInput = new ModernString(0, 0, 80, 20, 
            Text.literal("Hex"), String.format("#%06X", currentColor),
            value -> onHexInputChanged(value), 7);
        
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
        return isExpanded ? EXPANDED_HEIGHT : COLLAPSED_HEIGHT;
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
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw label
        String labelText = getMessage().getString();
        
        int textColor = 0xFFFFFFFF;
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, labelText + ":", getX(), getY() - 2, textColor);
        
        // Draw color swatch button
        int swatchX = getX();
        int swatchY = getY() + 12;
        
        // Background with border
        RenderUtil.drawRoundedRect(context, swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, 3, 0xFF000000);
        RenderUtil.drawRoundedRect(context, swatchX + 1, swatchY + 1, SWATCH_SIZE - 2, SWATCH_SIZE - 2, 2, 0xFF000000 | currentColor);
        
        // Hover effect
        if (isHovered()) {
            RenderUtil.drawRoundedRect(context, swatchX - 1, swatchY - 1, SWATCH_SIZE + 2, SWATCH_SIZE + 2, 4, 0x40FFFFFF);
        }
        
        // Draw expand button
        int expandX = swatchX + SWATCH_SIZE + 5;
        int expandY = swatchY + 2;
        String expandText = isExpanded ? "▲" : "▼";
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, expandText, expandX, expandY, 0xFFFFFFFF);
        
        // Position and render hex input (always visible to the right)
        int hexInputX = expandX + 20;
        int hexInputY = swatchY - 2;
        hexInput.setX(hexInputX);
        hexInput.setY(hexInputY);
        hexInput.setWidth(80);
        hexInput.render(context, mouseX, mouseY, delta);
        
        // Draw color picker if expanded
        if (isExpanded) {
            drawColorPicker(context, mouseX, mouseY, delta);
        }
    }
    
    private void drawColorPicker(DrawContext context, int mouseX, int mouseY, float delta) {
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
    
    private void drawSaturationBrightnessPicker(DrawContext context, int startX, int startY) {
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
        
        // Draw selection indicator
        int indicatorX = (int) (startX + saturation * PICKER_WIDTH);
        int indicatorY = (int) (startY + (1.0f - brightness) * PICKER_HEIGHT);
        
        // Draw white circle with black border
        RenderUtil.drawRoundedRect(context, indicatorX - 4, indicatorY - 4, 8, 8, 4, 0xFF000000);
        RenderUtil.drawRoundedRect(context, indicatorX - 3, indicatorY - 3, 6, 6, 3, 0xFFFFFFFF);
    }
    
    private void drawHueBar(DrawContext context, int startX, int startY) {
        // Use pre-calculated hue gradient (much more efficient with larger blocks)
        int blockSize = HUE_BAR_HEIGHT / hueBarCache.length;
        for (int y = 0; y < hueBarCache.length; y++) {
            int color = hueBarCache[y];
            int alpha = 0xFF000000;
            
            int pixelY = startY + y * blockSize;
            context.fill(startX, pixelY, startX + HUE_BAR_WIDTH, pixelY + blockSize, alpha | color);
        }
        
        // Draw selection indicator
        int indicatorY = (int) (startY + hue * HUE_BAR_HEIGHT);
        
        // Draw white line with black border
        context.fill(startX - 2, indicatorY - 1, startX + HUE_BAR_WIDTH + 2, indicatorY + 1, 0xFF000000);
        context.fill(startX - 1, indicatorY, startX + HUE_BAR_WIDTH + 1, indicatorY + 1, 0xFFFFFFFF);
    }
    
        @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int swatchX = getX();
            int swatchY = getY() + 12;
            int expandX = swatchX + SWATCH_SIZE + 5;
            int hexInputX = expandX + 20;
            
            // Check hex input first (always visible)
            if (hexInput.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            
            // Check if clicking on swatch or expand button
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (isDragging || isDraggingHue)) {
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && isDragging) {
            updateSaturationBrightness(mouseX, mouseY);
            return true;
        }
        if (button == 0 && isDraggingHue) {
            updateHue(mouseY);
            return true;
        }
        return false;
    }
    
    private void updateSaturationBrightness(double mouseX, double mouseY) {
        int pickerX = getX();
        int pickerY = getY() + 40;
        
        saturation = MathHelper.clamp((float) (mouseX - pickerX) / PICKER_WIDTH, 0.0f, 1.0f);
        brightness = MathHelper.clamp(1.0f - (float) (mouseY - pickerY) / PICKER_HEIGHT, 0.0f, 1.0f);
        
        updateColorFromHSV();
    }
    
    private void updateHue(double mouseY) {
        int pickerY = getY() + 40;
        
        hue = MathHelper.clamp((float) (mouseY - pickerY) / HUE_BAR_HEIGHT, 0.0f, 1.0f);
        
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hexInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (hexInput.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
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

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
} 