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

package com.twinsoft.convertigo.eclipse.preferences;

import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.EnginePreferenceComposite;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyType;

public class EnginePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public EnginePreferencePage() {
		super();
		setDescription("You can hover your mouse on all properties configured with symbols in order to display the computed value in a tooltip.");
	}
	
	private Map<PropertyName, String> modifiedProperties;
	
	protected Control createContents(Composite parent) {
		EnginePreferenceComposite composite = new EnginePreferenceComposite(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout();
		
		layout.marginTop = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		modifiedProperties = composite.getModifiedProperties();
        return composite.getExpandBar();
	}

	public void init(IWorkbench workbench) { }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		ConvertigoPlugin.infoMessageBox("Not implemented!");
		super.performDefaults();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		for (PropertyName property : modifiedProperties.keySet()) {
			ConvertigoPlugin.logDebug("Updating engine property " + property.name() + ": " + modifiedProperties.get(property));
			if (property.getType() == PropertyType.Array) {
				String value = modifiedProperties.get(property);
				StringTokenizer st = new StringTokenizer(value, "\r\n", false);
				String[] propertyAsStringArray = new String[st.countTokens()];
				int i = 0;
				while (st.hasMoreTokens()) {
					String item = st.nextToken();
					propertyAsStringArray[i] = item;
					i++;
				}
				
				EnginePropertiesManager.setPropertyFromStringArray(property, propertyAsStringArray);
			}
			else {
				EnginePropertiesManager.setProperty(property, modifiedProperties.get(property));
			}
		}
		try {
			EnginePropertiesManager.saveProperties();
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to save engine properties!");
		}
		return super.performOk();
	}
}
