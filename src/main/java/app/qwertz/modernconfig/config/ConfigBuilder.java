package app.qwertz.modernconfig.config;

import net.minecraft.util.Identifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ConfigBuilder {
    private final Map<String, Object> options = new LinkedHashMap<>();
    private final ConfigBuilder parent;
    private final String name;
    private final String description;
    private final String id;
    private final Identifier icon;

    private ConfigBuilder(String id, String name, String description, Identifier icon, ConfigBuilder parent) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.parent = parent;
    }

    public static ConfigBuilder create(String name, String description) {
        return new ConfigBuilder(name.toLowerCase(), name, description, null, null);
    }

    public static ConfigBuilder create(String name, String description, Identifier icon) {
        return new ConfigBuilder(name.toLowerCase(), name, description, icon, null);
    }

    public ConfigBuilder category(String id, String displayName, String description) {
        return new ConfigBuilder(id, displayName, description, null, this);
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

    public ConfigBuilder list(String id, String name, String childName) {
        options.put(id, new ListConfigOption(id, name, name, childName));
        return this;
    }

    public ConfigBuilder color(String id, String name, int defaultValue) {
        options.put(id, new ColorConfigOption(id, name, name, defaultValue));
        return this;
    }

    public ConfigBuilder dropdown(String id, String name, List<String> options, String defaultValue) {
        this.options.put(id, new DropdownConfigOption(id, name, name, options, defaultValue));
        return this;
    }

    public ConfigBuilder dropdown(String id, String name, List<String> options) {
        this.options.put(id, new DropdownConfigOption(id, name, name, options));
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

    public Map<String, Object> buildRaw() {
        if (parent != null) {
            throw new IllegalStateException("build() can only be called on the root builder. Use end() to finish a category.");
        }
        return options;
    }

    public ModernConfig build() {
        if (parent != null) {
            throw new IllegalStateException("buildConfig() can only be called on the root builder. Use end() to finish a category.");
        }
        return ModernConfig.create(id, name, description, icon, options);
    }

    public Identifier getIcon() {
        return icon;
    }
}