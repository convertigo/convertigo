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

package com.twinsoft.convertigo.engine;

import java.util.ArrayList;
import java.util.List;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;
import com.twinsoft.tas.KeyManager;

public class MonitorMetrics {
	private static final long MB = 1024 * 1024;
	private static final long HISTORY_DURATION = 15 * 60 * 1000L;
	private static final int HISTORY_MAX_SAMPLES = 60;
	private static final List<Sample> history = new ArrayList<Sample>();

	public static class Sample {
		public final long memoryMaximal;
		public final long memoryTotal;
		public final long memoryUsed;
		public final int threads;
		public final int contexts;
		public final int sessions;
		public final int sessionMaxCV;
		public final int availableSessions;
		public final long requests;
		public final boolean engineState;
		public final long startTime;
		public final long time;

		private Sample(
				long memoryMaximal,
				long memoryTotal,
				long memoryUsed,
				int threads,
				int contexts,
				int sessions,
				int sessionMaxCV,
				int availableSessions,
				long requests,
				boolean engineState,
				long startTime,
				long time) {
			this.memoryMaximal = memoryMaximal;
			this.memoryTotal = memoryTotal;
			this.memoryUsed = memoryUsed;
			this.threads = threads;
			this.contexts = contexts;
			this.sessions = sessions;
			this.sessionMaxCV = sessionMaxCV;
			this.availableSessions = availableSessions;
			this.requests = requests;
			this.engineState = engineState;
			this.startTime = startTime;
			this.time = time;
		}
	}

	public static Sample current() {
		Runtime runtime = Runtime.getRuntime();
		long memoryTotal = runtime.totalMemory();
		int sessionCount = getSessionCount();
		int sessionMaxCV = getSessionMaxCV();
		long requests = Math.max(EngineStatistics.getAverage(EngineStatistics.REQUEST), 0);

		return new Sample(
			runtime.maxMemory() / MB,
			memoryTotal / MB,
			(memoryTotal - runtime.freeMemory()) / MB,
			RequestableObject.nbCurrentWorkerThreads,
			getContextCount(),
			sessionCount,
			sessionMaxCV,
			Math.max(0, sessionMaxCV - sessionCount),
			requests,
			Engine.isStarted,
			Engine.startStopDate,
			System.currentTimeMillis()
		);
	}

	public static synchronized void captureCurrent() {
		Sample sample = current();
		history.add(sample);
		prune(sample.time);
	}

	public static synchronized List<Sample> getHistory() {
		prune(System.currentTimeMillis());
		return new ArrayList<Sample>(history);
	}

	public static synchronized void clearHistory() {
		history.clear();
	}

	private static int getContextCount() {
		try {
			return Engine.isStarted && Engine.theApp != null && Engine.theApp.contextManager != null
					? Engine.theApp.contextManager.getNumberOfContexts()
					: 0;
		} catch (Exception e) {
			return 0;
		}
	}

	private static int getSessionCount() {
		try {
			return ConvertigoHttpSessionManager.getInstance().countCountedSessions();
		} catch (Exception e) {
			return 0;
		}
	}

	private static int getSessionMaxCV() {
		try {
			return KeyManager.getMaxCV(Session.EmulIDSE);
		} catch (Exception e) {
			return 0;
		}
	}

	private static void prune(long now) {
		long minimumTime = now - HISTORY_DURATION;
		while (!history.isEmpty() && (history.get(0).time < minimumTime || history.size() > HISTORY_MAX_SAMPLES)) {
			history.remove(0);
		}
	}
}
