package com.twinsoft.convertigo.eclipse.property_editors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.eclipse.ColorEnum;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class MobileSmartSourceTypeCellEditor extends AbstractDialogCellEditor {
	
    private MobileSmartSourceType msst;
    private Button dotButton;
    private List<Button> buttons;
	private String[] items;
	private CCombo comboBox;
	int selection;
    
    private enum DataKeys {
    	TEXT_COLOR,
    	SMART_TYPE,
    	EDITOR_CLASS
    }

    public MobileSmartSourceTypeCellEditor(Composite parent, int style) {
    	this(parent, style, new String[] {});
    }
    
    public MobileSmartSourceTypeCellEditor(Composite parent, int style, String[] items) {
    	super(parent, style);
    	this.dialogTitle = "MobileComponent Source";
		this.items = items;
		populateComboBoxItems();
    }
    
	@Override
	protected Control createControl(Composite parent) {
		final boolean itemsReadOnly = getStyle() == SWT.READ_ONLY;
		final Composite control = new Composite(parent, SWT.NONE);
		
		Font font = parent.getFont();
		Color bg = parent.getBackground();
		
		control.setFont(font);
		control.setBackground(bg);
		
		GridLayout gl = new GridLayout(99, false);
		gl.horizontalSpacing = gl.marginHeight = gl.marginWidth = gl.verticalSpacing = 0;
		control.setLayout(gl);
		
		comboBox = new CCombo(control, SWT.NONE);
		comboBox.setFont(control.getFont());
		comboBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		populateComboBoxItems();

		comboBox.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				keyReleaseOccured(e);
			}
		});

		comboBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				applyEditorValueAndDeactivate();
			}

			@Override
			public void widgetSelected(SelectionEvent event) {
				selection = comboBox.getSelectionIndex();
			}
		});

		comboBox.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE
						|| e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
				}
			}
		});

		comboBox.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				//MobileSmartSourceTypeCellEditor.this.focusLost();
			}
		});
		
		FontData fontDefaultData = font.getFontData()[0];
		fontDefaultData.setStyle(SWT.BOLD);
		fontDefaultData.setHeight(Math.round(fontDefaultData.getHeight() * 0.7f));
		final Font fontTitle = new Font(parent.getDisplay(), fontDefaultData);
		
		dotButton = new Button(control, SWT.NONE);
		dotButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		dotButton.setFont(fontTitle);
		dotButton.setText("…");
		
		dotButton.addSelectionListener(new SelectionAdapter() {
        	
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
				
				comboBox.setBackground((Color) button.getData(DataKeys.TEXT_COLOR.name()));
				
				Mode mode = (Mode) button.getData(DataKeys.SMART_TYPE.name());
				msst.setMode(mode);
				
				if (Mode.PLAIN.equals(mode)) {
					populateComboBoxItems();
				} else {
					comboBox.removeAll();
					comboBox.add("");
					selection = -1;
				}
				
				comboBox.setText(msst.getValue());
				comboBox.setEditable(!(Mode.PLAIN.equals(mode) && itemsReadOnly) || Mode.SCRIPT.equals(mode));
				comboBox.setEnabled(!Mode.SOURCE.equals(mode));
				
				dotButton.setEnabled(!(Mode.PLAIN.equals(mode) && itemsReadOnly));
				
				dialogCompositeClass = GenericUtils.cast(button.getData(DataKeys.EDITOR_CLASS.name()));
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		
		Button button = null;
		
		button = new Button(control, SWT.TOGGLE | SWT.FLAT);
		button.setForeground(ColorEnum.DARK_YELLOW.get());
		button.setData(DataKeys.TEXT_COLOR.name(), ColorEnum.LIGHT_YELLOW.get());
		button.setData(DataKeys.SMART_TYPE.name(), Mode.PLAIN);
		button.setData(DataKeys.EDITOR_CLASS.name(), TextEditorComposite.class);
		buttons.add(button);
		
		button = new Button(control, SWT.TOGGLE | SWT.FLAT);
		button.setForeground(ColorEnum.DARK_BLUE.get());
		button.setData(DataKeys.TEXT_COLOR.name(), ColorEnum.JAVASCRIPTABLE.get());
		button.setData(DataKeys.SMART_TYPE.name(), Mode.SCRIPT);
		button.setData(DataKeys.EDITOR_CLASS.name(), TextEditorComposite.class);
		buttons.add(button);
		
		button = new Button(control, SWT.TOGGLE | SWT.FLAT);
		button.setForeground(ColorEnum.DARK_GREEN.get());
		button.setData(DataKeys.TEXT_COLOR.name(), ColorEnum.LIGHT_GREEN.get());
		button.setData(DataKeys.SMART_TYPE.name(), Mode.SOURCE);
		button.setData(DataKeys.EDITOR_CLASS.name(), MobileSmartSourceEditorComposite.class);
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
	
	public LayoutData getLayoutData() {
		LayoutData layoutData = super.getLayoutData();
		if ((comboBox == null) || comboBox.isDisposed()) {
			layoutData.minimumWidth = 60;
		} else {
			// make the comboBox 10 characters wide
			GC gc = new GC(comboBox);
			layoutData.minimumWidth = (gc.getFontMetrics()
					.getAverageCharWidth() * 10) + 10;
			gc.dispose();
		}
		return layoutData;
	}
	
    private String[] getTags() {
    	if (databaseObjectTreeObject != null && propertyDescriptor != null) {
    		String propertyName = (String) propertyDescriptor.getId();
        	DatabaseObject bean = (DatabaseObject) databaseObjectTreeObject.getObject();
    		if (bean instanceof ITagsProperty) {
    			return ((ITagsProperty) bean).getTagsForProperty(propertyName);
    		}
    	}
		return new String[] {""};
    }

	@Override
	public void activate() {
		if (items == null) {
			items = getTags();
			populateComboBoxItems();
		}
		super.activate();
	}

	private void populateComboBoxItems() {
		if (comboBox != null && items != null) {
			comboBox.removeAll();
			if (items.length > 0) {
				for (int i = 0; i < items.length; i++) {
					comboBox.add(items[i], i);
				}
				setValueValid(true);
				selection = 0;
			} else {
				comboBox.add("");
				selection = -1;
			}
		}
	}
	
	protected void focusLost() {
		if (isActivated()) {
			applyEditorValueAndDeactivate();
		}
	}
	
	protected void applyEditorValueAndDeactivate() {
		// must set the selection before getting msst
		selection = comboBox.getSelectionIndex();
		Object newValue = doGetValue();
		markDirty();
		boolean isValid = isCorrect(newValue);
		setValueValid(isValid);

		if (!isValid) {
			// Only format if the 'index' is valid
			if (items.length > 0 && selection >= 0 && selection < items.length) {
				// try to insert the current msst into the error message.
				setErrorMessage(MessageFormat.format(getErrorMessage(),
						new Object[] { items[selection] }));
			} else {
				// Since we don't have a valid index, assume we're using an
				// 'edit'
				// combo so format using its text msst
				setErrorMessage(MessageFormat.format(getErrorMessage(),
						new Object[] { comboBox.getText() }));
			}
		}

		fireApplyEditorValue();
		deactivate();
	}
	
	protected void openDialog() {
    	Object newValue = openDialogBox(null);
    	
    	if (newValue != null) {
    		if (newValue instanceof String) {
    			msst.setSmartValue((String) newValue);
    		}
   			comboBox.setText(msst.getValue());
    	}
	}

	@Override
	protected void doSetFocus() {
		comboBox.setFocus();
	}
	
	@Override
	protected Object doGetValue() {
		if (!Mode.SOURCE.equals(msst.getMode())) {
			msst.setSmartValue(comboBox.getText());
		}
		return msst;
	}
	
	@Override
	protected void doSetValue(Object value) {
		this.msst = ((MobileSmartSourceType) value).clone();
		Mode mode = this.msst.getMode();
		
		comboBox.setText(this.msst.getValue());
		
		for (Button button : buttons) {
			if (button.getData(DataKeys.SMART_TYPE.name()) == mode) {
				button.notifyListeners(SWT.Selection, null);
				break;
			}
		}
	}

	@Override
	public Object getEditorData() {
		return msst.getEditorData();
	}
}
