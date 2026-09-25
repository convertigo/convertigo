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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.ngx.components.UIUseVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;

/**
 * The "Import variables" and "Import events" actions of the Eclipse Studio on the NGX components: a call
 * of a sequence gets a variable for each variable of the sequence, an invoke of a shared action or a use of
 * a shared component a variable for each of its variables, and a use of a shared component an event for
 * each of its events.
 */
class NgxImports {

	private NgxImports() {
	}

	static boolean importsVariables(DatabaseObject dbo) {
		return dbo instanceof UIUseShared || dbo instanceof UIDynamicInvoke
				|| dbo instanceof UIDynamicAction action && action.getIonBean() != null
						&& "CallSequenceAction".equals(action.getIonBean().getName());
	}

	static boolean importsEvents(DatabaseObject dbo) {
		return dbo instanceof UIUseShared;
	}

	/**
	 * @return the number of variables added to the component
	 */
	static int importVariables(DatabaseObject dbo) throws Exception {
		var added = 0;
		if (dbo instanceof UIUseShared useShared) {
			var shared = useShared.getTargetSharedComponent();
			if (shared == null) {
				throw new EngineException("This component uses no shared component.");
			}
			for (var variable : shared.getVariables()) {
				if (useShared.getVariable(variable.getName()) == null) {
					var imported = new UIUseVariable();
					imported.setName(normalized(variable.getName()));
					imported.setComment(variable.getComment());
					imported.setVarSmartType(script(variable.getVariableValue()));
					added += add(useShared, imported);
				}
			}
		} else if (dbo instanceof UIDynamicInvoke invoke) {
			var stack = invoke.getTargetSharedAction();
			if (stack == null) {
				throw new EngineException("This action invokes no shared action.");
			}
			for (var variable : stack.getVariables()) {
				if (invoke.getVariable(variable.getName()) == null) {
					var imported = new UIControlVariable();
					imported.setName(normalized(variable.getName()));
					imported.setComment(variable.getComment());
					imported.setVarSmartType(script(variable.getVariableValue()));
					added += add(invoke, imported);
				}
			}
		} else if (dbo instanceof UIDynamicAction action) {
			var target = String.valueOf(action.getIonBean().getProperty("requestable").getValue());
			var index = target.indexOf('.');
			var project = index < 0 ? null : Engine.theApp.databaseObjectsManager.getOriginalProjectByName(target.substring(0, index));
			Sequence sequence = null;
			try {
				sequence = project == null ? null : project.getSequenceByName(target.substring(index + 1));
			} catch (Exception e) {
				// no sequence
			}
			if (sequence == null) {
				throw new EngineException("This action calls no sequence.");
			}
			for (var variable : sequence.getVariablesList()) {
				if (action.getVariable(variable.getName()) == null) {
					var imported = new UIControlVariable();
					imported.setName(normalized(variable.getName()));
					imported.setComment(variable.getComment());
					imported.setVarSmartType(new MobileSmartSourceType(defaultValue(variable)));
					added += add(action, imported);
				}
			}
		}
		return added;
	}

	/**
	 * @return the number of events added to the use of a shared component
	 */
	static int importEvents(UIUseShared useShared) throws Exception {
		var shared = useShared.getTargetSharedComponent();
		if (shared == null) {
			throw new EngineException("This component uses no shared component.");
		}
		var added = 0;
		for (var event : shared.getUICompEventList()) {
			if (useShared.getEvent(event.getName()) == null) {
				var imported = new UIControlEvent();
				normalized(event.getName());
				imported.setEventName(event.getName());
				imported.setComment(event.getComment());
				added += add(useShared, imported);
			}
		}
		return added;
	}

	private static int add(UIComponent parent, UIComponent child) throws Exception {
		parent.add(child);
		child.bNew = true;
		child.hasChanged = true;
		parent.hasChanged = true;
		BuilderUtils.dboAdded(child);
		return 1;
	}

	private static String normalized(String name) throws EngineException {
		if (!StringUtils.isNormalized(name)) {
			throw new EngineException("The name \"" + name + "\" is not normalized.");
		}
		return name;
	}

	private static MobileSmartSourceType script(String value) {
		var smartType = new MobileSmartSourceType();
		smartType.setMode(MobileSmartSourceType.Mode.SCRIPT);
		smartType.setSmartValue(value);
		return smartType;
	}

	private static String defaultValue(RequestableVariable variable) {
		// a variable with a compilable value has no default value to copy
		var value = variable.getCompilablePropertySourceValue("value") == null ? variable.getDefaultValue() : null;
		return value == null ? "" : value.toString();
	}
}
