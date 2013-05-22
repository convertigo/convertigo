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

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.eclipse.ColorEnum;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class SmartTypeCellEditor extends AbstractDialogCellEditor {
	
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
		final Composite control = new Composite(parent, SWT.NONE);
		
		Font font = parent.getFont();
		Color bg = parent.getBackground();
		
		control.setFont(font);
		control.setBackground(bg);
		
		GridLayout gl = new GridLayout(99, false);
		gl.horizontalSpacing = gl.marginHeight = gl.marginWidth = gl.verticalSpacing = 0;
		control.setLayout(gl);
		
		text = new Text(control, SWT.NONE);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
        text.addKeyListener(new KeyAdapter() {
        	
            @Override
            public void keyPressed(KeyEvent e) {
                keyReleaseOccured(e);
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
				
				text.setBackground((Color) button.getData(DataKeys.TEXT_COLOR.name()));
				
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
}
