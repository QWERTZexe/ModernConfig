package app.qwertz.modernconfig.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.function.Consumer;

public class ModernSlider extends ClickableWidget {
    private final double minValue;
    private final double maxValue;
    private double currentValue;
    private final Consumer<Double> onValueChanged;
    private Consumer<Double> onDragComplete;
    private boolean isDragging = false;
    private float alpha = 1.0f;
    private boolean isHovering = false;
    private final int precision; // Number of decimal places
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 50; // 50ms = 0.05 seconds
    
    public ModernSlider(int x, int y, int width, int height, Text message, 
                       double minValue, double maxValue, double currentValue, 
                       int precision, Consumer<Double> onValueChanged) {
        super(x, y, width, height, message);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = Math.max(minValue, Math.min(maxValue, currentValue));
        this.precision = precision;
        this.onValueChanged = onValueChanged;
        this.onDragComplete = null;
    }
    
    public ModernSlider setOnDragComplete(Consumer<Double> onDragComplete) {
        this.onDragComplete = onDragComplete;
        return this;
    }
    
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update hover state
        isHovering = mouseX >= getX() && mouseX <= getX() + getWidth() && 
                    mouseY >= getY() && mouseY <= getY() + getHeight();
        
        // Handle drag simulation with polling
        if (isDragging) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
                lastUpdateTime = currentTime;
                
                // Check if mouse button is still pressed
                boolean isMousePressed = GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                
                if (isMousePressed) {
                    // Update slider position based on current mouse position
                    updateValueFromMouse(mouseX);
                } else {
                    // Stop dragging if mouse released or not hovering
                    isDragging = false;
                    if (onDragComplete != null) {
                        onDragComplete.accept(currentValue);
                    }
                }
            }
        }
        
        // Calculate slider position
        double valuePercent = (currentValue - minValue) / (maxValue - minValue);
        int sliderPos = (int) (getX() + 8 + (getWidth() - 20) * valuePercent);
        
        // Track color changes to yellow on hover
        int trackColor = isHovering ? RenderUtil.applyAlpha(0xFFFFD700, alpha) : RenderUtil.applyAlpha(0xFF444444, alpha);
        int thumbColor = RenderUtil.applyAlpha(0xFFFFFFFF, alpha);
        
        // Draw track background - gray normally, yellow on hover
        RenderUtil.drawRoundedRect(context, getX() + 8, getY() + getHeight() / 2 - 2, 
                                 getWidth() - 16, 4, 2, trackColor);
        
        // Draw thumb - simple white circle that moves
        int thumbSize = 12;
        int thumbY = getY() + getHeight() / 2 - thumbSize / 2;
        
        // Thumb
        RenderUtil.drawRoundedRect(context, sliderPos - thumbSize / 2, thumbY, 
                                 thumbSize, thumbSize, thumbSize / 2, thumbColor);
        
        // Draw label - moved closer to the slider
        try {
            String labelText = getMessage().getString();
            String valueText = formatValue(currentValue);
            String fullText = labelText + ": " + valueText;
            
            int textColor = 0xFFFFFFFF;
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, fullText, getX(), getY() - 2, textColor);
        } catch (Exception e) {
            // Fail silently if text rendering has issues
        }
    }
    
    private String formatValue(double value) {
        if (precision == 0) {
            return String.valueOf((int) Math.round(value));
        } else {
            return String.format("%." + precision + "f", value);
        }
    }
    
        @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= getX() && mouseX <= getX() + getWidth() && 
            mouseY >= getY() && mouseY <= getY() + getHeight()) {
            isDragging = true;
            lastUpdateTime = System.currentTimeMillis(); // Initialize timer
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            // Call drag complete callback to save
            if (onDragComplete != null) {
                onDragComplete.accept(currentValue);
            }
            return true;
        }
        return false;
    }

    // We can remove these since we're using polling instead of drag events
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // No longer needed - using polling approach
        return false;
    }
    
    private void updateValueFromMouse(double mouseX) {
        double trackStart = getX() + 8;
        double trackWidth = getWidth() - 16;
        double percent = Math.max(0, Math.min(1, (mouseX - trackStart) / trackWidth));
        
        double newValue = minValue + (maxValue - minValue) * percent;
        
        // Round to precision
        if (precision == 0) {
            newValue = Math.round(newValue);
        } else {
            double factor = Math.pow(10, precision);
            newValue = Math.round(newValue * factor) / factor;
        }
        
        if (Math.abs(newValue - currentValue) > 0.001) { // Small threshold to prevent excessive updates
            currentValue = newValue;
            if (onValueChanged != null) {
                onValueChanged.accept(currentValue);
            }
        }
    }
    
    public double getValue() {
        return currentValue;
    }
    
    public void setValue(double value) {
        this.currentValue = Math.max(minValue, Math.min(maxValue, value));
    }
    
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
    
    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
} 