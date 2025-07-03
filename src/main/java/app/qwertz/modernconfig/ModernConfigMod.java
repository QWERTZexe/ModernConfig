package app.qwertz.modernconfig;

import app.qwertz.modernconfig.config.ConfigManager;
import app.qwertz.modernconfig.config.ModernConfig;
import app.qwertz.modernconfig.ui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ModernConfigMod implements ClientModInitializer {
    private static KeyBinding configKeyBinding;

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
    }

    }