package app.qwertz.modernconfig.ui;

import app.qwertz.modernconfig.config.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Arrays;

public class ConfigScreen extends Screen {
    private float openProgress = 0.0f;
    private float transitionProgress = 1.0f;
    private static final int ANIMATION_DURATION = 200; // milliseconds
    private long lastTime = System.currentTimeMillis();
    private boolean closing = false;
    private ModernContainer mainContainer;
    private static Stack<ModernContainer> containerStack = new Stack<>();
    private ModernContainer previousContainer = null;
    private boolean isTransitioningBack = false;
    private final List<String> currentPath = new ArrayList<>();
    private final String modId;
    private final Map<String, Object> config;

    public ConfigScreen() {
        super(Text.literal("ModernConfig Settings"));
        this.modId = null;
        this.config = null;
    }

    public ConfigScreen(String modId) {
        super(Text.literal(modId + " Settings"));
        this.modId = modId.toLowerCase();
        this.config = ConfigManager.getConfig(modId);
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
            height
        );

        // Set container title and setup
        Text title = getTitle();
        container.setTitle(title);
        container.setPadding(12);
        container.setColumns(1);

        // Add back button if not at root
        if (!currentPath.isEmpty()) {
            ModernButton backButton = new ModernButton(
                0, 0, 100, 20,
                Text.literal("Back"),
                () -> navigateBack()
            );
            container.addElement(backButton, new ModernContainer.LayoutOptions().setSpanColumns(1));
        }

        // Handle global config screen (mod list)
        if (modId == null) {
            addModListToContainer(container, width);
        } else {
            addModConfigToContainer(container, width);
        }

        // Add done button only on root windows (mod list or mod's root config)
        if (currentPath.isEmpty()) {
            ModernButton doneButton = new ModernButton(
                0, 0, 100, 20,
                Text.literal("Done"),
                this::closeScreen
            );
            container.addElement(doneButton, new ModernContainer.LayoutOptions().setSpanColumns(1));
        }

