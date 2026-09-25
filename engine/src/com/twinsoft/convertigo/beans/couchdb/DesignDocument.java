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

package com.twinsoft.convertigo.beans.couchdb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.JsonDocument;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class DesignDocument extends JsonDocument {

	private static final long serialVersionUID = -1523783503757936794L;
	
	public static final Pattern splitFunctionName = Pattern.compile("(.+?)/(.+)");

	/** The map function of a new view, as the Eclipse Studio makes it */
	public static final String DEFAULT_MAP = "function (doc) {\r\n\ttry {\r\n\t\temit(doc._id, doc._rev);\r\n\t} catch (err) {\r\n\t\tlog(err.message);\r\n\t}\r\n}";

	/** The functions of a new filter, update or validate function, as the Eclipse Studio makes them */
	public static final String DEFAULT_FILTER = "function (doc, req) {\r\n\ttry {\r\n\t\treturn true;\r\n\t} catch (err) {\r\n\t\tlog(err.message);\r\n\t}\r\n}";
	public static final String DEFAULT_UPDATE = "function (doc, req) {\r\n\ttry {\r\n\t\tvar json = JSON.parse(req.body);\r\n\t\treturn [doc, {json: {result: 'nothing done'}}];\r\n\t} catch (err) {\r\n\t\tlog(err.message);\r\n\t}\r\n}";
	public static final String DEFAULT_VALIDATE = "function(newDoc, oldDoc, userCtx, secObj) {\r\n\t// check if newDoc respect the DB rules\r\n\t// if not, throw an exception like that:\r\n\t// throw({forbidden : 'type your error here'});\r\n\treturn true;\r\n}";

	/** The reduce function added to a view, as the Eclipse Studio makes it */
	public static final String DEFAULT_REDUCE = "function (keys, values, rereduce) {\r\n\ttry {\r\n\t\treturn values.length;\r\n\t} catch (err) {\r\n\t\tlog(err.message);\r\n\t}\r\n}";
	
	public DesignDocument() {
		super();
	}

	@Override
	public DesignDocument clone() throws CloneNotSupportedException {
		DesignDocument clonedObject =  (DesignDocument) super.clone();
		return clonedObject;
	}

	@Override
	public String getRenderer() {
		return "DesignDocumentTreeObject";
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		/*if (jsonDocument != null) {
			if (bNew) {
				CouchKey._rev.remove(jsonDocument);
				CouchKey._id.put(jsonDocument, CouchKey._design.key() + getName());
			}
		}*/
		return super.toXml(document);
	}

	//*********************************************************************************
	// DO NOT REMOVE following fake getters/setters of _id and _rev bean's properties
	// The properties values are handled by the renderer : see DesignDocumentTreeObject
	//---------------------------------------------------------------------------------
	public String getId() {
		return "";
	}

	public void setId(String id) {
		// does nothing
	}
	
	public String getRevision() {
		return "";
	}

	public void setRevision(String revision) {
		// does nothing
	}
	//*********************************************************************************
	
	@Override
	public CouchDbConnector getConnector() {
		return (CouchDbConnector) super.getConnector();
	}

	@Override
	public List<DatabaseObject> getDatabaseObjectChildren() throws Exception {
		var children = new ArrayList<>(super.getDatabaseObjectChildren());
		var views = getJSONObject().optJSONObject("views");
		if (views != null) {
			var names = new ArrayList<String>();
			for (var iterator = views.keys(); iterator.hasNext();) {
				names.add(String.valueOf(iterator.next()));
			}
			names.sort(String.CASE_INSENSITIVE_ORDER);
			for (var name : names) {
				try {
					children.add(new DesignDocumentView(this, name));
				} catch (EngineException e) {
					com.twinsoft.convertigo.engine.Engine.logBeans
							.warn("Unable to expose FullSync view \"" + name + "\".", e);
				}
			}
		}
		for (var kind : new String[] { DesignDocumentFunction.FILTERS, DesignDocumentFunction.UPDATES }) {
			for (var name : functionNames(kind)) {
				children.add(new DesignDocumentFunction(this, kind, name));
			}
		}
		if (getJSONObject().has(DesignDocumentFunction.VALIDATE)) {
			children.add(new DesignDocumentFunction(this, DesignDocumentFunction.VALIDATE, DesignDocumentFunction.VALIDATE));
		}
		return children;
	}

	/**
	 * @return the sorted names of the filters or updates of the design document
	 */
	private List<String> functionNames(String kind) {
		var names = new ArrayList<String>();
		var functions = getJSONObject().optJSONObject(kind);
		if (functions != null) {
			for (var iterator = functions.keys(); iterator.hasNext();) {
				names.add(String.valueOf(iterator.next()));
			}
		}
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	/**
	 * Adds a filter or an update function named as its kind, with a number if needed, or the validate
	 * function, with the function of the Eclipse Studio.
	 *
	 * @return the name of the function
	 */
	public String addFunction(String kind) throws EngineException {
		try {
			var json = getJSONObject();
			if (DesignDocumentFunction.VALIDATE.equals(kind)) {
				if (!json.has(kind)) {
					json.put(kind, DEFAULT_VALIDATE);
					hasChanged = true;
				}
				return kind;
			}
			var functions = json.optJSONObject(kind);
			if (functions == null) {
				functions = new JSONObject();
				json.put(kind, functions);
			}
			var prefix = DesignDocumentFunction.FILTERS.equals(kind) ? "filter" : "update";
			var name = prefix;
			for (var index = 1; functions.has(name); index++) {
				name = prefix + index;
			}
			functions.put(name, DesignDocumentFunction.FILTERS.equals(kind) ? DEFAULT_FILTER : DEFAULT_UPDATE);
			hasChanged = true;
			return name;
		} catch (Exception e) {
			throw new EngineException("Unable to add a function to the design document \"" + getName() + "\".", e);
		}
	}

	/**
	 * @return the object of a filter, an update or the validate function of the design document, or null
	 */
	public DesignDocumentFunction getFunction(String kind, String name) throws Exception {
		for (var child : getDatabaseObjectChildren()) {
			if (child instanceof DesignDocumentFunction function && function.getKind().equals(kind)
					&& function.getFunctionName().equals(name)) {
				return function;
			}
		}
		return null;
	}

	/**
	 * Renames a filter or an update function of the design document.
	 */
	public void renameFunction(String kind, String oldName, String newName) throws EngineException {
		var functions = getJSONObject().optJSONObject(kind);
		if (DesignDocumentFunction.VALIDATE.equals(kind) || functions == null || !functions.has(oldName)) {
			throw new EngineException("The function \"" + oldName + "\" cannot be renamed.");
		}
		if (functions.has(newName)) {
			throw new EngineException("The function named \"" + newName + "\" already exists.");
		}
		if (!oldName.equals(newName)) {
			try {
				functions.put(newName, functions.get(oldName));
				functions.remove(oldName);
			} catch (Exception e) {
				throw new EngineException("Unable to rename the function \"" + oldName + "\".", e);
			}
			hasChanged = true;
		}
	}

	/**
	 * Adds a view named "view", or "view" and a number, with the default map function.
	 *
	 * @return the name of the view
	 */
	public String addView() throws EngineException {
		try {
			var views = getJSONObject().optJSONObject("views");
			if (views == null) {
				views = new JSONObject();
				getJSONObject().put("views", views);
			}
			var name = "view";
			for (var index = 1; views.has(name); index++) {
				name = "view" + index;
			}
			views.put(name, new JSONObject().put("map", DEFAULT_MAP));
			hasChanged = true;
			return name;
		} catch (Exception e) {
			throw new EngineException("Unable to add a view to the design document \"" + getName() + "\".", e);
		}
	}

	/**
	 * @return the object of a view of the design document, or null
	 */
	public DesignDocumentView getView(String viewName) throws Exception {
		for (var child : getDatabaseObjectChildren()) {
			if (child instanceof DesignDocumentView view && view.getViewName().equals(viewName)) {
				return view;
			}
		}
		return null;
	}

	/**
	 * Renames a view of the design document.
	 */
	public void renameView(String oldName, String newName) throws EngineException {
		var views = getJSONObject().optJSONObject("views");
		if (views == null || !views.has(oldName)) {
			throw new EngineException("The design document \"" + getName() + "\" has no view named \"" + oldName + "\".");
		}
		if (views.has(newName)) {
			throw new EngineException("The view named \"" + newName + "\" already exists.");
		}
		if (!oldName.equals(newName)) {
			try {
				views.put(newName, views.get(oldName));
				views.remove(oldName);
			} catch (Exception e) {
				throw new EngineException("Unable to rename the view \"" + oldName + "\".", e);
			}
			hasChanged = true;
		}
	}

	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof DesignDocumentView view) {
			// a view is a part of the JSON of the design document
			var views = getJSONObject().optJSONObject("views");
			if (views != null && views.remove(view.getViewName()) != null) {
				hasChanged = true;
			}
		} else if (databaseObject instanceof DesignDocumentFunction function) {
			var json = getJSONObject();
			var functions = json.optJSONObject(function.getKind());
			if (DesignDocumentFunction.VALIDATE.equals(function.getKind()) ? json.remove(function.getKind()) != null
					: functions != null && functions.remove(function.getFunctionName()) != null) {
				hasChanged = true;
			}
		} else {
			super.remove(databaseObject);
		}
	}

	@Override
	public boolean hasDatabaseObjectChildren() throws Exception {
		var views = getJSONObject().optJSONObject("views");
		return (views != null && views.length() > 0) || !functionNames(DesignDocumentFunction.FILTERS).isEmpty()
				|| !functionNames(DesignDocumentFunction.UPDATES).isEmpty()
				|| getJSONObject().has(DesignDocumentFunction.VALIDATE) || super.hasDatabaseObjectChildren();
	}
	
	static public String[] getTags(AbstractCouchDbTransaction couchDbTransaction, CouchKey key) {
		List<String> values = new LinkedList<String>();
		values.add("");
		
		for (com.twinsoft.convertigo.beans.core.Document document : couchDbTransaction.getConnector().getDocumentsList()) {
			if (document instanceof DesignDocument) {
				JSONObject views = key.JSONObject(((DesignDocument) document).getJSONObject());
				if (views != null) {
					for (Iterator<String> i = GenericUtils.cast(views.keys()); i.hasNext(); ) {
						values.add(document.getName() + "/" + i.next());
					}
				}
			}
		}
		return values.toArray(new String[values.size()]);
	}
}
