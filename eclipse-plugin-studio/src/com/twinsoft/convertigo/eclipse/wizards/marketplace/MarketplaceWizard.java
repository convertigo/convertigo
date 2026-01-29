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

package com.twinsoft.convertigo.eclipse.wizards.marketplace;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.marketplace.MarketplaceView;

public class MarketplaceWizard extends Wizard implements INewWizard, IExecutableExtension {
	private String tag;
	private String pageTitle;
	private String pageDescription;

	public MarketplaceWizard() {
		setNeedsProgressMonitor(false);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		if (config != null) {
			String name = config.getAttribute("name");
			if (StringUtils.isNotBlank(name)) {
				setWindowTitle(name);
				pageTitle = name;
			}
			var descriptions = config.getChildren("description");
			if (descriptions != null && descriptions.length > 0) {
				String value = descriptions[0].getValue();
				if (StringUtils.isNotBlank(value)) {
					pageDescription = value.trim();
				}
			}
		}

		if (data instanceof String) {
			String payload = ((String) data).trim();
			if (!payload.isEmpty()) {
				try {
					JSONObject json = new JSONObject(payload);
					String parsedTag = json.optString("tag", null);
					if (StringUtils.isNotBlank(parsedTag)) {
						tag = parsedTag;
					}
					String parsedTitle = json.optString("title", null);
					if (StringUtils.isNotBlank(parsedTitle)) {
						pageTitle = parsedTitle;
					}
					String parsedDescription = json.optString("description", null);
					if (StringUtils.isNotBlank(parsedDescription)) {
						pageDescription = parsedDescription;
					}
				} catch (Exception e) {
					tag = payload;
				}
			}
		}
	}

	@Override
	public void addPages() {
		if (StringUtils.isBlank(pageTitle)) {
			pageTitle = "Open Marketplace";
			setWindowTitle(pageTitle);
		}
		if (StringUtils.isBlank(pageDescription)) {
			if (StringUtils.isNotBlank(tag)) {
				pageDescription = "Opens the Convertigo Marketplace filtered by \"" + tag + "\".";
			} else {
				pageDescription = "Opens the Convertigo Marketplace.";
			}
		}
		addPage(new MarketplaceWizardPage(pageTitle, pageDescription, tag));
	}

	@Override
	public boolean performFinish() {
		ConvertigoPlugin.asyncExec(this::openMarketplace);
		return true;
	}

	private void openMarketplace() {
		try {
			var workbench = PlatformUI.getWorkbench();
			var window = workbench.getActiveWorkbenchWindow();
			if (window == null) {
				return;
			}
			var page = window.getActivePage();
			if (page == null) {
				return;
			}
			var view = (MarketplaceView) page.showView(MarketplaceView.ID);
			if (StringUtils.isNotBlank(tag)) {
				view.openTag(tag);
			}
		} catch (Exception e) {
		}
	}
}
