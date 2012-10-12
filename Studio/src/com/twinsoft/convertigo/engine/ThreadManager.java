/*
 * Copyright (c) 2001-2012 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.1.x/Studio/src/com/twinsoft/convertigo/engine/ContextManager.java $
 * $Author: fabienb $
 * $Revision: 31601 $
 * $Date: 2012-09-06 15:27:01 +0200 (jeu., 06 sept. 2012) $
 */

package com.twinsoft.convertigo.engine;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class ThreadManager extends AbstractRunnableManager {

	public void init() throws EngineException {
		Engine.logUsageMonitor.info("[ThreadManager] Initialization...");

		threadMXBean = ManagementFactory.getThreadMXBean();

		Engine.logUsageMonitor.debug("[ThreadManager] End of initialization");
	}

	@Override
	public void destroy() throws EngineException {
		Engine.logUsageMonitor.info("[ThreadManager] Destroying...");

		super.destroy();
	}

	@Override
	public void run() {
		Engine.logUsageMonitor.info("[ThreadManager] Starting the vulture thread for thread management");

		while (isRunning) {
			long sleepTime = System.currentTimeMillis() + 60000;
			try {
				// Active threads
				int threadCount = threadMXBean.getThreadCount();
				Engine.logUsageMonitor.info("[ThreadManager] Current JVM thread count: " + threadCount);
				Engine.logUsageMonitor.debug("[ThreadManager] Thread dump:\n" + threadDump());
				
				// Dead locks
				findDeadlock();
				Engine.logUsageMonitor.trace("[ThreadManager] Vulture task done");
			} catch (Exception e) {
				Engine.logUsageMonitor.error("An unexpected error has occured in the ThreadManager vulture.",
						e);
			} finally {
				if ((sleepTime -= System.currentTimeMillis()) > 0) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						// Ignore
						Engine.logUsageMonitor
								.debug("[ThreadManager] InterruptedException received: probably a request for stopping the vulture.");
					}
				}
			}
		}

		Engine.logUsageMonitor.info("[ThreadManager] The vulture thread has been stopped.");
	}

	private ThreadMXBean threadMXBean;

	/**
	 * Gets the thread dump information.
	 */
	public String threadDump() {
		String message = "";
		
		long[] tids = threadMXBean.getAllThreadIds();
		ThreadInfo[] tinfos = threadMXBean.getThreadInfo(tids, Integer.MAX_VALUE);
		for (ThreadInfo ti : tinfos) {
			message += getThreadInfoWithStacktrace(ti);
		}
		
		return message;
	}

	private static String INDENT = "    ";

	private String getThreadInfoWithStacktrace(ThreadInfo ti) {
		// print thread information
		String message = getThreadInfo(ti);

		// print stack trace with locks
		StackTraceElement[] stacktrace = ti.getStackTrace();
		MonitorInfo[] monitors = ti.getLockedMonitors();
		for (int i = 0; i < stacktrace.length; i++) {
			StackTraceElement ste = stacktrace[i];
			message += INDENT + "at " + ste.toString() + "\n";
			for (MonitorInfo mi : monitors) {
				if (mi.getLockedStackDepth() == i) {
					message += INDENT + "  - locked " + mi + "\n";
				}
			}
		}
		return message + "\n";
	}

	private String getThreadInfo(ThreadInfo ti) {
		String message = "\"" + ti.getThreadName() + "\" [ID=" + ti.getThreadId() + "]";
		message += " is " + ti.getThreadState();
		
		if (ti.isSuspended()) {
			message += " (suspended)";
		}
		
		if (ti.isInNative()) {
			message += " (running in native)";
		}

		message += ":\n";
		
		if (ti.getLockName() != null) {
			message += INDENT + "waiting to lock " + ti.getLockName() + "\n";
		}
		
		if (ti.getLockOwnerName() != null) {
			message += INDENT + "owned by \"" + ti.getLockOwnerName() + "\" [ID=" + ti.getLockOwnerId() + "]\n";
		}
		
		return message;
	}

	/**
	 * Checks if any threads are deadlocked. If any, print the thread dump
	 * information.
	 */
	public boolean findDeadlock() {
		long[] tids = threadMXBean.findMonitorDeadlockedThreads();
		if (tids == null) {
			return false;
		}

		String message = "Found one Java-level deadlock:\n\n";
		ThreadInfo[] infos = threadMXBean.getThreadInfo(tids, Integer.MAX_VALUE);
		for (ThreadInfo ti : infos) {
			message += getThreadInfoWithStacktrace(ti);
		}
		Engine.logUsageMonitor.error(message);

		return true;
	}

}