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


package com.twinsoft.convertigo.engine.util;

/**
 * The thread runs a native requestable (a native sequence, or a requestable it calls natively): the optimizations
 * whose property is "native" apply to what this thread does (engine.copy_on_demand, classloader.lookup_cache).
 */
public final class NativeRunning {
	private static final ThreadLocal<Boolean> running = new ThreadLocal<Boolean>();

	private NativeRunning() {
	}

	public static boolean isRunning() {
		return Boolean.TRUE.equals(running.get());
	}

	/** marks the thread for a requestable, returns the previous mark to give back to {@link #restore} */
	public static Boolean enter(boolean nativeRequestable) {
		Boolean previous = running.get();
		running.set(nativeRequestable);
		return previous;
	}

	public static void restore(Boolean previous) {
		if (previous == null) {
			running.remove();
		} else {
			running.set(previous);
		}
	}
}
