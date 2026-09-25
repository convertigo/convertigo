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
		return children;
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
		} else {
			super.remove(databaseObject);
		}
	}

	@Override
	public boolean hasDatabaseObjectChildren() throws Exception {
		var views = getJSONObject().optJSONObject("views");
		return (views != null && views.length() > 0) || super.hasDatabaseObjectChildren();
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
