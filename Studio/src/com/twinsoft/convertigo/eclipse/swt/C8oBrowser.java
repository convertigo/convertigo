package com.twinsoft.convertigo.eclipse.swt;

import java.awt.Frame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Composite;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.BrowserPreferences;
import com.teamdev.jxbrowser.chromium.events.FailLoadingEvent;
import com.teamdev.jxbrowser.chromium.events.FinishLoadingEvent;
import com.teamdev.jxbrowser.chromium.events.FrameLoadEvent;
import com.teamdev.jxbrowser.chromium.events.LoadEvent;
import com.teamdev.jxbrowser.chromium.events.LoadListener;
import com.teamdev.jxbrowser.chromium.events.ProvisionalLoadingEvent;
import com.teamdev.jxbrowser.chromium.events.StartLoadingEvent;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class C8oBrowser extends Composite {
	
	static {
		//int port = ConvertigoPlugin.getDefault().getEmbeddedTomcat().getHttpPort() + 2;
		int port = 18082;
		BrowserPreferences.setChromiumSwitches("--remote-debugging-port=" + port);
	}

	private BrowserView browserView;

	public C8oBrowser(Composite parent, int style) {
		super(parent, style | SWT.EMBEDDED | SWT.NO_BACKGROUND);
	    Frame frame = SWT_AWT.new_Frame(this);
		browserView = new BrowserView(new Browser());
		frame.add(browserView);
	}
	
	
	
	@Override
	public void dispose() {
		getBrowser().dispose();
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
}
