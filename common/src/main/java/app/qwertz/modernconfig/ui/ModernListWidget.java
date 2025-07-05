package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.ListConfigOption;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class ModernListWidget extends ClickableWidget {
    private final ModernList modernList;

    public ModernListWidget(int x, int y, int width, ListConfigOption option, Text description) {
        super(x, y, width, 30, description); // Initial height, will be updated
        this.modernList = new ModernList(option);
        this.modernList.setParentWidget(this);
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
        modernList.setPosition(getX(), getY(), getWidth());
        // Note: Height is managed by the container, not dynamically changed here
    }

    @Override
    public int getHeight() {
        return modernList.getHeight();
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        updatePosition(); // Ensure position is current
        modernList.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        // Add narration for accessibility
        appendDefaultNarrations(builder);
    }
} 