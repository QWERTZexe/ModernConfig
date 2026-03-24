package app.qwertz.modernconfig.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class ItemConfigOption extends ConfigOption<Identifier> {
    
    public ItemConfigOption(String id, String name, String description, Identifier defaultValue) {
        super(id, name, description, defaultValue);
    }
    
    public ItemConfigOption(String id, String name, String description, String defaultItemId) {
        super(id, name, description, Identifier.parse(defaultItemId));
    }
    
    public String getItemId() {
        return getValue().toString();
    }
    
    public void setItemId(String itemId) {
        setValue(Identifier.parse(itemId));
    }
    
    public Item getItem() {
        return BuiltInRegistries.ITEM.getValue(getValue());
    }
} 