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

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class DesignDocumentTreeObject extends DocumentTreeObject {

	private static final String KEY_ID = "_id";
	private static final String KEY_REV = "_rev";
	private static final String KEY_VIEWS = "views";

	private FolderTreeObject fViews = null;
	
	public DesignDocumentTreeObject(Viewer viewer, DatabaseObject object) {
		super(viewer, object);
	}

	public DesignDocumentTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object, inherited);
		fViews = new FolderTreeObject(viewer, "Views");
		loadViews();
	}

	@Override
	public DesignDocument getObject() {
		return (DesignDocument)super.getObject();
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
		if (KEY_ID.equals(propertyName)) {
			try {
				return getObject().getJSONObject().getString(KEY_ID);
			} catch (JSONException e) {
				return "";
			}
		}
		else if (KEY_REV.equals(propertyName)) {
			try {
				return getObject().getJSONObject().getString(KEY_REV);
			} catch (JSONException e) {
				return "";
			}
		}
		return super.getPropertyValue(id);
	}

	public String getDefaultViewName() {
		return "View";
	}
	
	protected synchronized void hasBeenModified() {
		JSONObject views = new JSONObject();
		for (TreeObject to : fViews.getChildren()) {
			DesignDocumentViewTreeObject ddvto = (DesignDocumentViewTreeObject)to;
			ViewObject viewObject = ddvto.getObject();
			try {
				views.put(viewObject.name, viewObject.getJSONObject());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			DesignDocument dd = getObject();
			JSONObject jso = dd.getJSONObject();
			jso.put(KEY_VIEWS, views);
			dd.hasChanged = true;
			hasBeenModified(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
    	viewer.update(this, null);
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
		DesignDocumentViewTreeObject ddvto = (view == null) ? newView():view;
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
		ViewObject view = new ViewObject(viewName, new JSONObject());
		view.createMap();
		view.createReduce();
		return view;
	}
	
	private void loadViews() {
		JSONObject jsonDocument = getObject().getJSONObject();
		JSONObject views = null;
		try {
			views = jsonDocument.getJSONObject(KEY_VIEWS);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (views != null) {
			Iterator<String> it = GenericUtils.cast(views.keys());
			if (it.hasNext())
				addChild(fViews);
			
			while(it.hasNext()) {
				String key = it.next();
				try {
					ViewObject view = new ViewObject(key, views.getJSONObject(key));
					fViews.addChild(new DesignDocumentViewTreeObject(viewer, view));
				} catch (JSONException e) {
					Engine.logBeans.warn("[DesignDocument] view named '"+key+"' is null; skipping...");
				}
			}
		}
	}
	
	public class ViewObject {
		private static final String KEY_MAP = "map";
		private static final String KEY_REDUCE = "reduce";
		
		private String name = null;
		private JSONObject jsonObject = null;
		
		ViewObject(String name, JSONObject jsonObject) {
			this.name = name;
			this.jsonObject = jsonObject;
		}

		public boolean hasMap() {
			return jsonObject.has(KEY_MAP);
		}
		
		public boolean hasReduce() {
			return jsonObject.has(KEY_REDUCE);
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
				try {
					return new FunctionObject(KEY_MAP, jsonObject.getString(KEY_MAP));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}
		
		protected FunctionObject createMap() {
			if (!hasMap()) {
				try {
					jsonObject.put(KEY_MAP, "function (doc) {\r\n\temit(doc._id, doc._rev);\r\n}");
					return getMap();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}
		
		protected FunctionObject getReduce() {
			if (hasReduce()) {
				try {
					return new FunctionObject(KEY_REDUCE, jsonObject.getString(KEY_REDUCE));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}
		
		protected FunctionObject createReduce() {
			if (!hasReduce()) {
				try {
					jsonObject.put(KEY_REDUCE, "function (keys, values) {\r\n\treturn sum(values);\r\n}");
					return getReduce();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
