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

package com.twinsoft.convertigo.eclipse.wizards.deploy;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;


class ProjectBuildComposite extends Composite {
	private Label label = null;
	
	ProjectBuildComposite(Composite parent, int style) {
		super(parent, style);
		initialize();
	}

	private void initialize() {
		GridLayout gl = new GridLayout(1, false);
		gl.verticalSpacing = 10;
		setLayout(gl);
		
		label = new Label(this, SWT.NONE);
		label.setText("");
	}

	protected void updateLabelText(String text) {
		ConvertigoPlugin.asyncExec(() -> {
			try {
				if (!label.isDisposed()) {
					if (text != null) label.setText(text);
					ProjectBuildComposite.this.requestLayout();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
}
