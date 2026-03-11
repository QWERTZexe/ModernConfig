package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.ModernConfigSettings;
import app.qwertz.modernconfig.config.ListConfigOption;
import app.qwertz.modernconfig.theme.ModernConfigTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class ModernListWidget extends ClickableWidget {
    private static final int LIST_HEADER_HEIGHT = 25;
    private static final int LIST_CONTENT_GAP = 2;

    private final ModernList modernList;
    private final boolean expandable;
    private boolean isExpanded = false;
    private float expandProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();

    public ModernListWidget(int x, int y, int width, ListConfigOption option, Text description) {
        this(x, y, width, option, description, null);
    }

    public ModernListWidget(int x, int y, int width, ListConfigOption option, Text description, ModernConfigTheme theme) {
        super(x, y, width, 30, description);
        this.expandable = option.isExpandable();
        this.modernList = new ModernList(option, theme);
        this.modernList.setParentWidget(this);
        if (expandable) {
            this.modernList.setDrawHeader(false);
        }
        updatePosition();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updatePosition();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updatePosition();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        updatePosition();
    }

    private void updatePosition() {
        if (expandable) {
            modernList.setPosition(getX(), getY() + LIST_HEADER_HEIGHT + LIST_CONTENT_GAP, getWidth());
        } else {
            modernList.setPosition(getX(), getY(), getWidth());
        }
    }

    public int getMainHeight() {
        return expandable ? LIST_HEADER_HEIGHT : modernList.getHeight();
    }

    @Override
    public int getHeight() {
        if (!expandable) {
            return modernList.getHeight();
        }
        if (expandProgress > 0.001f) {
            int contentHeight = modernList.getHeight();
            return LIST_HEADER_HEIGHT + LIST_CONTENT_GAP + (int) (contentHeight * expandProgress);
        }
        return LIST_HEADER_HEIGHT;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    private void updateParentLayout() {
        try {
            ModernContainer container = ConfigScreen.getTLContainer();
            if (container != null) {
                container.updateLayout();
            }
        } catch (Exception ignored) {}
    }

    /** Draw a small "list" icon (stacked lines) so collapsed state looks like a list, not a dropdown. */
    private void drawListIcon(DrawContext context, int x, int y, int color) {
        int w = 10;
        int h = 2;
        context.fill(x, y, x + w, y + h, color);
        context.fill(x, y + 4, x + w, y + 4 + h, color);
        context.fill(x, y + 8, x + w, y + 8 + h, color);
    }

    private void drawListArrow(DrawContext context, int x, int y, boolean expanded, int color) {
        if (expanded) {
            context.fill(x - 3, y + 1, x + 4, y + 2, color);
            context.fill(x - 2, y, x + 3, y + 1, color);
            context.fill(x - 1, y - 1, x + 2, y, color);
        } else {
            context.fill(x - 3, y - 1, x + 4, y, color);
            context.fill(x - 2, y, x + 3, y + 1, color);
            context.fill(x - 1, y + 1, x + 2, y + 2, color);
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        updatePosition();

        if (!expandable) {
            modernList.render(context, mouseX, mouseY, delta);
            return;
        }

        long currentTime = System.currentTimeMillis();
        int durationMs = Math.max(1, ModernConfigSettings.getAnimationDurationMs());
        float deltaTime = (currentTime - lastTime) / (float) durationMs;
        lastTime = currentTime;

        float oldExpandProgress = expandProgress;
        if (isExpanded) {
            expandProgress = Math.min(1.0f, expandProgress + deltaTime * 2);
        } else {
            expandProgress = Math.max(0.0f, expandProgress - deltaTime * 2);
        }
        if (Math.abs(oldExpandProgress - expandProgress) > 0.01f) {
            updateParentLayout();
        }

        int textColor = 0xFFFFFFFF;
        int mutedColor = 0xFF888888;
        int arrowColor = 0xFFAAAAAA;
        RenderUtil.drawRoundedRect(context, getX(), getY(), getWidth(), LIST_HEADER_HEIGHT, 4, 0xFF2A2A2A);
        int left = getX() + 8;
        int textY = getY() + (LIST_HEADER_HEIGHT - 8) / 2;
        drawListIcon(context, left, textY - 1, mutedColor);
        left += 14;
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), left, textY, textColor);
        int n = modernList.getOption().getValue().size();
        String countText = n == 1 ? "1 item" : n + " items";
        int countWidth = MinecraftClient.getInstance().textRenderer.getWidth(countText);
        int arrowX = getX() + getWidth() - 16;
        int arrowY = getY() + LIST_HEADER_HEIGHT / 2;
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, countText, arrowX - countWidth - 8, textY, mutedColor);
        drawListArrow(context, arrowX, arrowY, isExpanded, arrowColor);

        if (expandProgress > 0.001f) {
            int clipX = getX();
            int clipY = getY();
            int clipW = getWidth();
            int clipH = getHeight();
            context.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);
            modernList.render(context, mouseX, mouseY, delta);
            context.disableScissor();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (expandable) {
            boolean inHeader = mouseX >= getX() && mouseX <= getX() + getWidth()
                && mouseY >= getY() && mouseY <= getY() + LIST_HEADER_HEIGHT;
            if (inHeader) {
                isExpanded = !isExpanded;
                updateParentLayout();
                MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
            if (isExpanded && mouseY >= getY() + LIST_HEADER_HEIGHT && mouseY <= getY() + getHeight()) {
                return modernList.mouseClicked(mouseX, mouseY, button);
            }
            return false;
        }
        return modernList.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return modernList.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return modernList.charTyped(chr, modifiers);
    }

    public ModernList getModernList() {
        return modernList;
    }

    public void clearFocus() {
        modernList.clearFocus();
        if (expandable && isExpanded) {
            isExpanded = false;
            updateParentLayout();
        }
    }

    public void clearFocus(boolean updateLayout) {
        modernList.clearFocus();
        if (expandable && isExpanded) {
            isExpanded = false;
            if (updateLayout) {
                updateParentLayout();
            }
        }
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
