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
 * MERCHANTABILITY  or  FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.views.loggers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.teamdev.jxbrowser.dom.Element;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowserPostMessageHelper;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.admin.AuthenticatedBrowser;
import com.twinsoft.convertigo.eclipse.views.admin.AdminView;

public class LogViewerView extends ViewPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.views.loggers.EngineLogView_preview";
	private static final String LOG_VIEWER_PATH = "/admin/eclipse/logs";
	private static final String EMBEDDED_LOG_VIEWER_PATH = LOG_VIEWER_PATH + "?studioMode=true";

	private C8oBrowser browser = null;
	private C8oBrowserPostMessageHelper postMessageHelper = null;
	private AuthenticatedBrowser authenticatedBrowser = null;

	@Override
	public void dispose() {
		if (authenticatedBrowser != null) {
			authenticatedBrowser.dispose();
			authenticatedBrowser = null;
		}
		postMessageHelper = null;
		if (browser != null) {
			browser.dispose();
			browser = null;
		}
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		SwtUtils.refreshTheme();

		parent.setLayout(new FillLayout());
		browser = new C8oBrowser(parent, SWT.NONE);
		browser.setUseExternalBrowser(false);
		ConvertigoPlugin.logDebug("Log viewer debug : " + browser.getDebugUrl());

		browser.onClick(ev -> {
			try {
				Element elt = (Element) ev.target().get();
				while (!elt.nodeName().equalsIgnoreCase("a")) {
					elt = (Element) elt.parent().get();
				}
				String href = elt.attributes().get("href");
				if (href.startsWith("http")) {
					ev.preventDefault();
					Program.launch(href);
					return false;
				}
			} catch (Exception e) {
			}
			return true;
		});

		postMessageHelper = new C8oBrowserPostMessageHelper(browser);
		postMessageHelper.onMessage(message -> {
			if ("reloadEngineLogView".equals(message.optString("type"))) {
				ConvertigoPlugin.asyncExec(this::reloadBrowser);
			}
		});

		authenticatedBrowser = new AuthenticatedBrowser(browser, this::getEmbeddedUrl);
		authenticatedBrowser.load();
		parent.layout(true, true);
	}

	@Override
	public void setFocus() {
		if (browser != null) {
			browser.setFocus();
		}
	}

	private void reloadBrowser() {
		if (authenticatedBrowser != null) {
			authenticatedBrowser.load();
		}
	}

	private String getEmbeddedUrl() {
		return AdminView.getAuthenticatedUrl(EMBEDDED_LOG_VIEWER_PATH);
	}
}
