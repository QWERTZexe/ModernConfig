package app.qwertz.modernconfig.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class CategoryInfo {
    private final String title;
    private final String description;
    private final Map<String, Object> options = new LinkedHashMap<>();

    public CategoryInfo(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void addOption(String key, ConfigOption<?> option) {
        options.put(key, option);
    }

    public void addCategory(String key, CategoryInfo category) {
        options.put(key, category);
    }
} 