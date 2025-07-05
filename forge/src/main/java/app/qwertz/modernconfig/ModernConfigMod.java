package app.qwertz.modernconfig;

import app.qwertz.modernconfig.config.ConfigManager;
import app.qwertz.modernconfig.config.ModernConfig;
import app.qwertz.modernconfig.ui.ConfigScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ModernConfigMod.MODID)
public class ModernConfigMod {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "modernconfig";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    private static KeyMapping configKeyBinding;

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            // Register keybind
            configKeyBinding = new KeyMapping(
                "key.modernconfig.open_config",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.modernconfig.general"
            );
            event.register(configKeyBinding);
        }
    }

    // Client tick event handler
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (configKeyBinding != null && configKeyBinding.consumeClick()) {
                ModernConfig.openGlobalConfig();
            }
        }
    }
}
