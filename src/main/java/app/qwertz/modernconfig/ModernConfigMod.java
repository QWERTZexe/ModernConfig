package app.qwertz.modernconfig;

import app.qwertz.modernconfig.config.*;
import app.qwertz.modernconfig.ui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.Arrays;

public class ModernConfigMod implements ClientModInitializer {
    private static KeyBinding configKeyBinding;
    private ModernConfig modernConfig;

    @Override
    public void onInitializeClient() {
        // Register keybind
        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.modernconfig.open_config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.modernconfig.general"
        ));

        // Register tick event for keybind
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (configKeyBinding.wasPressed()) {
                ModernConfig.openGlobalConfig();
            }
        });
        modernConfig = buildConfig();
    }


    private ModernConfig buildConfig() {
        // Create icon for ModernConfig
        Identifier modernConfigIcon = Identifier.of("modernconfig", "icon.png");

        // Build the configuration with examples of all config types
        return ConfigBuilder.create("ModernConfig", "Configuration for ModernConfig itself", modernConfigIcon)
            .category("examples", "Examples", "Examples of different config types", category -> category
                .list("example_list", "Example List", "Sample Entry")
                .dropdown("example_dropdown", "Example Dropdown", Arrays.asList("Easy", "Medium", "Hard", "Expert"), "Medium")
                .dropdown("example_dropdown_2", "Example Dropdown 2", Arrays.asList("Dark", "Light", "Auto"), "Dark")
                .dropdown("example_dropdown_3", "Example Dropdown 3", Arrays.asList("English", "Spanish", "French", "German", "Chinese"), "English")
                .color("example_color", "Example Color", 0x0066CC)
                .color("example_color_2", "Example Color 2", 0x00CC66)
                .color("example_color_3", "Example Color 3", 0xCC6600)
                .toggle("example_toggle", "Example Toggle", false)
                .text("example_text", "Example Text", "Hello World!")
                .slider("example_slider", "Example Slider", 50, 0, 100, 1)
            .end())
            .build();
    }
}