/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU Affero General Public
 * License  as published by the Free Software Foundation; either
 * version  3  of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import java.lang.reflect.InvocationTargetException;

import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FlowVirtualObjectTreeObject;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

public class FlowOutputSchemaPropertyDescriptor extends PropertyDescriptor {

	private static final String LABEL = "Open schema viewer...";
	private final FlowVirtualObjectTreeObject treeObject;

	public FlowOutputSchemaPropertyDescriptor(Object id, String displayName, FlowVirtualObjectTreeObject treeObject) {
		super(id, displayName);
		this.treeObject = treeObject;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent) {
		var editor = new FlowOutputSchemaCellEditor(parent, treeObject);
		if (getValidator() != null) {
			editor.setValidator(getValidator());
		}
		return editor;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		if (isLabelProviderSet()) {
			return super.getLabelProvider();
		}
		return new LabelProvider() {
			@Override
			public String getText(Object element) {
				return LABEL;
			}
		};
	}

	private static class FlowOutputSchemaCellEditor extends DialogCellEditor {

		private final FlowVirtualObjectTreeObject treeObject;

		FlowOutputSchemaCellEditor(Composite parent, FlowVirtualObjectTreeObject treeObject) {
			super(parent);
			this.treeObject = treeObject;
		}

		@Override
		protected void updateContents(Object value) {
			var label = getDefaultLabel();
			if (label != null) {
				label.setText(LABEL);
			}
		}

		@Override
		protected Object openDialogBox(Control cellEditorWindow) {
			var shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			var schema = new JSONObject[1];
			try {
				new ProgressMonitorDialog(shell).run(true, false, monitor -> {
					try {
						monitor.beginTask("Computing Flow output schema...", IProgressMonitor.UNKNOWN);
						schema[0] = FlowStudioSupport.outputSchema(treeObject.getObject());
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				});
				JsonInfoPropertyDescriptor.openDialog(shell, "Flow output schema", schema[0]);
			} catch (InvocationTargetException e) {
				var cause = e.getCause() == null ? e : e.getCause();
				ConvertigoPlugin.logException(cause, "Unable to compute Flow output schema.");
				MessageDialog.openError(shell, "Flow output schema", cause.getMessage());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return getValue();
		}
	}
}
