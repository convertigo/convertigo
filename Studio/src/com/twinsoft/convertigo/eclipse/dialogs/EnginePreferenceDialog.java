/*
 * Copyright (c) 2001-2015 Convertigo SA.
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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyType;

public class EnginePreferenceDialog extends Dialog {

	private Map<PropertyName, String> modifiedProperties;
	private EnginePreferenceComposite engineComposite;
	private int nWidth = 550;
	private int nHeight = 600;
	
	public EnginePreferenceDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	public void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Engine Log settings");
		newShell.setSize(nWidth, nHeight);

		int nLeft = 0;
		int nTop = 0;

		Display display = newShell.getDisplay();

		Point pt = display.getCursorLocation();
		Monitor[] monitors = display.getMonitors();

		for (int i = 0; i < monitors.length; i++) {
			if (monitors[i].getBounds().contains(pt)) {
				Rectangle rect = monitors[i].getClientArea();

				if (rect.x < 0)
					nLeft = ((rect.width - nWidth) / 2) + rect.x;
				else
					nLeft = (rect.width - nWidth) / 2;

				if (rect.y < 0)
					nTop = ((rect.height - nHeight) / 2) + rect.y;
				else
					nTop = (rect.height - nHeight) / 2;

				break;
			}
		}

		newShell.setBounds(nLeft, nTop, nWidth, nHeight);
		
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		List<String> filterList = new ArrayList<String>();
		filterList.add("Logs");
		
		engineComposite = new EnginePreferenceComposite(parent, SWT.NONE, filterList);
		engineComposite.getExpandBar().setLayoutData(new GridData(nWidth - 30, nHeight - 100));
		
		modifiedProperties = engineComposite.getModifiedProperties();
		
        return engineComposite.getExpandBar();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {	
		/* APPLY ACTION */
		Button buttonApply = createButton(parent, IDialogConstants.PROCEED_ID, "Apply", true);
		buttonApply.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				applyProceed();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		buttonApply.setEnabled(true);
	}
	
	private void applyProceed() {		
		if (modifiedProperties != null) {
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
		}
		
		this.close();
	}
}
