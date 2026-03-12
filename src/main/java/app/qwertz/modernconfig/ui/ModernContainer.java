package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.theme.ModernConfigTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ModernContainer {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<GuiEventListener> children = new ArrayList<>();
    private final List<LayoutElement> layoutElements = new ArrayList<>();
    private int currentY;
    private float alpha = 1.0f;
    private boolean isFullWidth = false;
    private int columns = 1;
    private int padding = 8;
    private Component title = null;
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private boolean isDraggingScrollbar = false;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_PADDING = 2;
    private final ModernConfigTheme theme;

    public ModernContainer(int x, int y, int width, int height) {
        this(x, y, width, height, null);
    }

    public ModernContainer(int x, int y, int width, int height, ModernConfigTheme theme) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.theme = theme;
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

    public Component getTitle() {
        return title;
    }

    public int getPadding() {
        return padding;
    }

    public int getColumns() {
        return columns;
    }

    public ModernContainer setTitle(Component title) {
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

    public void addElement(AbstractWidget element) {
        addElement(element, new LayoutOptions());
    }

    public void addElement(AbstractWidget element, LayoutOptions options) {
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
            AbstractWidget element = layoutElement.element;
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

        // Update total content height (+2 so last element doesn't touch bottom when scrolled)
        int oldContentHeight = contentHeight;
        contentHeight = currentY - startY + maxHeightInRow + padding;
        
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
                AbstractWidget element = layoutElement.element;
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

    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Draw container background
        int bg = theme != null ? theme.getContainerBackground() : 0x202020;
        int backgroundColor = (int)(alpha * 255) << 24 | (bg & 0xFFFFFF);
        RenderUtil.drawRoundedRect(context, x, y, width, height, 8, backgroundColor);
        
        // Draw container outline (top/bottom one pixel outside so content doesn't overlap)
        int outlineRgb = theme != null ? (theme.getContainerOutline() & 0xFFFFFF) : 0xFFFFFF;
        int outlineColor = (int)(alpha * 64) << 24 | outlineRgb;
        RenderUtil.drawRoundedRect(context, x, y - 1, width, 1, 0, outlineColor);
        RenderUtil.drawRoundedRect(context, x, y + height, width, 1, 0, outlineColor);
        RenderUtil.drawRoundedRect(context, x, y, 1, height, 0, outlineColor);
        RenderUtil.drawRoundedRect(context, x + width - 1, y, 1, height, 0, outlineColor);

        // Enable scissor to clip content
        context.enableScissor(x, y, x + width, y + height);

        // Draw title if present
        if (title != null) {
            int titleRgb = theme != null ? (theme.getTextColor() & 0xFFFFFF) : 0xFFFFFF;
            int titleColor = (int)(alpha * 255) << 24 | titleRgb;
            context.drawString(
                net.minecraft.client.Minecraft.getInstance().font,
                title,
                x + width / 2 - net.minecraft.client.Minecraft.getInstance().font.width(title) / 2,
                y + padding - scrollOffset,
                titleColor
            );
        }

        // Render all child elements
        for (GuiEventListener child : children) {
            if (child instanceof AbstractWidget widget) {
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
            int scrollbarRgb = theme != null ? theme.getAccentColor() : 0xFFFFFF;
            int scrollbarColor = (int)(alpha * 160) << 24 | (scrollbarRgb & 0xFFFFFF);
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

        // First, find which child widget the click is targeting (if any)
        // Two-pass approach: first check all main areas, then check expanded areas
        // This ensures widgets' main buttons are prioritized over expanded areas
        GuiEventListener targetChild = null;
        GuiEventListener targetChildExpanded = null;
        
        // Pass 1: Check all main widget areas (non-expanded) in reverse order
        for (int i = children.size() - 1; i >= 0; i--) {
            GuiEventListener child = children.get(i);
            if (child instanceof AbstractWidget widget) {
                int mainHeight = widget.getHeight();
                // Use getMainHeight() for widgets that can expand
                if (child instanceof ModernDropdown dropdown) {
                    mainHeight = dropdown.getMainHeight();
                } else if (child instanceof ModernColorPicker picker) {
                    mainHeight = picker.getMainHeight();
                } else if (child instanceof ModernListWidget listW) {
                    mainHeight = listW.getMainHeight();
                }
                
                boolean inMainArea = widget.getY() + mainHeight >= y && widget.getY() <= y + height &&
                                   mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth() &&
                                   mouseY >= widget.getY() && mouseY <= widget.getY() + mainHeight;
                
                if (inMainArea) {
                    targetChild = child;
                    break; // Found a widget whose main area contains the click
                }
            }
        }
        
        // Pass 2: Only if no main area match, check expanded areas (and only for actually expanded widgets)
        if (targetChild == null) {
            for (int i = children.size() - 1; i >= 0; i--) {
                GuiEventListener child = children.get(i);
                if (child instanceof AbstractWidget widget) {
                    int mainHeight = widget.getHeight();
                    boolean isExpanded = false;
                    
                    if (child instanceof ModernDropdown dropdown) {
                        mainHeight = dropdown.getMainHeight();
                        isExpanded = dropdown.isExpanded();
                    } else if (child instanceof ModernColorPicker picker) {
                        mainHeight = picker.getMainHeight();
                        isExpanded = picker.isExpanded();
                    } else if (child instanceof ModernListWidget listW) {
                        mainHeight = listW.getMainHeight();
                        isExpanded = listW.isExpanded();
                    }
                    
                    // Only check expanded area if widget is actually expanded
                    if (isExpanded) {
                        int expandedHeight = widget.getHeight();
                        if (expandedHeight > mainHeight) {
                            boolean inExpandedArea = widget.getY() + expandedHeight >= y && widget.getY() <= y + height &&
                                                   mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth() &&
                                                   mouseY >= widget.getY() + mainHeight && mouseY <= widget.getY() + expandedHeight;
                            
                            if (inExpandedArea) {
                                targetChildExpanded = child;
                                break;
                            }
                        }
                    }
                }
            }
            
            if (targetChildExpanded != null) {
                targetChild = targetChildExpanded;
            }
        }

        // Clear focus from all OTHER focusable children (not the target), so only the clicked one gets focus
        // Collect widgets that need layout updates, then update once at the end
        boolean needsLayoutUpdate = false;
        for (GuiEventListener child : children) {
            if (child == targetChild) continue; // Skip the target - it will handle its own focus
            
            if (child instanceof ModernString s) {
                s.setFocused(false);
            } else if (child instanceof ModernListWidget w) {
                if (w.isExpanded()) {
                    w.clearFocus(false);
                    needsLayoutUpdate = true;
                } else {
                    w.clearFocus(false);
                }
            } else if (child instanceof ModernColorPicker p) {
                if (p.isExpanded()) {
                    p.clearFocus(false); // Don't update layout yet
                    needsLayoutUpdate = true;
                } else {
                    p.clearFocus(false);
                }
            } else if (child instanceof ModernItemSelector i) {
                i.clearFocus();
            } else if (child instanceof ModernDropdown d) {
                if (d.isExpanded()) {
                    d.clearFocus(false); // Don't update layout yet
                    needsLayoutUpdate = true;
                } else {
                    d.clearFocus(false);
                }
            }
        }

        // Process the click FIRST (before layout updates) so coordinates are still correct
        boolean clickHandled = false;
        if (targetChild != null && targetChild instanceof AbstractWidget widget) {
            clickHandled = widget.mouseClicked(mouseX, mouseY, button);
        } else {
            // No specific target found, check all children (fallback)
            for (GuiEventListener child : children) {
                if (child instanceof AbstractWidget widget) {
                    if (widget.getY() + widget.getHeight() >= y && widget.getY() <= y + height) {
                        if (child.mouseClicked(mouseX, mouseY, button)) {
                            clickHandled = true;
                            break;
                        }
                    }
                }
            }
        }
        
        // Update layout AFTER processing click (if any widget was collapsed)
        // This way the click uses correct coordinates before widgets move
        if (needsLayoutUpdate) {
            updateLayout();
        }
        
        return clickHandled;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        
        // Forward release events to ALL child elements (important for drag completion)
        for (GuiEventListener child : children) {
            if (child instanceof AbstractWidget widget) {
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
        for (GuiEventListener child : children) {
            if (child instanceof AbstractWidget widget) {
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
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, contentHeight - height));
        updateLayout();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (contentHeight > height) {
            scrollOffset = (int) Mth.clamp(scrollOffset - amount * 20, 0, contentHeight - height);
            updateLayout();
            return true;
        }
        return false;
    }

    public List<GuiEventListener> children() {
        return children;
    }

    private static class LayoutElement {
        final AbstractWidget element;
        final LayoutOptions options;

        LayoutElement(AbstractWidget element, LayoutOptions options) {
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