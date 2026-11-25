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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
import com.teamdev.jxbrowser.navigation.event.NavigationStarted;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.engine.Engine;

/**
 * JavaScript dialogs of the application viewers (NGX and mobile application editors).
 * <p>
 * The JxBrowser defaults open native boxes modal for the whole Studio: on Windows they can stay behind the Studio
 * window while every Studio window is disabled, and an agent driving the page waits until someone answers. No window
 * is opened and the page is not modified: the viewer shows its dialogs in a bar above the browser, like an
 * information bar.
 * <ul>
 * <li>alert() only informs: the page goes on at once, the message stays in the bar until OK or a new page, and the
 * Studio logs it. Several alerts show the last one with their count.</li>
 * <li>confirm() and prompt() must return the answer to the page, whose script waits for it: a page cannot ask
 * itself, its script is paused. Only the page waits; the answer is Cancel when the page changes, the editor closes
 * or the bar cannot be shown. A question goes before the alerts.</li>
 * <li>beforeunload: the viewer leaves the page, it reloads the application on each build.</li>
 * </ul>
 * Only the viewers use it: the deployed application keeps the dialogs of its browser.
 */
public final class ViewerJsDialogs {
	private final String viewer;
	private final Display display;
	private final Composite bar;
	/** The question waiting for its answer, read and written on the UI thread only. */
	private Question pending;
	/** The alerts not acknowledged yet, oldest first, read and written on the UI thread only. */
	private final List<String> alerts = new ArrayList<>();

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
			alerts.clear();
			Question question = pending;
			pending = null;
			if (question != null) {
				cancel(question, "the viewer is closed");
			}
		});
	}

	/**
	 * Installs the dialog handlers on the browser of a viewer, again after each recreation of the browser. The bar is
	 * placed above the control below, in their parent with a GridLayout.
	 */
	public static ViewerJsDialogs install(C8oBrowser c8oBrowser, Control below, String viewer) {
		ViewerJsDialogs dialogs = new ViewerJsDialogs(below, viewer);
		c8oBrowser.onBrowserReady(() -> dialogs.install(c8oBrowser.getBrowser()));
		return dialogs;
	}

	void install(Browser browser) {
		// a recreated browser no longer waits for the previous question
		onUiThread(() -> newPage("the browser was recreated"));

		browser.set(AlertCallback.class, (params, tell) -> {
			// the page does not wait for an alert
			tell.ok();
			String message = params.message() == null ? "" : params.message();
			Engine.logStudio.warn("(" + viewer + ") JavaScript alert from " + params.url() + ": " + message);
			onUiThread(() -> {
				alerts.add(message);
				// a waiting question keeps the bar, and its typed answer: the alert comes after it
				if (pending == null) {
					refresh();
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

		// a new page neither waits for the question of the previous one nor keeps its alerts
		browser.navigation().on(NavigationStarted.class, event -> {
			if (event.isInMainFrame() && !event.isSameDocument()) {
				onUiThread(() -> newPage("the page changes"));
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

	private void onUiThread(Runnable runnable) {
		try {
			display.asyncExec(runnable);
		} catch (Exception e) {
			// the Studio is closing
		}
	}

	private void newPage(String reason) {
		alerts.clear();
		Question question = pending;
		pending = null;
		if (question != null) {
			cancel(question, reason);
		}
		refresh();
	}

	private void show(Question question) {
		if (question.isAnswered()) {
			return;
		}
		if (bar.isDisposed()) {
			cancel(question, "the viewer is closed");
			return;
		}
		Question previous = pending;
		pending = question;
		if (previous != null) {
			cancel(previous, "a new question replaces it");
		}
		try {
			refresh();
		} catch (Exception e) {
			pending = null;
			cancel(question, "the viewer cannot show the question: " + e);
			refresh();
		}
	}

	/** Shows the waiting question, else the alerts, else hides the bar. */
	private void refresh() {
		if (bar.isDisposed()) {
			return;
		}
		// the keyboard goes to the bar only if it was in the viewer: an agent driving the page must not take it from
		// another editor
		boolean focusInViewer = isInViewer(display.getFocusControl());
		for (Control child : bar.getChildren()) {
			child.dispose();
		}
		Control focus = null;
		Question question = pending;
		if (question != null) {
			focus = showQuestion(question);
		} else if (!alerts.isEmpty()) {
			showAlerts();
		}
		boolean visible = question != null || !alerts.isEmpty();
		((GridData) bar.getLayoutData()).exclude = !visible;
		bar.setVisible(visible);
		bar.getParent().layout(true, true);
		if (focus != null && focusInViewer) {
			focus.setFocus();
		}
	}

	private Control showQuestion(Question question) {
		bar.setLayout(barLayout(question.isPrompt() ? 5 : 4));
		icon(SWT.ICON_QUESTION);
		message(question.message);

		Text field = null;
		if (question.isPrompt()) {
			Text input = new Text(bar, SWT.BORDER | SWT.SINGLE);
			input.setText(question.defaultValue);
			GridData inputData = new GridData(SWT.FILL, SWT.CENTER, false, false);
			inputData.widthHint = 200;
			input.setLayoutData(inputData);
			input.addListener(SWT.DefaultSelection, e -> reply(question, input.getText()));
			input.addListener(SWT.Traverse, e -> {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
					reply(question, null);
				}
			});
			input.selectAll();
			field = input;
		}
		Text answer = field;

		Button ok = new Button(bar, SWT.PUSH);
		ok.setText("OK");
		ok.addListener(SWT.Selection, e -> reply(question, answer != null ? answer.getText() : ""));
		Button cancel = new Button(bar, SWT.PUSH);
		cancel.setText("Cancel");
		cancel.addListener(SWT.Selection, e -> reply(question, null));
		return answer != null ? answer : ok;
	}

	private void showAlerts() {
		int count = alerts.size();
		bar.setLayout(barLayout(count > 1 ? 4 : 3));
		icon(SWT.ICON_INFORMATION);
		message(alerts.get(count - 1));
		if (count > 1) {
			Label counter = new Label(bar, SWT.NONE);
			counter.setText(count + " alerts");
			counter.setToolTipText("The previous alerts are in the Studio log");
			counter.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		}
		Button ok = new Button(bar, SWT.PUSH);
		ok.setText("OK");
		ok.addListener(SWT.Selection, e -> {
			alerts.clear();
			refresh();
		});
	}

	private GridLayout barLayout(int columns) {
		GridLayout layout = new GridLayout(columns, false);
		layout.marginWidth = 8;
		layout.marginHeight = 4;
		return layout;
	}

	private void icon(int systemImage) {
		Label icon = new Label(bar, SWT.NONE);
		icon.setImage(display.getSystemImage(systemImage));
		icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
	}

	private void message(String text) {
		Label message = new Label(bar, SWT.WRAP);
		message.setText(text.replace("&", "&&"));
		GridData messageData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		messageData.widthHint = 200;
		message.setLayoutData(messageData);
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
		if (pending == question) {
			pending = null;
		}
		if (question.answer(value)) {
			Engine.logStudio.info("(" + viewer + ") JavaScript " + (question.isPrompt() ? "prompt" : "confirm")
					+ " answered " + (value == null ? "Cancel" : "OK"));
		}
		// the alerts received meanwhile come back
		refresh();
	}

	private void cancel(Question question, String reason) {
		if (question.answer(null)) {
			Engine.logStudio.info("(" + viewer + ") JavaScript " + (question.isPrompt() ? "prompt" : "confirm")
					+ " answered Cancel: " + reason);
		}
	}
}
