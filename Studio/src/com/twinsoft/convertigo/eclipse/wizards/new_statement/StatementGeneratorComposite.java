/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.wizards.new_statement;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectsExplorerComposite;

public class StatementGeneratorComposite extends ObjectsExplorerComposite {

	private final String labels [] = new String[] {	"Mouse statement", 
													"Set input statement", 
													"Set checkable statement",
													"Set select statement",
													"Set checkable statement",
													"Form elements statements"};

	static public final int MOUSE 	= 0;
	static public final int INPUT 	= 1;
	static public final int CHECK 	= 2;
	static public final int SELECT = 3;
	static public final int RADIO 	= 4;
	static public final int FORM 	= 5;
	
	
	public StatementGeneratorComposite(WizardPage wizardPage, Composite parent, int style, StatementWithExpressions parentObject) {
		super(wizardPage, parent, style, parentObject, null);
		super.initialize();
	}

	public StatementGeneratorComposite(Composite parent, int style, StatementWithExpressions parentObject) {
		super(parent, style, parentObject, null);
	}

	/*
	 * Overrides the ObjectsExplorerComposite method 
	 */
	protected void findDatabaseObjects() {
    	if (wizardPage != null) {
			if (objectsMap.isEmpty()) {
		        handCursor = new Cursor(Display.getDefault(), SWT.CURSOR_HAND);
				
		        StatementGeneratorWizardPage sgwp;
		        String name, shortDescription, beanInfoClassName = "";
		        try {
		        	sgwp = (StatementGeneratorWizardPage)wizardPage;
		        	
			        if (sgwp.isClickable()) {
			        	name = labels[MOUSE];
			        	shortDescription = "Generate mouse statement on this element.";
			        	beanInfoClassName = "com.twinsoft.convertigo.beans.statements.MouseStatementBeanInfo";
			        	
			        	getImageAndAddLabel(name, shortDescription, beanInfoClassName, MOUSE);
			        }
			        
			        if (sgwp.isValuable()) {
			        	name = labels[INPUT];
			        	shortDescription = "Generate set value statement on this element.";
			        	beanInfoClassName = "com.twinsoft.convertigo.beans.statements.InputHtmlSetValueStatementBeanInfo";
			        	
			        	getImageAndAddLabel(name, shortDescription, beanInfoClassName, INPUT);
			        }
			        
			        if (sgwp.isCheckable()) {
			        	name = labels[CHECK];
			        	shortDescription = "Generate set checked statement on this element.";
			        	beanInfoClassName = "com.twinsoft.convertigo.beans.statements.InputHtmlSetCheckedStatementBeanInfo";
			        	
			        	getImageAndAddLabel(name, shortDescription, beanInfoClassName, CHECK);
			        }
			        
			        if (sgwp.isSelectable()) {
			        	name = labels[SELECT];
			        	shortDescription = "Generate set selected statement on this element.";
			        	beanInfoClassName = "com.twinsoft.convertigo.beans.statements.InputHtmlSetSelectedStatementBeanInfo";
			        	
			        	getImageAndAddLabel(name, shortDescription, beanInfoClassName, SELECT);
			        }
			        
			        if (sgwp.isRadioable()) {
			        	name = labels[RADIO];
			        	shortDescription = "Generate set checked statement on this radio button.";
			        	beanInfoClassName = "com.twinsoft.convertigo.beans.statements.InputHtmlSetCheckedStatementBeanInfo";
			        	
			        	getImageAndAddLabel(name, shortDescription, beanInfoClassName, RADIO);
			        }
			        
			        if (sgwp.isFormable()) {
			        	name = labels[FORM];
			        	shortDescription = "Generate statements on the FORM elements.";
			        	beanInfoClassName = "com.twinsoft.convertigo.beans.statements.AbstractComplexeEventStatementBeanInfo";
			        	
			        	getImageAndAddLabel(name, shortDescription, beanInfoClassName, FORM);
			        }
		        } catch (ClassCastException e) {
		        	ConvertigoPlugin.logException(e,"Wizard page instantiating \"StatementGeneratorComposite\" is not from \"StatementGeneratorWizardPage\" class.");
		        } catch (ClassNotFoundException e) {
		        	String message = java.text.MessageFormat.format("Unable to find the \"{0}\" class.", new Object[] {beanInfoClassName});
                    ConvertigoPlugin.logWarning(message);
				} catch (IntrospectionException e) {
					String message = java.text.MessageFormat.format("Unable to find the bean info linked to the \"{0}\" class.", new Object[] {beanInfoClassName});
                    ConvertigoPlugin.logWarning(message);
				} catch (InstantiationException e) {
					String message = java.text.MessageFormat.format("Unable to instantiate the bean info linked to the \"{0}\" class.", new Object[] {beanInfoClassName});
                    ConvertigoPlugin.logException(e,message);
				} catch (IllegalAccessException e) {
					String message = java.text.MessageFormat.format("Unable to retrieve the bean info image linked to the \"{0}\" class.", new Object[] {beanInfoClassName});
                    ConvertigoPlugin.logException(e,message);
				}
	    	}
    	}
    }
	
	private void getImageAndAddLabel(String name, String shortDescription, String beanInfoClassName, int index) 
	throws ClassNotFoundException, InstantiationException, IllegalAccessException, IntrospectionException {
		Class<?> beanInfoClass = Class.forName(beanInfoClassName);
        Image image = (ConvertigoPlugin.getDefault().getBeanIcon((BeanInfo)beanInfoClass.newInstance(), BeanInfo.ICON_COLOR_32x32));
        
        addLabelEx(image, name, shortDescription, false, new Integer(index));
	}
	
	public int getCurrentSelectedOption() {
		int option = -1;
    	if (currentSelectedObject != null) {
    		option = ((Integer)objectsMap.get(currentSelectedObject)).intValue();
    	}
    	return option;
	}
}
