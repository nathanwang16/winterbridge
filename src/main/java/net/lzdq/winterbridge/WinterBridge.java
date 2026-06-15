package net.lzdq.winterbridge;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(WinterBridge.MODID)
public class WinterBridge {
	public static final String MODID = "winterbridge";
	public static final Logger LOGGER = LogUtils.getLogger();

	// NeoForge injects the mod event bus and mod container into the constructor.
	public WinterBridge(IEventBus modEventBus, ModContainer modContainer) {
		NeoForge.EVENT_BUS.register(this);
		modContainer.registerConfig(Type.CLIENT, ModConfig.CONFIG, "winterbridge-client.toml");
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		LOGGER.info("HELLO from server starting");
	}
}
