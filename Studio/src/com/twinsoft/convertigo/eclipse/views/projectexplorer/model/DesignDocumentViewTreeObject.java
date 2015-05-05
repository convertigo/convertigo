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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IActionFilter;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.JsonData;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject.FunctionObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject.ViewObject;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class DesignDocumentViewTreeObject extends TreeParent implements IDesignTreeObject, IActionFilter {

	public DesignDocumentViewTreeObject(Viewer viewer, Object object) {
		super(viewer, object);
		loadFunctions();
	}

	@Override
	public ViewObject getObject() {
		return (ViewObject)super.getObject();
	}

	@Override
	public TreeParent getTreeObjectOwner() {
		return getParent().getParent();
	}
	
	@Override
	public DesignDocumentTreeObject getParentDesignTreeObject() {
		return (DesignDocumentTreeObject) getParent().getParent();
	}
	
	public boolean hasMap() {
		return getObject().hasMap();
	}
	
	public boolean hasReduce() {
		return getObject().hasReduce();
	}

	public String getDocViewName() {
		return getParentDesignTreeObject().getName() + "/" + getName();
	}
	
	public DesignDocumentFunctionTreeObject addReduce() {
		DesignDocumentFunctionTreeObject ddfto = null;
		FunctionObject fo = getObject().createReduce();
		if (fo != null) {
			ddfto = new DesignDocumentFunctionTreeObject(viewer, fo);
			addChild(ddfto);
			hasBeenModified();
		}
		return ddfto;
	}
	
	public void removeFunction(DesignDocumentFunctionTreeObject ddfto) {
		if (ddfto != null) {
			if (CouchKey.reduce.name().equals(ddfto.getName())) {
				removeChild(ddfto);
				hasBeenModified();
			}
		}
	}
	
	public boolean rename(String newName, Boolean bDialog) {
		if (getName().equals(newName))
			return true;
		if (getParentDesignTreeObject().hasView(newName)) {
			ConvertigoPlugin.logException(new ConvertigoException("The view named \"" + newName + "\" already exists."), "Unable to change the object name.", bDialog);
			return false;
		}
		
		getObject().setName(newName);
		hasBeenModified();
		
        TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
    	viewer.update(this, null);
		
		return true;
	}
	
	private void loadFunctions() {
		ViewObject view = getObject();
		if (view.hasMap())
			addChild(new DesignDocumentFunctionTreeObject(viewer, view.getMap()));
		if (view.hasReduce())
			addChild(new DesignDocumentFunctionTreeObject(viewer, view.getReduce()));
	}

	@Override
	public void hasBeenModified() {
		JSONObject functions = new JSONObject();
		for (TreeObject to : getChildren()) {
			DesignDocumentFunctionTreeObject ddfto = (DesignDocumentFunctionTreeObject)to;
			FunctionObject fo = ddfto.getObject();
			try {
				functions.put(fo.name, fo.getStringObject());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
			}
		}
		
		getObject().setJSONObject(functions);
		
		DesignDocumentTreeObject ddto = getParentDesignTreeObject();
		if (ddto != null) {
			ddto.hasBeenModified();
		}
	}

	@Override
	public IDesignTreeObject add(Object object, boolean bChangeName) {
		if (object instanceof JsonData) {
			JsonData jsonData = (JsonData)object;
			Class<? extends TreeParent> c = jsonData.getOwnerClass();
			if (c.equals(DesignDocumentFunctionTreeObject.class)) {
				JSONObject jsonFunction = jsonData.getData();
				try {
					String funcName = jsonFunction.getString("name");
					String funcValue = jsonFunction.getString("value");
					FunctionObject fo = getObject().createFunction(funcName, funcValue);
					if (fo != null) {
						return addFunction(newFunction(fo));
					}
				} catch (Exception e) {
				}
			}
			else if (c.equals(DesignDocumentViewTreeObject.class)) {
				return getParentDesignTreeObject().add(object, bChangeName);
			}
		}
		else if (object instanceof DesignDocumentFunctionTreeObject) {
			return addFunction((DesignDocumentFunctionTreeObject)object);
		}
		else if (object instanceof DesignDocumentViewTreeObject) {
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
		else if (object instanceof DesignDocumentFunctionTreeObject) {
			removeFunction((DesignDocumentFunctionTreeObject)object);
		}
	}

	private DesignDocumentFunctionTreeObject newFunction(FunctionObject function) {
		return new DesignDocumentFunctionTreeObject(viewer, function);
	}

	private IDesignTreeObject addFunction(DesignDocumentFunctionTreeObject function) {
		if (function != null) {
			addChild(function);
			hasBeenModified();
		}
		return function;
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
			return new JsonData(DesignDocumentViewTreeObject.class, jsondata);
		}
		return null;
	}
	
	@Override
	public Element toXml(Document document) {
		Element element = document.createElement("view");
		element.setAttribute("classname", getClass().getName());
		JSONObject jsondata = new JSONObject();
		try {
			jsondata.put("name", getObject().getName());
			jsondata.put("value", getObject().getJSONObject());
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
			if (object.getClass().equals(DesignDocumentFunctionTreeObject.class))
				return true;
			else if (object instanceof DesignDocumentViewTreeObject)
				return true;
		}
		return false;
	}

}
