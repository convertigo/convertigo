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
 * MERCHANTABILITY  or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.views.admin;

import java.util.Locale;
import java.util.function.Supplier;

import com.teamdev.jxbrowser.navigation.event.NavigationFinished;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.engine.Engine;

public class AuthenticatedBrowser {
	private static final long RECONNECT_POLL_DELAY = 5000;
	private static final long RECONNECT_THROTTLE = 1500;
	private static final String WAITING_HTML = "<!doctype html><html><head><meta charset=\"utf-8\">"
			+ "<style>"
			+ "html,body{height:100%;margin:0;background:$background$;color:$foreground$;font-family:Arial,sans-serif;}"
			+ "body{display:grid;place-items:center;text-align:center;}"
			+ ".logo{color:#00bfff;font-size:42px;font-weight:700;margin-bottom:16px;}"
			+ ".message{font-size:14px;opacity:.8;}"
			+ "</style></head><body>"
			+ "<div><div class=\"logo\">convertigo</div>"
			+ "<div class=\"message\">Waiting for the Convertigo engine...</div></div>"
			+ "</body></html>";

	private final C8oBrowser browser;
	private final Supplier<String> urlSupplier;
	private boolean disposed = false;
	private boolean waiting = false;
	private boolean startupLoadQueued = false;
	private long lastReconnect = 0;

	public AuthenticatedBrowser(C8oBrowser browser, Supplier<String> urlSupplier) {
		this.browser = browser;
		this.urlSupplier = urlSupplier;
		browser.setRestoreHandler(this::load);
		browser.onBrowserReady(() -> browser.getBrowser().navigation().on(NavigationFinished.class,
				event -> ConvertigoPlugin.asyncExec(() -> reconnectIfNeeded(event))));
		scheduleReconnectCheck();
	}

	public void load() {
		if (!canUseBrowser()) {
			return;
		}
		if (Engine.isStarted) {
			loadNow();
		} else {
			showWaitingPage();
			if (!startupLoadQueued) {
				startupLoadQueued = true;
				ConvertigoPlugin.runAtStartup(() -> ConvertigoPlugin.asyncExec(() -> {
					startupLoadQueued = false;
					loadNow();
				}));
			}
		}
	}

	public String getUrl() {
		return urlSupplier.get();
	}

	public void dispose() {
		disposed = true;
	}

	private void scheduleReconnectCheck() {
		browser.getDisplay().timerExec((int) RECONNECT_POLL_DELAY, () -> {
			if (!canUseBrowser()) {
				return;
			}
			reconnectIfNeeded();
			scheduleReconnectCheck();
		});
	}

	private void reconnectIfNeeded() {
		if (!canUseBrowser()) {
			return;
		}
		if (!Engine.isStarted) {
			showWaitingPage();
			return;
		}
		if (waiting || isDisconnectedUrl(browser.getURL())) {
			reconnect();
		}
	}

	private void reconnectIfNeeded(NavigationFinished event) {
		if (!canUseBrowser()) {
			return;
		}
		if (event.isInMainFrame() && event.isErrorPage()) {
			reconnect();
			return;
		}
		reconnectIfNeeded();
	}

	private void reconnect() {
		long now = System.currentTimeMillis();
		if (now - lastReconnect < RECONNECT_THROTTLE) {
			return;
		}
		lastReconnect = now;
		loadNow();
	}

	private void loadNow() {
		if (!canUseBrowser()) {
			return;
		}
		lastReconnect = System.currentTimeMillis();
		waiting = false;
		browser.setUrl(getUrl());
	}

	private void showWaitingPage() {
		if (!canUseBrowser() || waiting) {
			return;
		}
		waiting = true;
		browser.setText(WAITING_HTML);
	}

	private boolean canUseBrowser() {
		return !disposed && browser != null && !browser.isDisposed();
	}

	private boolean isDisconnectedUrl(String url) {
		if (url == null) {
			return false;
		}
		url = url.toLowerCase(Locale.ROOT);
		return url.startsWith("chrome-error://")
				|| "about:blank".equals(url)
				|| url.contains("/login/")
				|| url.endsWith("/login")
				|| url.contains("/admin/login.html");
	}
}
