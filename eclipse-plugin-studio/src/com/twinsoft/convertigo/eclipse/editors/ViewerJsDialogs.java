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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.AlertCallback;
import com.teamdev.jxbrowser.browser.callback.BeforeUnloadCallback;
import com.teamdev.jxbrowser.browser.callback.ConfirmCallback;
import com.teamdev.jxbrowser.browser.callback.PromptCallback;
import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived;
import com.teamdev.jxbrowser.frame.Frame;
import com.teamdev.jxbrowser.navigation.event.NavigationStarted;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.Engine;

/**
 * JavaScript dialogs of the application viewers (NGX and mobile application editors).
 * <p>
 * The JxBrowser defaults open native boxes modal for the whole Studio: on Windows they can stay behind the Studio
 * window while every Studio window is disabled, and an agent driving the page waits until someone answers. No window
 * is opened here:
 * <ul>
 * <li>alert() only informs: the page shows its message in a small overlay (viewer-alert.js, installed when each
 * frame is created) and the Studio logs it.</li>
 * <li>confirm() and prompt() must return the answer to the page, whose script waits for it: a page cannot ask
 * itself, its script is paused. The viewer asks in a bar above the browser, like an information bar. Only the page
 * waits; the answer is Cancel when the page changes, the editor closes or the bar cannot be shown.</li>
 * <li>beforeunload: the viewer leaves the page, it reloads the application on each build.</li>
 * </ul>
 * Only the viewers use it: the deployed application keeps the dialogs of its browser.
 */
public final class ViewerJsDialogs {
	static final String ALERT_LOG_PREFIX = "[Convertigo viewer] alert: ";
	private static final String ALERT_SCRIPT = loadAlertScript();

	private final String viewer;
	private final Display display;
	private final Composite bar;
	/** The question shown in the bar, read and written on the UI thread only. */
	private Question pending;

	/** The answer of one confirm() or prompt(), given once: null means Cancel. */
	static final class Question {
		final String message;
		final String defaultValue;
		private final Consumer<String> reply;
		private final AtomicBoolean answered = new AtomicBoolean();

		Question(String message, String defaultValue, Consumer<String> reply) {
			this.message = message == null ? "" : message;
			this.defaultValue = defaultValue;
			this.reply = reply;
		}

		boolean isPrompt() {
			return defaultValue != null;
		}

		boolean answer(String value) {
			if (!answered.compareAndSet(false, true)) {
				return false;
			}
			try {
				reply.accept(value);
			} catch (Exception e) {
				// the page went away meanwhile
				Engine.logStudio.debug("(ViewerJsDialogs) Unable to answer the page", e);
			}
			return true;
		}

		boolean isAnswered() {
			return answered.get();
		}
	}

	ViewerJsDialogs(Control below, String viewer) {
		this.viewer = viewer;
		this.display = below.getDisplay();
		bar = new Composite(below.getParent(), SWT.NONE);
		bar.moveAbove(below);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.exclude = true;
		bar.setLayoutData(gd);
		bar.setVisible(false);
		if (SwtUtils.isDark()) {
			bar.setData("style", "background-color: #4c525d");
		}
		bar.addDisposeListener(e -> {
			Question question = pending;
			pending = null;
			if (question != null) {
				cancel(question, "the viewer is closed");
			}
		});
	}

	/** The script replacing window.alert, to run in each frame of a viewer when its script context is created. */
	public static String alertScript() {
		return ALERT_SCRIPT;
	}

	/**
	 * Installs the dialog handlers on the browser of a viewer, again after each recreation of the browser. The
	 * question bar is placed above the control below, in their parent with a GridLayout.
	 */
	public static ViewerJsDialogs install(C8oBrowser c8oBrowser, Control below, String viewer) {
		ViewerJsDialogs dialogs = new ViewerJsDialogs(below, viewer);
		c8oBrowser.onBrowserReady(() -> dialogs.install(c8oBrowser.getBrowser()));
		return dialogs;
	}

