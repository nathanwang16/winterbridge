# CHANGELOG

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
