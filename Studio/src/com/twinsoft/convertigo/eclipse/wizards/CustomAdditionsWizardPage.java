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

package com.twinsoft.convertigo.eclipse.wizards;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class CustomAdditionsWizardPage extends WizardPage {

    protected Color FOREGROUND_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
    protected Color BACKGROUND_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
    protected Color FOREGROUND_SELECTED_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
    protected Color BACKGROUND_SELECTED_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);    

    protected Cursor handCursor;
    protected CLabel currentSelectedItem = null;
    
    private Map<String, Image> icons = new HashMap<String, Image>(256);
    
    public CustomAdditionsWizardPage(String pageName) {
		super(pageName);
	}

	public CustomAdditionsWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	
	@Override
	public void dispose() {
		super.dispose();
		
    	for (Image image : icons.values()) {
            if (image != null)
            	image.dispose();
        }
    	icons.clear();
		
		if (handCursor != null)
			handCursor.dispose();
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		
		Composite compositeObjects = new Composite(container, SWT.NONE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		//gridData.horizontalSpan = 2;
		gridData.verticalAlignment = GridData.BEGINNING;
		gridData.grabExcessVerticalSpace = true;
		compositeObjects.setLayoutData(gridData);
		compositeObjects.setLayout(new FillLayout());
		
		ExpandBar bar = new ExpandBar(compositeObjects, SWT.V_SCROLL);
		
		GridLayout gridLayout = new GridLayout(3, true);
		gridLayout.verticalSpacing = 10;
		gridLayout.horizontalSpacing = 10;
		
		handCursor = new Cursor(Display.getDefault(), SWT.CURSOR_HAND);
		
		Document doc = null;
		InputStream is = getClass().getResourceAsStream("/custom_additions.xml");
		try {
			doc = XMLUtils.parseDOM(is);
			//System.out.println(XMLUtils.prettyPrintDOM(doc));
			
			Composite composite;
			ExpandItem exitem;
			GridData gdData;
			CLabel label;
			Element category, item;
			NodeList categoryList, itemList;
			String categoryType, iconPath;
			
			categoryList= doc.getElementsByTagName("category");
			for (int i=0; i<categoryList.getLength(); i++) {
				category = (Element)categoryList.item(i);
				categoryType = category.getAttribute("type");
				iconPath = category.getAttribute("icon");
				
				composite = new Composite (bar, SWT.NONE);
				composite.setLayout(gridLayout);
				composite.setBackground(BACKGROUND_COLOR);
				
				itemList = category.getElementsByTagName(categoryType);
				for (int j=0; j<itemList.getLength(); j++) {
					item = (Element)itemList.item(j);
					
					label = new CLabel(composite, SWT.NONE);
			        label.setImage(getImage(categoryType, iconPath));
			        label.setText(item.getAttribute("name"));
			        label.setAlignment(SWT.CENTER);
			        label.setToolTipText(item.getAttribute("tooltip"));
			        label.setCursor(handCursor);

			        gdData = new GridData();
			        gdData.horizontalAlignment = SWT.BEGINNING;
			        gdData.grabExcessHorizontalSpace = true;
			        gdData.verticalAlignment = GridData.BEGINNING;
			        gdData.grabExcessVerticalSpace = true;
					label.setLayoutData(gdData);
					
			        // We select by default the first item
			        if (currentSelectedItem == null) {
			        	currentSelectedItem = label;
			        	currentSelectedItem.setForeground(FOREGROUND_SELECTED_COLOR);
			        	currentSelectedItem.setBackground(BACKGROUND_SELECTED_COLOR);
			        }
					
			        label.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseUp(MouseEvent e) {
							if (currentSelectedItem == (CLabel)e.getSource()) return;

							if (currentSelectedItem != null) {
			                    currentSelectedItem.setForeground(FOREGROUND_COLOR);
			                    currentSelectedItem.setBackground(BACKGROUND_COLOR);
			                }
							
							currentSelectedItem = (CLabel)e.getSource();

							ConvertigoPlugin.logDebug("currentSelectedItem: '" + currentSelectedItem.getText() + "'.");
							
				            currentSelectedItem.setForeground(FOREGROUND_SELECTED_COLOR);
			                currentSelectedItem.setBackground(BACKGROUND_SELECTED_COLOR);
						}
			        });
				}

				exitem = new ExpandItem (bar, SWT.NONE, i);
				exitem.setText(category.getAttribute("description"));
				exitem.setHeight(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
				exitem.setControl(composite);
				exitem.setExpanded(true);
			}
			
		} catch (Exception e) {
			ConvertigoPlugin.logWarning(e.getMessage());
		}
		
		bar.setSpacing(8);
		
		setControl(container);
	}

	@Override
	public boolean isPageComplete() {
		return (getSelectedFileName() != null);
	}
	
	public String getSelectedFileName() {
		return currentSelectedItem.getText();
	}
	
	private Image getImage(String categoryType, String iconPath) {
		Image image = (Image) icons.get(categoryType);
		if (image == null) {
			Device device = Display.getCurrent();
			InputStream inputStream = ConvertigoPlugin.class.getResourceAsStream(iconPath);
			if (inputStream != null)
				image = new Image(device, inputStream);
			if (image != null)
				icons.put(categoryType, image);
		}
		return image;
	}

}
