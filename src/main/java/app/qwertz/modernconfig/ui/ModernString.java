package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.ModernConfigSettings;
import app.qwertz.modernconfig.theme.ModernConfigTheme;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ModernString extends AbstractWidget {
    private String value;
    private final Consumer<String> onChange;
    private final ModernConfigTheme theme;
    private float animationProgress = 0.0f;
    private boolean focused = false;
    private int cursorPosition = 0;
    private int selectionStart = 0;
    private int selectionEnd = 0;
    private long lastTime = System.currentTimeMillis();
    private float cursorBlink = 0.0f;
    private final int maxLength;

    public ModernString(int x, int y, int width, int height, Component text, String initial, Consumer<String> onChange) {
        this(x, y, width, height, text, initial, onChange, 32, null);
    }

    public ModernString(int x, int y, int width, int height, Component text, String initial, Consumer<String> onChange, int maxLength) {
        this(x, y, width, height, text, initial, onChange, maxLength, null);
    }

    public ModernString(int x, int y, int width, int height, Component text, String initial, Consumer<String> onChange, int maxLength, ModernConfigTheme theme) {
        super(x, y, width, height, text);
        this.value = initial != null ? initial : "";
        this.onChange = onChange;
        this.maxLength = maxLength;
        this.theme = theme;
        this.cursorPosition = this.value.length();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!focused) return false;
        int keyCode = event.key();

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
    public boolean charTyped(CharacterEvent event) {
        if (!focused) return false;
        if (value.length() >= maxLength) return false;
        int cp = event.codepoint();
        if (Character.isLetterOrDigit(cp) || cp == '_' || cp == '-' || cp == '.' || cp == ' ' || cp == '#') {
            String ch = Character.toString(cp);
            value = value.substring(0, cursorPosition) + ch + value.substring(cursorPosition);
            cursorPosition += ch.length();
            onChange.accept(value);
            return true;
        }
        return false;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float) ModernConfigSettings.getAnimationDurationMs();
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
        
        // Draw outline (accent when themed)
        int outlineLo = 0x33FFFFFF;
        int outlineHi = 0x77FFFFFF;
        if (theme != null && easedProgress > 0) {
            outlineHi = (0x77 << 24) | (theme.getAccentColor() & 0xFFFFFF);
        }
        int outlineColor = RenderUtil.interpolateColor(outlineLo, outlineHi, easedProgress);
        RenderUtil.drawRoundedRect(context, getX(), getY(), getWidth(), 1, 0, outlineColor); // top
        RenderUtil.drawRoundedRect(context, getX(), getY() + getHeight() - 1, getWidth(), 1, 0, outlineColor); // bottom
        RenderUtil.drawRoundedRect(context, getX(), getY(), 1, getHeight(), 0, outlineColor); // left
        RenderUtil.drawRoundedRect(context, getX() + getWidth() - 1, getY(), 1, getHeight(), 0, outlineColor); // right

        int textColor = theme != null ? theme.getTextColor() : 0xFFFFFFFF;
        // Draw label
        float textY = getY() + (getHeight() - 8) / 2.0f;
        context.drawString(
            Minecraft.getInstance().font,
            getMessage(),
            getX() + 8,
            (int)textY,
            textColor
        );

        // Calculate text position
        int labelWidth = Minecraft.getInstance().font.width(getMessage()) + 16;
        int textX = getX() + labelWidth;
        int maxTextWidth = getWidth() - labelWidth - 8;

        // Draw text
        String visibleText = Minecraft.getInstance().font.plainSubstrByWidth(value, maxTextWidth);
        context.drawString(
            Minecraft.getInstance().font,
            visibleText,
            textX,
            (int)textY,
            textColor
        );

        // Draw cursor (ensure full opacity - theme accent may be RGB only)
        if (focused && cursorBlink < 1.0f) {
            String textBeforeCursor = value.substring(0, cursorPosition);
            int cursorX = textX + Minecraft.getInstance().font.width(textBeforeCursor);
            int cursorColor = theme != null ? (0xFF000000 | (theme.getAccentColor() & 0xFFFFFF)) : 0xFFFFFFFF;
            context.fill(cursorX, (int)textY - 1, cursorX + 1, (int)textY + 9, cursorColor);
        }
    }

    private void applyClickFocusAndCursor(double mouseX, double mouseY) {
        focused = true;
        int labelWidth = Minecraft.getInstance().font.width(getMessage()) + 16;
        int textX = getX() + labelWidth;
        int relativeX = (int) mouseX - textX;
        String visibleText = value;
        cursorPosition = 0;
        int currentWidth = 0;
        for (int i = 0; i < visibleText.length(); i++) {
            int charWidth = Minecraft.getInstance().font.width(String.valueOf(visibleText.charAt(i)));
            if (Math.abs(currentWidth - relativeX) > Math.abs(currentWidth + charWidth - relativeX)) {
                cursorPosition = i + 1;
            }
            currentWidth += charWidth;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0 && mouseX >= getX() && mouseX <= getX() + getWidth() &&
            mouseY >= getY() && mouseY <= getY() + getHeight()) {
            applyClickFocusAndCursor(mouseX, mouseY);
            return true;
        }
        focused = false;
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
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