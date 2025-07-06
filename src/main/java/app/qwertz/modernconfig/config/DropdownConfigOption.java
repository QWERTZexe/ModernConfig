package app.qwertz.modernconfig.config;

import java.util.List;
import java.util.ArrayList;

public class DropdownConfigOption extends ConfigOption<String> {
    private final List<String> options;
    
    public DropdownConfigOption(String key, String description, String category, List<String> options, String defaultValue) {
        super(key, description, category, defaultValue);
        this.options = new ArrayList<>(options);
        
        // Ensure default value is in options, if not add it
        if (!this.options.contains(defaultValue)) {
            this.options.add(0, defaultValue);
        }
    }
    
    public DropdownConfigOption(String key, String description, String category, List<String> options) {
        this(key, description, category, options, options.isEmpty() ? "" : options.get(0));
    }
    
    public List<String> getOptions() {
        return new ArrayList<>(options);
    }
    
    public int getSelectedIndex() {
        String currentValue = getValue();
        int index = options.indexOf(currentValue);
        return index >= 0 ? index : 0;
    }
    
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            setValue(options.get(index));
        }
    }
    
    public String getSelectedOption() {
        return getValue();
    }
    
    public void setSelectedOption(String option) {
        if (options.contains(option)) {
            setValue(option);
        }
    }
    
    public int getOptionCount() {
        return options.size();
    }
    
    public String getOptionAt(int index) {
        if (index >= 0 && index < options.size()) {
            return options.get(index);
        }
        return "";
    }
} 