	void install(Browser browser) {
		// a recreated browser no longer waits for the previous question
		display.asyncExec(() -> {
			if (pending != null) {
				Question question = pending;
				hide(question);
				cancel(question, "the browser was recreated");
			}
		});

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
			Engine.logStudio.info("(" + viewer + ") JavaScript confirm from " + params.url() + ", answer it in the viewer: " + params.message());
			ask(new Question(params.message(), null, value -> {
				if (value != null) {
					tell.ok();
				} else {
					tell.cancel();
				}
			}));
		});

		browser.set(PromptCallback.class, (params, tell) -> {
			Engine.logStudio.info("(" + viewer + ") JavaScript prompt from " + params.url() + ", answer it in the viewer: " + params.message());
			String text = params.text();
			ask(new Question(params.message(), text == null ? "" : text, value -> {
				if (value != null) {
					tell.ok(value);
				} else {
					tell.cancel();
				}
			}));
		});

		browser.set(BeforeUnloadCallback.class, (params, tell) -> tell.leave());

		// a new page does not wait for the question of the previous one
		browser.navigation().on(NavigationStarted.class, event -> {
			if (event.isInMainFrame() && !event.isSameDocument()) {
				display.asyncExec(() -> {
					Question question = pending;
					if (question != null) {
						hide(question);
						cancel(question, "the page changes");
					}
				});
			}
		});
	}

	/** Called by the browser: shows the question in the bar, or answers Cancel at once if it cannot. */
	void ask(Question question) {
		try {
			display.asyncExec(() -> show(question));
		} catch (Exception | LinkageError e) {
			cancel(question, "the viewer cannot show the question: " + e);
		}
	}

	private void show(Question question) {
		if (question.isAnswered()) {
			return;
		}
		try {
			if (bar.isDisposed()) {
				cancel(question, "the viewer is closed");
				return;
			}
			Question previous = pending;
			if (previous != null) {
				cancel(previous, "a new question replaces it");
			}
			pending = question;
			for (Control child : bar.getChildren()) {
				child.dispose();
			}
			GridLayout layout = new GridLayout(question.isPrompt() ? 5 : 4, false);
			layout.marginWidth = 8;
			layout.marginHeight = 4;
			bar.setLayout(layout);

			Label icon = new Label(bar, SWT.NONE);
			icon.setImage(display.getSystemImage(SWT.ICON_QUESTION));
			icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

			Label message = new Label(bar, SWT.WRAP);
			message.setText(question.message.replace("&", "&&"));
			GridData messageData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			messageData.widthHint = 200;
			message.setLayoutData(messageData);

			Text input = null;
			if (question.isPrompt()) {
				Text field = new Text(bar, SWT.BORDER | SWT.SINGLE);
				field.setText(question.defaultValue);
				GridData fieldData = new GridData(SWT.FILL, SWT.CENTER, false, false);
				fieldData.widthHint = 200;
				field.setLayoutData(fieldData);
				field.addListener(SWT.DefaultSelection, e -> reply(question, field.getText()));
				field.addListener(SWT.Traverse, e -> {
					if (e.detail == SWT.TRAVERSE_ESCAPE) {
						e.doit = false;
						reply(question, null);
					}
				});
				input = field;
			}
			Text field = input;

			Button ok = new Button(bar, SWT.PUSH);
			ok.setText("OK");
			ok.addListener(SWT.Selection, e -> reply(question, field != null ? field.getText() : ""));
			Button cancel = new Button(bar, SWT.PUSH);
			cancel.setText("Cancel");
			cancel.addListener(SWT.Selection, e -> reply(question, null));

			// the keyboard goes to the bar only if it was in the viewer: an agent driving the page must not take it
			// from another editor
			boolean focusInViewer = isInViewer(display.getFocusControl());
			((GridData) bar.getLayoutData()).exclude = false;
			bar.setVisible(true);
			bar.getParent().layout(true, true);
			if (focusInViewer) {
				if (field != null) {
					field.selectAll();
					field.setFocus();
				} else {
					ok.setFocus();
				}
			}
		} catch (Exception e) {
			hide(question);
			cancel(question, "the viewer cannot show the question: " + e);
		}
	}

	private boolean isInViewer(Control control) {
		for (Control c = control; c != null; c = c.getParent()) {
			if (c == bar.getParent()) {
				return true;
			}
		}
		return false;
	}

	private void reply(Question question, String value) {
		hide(question);
		if (question.answer(value)) {
			Engine.logStudio.info("(" + viewer + ") JavaScript " + (question.isPrompt() ? "prompt" : "confirm")
					+ " answered " + (value == null ? "Cancel" : "OK"));
		}
	}

	/** Hides the bar if it still shows this question. */
	private void hide(Question question) {
		if (pending != question) {
			return;
		}
		pending = null;
		if (!bar.isDisposed()) {
			for (Control child : bar.getChildren()) {
				child.dispose();
			}
			((GridData) bar.getLayoutData()).exclude = true;
			bar.setVisible(false);
			bar.getParent().layout(true, true);
		}
	}

	private void cancel(Question question, String reason) {
		if (question.answer(null)) {
			Engine.logStudio.info("(" + viewer + ") JavaScript " + (question.isPrompt() ? "prompt" : "confirm")
					+ " answered Cancel: " + reason);
		}
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
