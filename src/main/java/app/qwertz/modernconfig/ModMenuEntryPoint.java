package app.qwertz.modernconfig;

import app.qwertz.modernconfig.config.ModernConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuEntryPoint implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::getModMenuConfigScreen;
    }

    public Screen getModMenuConfigScreen(Screen screen) {
        return ModernConfig.getGlobalConfigScreen();
    }
}
