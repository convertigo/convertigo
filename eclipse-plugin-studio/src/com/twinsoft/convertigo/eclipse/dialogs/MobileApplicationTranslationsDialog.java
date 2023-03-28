/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.Locale;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class MobileApplicationTranslationsDialog extends MyAbstractDialog {

	private MobileApplicationTranslationsDialogComposite mobileApplicationTranslationsDialog = null;
	
	private boolean auto = false;
	private Locale localeFrom = null, localeTo = null;
	
	public MobileApplicationTranslationsDialog(Shell parentShell) {
		this(parentShell, MobileApplicationTranslationsDialogComposite.class, "Mobile Application translations");
	}
	
	public MobileApplicationTranslationsDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle) {
		super(parentShell, dialogAreaClass, dialogTitle);
	}

	@Override
	protected void okPressed() {
		if (dialogComposite instanceof MobileApplicationTranslationsDialogComposite) {
			mobileApplicationTranslationsDialog = (MobileApplicationTranslationsDialogComposite)dialogComposite;
			setLocaleFrom((Locale) mobileApplicationTranslationsDialog.getValue("from"));
			setLocaleTo((Locale) mobileApplicationTranslationsDialog.getValue("to"));
			setAuto((boolean) mobileApplicationTranslationsDialog.getValue("auto"));
		}
		super.okPressed();
	}

	public Locale getLocaleFrom() {
		return localeFrom;
	}

	public void setLocaleFrom(Locale localeFrom) {
		this.localeFrom = localeFrom;
	}

	public Locale getLocaleTo() {
		return localeTo;
	}

	public void setLocaleTo(Locale localeTo) {
		this.localeTo = localeTo;
	}

	public boolean isAuto() {
		return auto;
	}

	public void setAuto(boolean auto) {
		this.auto = auto;
	}
	
}
