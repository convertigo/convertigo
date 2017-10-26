package com.twinsoft.convertigo.eclipse.swt;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.BrowserContext;
import com.teamdev.jxbrowser.chromium.BrowserContextParams;
import com.teamdev.jxbrowser.chromium.BrowserPreferences;
import com.teamdev.jxbrowser.chromium.events.FailLoadingEvent;
import com.teamdev.jxbrowser.chromium.events.FinishLoadingEvent;
import com.teamdev.jxbrowser.chromium.events.FrameLoadEvent;
import com.teamdev.jxbrowser.chromium.events.LoadEvent;
import com.teamdev.jxbrowser.chromium.events.LoadListener;
import com.teamdev.jxbrowser.chromium.events.ProvisionalLoadingEvent;
import com.teamdev.jxbrowser.chromium.events.StartLoadingEvent;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class C8oBrowser extends Composite {
	
	static {
		int port = 18082;
		BrowserPreferences.setChromiumSwitches("--remote-debugging-port=" + port);
	}

	private static BrowserContext getBrowserContext(Project project) {
		File browserIdFile = new File(project.getDirPath() + "/_private/browser_id");
		String browserId = Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
		try {
			browserId = FileUtils.readFileToString(browserIdFile, "UTF-8");
		} catch (Exception e) {
			try {
				FileUtils.write(browserIdFile, browserId, "UTF-8");
			} catch (IOException e1) {
			}
		}
		File browserWorks = new File(Engine.USER_WORKSPACE_PATH + "/browser-works");
		browserWorks.mkdirs();
		return new BrowserContext(new BrowserContextParams(Engine.USER_WORKSPACE_PATH + "/browser-works/" + browserId));
	}
	
	private static Thread threadSwt = null;

	private BrowserView browserView;

	public C8oBrowser(Composite parent, int style) {
		this(parent, style, BrowserContext.defaultContext());
	}

	public C8oBrowser(Composite parent, int style, Project project) {
		this(parent, style, getBrowserContext(project));
	}

	public C8oBrowser(Composite parent, int style, BrowserContext browserContext) {
		super(parent, style | SWT.EMBEDDED | SWT.NO_BACKGROUND);
	    Frame frame = SWT_AWT.new_Frame(this);
	    browserView = new BrowserView(new Browser(browserContext));
		frame.add(browserView);
		threadSwt = parent.getDisplay().getThread();
		parent.addDisposeListener(new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				dispose();
			}
		});
	}
	
	@Override
	public void dispose() {
		run(() -> {
			getBrowser().dispose();			
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
		getBrowser().loadHTML(html);
	}

	public void setUrl(String url) {
		getBrowser().loadURL(url);
	}
		
	@Override
	public boolean setFocus() {
		C8oBrowser.run(() -> browserView.requestFocus());
		return super.setFocus();
	}

	public void addProgressListener(ProgressListener progressListener) {
		getBrowser().addLoadListener(new LoadListener() {
			
			@Override
			public void onStartLoadingFrame(StartLoadingEvent event) {
			}
			
			@Override
			public void onProvisionalLoadingFrame(ProvisionalLoadingEvent event) {
			}
			
			@Override
			public void onFinishLoadingFrame(FinishLoadingEvent event) {
				progressListener.completed(null);
			}
			
			@Override
			public void onFailLoadingFrame(FailLoadingEvent event) {
				
			}
			
			@Override
			public void onDocumentLoadedInMainFrame(LoadEvent event) {
			}
			
			@Override
			public void onDocumentLoadedInFrame(FrameLoadEvent event) {
				
			}
		});
	}
	
	public static void run(Runnable runnable) {
		if (threadSwt != null && threadSwt.equals(Thread.currentThread())) {
			Engine.execute(runnable);
		} else {
			runnable.run();
		}
	}
}
