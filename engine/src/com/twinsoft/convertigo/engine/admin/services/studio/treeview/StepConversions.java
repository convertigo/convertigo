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

package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import java.util.ArrayList;
import java.util.List;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.ElementStep;
import com.twinsoft.convertigo.beans.steps.JsonArrayStep;
import com.twinsoft.convertigo.beans.steps.JsonFieldStep;
import com.twinsoft.convertigo.beans.steps.JsonObjectStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLComplexStep;
import com.twinsoft.convertigo.beans.steps.XMLConcatStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.engine.enums.JsonFieldType;
import com.twinsoft.convertigo.engine.util.StringUtils;

/**
 * The "Change to" actions of the Eclipse Studio between the XML, the JSON and the concat steps: the node
 * name, text and source of a step become the key and value of the other. An XML step keeps the type and
 * the original key of a JSON step in its "type" and "originalKeyName" attributes, and a JSON step reads
 * them back.
 */
class StepConversions {
	private static final String TYPE = "type";
	private static final String ORIGINAL_KEY_NAME = "originalKeyName";

	private StepConversions() {
	}

	/**
	 * @return whether the change needs a conversion of this class
	 */
	static boolean handles(DatabaseObject old, Class<? extends DatabaseObject> target) {
		return isJson(old.getClass()) || isJson(target) || old instanceof XMLConcatStep
				|| target == XMLConcatStep.class;
	}

	private static boolean isJson(Class<?> type) {
		return type == JsonArrayStep.class || type == JsonObjectStep.class || type == JsonFieldStep.class;
	}

	private static boolean isJsonContainer(DatabaseObject step) {
		return step instanceof JsonArrayStep || step instanceof JsonObjectStep;
	}

	/**
	 * @return whether the target is a JSON step, or an XML step made from one, that reads the attributes of
	 *         the old step
	 */
	private static boolean toJson(DatabaseObject old, Class<?> target) {
		return isJson(target) || target == XMLComplexStep.class && isJsonContainer(old);
	}

	private static boolean readAttribute(DatabaseObject old, Class<?> target, DatabaseObject child) {
		return toJson(old, target) && child instanceof XMLAttributeStep attribute
				&& (TYPE.equals(attribute.getNodeName()) || ORIGINAL_KEY_NAME.equals(attribute.getNodeName()));
	}

	/**
	 * Sets the properties of the new step from the old one.
	 */
	static void convert(Step old, Step replacement) throws Exception {
		if (replacement instanceof JsonFieldStep field) {
			field.getKey().setExpression(old.getStepNodeName());
			var value = field.getValue();
			if (old instanceof XMLElementStep element) {
				if (!element.getSourceDefinition().isEmpty()) {
					value.setSourceDefinition(element.getSourceDefinition());
					value.setMode(Mode.SOURCE);
				} else {
					value.setExpression(element.getNodeText());
				}
			} else if (old instanceof ElementStep element) {
				if (!blank(element.getExpression())) {
					value.setExpression(element.getExpression());
					value.setMode(Mode.JS);
				} else {
					value.setExpression(element.getNodeText());
				}
			}
		} else if (isJsonContainer(replacement) && !isJsonContainer(old)) {
			key(replacement).setExpression(old.getStepNodeName());
		} else if (replacement instanceof XMLComplexStep complex && isJsonContainer(old)) {
			complex.setNodeName(StringUtils.normalize(key(old).toStringContent()));
		} else if (old instanceof JsonFieldStep field) {
			var value = field.getValue();
			var nodeName = StringUtils.normalize(field.getKey().toStringContent());
			if (replacement instanceof XMLElementStep element) {
				if (value.isUseSource()) {
					element.setSourceDefinition(value.getSourceDefinition());
				} else {
					element.setNodeText(value.toStringContent());
				}
				element.setNodeName(nodeName);
			} else if (replacement instanceof ElementStep element) {
				if (value.getMode() == Mode.JS) {
					element.setExpression(value.toStringContent());
				} else if (value.getMode() == Mode.PLAIN) {
					element.setNodeText(value.toStringContent());
				}
				element.setNodeName(nodeName);
			}
		} else if (old instanceof XMLConcatStep concat && replacement instanceof XMLElementStep element) {
			var sourceDefinition = new XMLVector<String>();
			var nodeText = new StringBuilder();
			var sources = concat.getSourcesDefinition();
			for (int i = 0; i < sources.size(); i++) {
				var source = sources.get(i);
				if (source.size() < 3) {
					continue;
				}
				nodeText.append(source.get(2)).append(i < sources.size() - 1 ? concat.getSeparator() : "");
				if (sourceDefinition.isEmpty() && source.get(1) instanceof XMLVector<?> definition
						&& !definition.isEmpty()) {
					for (var part : definition) {
						sourceDefinition.add(String.valueOf(part));
					}
				}
			}
			element.setNodeName(concat.getNodeName());
			element.setNodeText(nodeText.toString());
			element.setSourceDefinition(sourceDefinition);
		} else if (old instanceof XMLElementStep element && replacement instanceof XMLConcatStep concat) {
			var source = new XMLVector<Object>();
			source.add("description");
			source.add(element.getSourceDefinition());
			source.add(element.getNodeText());
			var sources = new XMLVector<XMLVector<Object>>();
			sources.add(source);
			concat.setNodeName(element.getNodeName());
			concat.setSourcesDefinition(sources);
		}
		if (toJson(old, replacement.getClass())) {
			// the attributes an XML step made from a JSON step keeps
			var key = isJsonContainer(old) && isJsonContainer(replacement) ? null : key(replacement);
			for (var child : old.getDatabaseObjectChildren()) {
				if (child instanceof XMLAttributeStep attribute && !blank(attribute.getNodeText())) {
					if (ORIGINAL_KEY_NAME.equals(attribute.getNodeName()) && key != null) {
						key.setExpression(attribute.getNodeText());
					} else if (TYPE.equals(attribute.getNodeName()) && replacement instanceof JsonFieldStep field) {
						field.setType(JsonFieldType.parse(attribute.getNodeText()));
					}
				}
			}
		}
	}

