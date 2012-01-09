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

package com.twinsoft.convertigo.eclipse.editors.connector;

import java.util.List;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.statements.AbstractEventStatement;
import com.twinsoft.convertigo.beans.statements.InputHtmlSetCheckedStatement;
import com.twinsoft.convertigo.beans.statements.InputHtmlSetSelectedStatement;
import com.twinsoft.convertigo.beans.statements.InputHtmlSetValueStatement;
import com.twinsoft.convertigo.beans.statements.MouseStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.events.InputSelectEvent;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class EventStatementGenerator {
	protected String formName;
	protected String elementXpath;
	protected StatementWithExpressions block;
	protected HtmlTransaction transaction;
	
	
	public EventStatementGenerator(StatementWithExpressions block, String formXPath, String formName){
		this.block = block;
		transaction = (HtmlTransaction)block.getParentTransaction();
		this.formName = addVariable(formName, formXPath, false);
		this.elementXpath = null;
	}
	
	public EventStatementGenerator(StatementWithExpressions block, String xpath){
		this.block = block;
		transaction = (HtmlTransaction)block.getParentTransaction();
		this.formName = null;
		this.elementXpath = xpath;
	}
	
	protected String addVariableFormPrefixe(String name, String value){
		if(formName!=null) name = formName + "_" + name;
		return addVariable(name, value, true);
	}
	
	/*protected String addVariable(String name, String value, boolean wsdl){
		while(transaction.getVariableDefinitionIndex(name)!= -1){
			String base = name;
			int count = 1;
			int i_ = name.lastIndexOf("_");
			if(i_ != -1 && i_+1 < name.length()){
				String end = name.substring(i_+1);
				try{
					count = Integer.parseInt(end) + 1;
					base = name.substring(0, i_);
				}catch(NumberFormatException e){}
			}
			name = base + "_" + count;
		}
		
		transaction.addVariableDefinition(name, name, value, Boolean.valueOf(wsdl), Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, "GET", "");
		transaction.hasChanged = true;
		return name;
	}*/
	protected String addVariable(String name, String value, boolean wsdl){
		while(transaction.getVariable(name)!= null){
			String base = name;
			int count = 1;
			int i_ = name.lastIndexOf("_");
			if(i_ != -1 && i_+1 < name.length()){
				String end = name.substring(i_+1);
				try{
					count = Integer.parseInt(end) + 1;
					base = name.substring(0, i_);
				}catch(NumberFormatException e){}
			}
			name = base + "_" + count;
		}
		
		RequestableHttpVariable httpVariable = new RequestableHttpVariable();
		try {
			if (!StringUtils.isNormalized(name))
				throw new EngineException("Http variable name is not normalized : \""+name+"\"");
			
			httpVariable.setName(name);
			httpVariable.setDescription(name);
			httpVariable.setValueOrNull(value);
			httpVariable.setWsdl(Boolean.valueOf(wsdl));
			httpVariable.setCachedKey(Boolean.TRUE);
			httpVariable.setHttpMethod("GET");
			
			transaction.addVariable(httpVariable);
			transaction.hasChanged = true;
		} catch (EngineException e) {
		}
		return name;
	}
	
	
	protected void addStatement(AbstractEventStatement stat){
		if(stat != null){
			try {
				List<Statement> stats = block.getStatements();
				boolean goodName = false;
				String currentName = stat.getName();
				while(!goodName){
					goodName=true;
					for(int i=0;i<stats.size() && goodName;i++){
						DatabaseObject obj = (DatabaseObject)stats.get(i);
						goodName = !(obj.getName().equals(currentName));
					}
					if(!goodName){
						String base = currentName;
						int count = 1;
						int i_ = currentName.lastIndexOf("_");
						if(i_ != -1 && i_+1 < currentName.length()){
							String end = currentName.substring(i_+1);
							try{
								count = Integer.parseInt(end) + 1;
								base = currentName.substring(0, i_);
							}catch(NumberFormatException e){}
						}
						currentName = base + "_" + count;
					}
				}
				
				if (!StringUtils.isNormalized(currentName))
					throw new EngineException("Statement name is not normalized : \""+currentName+"\"");
				
				stat.setName(currentName);
				stat.hasChanged = true;
				stat.bNew = true;
				
				block.addStatement(stat);
				block.hasChanged = true;
				
			} catch (EngineException e) {
				ConvertigoPlugin.logException(e, "Error when adding an '" + stat.getClass().getName() + "' statement.");
			}
		}
	}
	
	protected String getXPath(String tagName, String selectBy, String selectType){
		if(elementXpath != null) return "'" + elementXpath + "'";
		return formName +" + '//" + tagName + "[@" + selectBy + "=\"" + selectType + "\"]'";
	}
	
	public void addInputText(String selectBy, String selectType, String tagName, String value){
		String var_name = addVariableFormPrefixe(selectType, value);
		String xpath = getXPath(tagName.toUpperCase(), selectBy, selectType);
		AbstractEventStatement stat = new InputHtmlSetValueStatement(xpath, var_name);
		addStatement(stat);
	}
	
	public void addInputRadio(String selectBy, String selectType, int check_index, String[] values){
		String comment = "";
		for(int i=0;i<values.length;i++) comment += Integer.toString(i+1) + " -> '" + values[i] + "'\n";
		String var_name = addVariableFormPrefixe(selectType, Integer.toString(check_index + 1));
		String xpath = null;
		if(elementXpath == null){
			xpath = (formName == null? "'" : formName +" + '");
		}else{
			xpath = "'" + elementXpath + "/ancestor::FORM[1]"; 
		}
		xpath = "'(' + " + xpath + "//INPUT[@" + selectBy + "=\"" + selectType + "\" and @type=\"radio\"])['+" + var_name +"+']'";
		AbstractEventStatement stat = new InputHtmlSetCheckedStatement(xpath, true);
		stat.setComment(comment);
		addStatement(stat);
	}
	
	public void addInputMouse(String selectBy, String selectType, String tagName){
		String xpath = getXPath(tagName.toUpperCase(), selectBy, selectType);
		AbstractEventStatement stat = new MouseStatement(xpath);
		addStatement(stat);
	}
	
	public void addInputCheckbox(String selectBy, String selectType, boolean checked){
		String xpath = getXPath("INPUT", selectBy, selectType);
		AbstractEventStatement stat = new InputHtmlSetCheckedStatement(xpath, checked);
		addStatement(stat);
	}
	
	public void addSelect(String selectBy, String selectType, boolean[] checks, String[] values, String[] contents){
		String comment = "";
		String value = null;
		for(int i=0;i<checks.length;i++){
			comment += Integer.toString(i) + " -> value: '" + values[i] + "'\ncontent: '" + contents[i] + "'\n";
			if(checks[i]){
				if(value==null){
					value = Integer.toString(i);
				}else{
					if(value.startsWith("[")) value = value.substring(1, value.length()-1);
					value = "[" + value + ", " + Integer.toString(i) + "]";
				}
			}
		}
		
		String var_name = addVariableFormPrefixe(selectType, value);
		String xpath = getXPath("SELECT", selectBy, selectType);
		if(value.startsWith("[")) var_name = "eval(''+"+var_name+")";
		AbstractEventStatement stat = new InputHtmlSetSelectedStatement(xpath, var_name, InputSelectEvent.MOD_INDEX);
		stat.setComment(comment);
		addStatement(stat);
	}

	public void setXpath(String xpath) {
		this.elementXpath = xpath;
	}
}
