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

package com.twinsoft.convertigo.eclipse.swt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.event.BrowserBecameResponsive;
import com.teamdev.jxbrowser.browser.event.BrowserBecameUnresponsive;
import com.teamdev.jxbrowser.browser.event.RenderProcessTerminated;
import com.teamdev.jxbrowser.browser.event.TerminationStatus;
import com.teamdev.jxbrowser.dom.Element;
import com.teamdev.jxbrowser.dom.event.Event;
import com.teamdev.jxbrowser.dom.event.EventType;
import com.teamdev.jxbrowser.engine.ChromiumBinariesDeliveryException;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.engine.PasswordStore;
import com.teamdev.jxbrowser.engine.ProprietaryFeature;
import com.teamdev.jxbrowser.engine.RenderingMode;
import com.teamdev.jxbrowser.engine.Theme;
import com.teamdev.jxbrowser.engine.event.EngineCrashed;
import com.teamdev.jxbrowser.event.Observer;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished;
import com.teamdev.jxbrowser.navigation.event.LoadFinished;
import com.teamdev.jxbrowser.navigation.event.NavigationFinished;
import com.teamdev.jxbrowser.view.swt.BrowserView;
import com.teamdev.jxbrowser.zoom.ZoomLevel;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.NetworkUtils;

public class C8oBrowser extends Composite {
	
	private static Thread threadSwt = null;
	private static Map<String, Engine> browserContexts = new HashMap<>();
	private static boolean render_offscreen = "offscreen".equals(System.getProperty("jxbrowser.render"));
	private static final String ABOUT_BLANK = "about:blank";
	private static final long BROWSER_RECOVERY_THROTTLE = 5000;
	private static final long UNRESPONSIVE_RECOVERY_DELAY = 60000;
	
	private String debugUrl;
	private String browserId;
	private Engine browserContext;
	private Engine recoveryEngine;
	private BrowserView browserView;
	private boolean useExternalBrowser = false;
	private Function<Event, Boolean> onClick = null;
	private boolean closed = false;
	private boolean browserRecreating = false;
	private boolean browserUnresponsive = false;
	private long lastBrowserRecovery = 0;
	private String lastLoadedUrl = ABOUT_BLANK;
	private String lastHtml = null;
	private Runnable restoreHandler = null;
	private final List<Runnable> browserReadyHandlers = new ArrayList<>();

	private void init(Engine browserContext) {
		setLayout(new FillLayout());
		browserView = BrowserView.newInstance(this, browserContext.newBrowser());
		threadSwt = getDisplay().getThread();
		Browser browser = getBrowser();
		
		Observer<Event> observer = ev -> {
			if (onClick != null && true == onClick.apply(ev)) {
				return;
			}
			if (!useExternalBrowser) {
				return;
			}
			try {
				Element elt = (Element) ev.target().get();
				while (!elt.nodeName().equalsIgnoreCase("a")) {
					elt = (Element) elt.parent().get();
				}
				String href = elt.attributes().get("href");
				if (!href.startsWith("http")) {
					String url = elt.document().baseUri();
					if (href.startsWith("/")) {
						href = url.replaceFirst("(https?://.*?)/.*", "$1" + href);
					} else {
						href = url.replaceFirst("(https?://.*/).*", "$1" + href);
					}
				}
				if (href.matches("https?://.*")) {
					String msg = "Internal browser open link with the default browser: " + href;
					if (com.twinsoft.convertigo.engine.Engine.logStudio != null) {
						com.twinsoft.convertigo.engine.Engine.logStudio.info(msg);
					} else {
						System.out.println(msg);
					}
					Program.launch(href);
					ev.preventDefault();
				}
			} catch (Exception e) {
			}
		};
		browser.mainFrame().get().document().get().addEventListener(EventType.CLICK, observer, false);
		browser.navigation().on(FrameLoadFinished.class, event -> {
			try {
				event.frame().document().get().addEventListener(EventType.CLICK, observer, false);
			} catch (Exception e) {
				// can fail on img
			}
		});
		browser.navigation().on(NavigationFinished.class, event -> removePreviousAboutBlankEntry(browser));
		installRecoveryHandlers();
		notifyBrowserReady();
	}
	
