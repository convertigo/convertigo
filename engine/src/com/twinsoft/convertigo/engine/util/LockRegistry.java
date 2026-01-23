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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockRegistry {
	private static final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

	public static LockHandle lock(String key) {
		ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
		return new LockHandle(key, lock);
	}

	public static class LockHandle {
		private final String key;
		private final ReentrantLock lock;

		private LockHandle(String key, ReentrantLock lock) {
			this.key = key;
			this.lock = lock;
		}

		public void lock() {
			lock.lock();
		}

		public void release() {
			try {
				lock.unlock();
			} finally {
				if (!lock.isLocked() && !lock.hasQueuedThreads()) {
					locks.remove(key, lock);
				}
			}
		}
	}
}
