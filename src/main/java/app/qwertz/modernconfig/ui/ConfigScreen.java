package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.*;
import app.qwertz.modernconfig.theme.ModernConfigTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ConfigScreen extends Screen {
    private float openProgress = 0.0f;
    private float transitionProgress = 1.0f;
    private long lastTime = System.currentTimeMillis();
    private boolean closing = false;
    private ModernContainer mainContainer;
    private static Stack<ModernContainer> containerStack = new Stack<>();
    private ModernContainer previousContainer = null;
    private boolean isTransitioningBack = false;
    private final List<String> currentPath = new ArrayList<>();
    private String modId;
    private Map<String, Object> config;
    private ModernConfigTheme theme;

    public ConfigScreen() {
        super(Component.literal("ModernConfig Settings"));
        this.modId = null;
        this.config = null;
        this.theme = null;
    }

    public ConfigScreen(String modId) {
        super(Component.literal(modId + " Settings"));
        this.modId = modId.toLowerCase();
        this.config = ConfigManager.getConfig(modId);
        ConfigManager.ModInfo modInfo = ConfigManager.getModInfo(modId);
        this.theme = modInfo != null ? modInfo.getTheme() : null;
    }

    @Override
    protected void init() {
        int containerWidth = 300;
        int containerHeight = height - 100;
        mainContainer = createContainerForPath(containerWidth, containerHeight);
        containerStack.push(mainContainer);
    }

    private ModernContainer createContainerForPath(int width, int height) {
        ModernContainer container = new ModernContainer(
            this.width / 2 - width / 2,
            50,
            width,
            height,
            theme
        );

        // Set container title and setup
        Component title = getTitle();
        container.setTitle(title);
        container.setPadding(12);
        container.setColumns(1);

        // Add back button if not at root
        if (!currentPath.isEmpty()) {
            ModernButton backButton = new ModernButton(
                0, 0, 100, 20,
                Component.literal("Back"),
                () -> navigateBack(),
                theme
            );
            container.addElement(backButton, new ModernContainer.LayoutOptions().setSpanColumns(1));
        }

        // Handle global config screen (mod list)
        if (modId == null) {
            addModListToContainer(container, width);
        } else {
            addModConfigToContainer(container, width);
        }

        // Add done/close/back button only on root windows (mod list or mod's root config)
        if (currentPath.isEmpty()) {
            if (modId == null) {
                ModernButton doneButton = new ModernButton(
                    0, 0, 100, 20,
                    Component.literal("Done"),
                    this::closeScreen,
                    theme
                );
                container.addElement(doneButton, new ModernContainer.LayoutOptions().setSpanColumns(1));
            } else {
                boolean useBack = "Back".equals(ModernConfigSettings.getModTopExitButton());
                ModernButton exitButton = new ModernButton(
                    0, 0, 100, 20,
                    Component.literal(useBack ? "Back" : "Close"),
                    useBack ? this::navigateBackToMainMenu : this::closeScreen,
                    theme
                );
                container.addElement(exitButton, new ModernContainer.LayoutOptions().setSpanColumns(1));
            }
        }

        return container;
    }

    public Component getTitle() {
        if (modId == null) {
            return Component.literal("ModernConfig Settings");
        }
        if (currentPath.isEmpty()) {
            ConfigManager.ModInfo modInfo = ConfigManager.getModInfo(modId);
            if (modInfo != null) {
                return Component.literal(modInfo.getName() + " Configuration");
            }
            return Component.literal(formatModName(modId) + " Configuration");
        }
        return Component.literal(formatCategoryName(currentPath.get(currentPath.size() - 1)));
    }

    private void addModListToContainer(ModernContainer container, int width) {
        for (Map.Entry<String, Map<String, Object>> entry : ConfigManager.getAllConfigs().entrySet()) {
            String modId = entry.getKey();
            ConfigManager.ModInfo modInfo = ConfigManager.getModInfo(modId);
            
            String displayName = modInfo != null ? modInfo.getName() : formatModName(modId);
            String description = modInfo != null ? modInfo.getDescription() : "Configure " + formatModName(modId);
            
            String modIdToOpen = modId;
            ModernCategory modButton;
            if (modInfo != null && modInfo.getIcon() != null) {
                modButton = new ModernCategory(
                    0, 0, width - 24, 60,
                    Component.literal(displayName),
                    Component.literal(description),
                    modInfo.getIcon(),
                    button -> {
                        Screen screen = Minecraft.getInstance().screen;
                        if (screen instanceof ConfigScreen configScreen) {
                            configScreen.navigateToMod(modIdToOpen);
                        } else {
                            Minecraft.getInstance().setScreen(new ConfigScreen(modIdToOpen));
                        }
                    }
                );
            } else {
                modButton = new ModernCategory(
                    0, 0, width - 24, 60,
                    Component.literal(displayName),
                    Component.literal(description),
                    button -> {
                        Screen screen = Minecraft.getInstance().screen;
                        if (screen instanceof ConfigScreen configScreen) {
                            configScreen.navigateToMod(modIdToOpen);
                        } else {
                            Minecraft.getInstance().setScreen(new ConfigScreen(modIdToOpen));
                        }
                    }
                );
            }
            container.addElement(modButton, new ModernContainer.LayoutOptions().setFullWidth(true));
        }
    }

    private void addModConfigToContainer(ModernContainer container, int width) {
        if (config == null) return;

        Map<String, Object> currentCategory = currentPath.isEmpty() ? 
            config :
            getNestedCategory(config, currentPath);

        if (currentCategory != null) {
            for (Map.Entry<String, Object> entry : currentCategory.entrySet()) {
                if (entry.getValue() instanceof CategoryInfo categoryInfo) {
                    ModernCategory categoryButton = new ModernCategory(
                        0, 0, width - 24, 60,
                        Component.literal(categoryInfo.getTitle()),
                        Component.literal(categoryInfo.getDescription()),
                        theme,
                        category -> navigateToCategory(entry.getKey())
                    );
                    container.addElement(categoryButton, new ModernContainer.LayoutOptions().setFullWidth(true));
                } else if (entry.getValue() instanceof ConfigOption<?> opt) {
                    addOptionToContainer(container, opt, theme);
                }
            }
        }
    }

    private String formatModName(String modId) {
        return modId.substring(0, 1).toUpperCase() + modId.substring(1);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedCategory(Map<String, Object> root, List<String> path) {
        Map<String, Object> current = root;
        for (String key : path) {
            Object next = current.get(key);
            if (next instanceof CategoryInfo) {
                current = ((CategoryInfo) next).getOptions();
            } else if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return null;
            }
        }
        return current;
    }

    private void addOptionToContainer(ModernContainer container, ConfigOption<?> opt, ModernConfigTheme theme) {
        if (opt instanceof SliderConfigOption sliderOpt) {
            ModernSlider slider = new ModernSlider(
                0, 0, 200, 30,
                Component.literal(sliderOpt.getDescription()),
                sliderOpt.getMinValue(),
                sliderOpt.getMaxValue(),
                sliderOpt.getValue(),
                sliderOpt.getPrecision(),
                newVal -> {
                    sliderOpt.setValue(newVal);
                    // Update value immediately for visual feedback
                },
                theme
            ).setOnDragComplete(newVal -> {
                // Save only when dragging is complete
                ConfigManager.save();
            });
            container.addElement(slider, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt instanceof ListConfigOption listOpt) {
            ModernListWidget listWidget = new ModernListWidget(
                0, 0, 200,
                listOpt,
                Component.literal(listOpt.getDescription()),
                theme
            );
            container.addElement(listWidget, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt instanceof ColorConfigOption colorOpt) {
            ModernColorPicker colorPicker = new ModernColorPicker(
                0, 0, 200, 30,
                Component.literal(colorOpt.getDescription()),
                colorOpt.getValue(),
                newVal -> {
                    colorOpt.setValue(newVal);
                    // Update value immediately for visual feedback
                },
                theme
            ).setOnColorComplete(newVal -> {
                // Save only when color selection is complete
                ConfigManager.save();
            });
            container.addElement(colorPicker, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt instanceof DropdownConfigOption dropdownOpt) {
            ModernDropdown dropdown = new ModernDropdown(
                0, 0, 200, 23,
                Component.literal(dropdownOpt.getDescription()),
                dropdownOpt.getOptions(),
                dropdownOpt.getSelectedIndex(),
                newIndex -> {
                    dropdownOpt.setSelectedIndex(newIndex);
                    ConfigManager.save();
                },
                theme
            );
            container.addElement(dropdown, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt instanceof ItemConfigOption itemOpt) {
            // Get the item from the registry
            Item item = BuiltInRegistries.ITEM.getValue(itemOpt.getValue());
            if (item == null) {
                // Fallback to stone if item not found
                item = BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", "stone"));
            }
            
            ModernItemSelector itemSelector = new ModernItemSelector(
                0, 0, 200, 30,
                Component.literal(itemOpt.getDescription()),
                item,
                newItem -> {
                    itemOpt.setValue(BuiltInRegistries.ITEM.getKey(newItem));
                    ConfigManager.save();
                },
                theme
            );
            container.addElement(itemSelector, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt.getDefaultValue() instanceof Boolean bool) {
                ModernToggle toggle = new ModernToggle(
                0, 0, 200, 20,
                        Component.literal(opt.getDescription()),
                        (Boolean) opt.getValue(),
                        newVal -> {
                            ((ConfigOption<Boolean>) opt).setValue(newVal);
                            ConfigManager.save();
                        },
                        theme
                );
            container.addElement(toggle, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt.getDefaultValue() instanceof String) {
            ModernString stringInput = new ModernString(
                0, 0, 200, 20,
                Component.literal(opt.getDescription()),
                (String) opt.getValue(),
                newVal -> {
                    ((ConfigOption<String>) opt).setValue(newVal);
                    ConfigManager.save();
                },
                32,
                theme
            );
            container.addElement(stringInput, new ModernContainer.LayoutOptions().setFullWidth(true));
        }
    }

    private void navigateToCategory(String category) {
        currentPath.add(category);
        ModernContainer newContainer = createContainerForPath(mainContainer.getWidth(), mainContainer.getHeight());
        previousContainer = containerStack.peek();
        containerStack.push(newContainer);
        transitionProgress = 0.0f;
        isTransitioningBack = false;
    }

    private void navigateBack() {
        if (!currentPath.isEmpty()) {
            currentPath.remove(currentPath.size() - 1);
            previousContainer = containerStack.pop();
            transitionProgress = 0.0f;
            isTransitioningBack = true;
        }
    }

    /** Navigate from main menu into a mod's config (same screen, swipe transition). */
    public void navigateToMod(String newModId) {
        if (newModId == null) return;
        this.modId = newModId.toLowerCase();
        this.config = ConfigManager.getConfig(modId);
        ConfigManager.ModInfo modInfo = ConfigManager.getModInfo(modId);
        this.theme = modInfo != null ? modInfo.getTheme() : null;
        previousContainer = containerStack.peek();
        ModernContainer newContainer = createContainerForPath(mainContainer.getWidth(), mainContainer.getHeight());
        containerStack.push(newContainer);
        transitionProgress = 0.0f;
        isTransitioningBack = false;
    }

    /** Navigate from mod root back to main menu (same screen, swipe transition). */
    public void navigateBackToMainMenu() {
        this.modId = null;
        this.config = null;
        this.theme = null;
        previousContainer = containerStack.pop();
        transitionProgress = 0.0f;
        isTransitioningBack = true;
    }

    private void closeScreen() {
        closing = true;
    }


    private String formatCategoryName(String categoryName) {
        return categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int animationDuration = ModernConfigSettings.getAnimationDurationMs();
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float)animationDuration;
        lastTime = currentTime;

        if (closing) {
            openProgress = Math.max(0.0f, openProgress - deltaTime * 2);
            if (openProgress <= 0.0f) {
                onClose();
                return;
            }
        } else {
            openProgress = Math.min(1.0f, openProgress + deltaTime * 2);
        }

        // Update transition animation
        if (transitionProgress < 1.0f) {
            transitionProgress = Math.min(1.0f, transitionProgress + deltaTime * 2);
        }

        float easedProgress = RenderUtil.easeOutExpo(openProgress);
        float easedTransition = RenderUtil.easeOutExpo(transitionProgress);

        // Draw darker background with blur
        RenderUtil.drawBlurredBackground(context, 0, 0, width, height, 0.7f * easedProgress);

        // Calculate container scale and position for transition
        float scale = 0.8f + 0.2f * easedProgress;
        ModernContainer currentContainer = containerStack.peek();
        
        int baseX = width / 2 - currentContainer.getWidth() / 2;
        int baseY = 50;

        // Apply transition animation
        if (transitionProgress < 1.0f && previousContainer != null) {
            int transitionOffset = isTransitioningBack ? -currentContainer.getWidth() : currentContainer.getWidth();
            
            // Calculate positions for animation
            int prevX = (int)(baseX + transitionOffset * easedTransition);
            int currX = (int)(baseX - transitionOffset * (1.0f - easedTransition));
            
            // Update container positions
            previousContainer.setAlpha(1.0f - easedTransition);
            currentContainer.setAlpha(easedTransition);

            // Create temporary containers for animation
            ModernContainer prevAnimContainer = new ModernContainer(
                prevX, baseY,
                previousContainer.getWidth(),
                previousContainer.getHeight()
            );
            for (GuiEventListener child : previousContainer.children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.setX(widget.getX() + (prevX - previousContainer.getX()));
                }
            }

            ModernContainer currAnimContainer = new ModernContainer(
                currX, baseY,
                currentContainer.getWidth(),
                currentContainer.getHeight()
            );
            for (GuiEventListener child : currentContainer.children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.setX(widget.getX() + (currX - currentContainer.getX()));
                }
            }

            // Render containers
            previousContainer.render(context, mouseX, mouseY, delta);
            currentContainer.render(context, mouseX, mouseY, delta);

            // Reset positions
            for (GuiEventListener child : previousContainer.children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.setX(widget.getX() - (prevX - previousContainer.getX()));
                }
            }
            for (GuiEventListener child : currentContainer.children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.setX(widget.getX() - (currX - currentContainer.getX()));
                }
            }
        } else {
            // Render current container normally
            currentContainer.setAlpha(1.0f);
            currentContainer.render(context, mouseX, mouseY, delta);
        }
        
        // Draw developer credit below the container (if enabled in settings)
        if (ModernConfigSettings.isShowCredit()) {
            String creditText = "ModernConfig - Developed by QWERTZ";
            int textWidth = font.width(creditText);
            int creditX = width / 2 - textWidth / 2;
            int creditY = baseY + currentContainer.getHeight() + 10;
            int creditColor = RenderUtil.applyAlpha(0xFFAAAAAA, easedProgress * 0.8f);
            context.drawString(font, creditText, creditX, creditY, creditColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return containerStack.peek().mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return containerStack.peek().mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return containerStack.peek().mouseDragged(mouseX, mouseY, button, deltaX, deltaY) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return containerStack.peek().mouseScrolled(mouseX, mouseY, verticalAmount) || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ModernContainer currentContainer = containerStack.peek();
        for (var child : currentContainer.children()) {
            if (child instanceof ModernString stringInput) {
                if (stringInput.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            } else if (child instanceof ModernListWidget listWidget) {
                if (listWidget.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            } else if (child instanceof ModernColorPicker colorPicker) {
                if (colorPicker.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            } else if (child instanceof ModernItemSelector itemSelector) {
                if (itemSelector.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        ModernContainer currentContainer = containerStack.peek();
        for (var child : currentContainer.children()) {
            if (child instanceof ModernString stringInput) {
                if (stringInput.charTyped(chr, modifiers)) {
                    return true;
                }
            } else if (child instanceof ModernListWidget listWidget) {
                if (listWidget.charTyped(chr, modifiers)) {
                    return true;
                }
            } else if (child instanceof ModernColorPicker colorPicker) {
                if (colorPicker.charTyped(chr, modifiers)) {
                    return true;
                }
            } else if (child instanceof ModernItemSelector itemSelector) {
                if (itemSelector.charTyped(chr, modifiers)) {
                    return true;
                }
            }
        }
        return super.charTyped(chr, modifiers);
    }
    public static ModernContainer getTLContainer() {
        return containerStack.peek();
    }
}
