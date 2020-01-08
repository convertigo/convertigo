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

package com.twinsoft.convertigo.eclipse.editors.jscript;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.wst.jsdt.ui.text.JavaScriptTextTools;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IJScriptContainer;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

@SuppressWarnings("restriction")
public class JScriptEditor extends CompilationUnitEditor {
	private IEditorSite eSite;
	private JScriptEditorInput eInput;
	private IJScriptContainer jsContainer;
	private JavaScriptTextTools jstt;
	
	public JScriptEditor() {
		super();
	}

	@Override
	public void dispose() {
		try {
			if (jstt != null) {
				jstt.dispose();
			}
			eInput.getFile().delete(true, null);
		} catch (Exception e) {
		}
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		super.doSave(monitor);
		
		try {
			// Get the jsEditor content and transfer it to the step object
			jsContainer.setExpression(getDocumentProvider().getDocument(super.getEditorInput()).get());
		} catch (Exception e) {
			ConvertigoPlugin.logWarning("Error writing step jscript code '" + eInput.getName() + "' : "+e.getMessage());
		}
		
		// Refresh tree
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null) {
			projectExplorerView.updateDatabaseObject(jsContainer.getDatabaseObject());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	public void doSaveAs() {
		super.doSaveAs();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	public boolean isDirty() {
		return super.isDirty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 */
	public void createPartControl(Composite parent) {
		JavaScriptTextTools jstt = new JavaScriptTextTools(getPreferenceStore());
		SourceViewerConfiguration configuration = new MyJSEditorSourceViewerConfiguration(jstt.getColorManager(), getPreferenceStore(), this, null);
		setSourceViewerConfiguration(configuration);
		try {
			super.init(eSite, eInput);
		} catch (PartInitException e) {
			ConvertigoPlugin.logException(e, "Error inialiazing  Javascript editor'" + eInput.getName() + "'");
		}
		super.createPartControl(parent);
		reload();
		doSave(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		super.setFocus();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		try {
			if (!(input instanceof JScriptEditorInput)) {
				try {
					site.getPage().closeEditor(this, false);
				} catch (Exception e) {
				}
				return;
			}
			setSite(site);
			setInput(input);
			eSite = site;
			eInput = (JScriptEditorInput) input;
			jsContainer = eInput.getJScriptContainer();
			setPartName(jsContainer.getEditorName());
		} catch (Exception e) {
			throw new PartInitException("Unable to create JS editor", e);
		}
	}
	
	public void reload() {
		reload("");
	}
	
	public void reload(String toAppend) {
		IDocument doc = getDocumentProvider().getDocument(getEditorInput());
		doc.set(jsContainer.getExpression() + toAppend);
	}
	
	public DatabaseObject getDatabaseObject() {
		return jsContainer.getDatabaseObject();
	}
}
