package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.ModernConfigSettings;
import app.qwertz.modernconfig.theme.ModernConfigTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModernItemSelector extends AbstractWidget {
    private final List<Item> allItems;
    private final List<Item> filteredItems;
    private Item selectedItem;
    private final Consumer<Item> onSelectionChange;
    private final ModernConfigTheme theme;
    private boolean isExpanded = false;
    private float animationProgress = 0.0f;
    private float expandProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();
    private final int optionHeight = 25;
    private final int maxVisibleOptions = 6;
    private String searchText = "";
    private boolean isSearchFocused = false;
    private int searchCursorPosition = 0;

    public ModernItemSelector(int x, int y, int width, int height, Component text, Item defaultItem, Consumer<Item> onSelectionChange) {
        this(x, y, width, height, text, defaultItem, onSelectionChange, null);
    }

    public ModernItemSelector(int x, int y, int width, int height, Component text, Item defaultItem, Consumer<Item> onSelectionChange, ModernConfigTheme theme) {
        super(x, y, width, height, text);
        this.selectedItem = defaultItem;
        this.onSelectionChange = onSelectionChange;
        this.theme = theme;
        
        // Get all registered items
        this.allItems = new ArrayList<>();
        this.filteredItems = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item != null) {
                allItems.add(item);
            }
        });
        updateFilteredItems();
    }

    private void updateFilteredItems() {
        filteredItems.clear();
        if (searchText.isEmpty()) {
            // Show first 100 items when no search
            filteredItems.addAll(allItems.stream().limit(100).collect(Collectors.toList()));
        } else {
            // Filter items by search text
            String searchLower = searchText.toLowerCase();
            filteredItems.addAll(allItems.stream()
                .filter(item -> {
                    String itemName = item.getName().getString().toLowerCase();
                    String itemId = BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase();
                    return itemName.contains(searchLower) || itemId.contains(searchLower);
                })
                .limit(100)
                .collect(Collectors.toList()));
        }
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
        
        // Update parent layout if expand progress changed
        if (Math.abs(oldExpandProgress - expandProgress) > 0.01f) {
            updateParentLayout();
        }

        float easedHoverProgress = RenderUtil.easeOutExpo(animationProgress);
        float easedExpandProgress = RenderUtil.easeInOutQuad(expandProgress);
        
        // Draw main selector background
        int baseColor = 0xFF2A2A2A;
        int hoverColor = 0xFF3A3A3A;
        int currentColor = RenderUtil.interpolateColor(baseColor, hoverColor, easedHoverProgress);
        int mainHeight = getMainHeight();
        RenderUtil.drawRoundedRect(context, getX(), getY(), getWidth(), mainHeight, 4, currentColor);
        
        // Draw label
        int textColor = theme != null ? theme.getTextColor() : 0xFFFFFFFF;
        float textY = getY() + (mainHeight - 8) / 2.0f;
        context.drawString(
            Minecraft.getInstance().font,
            getMessage(),
            getX() + 8,
            (int)textY,
            textColor
        );
        
        // Draw selected item
        if (selectedItem != null) {
            int itemX = getX() + getWidth() - 40;
            int itemY = getY() + (mainHeight - 16) / 2;
            // Draw item background (neutral gray, not theme)
            RenderUtil.drawRoundedRect(context, itemX - 2, itemY - 2, 20, 20, 2, 0xFF444444);
            
            // Draw item
            ItemStack stack = new ItemStack(selectedItem);
            context.renderItem(stack, itemX, itemY);
        }

        // Draw expand arrow
        int arrowX = getX() + getWidth() - 16;
        int arrowY = getY() + mainHeight / 2;
        drawArrow(context, arrowX, arrowY, isExpanded, easedExpandProgress);

        // Draw dropdown if expanded
        if (expandProgress > 0.0f) {
            renderDropdown(context, mouseX, mouseY, easedExpandProgress);
        }
    }

    private void renderDropdown(GuiGraphics context, int mouseX, int mouseY, float expandProgress) {
        int dropdownHeight = (int) ((maxVisibleOptions * optionHeight + 30) * expandProgress);
        int dropdownY = getY() + getMainHeight() + 2;

        // Draw dropdown background
        RenderUtil.drawRoundedRect(context, getX(), dropdownY, getWidth(), dropdownHeight, 4, 0xFF2A2A2A);
        
        // Draw search box (outline with theme accent when focused)
        int searchY = dropdownY + 5;
        int searchHeight = 20;
        RenderUtil.drawRoundedRect(context, getX() + 5, searchY, getWidth() - 10, searchHeight, 2, 0xFF1A1A1A);
        int searchOutline = theme != null ? theme.getAccentColor() : 0xFFFFFFFF;
        if (isSearchFocused) {
            RenderUtil.drawRoundedRect(context, getX() + 5, searchY, getWidth() - 10, 1, 0, (0x80 << 24) | (searchOutline & 0xFFFFFF));
            RenderUtil.drawRoundedRect(context, getX() + 5, searchY + searchHeight - 1, getWidth() - 10, 1, 0, (0x80 << 24) | (searchOutline & 0xFFFFFF));
            RenderUtil.drawRoundedRect(context, getX() + 5, searchY, 1, searchHeight, 0, (0x80 << 24) | (searchOutline & 0xFFFFFF));
            RenderUtil.drawRoundedRect(context, getX() + getWidth() - 6, searchY, 1, searchHeight, 0, (0x80 << 24) | (searchOutline & 0xFFFFFF));
        }
        
        // Draw search text
        String displayText = searchText.isEmpty() ? "Search items..." : searchText;
        int searchPlaceholderColor = theme != null ? theme.getTextColorSecondary() : 0xFF666666;
        int searchTextColor = searchText.isEmpty() ? searchPlaceholderColor : (theme != null ? theme.getTextColor() : 0xFFFFFFFF);
        context.drawString(
            Minecraft.getInstance().font,
            displayText,
            getX() + 10,
            searchY + 6,
            searchTextColor
        );
        
        // Draw cursor in search
        if (isSearchFocused && searchText.isEmpty()) {
            int cursorX = getX() + 10 + Minecraft.getInstance().font.width("Search items...");
            context.fill(cursorX, searchY + 6, cursorX + 1, searchY + 16, searchOutline);
        }
        
        // Draw items
        int itemsStartY = searchY + searchHeight + 5;
        int visibleItems = Math.min(filteredItems.size(), maxVisibleOptions);
        
        for (int i = 0; i < visibleItems; i++) {
            int itemY = itemsStartY + (int)(i * optionHeight * expandProgress);
            int itemHeight = (int)(this.optionHeight * expandProgress);
            
            if (itemHeight < 5) continue; // Skip if too small
            
            Item item = filteredItems.get(i);
            boolean isHovered = mouseX >= getX() && mouseX <= getX() + getWidth() && 
                               mouseY >= itemY && mouseY <= itemY + itemHeight;
            boolean isSelected = item == selectedItem;
            
            // Draw item background (selected stays neutral gray, not theme)
            if (isHovered) {
                RenderUtil.drawRoundedRect(context, getX() + 2, itemY, getWidth() - 4, itemHeight, 2, 0xFF3A3A3A);
            } else if (isSelected) {
                RenderUtil.drawRoundedRect(context, getX() + 2, itemY, getWidth() - 4, itemHeight, 2, 0xFF444444);
            }
            
            // Draw item icon
            int iconX = getX() + 8;
            int iconY = itemY + (itemHeight - 16) / 2;
            ItemStack stack = new ItemStack(item);
            context.renderItem(stack, iconX, iconY);
            
            // Draw item name
            String itemName = item.getName().getString();
            int nameX = iconX + 20;
            int nameY = itemY + (itemHeight - 8) / 2;
            int selectedNameColor = theme != null ? (0xFF000000 | (theme.getAccentSecondary() & 0xFFFFFF)) : 0xFF88CC88;
            int nameColor = isSelected ? selectedNameColor : (theme != null ? theme.getTextColor() : 0xFFFFFFFF);
            context.drawString(
                Minecraft.getInstance().font,
                itemName,
                nameX,
                nameY,
                nameColor
            );
        }
    }

    private void drawArrow(GuiGraphics context, int x, int y, boolean isExpanded, float progress) {
        int color = theme != null ? theme.getAccentColor() : 0xFFAAAAAA;
        
        if (isExpanded) {
            // Up arrow
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
            // Check if clicking on search box
            int searchY = getY() + getMainHeight() + 7;
            if (mouseY >= searchY && mouseY <= searchY + 20 && 
                mouseX >= getX() + 5 && mouseX <= getX() + getWidth() - 5) {
                isSearchFocused = true;
                return;
            }
            
            // Check if clicking on an item
            int itemsStartY = searchY + 25;
            int visibleItems = Math.min(filteredItems.size(), maxVisibleOptions);
            
            for (int i = 0; i < visibleItems; i++) {
                int itemY = itemsStartY + i * optionHeight;
                
                if (mouseY >= itemY && mouseY <= itemY + optionHeight) {
                    // Item clicked
                    selectedItem = filteredItems.get(i);
                    onSelectionChange.accept(selectedItem);
                    isExpanded = false;
                    updateParentLayout();
                    
                    Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
                    );
                    return;
                }
            }
            
            // Clicked outside, close dropdown
            isExpanded = false;
            isSearchFocused = false;
            updateParentLayout();
        } else {
            // Open dropdown
            isExpanded = true;
            isSearchFocused = true;
            updateParentLayout();
            Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isSearchFocused) {
            // Handle search input
            if (keyCode == 256) { // ESC
                isSearchFocused = false;
                return true;
            } else if (keyCode == 257) { // ENTER
                if (!filteredItems.isEmpty()) {
                    selectedItem = filteredItems.get(0);
                    onSelectionChange.accept(selectedItem);
                    isExpanded = false;
                    isSearchFocused = false;
                    updateParentLayout();
                }
                return true;
            } else if (keyCode == 259) { // BACKSPACE
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                    searchCursorPosition = Math.max(0, searchCursorPosition - 1);
                    updateFilteredItems();
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (isSearchFocused) {
            if (chr >= ' ' && chr <= '~') {
                searchText += chr;
                searchCursorPosition = searchText.length();
                updateFilteredItems();
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    public int getMainHeight() {
        return 30;
    }

    @Override
    public int getHeight() {
        // Use expandProgress (not isExpanded) so layout animates on collapse too; items below move up in sync
        if (expandProgress > 0.001f) {
            return getMainHeight() + 2 + (int)((maxVisibleOptions * optionHeight + 35) * expandProgress);
        }
        return getMainHeight();
    }

    public Item getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(Item item) {
        this.selectedItem = item;
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

    /** Clear search focus so only one input has focus at a time. */
    public void clearFocus() {
        isSearchFocused = false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }
} 