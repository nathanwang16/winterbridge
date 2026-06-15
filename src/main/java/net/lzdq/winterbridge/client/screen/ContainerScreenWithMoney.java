package net.lzdq.winterbridge.client.screen;

import com.mojang.blaze3d.platform.InputConstants;

import net.lzdq.winterbridge.WinterBridge;
import net.lzdq.winterbridge.client.ModKeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

public class ContainerScreenWithMoney extends ContainerScreen {
	public ContainerScreenWithMoney(ChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
	
    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        // Implement your custom key handling logic here
        if(super.keyPressed(keyEvent))
			return true;
		for (KeyMapping keymap : ModKeyBindings.INSTANCE.keys.values()){
			InputConstants.Key key = InputConstants.getKey(keyEvent);
			if (keymap.getCategory().equals(ModKeyBindings.CATEGORY_INVENTORY) &&
					keymap.getKey().equals(key)){
				KeyMapping.click(key);
			}
		}
		return true;  // Consume the event
    }
}
