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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTreeFunctionEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.JsonData;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject.FunctionObject;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class DesignDocumentFunctionTreeObject extends TreeParent implements IEditableTreeObject, IFunctionTreeObject, IDesignTreeObject, IActionFilter  {

	public DesignDocumentFunctionTreeObject(Viewer viewer, Object object) {
		super(viewer, object);
	}

	public TreeParent getTreeObjectOwner() {
		return getParent().getParent().getParent();
	}
	
	@Override
	public FunctionObject getObject() {
		return (FunctionObject)super.getObject();
	}

	protected void hasBeenModified() {
		DesignDocumentViewTreeObject ddvto = (DesignDocumentViewTreeObject) getParent();
		if (ddvto != null) {
			ddvto.hasBeenModified();
		}
	}
	
	@Override
	public String getFunction() {
		String function = new String(getObject().getStringObject());
		return function;
	}

	@Override
	public void setFunction(String function) {
		getObject().setStringObject(function);
		hasBeenModified();
	}
	
	@Override
	public void launchEditor(String editorType) {
		// Retrieve the project name
		String projectName = getConnectorTreeObject().getObject().getProject().getName();	
	
		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			
			// Open editor
			if ((editorType == null) || ((editorType != null) && (editorType.equals("JscriptHandlerEditor")))) {
				openJscriptHandlerEditor(project);
			}
		} 
		catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}

	public void openJscriptHandlerEditor(IProject project) {
		TreeObject object = getTreeObjectOwner();
		
		Document document = (Document)object.getObject();
		
		String tempFileName = 	"_private/"+project.getName()+
				"__"+getConnectorTreeObject().getName()+
				"__"+document.getName()+
				"__views."+getParent().getName()+"."+getName();

		IFile file = project.getFile(tempFileName);
		
		IWorkbenchPage activePage = PlatformUI
								.getWorkbench()
								.getActiveWorkbenchWindow()
								.getActivePage();
		
		if (activePage != null) {
			try {
				activePage.openEditor(new JscriptTreeFunctionEditorInput(file,this),
										"com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTreeFunctionEditor");
			} catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the document editor '" + document.getName() + "'");
			} 
		}
	}

	@Override
	public IDesignTreeObject add(Object object, boolean bChangeName) {
		if (object instanceof JsonData) {
			JsonData jsonData = (JsonData)object;
			Class<? extends TreeParent> c = jsonData.getOwnerClass();
			if (c.equals(DesignDocumentFunctionTreeObject.class)) {
				return ((IDesignTreeObject)getParent()).add(object, bChangeName);
			}
		}
		return null;
	}

	public static Object read(Node node) throws EngineException {
		String classname = null;
        JSONObject jsondata = null;
		try {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;
				classname = element.getAttribute("classname");
                Node cdata = XMLUtils.findChildNode(element, Node.CDATA_SECTION_NODE);
                if (cdata != null) {
                	jsondata = new JSONObject(cdata.getNodeValue());
                }
			}
		}
		catch (Exception e) {
            String message = "Unable to set the object properties from the serialized XML data.\n" +
			"Object class: '" + classname;
	        EngineException ee = new EngineException(message, e);
	        throw ee;
		}
		if (jsondata != null) {
			return new JsonData(DesignDocumentFunctionTreeObject.class, jsondata);
		}
		return null;
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
}
