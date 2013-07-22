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


import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class StatementGeneratorWizardPage extends WizardPage {
	private StatementWithExpressions parentObject = null;
	private Composite composite = null;
	private DatabaseObject newBean = null;
	private String xpath = null;
	private boolean[] possibleActions = null;
	private int lastOption = -1;
	
	public StatementGeneratorWizardPage(StatementWithExpressions parentObject, String xpath, boolean[] possibleActions) {
		super("StatementGeneratorWizardPage");
		this.parentObject = parentObject;
		this.xpath = xpath;
		this.possibleActions = possibleActions;
	}

	public void createControl(Composite parent) {
		composite = new StatementGeneratorComposite(this, parent, SWT.NULL, parentObject);
		setControl(composite);
	}
	
    public int getCurrentSelectedOption() {
    	int option = -1;
    	if (composite != null) {
   			option = ((StatementGeneratorComposite)composite).getCurrentSelectedOption();
    	}
    	return option;
    }

	private void createBean() {
		int option = getCurrentSelectedOption();
		if (option != -1) {
			try {
				// TODO use generators code to instantiate good statements
				switch(option){
					/*clickable */	
					case StatementGeneratorComposite.MOUSE: 
//TODO						generateMouseStatement(parentObject, xpath); 
						break;
					/*valuable  */	
					case StatementGeneratorComposite.INPUT: 
//TODO						generateSetInputStatement(parentObject, xpath); 
						break;
					/*checkable */	
					case StatementGeneratorComposite.CHECK: 
//TODO						generateSetCheckableStatement(parentObject, xpath, false); 
						break;
					/*selectable*/	
					case StatementGeneratorComposite.SELECT: 
//TODO						generateSetSelectStatement(parentObject, xpath); 
						break;
					/*radioable */	
					case StatementGeneratorComposite.RADIO: 
//TODO						generateSetCheckableStatement(parentObject, xpath, true); 
						break;
					/*formable  */	
					case StatementGeneratorComposite.FORM: 
//TODO						generateFormElements(parentObject, xpath); 
						break;
				}
			}
			//TODO delete comments around catch
			/*catch(EngineException e1){
				ConvertigoPlugin.logException(e1, "Error when generate statement for this xPath : "+xpath);
				ConvertigoPlugin.warningMessageBox("Error when generate statement for this xPath : "+xpath);
				newBean = null;
			}*/
			catch(Exception e1){
				ConvertigoPlugin.logException(e1, "Error when generate statement for this xPath : "+xpath);
				newBean = null;
			}
//TODO end of code
/*
			newBean = (DatabaseObject) bi.getBeanDescriptor().getBeanClass().newInstance();
			if (xpath != null) {
				// case we create an XPath criteria
				if (newBean instanceof XPath) {
					((XPath)newBean).setXpath(xpath);
				}
				// case we create an html extraction rule
				if (newBean instanceof HtmlExtractionRule) {
					((HtmlExtractionRule)newBean).setXpath(xpath);
				}
				// case we create an "xpathable" statement
				if (newBean instanceof AbstractEventStatement) {
					((AbstractEventStatement)newBean).setXpath(xpath);
				}
			}
*/
		}
	}
    
	public DatabaseObject getCreatedBean() {
		return newBean;
	}
	
	private void setInfoBeanName() {
		if (newBean != null) {
			try {
				String name = "New " + newBean.getName();
				name = StringUtils.normalize(name);
				((StatementInfoWizardPage)getWizard().getPage("StatementInfoWizardPage")).setBeanName(name);
			} catch (Exception e) {}
		}
	}
	
	public void setPageComplete(boolean complete) {
		if (complete) {
			createBean();
			setInfoBeanName();
		}
		super.setPageComplete(complete);
		
		// Handles double clic on object
		if (newBean != null) {
			int index = getCurrentSelectedOption();
			if (index == lastOption)
				showNextPage();
			lastOption = index;
		}
	}


	public boolean isPageComplete() {
		return (getCurrentSelectedOption() != -1);
	}
	
	public void showNextPage() {
		IWizardPage  page = getNextPage();
		if ((page != null) && isPageComplete())
			getWizard().getContainer().showPage(page);
	}
	
	public boolean isClickable() {
		return possibleActions[0];
	}
	public boolean isValuable() {
		return possibleActions[1];
	}
	public boolean isCheckable() {
		return possibleActions[2];
	}
	public boolean isSelectable() {
		return possibleActions[3];
	}
	public boolean isRadioable() {
		return possibleActions[4];
	}
	public boolean isFormable() {
		return possibleActions[5];
	}
	
// TODO make this methods (cut and paste from XulWebViewerImpl) work
/*
	protected Object[] initGenerate(StatementWithExpressions block, String xpath)throws EngineException{
		nsIDOMNode[] nodes = evaluteXpath(xpath);
		if(nodes.length == 0){
			throw new EngineException("No node evaluated from this XPath : "+xpath);
		}else if(nodes.length > 1){
			throw new EngineException("More than one node evaluated from this XPath : "+xpath);
		}
		nsIDOMHTMLElement element = null;
		try{
			element = (nsIDOMHTMLElement)nodes[0].queryInterface(nsIDOMHTMLElement.NS_IDOMHTMLELEMENT_IID);
		}catch (Exception e) {
			throw new EngineException("Node is not a Html Element from this XPath : "+xpath);
		}
		
		Object[] ids = giveIds(element);
		String selectBy = (String)ids[0];
		String selectType = (String)ids[1];
		String tagName = (String)ids[2];
		
		EventStatementGenerator evtGen = tagName.equalsIgnoreCase("FORM")?
				new EventStatementGenerator(block, xpath, selectType):
				new EventStatementGenerator(block, xpath);

		return new Object[]{selectBy, selectType, tagName, element, evtGen};
		
	}

	protected Object[] giveIds(nsIDOMHTMLElement element){
		String selectBy = "name";
		String selectType = element.getAttribute(selectBy);

		if(selectType == null || selectType.equals("")){
			selectBy = "id";
			selectType = element.getAttribute(selectBy);
		}
		
		if(selectType == null || selectType.equals("")){
			selectBy = "tagname";
			selectType = element.getTagName();
		}
		
		String tagName = element.getTagName();
		
		return new Object[]{selectBy, selectType, tagName};
	}
	
	public void generateFormElements(StatementWithExpressions block, String formXPath)throws EngineException{
		Object[] init = initGenerate(block, formXPath);
		nsIDOMHTMLFormElement form;
		try {
			form = (nsIDOMHTMLFormElement)((nsIDOMHTMLElement)init[3]).queryInterface(nsIDOMHTMLFormElement.NS_IDOMHTMLFORMELEMENT_IID);
		} catch (Exception e) {
			throw new EngineException("Node is not a Html Form Element from this XPath : "+formXPath);
		}
		EventStatementGenerator evtGen = ((EventStatementGenerator)init[4]);
		
		Vector radios_done = new Vector();
		
		nsIDOMHTMLCollection champs = form.getElements();
		try{
			for(int i=0;i<champs.getLength();i++){
				nsIDOMHTMLElement element = (nsIDOMHTMLElement)champs.item(i).queryInterface(nsIDOMHTMLElement.NS_IDOMHTMLELEMENT_IID);
				
				Object[] ids = giveIds(element);
				String selectBy = (String)ids[0];
				String selectType = (String)ids[1];
				String tagName = (String)ids[2];
				
				if(tagName.equalsIgnoreCase("INPUT")){
					nsIDOMHTMLInputElement input = (nsIDOMHTMLInputElement) element.queryInterface(nsIDOMHTMLInputElement.NS_IDOMHTMLINPUTELEMENT_IID);

					String type = input.getType();

					if(type.equalsIgnoreCase("text")||type.equalsIgnoreCase("password")){
						evtGen.addInputText(selectBy, selectType, tagName, input.getValue());
					}else if(type.equalsIgnoreCase("radio")){
						String key = selectBy + "=" + selectType;
						if(!radios_done.contains(key)){
							radios_done.add(key);
							generateSetCheckableStatementForRadio(selectBy, selectType, form, evtGen);
						}
					}else if(type.equalsIgnoreCase("checkbox")){
						evtGen.addInputCheckbox(selectBy, selectType, input.getChecked());
					}
				}else if(tagName.equalsIgnoreCase("SELECT")){
					nsIDOMHTMLSelectElement select = (nsIDOMHTMLSelectElement) element.queryInterface(nsIDOMHTMLSelectElement.NS_IDOMHTMLSELECTELEMENT_IID);
					generateSetSelectStatement(selectBy, selectType, select, evtGen);
				}else if(tagName.equalsIgnoreCase("TEXTAREA")){
					nsIDOMHTMLTextAreaElement textArea = (nsIDOMHTMLTextAreaElement) element.queryInterface(nsIDOMHTMLTextAreaElement.NS_IDOMHTMLTEXTAREAELEMENT_IID);
					evtGen.addInputText(selectBy, selectType, tagName, textArea.getValue());
				}else {
					ConvertigoPlugin.logDebug("XulWebViewer generateFormElements unknow tagName : "+tagName);
				}
			}
		}catch (Exception e) {
			throw new EngineException("Some error when generate statement from this FORM XPath : "+formXPath, e);
		}
	}
		
	public void generateMouseStatement(StatementWithExpressions block, String xpath)throws EngineException{
		Object[] init = initGenerate(block, xpath);
		String selectBy = (String)init[0];
		String selectType = (String)init[1];
		String tagName = (String)init[2];
		EventStatementGenerator evtGen = (EventStatementGenerator)init[4];
		
		evtGen.addInputMouse(selectBy, selectType, tagName);
	}

	public void generateSetInputStatement(StatementWithExpressions block, String xpath)throws EngineException{	
		Object[] init = initGenerate(block, xpath);
		String selectBy = (String)init[0];
		String selectType = (String)init[1];
		String tagName = (String)init[2];
		String value = "";
		if(tagName.equalsIgnoreCase("textarea")){
			nsIDOMHTMLTextAreaElement element = (nsIDOMHTMLTextAreaElement)((nsIDOMHTMLElement)init[3]).queryInterface(nsIDOMHTMLTextAreaElement.NS_IDOMHTMLTEXTAREAELEMENT_IID);
			value = element.getValue();
		}else{
			nsIDOMHTMLInputElement element = (nsIDOMHTMLInputElement)((nsIDOMHTMLElement)init[3]).queryInterface(nsIDOMHTMLInputElement.NS_IDOMHTMLINPUTELEMENT_IID);
			value = element.getValue();
		}
		
		EventStatementGenerator evtGen = (EventStatementGenerator)init[4];
		
		evtGen.addInputText(selectBy, selectType, tagName, value);
	}

	protected void generateSetCheckableStatementForRadio(String selectBy, String selectType, nsIDOMHTMLElement element, EventStatementGenerator evtGen){
		String prexpath = element.getTagName().equalsIgnoreCase("FORM")?"":"ancestor::FORM[1]";
		nsIDOMNode[] radios = evaluteXpath(prexpath + "//INPUT[@"+selectBy+"=\""+selectType+"\" and @type=\"radio\"]", element);
		String [] values = new String[radios.length];
		int check_index = -1;
		for(int j=0;j<values.length;j++){
			nsIDOMHTMLInputElement radio = (nsIDOMHTMLInputElement) radios[j].queryInterface(nsIDOMHTMLInputElement.NS_IDOMHTMLINPUTELEMENT_IID);
			values[j] = radio.getValue();
			if(radio.getChecked()) check_index = j;
		}

		evtGen.addInputRadio(selectBy, selectType, check_index, values);						
	}
	
	public void generateSetCheckableStatement(StatementWithExpressions block, String xpath, boolean radioGroup)throws EngineException{
		Object[] init = initGenerate(block, xpath);
		String selectBy = (String)init[0];
		String selectType = (String)init[1];
		nsIDOMHTMLInputElement element = (nsIDOMHTMLInputElement)((nsIDOMHTMLElement)init[3]).queryInterface(nsIDOMHTMLInputElement.NS_IDOMHTMLINPUTELEMENT_IID);
		EventStatementGenerator evtGen = (EventStatementGenerator)init[4];
				
		if(radioGroup) generateSetCheckableStatementForRadio(selectBy, selectType, element, evtGen);
		else evtGen.addInputCheckbox(selectBy, selectType, element.getChecked());	
	}
	
	protected void generateSetSelectStatement(String selectBy, String selectType, nsIDOMHTMLSelectElement select, EventStatementGenerator evtGen){
		nsIDOMHTMLOptionsCollection options = select.getOptions();
		int nb_options = (int)options.getLength();
		boolean [] checks = new boolean[nb_options];
		String [] values = new String[nb_options];
		String [] contents = new String[nb_options];
		
		for(int j=0;j<nb_options;j++){
			nsIDOMHTMLOptionElement option = (nsIDOMHTMLOptionElement)options.item(j).queryInterface(nsIDOMHTMLOptionElement.NS_IDOMHTMLOPTIONELEMENT_IID);
			nsIDOMNode child = option.getFirstChild();
			contents[j] = (child!=null && child.getNodeType() == nsIDOMNode.TEXT_NODE)? child.getNodeValue():"";
			checks[j] = option.getSelected();
			values[j] = option.getValue();
			if(values[j]==null)values[j] = "";
		}
		evtGen.addSelect(selectBy, selectType, checks, values, contents);
	}
	
	public void generateSetSelectStatement(StatementWithExpressions block, String xpath)throws EngineException{
		Object[] init = initGenerate(block, xpath);
		String selectBy = (String)init[0];
		String selectType = (String)init[1];
		nsIDOMHTMLSelectElement select = (nsIDOMHTMLSelectElement)((nsIDOMHTMLElement)init[3]).queryInterface(nsIDOMHTMLSelectElement.NS_IDOMHTMLSELECTELEMENT_IID);
		EventStatementGenerator evtGen = (EventStatementGenerator)init[4];
		
		generateSetSelectStatement(selectBy, selectType, select, evtGen);
	}
*/
	
}
