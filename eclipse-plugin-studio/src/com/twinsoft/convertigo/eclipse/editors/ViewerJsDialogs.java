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

package com.twinsoft.convertigo.eclipse.editors;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.AlertCallback;
import com.teamdev.jxbrowser.browser.callback.BeforeUnloadCallback;
import com.teamdev.jxbrowser.browser.callback.ConfirmCallback;
import com.teamdev.jxbrowser.browser.callback.PromptCallback;
import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived;
import com.teamdev.jxbrowser.frame.Frame;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.engine.Engine;

/**
 * JavaScript dialogs of the application viewers (NGX and mobile application editors).
 * <p>
 * The JxBrowser defaults open a native box modal for the whole Studio: on Windows it can stay behind the Studio
 * window while every Studio window is disabled, and an agent driving the page waits until someone answers it.
 * <ul>
 * <li>alert() only informs: the page shows its message in a small overlay (viewer-alert.js, installed when each
 * frame is created) and the Studio logs it. Nothing is modal.</li>
 * <li>confirm() and prompt() return the answer to the page, which waits for it: the Studio asks with a dialog of
 * its own window, kept above every window so it cannot hide behind the Studio.</li>
 * <li>beforeunload: the viewer leaves the page, it reloads the application on each build.</li>
 * </ul>
 * Only the viewers use it: the deployed application keeps the dialogs of its browser.
 */
public final class ViewerJsDialogs {
	static final String ALERT_LOG_PREFIX = "[Convertigo viewer] alert: ";
	private static final String ALERT_SCRIPT = loadAlertScript();

	private ViewerJsDialogs() {
	}

	/** The script replacing window.alert, to run in each frame of a viewer when its script context is created. */
	public static String alertScript() {
		return ALERT_SCRIPT;
	}

	/** Installs the dialog handlers on the browser of a viewer, again after each recreation of the browser. */
	public static void install(C8oBrowser c8oBrowser, String viewer) {
		c8oBrowser.onBrowserReady(() -> install(c8oBrowser.getBrowser(), viewer));
	}

	private static void install(Browser browser, String viewer) {
		browser.on(ConsoleMessageReceived.class, event -> {
			String message = event.consoleMessage().message();
			if (message != null && message.startsWith(ALERT_LOG_PREFIX)) {
				Engine.logStudio.warn("(" + viewer + ") JavaScript alert: " + message.substring(ALERT_LOG_PREFIX.length()));
			}
		});

		// An alert that viewer-alert.js did not replace (a page created before it, a failed injection): answered at
		// once, then shown in the page.
		browser.set(AlertCallback.class, (params, tell) -> {
			tell.ok();
			String message = params.message();
			Engine.logStudio.warn("(" + viewer + ") JavaScript alert from " + params.url() + ": " + message);
			Engine.execute(() -> {
				try {
					Frame frame = browser.mainFrame().orElse(null);
					if (frame != null) {
						frame.executeJavaScript(ALERT_SCRIPT + "\nwindow.__c8oViewerAlert.show(" + JSONObject.quote(message) + ");");
					}
				} catch (Exception e) {
					Engine.logStudio.debug("(" + viewer + ") Unable to show the JavaScript alert in the page", e);
				}
			});
		});

		browser.set(ConfirmCallback.class, (params, tell) -> {
			Engine.logStudio.info("(" + viewer + ") JavaScript confirm from " + params.url() + ": " + params.message());
			ask(() -> {
				boolean ok = false;
				try {
					// SYSTEM_MODAL: on Windows, SWT forces a system modal box to the front (MB_TOPMOST).
					MessageBox box = new MessageBox(mainShell(), SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION | SWT.SYSTEM_MODAL);
					box.setText(dialogTitle(params.title()));
					box.setMessage(params.message());
					ok = box.open() == SWT.OK;
				} finally {
					if (ok) {
						tell.ok();
					} else {
						tell.cancel();
					}
				}
			}, tell::cancel);
		});

		browser.set(PromptCallback.class, (params, tell) -> {
			Engine.logStudio.info("(" + viewer + ") JavaScript prompt from " + params.url() + ": " + params.message());
			ask(() -> {
				String value = null;
				try {
					InputDialog dialog = new InputDialog(mainShell(), dialogTitle(params.title()), params.message(), params.text(), null) {
						{
							// above every window, as the confirm box
							setShellStyle(getShellStyle() | SWT.ON_TOP);
						}
					};
					if (dialog.open() == Window.OK) {
						value = dialog.getValue();
					}
				} finally {
					if (value != null) {
						tell.ok(value);
					} else {
						tell.cancel();
					}
				}
			}, tell::cancel);
		});

		browser.set(BeforeUnloadCallback.class, (params, tell) -> tell.leave());
	}

	/** Asks on the UI thread, without holding the browser thread; the page gets a cancel if the Studio cannot ask. */
	private static void ask(Runnable question, Runnable cancel) {
		try {
			ConvertigoPlugin.asyncExec(question);
		} catch (Exception | LinkageError e) {
			// a page waiting for an answer must always get one
			cancel.run();
		}
	}

	private static Shell mainShell() {
		Shell shell = ConvertigoPlugin.getMainShell();
		if (shell.getMinimized()) {
			shell.setMinimized(false);
		}
		shell.forceActive();
		return shell;
	}

	private static String dialogTitle(String title) {
		return title == null || title.isBlank() ? "Convertigo application viewer" : title;
	}

	private static String loadAlertScript() {
		try (InputStream is = ViewerJsDialogs.class.getResourceAsStream("viewer-alert.js")) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			// without the overlay, the alert callback still answers at once and logs the message
			return "window.__c8oViewerAlert = window.__c8oViewerAlert || { show: function () {} };";
		}
	}
}