        return container;
    }

    public Text getTitle() {
        if (modId == null) {
            return Text.literal("ModernConfig Settings");
        }
        if (currentPath.isEmpty()) {
            ConfigManager.ModInfo modInfo = ConfigManager.getModInfo(modId);
            if (modInfo != null) {
                return Text.literal(modInfo.getName() + " Configuration");
            }
            return Text.literal(formatModName(modId) + " Configuration");
        }
        return Text.literal(formatCategoryName(currentPath.get(currentPath.size() - 1)));
    }

    private void addModListToContainer(ModernContainer container, int width) {
        for (Map.Entry<String, Map<String, Object>> entry : ConfigManager.getAllConfigs().entrySet()) {
            String modId = entry.getKey();
            ConfigManager.ModInfo modInfo = ConfigManager.getModInfo(modId);
            
            String displayName = modInfo != null ? modInfo.getName() : formatModName(modId);
            String description = modInfo != null ? modInfo.getDescription() : "Configure " + formatModName(modId);
            
            ModernCategory modButton;
            if (modInfo != null && modInfo.getIcon() != null) {
                modButton = new ModernCategory(
                    0, 0, width - 24, 60,
                    Text.literal(displayName),
                    Text.literal(description),
                    modInfo.getIcon(),
                    button -> MinecraftClient.getInstance().setScreen(new ConfigScreen(modId))
                );
            } else {
                modButton = new ModernCategory(
                    0, 0, width - 24, 60,
                    Text.literal(displayName),
                    Text.literal(description),
                    button -> MinecraftClient.getInstance().setScreen(new ConfigScreen(modId))
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
                        Text.literal(categoryInfo.getTitle()),
                        Text.literal(categoryInfo.getDescription()),
                        category -> navigateToCategory(entry.getKey())
                    );
                    container.addElement(categoryButton, new ModernContainer.LayoutOptions().setFullWidth(true));
                } else if (entry.getValue() instanceof ConfigOption<?> opt) {
                    addOptionToContainer(container, opt);
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

    private void addOptionToContainer(ModernContainer container, ConfigOption<?> opt) {
        if (opt instanceof SliderConfigOption sliderOpt) {
            ModernSlider slider = new ModernSlider(
                0, 0, 200, 30,
                Text.literal(sliderOpt.getDescription()),
                sliderOpt.getMinValue(),
                sliderOpt.getMaxValue(),
                sliderOpt.getValue(),
                sliderOpt.getPrecision(),
                newVal -> {
                    sliderOpt.setValue(newVal);
                    // Update value immediately for visual feedback
                }
            ).setOnDragComplete(newVal -> {
                // Save only when dragging is complete
                ConfigManager.save();
            });
            container.addElement(slider, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt instanceof ListConfigOption listOpt) {
            ModernListWidget listWidget = new ModernListWidget(
                0, 0, 200,
                listOpt,
                Text.literal(listOpt.getDescription())
            );
            container.addElement(listWidget, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt instanceof ColorConfigOption colorOpt) {
            ModernColorPicker colorPicker = new ModernColorPicker(
                0, 0, 200, 30,
                Text.literal(colorOpt.getDescription()),
                colorOpt.getValue(),
                newVal -> {
                    colorOpt.setValue(newVal);
                    // Update value immediately for visual feedback
                }
            ).setOnColorComplete(newVal -> {
                // Save only when color selection is complete
                ConfigManager.save();
            });
            container.addElement(colorPicker, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt instanceof DropdownConfigOption dropdownOpt) {
            ModernDropdown dropdown = new ModernDropdown(
                0, 0, 200, 23,
                Text.literal(dropdownOpt.getDescription()),
                dropdownOpt.getOptions(),
                dropdownOpt.getSelectedIndex(),
                newIndex -> {
                    dropdownOpt.setSelectedIndex(newIndex);
                    ConfigManager.save();
                }
            );
            container.addElement(dropdown, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt instanceof ItemConfigOption itemOpt) {
            // Get the item from the registry
            Item item = Registries.ITEM.get(itemOpt.getValue());
            if (item == null) {
                // Fallback to stone if item not found
                item = Registries.ITEM.get(Identifier.of("minecraft", "stone"));
            }
            
            ModernItemSelector itemSelector = new ModernItemSelector(
                0, 0, 200, 30,
                Text.literal(itemOpt.getDescription()),
                item,
                newItem -> {
                    itemOpt.setValue(Registries.ITEM.getId(newItem));
                    ConfigManager.save();
                }
            );
            container.addElement(itemSelector, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt.getDefaultValue() instanceof Boolean bool) {
                ModernToggle toggle = new ModernToggle(
                0, 0, 200, 20,
                        Text.literal(opt.getDescription()),
                        (Boolean) opt.getValue(),
                        newVal -> {
                            ((ConfigOption<Boolean>) opt).setValue(newVal);
                            ConfigManager.save();
                        }
                );
            container.addElement(toggle, new ModernContainer.LayoutOptions().setFullWidth(true));
        } else if (opt.getDefaultValue() instanceof String) {
            ModernString stringInput = new ModernString(
                0, 0, 200, 20,
                Text.literal(opt.getDescription()),
                (String) opt.getValue(),
                newVal -> {
                    ((ConfigOption<String>) opt).setValue(newVal);
                    ConfigManager.save();
                }
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
            ModernContainer newContainer = createContainerForPath(mainContainer.getWidth(), mainContainer.getHeight());
            previousContainer = containerStack.pop();
            containerStack.push(newContainer);
            transitionProgress = 0.0f;
            isTransitioningBack = true;
        }
    }

    private void closeScreen() {
        closing = true;
    }


    private String formatCategoryName(String categoryName) {
        return categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / (float)ANIMATION_DURATION;
        lastTime = currentTime;

        if (closing) {
            openProgress = Math.max(0.0f, openProgress - deltaTime * 2);
            if (openProgress <= 0.0f) {
                close();
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
            for (Element child : previousContainer.children()) {
                if (child instanceof ClickableWidget widget) {
                    widget.setX(widget.getX() + (prevX - previousContainer.getX()));
                }
            }

            ModernContainer currAnimContainer = new ModernContainer(
                currX, baseY,
                currentContainer.getWidth(),
                currentContainer.getHeight()
            );
            for (Element child : currentContainer.children()) {
                if (child instanceof ClickableWidget widget) {
                    widget.setX(widget.getX() + (currX - currentContainer.getX()));
                }
            }

            // Render containers
            previousContainer.render(context, mouseX, mouseY, delta);
            currentContainer.render(context, mouseX, mouseY, delta);

            // Reset positions
            for (Element child : previousContainer.children()) {
                if (child instanceof ClickableWidget widget) {
                    widget.setX(widget.getX() - (prevX - previousContainer.getX()));
                }
            }
            for (Element child : currentContainer.children()) {
                if (child instanceof ClickableWidget widget) {
                    widget.setX(widget.getX() - (currX - currentContainer.getX()));
                }
            }
        } else {
            // Render current container normally
            currentContainer.setAlpha(1.0f);
            currentContainer.render(context, mouseX, mouseY, delta);
        }
        
        // Draw developer credit below the container
        String creditText = "ModernConfig - Developed by QWERTZ";
        int textWidth = textRenderer.getWidth(creditText);
        int creditX = width / 2 - textWidth / 2;
        int creditY = baseY + currentContainer.getHeight() + 10;
        
        int creditColor = RenderUtil.applyAlpha(0xFFAAAAAA, easedProgress * 0.8f);
        context.drawTextWithShadow(textRenderer, creditText, creditX, creditY, creditColor);
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
