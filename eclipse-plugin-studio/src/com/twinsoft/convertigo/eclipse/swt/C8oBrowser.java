/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.dom.Element;
import com.teamdev.jxbrowser.dom.event.Event;
import com.teamdev.jxbrowser.dom.event.EventType;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.engine.RenderingMode;
import com.teamdev.jxbrowser.event.Observer;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished;
import com.teamdev.jxbrowser.navigation.event.LoadFinished;
import com.teamdev.jxbrowser.view.swt.BrowserView;
import com.teamdev.jxbrowser.zoom.ZoomLevel;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.NetworkUtils;

public class C8oBrowser extends Composite {
	
	private static Thread threadSwt = null;
	private static Map<String, Engine> browserContexts = new HashMap<>();
	private static boolean render_offscreen = "offscreen".equals(System.getProperty("jxbrowser.render"));
	
	private String debugUrl;
	private BrowserView browserView;
	private boolean useExternalBrowser = false;
	private double dpiRatio = 1;

	private void init(Engine browserContext) {
		setLayout(new FillLayout());
		browserView = BrowserView.newInstance(this, browserContext.newBrowser());
		threadSwt = getDisplay().getThread();
		
		Observer<Event> observer = ev -> {
			if (!useExternalBrowser) {
				return;
			}
			try {
				Element elt = (Element) ev.target().get();
				if (!elt.nodeName().equalsIgnoreCase("a")) {
					return;
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
					com.twinsoft.convertigo.engine.Engine.logStudio.info("Internal browser open link with the default browser: " + href);
					Program.launch(href);
					ev.preventDefault();
				}
			} catch (Exception e) {
			}
		};
		getBrowser().mainFrame().get().document().get().addEventListener(EventType.CLICK, observer, false);
		getBrowser().navigation().on(FrameLoadFinished.class, event -> {
			event.frame().document().get().addEventListener(EventType.CLICK, observer, false);
		});
	}
	
	public C8oBrowser(Composite parent, int style) {
		this(parent, style, (Project) null);
	}

	public C8oBrowser(Composite parent, int style, Project project) {
		super(parent, style);
		boolean retry = false;
		dpiRatio = parent.getDisplay().getDPI().x / 96f;
		do {
			File browserIdFile = null;
			String browserId = "default";
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
				browserContext = Engine.newInstance(EngineOptions.newBuilder(render_offscreen ? RenderingMode.OFF_SCREEN : RenderingMode.HARDWARE_ACCELERATED)
						.userDataDir(Paths.get(com.twinsoft.convertigo.engine.Engine.USER_WORKSPACE_PATH, "browser-works", browserId))
						.licenseKey(JBL.get())
						.addSwitch("--illegal-access=warn")
						.remoteDebuggingPort(debugPort).build());
				browserContexts.put(browserId, browserContext);
			}
			debugUrl = "http://localhost:" + browserContext.options().remoteDebuggingPort().get();
			try {
				init(browserContext);
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

	public C8oBrowser(Composite parent, int style, Engine browserContext) {
		super(parent, style);
		init(browserContext);
	}
	
	@Override
	public void dispose() {
		run(() -> {
			getBrowser().close();
		});
		super.dispose();
	}

	public BrowserView getBrowserView() {
		return browserView;
	}

	public Browser getBrowser() {
		return browserView.getBrowser();
	}
	
	public void setText(String html) {
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
		getBrowser().navigation().loadUrl(url);
	}
	
	public void reset() {
		getBrowser().navigation().loadUrlAndWait("about:blank");
	}
		
	@Override
	public boolean setFocus() {
		C8oBrowser.run(() -> browserView.forceFocus());
		return super.setFocus();
	}

	public void addProgressListener(ProgressListener progressListener) {
		getBrowser().navigation().on(LoadFinished.class, event -> {
			progressListener.completed(null);
		});
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
		getBrowser().navigation().loadUrl(url);
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
	
	public int fixDPI(int i) {
		return (int) (i * dpiRatio);
	}
}
