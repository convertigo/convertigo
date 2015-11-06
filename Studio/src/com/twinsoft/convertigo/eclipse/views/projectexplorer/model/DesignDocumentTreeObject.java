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

import java.util.Iterator;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.JsonData;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class DesignDocumentTreeObject extends DocumentTreeObject implements IDesignTreeObject {
	private FolderTreeObject fFilters = null;
	private FolderTreeObject fUpdates = null;
	private FolderTreeObject fViews = null;
	private String lastRev = "?";

	public DesignDocumentTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object, inherited);
		fFilters = new FolderTreeObject(viewer, "Filters");
		fUpdates = new FolderTreeObject(viewer, "Updates");
		fViews = new FolderTreeObject(viewer, "Views");
		loadFilters();
		loadUpdates();
		loadViews();
		
		if (object.bNew) {
			JSONObject json = getObject().getJSONObject();
			CouchKey._id.put(json, CouchKey._design.key() + getObject().getName());
			CouchKey._rev.remove(json);
		}
		
		syncDocument();
	}

	@Override
	public DesignDocument getObject() {
		return (DesignDocument) super.getObject();
	}

	@Override
	public boolean rename(String newName, boolean bDialog) {
		String oldName = getName();
		if (oldName.equals(newName))
			return true;
		
		if (super.rename(newName, bDialog)) {
			renameDocument(oldName, newName);
			return true;
		}
		return false;
	}

	private void renameDocument(String oldName, String newName) {
		if (oldName != null && newName != null) {
			try {
				CouchDbConnector couchDbConnector = getObject().getConnector();
				CouchClient couchClient = couchDbConnector.getCouchClient();
				String db = couchDbConnector.getDatabaseName();
				
				// delete old document
				couchClient.deleteDocument(db, CouchKey._design.key() + oldName, (Map<String, String>) null);
				
				// create new document
				JSONObject jso = getObject().getJSONObject();
				CouchKey._id.put(jso, CouchKey._design.key() + newName);
				CouchKey._rev.remove(jso);
				lastRev = CouchDbManager.syncDocument(couchClient, db, jso.toString());
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Coudn't rename document \""+oldName+"\" to \""+newName+"\" in the CouchDb database", false);
			}
		}
	}
		
	@Override
	public Object getPropertyValue(Object id) {
		String propertyName = (String)id;
		if (CouchKey._id.key().equals(propertyName)) {
			try {
				return CouchKey._id.String(getObject().getJSONObject());
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
		return "view";
	}
	
	public String getDefaultFilterName() {
		return "filter";
	}

	public String getDefaultUpdateName() {
		return "update";
	}

	@Override
	public synchronized void hasBeenModified() {
		JSONObject filters = new JSONObject();
		for (TreeObject to : fFilters.getChildren()) {
			DesignDocumentFilterTreeObject ddfto = (DesignDocumentFilterTreeObject)to;
			FilterObject filterObject = ddfto.getObject();
			try {
				filters.put(filterObject.name, filterObject.function);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
			}
		}

		JSONObject updates = new JSONObject();
		for (TreeObject to : fUpdates.getChildren()) {
			DesignDocumentUpdateTreeObject dduto = (DesignDocumentUpdateTreeObject)to;
			UpdateObject updateObject = dduto.getObject();
			try {
				updates.put(updateObject.name, updateObject.function);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
			}
		}

		JSONObject views = new JSONObject();
		for (TreeObject to : fViews.getChildren()) {
			DesignDocumentViewTreeObject ddvto = (DesignDocumentViewTreeObject)to;
			ViewObject viewObject = ddvto.getObject();
			try {
				views.put(viewObject.name, viewObject.getJSONObject());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
			}
		}
		
		try {
			DesignDocument dd = getObject();
			JSONObject jso = dd.getJSONObject();
			CouchKey.filters.put(jso, filters);
			CouchKey.updates.put(jso, updates);
			CouchKey.views.put(jso, views);
			dd.hasChanged = true;
			hasBeenModified(true);
			
			syncDocument();
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}
		
        TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
    	viewer.update(this, null);
	}
	
	private void syncDocument() {
		try {
			DesignDocument dd = getObject();
			JSONObject jso = dd.getJSONObject();
			
			if (CouchKey._id.has(jso)) {
				CouchDbConnector couchDbConnector = dd.getConnector();
				lastRev = CouchDbManager.syncDocument(couchDbConnector.getCouchClient(), couchDbConnector.getDatabaseName(), jso.toString());
			}
		} catch (Throwable t) {
			ConvertigoPlugin.logException(t, "Failed to syncDocument");
		}
	}
	
	public DesignDocumentViewTreeObject addNewView() {
		return addView(null);
	}
	
	public DesignDocumentFilterTreeObject addNewFilter() {
		return addFilter(null);
	}

	public DesignDocumentUpdateTreeObject addNewUpdate() {
		return addUpdate(null);
	}

	public void removeView(DesignDocumentViewTreeObject view) {
		if (view != null) {
			fViews.removeChild(view);
			hasBeenModified();
		}
	}
	
	public void removeFilter(DesignDocumentFilterTreeObject filter) {
		if (filter != null) {
			fFilters.removeChild(filter);
			hasBeenModified();
		}
	}

	public void removeUpdate(DesignDocumentUpdateTreeObject update) {
		if (update != null) {
			fUpdates.removeChild(update);
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
	
	protected DesignDocumentFilterTreeObject addFilter(DesignDocumentFilterTreeObject filter) {
		DesignDocumentFilterTreeObject ddfto = (filter == null) ? newFilter() : filter;
		if (ddfto != null) {
			fFilters.addChild(ddfto);
			hasBeenModified();
		}
		return ddfto;
	}

	protected DesignDocumentUpdateTreeObject addUpdate(DesignDocumentUpdateTreeObject update) {
		DesignDocumentUpdateTreeObject dduto = (update == null) ? newUpdate() : update;
		if (dduto != null) {
			fUpdates.addChild(dduto);
			hasBeenModified();
		}
		return dduto;
	}

	protected boolean hasView(String viewName) {
		for (TreeObject to : fViews.getChildren()) {
			if (to.getName().equals(viewName)) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean hasFilter(String filterName) {
		for (TreeObject to : fFilters.getChildren()) {
			if (to.getName().equals(filterName)) {
				return true;
			}
		}
		return false;
	}

	protected boolean hasUpdate(String updateName) {
		for (TreeObject to : fUpdates.getChildren()) {
			if (to.getName().equals(updateName)) {
				return true;
			}
		}
		return false;
	}

	protected DesignDocumentViewTreeObject newView() {
		return newView(createViewObject());
	}
	
	protected DesignDocumentFilterTreeObject newFilter() {
		return newFilter(createFilterObject());
	}

	protected DesignDocumentUpdateTreeObject newUpdate() {
		return newUpdate(createUpdateObject());
	}

	protected DesignDocumentViewTreeObject newView(ViewObject view) {
		return new DesignDocumentViewTreeObject(viewer, view);
	}
	
	protected DesignDocumentFilterTreeObject newFilter(FilterObject filter) {
		return new DesignDocumentFilterTreeObject(viewer, filter);
	}

	protected DesignDocumentUpdateTreeObject newUpdate(UpdateObject update) {
		return new DesignDocumentUpdateTreeObject(viewer, update);
	}

	protected ViewObject createViewObject() {
		int index = 1;
		String viewName = getDefaultViewName();
		
		while (hasView(viewName)) {
			viewName = getDefaultViewName() + index++;
		}
		
		ViewObject view = new ViewObject(viewName, new JSONObject());
		view.createMap();
		return view;
	}
	
	protected FilterObject createFilterObject() {
		int index = 1;
		String filterName = getDefaultFilterName();
		
		while (hasFilter(filterName)) {
			filterName = getDefaultFilterName() + index++;
		}
		
		FilterObject filter = new FilterObject(filterName, "function (doc, req) {\r\n\ttry {\r\n\t\treturn true;\r\n\t} catch (err) {\r\n\t\tlog(err.message);\r\n\t}\r\n}");
		return filter;
	}
	
	protected UpdateObject createUpdateObject() {
		int index = 1;
		String updateName = getDefaultUpdateName();
		
		while (hasUpdate(updateName)) {
			updateName = getDefaultUpdateName() + index++;
		}
		
		UpdateObject update = new UpdateObject(updateName, "function (doc, req) {\r\n\ttry {\r\n\t\tvar json = JSON.parse(req.body);\r\n\t\treturn [doc, {json: {result: 'nothing done'}}];\r\n\t} catch (err) {\r\n\t\tlog(err.message);\r\n\t}\r\n}");
		return update;
	}

	private void loadViews() {
		JSONObject jsonDocument = getObject().getJSONObject();
		JSONObject views = CouchKey.views.JSONObject(jsonDocument);
		
		if (views != null) {
			if (views.length() > 0) {
				addChild(fViews);
			}
			
			for (Iterator<String> i = GenericUtils.cast(views.keys()); i.hasNext(); ) {
				String key = i.next();
				
				try {
					ViewObject view = new ViewObject(key, views.getJSONObject(key));
					fViews.addChild(new DesignDocumentViewTreeObject(viewer, view));
				} catch (Exception e) {
					Engine.logBeans.warn("[DesignDocument] view named '" + key + "' is null; skipping...");
				}
			}
		}
	}
	
	private void loadFilters() {
		JSONObject jsonDocument = getObject().getJSONObject();
		JSONObject filters = CouchKey.filters.JSONObject(jsonDocument);
		
		if (filters != null) {
			if (filters.length() > 0) {
				addChild(fFilters);
			}
			
			for (Iterator<String> i = GenericUtils.cast(filters.keys()); i.hasNext(); ) {
				String key = i.next();
				
				try {
					FilterObject filter = new FilterObject(key, filters.getString(key));
					fFilters.addChild(new DesignDocumentFilterTreeObject(viewer, filter));
				} catch (Exception e) {
					Engine.logBeans.warn("[DesignDocument] filter named '" + key + "' is null; skipping...");
				}
			}
		}
	}
	
	private void loadUpdates() {
		JSONObject jsonDocument = getObject().getJSONObject();
		JSONObject updates = CouchKey.updates.JSONObject(jsonDocument);
		
		if (updates != null) {
			if (updates.length() > 0) {
				addChild(fUpdates);
			}
			
			for (Iterator<String> i = GenericUtils.cast(updates.keys()); i.hasNext(); ) {
				String key = i.next();
				
				try {
					UpdateObject update = new UpdateObject(key, updates.getString(key));
					fUpdates.addChild(new DesignDocumentUpdateTreeObject(viewer, update));
				} catch (Exception e) {
					Engine.logBeans.warn("[DesignDocument] update named '" + key + "' is null; skipping...");
				}
			}
		}
	}
	
	private String getAvailableViewName(String givenName) {
		int index = 1;
		String viewName = givenName;
		while (hasView(viewName)) {
			viewName = givenName + index++;
		}
		return viewName;
	}
	
	private String getAvailableFilterName(String givenName) {
		int index = 1;
		String filterName = givenName;
		while (hasFilter(filterName)) {
			filterName = givenName + index++;
		}
		return filterName;
	}

	private String getAvailableUpdateName(String givenName) {
		int index = 1;
		String updateName = givenName;
		while (hasUpdate(updateName)) {
			updateName = givenName + index++;
		}
		return updateName;
	}

	@Override
	public TreeParent getTreeObjectOwner() {
		return getParent().getParent();
	}

	@Override
	public DesignDocumentTreeObject getParentDesignTreeObject() {
		return this;
	}
	
	@Override
	public IDesignTreeObject add(Object object, boolean bChangeName) {
		if (object instanceof JsonData) {
			JsonData jsonData = (JsonData)object;
			Class<? extends TreeParent> c = jsonData.getOwnerClass();
			if (c.equals(DesignDocumentViewTreeObject.class)) {
				JSONObject jsonView = jsonData.getData();
				try {
					String viewName = getAvailableViewName(jsonView.getString("name"));
					JSONObject jsonObject = jsonView.getJSONObject("value");
					ViewObject view = new ViewObject(viewName, jsonObject);
					return addView(newView(view));
				} catch (Exception e) {
				}
			}
			else if (c.equals(DesignDocumentFilterTreeObject.class)) {
				JSONObject jsonFilter = jsonData.getData();
				try {
					String filterName = getAvailableFilterName(jsonFilter.getString("name"));
					String filterValue = jsonFilter.getString("value");
					FilterObject filter = new FilterObject(filterName, filterValue);
					return addFilter(newFilter(filter));
				} catch (Exception e) {
				}
			}
			else if (c.equals(DesignDocumentUpdateTreeObject.class)) {
				JSONObject jsonUpdate = jsonData.getData();
				try {
					String updateName = getAvailableUpdateName(jsonUpdate.getString("name"));
					String updateValue = jsonUpdate.getString("value");
					UpdateObject update = new UpdateObject(updateName, updateValue);
					return addUpdate(newUpdate(update));
				} catch (Exception e) {
				}
			}
		}
		else if (object instanceof DesignDocumentViewTreeObject) {
			return addView((DesignDocumentViewTreeObject)object);
		}
		else if (object instanceof DesignDocumentFilterTreeObject) {
			return addFilter((DesignDocumentFilterTreeObject)object);
		}
		else if (object instanceof DesignDocumentUpdateTreeObject) {
			return addUpdate((DesignDocumentUpdateTreeObject)object);
		}
		return null;
	}

	@Override
	public void remove(Object object) {
		if (object instanceof DesignDocumentViewTreeObject) {
			removeView((DesignDocumentViewTreeObject)object);
		}
		else if (object instanceof DesignDocumentFilterTreeObject) {
			removeFilter((DesignDocumentFilterTreeObject)object);
		}
		else if (object instanceof DesignDocumentUpdateTreeObject) {
			removeUpdate((DesignDocumentUpdateTreeObject)object);
		}
	}
	
	public static Object read(Node node) throws EngineException {
		return DatabaseObject.read(node);
	}
	
	@Override
	public Element toXml(Document document) {
		try {
			return getObject().toXml(document);
		} catch (EngineException e) {
			return null;
		}
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
	
	public class ViewObject {		
		private String name = null;
		private JSONObject jsonObject = null;
		
		ViewObject(String name, JSONObject jsonObject) {
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
		
		protected JSONObject getJSONObject() {
			return jsonObject;
		}
		
		protected void setJSONObject(JSONObject jsonObject) {
			this.jsonObject = jsonObject;
		}

		protected FunctionObject getMap() {
			if (hasMap()) {
				return new FunctionObject(CouchKey.map.key(), CouchKey.map.String(jsonObject));
			}
			return null;
		}
		
		protected FunctionObject createMap() {
			if (!hasMap()) {
				CouchKey.map.put(jsonObject, "function (doc) {\r\n\ttry {\r\n\t\temit(doc._id, doc._rev);\r\n\t} catch (err) {\r\n\t\tlog(err.message);\r\n\t}\r\n}");
				return getMap();
			}
			return null;
		}
		
		protected FunctionObject getReduce() {
			if (hasReduce()) {
				return new FunctionObject(CouchKey.reduce.key(), CouchKey.reduce.String(jsonObject));
			}
			return null;
		}
		
		protected FunctionObject createReduce() {
			if (!hasReduce()) {
				CouchKey.reduce.put(jsonObject, "function (keys, values, rereduce) {\r\n\ttry {\r\n\t\treturn values.length;\r\n\t} catch (err) {\r\n\t\tlog(err.message);\r\n\t}\r\n}");
				return getReduce();
			}
			return null;
		}
		
		protected FunctionObject createFunction(String funcName, String funcValue) {
			if (CouchKey.map.name().equals(funcName) && !hasMap())
				return new FunctionObject(funcName, funcValue);
			if (CouchKey.reduce.name().equals(funcName) && !hasReduce())
				return new FunctionObject(funcName, funcValue);
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

		public String getName() {
			return this.name;
		}
		
		@Override
		public String toString() {
			return this.name;
		}

		public void setName(String newName) {
			this.name = newName;
		}

	}

	public class FilterObject extends FunctionObject {
		FilterObject(String name, String function) {
			super(name, function);
		}
		
	}

	public class UpdateObject extends FunctionObject {
		UpdateObject(String name, String function) {
			super(name, function);
		}
		
	}

	@Override
	public boolean canPaste(Object object) {
		if (object != null) {
			if (object instanceof DesignDocumentFilterTreeObject)
				return true;
			else if (object instanceof DesignDocumentUpdateTreeObject)
				return true;
			else if (object instanceof DesignDocumentViewTreeObject)
				return true;
		}
		return false;
	}
}
