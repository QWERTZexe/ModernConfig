package app.qwertz.modernconfig.config;

import java.util.ArrayList;
import java.util.List;

public class ListConfigOption extends ConfigOption<List<String>> {
    private final String childName;
    
    public ListConfigOption(String key, String description, String category, List<String> defaultValue, String childName) {
        super(key, description, category, new ArrayList<>(defaultValue));
        this.childName = childName != null ? childName : "Item";
    }

    public ListConfigOption(String key, String description, String category, String childName) {
        super(key, description, category, new ArrayList<>());
        this.childName = childName != null ? childName : "Item";
    }

    public ListConfigOption(String key, String description, String category) {
        super(key, description, category, new ArrayList<>());
        this.childName = "Item"; // Default fallback
    }

    public void addItem(String item) {
        if (item != null && !item.trim().isEmpty() && !getValue().contains(item.trim())) {
            getValue().add(item.trim());
        }
    }

    public void removeItem(int index) {
        if (index >= 0 && index < getValue().size()) {
            getValue().remove(index);
        }
    }

    public void removeItem(String item) {
        getValue().remove(item);
    }

    public void updateItem(int index, String newValue) {
        if (index >= 0 && index < getValue().size() && newValue != null && !newValue.trim().isEmpty()) {
            getValue().set(index, newValue.trim());
        }
    }

    public int size() {
        return getValue().size();
    }

    public boolean isEmpty() {
        return getValue().isEmpty();
    }

    public List<String> getItems() {
        return new ArrayList<>(getValue());
    }

    public void setItems(List<String> items) {
        setValue(new ArrayList<>(items));
    }
    
    public String getChildName() {
        return childName;
    }
} 