	/**
	 * @return the children of the old step the new one keeps, without the attributes read by a JSON step
	 */
	static List<DatabaseObject> kept(Step old, Class<?> target, List<DatabaseObject> children) {
		var kept = new ArrayList<DatabaseObject>();
		if (StepWithExpressions.class.isAssignableFrom(target)) {
			for (var child : children) {
				if (!readAttribute(old, target, child)) {
					kept.add(child);
				}
			}
		}
		return kept;
	}

	/**
	 * @return the number of children of the old step the new one cannot hold, deleted with the old step
	 */
	static int lost(Step old, Class<?> target) throws Exception {
		var children = old.getDatabaseObjectChildren();
		var read = 0;
		for (var child : children) {
			read += readAttribute(old, target, child) ? 1 : 0;
		}
		return children.size() - read - kept(old, target, children).size();
	}

	/**
	 * @return the attributes an XML step made from a JSON step adds first, for its type and original key
	 */
	static List<Step> added(Step old, Step replacement) throws Exception {
		var added = new ArrayList<Step>();
		String type = null;
		String key = null;
		if (replacement instanceof XMLComplexStep && isJsonContainer(old)) {
			type = old instanceof JsonArrayStep ? "array" : "object";
			key = key(old).toStringContent();
		} else if ((replacement instanceof XMLElementStep || replacement instanceof ElementStep)
				&& old instanceof JsonFieldStep field) {
			type = field.getType() == JsonFieldType.string ? null : field.getType().toString();
			key = field.getKey().toStringContent();
		}
		if (type != null) {
			added.add(attribute(old, TYPE, type));
		}
		if (key != null && !StringUtils.normalize(key).equals(key)) {
			added.add(attribute(old, ORIGINAL_KEY_NAME, key));
		}
		return added;
	}

	private static XMLAttributeStep attribute(Step old, String name, String text) throws Exception {
		var attribute = new XMLAttributeStep();
		attribute.bNew = true;
		attribute.setOutput(old.isOutput());
		attribute.setEnabled(old.isEnabled());
		attribute.setName(name);
		attribute.setNodeName(name);
		attribute.setNodeText(text);
		return attribute;
	}

	private static boolean blank(String text) {
		return text == null || text.isBlank();
	}

	private static SmartType key(DatabaseObject step) {
		if (step instanceof JsonArrayStep array) {
			return array.getKey();
		}
		if (step instanceof JsonObjectStep object) {
			return object.getKey();
		}
		return step instanceof JsonFieldStep field ? field.getKey() : null;
	}
}
