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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ComboEnum;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyCategory;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class EnginePreferenceComposite extends Composite {

	private ExpandBar bar;
	
	private Map<PropertyName, String> modifiedProperties;
	private List<String> filterList = null;
	
	private Map<PropertyCategory, Composite> containersMap = new HashMap<PropertyCategory, Composite>();
	private Map<PropertyCategory, ExpandItem> expandItemsMap = new HashMap<PropertyCategory, ExpandItem>();

	public EnginePreferenceComposite(Composite parent, int style) {
		super(parent, style);
		createContents();
	}
	
	public EnginePreferenceComposite(Composite parent, int style, List<String> filterList) {
		super(parent, style);
		this.filterList = filterList;
		createContents();
	}
	
	protected void createContents() {
		modifiedProperties = new HashMap<PropertyName, String>();
		
		boolean all = (filterList == null);
		
		if (!all) {
			GridLayout mainLayout = new GridLayout(1, true);
			mainLayout.marginTop = 0;
			
			setLayout(mainLayout);
		}
		
		bar = new ExpandBar(this, SWT.VERTICAL);
		
		GridData gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		bar.setLayoutData(gridData);
		
		for (PropertyCategory propertyCategory : PropertyCategory.getSortedValues()) {
			if (propertyCategory.isVisible()) {		
				Composite container = new Composite(bar, SWT.NONE);
				
				GridLayout layout = new GridLayout(1, true);
				layout.marginLeft = layout.marginTop = layout.marginRight = layout.marginBottom = 2;
				layout.verticalSpacing = 4;
				container.setLayout(layout);
				
				if (all || filterList.contains(propertyCategory.getDisplayName())) {
					ExpandItem item = new ExpandItem(bar, SWT.NONE);
					item.setText(propertyCategory.getDisplayName());
					item.setControl(container);
		
					containersMap.put(propertyCategory, container);
					expandItemsMap.put(propertyCategory, item);
				} 
			}
		}
        
		boolean toggleLineBackground = true;
		for (final PropertyName property : PropertyName.values()) {
			if (property.isVisible() && property.getCategory().isVisible()) {
				toggleLineBackground = !toggleLineBackground;

				Composite container = containersMap.get(property.getCategory());

				if (container != null) {
					Composite line = new Composite(container, SWT.NONE);
					GridLayout layout = new GridLayout(2, false);
					layout.marginLeft = layout.marginTop = layout.marginRight = layout.marginBottom = 2;
					layout.verticalSpacing = 0;
					line.setLayout(layout);
					GridData data = new GridData(GridData.FILL_HORIZONTAL);
					line.setLayoutData(data);
	
					if (toggleLineBackground) {
						line.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
					}
	
					Label propertyLabel = new Label(line, SWT.WRAP);
					data = new GridData(240, SWT.DEFAULT);
					data.verticalAlignment = SWT.TOP;
					propertyLabel.setText(property.getDescription());
					propertyLabel.setLayoutData(data);
					propertyLabel.setBackground(line.getBackground());
	
					switch (property.getType()) {
						case Text: {
							final Text propertyEditor = new Text(line, SWT.BORDER);
							data = new GridData(GridData.FILL_HORIZONTAL);
							data.verticalAlignment = SWT.TOP;
							propertyEditor.setLayoutData(data);
		
							String value = EnginePropertiesManager.getProperty(property);
							String originalValue = EnginePropertiesManager.getOriginalProperty(property);
							propertyEditor.setText(originalValue);
							if (!originalValue.equals(value)) propertyEditor.setToolTipText(value);
		
							propertyEditor.addModifyListener(new ModifyListener() {
								public void modifyText(ModifyEvent e) {
									modifiedProperties.put(property, propertyEditor.getText());
								}
							});
							break;
						}
		
						case PasswordHash:
						case PasswordPlain: {
							final Text propertyEditor = new Text(line, SWT.BORDER);
							data = new GridData(GridData.FILL_HORIZONTAL);
							data.verticalAlignment = SWT.TOP;
							propertyEditor.setLayoutData(data);
		
							String value = EnginePropertiesManager.getProperty(property);
							propertyEditor.setText(value);
							propertyEditor.setEchoChar('*');
		
							propertyEditor.addModifyListener(new ModifyListener() {
								public void modifyText(ModifyEvent e) {
									modifiedProperties.put(property, "" + propertyEditor.getText());
								}
							});
							break;
						}
		
						case Boolean: {
							final Button propertyEditor = new Button(line, SWT.CHECK);
							propertyEditor.setBackground(line.getBackground());
		
							data = new GridData();
							data.verticalAlignment = SWT.TOP;
							propertyEditor.setLayoutData(data);
		
							boolean value = Boolean.parseBoolean(EnginePropertiesManager.getProperty(property));
							propertyEditor.setSelection(value);
		
							propertyEditor.addSelectionListener(new SelectionListener() {
								public void widgetSelected(SelectionEvent e) {
									modifiedProperties.put(property, "" + propertyEditor.getSelection());
								}
		
								public void widgetDefaultSelected(SelectionEvent e) {
									modifiedProperties.put(property, "" + propertyEditor.getSelection());
								}
							});
		
							break;
						}
		
						case Combo: {
							final Combo propertyEditor = new Combo(line, SWT.SIMPLE | SWT.DROP_DOWN | SWT.READ_ONLY);
		
							data = new GridData(GridData.FILL_HORIZONTAL);
							data.verticalAlignment = SWT.TOP;
							propertyEditor.setLayoutData(data);
		
							String value = EnginePropertiesManager.getProperty(property);
							String comboDisplay, comboValue;
							int selectedIndex = 0;
							int i = 0;
							for (ComboEnum comboItem : property.getCombo()) {
								comboDisplay = comboItem.getDisplay();
								if (comboDisplay != null) {
									comboValue = comboItem.getValue();
									propertyEditor.add(comboDisplay);
									propertyEditor.setData(comboDisplay, comboValue);
									if (value.equals(comboValue)) {
										selectedIndex = i;
									}
									i++;
								}
							}
							propertyEditor.select(selectedIndex);
		
							propertyEditor.addModifyListener(new ModifyListener() {
								public void modifyText(ModifyEvent e) {
									modifiedProperties.put(property, "" + propertyEditor.getData(propertyEditor.getText()));
								}
							});
		
							break;
						}
		
						case Array: {
							final Text propertyEditor = new Text(line, SWT.BORDER | SWT.MULTI);
		
							data = new GridData(GridData.FILL_HORIZONTAL);
							data.verticalAlignment = SWT.TOP;
							propertyEditor.setLayoutData(data);
		
							String[] originalValue = EnginePropertiesManager.getOriginalPropertyAsStringArray(property);
		
							for (String item : originalValue) {
								propertyEditor.append(item + "\r\n");
							}
		
							String v1 = EnginePropertiesManager.getProperty(property);
							String v2 = EnginePropertiesManager.getOriginalProperty(property);
							if (!v1.equals(v2)) propertyEditor.setToolTipText(v1);
		
							propertyEditor.addModifyListener(new ModifyListener() {
								public void modifyText(ModifyEvent e) {
									modifiedProperties.put(property, propertyEditor.getText());
								}
							});
		
							break;
						}
					}
				}
			}
		}
        
		for (PropertyCategory propertyCategory : PropertyCategory.getSortedValues()) {
			if (propertyCategory.isVisible()) {
				Composite container = containersMap.get(propertyCategory);
				if (container != null){
					ExpandItem item = expandItemsMap.get(propertyCategory);
					item.setExpanded(!all);
					item.setHeight(container.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
				}
			}
		} 
	}
	
	public ExpandBar getExpandBar(){
		return bar;
	}
	
	public Map<PropertyName, String> getModifiedProperties(){
		return modifiedProperties;
	}
}
