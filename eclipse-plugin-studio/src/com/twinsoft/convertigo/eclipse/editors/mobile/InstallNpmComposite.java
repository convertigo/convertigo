/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.mobile;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

import com.twinsoft.convertigo.engine.Engine;

public class InstallNpmComposite extends Composite {

	public InstallNpmComposite(Composite parent, int style) {
		super(parent, style);
		
//		setBackground(getDisplay().getSystemColor(SWT.COLOR_RED));
		setLayout(new GridLayout(1, true));
		
		Link link = new Link(this, SWT.WRAP);
		
		link.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		if (Engine.isWindows()) {
			link.setText(
				"NodeJS and NPM (≥ 5) are mandatory to use the Mobile Builder.\n" +
				"To get both, please use the 8.15.0 NodeJS installer from <a href=\"https://nodejs.org/dist/latest-v8.x/node-v8.15.0-x64.msi\">https://nodejs.org/dist/latest-v8.x/node-v8.15.0-x64.msi</a>.\n" +
				"Install, and then close and re-open this editor.");					
		} else {
			link.setText(
				"NodeJS and NPM (≥ 5) are mandatory to use the Mobile Builder.\n" +
				"To get both, please use the LTS NodeJS installer from <a href=\"https://nodejs.org\">https://nodejs.org</a>.\n" +
				"Install, and then close and re-open this editor.");	
		}
		
		
		
		link.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.program.Program.launch(e.text);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {	
			}
			
		});
		
		FontData[] fd = link.getFont().getFontData();
		fd[0].setHeight(18);
		link.setFont(new Font(getDisplay(), fd));
	}

}
