/*
 * Copyright (c) 2001-2024 Convertigo SA.
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.events.StudioEvent;
import com.twinsoft.convertigo.engine.events.StudioEventListener;


public class ProjectDeployResultComposite extends Composite {
	Label label = null;
	Link link = null;
	
	public ProjectDeployResultComposite(Composite parent, int style) {
		super(parent, style);
		initialize();
	}

	private void initialize() {
		GridLayout gl = new GridLayout(1, false);
		gl.verticalSpacing = 10;
		setLayout(gl);
		
		label = new Label(this, SWT.NONE);
		label.setText("");

		link = new Link(this, SWT.NONE);
		link.setText("");
		link.addListener (SWT.Selection, new Listener () {
			public void handleEvent(Event event) {
				Engine.theApp.eventManager.dispatchEvent(new StudioEvent("linkOpen", event.text), StudioEventListener.class);
				org.eclipse.swt.program.Program.launch(event.text);
			}
		});
				
		link.setSize(330, 150);
	}

	protected void updateLabelText(String text) {
		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					if (!label.isDisposed()) {
						if (text != null) label.setText(text);
						ProjectDeployResultComposite.this.requestLayout();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	protected void updateLinkText(String text) {
		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					if (!link.isDisposed()) {
						if (text != null) link.setText(text);
						link.setVisible(text != null && !text.isEmpty());
						ProjectDeployResultComposite.this.requestLayout();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
}
