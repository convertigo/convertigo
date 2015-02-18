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

import java.util.Map.Entry;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.cdbproxy.CouchDbProxyManager;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.util.Json;

public class DesignDocumentTreeObject extends DocumentTreeObject {
	private FolderTreeObject fViews = null;
	private String lastRev = "?";

	public DesignDocumentTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object, inherited);
		fViews = new FolderTreeObject(viewer, "Views");
		loadViews();
		syncDocument();
	}

	@Override
	public DesignDocument getObject() {
		return (DesignDocument) super.getObject();
	}

	@Override
	public boolean rename(String newName, boolean bDialog) {
		if (getObject().bNew) {
			return super.rename(newName, bDialog);
		}
		else {
			ConvertigoPlugin.logException(new ConvertigoException("The document named \"" + newName + "\" can't be renamed anymore."), "Unable to change the object name.", bDialog);
			return false;
		}
	}

	@Override
	public Object getPropertyValue(Object id) {
		String propertyName = (String)id;
		if (CouchKey._id.key().equals(propertyName)) {
			try {
				return CouchKey._id.string(getObject().getJsonObject());
			} catch (Exception e) {
				return "";
			}
		}
		else if (CouchKey._rev.key().equals(propertyName)) {
			return lastRev;
		}
		return super.getPropertyValue(id);
	}

	public String getDefaultViewName() {
		return "View";
	}
	
	protected synchronized void hasBeenModified() {
		JsonObject views = new JsonObject();
		for (TreeObject to : fViews.getChildren()) {
			DesignDocumentViewTreeObject ddvto = (DesignDocumentViewTreeObject)to;
			ViewObject viewObject = ddvto.getObject();
			views.add(viewObject.name, viewObject.getJsonObject());
		}
		
		try {
			DesignDocument dd = getObject();
			JsonObject jso = dd.getJsonObject();
			CouchKey.views.add(jso, views);
			dd.hasChanged = true;
			hasBeenModified(true);
			
			syncDocument();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
    	viewer.update(this, null);
	}
	
	private void syncDocument() {
		DesignDocument dd = getObject();
		JsonObject jso = dd.getJsonObject();
		
		if (CouchKey._id.has(jso)) {
			CouchDbConnector couchDbConnector = dd.getConnector();
			lastRev = CouchDbProxyManager.syncDocument(couchDbConnector.getCouchDbClient(), couchDbConnector.getDatabaseName(), jso.toString());
		}
	}
	
	public DesignDocumentViewTreeObject addNewView() {
		return addView(null);
	}
	
	public void removeView(DesignDocumentViewTreeObject view) {
		if (view != null) {
			fViews.removeChild(view);
			hasBeenModified();
		}
	}
	
	protected DesignDocumentViewTreeObject addView(DesignDocumentViewTreeObject view) {
		DesignDocumentViewTreeObject ddvto = (view == null) ? newView() : view;
		
		if (ddvto != null) {
			fViews.addChild(ddvto);
			hasBeenModified();
		}
		return ddvto;
	}
	
	protected boolean hasView(String viewName) {
		for (TreeObject to : fViews.getChildren()) {
			if (to.getName().equals(viewName)) {
				return true;
			}
		}
		return false;
	}
	
	protected DesignDocumentViewTreeObject newView() {
		return newView(createViewObject());
	}
	
	protected DesignDocumentViewTreeObject newView(ViewObject view) {
		return new DesignDocumentViewTreeObject(viewer, view);
	}
	
	protected ViewObject createViewObject() {
		int index = 1;
		String viewName = getDefaultViewName();
		
		while (hasView(viewName)) {
			viewName = getDefaultViewName() + index++;
		}
		
		ViewObject view = new ViewObject(viewName, new JsonObject());
		view.createMap();
		view.createReduce();
		return view;
	}
	
	private void loadViews() {
		JsonObject jsonDocument = getObject().getJsonObject();
		JsonObject views = null;
		
		views = jsonDocument.getAsJsonObject(CouchKey.views.key());
		
		if (views != null) {
			if (!Json.isEmpty(views)) {
				addChild(fViews);
			}
			
			for (Entry<String, JsonElement> entry: views.entrySet()) {
				try {
					ViewObject view = new ViewObject(entry.getKey(), entry.getValue().getAsJsonObject());
					fViews.addChild(new DesignDocumentViewTreeObject(viewer, view));
				} catch (Exception e) {
					Engine.logBeans.warn("[DesignDocument] view named '" + entry.getKey() + "' is null; skipping...");
				}
			}
		}
	}
	
	public class ViewObject {		
		private String name = null;
		private JsonObject jsonObject = null;
		
		ViewObject(String name, JsonObject jsonObject) {
			this.name = name;
			this.jsonObject = jsonObject;
		}

		public boolean hasMap() {
			return CouchKey.map.has(jsonObject);
		}
		
		public boolean hasReduce() {
			return CouchKey.reduce.has(jsonObject);
		}
		
		public String getName() {
			return this.name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		protected JsonObject getJsonObject() {
			return jsonObject;
		}
		
		protected void setJsonObject(JsonObject jsonObject) {
			this.jsonObject = jsonObject;
		}

		protected FunctionObject getMap() {
			if (hasMap()) {
				return new FunctionObject(CouchKey.map.key(), CouchKey.map.string(jsonObject));
			}
			return null;
		}
		
		protected FunctionObject createMap() {
			if (!hasMap()) {
				jsonObject.addProperty(CouchKey.map.key(), "function (doc) {\r\n\temit(doc._id, doc._rev);\r\n}");
				return getMap();
			}
			return null;
		}
		
		protected FunctionObject getReduce() {
			if (hasReduce()) {
				return new FunctionObject(CouchKey.reduce.key(), CouchKey.reduce.string(jsonObject));
			}
			return null;
		}
		
		protected FunctionObject createReduce() {
			if (!hasReduce()) {
				jsonObject.addProperty(CouchKey.reduce.key(), "function (keys, values) {\r\n\treturn sum(values);\r\n}");
				return getReduce();
			}
			return null;
		}
		
		@Override
		public String toString() {
			return getName();
		}
	}

	public class FunctionObject {
		protected String name = null;
		protected String function = null;
		
		FunctionObject(String name, String function) {
			this.name = name;
			this.function = function;
		}

		public String getStringObject() {
			return function;
		}

		public void setStringObject(String function) {
			this.function = function;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
