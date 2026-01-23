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

package com.twinsoft.convertigo.eclipse.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.teamdev.jxbrowser.dom.Element;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.actions.OpenTutorialView;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.util.URLUtils;

public class StartupEditor extends EditorPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.editors.StartupEditor";
	private static final String STARTUP_URL = "https://www.convertigo.com/convertigo-startup-page-8-3/";

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
		StartupInput si = (StartupInput) getEditorInput();
		
		parent.setLayout(new GridLayout(1, true));
		ToolBar tb = new ToolBar(parent, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
		tb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		tb.setVisible(false);
		
		browser = new C8oBrowser(parent, SWT.NONE);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(true);
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
		ti = new ToolItem(tb, SWT.SEPARATOR);
		ToolItem[] tic = {null};
		if (si.autoClose) {
			tic[0] = new ToolItem(tb, SWT.CHECK);
			tic[0].setImage(ViewImageProvider.getImageFromCache("/com/twinsoft/convertigo/eclipse/editors/images/stop.png"));
			tic[0].setText("Auto close");
			tic[0].addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					if (tic[0].getSelection()) {
						ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS, "true");
						boolean s[] = {true};
						int remains[] = {10};
						tic[0].setText("Auto close in " + remains[0] + "s");
						Engine.execute(() -> {
							try {
								while (--remains[0] >= 0 && s[0]) {
									Thread.sleep(1000);
									tic[0].getDisplay().syncExec(() -> {
										if (!tic[0].isDisposed()) {
											if (s[0] = tic[0].getSelection()) {
												tic[0].setText("Auto close in " + remains[0] + "s");
											}
										}
									});
								}
								if (s[0]) {
									tic[0].getDisplay().asyncExec(() -> {
										PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(StartupEditor.this, false);
									});
								}
							} catch (Exception e2) {
							}
						});
					} else {
						tic[0].setText("Auto close");
						ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS, "false");
					}
				}
			});
		}
		String url = STARTUP_URL;
		url += "?" + URLUtils.encodePart("user", si.user);
		url += "&" + URLUtils.encodePart("site", si.site);
		url += "&" + URLUtils.encodePart("version", ProductVersion.fullProductVersion);

		browser.addProgressListener(new ProgressAdapter() {
			
			@Override
			public void completed(ProgressEvent event) {
				if (!tb.isDisposed()) {
					tb.getDisplay().asyncExec(() -> {
						tb.setVisible(true);
						if (ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS).equalsIgnoreCase("true")) {
							if (tic[0] != null && !tic[0].isDisposed()) {
								tic[0].setSelection(true);
								tic[0].notifyListeners(SWT.Selection, new Event());
							}
						}
					});
				}
			}
			
		});
		browser.setUrl(url);
	}

	@Override
	public void setFocus() {
	}

	public static IEditorInput makeInput(String user, String site, boolean autoClose) {
		return new StartupInput(user, site, autoClose);
	}
	
	private static class StartupInput implements IEditorInput {
		private String user;
		private String site;
		private boolean autoClose;
		
		private StartupInput(String user, String site, boolean autoClose) {
			this.user = user;
			this.site = site;
			this.autoClose = autoClose;
		}
		
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}
		
		@Override
		public String getToolTipText() {
			return "Convertigo Startup";
		}
		
		@Override
		public IPersistableElement getPersistable() {
			return null;
		}
		
		@Override
		public String getName() {
			return user + "@" + site;
		}
		
		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}
		
		@Override
		public boolean exists() {
			return false;
		}
	}

}
