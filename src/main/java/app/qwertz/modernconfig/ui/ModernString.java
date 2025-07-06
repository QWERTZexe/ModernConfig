package app.qwertz.modernconfig.ui;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.MinecraftClient;

import java.util.function.Consumer;

public class ModernString extends ClickableWidget {
    private String value;
    private final Consumer<String> onChange;
    private float animationProgress = 0.0f;
    private boolean focused = false;
    private int cursorPosition = 0;
    private int selectionStart = 0;
    private int selectionEnd = 0;
    private static final int ANIMATION_DURATION = 200; // milliseconds
    private long lastTime = System.currentTimeMillis();
    private float cursorBlink = 0.0f;
    private final int maxLength;

    public ModernString(int x, int y, int width, int height, Text text, String initial, Consumer<String> onChange) {
        this(x, y, width, height, text, initial, onChange, 32);
    }

    public ModernString(int x, int y, int width, int height, Text text, String initial, Consumer<String> onChange, int maxLength) {
        super(x, y, width, height, text);
        this.value = initial != null ? initial : "";
        this.onChange = onChange;
        this.maxLength = maxLength;
        this.cursorPosition = this.value.length();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        switch (keyCode) {
            case 259: // Backspace
                if (cursorPosition > 0) {
                    value = value.substring(0, cursorPosition - 1) + value.substring(cursorPosition);
                    cursorPosition--;
                    onChange.accept(value);
                }
                return true;
            case 261: // Delete
                if (cursorPosition < value.length()) {
                    value = value.substring(0, cursorPosition) + value.substring(cursorPosition + 1);
                    onChange.accept(value);
                }
                return true;
            case 262: // Right
                if (cursorPosition < value.length()) {
                    cursorPosition++;
                }
                return true;
            case 263: // Left
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
                return true;
            case 268: // Home
                cursorPosition = 0;
                return true;
            case 269: // End
                cursorPosition = value.length();
                return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;
        if (value.length() >= maxLength) return false;

        if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == '.' || chr == ' ' || chr == '#') {
            value = value.substring(0, cursorPosition) + chr + value.substring(cursorPosition);
            cursorPosition++;
            onChange.accept(value);
            return true;
        }
        return false;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float)ANIMATION_DURATION;
        lastTime = currentTime;

        if (isHovered() || focused) {
            animationProgress = Math.min(1.0f, animationProgress + deltaTime * 2);
        } else {
            animationProgress = Math.max(0.0f, animationProgress - deltaTime * 2);
        }

        cursorBlink = (cursorBlink + delta * 0.05f) % 2.0f;
        float easedProgress = RenderUtil.easeOutExpo(animationProgress);
        
        // Draw background
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

        // Draw label
        float textY = getY() + (getHeight() - 8) / 2.0f;
        context.drawTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            getMessage(),
            getX() + 8,
            (int)textY,
            0xFFFFFFFF
        );

        // Calculate text position
        int labelWidth = MinecraftClient.getInstance().textRenderer.getWidth(getMessage()) + 16;
        int textX = getX() + labelWidth;
        int maxTextWidth = getWidth() - labelWidth - 8;

        // Draw text
        String visibleText = MinecraftClient.getInstance().textRenderer.trimToWidth(value, maxTextWidth);
        context.drawTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            visibleText,
            textX,
            (int)textY,
            0xFFFFFFFF
        );

        // Draw cursor
        if (focused && cursorBlink < 1.0f) {
            String textBeforeCursor = value.substring(0, cursorPosition);
            int cursorX = textX + MinecraftClient.getInstance().textRenderer.getWidth(textBeforeCursor);
            context.fill(cursorX, (int)textY - 1, cursorX + 1, (int)textY + 9, 0xFFFFFFFF);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        focused = true;
        
        // Calculate text position for cursor placement
        int labelWidth = MinecraftClient.getInstance().textRenderer.getWidth(getMessage()) + 16;
        int textX = getX() + labelWidth;
        
        // Find closest character position to click
        int relativeX = (int)mouseX - textX;
        String visibleText = value;
        cursorPosition = 0;
        
        int currentWidth = 0;
        for (int i = 0; i < visibleText.length(); i++) {
            int charWidth = MinecraftClient.getInstance().textRenderer.getWidth(String.valueOf(visibleText.charAt(i)));
            if (Math.abs(currentWidth - relativeX) > Math.abs(currentWidth + charWidth - relativeX)) {
                cursorPosition = i + 1;
            }
            currentWidth += charWidth;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clicked = super.mouseClicked(mouseX, mouseY, button);
        if (!clicked) {
            focused = false;
        }
        return clicked;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (value == null) {
            this.value = "";
        } else if (value.length() > maxLength) {
            this.value = value.substring(0, maxLength);
        } else {
            this.value = value;
        }
        this.cursorPosition = this.value.length();
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }
} 