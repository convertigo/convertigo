/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.util.XMLUtils;

abstract class AbstractStubAction extends MyAbstractAction {

	public AbstractStubAction() {
		super();
	}

	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
 		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
				
				DatabaseObject dbo = null;
				if ((treeObject != null) && (treeObject instanceof SequenceTreeObject)) {
					dbo = ((SequenceTreeObject) treeObject).getObject();
				} else if ((treeObject != null) && (treeObject instanceof TransactionTreeObject)) {
					dbo = ((TransactionTreeObject) treeObject).getObject();
				}
				
				if (dbo != null) {
					File stubDir = new File(dbo.getProject().getDirPath() + "/stubs");
					String defaultStubFileName = ((RequestableObject)dbo).getDefaultStubFileName();
					File defaultStubFile = new File(stubDir, defaultStubFileName);
					
					FileDialog fileDialog = new FileDialog(shell, SWT.PRIMARY_MODAL | SWT.SAVE);
					fileDialog.setText("Save Stub");
					fileDialog.setFilterExtensions(new String[]{"*.xml"});
					fileDialog.setFilterNames(new String[]{"Convertigo stubs"});
					fileDialog.setFilterPath(stubDir.getCanonicalPath());
					fileDialog.setFileName(defaultStubFile.getName());
	
					String filePath = fileDialog.open();
					if (filePath != null) {
						File stubFile = new File(filePath);
						if (stubFile.exists()) {
							if (ConvertigoPlugin.questionMessageBox(shell, "File already exists. Do you want to overwrite?") == SWT.YES) {
								if (!stubFile.delete()) {
									ConvertigoPlugin.warningMessageBox("Error when deleting the file " + stubFile.getName() + "! Please verify access rights!");
									return;
								}
							} else {
								return;
							}
						}
	
						if (Pattern.matches(".+(\\.xml)", stubFile.getName())) {
							Document dom = getXML(treeObject);
							stubDir.mkdirs();
							writeStub(dom, stubFile);
							if (!defaultStubFile.exists() && !defaultStubFile.equals(stubFile)) {
								writeStub(dom, defaultStubFile);
							}
						}
						else {
							Toolkit.getDefaultToolkit().beep();
							ConvertigoPlugin.logWarning("Wrong file extension!");
						}
					}
				}
			}
		} catch (NoSuchElementException e) {
			ConvertigoPlugin.logException(e, "No previous XML found");
		} catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to save stub!");
		} finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

	static private void writeStub(Document dom, File stubFile) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(stubFile);
			XMLUtils.prettyPrintDOMWithEncoding(dom, "UTF-8", fos);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	public abstract Document getXML(TreeObject treeObject) throws Exception;
}
