package com.twinsoft.convertigo.beans.connectors;

import com.convertigo.externalbrowser.common.enums.BrowserVersion;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.transactions.ExternalBrowserTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.externalbrowser.ExternalBrowserInterface;

public class ExternalBrowserConnector extends Connector {
	private static final long serialVersionUID = -9120542678913016863L;
	
	private String browserVersion = "";

	transient ExternalBrowserInterface ebi = null;
	
	@Override
	public void prepareForTransaction(Context context) throws EngineException {
		// TODO Auto-generated method stub

	}

	@Override
	public ExternalBrowserTransaction newTransaction() {
		return new ExternalBrowserTransaction();
	}

	public String getBrowserVersion() {
		return browserVersion.toString();
	}

	public void setBrowserVersion(String browserVersion) {
		this.browserVersion = browserVersion;
	}
	
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("browserVersion")) {
			return BrowserVersion.names();
		}
		return super.getTagsForProperty(propertyName);
	}
	
	public ExternalBrowserInterface getEBI() throws EngineException {
		if (ebi == null || ebi.isDead()) {
			ebi = Engine.theApp.externalBrowserManager.getEbi(BrowserVersion.valueOf(browserVersion));
		}
		return ebi;
	}
}