	public C8oBrowser(Composite parent, int style) {
		this(parent, style, (Project) null);
	}

	public C8oBrowser(Composite parent, int style, Project project) {
		this(parent, style, project, "default");
	}
	
	private C8oBrowser(Composite parent, int style, Project project, String browserId) {
		super(parent, style);
		this.browserId = browserId;
		addDisposeListener(e -> {
			if (!closed) {
				closed = true;
				closeCurrentBrowser();
			}
		});
		boolean retry = false;
		do {
			File browserIdFile = null;
			if (project != null) {
				browserIdFile = new File(project.getDirPath() + "/_private/browser_id");
				browserId = Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
				try {
					browserId = FileUtils.readFileToString(browserIdFile, "UTF-8");
				} catch (Exception e) {
					try {
						FileUtils.write(browserIdFile, browserId, "UTF-8");
					} catch (IOException e1) {
					}
				}
			}
			this.browserId = browserId;
			try {
				init(getOrCreateBrowserContext());
			} catch (Exception e) {
				if (!retry) {
					if (browserIdFile != null) {
						browserIdFile.delete();
					}
					retry = true;
				} else {
					throw e;
				}
			}
		} while (retry);
	}

	public BrowserView getBrowserView() {
		return browserView;
	}

	public Browser getBrowser() {
		return browserView.getBrowser();
	}
	
	public void setText(String html) {
		lastHtml = html;
		lastLoadedUrl = ABOUT_BLANK;
		html = html.replace("target='_blank'", "");
		if (html.contains("$background$")) {
			org.eclipse.swt.graphics.Color bg = getBackground();
			String background = "rgb(" + bg.getRed() + ", " + bg.getGreen() + ", " + bg.getBlue() + ")";
			String foreground = bg.getRed() < 128 ? "white" : "black";
			String link = bg.getRed() < 128 ? "cyan" : "blue";
			html = html.replace("$background$", background).replace("$foreground$", foreground).replace("$link$", link);
		}
		getBrowser().mainFrame().get().document().get().documentElement().get().innerHtml(html);
		
		/** fix #522, have to recreate script after innerHtml **/
		getBrowser().mainFrame().get().executeJavaScript(
		"Array.from(document.querySelectorAll(\"script\")).forEach( oldScript => {\r\n"
		+ "    const newScript = document.createElement(\"script\");\r\n"
		+ "    Array.from(oldScript.attributes)\r\n"
		+ "      .forEach( attr => newScript.setAttribute(attr.name, attr.value) );\r\n"
		+ "    newScript.appendChild(document.createTextNode(oldScript.innerHTML));\r\n"
		+ "    oldScript.parentNode.replaceChild(newScript, oldScript);\r\n"
		+ "});");
	}
	
	public void reloadText() {
		getBrowser().mainFrame().ifPresent(frame -> setText(frame.html()));
	}

	public void setUrl(String url) {
		loadUrlWithRevalidation(url);
	}
	
	public void reset() {
		lastLoadedUrl = ABOUT_BLANK;
		lastHtml = null;
		getBrowser().navigation().loadUrlAndWait("about:blank");
	}
		
	@Override
	public boolean setFocus() {
		browserView.forceFocus();
		return super.setFocus();
	}

	public void addProgressListener(ProgressListener progressListener) {
		onBrowserReady(() -> getBrowser().navigation().on(LoadFinished.class, event -> {
			progressListener.completed(null);
		}));
	}
	
	public String getDebugUrl() {
		return debugUrl;
	}
	
	public static void run(Runnable runnable) {
		if (threadSwt != null && threadSwt.equals(Thread.currentThread())) {
			com.twinsoft.convertigo.engine.Engine.execute(runnable);
		} else {
			runnable.run();
		}
	}
	
	public void goBack() {
		getBrowser().navigation().goBack();
	}
	
	public void reload() {
		getBrowser().navigation().reload();
	}
	
	public int getCurrentNavigationEntryIndex() {
		return getBrowser().navigation().currentEntryIndex();
	}
	
	public <T> T executeJavaScriptAndReturnValue(String script) {
		return getBrowser().mainFrame().get().executeJavaScript(script);
	}
	
