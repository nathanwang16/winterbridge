# CHANGELOG

## v2.6

* **Port to Minecraft 1.21.11 / NeoForge 21.11.42 (Java 21).** Hypixel and most servers dropped the 1.9–1.21.1 range (they support 1.8.x or ~1.21.2+), so the old 1.19.4 build could no longer connect; 1.21.11 is the newest 1.21.x and clears every current Hypixel cutoff (BedWars ≥1.21.2, SkyBlock ≥1.21.9).
* Build system migrated from ForgeGradle to **ModDevGradle 2.0.141** (Gradle 9.2.1); `mods.toml` → `src/main/templates/META-INF/neoforge.mods.toml`; `build.sh` now pins JDK 21.
* Loader migration: `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`, constructor injection of `IEventBus`/`ModContainer`, `@Mod.EventBusSubscriber` → `@EventBusSubscriber` (auto bus routing), `ForgeConfigSpec` → `ModConfigSpec`.
* Event renames: `TickEvent.ClientTickEvent`/`RenderTickEvent` → `ClientTickEvent.Pre`/`RenderFrameEvent.Pre`; `LivingHurtEvent` → `LivingIncomingDamageEvent`.
* Vanilla API renames: `isOnGround()`→`onGround()`, `getBlockReach()`→`blockInteractionRange()`, `Direction.getNormal()`→`getUnitVec3i()`, `Inventory.selected`/`getSelected()`/`items` → `getSelectedSlot()`/`setSelectedSlot()`/`getSelectedItem()`/`getNonEquipmentItems()`, `Screen.keyPressed(int,int,int)` → `keyPressed(KeyEvent)`, keybind String categories → `KeyMapping.Category`, `InteractionResult.shouldSwing()` → `Success.swingSource()`.
* Potions reworked for data components: `PotionUtils.getMobEffects` → `ItemStack.get(DataComponents.POTION_CONTENTS)`; `MobEffects.JUMP`→`JUMP_BOOST`, `MOVEMENT_SPEED`→`SPEED`.
* Removed the dead `SimpleChannel` networking (`PacketHandler`/`SimplePacket`/`PacketInspector`) — it only did a server-side teleport that never fired on real servers and registered no usable channels. As a side effect the mod now registers zero network channels, so NeoForge lets it connect to vanilla servers without a server-side counterpart.
* Dropped the `configured` dependency; configure via `config/winterbridge-client.toml`.
* Behavior fix surfaced by the stricter API: `autoE`'s knockback-stick search now scans only the hotbar (the new `setSelectedSlot` rejects non-hotbar slots, which the old direct field write silently allowed).
* Fix the four ninja bridges, which after the port just crouched in place and never placed a block or moved forward. 1.21.x resolves `getOnPos()`/`getBlockStateOn()` to the *main supporting block* (the solid block under your weight), so the edge check `getBlockStateOn().isAir()` never read air while sneaking on a ledge and the `adjust`→`sneak` gate could never fire. Reproduce the old "block under the player's position centre" semantics (`floor(x), floor(y-0.2), floor(z)`) with a local `onPos()`/`onAir()` helper, used across every bridge handler (adjust gate, sneak/place step, walk-up, and base-position setup). The bridge state machine now also ticks in `ClientTickEvent.Post` (matching the original `TickEvent.Phase.END`) so it sees post-movement position and an up-to-date look target; keybind/click detection stays in `ClientTickEvent.Pre`.

## v2.5

* Rework spam-clicking into a humanized, CPS-governed auto-clicker, ported from the standalone `double_click.py` engine: a rolling 1-second clicks-per-second cap, jittered inter-click delays (`base ± jitter`), and an occasional skipped click so the cadence is less mechanically flat. Applies to both left (attack) and right (block) clicking, driven by the same hold-to-activate triggers (`1` for sword, the `blocks` key for placing) as before, and keeping the existing game guards (entity gate, miss-click probability, sword-switch delay).
* Replace the old fixed-interval config `spam_left_min` / `spam_left_max` with the auto-clicker tunables `autoclick_cps_cap`, `autoclick_base_delay`, `autoclick_jitter`, and `autoclick_skip_prob` (under "PVP Settings").
* Wire up the previously-dormant straight-increase and diagonal ninja bridges to keybinds (`Ninja bridge (increase)` → F7, `Diagonal ninja bridge` → Y, `Diagonal ninja bridge (increase)` → U). Before this only flat straight ninja was reachable; the handlers existed but were unbound.

## v2.4

* Fix bug in v2.3 that double block still works when the player is on ground.
* Switching to ladder or hardest block now has a new feature: after some seconds of block-in (default 15s, configurable at `blockin_post_time`), the switch is directly to hardest block, without preferring ladder.

## v2.3

* Disable block clutch if has jump boost.
* Double block now only works when the player is not on the ground.

## v2.2

* Add ice bridge keybinding and treat it as non-block.
* Send different inc messages ("inc" then "incc" then "inc", ...) to avoid being intercepted by Jartex.
* Update terracotta texture pack to sanrio.
* Update logs and planks texture pack.
* Add "configured" as a dependency and tool to modify configuration.
* Remove all hotbar sorting related things.
* Misc changes on code, config, gradle version, etc.
