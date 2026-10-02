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

package com.twinsoft.convertigo.engine.util;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.IStepSourcesContainer;
import com.twinsoft.convertigo.beans.core.IStepSmartTypeContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.steps.SmartType;

/** Object filters shared by the Eclipse and web Studio searches, combined with AND. */
public record DatabaseObjectSearchFilters(boolean brokenSources, boolean inactive, boolean symbols, boolean unknownSymbols) {
	public boolean hasCriteria() {
		return brokenSources || inactive || symbols || unknownSymbols;
	}

	public boolean matches(DatabaseObject databaseObject) throws Exception {
		if (databaseObject == null) {
			return false;
		}
		if (unknownSymbols && !databaseObject.isSymbolError()) {
			return false;
		}
		if (brokenSources && !hasBrokenSource(databaseObject)) {
			return false;
		}
		if (!inactive && !symbols) {
			return true;
		}
		var isInactive = false;
		var usesSymbols = databaseObject.isSymbolError();
		for (var property : CachedIntrospector.getBeanInfo(databaseObject).getPropertyDescriptors()) {
			var name = property.getName();
			if (inactive && ("isEnabled".equals(name) || "isEnable".equals(name)) && property.getReadMethod() != null) {
				isInactive |= Boolean.FALSE.equals(property.getReadMethod().invoke(databaseObject));
			}
			// Compilation keeps the original value only when it contains a global symbol,
			// including encrypted properties, nested XMLVectors and SmartType expressions.
			if (symbols && databaseObject.getCompilablePropertySourceValue(name) != null) {
				usesSymbols = true;
			}
		}
		return (!inactive || isInactive) && (!symbols || usesSymbols);
	}

	private boolean hasBrokenSource(DatabaseObject databaseObject) throws Exception {
		var owner = databaseObject instanceof Step step ? step
				: databaseObject.getParent() instanceof Step parentStep ? parentStep : null;
		if (owner == null) {
			return false;
		}
		if (databaseObject instanceof IStepSourceContainer container) {
			if (isBrokenSource(owner, container.getSourceDefinition())) return true;
		}
		// Inspect multi-source rows defensively: legacy rows can contain text instead
		// of a source definition, and this search also covers step variables.
		if (databaseObject instanceof IStepSourcesContainer container) {
			var definitions = container.getSourcesDefinition();
			if (definitions != null) {
				for (Object entry : definitions) {
					if (entry instanceof XMLVector<?> row && row.size() > 1
							&& row.get(1) instanceof XMLVector<?> definition
							&& isBrokenSource(owner, definition)) return true;
				}
			}
		}
		if (databaseObject instanceof IStepSmartTypeContainer container) {
			var smartTypes = container.getSmartTypes();
			if (smartTypes != null) {
				for (var smartType : smartTypes) {
					if (smartType != null && smartType.isUseSource()
							&& isBrokenSource(owner, smartType.getSourceDefinition())) return true;
				}
			}
		}
		// Some steps have additional source properties (connection string, dates, ...)
		// that are not exposed by Step.getSources().
		for (var property : CachedIntrospector.getBeanInfo(databaseObject).getPropertyDescriptors()) {
			var getter = property.getReadMethod();
			var editor = property.getPropertyEditorClass();
			if (getter == null) continue;
			// Editor classes are absent in a standalone engine; the web source picker
			// provides the same source-property metadata without an Eclipse dependency.
			if (XMLVector.class.isAssignableFrom(property.getPropertyType())
					&& (StepSources.handles(databaseObject, property.getName())
							|| editor != null && "StepSourceEditor".equals(editor.getSimpleName()))) {
				if (getter.invoke(databaseObject) instanceof XMLVector<?> definition
						&& isBrokenSource(owner, definition)) return true;
			} else if (SmartType.class.equals(property.getPropertyType())) {
				var smartType = (SmartType) getter.invoke(databaseObject);
				if (smartType != null && smartType.isUseSource()
						&& isBrokenSource(owner, smartType.getSourceDefinition())) return true;
			}
		}
		return false;
	}

	private boolean isBrokenSource(Step owner, XMLVector<?> definition) {
		if (definition == null || definition.isEmpty()) {
			return false;
		}
		if (!(definition.get(0) instanceof String priority)) {
			return true;
		}
		try {
			// Same target lookup as StepSource.isBroken(), without assuming vector element types.
			var sequence = owner.getParentSequence();
			return sequence == null || sequence.loadedSteps.get(Long.valueOf(priority)) == null;
		} catch (NumberFormatException e) {
			return true;
		}
	}

}
