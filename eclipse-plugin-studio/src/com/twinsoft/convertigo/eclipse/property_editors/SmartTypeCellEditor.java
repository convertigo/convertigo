/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IJScriptContainer;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.eclipse.ColorEnum;
import com.twinsoft.convertigo.eclipse.editors.jscript.JScriptEditorInput;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class SmartTypeCellEditor extends AbstractDialogCellEditor implements IJScriptContainer {
	
    public SmartTypeCellEditor(Composite parent) {
    	this(parent, SWT.NONE);
    }
        
    public SmartTypeCellEditor(Composite parent, int style) {
    	super(parent, style);
    }
    
    private SmartType value;
    private List<Button> buttons;
    private Text text;
    
    private enum DataKeys {
    	TEXT_COLOR,
    	SMART_TYPE,
    	EDITOR_CLASS
    }
	
	@Override
	protected Control createControl(Composite parent) {
		final Composite control = new Composite(parent, SWT.NONE) {

			@Override
			public boolean isFocusControl() {
				return true;
			}
        	
        };
		
		Font font = parent.getFont();
		Color bg = parent.getBackground();
		
		control.setFont(font);
		control.setBackground(bg);
		
		GridLayout gl = new GridLayout(99, false);
		gl.horizontalSpacing = gl.marginHeight = gl.marginWidth = gl.verticalSpacing = 0;
		control.setLayout(gl);
		
		text = new Text(control, SWT.NONE);
		text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
        text.addKeyListener(new KeyAdapter() {
        	
            @Override
            public void keyPressed(KeyEvent keyEvent) {
				if (keyEvent.character == '\u001b') { // Escape character
					fireCancelEditor();
				} else {
					if (keyEvent.character == '\r') { // Return key
						fireApplyEditorValue();
						deactivate();
					}
				}
            }
            
        });
        
		text.addTraverseListener(new TraverseListener() {
            public void keyTraversed(TraverseEvent e) {
            	e.widget = control;
                if (e.detail == SWT.TRAVERSE_ESCAPE
                        || e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                }
            }
        });
		
		FontData fontDefaultData = font.getFontData()[0];
		fontDefaultData.setStyle(SWT.BOLD);
		fontDefaultData.setHeight(Math.round(fontDefaultData.getHeight() * 0.7f));
		final Font fontTitle = new Font(parent.getDisplay(), fontDefaultData);
		
		Button button = new Button(control, SWT.NONE);
		button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		button.setFont(fontTitle);
		button.setText("…");
		
        button.addSelectionListener(new SelectionAdapter() {
        	
            @Override
            public void widgetSelected(SelectionEvent event) {
            	openDialog();
            }
            
        });
        
		buttons = new ArrayList<Button>(3);
		SelectionListener selectionListener = new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				for (Button button : buttons) {
					button.setSelection(false);
				}
				
				Button button = (Button) e.widget; 
				button.setSelection(true);
				
				text.getDisplay().asyncExec(() -> {
					text.setBackground((Color) button.getData(DataKeys.TEXT_COLOR.name()));
					text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_BLACK));
				});
				
				Mode mode = (Mode) button.getData(DataKeys.SMART_TYPE.name());
				value.setMode(mode);
				text.setText(value.toStringContent());
				text.setEditable(mode != Mode.SOURCE);
				
				dialogCompositeClass = GenericUtils.cast(button.getData(DataKeys.EDITOR_CLASS.name()));
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		
		button = new Button(control, SWT.TOGGLE | SWT.FLAT);
		button.setForeground(ColorEnum.DARK_YELLOW.get());
		button.setData(DataKeys.TEXT_COLOR.name(), ColorEnum.LIGHT_YELLOW.get());
		button.setData(DataKeys.SMART_TYPE.name(), SmartType.Mode.PLAIN);
		button.setData(DataKeys.EDITOR_CLASS.name(), TextEditorComposite.class);
		buttons.add(button);
		
		button = new Button(control, SWT.TOGGLE | SWT.FLAT);
		button.setForeground(ColorEnum.DARK_BLUE.get());
		button.setData(DataKeys.TEXT_COLOR.name(), ColorEnum.JAVASCRIPTABLE.get());
		button.setData(DataKeys.SMART_TYPE.name(), SmartType.Mode.JS);
		button.setData(DataKeys.EDITOR_CLASS.name(), TextEditorComposite.class);
		buttons.add(button);
		
		button = new Button(control, SWT.TOGGLE | SWT.FLAT);
		button.setForeground(ColorEnum.DARK_GREEN.get());
		button.setData(DataKeys.TEXT_COLOR.name(), ColorEnum.LIGHT_GREEN.get());
		button.setData(DataKeys.SMART_TYPE.name(), SmartType.Mode.SOURCE);
		button.setData(DataKeys.EDITOR_CLASS.name(), StepSourceEditorComposite.class);
		buttons.add(button);
		
		for (Button bt : buttons) {
			Mode mode = (Mode) bt.getData(DataKeys.SMART_TYPE.name());
			bt.setText(mode.label());
			bt.setToolTipText(mode.tooltip());
			bt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			bt.setFont(fontTitle);
			bt.addSelectionListener(selectionListener);
			bt.addMouseListener(new MouseListener() {
				
				public void mouseUp(MouseEvent e) {
				}
				
				public void mouseDown(MouseEvent e) {
				}
				
				public void mouseDoubleClick(MouseEvent e) {
					openDialog();
				}
				
			});
		}
		
		return control;
	}
	
	protected void openDialog() {
		if (value.getMode() == Mode.JS) {
			try {
				JScriptEditorInput.openJScriptEditor(databaseObjectTreeObject, this);
				return;
			} catch (PartInitException e) {
				Engine.logStudio.error("failed to open editor", e);
			}
		}
    	Object newValue = openDialogBox(null);
    	
    	if (newValue != null) {
    		if (newValue instanceof String) {
    			value.setExpression((String) newValue);
    		} else {
    			value.setSourceDefinition(GenericUtils.<XMLVector<String>>cast(newValue));
    		}
			text.setText(value.toStringContent());
    	}
	}

	@Override
	protected Object doGetValue() {
		if (value.isUseExpression()) {
			value.setExpression(text.getText());
		}
		value.pack();
		return value;
	}
	
	@Override
	protected void doSetFocus() {
		text.selectAll();
		text.setFocus();
	}
	
	@Override
	protected void doSetValue(Object value) {
		this.value = ((SmartType) value).clone();
		Mode mode = this.value.getMode();
		
		for (Button button : buttons) {
			if (button.getData(DataKeys.SMART_TYPE.name()) == mode) {
				button.notifyListeners(SWT.Selection, null);
				break;
			}
		}
	}

	@Override
	public Object getEditorData() {
		return value.isUseExpression() ? value.getExpression() : value.getSourceDefinition();
	}
	
    /**
     * State information for updating action enablement
     */
    private boolean isSelection = false;

    private boolean isDeleteable = false;

    private boolean isSelectable = false;

    /**
     * Checks to see if the "deletable" state (can delete/
     * nothing to delete) has changed and if so fire an
     * enablement changed notification.
     */
    private void checkDeleteable() {
        boolean oldIsDeleteable = isDeleteable;
        isDeleteable = isDeleteEnabled();
        if (oldIsDeleteable != isDeleteable) {
            fireEnablementChanged(DELETE);
        }
    }

    /**
     * Checks to see if the "selectable" state (can select)
     * has changed and if so fire an enablement changed notification.
     */
    private void checkSelectable() {
        boolean oldIsSelectable = isSelectable;
        isSelectable = isSelectAllEnabled();
        if (oldIsSelectable != isSelectable) {
            fireEnablementChanged(SELECT_ALL);
        }
    }

    /**
     * Checks to see if the selection state (selection /
     * no selection) has changed and if so fire an
     * enablement changed notification.
     */
    private void checkSelection() {
        boolean oldIsSelection = isSelection;
        isSelection = text.getSelectionCount() > 0;
        if (oldIsSelection != isSelection) {
            fireEnablementChanged(COPY);
            fireEnablementChanged(CUT);
        }
    }
	
    /**
     * The <code>TextCellEditor</code>  implementation of this
     * <code>CellEditor</code> method returns <code>true</code> if
     * the current selection is not empty.
     */
    @Override
	public boolean isCopyEnabled() {
        if (text == null || text.isDisposed()) {
			return false;
		}
        return text.getSelectionCount() > 0;
    }

    /**
     * The <code>TextCellEditor</code>  implementation of this
     * <code>CellEditor</code> method returns <code>true</code> if
     * the current selection is not empty.
     */
    @Override
	public boolean isCutEnabled() {
        if (text == null || text.isDisposed()) {
			return false;
		}
        return text.getSelectionCount() > 0;
    }

    /**
     * The <code>TextCellEditor</code>  implementation of this
     * <code>CellEditor</code> method returns <code>true</code>
     * if there is a selection or if the caret is not positioned
     * at the end of the text.
     */
    @Override
	public boolean isDeleteEnabled() {
        if (text == null || text.isDisposed()) {
			return false;
		}
        return text.getSelectionCount() > 0
                || text.getCaretPosition() < text.getCharCount();
    }

    /**
     * The <code>TextCellEditor</code>  implementation of this
     * <code>CellEditor</code> method always returns <code>true</code>.
     */
    @Override
	public boolean isPasteEnabled() {
        if (text == null || text.isDisposed()) {
			return false;
		}
        return true;
    }

    /**
     * Check if save all is enabled
     * @return true if it is
     */
    public boolean isSaveAllEnabled() {
        if (text == null || text.isDisposed()) {
			return false;
		}
        return true;
    }

    /**
     * Returns <code>true</code> if this cell editor is
     * able to perform the select all action.
     * <p>
     * This default implementation always returns
     * <code>false</code>.
     * </p>
     * <p>
     * Subclasses may override
     * </p>
     * @return <code>true</code> if select all is possible,
     *  <code>false</code> otherwise
     */
    @Override
	public boolean isSelectAllEnabled() {
        if (text == null || text.isDisposed()) {
			return false;
		}
        return text.getCharCount() > 0;
    }

    /**
     * Processes a key release event that occurred in this cell editor.
     * <p>
     * The <code>TextCellEditor</code> implementation of this framework method
     * ignores when the RETURN key is pressed since this is handled in
     * <code>handleDefaultSelection</code>.
     * An exception is made for Ctrl+Enter for multi-line texts, since
     * a default selection event is not sent in this case.
     * </p>
     *
     * @param keyEvent the key event
     */
    @Override
	protected void keyReleaseOccured(KeyEvent keyEvent) {
        if (keyEvent.character == '\r') { // Return key
            // Enter is handled in handleDefaultSelection.
            // Do not apply the editor value in response to an Enter key event
            // since this can be received from the IME when the intent is -not-
            // to apply the value.
            // See bug 39074 [CellEditors] [DBCS] canna input mode fires bogus event from Text Control
            //
            // An exception is made for Ctrl+Enter for multi-line texts, since
            // a default selection event is not sent in this case.
            if (text != null && !text.isDisposed()
                    && (text.getStyle() & SWT.MULTI) != 0) {
                if ((keyEvent.stateMask & SWT.CTRL) != 0) {
                    super.keyReleaseOccured(keyEvent);
                }
            }
            return;
        }
        super.keyReleaseOccured(keyEvent);
    }

    /**
     * The <code>TextCellEditor</code> implementation of this
     * <code>CellEditor</code> method copies the
     * current selection to the clipboard.
     */
    @Override
	public void performCopy() {
        text.copy();
    }

    /**
     * The <code>TextCellEditor</code> implementation of this
     * <code>CellEditor</code> method cuts the
     * current selection to the clipboard.
     */
    @Override
	public void performCut() {
        text.cut();
        checkSelection();
        checkDeleteable();
        checkSelectable();
    }

    /**
     * The <code>TextCellEditor</code> implementation of this
     * <code>CellEditor</code> method deletes the
     * current selection or, if there is no selection,
     * the character next character from the current position.
     */
    @Override
	public void performDelete() {
        if (text.getSelectionCount() > 0) {
			// remove the contents of the current selection
            text.insert(""); //$NON-NLS-1$
		} else {
            // remove the next character
            int pos = text.getCaretPosition();
            if (pos < text.getCharCount()) {
                text.setSelection(pos, pos + 1);
                text.insert(""); //$NON-NLS-1$
            }
        }
        checkSelection();
        checkDeleteable();
        checkSelectable();
    }

    /**
     * The <code>TextCellEditor</code> implementation of this
     * <code>CellEditor</code> method pastes the
     * the clipboard contents over the current selection.
     */
    @Override
	public void performPaste() {
        text.paste();
        checkSelection();
        checkDeleteable();
        checkSelectable();
    }

    /**
     * The <code>TextCellEditor</code> implementation of this
     * <code>CellEditor</code> method selects all of the
     * current text.
     */
    @Override
	public void performSelectAll() {
        text.selectAll();
        checkSelection();
        checkDeleteable();
    }
    
	@Override
	public String getExpression() {
		return value.getExpression();
	}

	@Override
	public void setExpression(String expression) {
		if (value.getMode() == Mode.JS) {
			SmartType st = value.clone();
			st.setExpression(expression);
			databaseObjectTreeObject.setPropertyValue(propertyDescriptor.getId(), st);
		}
	}

	@Override
	public String getName() {
		return databaseObjectTreeObject.getName() + ":" + propertyDescriptor.getDisplayName();
	}

	@Override
	public DatabaseObject getDatabaseObject() {
		return databaseObjectTreeObject.getObject();
	}
}
