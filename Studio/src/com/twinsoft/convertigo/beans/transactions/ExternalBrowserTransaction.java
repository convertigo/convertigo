/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.transactions;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.connectors.ExternalBrowserConnector;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.externalbrowser.ExternalBrowserInterface;
import com.twinsoft.convertigo.engine.externalbrowser.events.DocumentCompletedListener;

public class ExternalBrowserTransaction extends TransactionWithVariables {
	private static final long serialVersionUID = -1726228364762123615L;


	public ExternalBrowserTransaction() {
		super();
	}

	@Override
	public void runCore() throws EngineException {
		//		String location = SiteClipperConnector.constructSiteLocation(context, getConnector(), targetURL);		
		//		Engine.logBeans.debug("(SiteClipperTransaction) Computed location for SiteClipper : " + location);
		//		context.outputDocument.getDocumentElement().setAttribute("redirect_location", location);
		//		
		//		context.setSharedScope(RhinoUtils.copyScope(((RequestableThread) Thread.currentThread()).javascriptContext, scope));
		//		Engine.logEngine.info("bench begin");

		//		ExternalBrowser externalBrowser = ExtBroLauncher.getInstance().launch(BrowserVersion.firefox3);
		//		Engine.logEngine.info("bench start");
		//		Document doc = null;
		//		for (String url : Arrays.asList("http://www.google.fr", "http://finus", "http://demo.convertigo.net/cems/", "http://www.google.fr", "http://finus", "http://demo.convertigo.net/cems/", "about:blank")) {
		//			doc = externalBrowser.gotoUrl(url);
		//			Engine.logEngine.info("bench goto " + url);
		//		}
		//		externalBrowser.terminate();
		//		Engine.logEngine.info("bench term");
		//		context.outputDocument.getDocumentElement().appendChild(context.outputDocument.importNode(doc.getDocumentElement(), true));
		String url = (String) variables.get("url");
		if (url == null) {
			url = "http://www.google.fr";
		}

		final ExternalBrowserInterface ebi = getConnector().getEBI();
		ebi.setContext(context);
		final boolean [] done = { false };
		DocumentCompletedListener listener = new DocumentCompletedListener() {
			public void onDocumentCompleted() {
				synchronized (this) {
					done[0] = true;
					this.notify();
				}
			}
		};
		try {
			ebi.addDocumentCompledListener(listener);
			synchronized (listener) {
				ebi.gotoUrl(url);
				//Document d = ebi.getDom();
				try {
					listener.wait(30000);
					if (!done[0]) {
						throw new EngineException("(ExternalBrowserTransaction) Wait document completed timeout !");
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Document doc = ebi.getDom();
			if (doc != null) {
				context.outputDocument.getDocumentElement().appendChild(context.outputDocument.importNode(doc.getDocumentElement(), true));
			} else {
				throw new EngineException("(EBA) no dom !");
			}
		} finally {
			ebi.removeDocumentCompledListener(listener);
			//			ebi.setContext(null);
		}
	}

	@Override
	public void setStatisticsOfRequestFromCache() {
		// TODO Auto-generated method stub

	}

	@Override
	public ExternalBrowserConnector getConnector() {
		return (ExternalBrowserConnector) super.getConnector();
	}
}
