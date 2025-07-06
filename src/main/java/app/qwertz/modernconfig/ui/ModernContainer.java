package app.qwertz.modernconfig.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.util.ArrayList;
import java.util.List;

public class ModernContainer {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<Element> children = new ArrayList<>();
    private final List<LayoutElement> layoutElements = new ArrayList<>();
    private int currentY;
    private float alpha = 1.0f;
    private boolean isFullWidth = false;
    private int columns = 1;
    private int padding = 8;
    private Text title = null;
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private boolean isDraggingScrollbar = false;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_PADDING = 2;

    public ModernContainer(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.currentY = y + padding;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Text getTitle() {
        return title;
    }

    public int getPadding() {
        return padding;
    }

    public int getColumns() {
        return columns;
    }

    public ModernContainer setTitle(Text title) {
        this.title = title;
        this.currentY += 25; // Space for title
        return this;
    }

    public ModernContainer setFullWidth(boolean fullWidth) {
        this.isFullWidth = fullWidth;
        return this;
    }

    public ModernContainer setColumns(int columns) {
        this.columns = columns;
        return this;
    }

    public ModernContainer setPadding(int padding) {
        this.padding = padding;
        return this;
    }

    public ModernContainer setAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public void addElement(ClickableWidget element) {
        addElement(element, new LayoutOptions());
    }

    public void addElement(ClickableWidget element, LayoutOptions options) {
        children.add(element);
        layoutElements.add(new LayoutElement(element, options));
        updateLayout();
    }

    void updateLayout() {
        int columnWidth = (width - (padding * (columns + 1))) / columns;
        int currentColumn = 0;
        int maxHeightInRow = 0;
        int startY = y + padding;
        currentY = startY;

        if (title != null) {
            currentY += 25; // Space for title
        }

        for (LayoutElement layoutElement : layoutElements) {
            ClickableWidget element = layoutElement.element;
            LayoutOptions options = layoutElement.options;

            int elementWidth = options.fullWidth ? (options.spanColumns * columnWidth + (options.spanColumns - 1) * padding) : columnWidth;
            int elementX = x + padding + (columnWidth + padding) * currentColumn;

            // If element spans multiple columns or won't fit in current row, move to next row
            if (options.spanColumns > 1 || currentColumn + options.spanColumns > columns) {
                currentColumn = 0;
                currentY += maxHeightInRow + padding;
                maxHeightInRow = 0;
                elementX = x + padding;
            }

            // Update element position and size
            element.setX(elementX);
            element.setY(currentY - scrollOffset);
            element.setWidth(elementWidth);

            maxHeightInRow = Math.max(maxHeightInRow, element.getHeight());
            currentColumn += options.spanColumns;

            // Move to next row if we've filled all columns
            if (currentColumn >= columns) {
                currentColumn = 0;
                currentY += maxHeightInRow + padding;
                maxHeightInRow = 0;
            }
        }

        // Update total content height
        int oldContentHeight = contentHeight;
        contentHeight = currentY - startY + maxHeightInRow;
        
        // Clamp scroll offset if content height decreased (e.g., when color picker collapses)
        boolean scrollChanged = false;
        if (contentHeight < oldContentHeight) {
            int maxScroll = Math.max(0, contentHeight - height);
            if (scrollOffset > maxScroll) {
                scrollOffset = maxScroll;
                scrollChanged = true;
            }
        }
        
        // If scroll changed, update element positions with new scroll offset
        if (scrollChanged) {
            columnWidth = (width - (padding * (columns + 1))) / columns;
            currentColumn = 0;
            maxHeightInRow = 0;
            currentY = startY;

            if (title != null) {
                currentY += 25; // Space for title
            }

            for (LayoutElement layoutElement : layoutElements) {
                ClickableWidget element = layoutElement.element;
                LayoutOptions options = layoutElement.options;

                int elementWidth = options.fullWidth ? (options.spanColumns * columnWidth + (options.spanColumns - 1) * padding) : columnWidth;
                int elementX = x + padding + (columnWidth + padding) * currentColumn;

                // If element spans multiple columns or won't fit in current row, move to next row
                if (options.spanColumns > 1 || currentColumn + options.spanColumns > columns) {
                    currentColumn = 0;
                    currentY += maxHeightInRow + padding;
                    maxHeightInRow = 0;
                    elementX = x + padding;
                }

                // Update element position with corrected scroll offset
                element.setX(elementX);
                element.setY(currentY - scrollOffset);
                element.setWidth(elementWidth);

                maxHeightInRow = Math.max(maxHeightInRow, element.getHeight());
                currentColumn += options.spanColumns;

                // Move to next row if we've filled all columns
                if (currentColumn >= columns) {
                    currentColumn = 0;
                    currentY += maxHeightInRow + padding;
                    maxHeightInRow = 0;
                }
            }
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw container background
        int backgroundColor = (int)(alpha * 255) << 24 | 0x202020;
        RenderUtil.drawRoundedRect(context, x, y, width, height, 8, backgroundColor);
        
        // Draw container outline
        int outlineColor = (int)(alpha * 64) << 24 | 0xFFFFFF;
        RenderUtil.drawRoundedRect(context, x, y, width, 1, 0, outlineColor);
        RenderUtil.drawRoundedRect(context, x, y + height - 1, width, 1, 0, outlineColor);
        RenderUtil.drawRoundedRect(context, x, y, 1, height, 0, outlineColor);
        RenderUtil.drawRoundedRect(context, x + width - 1, y, 1, height, 0, outlineColor);

        // Enable scissor to clip content
        context.enableScissor(x, y, x + width, y + height);

        // Draw title if present
        if (title != null) {
            int titleColor = (int)(alpha * 255) << 24 | 0xFFFFFF;
            context.drawTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                title,
                x + width / 2 - net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth(title) / 2,
                y + padding - scrollOffset,
                titleColor
            );
        }

        // Render all child elements
        for (Element child : children) {
            if (child instanceof ClickableWidget widget) {
                if (widget.getY() + widget.getHeight() >= y && widget.getY() <= y + height) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            }
        }

        context.disableScissor();

        // Draw scrollbar if needed
        if (contentHeight > height) {
            float scrollbarHeight = (float) height * height / contentHeight;
            float scrollbarY = y + (height - scrollbarHeight) * scrollOffset / (contentHeight - height);
            
            // Draw scrollbar background
            int scrollbarBgColor = (int)(alpha * 64) << 24 | 0x000000;
            RenderUtil.drawRoundedRect(context, 
                x + width - SCROLLBAR_WIDTH - SCROLLBAR_PADDING, 
                y, 
                SCROLLBAR_WIDTH, 
                height, 
                3, 
                scrollbarBgColor
            );

            // Draw scrollbar handle
            int scrollbarColor = (int)(alpha * 160) << 24 | 0xFFFFFF;
            RenderUtil.drawRoundedRect(context,
                x + width - SCROLLBAR_WIDTH - SCROLLBAR_PADDING,
                (int) scrollbarY,
                SCROLLBAR_WIDTH,
                (int) scrollbarHeight,
                3,
                scrollbarColor
            );
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking scrollbar
        if (contentHeight > height) {
            int scrollbarX = x + width - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH && 
                mouseY >= y && mouseY <= y + height) {
                isDraggingScrollbar = true;
                updateScrollFromMouse(mouseY);
                return true;
            }
        }

        // Check child elements
        for (Element child : children) {
            if (child instanceof ClickableWidget widget) {
                if (widget.getY() + widget.getHeight() >= y && widget.getY() <= y + height) {
                    if (child.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        
        // Forward release events to ALL child elements (important for drag completion)
        for (Element child : children) {
            if (child instanceof ClickableWidget widget) {
                if (child.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        
        // Forward drag events to ALL child elements (not just visible ones)
        // This is important for sliders being dragged outside their bounds
        for (Element child : children) {
            if (child instanceof ClickableWidget widget) {
                if (child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private void updateScrollFromMouse(double mouseY) {
        float scrollbarHeight = (float) height * height / contentHeight;
        float scrollPercent = (float) (mouseY - y - scrollbarHeight / 2) / (height - scrollbarHeight);
        scrollOffset = (int) (scrollPercent * (contentHeight - height));
        scrollOffset = MathHelper.clamp(scrollOffset, 0, Math.max(0, contentHeight - height));
        updateLayout();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (contentHeight > height) {
            scrollOffset = (int) MathHelper.clamp(scrollOffset - amount * 20, 0, contentHeight - height);
            updateLayout();
            return true;
        }
        return false;
    }

    public List<Element> children() {
        return children;
    }

    private static class LayoutElement {
        final ClickableWidget element;
        final LayoutOptions options;

        LayoutElement(ClickableWidget element, LayoutOptions options) {
            this.element = element;
            this.options = options;
        }
    }

    public static class LayoutOptions {
        boolean fullWidth = false;
        int spanColumns = 1;

        public LayoutOptions setFullWidth(boolean fullWidth) {
            this.fullWidth = fullWidth;
            return this;
        }

        public LayoutOptions setSpanColumns(int spanColumns) {
            this.spanColumns = spanColumns;
            return this;
        }
    }
} 