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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IJScriptContainer;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JScriptEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.JsonData;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject.FunctionObject;

public class DesignDocumentFunctionTreeObject extends TreeParent implements IEditableTreeObject, IJScriptContainer, IDesignTreeObject, IActionFilter  {

	DesignDocumentFunctionTreeObject(Viewer viewer, Object object) {
		super(viewer, object);
	}

	@Override
	public TreeParent getTreeObjectOwner() {
		return getParent().getParent().getParent();
	}
	
	@Override
	public IDesignTreeObject getParentDesignTreeObject() {
		return (IDesignTreeObject) getParent();
	}
	
	@Override
	public FunctionObject getObject() {
		return (FunctionObject)super.getObject();
	}

	@Override
	public DatabaseObject getDatabaseObject() {
		TreeObject treeObj = this;
		while (treeObj != null && !(treeObj instanceof DesignDocumentTreeObject)) {
			treeObj = treeObj.getParent();
		}
		if (treeObj != null && treeObj instanceof DesignDocumentTreeObject) {
			return (DatabaseObject) treeObj.getObject();
		}
		return null;
	}
	
	@Override
	public String getFullName() {
		DatabaseObject dbo = getDatabaseObject();
		return dbo != null ? (dbo.getQName() + "." + getParent().getName() + "." + getName()) : getName();
	}
	
	@Override
	public void hasBeenModified() {
		IDesignTreeObject ddvto = getParentDesignTreeObject();
		if (ddvto != null) {
			ddvto.hasBeenModified();
		}
	}
	
	@Override
	public String getExpression() {
		FunctionObject fun = getObject();
		String function = fun.getStringObject().replaceFirst("function", "function " + fun.getName());
		return function;
	}

	@Override
	public void setExpression(String function) {
		function = function.replaceFirst("function [\\S]*? ", "function ");
		getObject().setStringObject(function);
		hasBeenModified();
	}
	
	@Override
	public void launchEditor(String editorType) {	
		try {
			JScriptEditorInput.openJScriptEditor(getDesignDocumentTreeObject(), this); 
		} catch (PartInitException e) {
			ConvertigoPlugin.logException(e, "Error while loading the document editor '" + getName() + "'");
		}
	}
	
	public void closeAllEditors(boolean save) {
		DesignDocument ddoc = getDesignDocumentTreeObject().getObject();
		
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (activePage != null) {
			IEditorReference[] editorRefs = activePage.getEditorReferences();
			for (int i = 0; i < editorRefs.length; i++) {
				IEditorReference editorRef = (IEditorReference) editorRefs[i];
				try {
					IEditorInput editorInput = editorRef.getEditorInput();
					if (editorInput != null && editorInput instanceof JScriptEditorInput) {
						if (((JScriptEditorInput)editorInput).is(ddoc)) {
							activePage.closeEditor(editorRef.getEditor(false), save);
						}
					}
				} catch(Exception e) {
					
				}
			}
		}
	}

	@Override
	public IDesignTreeObject add(Object object, boolean bChangeName) {
		if (object instanceof JsonData) {
			JsonData jsonData = (JsonData)object;
			Class<? extends TreeParent> c = jsonData.getOwnerClass();
			if (DesignDocumentFunctionTreeObject.class.isAssignableFrom(c)) {
				return getParentDesignTreeObject().add(object, bChangeName);
			}
		}
		else if (object instanceof DesignDocumentFunctionTreeObject) {
			return getParentDesignTreeObject().add(object, bChangeName);
		}
		return null;
	}

	@Override
	public void remove(Object object) {
		if (object.equals(this)) {
			if (parent != null) {
				getParentDesignTreeObject().remove(object);
			}
		}
	}
	
	public boolean rename(String newName, Boolean bDialog) {
		return false;
	}
	
	@Override
	public Element toXml(org.w3c.dom.Document document) {
		Element element = document.createElement("function");
		element.setAttribute("classname", getClass().getName());
		JSONObject jsondata = new JSONObject();
		try {
			jsondata.put("name", getObject().getName());
			jsondata.put("value", getObject().getStringObject());
		} catch (JSONException e) {}
        CDATASection cDATASection = document.createCDATASection(jsondata.toString());
        element.appendChild(cDATASection);
		return element;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionFilter#testAttribute(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("canPaste")) {
			boolean canPaste = ((ConvertigoPlugin.clipboardManagerSystem.isCopy) || (ConvertigoPlugin.clipboardManagerSystem.isCut));
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(canPaste));
		}
		return super.testAttribute(target, name, value);
	}

	@Override
	public boolean canPaste(Object object) {
		if (object != null) {
			if (object.getClass().equals(getClass()))
				return true;
		}
		return false;
	}

	@Override
	public String getEditorName() {
		String name = parent.getName() + " " + getName(); 
		if (parent instanceof DesignDocumentViewTreeObject) {
			return parent.parent.getName() + " " + name;
		}
		return name;
	}

	
}
