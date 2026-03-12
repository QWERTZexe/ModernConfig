package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.ModernConfigSettings;
import app.qwertz.modernconfig.theme.ModernConfigTheme;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class ModernDropdown extends AbstractWidget {
    private final List<String> options;
    private int selectedIndex;
    private final Consumer<Integer> onSelectionChange;
    private final ModernConfigTheme theme;
    private boolean isExpanded = false;
    private float animationProgress = 0.0f;
    private float expandProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();
    private final int optionHeight = 20;
    private final int maxVisibleOptions = 10;

    public ModernDropdown(int x, int y, int width, int height, Component text, List<String> options, int selectedIndex, Consumer<Integer> onSelectionChange) {
        this(x, y, width, height, text, options, selectedIndex, onSelectionChange, null);
    }

    public ModernDropdown(int x, int y, int width, int height, Component text, List<String> options, int selectedIndex, Consumer<Integer> onSelectionChange, ModernConfigTheme theme) {
        super(x, y, width, height, text);
        this.options = options;
        this.selectedIndex = selectedIndex;
        this.onSelectionChange = onSelectionChange;
        this.theme = theme;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float) ModernConfigSettings.getAnimationDurationMs();
        lastTime = currentTime;

        // Hover animation
        if (isHovered()) {
            animationProgress = Math.min(1.0f, animationProgress + deltaTime);
        } else {
            animationProgress = Math.max(0.0f, animationProgress - deltaTime);
        }

        // Expand animation
        float oldExpandProgress = expandProgress;
        if (isExpanded) {
            expandProgress = Math.min(1.0f, expandProgress + deltaTime * 2);
        } else {
            expandProgress = Math.max(0.0f, expandProgress - deltaTime * 2);
        }
        
        // Update parent layout if expand progress changed (height changed)
        if (Math.abs(oldExpandProgress - expandProgress) > 0.01f) {
            updateParentLayout();
        }

        float easedHoverProgress = RenderUtil.easeOutExpo(animationProgress);
        float easedExpandProgress = RenderUtil.easeInOutQuad(expandProgress);
        
        // Draw main dropdown background
        int baseColor = 0xFF2A2A2A;
        int hoverColor = 0xFF3A3A3A;
        int currentColor = RenderUtil.interpolateColor(baseColor, hoverColor, easedHoverProgress);
        int mainHeight = getMainHeight();
        RenderUtil.drawRoundedRect(context, getX(), getY(), getWidth(), mainHeight, 4, currentColor);
        
        // Draw label (dropdown name)
        int textColor = theme != null ? theme.getTextColor() : 0xFFFFFFFF;
        float textY = getY() + (mainHeight - 8) / 2.0f;
        context.drawString(
            Minecraft.getInstance().font,
            getMessage(),
            getX() + 8,
            (int)textY,
            textColor
        );
        
        // Calculate text position for selected option
        int labelWidth = Minecraft.getInstance().font.width(getMessage()) + 16;
        int selectedTextX = getX() + labelWidth;
        
        // Draw selected option text
        String selectedText = selectedIndex >= 0 && selectedIndex < options.size() ? options.get(selectedIndex) : "";
        context.drawString(
            Minecraft.getInstance().font,
            Component.literal(selectedText),
            selectedTextX,
            (int)textY,
            textColor
        );

        // Draw dropdown arrow
        int arrowX = getX() + getWidth() - 16;
        int arrowY = getY() + mainHeight / 2;
        drawArrow(context, arrowX, arrowY, isExpanded, easedExpandProgress);

        // Draw dropdown options if expanded
        if (expandProgress > 0.0f) {
            renderDropdownOptions(context, mouseX, mouseY, easedExpandProgress);
        }
    }

    private void renderDropdownOptions(GuiGraphics context, int mouseX, int mouseY, float expandProgress) {
        if (options.isEmpty()) return;

        int visibleOptions = Math.min(options.size(), maxVisibleOptions);
        int dropdownHeight = (int) (visibleOptions * optionHeight * expandProgress);
        int dropdownY = getY() + getMainHeight() + 2;

        // Draw dropdown background
        RenderUtil.drawRoundedRect(context, getX(), dropdownY, getWidth(), dropdownHeight, 4, 0xFF2A2A2A);
        
        // Draw options
        for (int i = 0; i < visibleOptions && i < options.size(); i++) {
            int optionY = dropdownY + (int)(i * optionHeight * expandProgress);
            int optionHeight = (int)(this.optionHeight * expandProgress);
            
            // Check if option is hovered
            boolean isHovered = mouseX >= getX() && mouseX <= getX() + getWidth() && 
                               mouseY >= optionY && mouseY <= optionY + optionHeight;
            
            // Draw option background
            if (isHovered) {
                RenderUtil.drawRoundedRect(context, getX(), optionY, getWidth(), optionHeight, 2, 0xFF3A3A3A);
            } else if (i == selectedIndex) {
                RenderUtil.drawRoundedRect(context, getX(), optionY, getWidth(), optionHeight, 2, 0xFF444444);
            }
            
            // Draw option text
            if (optionHeight > 4) { // Only draw text if there's enough space
                int selectedColor = theme != null ? (0xFF000000 | (theme.getAccentSecondary() & 0xFFFFFF)) : 0xFF88CC88;
                int optionTextColor = (i == selectedIndex) ? selectedColor : (theme != null ? theme.getTextColor() : 0xFFFFFFFF);
                context.drawString(
                    Minecraft.getInstance().font,
                    Component.literal(options.get(i)),
                    getX() + 8,
                    optionY + (optionHeight - 8) / 2,
                    optionTextColor
                );
            }
        }
    }

    private void drawArrow(GuiGraphics context, int x, int y, boolean isExpanded, float progress) {
        int color = 0xFFAAAAAA;
        
        if (isExpanded) {
            // Up arrow (rotated)
            float rotation = progress * 180.0f;
            // Draw simple up arrow
            context.fill(x - 3, y + 1, x + 4, y + 2, color);
            context.fill(x - 2, y, x + 3, y + 1, color);
            context.fill(x - 1, y - 1, x + 2, y, color);
        } else {
            // Down arrow
            context.fill(x - 3, y - 1, x + 4, y, color);
            context.fill(x - 2, y, x + 3, y + 1, color);
            context.fill(x - 1, y + 1, x + 2, y + 2, color);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (isExpanded) {
            // Check if clicking on an option
            int dropdownY = getY() + getMainHeight() + 2;
            int visibleOptions = Math.min(options.size(), maxVisibleOptions);
            
            for (int i = 0; i < visibleOptions && i < options.size(); i++) {
                int optionY = dropdownY + i * optionHeight;
                
                if (mouseY >= optionY && mouseY <= optionY + optionHeight) {
                    // Option clicked
                    selectedIndex = i;
                    onSelectionChange.accept(selectedIndex);
                    isExpanded = false;
                    updateParentLayout();
                    
                    Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
                    );
                    return;
                }
            }
            
            // Clicked outside options, close dropdown
            isExpanded = false;
            updateParentLayout();
        } else {
            // Open dropdown
            isExpanded = true;
            updateParentLayout();
            Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check if click is within the dropdown area when expanded
            if (isExpanded) {
                int mainHeight = getMainHeight();
                int dropdownY = getY() + mainHeight + 2;
                int visibleOptions = Math.min(options.size(), maxVisibleOptions);
                int dropdownHeight = visibleOptions * optionHeight;
                
                if (mouseX >= getX() && mouseX <= getX() + getWidth() && 
                    mouseY >= getY() && mouseY <= dropdownY + dropdownHeight) {
                    onClick(mouseX, mouseY);
                    return true;
                } else {
                    // Click outside dropdown, close it
                    isExpanded = false;
                    updateParentLayout();
                    return false;
                }
            } else {
                // Check if click is within the main dropdown button
                int mainHeight = getMainHeight();
                if (mouseX >= getX() && mouseX <= getX() + getWidth() && 
                    mouseY >= getY() && mouseY <= getY() + mainHeight) {
                    onClick(mouseX, mouseY);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getHeight() {
        // Use expandProgress (not isExpanded) so layout animates on collapse too; items below move up in sync
        if (expandProgress > 0.001f) {
            int visibleOptions = Math.min(options.size(), maxVisibleOptions);
            return getMainHeight() + 2 + (int)(visibleOptions * optionHeight * expandProgress);
        }
        return getMainHeight();
    }
    
    public int getMainHeight() {
        // Always return the original widget height for rendering the main part
        return super.getHeight();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            this.selectedIndex = index;
        }
    }

    public String getSelectedOption() {
        return selectedIndex >= 0 && selectedIndex < options.size() ? options.get(selectedIndex) : "";
    }

    public void collapse() {
        isExpanded = false;
        updateParentLayout();
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    /** Clear focus (collapse) so only one dropdown is open at a time. */
    public void clearFocus() {
        clearFocus(true);
    }
    
    /** Clear focus with option to skip layout update (for batching). */
    public void clearFocus(boolean updateLayout) {
        if (isExpanded) {
            isExpanded = false;
            if (updateLayout) {
                updateParentLayout();
            }
        }
    }

    private void updateParentLayout() {
        try {
            ModernContainer currentContainer = ConfigScreen.getTLContainer();
            if (currentContainer != null) {
                currentContainer.updateLayout();
            }
        } catch (Exception e) {
            // Silently ignore if we can't update layout
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }
} 