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

package com.twinsoft.convertigo.engine.admin.services.studio.properties;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.transactions.JsonHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.engine.enums.Visibility;

/**
 * The choices of the integer properties the Eclipse Studio edits with its own tag or flag editors, which an
 * engine without the Studio does not have: the property shows its tag, and a tag set gives back its index.
 */
class PropertyTags {

	/**
	 * The text properties whose choices the Eclipse Studio proposes in a combo that also takes a typed text
	 * (StringComboBoxPropertyDescriptor, PropertyWithDynamicTagsEditor), by the class declaring them
	 */
	private static final java.util.Set<String> FREE_TEXT = java.util.Set.of("ApplicationComponent.splitPaneLayout",
			"ApplicationComponent.tplProjectName", "PageComponent.changeDetection", "PageComponent.icon",
			"PageComponent.iconPosition", "PageComponent.preloadPriority", "RouteActionComponent.action",
			"RouteFullsyncEvent.verb", "UIAnimation.animationName", "UIAppEvent.appEvent", "UIAppGuard.guardType",
			"UIControlDirective.directiveName", "UIControlEvent.eventName", "UIDynamicMenuItem.itemicon",
			"UIDynamicMenuItem.itemiconPos", "UIFontStyle.fontFamily", "UIFontStyle.fontSize", "UIFontStyle.fontStyle",
			"UIFontStyle.fontWeight", "UIFontStyle.ruleTargets", "UIFormControlValidator.email",
			"UIFormControlValidator.maxLength", "UIFormControlValidator.minLength", "UIFormControlValidator.pattern",
			"UIFormControlValidator.required", "UIFormControlValidator.requiredTrue", "UIPageEvent.viewEvent",
			"UISharedComponentEvent.componentEvent", "UIUseVariable.binding", "UIDynamicTag.tagName",
			"Connector.endTransactionName");

	private PropertyTags() {
	}

	/**
	 * @return the models a REST body parameter or response can reference, as the model editor of the
	 *         Eclipse Studio lists them: the models of the URL mapper and the definitions of the referenced
	 *         OpenAPI schemas; null for another property
	 */
	static JSONArray models(DatabaseObject dbo, String property) throws Exception {
		if (!"modelReference".equals(property) || !(dbo instanceof com.twinsoft.convertigo.beans.rest.BodyParameter
				|| dbo instanceof com.twinsoft.convertigo.beans.rest.AbstractRestResponse)) {
			return null;
		}
		var models = new java.util.TreeSet<String>();
		var project = dbo.getProject();
		var mapper = project.getUrlMapper();
		try {
			var mapperModels = mapper == null ? "" : mapper.getModels();
			var json = mapperModels.isEmpty() ? new org.codehaus.jettison.json.JSONObject()
					: new org.codehaus.jettison.json.JSONObject(mapperModels);
			for (var it = json.keys(); it.hasNext();) {
				models.add((String) it.next());
			}
		} catch (Exception e) {
			// no model
		}
		try {
			var projectName = project.getName();
			com.twinsoft.convertigo.engine.servlets.RestApiServlet.buildSwaggerDefinition(projectName, false);
			var dir = new java.io.File(com.twinsoft.convertigo.engine.Engine.projectDir(projectName),
					com.twinsoft.convertigo.engine.util.OpenApiUtils.jsonSchemaDirectory);
			var files = dir.listFiles((d, name) -> name.endsWith(".jsonschema") && !name.equals(projectName + ".jsonschema"));
			for (var file : files == null ? new java.io.File[0] : files) {
				var json = new org.codehaus.jettison.json.JSONObject(
						java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8));
				var definitions = json.optJSONObject("definitions");
				for (var it = definitions == null ? java.util.Collections.emptyIterator() : definitions.keys(); it.hasNext();) {
					models.add(file.getName() + "#/definitions/" + it.next());
				}
			}
		} catch (Exception e) {
			// no referenced definition
		}
		return new JSONArray(models);
	}

	/**
	 * @return whether the property takes a typed text as well as one of its choices
	 */
	static boolean freeText(DatabaseObject dbo, String property) {
		for (Class<?> type = dbo.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
			if (FREE_TEXT.contains(type.getSimpleName() + "." + property)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the tags of the property, its value being the index of one, or null
	 */
	static String[] tags(DatabaseObject dbo, String property) {
		return switch (property) {
		case "sheetLocation" -> dbo instanceof Sequence ? new String[] { "None", "From sequence" }
				: dbo instanceof Transaction
						? new String[] { "None", "From transaction", "From last detected screen class" }
						: dbo instanceof RequestableObject
								? new String[] { "None", "From requested object", "From last detected object" }
								: null;
		case "xmlOutput" -> dbo instanceof SqlTransaction
				? new String[] { "RAW", "AUTO", "ELEMENT", "ELEMENT_WITH_ATTRIBUTES", "FLAT_ELEMENT" }
				: null;
		case "autoCommit" -> dbo instanceof SqlTransaction
				? new String[] { "disabled, manual commit", "enabled, after each query", "enabled, once at the end" }
				: null;
		case "jsonArrayTranslationPolicy" -> dbo instanceof JsonHttpTransaction
				? JsonHttpTransaction.JSON_ARRAY_TRANSLATION_POLICY
				: null;
		case "dataEncoding" -> dbo instanceof HttpTransaction ? new String[] { "string", "base64" } : null;
		default -> null;
		};
	}

	/**
	 * @return the flags of the property, its value being a mask of them, or null
	 */
	static JSONArray flags(DatabaseObject dbo, String property) throws Exception {
		if (!(dbo instanceof Variable) || !"visibility".equals(property)) {
			return null;
		}
		return new JSONArray()
				.put(new JSONObject().put("label", "Mask in the log files").put("mask", Visibility.Logs.getMask()))
				.put(new JSONObject().put("label", "Mask in the Studio").put("mask", Visibility.Studio.getMask()))
				.put(new JSONObject().put("label", "Mask in the platform").put("mask", Visibility.Platform.getMask()))
				.put(new JSONObject().put("label", "Mask in the XML files of the project").put("mask",
						Visibility.XmlFile.getMask()));
	}

	/**
	 * @return the index of the tag the Studio sets, or the value when it is not a tag
	 */
	static String index(DatabaseObject dbo, String property, String value) {
		var tags = tags(dbo, property);
		if (tags != null) {
			for (var i = 0; i < tags.length; i++) {
				if (tags[i].equals(value)) {
					return Integer.toString(i);
				}
			}
		}
		return value;
	}
}
