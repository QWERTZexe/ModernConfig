package app.qwertz.modernconfig.ui;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.util.List;
import java.util.function.Consumer;

public class ModernDropdown extends ClickableWidget {
    private final List<String> options;
    private int selectedIndex;
    private final Consumer<Integer> onSelectionChange;
    private boolean isExpanded = false;
    private float animationProgress = 0.0f;
    private float expandProgress = 0.0f;
    private static final int ANIMATION_DURATION = 200; // milliseconds
    private long lastTime = System.currentTimeMillis();
    private final int optionHeight = 20;
    private final int maxVisibleOptions = 5;

    public ModernDropdown(int x, int y, int width, int height, Text text, List<String> options, int selectedIndex, Consumer<Integer> onSelectionChange) {
        super(x, y, width, height, text);
        this.options = options;
        this.selectedIndex = selectedIndex;
        this.onSelectionChange = onSelectionChange;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float)ANIMATION_DURATION;
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
        float textY = getY() + (mainHeight - 8) / 2.0f;
        context.drawTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            getMessage(),
            getX() + 8,
            (int)textY,
            0xFFFFFFFF
        );
        
        // Calculate text position for selected option
        int labelWidth = MinecraftClient.getInstance().textRenderer.getWidth(getMessage()) + 16;
        int selectedTextX = getX() + labelWidth;
        
        // Draw selected option text
        String selectedText = selectedIndex >= 0 && selectedIndex < options.size() ? options.get(selectedIndex) : "";
        context.drawTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            Text.literal(selectedText),
            selectedTextX,
            (int)textY,
            0xFFFFFFFF
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

    private void renderDropdownOptions(DrawContext context, int mouseX, int mouseY, float expandProgress) {
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
                int textColor = (i == selectedIndex) ? 0xFF88CC88 : 0xFFFFFFFF;
                context.drawTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    Text.literal(options.get(i)),
                    getX() + 8,
                    optionY + (optionHeight - 8) / 2,
                    textColor
                );
            }
        }
    }

    private void drawArrow(DrawContext context, int x, int y, boolean isExpanded, float progress) {
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
                    
                    MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
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
            MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
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
        // Return expanded height for layout system to move other widgets down
        if (isExpanded && expandProgress > 0.0f) {
            int visibleOptions = Math.min(options.size(), maxVisibleOptions);
            return super.getHeight() + 2 + (int)(visibleOptions * optionHeight * expandProgress);
        }
        return super.getHeight();
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
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
} 