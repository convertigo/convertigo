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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.rest.BodyParameter;
import com.twinsoft.convertigo.beans.rest.FormParameter;
import com.twinsoft.convertigo.beans.rest.HeaderParameter;
import com.twinsoft.convertigo.beans.rest.QueryParameter;
import com.twinsoft.convertigo.beans.steps.AttributeStep;
import com.twinsoft.convertigo.beans.steps.ElementStep;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.beans.variables.RequestableHttpMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.StepMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.beans.variables.TestCaseMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.EngineException;

/**
 * The "Change to" actions of the tree of the Eclipse Studio: an object is replaced, at its position and
 * with its name, by an object of another class that keeps the properties both classes have, and its
 * children. The steps using the replaced step as source use the new one.
 */
class ChangeTo {
	private static final Set<String> NOT_COPIED = Set.of("name", "priority", "parent", "QName", "qName");

	/** For each class, the classes an object of this class can change to, with their label */
	private static final Map<Class<? extends DatabaseObject>, Map<Class<? extends DatabaseObject>, String>> TARGETS = new LinkedHashMap<>();

	static {
		// the most specific classes first
		target(RequestableHttpMultiValuedVariable.class, RequestableHttpVariable.class, "single-valued variable");
		target(RequestableHttpVariable.class, RequestableHttpMultiValuedVariable.class, "multi-valued variable");
		target(RequestableMultiValuedVariable.class, RequestableVariable.class, "single-valued variable");
		target(RequestableVariable.class, RequestableMultiValuedVariable.class, "multi-valued variable");
		target(StepMultiValuedVariable.class, StepVariable.class, "single-valued variable");
		target(StepVariable.class, StepMultiValuedVariable.class, "multi-valued variable");
		target(TestCaseMultiValuedVariable.class, TestCaseVariable.class, "single-valued variable");
		target(TestCaseVariable.class, TestCaseMultiValuedVariable.class, "multi-valued variable");
		for (var parameter : List.of(BodyParameter.class, FormParameter.class, HeaderParameter.class, QueryParameter.class)) {
			for (var other : List.of(BodyParameter.class, FormParameter.class, HeaderParameter.class, QueryParameter.class)) {
				if (other != parameter) {
					target(parameter, other, other.getSimpleName().replace("Parameter", "").toLowerCase() + " parameter");
				}
			}
		}
		target(XMLElementStep.class, XMLAttributeStep.class, "XML attribute step");
		target(XMLElementStep.class, ElementStep.class, "jElement step");
		target(XMLAttributeStep.class, XMLElementStep.class, "XML element step");
		target(XMLAttributeStep.class, AttributeStep.class, "jAttribute step");
		target(ElementStep.class, AttributeStep.class, "jAttribute step");
		target(ElementStep.class, XMLElementStep.class, "XML element step");
		target(AttributeStep.class, ElementStep.class, "jElement step");
		target(AttributeStep.class, XMLAttributeStep.class, "XML attribute step");
	}

	private ChangeTo() {
	}

	private static void target(Class<? extends DatabaseObject> source, Class<? extends DatabaseObject> target, String label) {
		TARGETS.computeIfAbsent(source, key -> new LinkedHashMap<>()).put(target, label);
	}

	/**
	 * @return the classes the object can change to, with their label
	 */
	static Map<Class<? extends DatabaseObject>, String> targets(DatabaseObject dbo) {
		var targets = TARGETS.get(dbo.getClass());
		return targets == null ? Map.of() : targets;
	}

	/**
	 * Replaces the object by an object of the target class, which must be one of its targets.
	 *
	 * @return the new object
	 */
	static DatabaseObject run(DatabaseObject old, String targetName) throws Exception {
		Class<? extends DatabaseObject> targetClass = null;
		for (var target : targets(old).keySet()) {
			if (target.getSimpleName().equals(targetName)) {
				targetClass = target;
			}
		}
		if (targetClass == null) {
			throw new EngineException("The object " + old.getName() + " cannot change to " + targetName + ".");
		}
		var parent = old.getParent();
		var replacement = targetClass.getConstructor().newInstance();
		if (!DatabaseObjectsManager.acceptDatabaseObjects(parent, replacement)) {
			throw new EngineException("A " + parent.getClass().getSimpleName() + " cannot hold a " + targetClass.getSimpleName() + ".");
		}
		List<DatabaseObject> children = new ArrayList<>(old.getDatabaseObjectChildren());
		for (var child : children) {
			if (!DatabaseObjectsManager.acceptDatabaseObjects(replacement, child)) {
				throw new EngineException("A " + targetClass.getSimpleName() + " cannot hold the " + child.getName() + " child of " + old.getName() + ".");
			}
		}

		copyProperties(old, replacement);
		replacement.bNew = true;
		replacement.hasChanged = true;
		var oldPriority = old.priority;
		if (parent instanceof UrlMappingOperation operation) {
			operation.changeTo(replacement);
		} else {
			parent.add(replacement);
			try {
				parent.getClass().getMethod("insertAtOrder", DatabaseObject.class, long.class).invoke(parent, replacement, oldPriority);
			} catch (NoSuchMethodException e) {
				// the parent does not order its children
			}
		}
		for (var child : children) {
			child.delete();
			replacement.add(child);
		}
		old.delete();
		replacement.setName(old.getName());
		if (replacement instanceof Step step && old instanceof Step && step.getSequence() != null) {
			// the steps using the old step as source use the new one
			step.getSequence().fireStepMoved(new StepEvent(step, String.valueOf(oldPriority)));
		}
		parent.hasChanged = true;
		return replacement;
	}

	/**
	 * Copies the properties both classes have, with a compatible type.
	 */
	private static void copyProperties(DatabaseObject from, DatabaseObject to) throws Exception {
		Map<String, PropertyDescriptor> source = new LinkedHashMap<>();
		for (var pd : Introspector.getBeanInfo(from.getClass()).getPropertyDescriptors()) {
			source.put(pd.getName(), pd);
		}
		for (var pd : Introspector.getBeanInfo(to.getClass()).getPropertyDescriptors()) {
			var from_pd = source.get(pd.getName());
			if (NOT_COPIED.contains(pd.getName()) || from_pd == null || from_pd.getReadMethod() == null
					|| pd.getWriteMethod() == null) {
				continue;
			}
			var type = pd.getWriteMethod().getParameterTypes()[0];
			var value = from_pd.getReadMethod().invoke(from);
			if (value == null || wrap(type).isInstance(value)) {
				try {
					pd.getWriteMethod().invoke(to, value);
				} catch (Exception e) {
					// a property that does not apply to the new object keeps its default value
				}
			}
		}
	}

	private static Class<?> wrap(Class<?> type) {
		if (!type.isPrimitive()) {
			return type;
		}
		return switch (type.getName()) {
		case "boolean" -> Boolean.class;
		case "int" -> Integer.class;
		case "long" -> Long.class;
		case "double" -> Double.class;
		case "float" -> Float.class;
		case "short" -> Short.class;
		case "byte" -> Byte.class;
		default -> Character.class;
		};
	}
}
