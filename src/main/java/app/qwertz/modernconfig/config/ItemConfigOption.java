package app.qwertz.modernconfig.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ItemConfigOption extends ConfigOption<ResourceLocation> {
    
    public ItemConfigOption(String id, String name, String description, ResourceLocation defaultValue) {
        super(id, name, description, defaultValue);
    }
    
    public ItemConfigOption(String id, String name, String description, String defaultItemId) {
        super(id, name, description, ResourceLocation.parse(defaultItemId));
    }
    
    public String getItemId() {
        return getValue().toString();
    }
    
    public void setItemId(String itemId) {
        setValue(ResourceLocation.parse(itemId));
    }
    
    public Item getItem() {
        return BuiltInRegistries.ITEM.getValue(getValue());
    }
} 