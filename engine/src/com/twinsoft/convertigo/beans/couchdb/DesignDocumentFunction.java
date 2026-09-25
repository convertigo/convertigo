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

/**
 * A filter, an update or the validate function of a design document, exposed as an object of the tree as
 * the Eclipse Studio shows them. Its name tells its kind, as a filter and an update can share a name.
 */
public class DesignDocumentFunction extends DatabaseObject implements IDynamicPropertyContainer {

	private static final long serialVersionUID = -4153087706187340541L;

	public static final String FILTERS = "filters";
	public static final String UPDATES = "updates";
	public static final String VALIDATE = "validate_doc_update";

	private final String kind;
	private final String functionName;

	DesignDocumentFunction(DesignDocument parent, String kind, String functionName) throws EngineException {
		this.kind = kind;
		this.functionName = functionName;
		this.parent = parent;
		databaseType = "designdocumentfunction";
		setName(VALIDATE.equals(kind) ? VALIDATE
				: DesignDocumentView.safeName((FILTERS.equals(kind) ? "filter_" : "update_") + functionName));
		priority = DesignDocumentView.stablePriority(parent.getQName() + "." + kind + "." + functionName);
	}

	@Override
	public String toString() {
		return VALIDATE.equals(kind) ? VALIDATE : functionName;
	}

	/**
	 * @return filters, updates or validate_doc_update, the key of the function in the design document
	 */
	public String getKind() {
		return kind;
	}

	/**
	 * @return the name of the filter or update function, the key of the validate function
	 */
	public String getFunctionName() {
		return functionName;
	}

	public DesignDocument getDesignDocument() {
		return (DesignDocument) parent;
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
			var property = document.createElement("property");
			property.setAttribute("name", "function");
			property.setAttribute("displayName", "Function");
			property.setAttribute("category", label());
			property.setAttribute("isHidden", "false");
			property.setAttribute("isMasked", "false");
			property.setAttribute("isExpert", "false");
			property.setAttribute("isDisabled", "false");
			property.setAttribute("isMultiline", "true");
			property.setAttribute("shortDescription", label() + " function of the design document.");
			property.setAttribute("editorClass", "null");
			try {
				property.appendChild(XMLUtils.writeObjectToXml(document, getFunction()));
			} catch (Exception e) {
				throw new EngineException("Unable to expose the function \"" + functionName + "\".", e);
			}
			element.appendChild(property);
		}
		return element;
	}

	@Override
	public boolean setDynamicProperty(String name, String value) throws EngineException {
		if (!"function".equals(name)) {
			return false;
		}
		try {
			var json = getDesignDocument().getJSONObject();
			if (VALIDATE.equals(kind)) {
				json.put(VALIDATE, value == null ? "" : value);
			} else {
				var functions = json.optJSONObject(kind);
				if (functions == null) {
					functions = new JSONObject();
					json.put(kind, functions);
				}
				functions.put(functionName, value == null ? "" : value);
			}
			getDesignDocument().hasChanged = true;
			return true;
		} catch (Exception e) {
			throw new EngineException("Unable to update the function \"" + functionName + "\".", e);
		}
	}

	private String label() {
		return switch (kind) {
		case FILTERS -> "Filter";
		case UPDATES -> "Update";
		default -> "Validate";
		};
	}

	private String getFunction() {
		var json = getDesignDocument().getJSONObject();
		if (VALIDATE.equals(kind)) {
			return json.optString(VALIDATE, "");
		}
		var functions = json.optJSONObject(kind);
		return functions == null ? "" : functions.optString(functionName, "");
	}
}
