package net.lzdq.winterbridge;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;


public class ModConfig {
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec CONFIG;
	public static final ModConfigSpec.IntValue cheat_mode;
	public static final ModConfigSpec.DoubleValue ninja_side_dist, ninja_pitch, ninja_walk_dist;
	public static final ModConfigSpec.DoubleValue ninja_diag_pitch, ninja_diag_walk_dist;
	public static final ModConfigSpec.DoubleValue god_pitch, god_walk_dist;
	public static final ModConfigSpec.ConfigValue<List<Integer>> ninja_wait_tick;
	public static final ModConfigSpec.ConfigValue<List<Double>> yaw_var, pitch_var;
	public static final ModConfigSpec.ConfigValue<String> auto_login_command;
	public static final ModConfigSpec.IntValue timeout_doubleclick;
	public static final ModConfigSpec.IntValue autoclick_cps_cap, autoclick_base_delay, autoclick_jitter;
	public static final ModConfigSpec.IntValue blockin_rotate_tick, blockin_post_time;
	public static final ModConfigSpec.DoubleValue blockin_offset;
	public static final ModConfigSpec.IntValue delay_sword, delay_double_attack;
	public static final ModConfigSpec.DoubleValue spam_miss_click_prob, autoclick_skip_prob;
	public static final ModConfigSpec.IntValue ladder_rotate_tick;

	static {
		BUILDER.push("Bridge Settings");

		ninja_side_dist = BUILDER
				.comment("Ninja and god bridge's side distance from block center")
				.defineInRange("ninja_side_dist", 0.28, 0.0, 1.0);

		ninja_pitch = BUILDER
				.comment("Pitch of ninja bridge")
				.defineInRange("ninja_pitch", 77.5, -90.0, 90.0);

		ninja_walk_dist = BUILDER
				.comment("Ninja bridge's walk distance from base block's center. Higher value is faster but increases the chance of falling.")
				.defineInRange("ninja_walk_dist", 0.55, 0.5, 0.7);

		ninja_diag_pitch = BUILDER
				.comment("Pitch of ninja diagonal bridge")
				.defineInRange("ninja_diag_pitch", 76.0, -90.0, 90.0);

		ninja_diag_walk_dist = BUILDER
				.comment("Manhattan walk dist of ninja diagonal bridge. Higher value is faster but increases the chance of falling.")
				.defineInRange("ninja_diag_walk_dist", 1.1, 1.0, 1.5);

		ninja_wait_tick = BUILDER
				.comment("Ninja bridge ticks to wait after each step for 3 different cheat modes. Higher value is slower but decreases the chance of detected by the server.")
				.define("ninja_wait_tick", Arrays.asList(0, 1, 2));

		yaw_var = BUILDER
				.comment("Maximum variance of yaw when adjusting for 3 different cheat modes. Used for anti-anti-cheat.")
				.define("yaw_var", Arrays.asList(0.0, 0.1, 0.1));

		pitch_var = BUILDER
				.comment("Maximum variance of pitch when adjusting for 3 different cheat modes. Used for anti-anti-cheat. Higher values make you deviate from the lane.")
				.define("pitch_var", Arrays.asList(0.0, 0.1, 0.1));

		god_pitch = BUILDER
				.comment("Pitch of god bridge")
				.defineInRange("god_pitch", 76.0, -90.0, 90.0);

		god_walk_dist = BUILDER
				.comment("God bridge's walk distance from base block's center")
				.defineInRange("god_walk_dist", 0.5, -0.5, 1.0);

		BUILDER.pop();

		BUILDER.push("PVP Settings");

		autoclick_cps_cap = BUILDER
				.comment("Auto-clicker rolling clicks-per-second cap (clicks in a 1s window). Shared shape for both left attack and right block spam.")
				.defineInRange("autoclick_cps_cap", 24, 1, 100);

		autoclick_base_delay = BUILDER
				.comment("Auto-clicker base delay between clicks, ms")
				.defineInRange("autoclick_base_delay", 25, 1, 1000);

		autoclick_jitter = BUILDER
				.comment("Auto-clicker random +/- jitter added to each click delay, ms (human-like variance)")
				.defineInRange("autoclick_jitter", 12, 0, 1000);

		autoclick_skip_prob = BUILDER
				.comment("Auto-clicker probability of skipping a scheduled click (humanizing), 0..1")
				.defineInRange("autoclick_skip_prob", 0.10, 0.0, 1.0);

		delay_sword = BUILDER
				.comment("Delay after switching to sword, before spam-clicking, in ms")
				.defineInRange("delay_sword", 100, 0, 200);

		timeout_doubleclick = BUILDER
				.comment("Timeout for a double click")
				.defineInRange("timeout_doubleclick", 500, 0, 1000);

		delay_double_attack = BUILDER
				.comment("Delay between the two attacks, in ms")
				.defineInRange("delay_double_attack", 30, 0, 200);

		spam_miss_click_prob = BUILDER
				.comment("Probability of still left click if hitResult is not entity")
				.defineInRange("spam_miss_click_prob", 0.9, 0.0, 1.0);

		BUILDER.pop();

		BUILDER.push("Other Settings");

		blockin_offset = BUILDER
				.comment("Block-in block placement offset from center (doubled). 1.0 means random on whole face, 0.0 means only center of face")
				.defineInRange("blockin_offset", 0.99, 0.0, 1.0);

		blockin_rotate_tick = BUILDER
				.comment("Block-in ticks for rotating")
				.defineInRange("blockin_rotate_tick", 2, 1, 20);

		blockin_post_time = BUILDER
				.comment("How many seconds after block-in that q directly switches to hardest block")
				.defineInRange("blockin_post_time", 15, 0, 100);

		ladder_rotate_tick = BUILDER
				.comment("Block + ladder clutch rotate ticks (after block, before ladder). 0 means disabling rotate")
				.defineInRange("ladder_rotate_tick", 0, 0, 3);

		auto_login_command = BUILDER
				.comment("Auto login command, usually /login password (DO NOT include the slash)")
				.define("auto_login_command", "login password");

		cheat_mode = BUILDER
				.comment("Default cheat mode. 0: absolute  1: relative	2: slightly")
				.defineInRange("cheat_mode", 0, 0, 2);

		BUILDER.pop();

		CONFIG = BUILDER.build();
	}
}
