package com.twinsoft.convertigo.eclipse.editors.mobile;

import com.teamdev.jxbrowser.chromium.JSObject;

public class ApplicationComponentBrowserImpl implements ApplicationComponentBrowserInterface {
	ApplicationComponentBrowserInterface i;

	public ApplicationComponentBrowserImpl(ApplicationComponentBrowserInterface i) {
		this.i = i;
	}
	
	@Override
	public void onDragOver(JSObject o) {
		i.onDragOver(o);
	}

	@Override
	public void onDrop(JSObject o) {
		i.onDrop(o);
	}

}
