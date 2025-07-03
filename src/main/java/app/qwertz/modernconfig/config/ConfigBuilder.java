package app.qwertz.modernconfig.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ConfigBuilder {
    private final Map<String, Object> options = new LinkedHashMap<>();
    private final ConfigBuilder parent;
    private final String name;
    private final String description;
    private final String id;

    private ConfigBuilder(String id, String name, String description, ConfigBuilder parent) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.parent = parent;
    }

    public static ConfigBuilder create(String name, String description) {
        return new ConfigBuilder(name.toLowerCase(), name, description, null);
    }

    public ConfigBuilder category(String id, String displayName, String description) {
        return new ConfigBuilder(id, displayName, description, this);
    }

    public ConfigBuilder category(String id, String displayName, String description, Consumer<ConfigBuilder> builder) {
        ConfigBuilder category = category(id, displayName, description);
        builder.accept(category);
        return this;
    }

    public ConfigBuilder toggle(String id, String name, boolean defaultValue) {
        options.put(id, new ConfigOption<>(id, name, name, defaultValue));
        return this;
    }

    public ConfigBuilder text(String id, String name, String defaultValue) {
        options.put(id, new ConfigOption<>(id, name, name, defaultValue));
        return this;
    }

    public ConfigBuilder slider(String id, String name, int defaultValue, int min, int max, int step) {
        options.put(id, new SliderConfigOption(id, name, name, defaultValue, min, max, step));
        return this;
    }

    public ConfigBuilder list(String id, String name) {
        options.put(id, new ListConfigOption(id, name, name));
        return this;
    }

    public <T> ConfigBuilder option(String id, String name, T defaultValue) {
        options.put(id, new ConfigOption<>(id, name, name, defaultValue));
        return this;
    }

    public <T, O extends ConfigOption<T>> ConfigBuilder option(String id, O option) {
        options.put(id, option);
        return this;
    }

    public ConfigBuilder end() {
        if (parent != null) {
            CategoryInfo category = new CategoryInfo(name, description);
            category.getOptions().putAll(options);
            parent.options.put(id, category);
            return parent;
        }
        return this;
    }

    public Map<String, Object> build() {
        if (parent != null) {
            throw new IllegalStateException("build() can only be called on the root builder. Use end() to finish a category.");
        }
        return options;
    }
}