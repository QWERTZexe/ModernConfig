package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.ListConfigOption;
import app.qwertz.modernconfig.config.ConfigManager;
import app.qwertz.modernconfig.theme.ModernConfigTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ModernList {
    private final ListConfigOption option;
    private final ModernConfigTheme theme;
    private final List<ModernString> inputs;
    private ModernListWidget parentWidget;
    private int x, y, width;
    private int itemHeight = 25;
    private int padding = 5;
    private int headerHeight = 20;
    private int focusedIndex = -1;
    private long lastClickTime = 0;
    private boolean drawHeader = true;

    public ModernList(ListConfigOption option) {
        this(option, null);
    }

    public ModernList(ListConfigOption option, ModernConfigTheme theme) {
        this.option = option;
        this.theme = theme;
        this.inputs = new ArrayList<>();
        rebuildInputs();
    }

    public void setParentWidget(ModernListWidget widget) {
        this.parentWidget = widget;
    }

    private void rebuildInputs() {
        inputs.clear();
        List<String> items = option.getItems();
        
        // Add inputs for existing items
        for (String item : items) {
            ModernString input = new ModernString(0, 0, 200, 20, 
                Text.literal(option.getChildName()), item, 
                newVal -> {
                    // Value changes will be handled in updateOptionFromInputs
                }, 16, theme);
            inputs.add(input);
        }
        
        // Always add one empty input at the end for adding new items
        ModernString addInput = new ModernString(0, 0, 200, 20, 
            Text.literal(option.getChildName()), "", 
            newVal -> {
                // Value changes will be handled in updateOptionFromInputs
            }, 16, theme);
        inputs.add(addInput);
    }

    /** When false, no label is drawn and getHeight() returns content-only height (for expandable widget). */
    public void setDrawHeader(boolean draw) {
        this.drawHeader = draw;
    }

    public void setPosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
        int contentStart = drawHeader ? headerHeight : 0;
        for (int i = 0; i < inputs.size(); i++) {
            int inputY = y + contentStart + (padding / 2) + i * (itemHeight + padding);
            int inputWidth = width - 30;
            ModernString input = inputs.get(i);
            input.setX(x);
            input.setY(inputY);
            input.setWidth(inputWidth);
        }
    }

    public int getHeight() {
        int content = (padding / 2) + Math.max(1, inputs.size()) * (itemHeight + padding) - padding;
        return drawHeader ? headerHeight + content : content;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int contentStart = drawHeader ? headerHeight : 0;
        if (drawHeader) {
            int headerColor = theme != null ? theme.getTextColor() : 0xFFFFFFFF;
            context.drawText(mc.textRenderer, Text.literal(option.getDescription()), x, y + 2, headerColor, false);
        }
        for (int i = 0; i < inputs.size(); i++) {
            int inputY = y + contentStart + (padding / 2) + i * (itemHeight + padding);
            ModernString input = inputs.get(i);
            
            // Render input field
            input.render(context, mouseX, mouseY, delta);
            
            // Render icons
            int iconX = x + width - 25;
            int iconY = inputY + 2;
            
            if (i < inputs.size() - 1) {
                // Dustbin icon for existing items
                boolean hoveringDelete = mouseX >= iconX && mouseX <= iconX + 20 && 
                                       mouseY >= iconY && mouseY <= iconY + 20;
                
                int deleteColor = hoveringDelete ? 0xFFFF4444 : 0xFF666666;
                drawDeleteIcon(context, iconX, iconY, deleteColor);
            } else {
                // Plus icon for add row
                boolean hoveringAdd = mouseX >= iconX && mouseX <= iconX + 20 && 
                                    mouseY >= iconY && mouseY <= iconY + 20;
                
                int addColor = hoveringAdd ? 0xFF44FF44 : 0xFF666666;
                drawPlusIcon(context, iconX, iconY, addColor);
            }
        }
    }

    private void drawDeleteIcon(DrawContext context, int x, int y, int color) {
        // Draw dustbin/trash icon
        // Lid
        context.fill(x + 5, y + 3, x + 15, y + 5, color);
        // Handle
        context.fill(x + 8, y + 1, x + 12, y + 3, color);
        // Body
        context.fill(x + 6, y + 5, x + 14, y + 17, color);
        // Lines inside
        context.fill(x + 8, y + 7, x + 9, y + 15, color);
        context.fill(x + 10, y + 7, x + 11, y + 15, color);
        context.fill(x + 12, y + 7, x + 13, y + 15, color);
    }

    private void drawPlusIcon(DrawContext context, int x, int y, int color) {
        // Draw plus icon
        // Horizontal line
        context.fill(x + 5, y + 9, x + 15, y + 11, color);
        // Vertical line
        context.fill(x + 9, y + 5, x + 11, y + 15, color);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int contentStart = drawHeader ? headerHeight : 0;
        // Find target by bounds first (so we can clear others before processing click; fixes bottom-to-top focus)
        int targetInputIndex = -1;
        int targetIconIndex = -1;
        for (int i = 0; i < inputs.size(); i++) {
            int inputY = y + contentStart + (padding / 2) + i * (itemHeight + padding);
            ModernString input = inputs.get(i);
            if (mouseX >= input.getX() && mouseX <= input.getX() + input.getWidth() &&
                mouseY >= input.getY() && mouseY <= input.getY() + input.getHeight()) {
                targetInputIndex = i;
                break;
            }
            int iconX = x + width - 25;
            int iconY = inputY + 2;
            if (mouseX >= iconX && mouseX <= iconX + 20 && mouseY >= iconY && mouseY <= iconY + 20) {
                targetIconIndex = i;
                break;
            }
        }

        // Clear focus from all inputs first so only the clicked one gets focus (fixes bottom-to-top)
        for (ModernString input : inputs) {
            input.setFocused(false);
        }
        focusedIndex = -1;

        if (targetIconIndex >= 0) {
            MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f)
            );
            if (targetIconIndex < inputs.size() - 1) {
                removeItem(targetIconIndex);
            } else {
                addItem();
            }
            return true;
        }
        if (targetInputIndex >= 0) {
            focusedIndex = targetInputIndex;
            return inputs.get(targetInputIndex).mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedIndex >= 0 && focusedIndex < inputs.size()) {
            ModernString input = inputs.get(focusedIndex);
            boolean handled = input.keyPressed(keyCode, scanCode, modifiers);
            
            // Update the option when the input changes
            updateOptionFromInputs();
            
            return handled;
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (focusedIndex >= 0 && focusedIndex < inputs.size()) {
            ModernString input = inputs.get(focusedIndex);
            boolean handled = input.charTyped(chr, modifiers);
            
            // Update the option when the input changes
            updateOptionFromInputs();
            
            return handled;
        }
        return false;
    }

    private void addItem() {
        ModernString lastInput = inputs.get(inputs.size() - 1);
        String newValue = lastInput.getValue().trim();
        
        if (!newValue.isEmpty()) {
            List<String> items = option.getItems();
            items.add(newValue);
            option.setItems(items);
            lastInput.setValue(""); // Clear the input field
            rebuildInputs();
            setPosition(x, y, width); // Refresh positions after adding
            ConfigManager.save(); // Save changes immediately
            ModernContainer currentContainer = ConfigScreen.getTLContainer();
            currentContainer.updateLayout(); // Update parent layout

        }
    }

    private void removeItem(int index) {
        List<String> items = option.getItems();
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            option.setItems(items);
            rebuildInputs();
            setPosition(x, y, width); // Refresh positions after removing
            ConfigManager.save(); // Save changes immediately
            ModernContainer currentContainer = ConfigScreen.getTLContainer();
            currentContainer.updateLayout(); // Update parent layout

        }
    }

    private void updateOptionFromInputs() {
        // Update existing items
        for (int i = 0; i < inputs.size() - 1 && i < option.size(); i++) {
            String value = inputs.get(i).getValue().trim();
            if (!value.isEmpty()) {
                option.updateItem(i, value);
            }
        }
        ConfigManager.save(); // Save changes after updating
    }

    public ListConfigOption getOption() {
        return option;
    }

    /** Clear focus from the focused input so only one list has focus at a time. */
    public void clearFocus() {
        focusedIndex = -1;
        for (ModernString input : inputs) {
            input.setFocused(false);
        }
    }
} 