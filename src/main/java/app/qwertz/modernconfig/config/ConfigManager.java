package app.qwertz.modernconfig.config;

import com.google.gson.*;
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
        
        public ModInfo(String name, String description) {
            this.name = name;
            this.description = description;
            this.icon = null;
        }
        
        public ModInfo(String name, String description, Identifier icon) {
            this.name = name;
            this.description = description;
            this.icon = icon;
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
    }
    
    private static final Map<String, Map<String, Object>> MOD_CONFIGS = new HashMap<>();
    private static final Map<String, ModInfo> MOD_INFO = new HashMap<>();
    private static final Map<String, String> CONFIG_PATHS = new HashMap<>();
    private static final Map<String, ModernConfig> MOD_CONFIG_INSTANCES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void registerConfig(String modId, Map<String, Object> config) {
        modId = modId.toLowerCase();
        MOD_CONFIGS.put(modId, config);
        CONFIG_PATHS.put(modId, "config/" + modId + ".json");
        load();
    }

    public static void registerConfig(String modId, String name, String description, Map<String, Object> config) {
        modId = modId.toLowerCase();
        MOD_CONFIGS.put(modId, config);
        MOD_INFO.put(modId, new ModInfo(name, description));
        CONFIG_PATHS.put(modId, "config/" + modId + ".json");
        load();
    }

    public static void registerConfig(String modId, String name, String description, Identifier icon, Map<String, Object> config) {
        modId = modId.toLowerCase();
        MOD_CONFIGS.put(modId, config);
        MOD_INFO.put(modId, new ModInfo(name, description, icon));
        CONFIG_PATHS.put(modId, "config/" + modId + ".json");
        load();
    }

    public static void load() {
        for (String modId : MOD_CONFIGS.keySet()) {
            String configPath = CONFIG_PATHS.get(modId);
            if (configPath == null) continue;

            Path path = Paths.get(configPath);
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
    }

    public static void save() {
        for (Map.Entry<String, Map<String, Object>> entry : MOD_CONFIGS.entrySet()) {
            String modId = entry.getKey();
            String configPath = CONFIG_PATHS.get(modId);
            if (configPath == null) continue;

            Map<String, Object> config = entry.getValue();
            JsonObject json = new JsonObject();
            saveOptionsRecursive(json, config);

            try {
                Path path = Paths.get(configPath);
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
