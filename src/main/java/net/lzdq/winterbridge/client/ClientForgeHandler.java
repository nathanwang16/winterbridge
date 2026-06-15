package net.lzdq.winterbridge.client;

import net.lzdq.winterbridge.ModConfig;
import static net.lzdq.winterbridge.Utils.*;
import net.lzdq.winterbridge.WinterBridge;
import net.lzdq.winterbridge.client.action.ActionHandler;
import net.lzdq.winterbridge.client.action.AutoClicker;
import net.lzdq.winterbridge.client.action.DoubleClickHandler;
import net.lzdq.winterbridge.client.blockin.BlockInHandler;
import net.lzdq.winterbridge.client.bridge.*;
import net.lzdq.winterbridge.client.clutch.AbstractClutchHandler;
import net.lzdq.winterbridge.client.clutch.BlockClutchHandler;
import net.lzdq.winterbridge.client.screen.ContainerScreenWithMoney;
import net.lzdq.winterbridge.client.action.RotateHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

@EventBusSubscriber(modid = WinterBridge.MODID, value = Dist.CLIENT)
public class ClientForgeHandler {
	static Minecraft mc;
	static Inventory inv;
	static boolean cancelled = false;
	static boolean inventoryOpen = false, chestOpen = false;
	static boolean isDoubleAttack = false;
	// Connection-state tracking (formerly piggybacked on the now-removed PacketInspector).
	static boolean connected = false, wasConnected = false;
	static int spam_left_mode = 1; // 0 - do not spam until hit entity (after switch)  1 - spam
	static int spam_right_mode = 0; // 0 - Not down  1 - Down but not click  2 - Down and click
	static final AutoClicker leftClicker = new AutoClicker();  // attack (left) auto-click engine
	static final AutoClicker rightClicker = new AutoClicker(); // place (right) auto-click engine
	static long until = 0, lastBlockinTime = 0;
	static AbstractBridgeHandler bridgeHandler;
	static AbstractClutchHandler clutchHandler;
	static BlockInHandler blockinHandler;
	static DoubleClickHandler doubleClickHandler;
	static List<Item> moneyItems = Arrays.asList(Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT);
	static boolean last_inc = false; // avoid sending duplicate messages

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Pre event) {
		mc = Minecraft.getInstance();
		connected = (mc.player != null);
		if (connected != wasConnected) {
			wasConnected = connected;
			if (connected) {
				spam_right_mode = 0;
				leftClicker.reset();
				rightClicker.reset();
				bridgeHandler = null;
				clutchHandler = null;
				CheatMode.changeCheatMode(ModConfig.cheat_mode.get());
			}
		}

		if (!connected)
			return;

		inv = mc.player.getInventory();
		inventoryOpen = (mc.screen instanceof InventoryScreen);
		chestOpen = (mc.screen instanceof ContainerScreen);

		if (!inventoryOpen && !chestOpen) {
			if (ModKeyBindings.INSTANCE.get("ninja").consumeClick())
				startBridge("ninja");

			if (ModKeyBindings.INSTANCE.get("ninja_inc3").consumeClick())
				startBridge("ninja_inc3");

			if (ModKeyBindings.INSTANCE.get("ninja_diag").consumeClick())
				startBridge("ninja_diag");

			if (ModKeyBindings.INSTANCE.get("ninja_diag_inc").consumeClick())
				startBridge("ninja_diag_inc");

			handleSpamClickRight();

			if (ModKeyBindings.INSTANCE.get("cancel").consumeClick()) {
				cancelled = true;
				RotateHandler.setCancelled();
			}

			if (ModKeyBindings.INSTANCE.get("change_cheat_mode").consumeClick())
				CheatMode.changeCheatMode();

			if (ModKeyBindings.INSTANCE.get("fireball").consumeClick())
				switchToItem(Items.FIRE_CHARGE);

			if (ModKeyBindings.INSTANCE.get("epearl").consumeClick())
				switchToItem(Items.ENDER_PEARL);

			if (ModKeyBindings.INSTANCE.get("egg").consumeClick())
				switchToItem(Items.EGG);

			if (ModKeyBindings.INSTANCE.get("gapple").consumeClick())
				switchToItem(Items.GOLDEN_APPLE);

			if (ModKeyBindings.INSTANCE.get("ladder_or_def").consumeClick()){
				if (System.currentTimeMillis() <
						lastBlockinTime + ModConfig.blockin_post_time.get() * 1000
						|| !switchToItem(Items.LADDER))
					switchToHardestBlock();
			}

			if (ModKeyBindings.INSTANCE.get("bow").consumeClick())
				switchToItem(Items.BOW);

			if (ModKeyBindings.INSTANCE.get("potions").consumeClick())
				switchBetweenPotions();

			if (ModKeyBindings.INSTANCE.get("blockin").consumeClick()) {
				cancelled = false;
				if (blockinHandler == null) {
					blockinHandler = new BlockInHandler();
					lastBlockinTime = System.currentTimeMillis();
				}
			}

			if (ModKeyBindings.INSTANCE.get("drop_money").consumeClick())
				normalDropMoney();

			if (ModKeyBindings.INSTANCE.get("tnt").consumeClick())
				switchToItem(Items.TNT);

			if (ModKeyBindings.INSTANCE.get("ice_bridge").consumeClick())
				switchToItem(Items.ICE);

			if (ModKeyBindings.INSTANCE.get("func_e").consumeClick())
				autoE();

			if (ModKeyBindings.INSTANCE.get("auto_login").consumeClick())
				mc.player.connection.sendCommand(ModConfig.auto_login_command.get());

			if (ModKeyBindings.INSTANCE.get("auto_who").consumeClick())
				mc.player.connection.sendCommand("who");

			if (ModKeyBindings.INSTANCE.get("auto_send_inc").consumeClick()) {
				if (last_inc ^= true)
					mc.player.connection.sendChat("inc");
				else
					mc.player.connection.sendChat("incc");
			}

			if (CheatMode.cheat_mode < 2 &&
					isBlock(mc.player.getMainHandItem()) &&
					mc.options.keyAttack.consumeClick()) {
				// Holding block and clicking. Block clutch or double-click
				// If NOT onGround and has block 5 below, do a double-click
				// otherwise (no block underneath), do a block clutch
				boolean isDoubleClick = false;
				if (!mc.player.onGround()) {
					BlockPos pos = mc.player.getOnPos();
					for(int i=0; !isDoubleClick && i<5; i++){
						if (!mc.level.getBlockState(pos).isAir())
							isDoubleClick = true;
						pos = pos.below();
					}
				}
				if (isDoubleClick){
					if (doubleClickHandler == null)
						doubleClickHandler = new DoubleClickHandler();
				} else {
					if (clutchHandler == null)
						clutchHandler = new BlockClutchHandler();
				}
			}

			if (CheatMode.cheat_mode < 2 &&
					mc.hitResult != null && mc.hitResult.getType() == Type.ENTITY &&
					(mc.player.getInventory().getSelectedSlot() == 0 ||
					 mc.player.getInventory().getSelectedItem().is(Items.STICK)) &&
					mc.options.keyUse.consumeClick()){
				// Holding sword or stick, pointing to an entity and right click (consume)
				// Do a double click = click once and click later in another frame
				KeyMapping.click(mc.options.keyAttack.getKey());
				isDoubleAttack = true;
				until = System.currentTimeMillis() + ModConfig.delay_double_attack.get();
			}

			if (CheatMode.cheat_mode < 2 && mc.options.keyJump.isDown() && mc.player.fallDistance > 4)
				blockLadderClutch();

		} else {
			// Handle the sharing keys
			if (ModKeyBindings.INSTANCE.get("store_money").consumeClick())
				storeMoney();

			if (ModKeyBindings.INSTANCE.get("get_money").consumeClick())
				getMoney();

			if (ModKeyBindings.INSTANCE.get("second_slot").consumeClick())
				swapMenuSlot(1);

			if (ModKeyBindings.INSTANCE.get("last_slot").consumeClick())
				swapMenuSlot(8);

			// Handle the chest keys
			if (chestOpen) {
				if (mc.player.containerMenu == null) {
					WinterBridge.LOGGER.error("chestOpen, containerMenu is null");
				} else if (mc.player.containerMenu.containerId == 0) {
					WinterBridge.LOGGER.error("chestOpen, containerMenu is 0");
				}
			}
			// Handle the inventory keys
			if (inventoryOpen) {
				if (mc.player.containerMenu == null) {
					WinterBridge.LOGGER.error("inventoryOpen, containerMenu is null");
				} else if (mc.player.containerMenu.containerId > 0) {
					WinterBridge.LOGGER.error("inventoryOpen, containerMenu is {}",
							mc.player.containerMenu.containerId);
				} else {
					if (ModKeyBindings.INSTANCE.get("drop_money").consumeClick()) {
						inventoryDropMoney();
					}
				}
			}
		}

		if (System.currentTimeMillis() < until)
			return;
		handleSpamClickLeft(); // To check whether is switching

		if (clutchHandler != null) {
			if (clutchHandler.isFinished())
				clutchHandler = null;
			else
				clutchHandler.tick();
		}

		if (blockinHandler != null) {
			if (blockinHandler.isFinished())
				blockinHandler = null;
			else if (cancelled)
				blockinHandler = null;
			else
				blockinHandler.tick();
		}

		if (doubleClickHandler != null){
			if (doubleClickHandler.isFinished())
				doubleClickHandler = null;
			else doubleClickHandler.tick();
		}
	}

	@SubscribeEvent
	public static void onClientTickControl(ClientTickEvent.Post event) {
		// The bridge state machine runs in the *Post* phase, restoring the original
		// mod's TickEvent.Phase.END timing. By Post, Minecraft has re-run
		// gameRenderer.pick() (fresh mc.hitResult) and ticked the player (this tick's
		// movement applied), so the bridge reads post-movement position and an
		// up-to-date look target instead of the stale, pre-movement state it saw when
		// the first port drove it from Pre. Keybind/click detection (incl. the
		// bridge keys) stays in Pre above. RotateHandler.tick() moves here too so it
		// still advances right after bridgeHandler.tick(), as it did originally.
		mc = Minecraft.getInstance();
		if (mc.player == null)
			return;
		if (System.currentTimeMillis() < until)
			return;

		if (bridgeHandler != null) {
			bridgeHandler.tick();
			if (cancelled)
				bridgeHandler.setCancelled("manual");
			if (bridgeHandler.isFinished())
				bridgeHandler = null;
		}

		RotateHandler.tick();
	}

	private static boolean switchToItem(Item item) {
		for (int i = 0; i < 9; i++)
			if (inv.getItem(i).is(item)) {
				inv.setSelectedSlot(i);
				return true;
			}
		return false;
	}

	private static void switchBetweenPotions() {
		List<String> potions = Arrays.asList("jump", "invis", "milk", "speed");
		List<String> tp = new ArrayList<>();

		for (int i = 0; i < 9; i++) {
			ItemStack stack = inv.getItem(i);
			tp.add("");
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			// Guard on PotionItem so tipped arrows (which also carry POTION_CONTENTS)
			// are not misclassified as drinkable potions.
			if (stack.getItem() instanceof PotionItem && contents != null) {
				WinterBridge.LOGGER.debug("potion at {} slot", i);
				for (MobEffectInstance effect : contents.getAllEffects()) {
					if (effect.getEffect().is(MobEffects.JUMP_BOOST))
						tp.set(i, "jump");
					else if (effect.getEffect().is(MobEffects.INVISIBILITY))
						tp.set(i, "invis");
					else if (effect.getEffect().is(MobEffects.SPEED))
						tp.set(i, "speed");
				}
			} else if (stack.is(Items.MILK_BUCKET)) {
				tp.set(i, "milk");
			}
		}
		WinterBridge.LOGGER.debug("type in hand: {}", tp.get(inv.getSelectedSlot()));
		for (int i = 1; i <= potions.size(); i++) {
			// i is the offset, j is the real one switching to
			// When not holding a potion, indexOf starts at -1
			int j = (i + potions.indexOf(tp.get(inv.getSelectedSlot()))) % potions.size();
			int k = tp.indexOf(potions.get(j));
			if (k != -1) {
				inv.setSelectedSlot(k);
				return;
			}
		}
	}

	private static void normalDropMoney() {
		// Drop money (from emerald to iron). Inventory not open
		int t = moneyItems.indexOf(inv.getSelectedItem().getItem());
		if (t != -1) {
			// Drop all money in the current slot
			mc.player.drop(true);
			return;
		}
		for (Item money : moneyItems)
			for (int i = 0; i < 9; i++)
				if (inv.getItem(i).is(money)) {
					inv.setSelectedSlot(i);
					return;
				}
	}

	private static void inventoryDropMoney() {
		InventoryMenu menu = mc.player.inventoryMenu;
		List<ItemStack> items = menu.getItems();
		for (Item money : moneyItems)
			for (int i = 0; i < items.size(); i++)
				if (items.get(i).is(money)) {
					mc.gameMode.handleInventoryMouseClick(
							menu.containerId,
							i,
							1, // 0 for throw one, 1 for throw all
							ClickType.THROW,
							mc.player);
					return;
				}
	}

	private static void storeMoney() {
		WinterBridge.LOGGER.info("Storing money into {}", inventoryOpen ? "inventory" : "chest");
		AbstractContainerMenu menu = mc.player.containerMenu;
		List<ItemStack> items = menu.getItems();
		for (Item money : moneyItems)
			// The last slots are player's inventory
			for (int i = items.size() - (inventoryOpen ? 9 : inv.getNonEquipmentItems().size()); i < items.size(); i++)
				if (items.get(i).is(money)) {
					WinterBridge.LOGGER.debug("Found money at {}, containerId={}", i,
							menu.containerId);
					mc.gameMode.handleInventoryMouseClick(
							menu.containerId,
							i,
							0,
							ClickType.QUICK_MOVE,
							mc.player);
					return;
				}
	}

	private static void getMoney() {
		WinterBridge.LOGGER.info("Get money from {}", inventoryOpen ? "inventory" : "chest");
		AbstractContainerMenu menu = mc.player.containerMenu;
		List<ItemStack> items = menu.getItems();
		for (Item money : moneyItems)
			// The last slots are player's inventory
			for (int i = 0; i < items.size() - (inventoryOpen ? 9 : inv.getNonEquipmentItems().size()); i++)
				if (items.get(i).is(money)) {
					WinterBridge.LOGGER.debug("Found money at {}, containerId={}", i,
							mc.player.containerMenu.containerId);
					mc.gameMode.handleInventoryMouseClick(
							menu.containerId,
							i,
							0,
							ClickType.QUICK_MOVE,
							mc.player);
					return;
				}
	}

	private static void switchToHardestBlock(){
		String[] blocks = {
			"Obsidian",
			"Clay", // Use clay for inner-most layer def
			"End Stone",
			"Plank", "Log",
			"Wool"
		};
		for (int i = 0; i < 9; i++) {
			for (String s : blocks) {
				if (inv.getSelectedItem().getDisplayName().getString().contains(s))
					break;
				if (inv.getItem(i).getDisplayName().getString().contains(s)) {
					inv.setSelectedSlot(i);
					break;
				}
			}
		}
	}

	@SubscribeEvent
	public static void onRenderTick(RenderFrameEvent.Pre event) {
		// RenderFrameEvent.Pre can fire before the first client tick has set `mc`
		// (e.g. during title-screen / disconnect rendering), so initialise it here
		// and bail when not in a world.
		mc = Minecraft.getInstance();
		if (mc.player == null)
			return;
		if (System.currentTimeMillis() < until)
			return;
		handleSpamClickLeft();
		if (isDoubleAttack){
			KeyMapping.click(mc.options.keyAttack.getKey());
			isDoubleAttack = false;
		}
	}

	private static void swapMenuSlot(int slot_to) {
		// slot_to is [0, 8] indicating the inventory slot
		// Swap it with current menu
		int slot_from = -1;
		if (inventoryOpen) {
			Slot slot = ((InventoryScreen) mc.screen).getSlotUnderMouse();
			if (slot == null)
				return;
			slot_from = slot.getSlotIndex();
			if (slot_from < 9)
				slot_from += InventoryMenu.USE_ROW_SLOT_START;
		} else if (chestOpen) {
			Slot slot = ((ContainerScreen) mc.screen).getSlotUnderMouse();
			if (slot == null)
				return;
			slot_from = slot.getSlotIndex();
			if (slot.container == inv) {
				if (slot_from < 9)
					slot_from += 36;
				slot_from += 18;
			}
		}
		if (slot_from != -1)
			mc.gameMode.handleInventoryMouseClick(
					mc.player.containerMenu.containerId,
					slot_from,
					slot_to,
					ClickType.SWAP,
					mc.player);
	}

	private static void blockLadderClutch() {
		// Try a block-ladder clutch
		if (mc.player.getMainHandItem().is(Items.LADDER)){
			// Do a ladder clutch
			WinterBridge.LOGGER.debug("Holding ladder");
			if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
				BlockHitResult blockHitResult = (BlockHitResult) mc.hitResult;
				BlockPos ladderPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection()),
						 playerPos = mc.player.getOnPos();
				WinterBridge.LOGGER.debug("Possibly placing a ladder. ladder position X: {} Z: {}  player position X: {} Z: {}",
						ladderPos.getX(), ladderPos.getZ(), playerPos.getX(), playerPos.getZ());
				if (ladderPos.getX() == playerPos.getX() && ladderPos.getZ() == playerPos.getZ()){
					if (ActionHandler.placeBlock())
						WinterBridge.LOGGER.info("Ladder clutched");
					else WinterBridge.LOGGER.info("Ladder clutch failed");
				}
			}
		} else if (isBlock(mc.player.getMainHandItem())) {
			// Check there is no jump boost
			if (mc.player.hasEffect(MobEffects.JUMP_BOOST))
				return;
			// Place a block first, then switch to ladder (if has)
			if (ActionHandler.placeBlock()){
				int ticks = ModConfig.ladder_rotate_tick.get();
				if (ticks > 0){
					// Rotate down
					Vec2 rot = mc.player.getRotationVector();
					rot = new Vec2(89 + (float)(Math.random()) / 2, rot.y); // Look down
					RotateHandler.init(rot, ticks);
				}
				switchToItem(Items.LADDER);
			}
		}
	}

	private static void autoSwitchTool() {
		int slot = inv.getFreeSlot(); // empty slot or sword
		if (slot == -1 || !Inventory.isHotbarSlot(slot))
			slot = 0;
		if (mc.hitResult.getType() != HitResult.Type.BLOCK) {
			if (isBlock(inv.getSelectedItem()))
				inv.setSelectedSlot(slot);
			return;
		}
		BlockHitResult hit = (BlockHitResult) mc.hitResult;
		BlockState blockState = mc.level.getBlockState(hit.getBlockPos());
		for (int i = 0; i < 9; i++)
			if (inv.getItem(i).getDestroySpeed(blockState) > inv.getItem(slot).getDestroySpeed(blockState))
				slot = i;
		inv.setSelectedSlot(slot);
	}

	private static void autoE() {
		/*
		 * Switch to tool or KB-stick.
		 * Rule: if no KB-stick, switch to tool. Avoid holding block.
		 * If pointing to block, switch to tool; otherwise switch to KB-stick.
		 */
		int slot_kb = -1;
		// Only a hotbar stick can be held, so only search the hotbar (1.21 setSelectedSlot rejects non-hotbar slots).
		for (int i = 0; i < 9; i++)
			if (inv.getItem(i).is(Items.STICK)) {
				slot_kb = i;
				break;
			}
		if (slot_kb == -1) {
			autoSwitchTool();
		} else {
			if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
				autoSwitchTool();
			} else {
				inv.setSelectedSlot(slot_kb);
			}
		}
	}

	private static void handleSpamClickLeft() {
		// Auto-attack while holding the sword-select key (slot 1). The AutoClicker
		// governs the cadence (CPS cap + jitter + skip); the guards here decide
		// whether a scheduled click is actually allowed to land.
		if (mc.options.keyHotbarSlots[0].isDown()) {
			if (mc.player.getInventory().getSelectedSlot() != 0) {
				until = System.currentTimeMillis() + ModConfig.delay_sword.get();
				spam_left_mode = 0; // do not spam until hit entity or next hold
				return;
			}
			if (spam_left_mode == 0) {
				if (mc.hitResult.getType() == Type.ENTITY)
					spam_left_mode = 1;
				else return;
			}
			if (leftClicker.tryClick(System.currentTimeMillis())) {
				// On a scheduled beat, swing if pointing at an entity; otherwise still
				// click most of the time so air-clicking stays human (miss prob).
				if (mc.hitResult.getType() == Type.ENTITY ||
						Math.random() < ModConfig.spam_miss_click_prob.get())
					KeyMapping.click(mc.options.keyAttack.getKey());
			}
		} else spam_left_mode = 1;
	}

	private static void handleSpamClickRight() {
		// Every client tick: place blocks while the "blocks" key is held, paced by
		// the AutoClicker. Block placement is tick-limited, so this mainly layers
		// the jitter/skip humanizing on top of ~tick-rate placement.
		if (ModKeyBindings.INSTANCE.get("blocks").isDown()) {
			if (spam_right_mode == 0) {
				if (isBlock(inv.getSelectedItem()) && inv.getSelectedItem().getCount() > 0) {
					// Already holding block. Start spam-click
					spam_right_mode = 2;
				} else {
					// Not holding, switch to block
					spam_right_mode = 1;
					int slot = inv.getSelectedSlot(), mxcnt = 0;
					for (int i = 0; i < 9; i++)
						if (isBlock(inv.getItem(i)) &&
								inv.getItem(i).getCount() > mxcnt) {
							slot = i;
							mxcnt = inv.getItem(i).getCount();
						}
					inv.setSelectedSlot(slot);
				}
			}
			if (spam_right_mode == 2 && rightClicker.tryClick(System.currentTimeMillis()))
				ActionHandler.placeBlock();
		} else
			spam_right_mode = 0;
	}

	private static void startBridge(String method) {
		mc.player.displayClientMessage(
				Component.literal("Start bridge: " + method)
						.withStyle(Style.EMPTY.withColor(0x00FF80)),
				true);
		cancelled = false;
		if (bridgeHandler == null) {
			if (method.startsWith("ninja") && !method.startsWith("ninja_diag"))
				bridgeHandler = new NinjaBridgeHandler(method);
			else if (method.startsWith("ninja_diag"))
				bridgeHandler = new NinjaDiagonalHandler(method);
			else if (method.equals("god"))
				bridgeHandler = new GodBridgeHandler();
		} else {
			bridgeHandler.update(method);
		}
	}

	@SubscribeEvent
	public static void onLivingHurt(LivingIncomingDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if (player == mc.player) {
				// Hit -> AutoCancel
				if (bridgeHandler != null)
					bridgeHandler.setCancelled("hit");
				if (blockinHandler != null)
					blockinHandler.setCancelled("hit");
			}
		}
	}

	@SubscribeEvent
	public static void onScreenOpen(ScreenEvent.Opening event) {
		KeyMapping.releaseAll(); // Release conflicting keys
		if (event.getScreen() instanceof InventoryScreen) {
			WinterBridge.LOGGER.debug("Opened inventory");
		}
		if (event.getScreen() instanceof ContainerScreen) {
			WinterBridge.LOGGER.debug("Opened a container");
			ContainerScreen chest = (ContainerScreen) event.getScreen();
			event.setNewScreen(new ContainerScreenWithMoney(
					chest.getMenu(),
					inv,
					chest.getTitle()));
		}
	}

	@SubscribeEvent
	public static void onScreenClose(ScreenEvent.Closing event) {
		KeyMapping.releaseAll(); // Release conflicting keys
		if (event.getScreen() instanceof InventoryScreen) {
			WinterBridge.LOGGER.debug("Closed inventory");
		}
		if (event.getScreen() instanceof ContainerScreen) {
			WinterBridge.LOGGER.debug("Closed a container");
		}
	}

	@SubscribeEvent
	public static void onPlayerDie(LivingDeathEvent event) {
		if (event.getEntity() == mc.player) {
			WinterBridge.LOGGER.debug("Player dies");
		}
	}
}
