/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU Affero General Public
 * License  as published by  the Free Software Foundation; either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FlowVirtualObjectTreeObject;

public class FlowPropertyDescriptor extends PropertyDescriptor {

	private final FlowVirtualObjectTreeObject treeObject;
	private final String propertyName;
	private final JSONObject definition;
	private final ILabelProvider labelProvider = new LabelProvider() {
		@Override
		public String getText(Object element) {
			if (!FlowPropertyCellEditor.isInlineEditable(definition)) {
				return FlowPropertyCellEditor.structuredSummary(element == null ? "" : String.valueOf(element));
			}
			return super.getText(element);
		}
	};

	public FlowPropertyDescriptor(Object id, String displayName, FlowVirtualObjectTreeObject treeObject,
			String propertyName, JSONObject definition) {
		super(id, displayName);
		this.treeObject = treeObject;
		this.propertyName = propertyName;
		this.definition = definition == null ? new JSONObject() : definition;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent) {
		var editor = new FlowPropertyCellEditor(parent, treeObject, propertyName, definition);
		if (getValidator() != null) {
			editor.setValidator(getValidator());
		}
		return editor;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return labelProvider;
	}
}
