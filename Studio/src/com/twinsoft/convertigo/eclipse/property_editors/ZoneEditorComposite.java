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

package com.twinsoft.convertigo.eclipse.property_editors;

import com.twinsoft.convertigo.beans.common.XMLRectangle;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;

public class ZoneEditorComposite extends AbstractDialogComposite {

	private Label labelTop = null;
	private Text textTop = null;
	private Label labelLeft = null;
	private Text textLeft = null;
	private Label labelWidth = null;
	private Text textWidth = null;
	private Label labelHeight = null;
	private Text textHeight = null;
	
	public ZoneEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		initialize();
		
		XMLRectangle zone = (XMLRectangle)cellEditor.databaseObjectTreeObject.getPropertyValue(cellEditor.propertyDescriptor.getId());
        textTop.setText(new Integer(zone.y).toString());
        textLeft.setText(new Integer(zone.x).toString());
        textWidth.setText(new Integer(zone.width).toString());
        textHeight.setText(new Integer(zone.height).toString());
	}

	private void initialize() {
		labelTop = new Label(this, SWT.NONE);
		labelTop.setText("Top");
		textTop = new Text(this, SWT.BORDER);
		textTop.addFocusListener(new org.eclipse.swt.events.FocusAdapter() {
			public void focusGained(org.eclipse.swt.events.FocusEvent e) {
				textTop.selectAll();
			}
		});
		labelLeft = new Label(this, SWT.NONE);
		labelLeft.setText("Left");
		textLeft = new Text(this, SWT.BORDER);
		textLeft.addFocusListener(new org.eclipse.swt.events.FocusAdapter() {
			public void focusGained(org.eclipse.swt.events.FocusEvent e) {
				textLeft.selectAll();
			}
		});
		labelWidth = new Label(this, SWT.NONE);
		labelWidth.setText("Width");
		textWidth = new Text(this, SWT.BORDER);
		textWidth.addFocusListener(new org.eclipse.swt.events.FocusAdapter() {
			public void focusGained(org.eclipse.swt.events.FocusEvent e) {
				textWidth.selectAll();
			}
		});
		labelHeight = new Label(this, SWT.NONE);
		labelHeight.setText("Height");
		textHeight = new Text(this, SWT.BORDER);
		textHeight.addFocusListener(new org.eclipse.swt.events.FocusAdapter() {
			public void focusGained(org.eclipse.swt.events.FocusEvent e) {
				textHeight.selectAll();
			}
		});
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);
		setSize(new org.eclipse.swt.graphics.Point(128,106));
	}
	public Object getValue() {
        int top, left, width, height;
        XMLRectangle zone = null;
        
        try {
            top = Integer.parseInt(textTop.getText());
            left = Integer.parseInt(textLeft.getText());
            width = Integer.parseInt(textWidth.getText());
            height = Integer.parseInt(textHeight.getText());
            zone = new XMLRectangle(left, top, width, height);
        }
        catch (Exception e) {;}
        
		return zone;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
