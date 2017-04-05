/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.mobile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMDocument;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.Engine;

public class ApplicationComponentEditor extends EditorPart implements ISelectionChangedListener {

	private ProjectExplorerView projectExplorerView = null;
	private ApplicationComponentEditorInput applicationEditorInput;
	private com.teamdev.jxbrowser.chromium.Browser browser;
	private boolean doOutput = true;
	private Collection<Process> processes = new LinkedList<>();
	
	public ApplicationComponentEditor() {
		projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null) {
			projectExplorerView.addSelectionChangedListener(this);
		}
	}

	
	@Override
	public void dispose() {
		if (projectExplorerView != null) {
			projectExplorerView.removeSelectionChangedListener(this);
		}
		if (browser != null) {
			browser.dispose();
		}
		
		for (Process p: processes) {
			p.destroyForcibly();
			p.destroy();
		}
		
		try {
			new ProcessBuilder("taskkill", "/F", "/IM", "node.exe").start();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		super.dispose();
	}


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
		
		applicationEditorInput = (ApplicationComponentEditorInput)input;
		setPartName(applicationEditorInput.application.getProject().getName() + " [A: " + applicationEditorInput.application.getName()+"]");
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
		createBrowser(parent);
		launchBuilder();
	}

	private void createBrowser(Composite parent) {
		browser = new C8oBrowser(parent, SWT.NONE).getBrowser();
		
		getSite().getWorkbenchWindow().getActivePage().activate(this);
	}

	@Override
	public void setFocus() {
//		if (browser != null && !browser.isDisposed()) {
//			browser.setFocus();
//		}
	}

	public void refreshBrowser() {
//		if (browser != null && !browser.isDisposed()) {
//			browser.refresh();
//		}
	}
	
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Engine.logEngine.info("ok");
		Engine.logEngine.info("" + browser.getRemoteDebuggingURL());
//		browser.executeScript("location.href='http://www.convertigo.com'");
//		browser.executeJavaScript("location.href='http://www.convertigo.com'");
//		if (event.getSource() instanceof ISelectionProvider) {
//			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
//			TreeObject treeObject = (TreeObject) selection.getFirstElement();
//			if (treeObject != null) {
//				if (treeObject instanceof MobileUIComponentTreeObject) {
//					TreeParent treeParent = treeObject.getParent();
//					while (treeParent != null) {
//						if (treeParent instanceof MobilePageComponentTreeObject) {
//							PageComponent page = ((MobilePageComponentTreeObject)treeParent).getObject();
//							if (pageEditorInput.is(page)) {
//								getEditorSite().getPage().bringToTop(this);
//							}
//							break;
//						}
//						treeParent = treeParent.getParent();
//					}
//				}
//				else if (treeObject instanceof MobilePageComponentTreeObject) {
//					PageComponent page = ((MobilePageComponentTreeObject)treeObject).getObject();
//					if (pageEditorInput.is(page)) {
//						getEditorSite().getPage().bringToTop(this);
//					}
//				}
//			}
//		}
	}
	
	private void appendOutput(String msg) {
		if (doOutput) {
			DOMDocument doc = browser.getDocument();
			DOMElement body = doc.findElement(By.tagName("body"));
			body.appendChild(doc.createElement("br"));
			if (StringUtils.isNotEmpty(msg)) {
				body.appendChild(doc.createTextNode(msg));
			}
			browser.executeJavaScript("document.body.scrollTop = document.body.scrollHeight");
		}
	}
	
	private void launchBuilder() {
		browser.loadHTML("<body>loading…</body>");
		Engine.execute(() -> {
			File ionicDir = new File(applicationEditorInput.application.getProject().getDirPath() + "/_private/ionic");
			if (!new File(ionicDir, "node_modules").exists()) {
				try {
					ProcessBuilder pb = new ProcessBuilder("npm.cmd", "install", "--verbose");
					pb.redirectErrorStream(true);
					pb.directory(ionicDir);
					Process p = pb.start();
					processes.add(p);
					BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line;
					while ((line = br.readLine()) != null) {
						appendOutput(line);
					}
					appendOutput("\\o/");
				} catch (Exception e) {
					appendOutput(":( " + e);
				}
			}

			try {
				ProcessBuilder pb = new ProcessBuilder("npm.cmd", "run", "ionic:serve", "--nobrowser");
				pb.redirectErrorStream(true);
				pb.directory(ionicDir);
				Process p = pb.start();
				processes.add(p);
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				Pattern isServe = Pattern.compile(".*?server running: (http.*)");
				while ((line = br.readLine()) != null) {
					appendOutput(line);
					Matcher m = isServe.matcher(line);
					if (m.matches()) {
						doOutput = false;
						browser.loadURL(m.group(1));
					}
				}
				appendOutput("\\o/");
			} catch (Exception e) {
				appendOutput(":( " + e);
			}
			
		});
	}

	public String getDebugUrl() {
		if (browser != null) {
			return browser.getRemoteDebuggingURL();
		}
		return null;
	}
}
