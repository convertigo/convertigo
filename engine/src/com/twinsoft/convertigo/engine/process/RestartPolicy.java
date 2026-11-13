/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.process;

import java.time.Duration;

/**
 * Bounded restarts with a growing delay, for a supervisor that restarts a managed process. Consecutive
 * failures are counted; a process that ran longer than stableAfter resets the count.
 */
public final class RestartPolicy {
	private final int maxAttempts;
	private final Duration initialDelay;
	private final Duration maxDelay;
	private final Duration stableAfter;
	private int failures = 0;

	public RestartPolicy(int maxAttempts, Duration initialDelay, Duration maxDelay, Duration stableAfter) {
		this.maxAttempts = Math.max(1, maxAttempts);
		this.initialDelay = initialDelay;
		this.maxDelay = maxDelay;
		this.stableAfter = stableAfter;
	}

	/**
	 * @return 3 attempts, 2 s then 4 s between them, at most 30 s, the count reset after 60 s of run
	 */
	public static RestartPolicy bounded() {
		return new RestartPolicy(3, Duration.ofSeconds(2), Duration.ofSeconds(30), Duration.ofSeconds(60));
	}

	/**
	 * A start attempt failed.
	 *
	 * @return the delay before the next attempt, or null when no attempt is left
	 */
	public synchronized Duration onFailure() {
		failures++;
		if (failures >= maxAttempts) {
			return null;
		}
		long delay = initialDelay.toMillis() << Math.min(20, failures - 1);
		return Duration.ofMillis(Math.min(delay, maxDelay.toMillis()));
	}

	/**
	 * A started process exited on its own after running for uptime.
	 *
	 * @return the delay before the next attempt, or null when no attempt is left
	 */
	public synchronized Duration onExit(Duration uptime) {
		if (uptime.compareTo(stableAfter) >= 0) {
			failures = 0;
		}
		return onFailure();
	}

	public synchronized int getFailures() {
		return failures;
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}
}
