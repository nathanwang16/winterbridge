package net.lzdq.winterbridge.client.action;

import net.lzdq.winterbridge.ModConfig;

import java.util.ArrayDeque;
import java.util.Random;

/**
 * Humanized, CPS-governed auto-clicker for a single mouse button. Ported from
 * the standalone {@code double_click.py} engine: a rolling 1-second
 * clicks-per-second cap, jittered inter-click delays ({@code base ± jitter}),
 * and an occasional skipped click so the cadence is not mechanically flat.
 *
 * <p>Unlike the Python tool, this runs on Minecraft's single client thread
 * (driven from {@code ClientTickEvent} / {@code RenderTickEvent}), so there is
 * no locking, no worker threads, and no synthetic-event bookkeeping. The
 * Python "activation gate" that only injects while you are physically clicking
 * fast is replaced by the mod's own hold-to-activate triggers: the caller only
 * invokes {@link #tryClick} while the relevant key is held, so holding the
 * trigger <em>is</em> the gate.
 *
 * <p>Tunables ({@link ModConfig#autoclick_cps_cap}, {@code autoclick_base_delay},
 * {@code autoclick_jitter}, {@code autoclick_skip_prob}) are read live on every
 * call so they stay hot-reloadable via the in-game config screen.
 */
public class AutoClicker {
	private final ArrayDeque<Long> recent = new ArrayDeque<>();
	private final Random rand = new Random();
	private long nextAllowed = 0;

	/** Drop click timestamps older than the 1-second rolling window. */
	private void prune(long now) {
		long cutoff = now - 1000;
		while (!recent.isEmpty() && recent.peekFirst() < cutoff)
			recent.removeFirst();
	}

	/**
	 * Decide whether a click should be emitted on this invocation. The caller is
	 * responsible for performing the actual click (attack / place) when this
	 * returns {@code true}.
	 *
	 * @param now current time in milliseconds ({@code System.currentTimeMillis()})
	 * @return {@code true} if a click should fire now
	 */
	public boolean tryClick(long now) {
		if (now < nextAllowed)
			return false;
		prune(now);
		if (recent.size() >= ModConfig.autoclick_cps_cap.get())
			return false; // at the rolling cap; recheck once a timestamp ages out

		// Schedule the next opportunity regardless of whether we click this round,
		// so a humanizing skip still leaves a real gap.
		double jitter = ModConfig.autoclick_jitter.get();
		double delay = ModConfig.autoclick_base_delay.get() + (rand.nextDouble() * 2 - 1) * jitter;
		nextAllowed = now + (long) Math.max(0, delay);

		if (rand.nextDouble() < ModConfig.autoclick_skip_prob.get())
			return false; // occasional skipped beat

		recent.addLast(now);
		return true;
	}

	/** Forget rolling state (e.g. on reconnect) so a new session starts clean. */
	public void reset() {
		recent.clear();
		nextAllowed = 0;
	}
}
