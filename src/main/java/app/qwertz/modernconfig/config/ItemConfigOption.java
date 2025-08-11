package app.qwertz.modernconfig.config;

import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

public class ItemConfigOption extends ConfigOption<Identifier> {
    
    public ItemConfigOption(String id, String name, String description, Identifier defaultValue) {
        super(id, name, description, defaultValue);
    }
    
    public ItemConfigOption(String id, String name, String description, String defaultItemId) {
        super(id, name, description, Identifier.of(defaultItemId));
    }
    
    public String getItemId() {
        return getValue().toString();
    }
    
    public void setItemId(String itemId) {
        setValue(Identifier.of(itemId));
    }
    
    public Item getItem() {
        return Registries.ITEM.get(getValue());
    }
} 