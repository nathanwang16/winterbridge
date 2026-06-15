package net.lzdq.winterbridge.client;

import net.lzdq.winterbridge.WinterBridge;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

// @EventBusSubscriber auto-routes each @SubscribeEvent to the correct bus by
// event type (RegisterKeyMappingsEvent is a mod-bus event).
@EventBusSubscriber(modid = WinterBridge.MODID, value = Dist.CLIENT)
public class ClientModHandler {
	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent event){
		// 1.21.x requires registering the categories before the mappings that use them.
		event.registerCategory(ModKeyBindings.CATEGORY);
		event.registerCategory(ModKeyBindings.CATEGORY_INVENTORY);
		for (KeyMapping key : ModKeyBindings.INSTANCE.keys.values()){
			WinterBridge.LOGGER.info("Registering key '{}'", key.getName());
			event.register(key);
		}
	}
}
