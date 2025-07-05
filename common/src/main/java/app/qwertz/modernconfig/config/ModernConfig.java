package app.qwertz.modernconfig.config;

import app.qwertz.modernconfig.ui.ConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ModernConfig {
    private final String modId;
    private final Map<String, Object> config;
    private final List<Runnable> saveListeners = new ArrayList<>();

    ModernConfig(String modId, Map<String, Object> config) {
        this.modId = modId.toLowerCase();
        this.config = config;
    }

    public static ModernConfig create(String modId, Map<String, Object> config) {
        ConfigManager.registerConfig(modId, config);
        return ConfigManager.getModConfig(modId);
    }

    public static ModernConfig create(String modId, String name, String description, Map<String, Object> config) {
        ConfigManager.registerConfig(modId, name, description, config);
        return ConfigManager.getModConfig(modId);
    }

    public static ModernConfig create(String modId, String name, String description, Identifier icon, Map<String, Object> config) {
        ConfigManager.registerConfig(modId, name, description, icon, config);
        return ConfigManager.getModConfig(modId);
    }

    public void openScreen() {
        MinecraftClient.getInstance().setScreen(new ConfigScreen(modId));
    }

    @SuppressWarnings("unchecked")
    public ConfigOption<?> getOption(String... path) {
        if (path == null || path.length == 0) return null;

        Map<String, Object> current = config;
        for (int i = 0; i < path.length - 1; i++) {
            Object value = current.get(path[i]);
            if (value instanceof Map) {
                current = (Map<String, Object>) value;
            } else if (value instanceof CategoryInfo) {
                current = ((CategoryInfo) value).getOptions();
            } else {
                return null;
            }
        }

        Object option = current.get(path[path.length - 1]);
        if (option instanceof ConfigOption<?>) {
            return (ConfigOption<?>) option;
        }
        return null;
    }

    public String getModId() {
        return modId;
    }

    public void onConfigSave(Runnable listener) {
        saveListeners.add(listener);
    }

    void notifySaveListeners() {
        for (Runnable listener : saveListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Called internally by ConfigOption when a value changes
    void save() {
        ConfigManager.save();
        notifySaveListeners();
    }
    public static void openGlobalConfig() {
        MinecraftClient.getInstance().setScreen(new ConfigScreen());
    }

} 