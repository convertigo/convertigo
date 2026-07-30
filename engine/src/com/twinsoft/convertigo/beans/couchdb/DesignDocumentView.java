/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software under the GNU Affero General Public License.
 */

package com.twinsoft.convertigo.beans.couchdb;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IDynamicPropertyContainer;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/** Studio-only projection of a CouchDB design document view. */
public class DesignDocumentView extends DatabaseObject implements IDynamicPropertyContainer {

	private static final long serialVersionUID = 6297670833748561580L;

	private final String viewName;

	DesignDocumentView(DesignDocument parent, String viewName) throws EngineException {
		this.viewName = viewName;
		this.parent = parent;
		databaseType = "designdocumentview";
		setName(safeName(viewName));
		priority = stablePriority(parent.getQName() + "." + viewName);
	}

	@Override
	public String toString() {
		return viewName;
	}

	@Override
	public boolean isHiddenProperty(String propertyName) {
		return switch (propertyName) {
		case "comment" -> true;
		default -> super.isHiddenProperty(propertyName);
		};
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		var element = super.toXml(document);
		if (exportOptions.contains(ExportOption.bIncludeDisplayName)) {
			appendProperty(document, element, "map", "Map", getFunction("map"), false);
			appendProperty(document, element, "reduce", "Reduce", getFunction("reduce"), false);
		}
		return element;
	}

	@Override
	public boolean setDynamicProperty(String name, String value) throws EngineException {
		if (!"map".equals(name) && !"reduce".equals(name)) {
			return false;
		}
		try {
			var views = designDocument().getJSONObject().optJSONObject("views");
			if (views == null) {
				views = new JSONObject();
				designDocument().getJSONObject().put("views", views);
			}
			var view = views.optJSONObject(viewName);
			if (view == null) {
				view = new JSONObject();
				views.put(viewName, view);
			}
			if ("reduce".equals(name) && (value == null || value.isBlank())) {
				view.remove(name);
			} else {
				view.put(name, value == null ? "" : value);
			}
			designDocument().hasChanged = true;
			return true;
		} catch (Exception e) {
			throw new EngineException("Unable to update FullSync view \"" + viewName + "\".", e);
		}
	}

	private DesignDocument designDocument() {
		return (DesignDocument) parent;
	}

	private String getFunction(String name) {
		var views = designDocument().getJSONObject().optJSONObject("views");
		var view = views == null ? null : views.optJSONObject(viewName);
		return view == null ? "" : view.optString(name, "");
	}

	private static void appendProperty(Document document, Element root, String name, String label,
			String value, boolean readOnly) throws EngineException {
		var property = document.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("displayName", label);
		property.setAttribute("category", "View");
		property.setAttribute("isHidden", "false");
		property.setAttribute("isMasked", "false");
		property.setAttribute("isExpert", "false");
		property.setAttribute("isDisabled", Boolean.toString(readOnly));
		property.setAttribute("isMultiline", "true");
		property.setAttribute("shortDescription", label + " function of the FullSync view.");
		property.setAttribute("editorClass", "null");
		try {
			property.appendChild(XMLUtils.writeObjectToXml(document, value));
		} catch (Exception e) {
			throw new EngineException("Unable to expose FullSync view property \"" + name + "\".", e);
		}
		root.appendChild(property);
	}

	private static String safeName(String value) {
		var name = value == null ? "view" : value.replaceAll("[^A-Za-z0-9_]", "_");
		if (name.isBlank()) {
			name = "view";
		}
		if (!Character.isLetter(name.charAt(0)) && name.charAt(0) != '_') {
			name = "_" + name;
		}
		if (value != null && !name.equals(value)) {
			name += "_" + Integer.toHexString(value.hashCode());
		}
		return name;
	}

	private static long stablePriority(String value) {
		var hash = 1125899906842597L;
		for (var i = 0; i < value.length(); i++) {
			hash = 31 * hash + value.charAt(i);
		}
		return Math.abs(hash == Long.MIN_VALUE ? 0 : hash);
	}
}
