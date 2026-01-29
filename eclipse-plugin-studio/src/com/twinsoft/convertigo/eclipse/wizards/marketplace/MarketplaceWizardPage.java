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
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

class MarketplaceWizardPage extends WizardPage {
	private final String message;

	MarketplaceWizardPage(String title, String description, String tag) {
		super("marketplaceWizardPage");
		setTitle(title);
		setDescription(description);
		if (StringUtils.isNotBlank(tag)) {
			message = "Click Finish to open the Convertigo Marketplace filtered by \"" + tag + "\".";
		} else {
			message = "Click Finish to open the Convertigo Marketplace.";
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		Label label = new Label(container, SWT.WRAP);
		label.setText(message);
		GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		gridData.widthHint = 400;
		label.setLayoutData(gridData);

		setControl(container);
		setPageComplete(true);
	}
}
