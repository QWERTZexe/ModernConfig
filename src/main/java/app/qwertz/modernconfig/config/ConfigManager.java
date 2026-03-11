package app.qwertz.modernconfig.config;

import app.qwertz.modernconfig.theme.ModernConfigTheme;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class ConfigManager {
    // Inner class to store mod metadata
    public static class ModInfo {
        private final String name;
        private final String description;
        private final Identifier icon;
        private final ModernConfigTheme theme;
        
        public ModInfo(String name, String description) {
            this.name = name;
            this.description = description;
            this.icon = null;
            this.theme = null;
        }
        
        public ModInfo(String name, String description, Identifier icon) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.theme = null;
        }
        
        public ModInfo(String name, String description, Identifier icon, ModernConfigTheme theme) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.theme = theme;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Identifier getIcon() {
            return icon;
        }
        
        public ModernConfigTheme getTheme() {
            return theme;
        }
    }
    
    private static final Map<String, Map<String, Object>> MOD_CONFIGS = new HashMap<>();
    private static final Map<String, ModInfo> MOD_INFO = new HashMap<>();
    private static final Map<String, ModernConfig> MOD_CONFIG_INSTANCES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Resolve config file path using Fabric's config directory so save/load persist across restarts. */
    private static Path getConfigPath(String modId) {
        return FabricLoader.getInstance().getConfigDir().resolve(modId + ".json");
    }

    public static void registerConfig(String modId, Map<String, Object> config) {
        modId = modId.toLowerCase();
        MOD_CONFIGS.put(modId, config);
        load();
    }

    public static void registerConfig(String modId, String name, String description, Map<String, Object> config) {
        modId = modId.toLowerCase();
        MOD_CONFIGS.put(modId, config);
        MOD_INFO.put(modId, new ModInfo(name, description));
        load();
    }

    public static void registerConfig(String modId, String name, String description, Identifier icon, Map<String, Object> config) {
        modId = modId.toLowerCase();
        MOD_CONFIGS.put(modId, config);
        MOD_INFO.put(modId, new ModInfo(name, description, icon));
        load();
    }

    public static void registerConfig(String modId, String name, String description, Identifier icon, ModernConfigTheme theme, Map<String, Object> config) {
        modId = modId.toLowerCase();
        MOD_CONFIGS.put(modId, config);
        MOD_INFO.put(modId, new ModInfo(name, description, icon, theme));
        load();
    }

    /** True during load - prevents setValue from triggering save and overwriting the file with partial/defaults. */
    static boolean isLoading = false;

    public static void load() {
        isLoading = true;
        try {
            for (String modId : MOD_CONFIGS.keySet()) {
                Path path = getConfigPath(modId);
                if (Files.exists(path)) {
                    try (Reader reader = Files.newBufferedReader(path)) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        Map<String, Object> config = MOD_CONFIGS.get(modId);
                        if (config != null) {
                            loadOptionsRecursive(json, config);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            isLoading = false;
        }
    }

    public static void save() {
        for (Map.Entry<String, Map<String, Object>> entry : MOD_CONFIGS.entrySet()) {
            String modId = entry.getKey();
            Map<String, Object> config = entry.getValue();
            JsonObject json = new JsonObject();
            saveOptionsRecursive(json, config);

            try {
                Path path = getConfigPath(modId);
                Files.createDirectories(path.getParent());
                try (Writer writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(json, writer);
                }

                // Notify listeners after successful save
                ModernConfig modernConfig = MOD_CONFIG_INSTANCES.get(modId);
                if (modernConfig != null) {
                    modernConfig.notifySaveListeners();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadOptionsRecursive(JsonObject json, Map<String, Object> options) {
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (json.has(key)) {
                JsonElement jsonValue = json.get(key);

                if (value instanceof ConfigOption<?>) {
                    ConfigOption<?> option = (ConfigOption<?>) value;
                    if (jsonValue.isJsonObject()) continue;

                    try {
                        if (option instanceof ListConfigOption && jsonValue.isJsonArray()) {
                            List<String> list = new ArrayList<>();
                            for (JsonElement element : jsonValue.getAsJsonArray()) {
                                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                                    list.add(element.getAsString());
                                }
                            }
                            ((ListConfigOption) option).setValue(list);
                        } else if (option.getDefaultValue() instanceof Boolean)
                            ((ConfigOption<Boolean>) option).setValue(jsonValue.getAsBoolean());
                        else if (option.getDefaultValue() instanceof Integer)
                            ((ConfigOption<Integer>) option).setValue(jsonValue.getAsInt());
                        else if (option.getDefaultValue() instanceof Float)
                            ((ConfigOption<Float>) option).setValue(jsonValue.getAsFloat());
                        else if (option.getDefaultValue() instanceof Double)
                            ((ConfigOption<Double>) option).setValue(jsonValue.getAsDouble());
                        else if (option.getDefaultValue() instanceof String)
                            ((ConfigOption<String>) option).setValue(jsonValue.getAsString());
                    } catch (Exception e) {
                        // Skip if value can't be converted to the correct type
                    }
                } else if (value instanceof CategoryInfo && jsonValue.isJsonObject()) {
                    CategoryInfo categoryInfo = (CategoryInfo) value;
                    loadOptionsRecursive(jsonValue.getAsJsonObject(), categoryInfo.getOptions());
                } else if (value instanceof Map && jsonValue.isJsonObject()) {
                    loadOptionsRecursive(jsonValue.getAsJsonObject(), (Map<String, Object>) value);
                }
            }
        }
    }

    private static void saveOptionsRecursive(JsonObject json, Map<String, Object> options) {
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof ConfigOption<?>) {
                ConfigOption<?> option = (ConfigOption<?>) value;
                Object val = option.getValue();
                if (option instanceof ListConfigOption) {
                    JsonArray array = new JsonArray();
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) val;
                    for (String item : list) {
                        array.add(item);
                    }
                    json.add(key, array);
                } else if (val instanceof Boolean)
                    json.addProperty(key, (Boolean) val);
                else if (val instanceof Integer)
                    json.addProperty(key, (Integer) val);
                else if (val instanceof Float)
                    json.addProperty(key, (Float) val);
                else if (val instanceof Double)
                    json.addProperty(key, (Double) val);
                else if (val instanceof String)
                    json.addProperty(key, (String) val);
            } else if (value instanceof CategoryInfo) {
                JsonObject categoryJson = new JsonObject();
                CategoryInfo categoryInfo = (CategoryInfo) value;
                saveOptionsRecursive(categoryJson, categoryInfo.getOptions());
                json.add(key, categoryJson);
            } else if (value instanceof Map) {
                JsonObject categoryJson = new JsonObject();
                @SuppressWarnings("unchecked")
                Map<String, Object> categoryMap = (Map<String, Object>) value;
                saveOptionsRecursive(categoryJson, categoryMap);
                json.add(key, categoryJson);
            }
        }
    }

    public static Map<String, Object> getConfig(String modId) {
        return MOD_CONFIGS.get(modId.toLowerCase());
    }

    public static ModInfo getModInfo(String modId) {
        modId = modId.toLowerCase();
        return MOD_INFO.get(modId);
    }

    public static ModernConfig getModConfig(String modId) {
        modId = modId.toLowerCase();
        ModernConfig config = MOD_CONFIG_INSTANCES.get(modId);
        if (config == null) {
            Map<String, Object> configData = MOD_CONFIGS.get(modId);
            if (configData != null) {
                config = new ModernConfig(modId, configData);
                MOD_CONFIG_INSTANCES.put(modId, config);
            }
        }
        return config;
    }

    public static Map<String, Map<String, Object>> getAllConfigs() {
        return MOD_CONFIGS;
    }

    public static ConfigOption<?> getOption(String modId, String category, String option) {
        ModernConfig config = getModConfig(modId);
        return config != null ? config.getOption(category, option) : null;
    }

}
