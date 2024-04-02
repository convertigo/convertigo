/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.property_editors;

import java.beans.PropertyDescriptor;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileInPlaceEditorInput;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.eclipse.ColorEnum;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.ConvertigoTypeScriptDefinition;


public class TextGenericCellEditor extends TextCellEditor {

	private Composite editor;
	private Button button;
	protected DatabaseObjectTreeObject databaseObjectTreeObject;
	protected PropertyDescriptor propertyDescriptor;
	protected IWorkbenchPage activePage;
	private IPropertyListener listener;
	protected FileInPlaceEditorInput input;
	protected IEditorPart editorPart;

	public TextGenericCellEditor(Composite parent, int style, DatabaseObjectTreeObject databaseObjectTreeObject, PropertyDescriptor propertyDescriptor) {
		super(parent, style);
		this.databaseObjectTreeObject = databaseObjectTreeObject;
		this.propertyDescriptor = propertyDescriptor;
		
		activePage = PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage();
	}

	@Override
	protected Control createControl(Composite parent) {
		Font font = parent.getFont();
		Color bg = parent.getBackground();

		editor = new Composite(parent, getStyle()) {
			@Override
			public boolean isFocusControl() {
				return true;
			}
		};
		editor.setFont(font);
		editor.setBackground(bg);

		GridLayout gl = new GridLayout(99, false);
		gl.horizontalSpacing = gl.marginHeight = gl.marginWidth = gl.verticalSpacing = 0;
		editor.setLayout(gl);

		super.createControl(editor);

		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		for (Listener l: text.getListeners(SWT.FocusOut)) {
			text.removeListener(SWT.FocusOut, l);
		}
		
		if ((text.getStyle() & SWT.MULTI) != 0) {
			button = new Button(editor, SWT.PUSH);
			button.setToolTipText("Open in editor");
			button.setText("…");
			button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			
			button.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent event) {}
				
				public void widgetSelected(SelectionEvent event) {
					openEditor();
				}
			});
	
			button.addKeyListener(new KeyListener() {
				public void keyPressed(KeyEvent keyEvent) {}
				
				public void keyReleased(KeyEvent keyEvent) {
					if (keyEvent.character == '\u001b' || // Escape character
							keyEvent.character == '\r') { // Return key
						fireCancelEditor();
					}
				}
			});
		}
		
		parent.getDisplay().asyncExec(() -> {
			if (Boolean.TRUE.equals(propertyDescriptor.getValue(MySimpleBeanInfo.SCRIPTABLE))) {
				text.setForeground(ColorEnum.BLACK.get());
				text.setBackground(ColorEnum.JAVASCRIPTABLE.get());
			}
		});

		return editor;
	}

	@Override
	protected Object doGetValue() {
		return text.getText();
	}

	@Override
	protected void doSetValue(Object value) {
		if (value == null) {
			text.setText("");
		} else {
			text.setText(value.toString());
		}
	}
	
	@Override
	public void setFocus() {
		editorPart = activePage.findEditor(getInput());
		if (editorPart != null) {
			setEditable(false);
			editorPart.removePropertyListener(getListener());
			editorPart.addPropertyListener(getListener());
		} else {
			setEditable(true);
		}
		
		super.setFocus();
	}
	
	@Override
	protected void keyReleaseOccured(KeyEvent keyEvent) {
		if (keyEvent.character == '\r') { // Return key
			if (text != null && !text.isDisposed()
					&& (text.getStyle() & SWT.MULTI) != 0) {
				if ((keyEvent.stateMask & SWT.CTRL) == 0) {
					keyEvent.doit = false;
					fireApplyEditorValue();
					deactivate();
				}
			}
			return;
		}
		if (keyEvent.character == '\u001b') { // Escape character
			fireCancelEditor();
		}
	}
	
	protected void setEditable(boolean editable) {
		text.setEditable(editable);
	}
	
	protected void setNewValue(Object newValue) {
		databaseObjectTreeObject.setPropertyValue((String) propertyDescriptor.getName(), newValue);
		databaseObjectTreeObject.hasBeenModified(true);
	}
	
	protected String editorInitValue() {
		return getValue().toString();
	}
	
	protected void openEditor() {
		try {
			getInput();
			String ext = input.getFile().getFileExtension();
			var value = editorInitValue();
			if ("js".equals(ext)) {
				var variables = ". Available variables: log, dom, context";
				DatabaseObject dbo = databaseObjectTreeObject.getObject();
				if (dbo instanceof Step) {
					dbo = ((Step) dbo).getSequence();
				}
				if (dbo instanceof IVariableContainer) {
					IVariableContainer vc = (IVariableContainer) dbo;
					StringBuilder sb = new StringBuilder();
					for (Variable v: vc.getVariables()) {
						variables += ", " + v.getName();
						sb.append("declare var ").append(v.getName()).append(v.isMultiValued() ? ": Array<string>" : ": string\n");
					}
					SwtUtils.fillFile(((IFolder) input.getFile().getParent()).getFile("variables.d.ts"), sb.toString());
				}
				if (dbo instanceof Transaction) {
					value = "// Copilot helper: this is a javascript script executed with RhinoJS by a Convertigo Transaction over the JVM" + variables + "\n" + value;
				} else {
					value = "// Copilot helper: this is a javascript script executed with RhinoJS by a Convertigo Sequence over the JVM" + variables + "\n" + value;
				}
				String conf = "{\"compilerOptions\": {\"module\": \"es6\", \"target\": \"es6\"},\n" + 
						"  \"include\": [\"" + ConvertigoTypeScriptDefinition.getDeclarationFile().getAbsolutePath().replace('\\', '/') + "\", \"*\"]}";
				SwtUtils.fillFile(((IFolder) input.getFile().getParent()).getFile("jsconfig.json"), conf);
			}
			SwtUtils.fillFile(input.getFile(), value);
			setEditable(false);
			
			if ("md".equals(ext)) {
				try {
					editorPart = activePage.openEditor(input, "org.eclipse.mylyn.wikitext.ui.editor.markupEditor", true);
				} catch (Exception e) {
					editorPart = activePage.openEditor(input, "org.eclipse.ui.genericeditor.GenericEditor", true);
				}
			} else {
				editorPart = activePage.openEditor(input, "org.eclipse.ui.genericeditor.GenericEditor", true);
			}
			editorPart.removePropertyListener(getListener());
			editorPart.addPropertyListener(listener);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}
	
	protected FileInPlaceEditorInput getInput() {
		if (input == null) {
			String extension = (propertyDescriptor != null &&
					propertyDescriptor.getValue(MySimpleBeanInfo.GENERIC_EDITOR_EXTENSION) != null) ?
							propertyDescriptor.getValue(MySimpleBeanInfo.GENERIC_EDITOR_EXTENSION).toString() : 
								Boolean.TRUE.equals(propertyDescriptor.getValue(MySimpleBeanInfo.SCRIPTABLE)) ? "js" : "txt";
			DatabaseObject dbo = databaseObjectTreeObject.getObject();
			IFile file = databaseObjectTreeObject.getProjectTreeObject().getFile(
					"_private/editor/" + dbo.getShortQName() + "-" + propertyDescriptor.getName() + "/" +
					dbo.getName() + "-" + propertyDescriptor.getName() + "." + extension);
			input = new FileInPlaceEditorInput(file);
		}
		return input;
	}
	
	private IPropertyListener getListener() {
		if (listener == null) {
			listener = (Object source, int propId) -> {
				if (propId == IEditorPart.PROP_DIRTY && !((IEditorPart) source).isDirty()) {
					try (InputStream is = getInput().getFile().getContents()) {
						var expression = IOUtils.toString(is, "UTF-8");
						expression = expression.replaceFirst("// Copilot helper:.*\n", "");
						setNewValue(expression);
					} catch (Exception e) {
						Engine.logStudio.error("Failed to save " + getInput().getFile().getName(), e);
					}
				}
			};
		}
		return listener;
	}
}
