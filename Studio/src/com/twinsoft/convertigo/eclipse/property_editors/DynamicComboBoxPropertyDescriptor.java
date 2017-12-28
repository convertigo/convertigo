package com.twinsoft.convertigo.eclipse.property_editors;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.ComboBoxLabelProvider;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class DynamicComboBoxPropertyDescriptor extends PropertyDescriptor {
	private static final ThreadLocal<ComboBoxCellEditor> last = new ThreadLocal<ComboBoxCellEditor>();
	private static final String SYMBOL = "${symbol}";
	private DatabaseObjectTreeObject databaseObjectTreeObject;
	private Method getTagsMethod;
	private String propertyName;
	private String[] tags = {SYMBOL};
	private ComboBoxCellEditor editor;
	private ComboBoxLabelProvider labelProvider;
	
	public DynamicComboBoxPropertyDescriptor(Object id, String displayName, String[] tags) {
		super(id, displayName);
    	tags = Arrays.copyOf(tags, tags.length + 1);
    	tags[tags.length - 1] = this.tags[this.tags.length - 1];
    	this.tags = tags;
	}
	
	public DynamicComboBoxPropertyDescriptor(Object id, String displayName, Method getTagsMethod,
			DatabaseObjectTreeObject databaseObjectTreeObject, String propertyName) {
		super(id, displayName);
		this.databaseObjectTreeObject = databaseObjectTreeObject;
		this.getTagsMethod = getTagsMethod;
		this.propertyName = propertyName;
	}

	public int setValue(String value) {
		//String newValue = Engine.theApp.databaseObjectsManager.getCompiledValue(value);
		String[] tags = getTags();
		for (int i = 0 ; i < tags.length; i++) {
			if (value.equals(tags[i])) {
				return i;
			}
		}
		tags[tags.length - 1] = value;
		if (editor != null) {
			editor.setItems(tags);
			editor.setValue(tags.length - 1);
		}
		return tags.length - 1;
	}
	
	public static ComboBoxCellEditor getLast() {
		ComboBoxCellEditor l = last.get();
		last.set(null);
		return l;
	}
	
    /**
     * The <code>ComboBoxPropertyDescriptor</code> implementation of this 
     * <code>IPropertyDescriptor</code> method creates and returns a new
     * <code>ComboBoxCellEditor</code>.
     * <p>
     * The editor is configured with the current validator if there is one.
     * </p>
     */
    public CellEditor createPropertyEditor(Composite parent) {
    	String[] tags = getTags();
    	editor = new ComboBoxCellEditor(parent, tags, SWT.READ_ONLY);
        if (getValidator() != null) {
			editor.setValidator(getValidator());
		}
        editor.addListener(new ICellEditorListener() {
			
			@Override
			public void editorValueChanged(boolean oldValidState, boolean newValidState) {
			}
			
			@Override
			public void cancelEditor() {
			}
			
			@Override
			public void applyEditorValue() {
				last.set(editor);
			}
		});
        return editor;
    }

    /**
     * The <code>ComboBoxPropertyDescriptor</code> implementation of this 
     * <code>IPropertyDescriptor</code> method returns the value set by
     * the <code>setProvider</code> method or, if no value has been set
     * it returns a <code>ComboBoxLabelProvider</code> created from the 
     * valuesArray of this <code>ComboBoxPropertyDescriptor</code>.
     *
     * @see #setLabelProvider(ILabelProvider)
     */
    public ILabelProvider getLabelProvider() {
        if (isLabelProviderSet()) {
			return super.getLabelProvider();
		}
		return labelProvider = new ComboBoxLabelProvider(getTags());
    }

    private String[] getTags() {
    	if (getTagsMethod != null) {
			try {
				String[] tags = (String[]) getTagsMethod.invoke(null, new Object[] { databaseObjectTreeObject, propertyName } );
				if (!SYMBOL.equals(tags[tags.length - 1])) {
					tags = Arrays.copyOf(tags, tags.length + 1);
			    	tags[tags.length - 1] = SYMBOL;
				}
		    	this.tags = tags;
			} catch (Exception e) {
				tags = new String[] {SYMBOL};
			}
    	}
    	
    	if (labelProvider != null) {
    		labelProvider.setValues(tags);
		}
    	
		return tags;
    }
}
