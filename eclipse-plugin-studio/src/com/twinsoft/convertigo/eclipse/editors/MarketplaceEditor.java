/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.teamdev.jxbrowser.dom.Element;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.actions.OpenTutorialView;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.engine.Engine;

public class MarketplaceEditor extends EditorPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.editors.MarketplaceEditor";
	private static final String STARTUP_URL = "https://beta.convertigo.net/convertigo/projects/marketplace/DisplayObjects/mobile/";

	private C8oBrowser browser = null;
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}
	
	@Override
	public void dispose() {
		if (browser != null) {
			browser.dispose();
		}
		super.dispose();
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		SwtUtils.refreshTheme();
		
		parent.setLayout(new GridLayout(1, true));
		ToolBar tb = new ToolBar(parent, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
		tb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		tb.setVisible(false);
		
		browser = new C8oBrowser(parent, SWT.NONE);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(true);
		Engine.logStudio.debug("Marketplace debug : "+ browser.getDebugUrl()); 
		browser.getDebugUrl();
		browser.onClick(ev -> {
			try {
				Element elt = (Element) ev.target().get();
				while (!elt.nodeName().equalsIgnoreCase("a")) {
					elt = (Element) elt.parent().get();
				}
				String href = elt.attributes().get("href");
				if (href.equals("#opentutorialview")) {
					ConvertigoPlugin.asyncExec(() -> {
						new OpenTutorialView().run(null);
					});
					ev.preventDefault();
					return true;
				} else if (href.startsWith("#") || elt.attributes().get("id").startsWith("weglot")) {
					return true;
				}
			} catch (Exception e) {
			}
			return false;
		});
		
		ToolItem ti = new ToolItem(tb, SWT.NONE);
		ti.setImage(ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/statement.png"));
		ti.setText("View with your external browser");
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch(browser.getURL().replaceFirst("\\?user=.*", ""));
			}
			
		});
		String url = STARTUP_URL;

		browser.setUrl(url);
	}

	@Override
	public void setFocus() {
	}

}
