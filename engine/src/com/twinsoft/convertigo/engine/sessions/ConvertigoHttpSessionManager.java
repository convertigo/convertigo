/*
 * Copyright (c) 2001-2025 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of  the  GNU  Affero General Public
 * License  as published by  the  Free Software Foundation;  either
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

package com.twinsoft.convertigo.engine.sessions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;

public final class ConvertigoHttpSessionManager implements PropertyChangeEventListener {
	private static volatile ConvertigoHttpSessionManager instance;

	private final Object mutex = new Object();
	private volatile SessionProvider provider;
	private volatile SessionStoreMode storeMode;

	private ConvertigoHttpSessionManager() {
		reload(computeStoreMode());
		try {
			Engine.theApp.eventManager.addListener(this, PropertyChangeEventListener.class);
		} catch (Exception ignored) {
			// Engine not fully started yet
		}
	}

	public static ConvertigoHttpSessionManager getInstance() {
		if (instance == null) {
			synchronized (ConvertigoHttpSessionManager.class) {
				if (instance == null) {
					instance = new ConvertigoHttpSessionManager();
				}
			}
		}
		return instance;
	}

	public HttpSession getSession(HttpServletRequest request, boolean create) {
		return provider.getSession(request, create);
	}

	public SessionStoreMode getStoreMode() {
		return storeMode;
	}

	private SessionStoreMode computeStoreMode() {
		var raw = EnginePropertiesManager.getProperty(PropertyName.SESSION_STORE_MODE);
		var result = SessionStoreMode.fromProperty(raw);
		debug("Resolved SESSION_STORE_MODE='" + raw + "' => store=" + result.name());
		return result;
	}

	private SessionProvider buildProvider(SessionStoreMode storeMode) {
		try {
			debug("Building session provider for store=" + storeMode.name());
			return switch (storeMode) {
				case tomcat -> new LegacySessionProvider();
				case redis -> new RedisSessionProvider();
				default -> new LegacySessionProvider();
			};
		} catch (Exception e) {
			logStartupFailure(storeMode.name(), e);
			return new LegacySessionProvider();
		}
	}

	private void reload(SessionStoreMode storeMode) {
		synchronized (mutex) {
			debug("Reload requested with store=" + storeMode.name());
			this.storeMode = storeMode;
			provider = buildProvider(storeMode);
			logSelectedMode();
		}
	}

	private void logSelectedMode() {
		try {
			if (Engine.logEngine != null && Engine.logEngine.isInfoEnabled()) {
				Engine.logEngine.info("(ConvertigoSessionManager) store=" + storeMode.name() + ", provider="
						+ provider.getClass().getSimpleName());
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}

	private void logStartupFailure(String mode, Exception e) {
		try {
			if (Engine.logEngine != null) {
				Engine.logEngine.error("(ConvertigoSessionManager) Failed to initialize session store '" + mode
						+ "'. Falling back to legacy mode.", e);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}

	@Override
	public void onEvent(PropertyChangeEvent event) {
		if (event == null || event.getKey() == null) {
			return;
		}
		switch (event.getKey()) {
		case SESSION_STORE_MODE:
			debug("Detected property change for " + event.getKey().name() + ", recomputing session mode");
			reload(computeStoreMode());
			break;
		default:
			break;
		}
	}

	private void debug(String message) {
		try {
			if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
				Engine.logEngine.debug("(ConvertigoSessionManager) " + message);
			}
		} catch (Exception ignore) {
			// ignore logging failures
		}
	}
}
