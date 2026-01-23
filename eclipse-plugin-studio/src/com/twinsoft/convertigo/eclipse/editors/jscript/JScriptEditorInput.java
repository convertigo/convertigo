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

package com.twinsoft.convertigo.eclipse.editors.jscript;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileInPlaceEditorInput;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IJScriptContainer;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentFunctionTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.ConvertigoTypeScriptDefinition;
import com.twinsoft.convertigo.engine.util.CopilotHelper;

public class JScriptEditorInput extends FileInPlaceEditorInput implements IPropertyListener {
	
	static private IFile makeFile(IJScriptContainer jsContainer, IProject project) {
		String fullname = jsContainer.getFullName();
		if (jsContainer instanceof DesignDocumentFunctionTreeObject) {
			IFile jsconfig = project.getFile("_private/editor/" + fullname + "/jsconfig.json");
			String conf = "{\"compilerOptions\": {\"module\": \"es5\", \"target\": \"es5\"}, \"include\": [\"*\"]}";
			SwtUtils.fillFile(jsconfig, conf);
			SwtUtils.fillFile(project.getFile("_private/editor/" + fullname + "/couchdb.d.ts"),
					"declare function emit(key, value)\n"
					+ "declare function sum(values)"
					+ "declare function log(txt)\n");
		} else {
			DatabaseObject dbo = jsContainer.getDatabaseObject();
			if (dbo instanceof Step) {
				dbo = ((Step) dbo).getSequence();
			}
			if (dbo instanceof IVariableContainer) {
				IVariableContainer vc = (IVariableContainer) dbo;
				StringBuilder sb = new StringBuilder();
				for (Variable v: vc.getVariables()) {
					sb.append("declare var ").append(v.getName()).append(v.isMultiValued() ? ": Array<string>" : ": string\n");
				}
				SwtUtils.fillFile(project.getFile("_private/editor/" + fullname + "/variables.d.ts"), sb.toString());
			}
			IFile jsconfig = project.getFile("_private/editor/" + fullname + "/jsconfig.json");
			String conf = "{\"compilerOptions\": {\"module\": \"es6\", \"target\": \"es6\"},\n" + 
					"  \"include\": [\"" + ConvertigoTypeScriptDefinition.getDeclarationFile().getAbsolutePath().replace('\\', '/') + "\", \"*\"]}";
			SwtUtils.fillFile(jsconfig, conf);
		}
		IFile file = project.getFile("_private/editor/" + fullname + "/code.js");
		var expression = jsContainer.getExpression();
		expression = CopilotHelper.addInstruction(jsContainer.getDatabaseObject(), expression);
		SwtUtils.fillFile(file, expression);
		
		return file;
	}
	
	static public IEditorPart openJScriptEditor(DatabaseObjectTreeObject dboTree, IJScriptContainer jsContainer) throws PartInitException {
		IWorkbenchPage activePage = PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage();
		if (activePage == null) {
			return null;
		}
		JScriptEditorInput jsInput = new JScriptEditorInput(dboTree, jsContainer);
		IEditorPart editor = activePage.openEditor(jsInput,	"org.eclipse.ui.genericeditor.GenericEditor");
		editor.addPropertyListener(jsInput);
		return editor;
	}
	
	static public IEditorPart openJScriptEditor(DatabaseObjectTreeObject dboTree, String propertyName) throws PartInitException {
		return openJScriptEditor(dboTree, new IJScriptContainer() {
			
			@Override
			public void setExpression(String expression) {
				dboTree.setPropertyValue(propertyName, expression);
			}
			
			@Override
			public DatabaseObject getDatabaseObject() {
				return dboTree.getObject();
			}
			
			@Override
			public String getName() {
				return dboTree.getName();
			}
			
			@Override
			public String getExpression() {
				Object o = dboTree.getPropertyValue(propertyName);
				return o instanceof String ? (String) o : "" + o;
			}
		});
	}
	
	private DatabaseObjectTreeObject dboTree;
	private IJScriptContainer jsContainer;
		
	private JScriptEditorInput(DatabaseObjectTreeObject dboTree, IJScriptContainer jsContainer) {
		super(makeFile(jsContainer, dboTree.getProjectTreeObject().getIProject()));
		this.dboTree = dboTree;
		this.jsContainer = jsContainer;
	}

	public IJScriptContainer getJScriptContainer() {
		return jsContainer;
	}
	
	public DatabaseObject getDatabaseObject() {
		return jsContainer.getDatabaseObject();
	}
	
	public Project getProject() {
		DatabaseObject dbo = jsContainer.getDatabaseObject();
		return dbo != null ? dbo.getProject() : null;
	}

	@Override
	public String getName() {
		return jsContainer.getName() + ".js";
	}

	@Override
	public String getToolTipText() {
		return jsContainer.getFullName();
	}
	
	public boolean is(DatabaseObject dbo) {
		DatabaseObject d = this.jsContainer.getDatabaseObject();
		do {
			if (dbo.equals(d)) {
				return true;
			}
			d = d.getParent();
		} while (!(d instanceof Project));
		return false;
	}
	
	public void reload() {
		SwtUtils.fillFile(getFile(), jsContainer.getExpression());
	}

	@Override
	public void propertyChanged(Object source, int propId) {
		if (propId == IEditorPart.PROP_DIRTY && !((IEditorPart) source).isDirty()) {
			try (InputStream is = getFile().getContents()) {
				var expression = IOUtils.toString(is, "UTF-8");
				expression = CopilotHelper.removeInstruction(expression);
				jsContainer.setExpression(expression);
				dboTree.hasBeenModified(true);
			} catch (Exception e) {
				Engine.logStudio.error("Failed to save " + jsContainer.getEditorName(), e);
			}
		}
	}
}
