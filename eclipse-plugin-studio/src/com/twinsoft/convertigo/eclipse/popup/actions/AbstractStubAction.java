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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/AbstractStubAction.java $
 * $Author: nathalieh $
 * $Revision: 33607 $
 * $Date: 2013-02-19 11:07:19 +0100 (Tue, 19 Feb 2013) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class AbstractStubAction extends MyAbstractAction {

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
				TreeObject treeObject = explorerView
						.getFirstSelectedTreeObject();
				if ((treeObject != null) && (treeObject instanceof SequenceTreeObject)) {
					SequenceTreeObject sequenceTreeObject = (SequenceTreeObject) treeObject;
					Sequence sequence = sequenceTreeObject.getObject();
					File stubDir = new File(Engine.PROJECTS_PATH + "/" + sequence.getProject().getName() + "/stubs");
					stubDir.mkdirs();
					File stubFile = new File(stubDir, sequence.getName() + ".xml");
					Document dom = getXML(treeObject);
					writeStub(dom, stubFile);
				} else if ((treeObject != null) && (treeObject instanceof TransactionTreeObject)) {
					TransactionTreeObject transactionTreeObject = (TransactionTreeObject) treeObject;
					Transaction transaction = transactionTreeObject.getObject();
					File stubDir = new File(Engine.PROJECTS_PATH + "/" + transaction.getProject().getName() + "/stubs");
					stubDir.mkdirs();
					File stubFile = new File(stubDir, transaction.getParent().getName() + "." + transaction.getName() + ".xml");
					Document dom = getXML(treeObject);
					writeStub(dom, stubFile);
				}
			}
		} catch (NoSuchElementException e) {
			ConvertigoPlugin.logException(e, "No previous XML file found");
		} catch (Throwable e) {
			ConvertigoPlugin.logException(e,
					"Unable to execute the selected sequence!");
		} finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

	public void writeStub(Document dom, File stubFile) throws IOException {
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