	public <T> T executeFunctionAndReturnValue(String function, Object... params) {
		return ((JsObject) getBrowser().mainFrame().get().executeJavaScript("window")).call(function, params);
	}
	
	public String getURL() {
		return getBrowser().url();
	}
	
	public void loadURL(String url) {
		loadUrlWithRevalidation(url);
	}
	
	public void setZoomEnabled(boolean enable) {
		if (!enable) {
			getBrowser().zoom().disable();
		}
	}
	
	public void setZoomLevel(ZoomLevel zoomLevel) {
		getBrowser().zoom().level(zoomLevel);
	}
	
	public void setUseExternalBrowser(boolean useExternalBrowser) {
		this.useExternalBrowser = useExternalBrowser;
	}
	
	public void onClick(Function<Event, Boolean> onClick) {
		this.onClick = onClick;
	}

	public void addToolItemNavigation(ToolBar toolbar) {
		addToolItemBack(toolbar);
		addToolItemStop(toolbar);
		addToolItemRefresh(toolbar);
		addToolItemForward(toolbar);
	}
	
	public void addToolItemBack(ToolBar toolbar) {
		var ti = new ToolItem(toolbar, SWT.NONE);
		try {
			ti.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/handlers_sc_exit.gif"));
		} catch (IOException e) {
			ti.setText("←");
		}
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				getBrowser().navigation().goBack();
			}
			
		});
		ti.setEnabled(false);
		onBrowserReady(() -> getBrowser().navigation().on(NavigationFinished.class, event -> ConvertigoPlugin.asyncExec(() ->
			ti.setEnabled(event.navigation().canGoBack())
		)));
	}
	
	public void addToolItemStop(ToolBar toolbar) {
		var ti = new ToolItem(toolbar, SWT.NONE);
		try {
			ti.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/stop_transaction.gif"));
		} catch (IOException e) {
			ti.setText("X");
		}
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				getBrowser().navigation().stop();
			}
			
		});
	}
	
	public void addToolItemRefresh(ToolBar toolbar) {
		var ti = new ToolItem(toolbar, SWT.NONE);
		try {
			ti.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/refresh.gif"));
		} catch (IOException e) {
			ti.setText("R");
		}
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				getBrowser().navigation().reload();
			}
			
		});
	}
	
	public void addToolItemForward(ToolBar toolbar) {
		var ti = new ToolItem(toolbar, SWT.NONE);
		try {
			ti.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/handlers_sc_entry.gif"));
		} catch (IOException e) {
			ti.setText("→");
		}
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				getBrowser().navigation().goForward();
			}

		});
		ti.setEnabled(false);
		onBrowserReady(() -> getBrowser().navigation().on(NavigationFinished.class, event -> ConvertigoPlugin.asyncExec(() ->
			ti.setEnabled(event.navigation().canGoForward())
		)));
	}

	public void addToolItemOpenExternal(ToolBar toolbar) {
		var ti = new ToolItem(toolbar, SWT.NONE);
		ti.setImage(ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/statement.png"));
		ti.setText("View with your external browser");
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch(getURL());
			}
			
		});
	}

	private void loadUrlWithRevalidation(String url) {
		lastLoadedUrl = url;
		lastHtml = null;
		if (url != null && url.matches("(?i)^https?://.*")) {
			var params = com.teamdev.jxbrowser.navigation.LoadUrlParams.newBuilder(url)
					.addExtraHeader(com.teamdev.jxbrowser.net.HttpHeader.of("Cache-Control", "no-cache"))
					.addExtraHeader(com.teamdev.jxbrowser.net.HttpHeader.of("Pragma", "no-cache"))
					.build();
			getBrowser().navigation().loadUrl(params);
		} else {
			getBrowser().navigation().loadUrl(url);
		}
	}

	private void removePreviousAboutBlankEntry(Browser browser) {
		if (!isCurrentBrowser(browser)) {
			return;
		}
		try {
			var navigation = browser.navigation();
			int currentIndex = navigation.currentEntryIndex();
			if (currentIndex <= 0) {
				return;
			}
			var currentEntry = navigation.entryAtIndex(currentIndex);
			if (currentEntry == null || ABOUT_BLANK.equalsIgnoreCase(currentEntry.url())) {
				return;
			}
			for (int index = currentIndex - 1; index >= 0; index--) {
				var entry = navigation.entryAtIndex(index);
				if (entry == null || !ABOUT_BLANK.equalsIgnoreCase(entry.url())) {
					break;
				}
				navigation.removeEntryAtIndex(index);
			}
		} catch (Exception e) {
		}
	}

	public void setRestoreHandler(Runnable restoreHandler) {
		this.restoreHandler = restoreHandler;
	}

	public void onBrowserReady(Runnable handler) {
		if (handler == null) {
			return;
		}
		browserReadyHandlers.add(handler);
		if (canUseBrowserControl()) {
			runBrowserReadyHandler(handler);
		}
	}

	private Engine getOrCreateBrowserContext() {
		File browserWorks = new File(com.twinsoft.convertigo.engine.Engine.USER_WORKSPACE_PATH + "/browser-works");
		browserWorks.mkdirs();
		Engine browserContext = browserContexts.get(browserId);
		if (browserContext == null || browserContext.isClosed()) {
			int debugPort;
			try {
				debugPort = (int) (Long.parseLong(browserId, Character.MAX_RADIX) % 10000) + 30000;
			} catch (Exception e) {
				debugPort = 30000;
			}
			debugPort = NetworkUtils.nextAvailable(debugPort);
			boolean off = render_offscreen || ConvertigoPlugin.getBrowserOffscreen();

			int rt = 2;
			while (rt > 0) {
				try {
					browserContext = Engine.newInstance(EngineOptions.newBuilder(off ? RenderingMode.OFF_SCREEN : RenderingMode.HARDWARE_ACCELERATED)
							.userDataDir(Paths.get(com.twinsoft.convertigo.engine.Engine.USER_WORKSPACE_PATH, "browser-works", browserId))
							.licenseKey(JBL.get())
							.passwordStore(PasswordStore.BASIC)
							.enableProprietaryFeature(ProprietaryFeature.AAC)
							.enableProprietaryFeature(ProprietaryFeature.H_264)
							.addSwitch("--illegal-access=warn")
							.addSwitch("--remote-allow-origins=*")
							.remoteDebuggingPort(debugPort).build());
					rt = 0;
					browserContext.setTheme(SwtUtils.isDark() ? Theme.DARK : Theme.LIGHT);
				} catch (ChromiumBinariesDeliveryException e) {
					rt--;
					if (rt == 0) {
						throw e;
					}
					String msg = e.getMessage();
					String path = msg.replaceFirst(".*?into ", "");
					try {
						FileUtils.deleteDirectory(new File(path));
						msg = "Browser extraction failed. Folder '" + path + "' deleted.";
						if (com.twinsoft.convertigo.engine.Engine.logStudio != null) {
							com.twinsoft.convertigo.engine.Engine.logStudio.info(msg);
						} else {
							System.out.println(msg);
						}
					} catch (IOException e1) {
						throw e;
					}
				}
			}
			browserContexts.put(browserId, browserContext);
		}
		debugUrl = "http://localhost:" + browserContext.options().remoteDebuggingPort().get();
		this.browserContext = browserContext;
		return browserContext;
	}

	private void installRecoveryHandlers() {
		Browser browser = getBrowser();
		browser.on(RenderProcessTerminated.class, event -> {
			if (event.status() != TerminationStatus.NORMAL_TERMINATION) {
				ConvertigoPlugin.asyncExec(() -> recoverBrowser(
						browser,
						"Render process terminated: " + event.status() + " (exit " + event.exitCode() + ")",
						true));
			}
		});
		browser.on(BrowserBecameUnresponsive.class,
				event -> ConvertigoPlugin.asyncExec(() -> browserBecameUnresponsive(browser)));
		browser.on(BrowserBecameResponsive.class,
				event -> ConvertigoPlugin.asyncExec(() -> {
					if (isCurrentBrowser(browser)) {
						browserUnresponsive = false;
					}
				}));
		browser.navigation().on(NavigationFinished.class, event -> {
			if (event.isInMainFrame() && event.isErrorPage()) {
				ConvertigoPlugin.asyncExec(() -> recoverBrowser(browser, "Navigation error page: " + event.error(), false));
			}
		});
		Engine engine = browser.engine();
		if (recoveryEngine != engine) {
			recoveryEngine = engine;
			engine.on(EngineCrashed.class, event -> ConvertigoPlugin.asyncExec(() -> {
				if (recoveryEngine != engine) {
					return;
				}
				browserContexts.remove(browserId);
				browserContext = null;
				recoverBrowser(browser, "JxBrowser engine crashed: exit " + event.exitCode(), true);
			}));
		}
	}

	private void browserBecameUnresponsive(Browser browser) {
		if (!isCurrentBrowser(browser)) {
			return;
		}
		browserUnresponsive = true;
		getDisplay().timerExec((int) UNRESPONSIVE_RECOVERY_DELAY, () -> {
			if (isCurrentBrowser(browser) && browserUnresponsive) {
				recoverBrowser(browser, "Browser stayed unresponsive for " + (UNRESPONSIVE_RECOVERY_DELAY / 1000) + " seconds",
						true);
			}
		});
	}

	private void recoverBrowser(Browser browser, String reason, boolean recreate) {
		if (!isCurrentBrowser(browser)) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastBrowserRecovery < BROWSER_RECOVERY_THROTTLE && (!recreate || browserRecreating)) {
			return;
		}
		lastBrowserRecovery = now;
		logRecovery(reason);
		if (recreate) {
			recreateBrowserView(reason);
		} else {
			restoreBrowser();
		}
	}

	private void recreateBrowserView(String reason) {
		if (!canUseBrowserControl() || browserRecreating) {
			return;
		}
		browserRecreating = true;
		try {
			closeCurrentBrowser();
			if (browserView != null && !browserView.isDisposed()) {
				browserView.dispose();
			}
			try {
				init(browserContext != null && !browserContext.isClosed() ? browserContext : getOrCreateBrowserContext());
			} catch (Exception e) {
				browserContexts.remove(browserId);
				browserContext = null;
				recoveryEngine = null;
				init(getOrCreateBrowserContext());
			}
			layout(true, true);
			restoreBrowser();
		} catch (Exception e) {
			String message = "(C8oBrowser) Unable to recreate browser after: " + reason;
			if (com.twinsoft.convertigo.engine.Engine.logStudio != null) {
				com.twinsoft.convertigo.engine.Engine.logStudio.warn(message, e);
			} else {
				e.printStackTrace();
			}
		} finally {
			browserUnresponsive = false;
			browserRecreating = false;
		}
	}

	private void restoreBrowser() {
		if (!canUseBrowserControl()) {
			return;
		}
		if (restoreHandler != null) {
			restoreHandler.run();
			return;
		}
		if (lastHtml != null) {
			setText(lastHtml);
		} else if (lastLoadedUrl != null) {
			loadUrlWithRevalidation(lastLoadedUrl);
		}
	}

	private void closeCurrentBrowser() {
		try {
			if (browserView == null || browserView.isDisposed()) {
				return;
			}
			Browser browser = browserView.getBrowser();
			if (browser != null && !browser.isClosed()) {
				run(browser::close);
			}
		} catch (Exception e) {
		}
	}

	private boolean canUseBrowserControl() {
		return !closed && !isDisposed() && browserView != null && !browserView.isDisposed();
	}

	private boolean isCurrentBrowser(Browser browser) {
		try {
			return canUseBrowserControl() && browserView.getBrowser() == browser;
		} catch (Exception e) {
			return false;
		}
	}

	private void notifyBrowserReady() {
		for (Runnable handler : new ArrayList<>(browserReadyHandlers)) {
			runBrowserReadyHandler(handler);
		}
	}

	private void runBrowserReadyHandler(Runnable handler) {
		try {
			handler.run();
		} catch (Exception e) {
			if (com.twinsoft.convertigo.engine.Engine.logStudio != null) {
				com.twinsoft.convertigo.engine.Engine.logStudio.warn("(C8oBrowser) Browser ready handler failed", e);
			} else {
				e.printStackTrace();
			}
		}
	}

	private void logRecovery(String reason) {
		String message = "(C8oBrowser) Recovering browser: " + reason;
		if (com.twinsoft.convertigo.engine.Engine.logStudio != null) {
			com.twinsoft.convertigo.engine.Engine.logStudio.warn(message);
		} else {
			System.out.println(message);
		}
	}
}
