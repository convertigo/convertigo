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
import com.twinsoft.convertigo.beans.core.JsonDocument;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class DesignDocument extends JsonDocument {

	private static final long serialVersionUID = -1523783503757936794L;

	/** Shared with the Studio; custom reduces must combine partial results on CouchDB. */
	public static final String DEFAULT_REDUCE = "function (keys, values, rereduce) {\r\n\tif (rereduce) {\r\n\t\treturn sum(values);\r\n\t}\r\n\treturn values.length;\r\n}";
	private static final Pattern REDUCE_WHITESPACE = Pattern.compile("\\s+");
	private static final String LEGACY_REDUCE = REDUCE_WHITESPACE.matcher(
			"function (keys, values, rereduce) { try { return values.length; } catch (err) { log(err.message); } }").replaceAll("");
	private transient List<String> warnedLegacyReduceViews = List.of();
	
	public static final Pattern splitFunctionName = Pattern.compile("(.+?)/(.+)");
	
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
	public void configure(Element element) throws Exception {
		super.configure(element);
		identify();
		warnAboutLegacyReduceFunctions();
	}

	/** Recognize only the old, unmodified template, ignoring indentation and line endings. */
	public static boolean isLegacyReduce(String function) {
		return function != null && LEGACY_REDUCE.equals(REDUCE_WHITESPACE.matcher(function).replaceAll(""));
	}

	public List<String> getLegacyReduceViews() {
		List<String> names = new ArrayList<>();
		JSONObject json = getJSONObject();
		JSONObject views = json == null ? null : json.optJSONObject(CouchKey.views.key());
		if (views != null) {
			for (Iterator<String> i = GenericUtils.cast(views.keys()); i.hasNext();) {
				String name = i.next();
				JSONObject view = views.optJSONObject(name);
				if (view != null && view.opt(CouchKey.reduce.key()) instanceof String reduce && isLegacyReduce(reduce)) {
					names.add(name);
				}
			}
		}
		names.sort(String::compareTo);
		return names;
	}

	/** Warn on load/sync, without changing existing projects or repeating on every sync. */
	public synchronized void warnAboutLegacyReduceFunctions() {
		if (getParent() == null || Engine.logCouchDbManager == null) {
			return;
		}
		List<String> names = getLegacyReduceViews();
		for (String name : names) {
			if (!warnedLegacyReduceViews.contains(name)) {
				Engine.logCouchDbManager.warn("(DesignDocument) View '" + getQName() + "/" + name
						+ "' uses the old default reduce function, which ignores rereduce and can return incorrect counts on CouchDB. "
						+ "Use 'Fix legacy count reduce' in the Studio, or replace it with _count or a function that sums partial results.");
			}
		}
		warnedLegacyReduceViews = List.copyOf(names);
	}

	@Override
	public void setName(String name) throws EngineException {
		super.setName(name);
		identify();
	}

	/**
	 * Gives the document the id of its name, which the Eclipse Studio sets when it shows a new or renamed
	 * design document, so that it syncs to the database without the Studio: a renamed document is a new one.
	 */
	private void identify() {
		var json = getJSONObject();
		if (json == null || getName() == null) {
			return;
		}
		var id = CouchKey._design.key() + getName();
		if (!id.equals(CouchKey._id.String(json))) {
			CouchKey._id.put(json, id);
			CouchKey._rev.remove(json);
		}
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
