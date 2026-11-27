/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.eclipse.editors;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.AlertCallback;
import com.teamdev.jxbrowser.browser.callback.PromptCallback;
import com.teamdev.jxbrowser.event.Observer;
import com.teamdev.jxbrowser.navigation.Navigation;
import com.teamdev.jxbrowser.navigation.event.NavigationStarted;
import com.twinsoft.convertigo.engine.Engine;

/** Run against compiled Studio and Eclipse dependencies, with -XstartOnFirstThread on macOS. */
public class ViewerJsDialogsTest {

	public static void main(String[] args) {
		Engine.logStudio = Logger.getLogger(ViewerJsDialogsTest.class);
		var display = new CountingDisplay();
		try {
			for (String viewer : new String[] { "NGX viewer", "Mobile viewer" }) {
				exerciseViewer(display, viewer);
			}
			System.out.println("Viewer dialog checks passed");
		} finally {
			display.dispose();
		}
	}

	private static void exerciseViewer(CountingDisplay display, String viewer) {
		var shell = new Shell(display);
		try {
			shell.setLayout(new GridLayout());
			var below = new Composite(shell, SWT.NONE);
			var dialogs = new ViewerJsDialogs(below, viewer);
			var browser = new BrowserCallbacks();
			dialogs.install(browser.browser);
			drain(display);

			var bar = (Composite) shell.getChildren()[0];
			int queued = display.queued.get();
			CompletableFuture.runAsync(() -> {
				for (int i = 1; i <= 500; i++) {
					browser.alert("alert " + i);
				}
			}).join();
			check(browser.alertReplies == 500, "all alerts must be answered immediately");
			check(display.queued.get() - queued == 1, "an alert burst must queue one UI task");
			drain(display);
			check(hasLabel(bar, "alert 500") && hasLabel(bar, "500 alerts"), "last message and count");

			var answers = new java.util.ArrayList<String>();
			browser.prompt(answers::add);
			drain(display);
			var input = (Text) bar.getChildren()[2];
			input.setText("typed answer");
			browser.alert("during prompt");
			drain(display);
			check(!input.isDisposed() && input.getText().equals("typed answer"), "the prompt keeps its input");
			((Button) bar.getChildren()[3]).notifyListeners(SWT.Selection, null);
			check(answers.equals(java.util.List.of("typed answer")), "the page receives the typed answer once");
			check(hasLabel(bar, "during prompt") && hasLabel(bar, "501 alerts"), "alerts return after the question");

			((Button) bar.getChildren()[3]).notifyListeners(SWT.Selection, null);
			check(!bar.getVisible(), "OK acknowledges the alert batch");
			browser.alert("old page");
			drain(display);
			var oldPageOk = (Button) bar.getChildren()[2];
			browser.prompt(answers::add);
			browser.navigate();
			browser.alert("new page");
			oldPageOk.notifyListeners(SWT.Selection, null);
			drain(display);
			check(answers.size() == 2 && answers.get(1) == null, "navigation cancels the old question once");
			check(hasLabel(bar, "new page") && !hasLabel(bar, "2 alerts"), "only the new page's alert remains");

			var replacement = new BrowserCallbacks();
			dialogs.install(replacement.browser);
			replacement.alert("replacement page");
			browser.alert("obsolete browser");
			browser.navigate();
			browser.prompt(answers::add);
			drain(display);
			check(answers.size() == 3 && answers.get(2) == null, "a question from the replaced browser is cancelled");
			check(hasLabel(bar, "replacement page"), "obsolete callbacks do not change the bar");

			replacement.prompt(answers::add);
			drain(display);
			replacement.alert("queued at close");
			shell.dispose();
			drain(display);
			queued = display.queued.get();
			replacement.alert("after close");
			replacement.prompt(answers::add);
			replacement.navigate();
			check(display.queued.get() == queued, "closed viewers do not queue work");
			check(answers.size() == 5 && answers.get(3) == null && answers.get(4) == null,
					"closing cancels both waiting and late questions once");
		} finally {
			shell.dispose();
		}
	}

	private static boolean hasLabel(Composite bar, String text) {
		for (var control : bar.getChildren()) {
			if (control instanceof Label label && text.equals(label.getText())) {
				return true;
			}
		}
		return false;
	}

	private static void drain(Display display) {
		while (display.readAndDispatch()) {
			// Run the queued SWT events.
		}
	}

	private static void check(boolean condition, String description) {
		if (!condition) {
			throw new AssertionError(description);
		}
	}

	private static final class CountingDisplay extends Display {
		final AtomicInteger queued = new AtomicInteger();

		@Override protected void checkSubclass() {}

		@Override public void asyncExec(Runnable runnable) {
			queued.incrementAndGet();
			super.asyncExec(runnable);
		}
	}

	private static final class BrowserCallbacks {
		private final Map<Class<?>, Object> callbacks = new HashMap<>();
		private Observer<NavigationStarted> navigationStarted;
		private int alertReplies;
		final Browser browser;

		@SuppressWarnings("unchecked")
		BrowserCallbacks() {
			var navigation = (Navigation) Proxy.newProxyInstance(Navigation.class.getClassLoader(),
					new Class<?>[] { Navigation.class }, (proxy, method, args) -> {
						navigationStarted = (Observer<NavigationStarted>) args[1];
						return null;
					});
			browser = (Browser) Proxy.newProxyInstance(Browser.class.getClassLoader(),
					new Class<?>[] { Browser.class }, (proxy, method, args) -> {
						if (method.getName().equals("navigation")) {
							return navigation;
						}
						callbacks.put((Class<?>) args[0], args[1]);
						return null;
					});
		}

		void alert(String text) {
			((AlertCallback) callbacks.get(AlertCallback.class)).on(new AlertCallback.Params() {
				@Override public String message() { return text; }
				@Override public String url() { return "about:blank"; }
			}, new AlertCallback.Action(response -> alertReplies++));
		}

		void prompt(Consumer<String> answer) {
			((PromptCallback) callbacks.get(PromptCallback.class)).on(new PromptCallback.Params() {
				@Override public String message() { return "Question"; }
				@Override public String url() { return "about:blank"; }
				@Override public String text() { return "Initial"; }
			}, new PromptCallback.Action(response -> answer.accept(response.hasCancel() ? null : response.getEnterText())));
		}

		void navigate() {
			navigationStarted.on(new NavigationStarted() {
				@Override public boolean isInMainFrame() { return true; }
				@Override public boolean isSameDocument() { return false; }
			});
		}
	}
}
