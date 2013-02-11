package com.twinsoft.convertigo.eclipse.property_editors;

import java.lang.reflect.Method;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.ComboBoxLabelProvider;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class DynamicComboBoxPropertyDescriptor extends PropertyDescriptor {

	private DatabaseObjectTreeObject databaseObjectTreeObject;
	private Method getTagsMethod;
	private String propertyName;
	
	public DynamicComboBoxPropertyDescriptor(Object id, String displayName, Method getTagsMethod,
			DatabaseObjectTreeObject databaseObjectTreeObject, String propertyName) {
		super(id, displayName);
		this.databaseObjectTreeObject = databaseObjectTreeObject;
		this.getTagsMethod = getTagsMethod;
		this.propertyName = propertyName;
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
        CellEditor editor = new ComboBoxCellEditor(parent, getTags(), SWT.READ_ONLY);
        if (getValidator() != null) {
			editor.setValidator(getValidator());
		}
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
		return new ComboBoxLabelProvider(getTags());
    }

    private String[] getTags() {
    	String[] tags;
		try {
			tags = (String[]) getTagsMethod.invoke(null, new Object[] { databaseObjectTreeObject, propertyName } );
		} catch (Exception e) {
			tags = new String[0];
		}
		return tags;
    }
}
