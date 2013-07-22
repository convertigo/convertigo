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

package com.twinsoft.convertigo.beans.common;

import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.twinj.iJavelin;

public class Choice extends JavelinExtractionRule {
	
    public static final long serialVersionUID = 4350644982787619412L;
    
    private String tagName = "choice";
    private String startPattern = "";
    private String endPattern = "";
    private String separatorChars = ",";
    
    public static final int CHOICE_CHARACTER_POLICY_INDEX_INDEX = 0;
    public static final int CHOICE_CHARACTER_POLICY_INDEX_SEPARATOR = 1;
    public static final int CHOICE_CHARACTER_POLICY_INDEX_FIRST_UPPERCASE_LETTER = 2;
    
    /** Holds value of property choiceCharacterPolicy. */
    private int choiceCharacterPolicy = Choice.CHOICE_CHARACTER_POLICY_INDEX_SEPARATOR;
    
    /** Holds value of property separatorCharsForTokens. */
    private String separatorCharsForTokens = "=";
    
    /** Holds value of property actions. */
    private XMLVector<XMLVector<String>> actions = new XMLVector<XMLVector<String>>();
    
    /** Holds value of property actionsFromScreen. */
    private boolean actionsFromScreen = true;
    
    /** Holds value of property radio. */
    private boolean	radio = false;   
    
    public Choice() {
        super();
    }
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
		// removing automatic radio mode : now it's a property
        //boolean	bSetToRadio = false;        
        
        // S'il faut prendre les valeurs des options sur l'écran
        if(actionsFromScreen){
            String textBlock = block.getText().trim();
            
            if ( (!textBlock.startsWith(startPattern) && startPattern.length() != 0) || 
                 (!textBlock.endsWith(endPattern) && endPattern.length() != 0) ) {
                return xrs;
            }

            Block previousBlock = block;
            boolean fieldFound = false;
            while (((previousBlock = blockFactory.getPreviousBlock(previousBlock))!= null) && (block.line == previousBlock.line)) {
                if (previousBlock.type.equals("field")) {
                	fieldFound = true;
					break;
                }
            }

            if (!fieldFound) {
                Engine.logBeans.debug("Criteria Choice not matching because of not finding field block before on the same line");
                return xrs;
            }

			// handle the start and end patterns
			int beginIndex= startPattern.length();
			int endIndex;
			if(endPattern.length() == 0)
				endIndex= textBlock.length();
			else	endIndex= textBlock.indexOf(endPattern);
            
			textBlock = textBlock.substring(beginIndex, endIndex);
			
			// Try to tokenize 
			StringTokenizer st = new StringTokenizer(textBlock, separatorChars, false);
			if (st.countTokens() == 1) {
				Engine.logBeans.debug("Only one token found this is not a choice...");
				return xrs;
			}

			// we have a choice block now build it.
            block.type = "choice";
            if (tagName.equals("")) {
                block.tagName = "choice";
            } else block.tagName = tagName;
            
            String currentAction= previousBlock.getText().trim();
            boolean currentActionExist= false;
            block.setText(previousBlock.getText());
			block.setOptionalAttribute("size", Integer.toString(previousBlock.length));
			if (previousBlock.getOptionalAttribute("hasFocus") == "true")
				block.setOptionalAttribute("hasFocus", "true");
				
            block.line = previousBlock.line;
            block.column = previousBlock.column;
            block.attribute = previousBlock.attribute;
            block.name = previousBlock.name;
            blockFactory.removeBlock(previousBlock);
            
            // Build the sub elemnts of the choice...
            Element element;
            String token;
            int index = 0;
            while (st.hasMoreTokens()) {
                token = st.nextToken().trim();
                element = dom.createElement("item");
                element.setAttribute("type", "choiceItem");

                switch (choiceCharacterPolicy) {

                    case Choice.CHOICE_CHARACTER_POLICY_INDEX_INDEX:
                        element.setAttribute("value", token);
                        element.setAttribute("action", Integer.toString(index));
                        if(currentAction.equalsIgnoreCase(token)){
                            element.setAttribute("selected", "true");
                            currentActionExist= true;
                        }
                        else
                            element.setAttribute("selected", "false");
                        break;

                    case Choice.CHOICE_CHARACTER_POLICY_INDEX_SEPARATOR:
                        try {
                            StringTokenizer st2 = new StringTokenizer(token, separatorCharsForTokens, false);
                            String action = st2.nextToken().trim();
                            element.setAttribute("action", action);
                            String value = st2.nextToken().trim();
                           	element.setAttribute("value", value);
								
                            if(currentAction.equalsIgnoreCase(action) ||
                                currentAction.equalsIgnoreCase(value)    ){
                                element.setAttribute("selected", "true");
                                currentActionExist= true;
                            }
                            else
                                element.setAttribute("selected", "false");
                        }
                        catch(Exception e) {
                        	// could not separate token
							Engine.logBeans.debug("Could not find separator, assume token is value ...");
							element.setAttribute("action", token);
							element.setAttribute("value", token);
							// removing automatic radio mode : now it's a property
							//bSetToRadio = true;
							if(currentAction.equalsIgnoreCase(token.trim())) {
								element.setAttribute("selected", "true");
								currentActionExist= true;
							}
                        }
                        break;

                    case Choice.CHOICE_CHARACTER_POLICY_INDEX_FIRST_UPPERCASE_LETTER:
                        int i = 0;
                        int len = token.length();
                        while (i < len) {
                            char c = token.charAt(i);
                            if (Character.isUpperCase(c)) {
                                element.setAttribute("value", token);
                                element.setAttribute("action", "" + c);
                                if(currentAction.equalsIgnoreCase(token) ||
                                    currentAction.equalsIgnoreCase("" + c)    ){
                                    element.setAttribute("selected", "true");
                                    currentActionExist= true;
                                }
                                else
                                    element.setAttribute("selected", "false");
                                break;
                            }
                            i++;
                        }
                        break;
                }
                index++;
                block.addOptionalChildren(element);
            }
            
            if(!currentActionExist){
                element = dom.createElement("item");
                element.setAttribute("type", "choiceItem");
                element.setAttribute("value", currentAction);
                element.setAttribute("action", currentAction);
                element.setAttribute("selected", "true");
                block.addOptionalChildren(element);
            }
            
            // specify if this is a radio like button
			if (isRadio())
				block.setOptionalAttribute("radio", "true");
			else
				block.setOptionalAttribute("radio", "false");
            
        }
        else{//s'il faut les prendre dans le vecteur
            if(actions == null || actions.isEmpty()){
                Engine.logBeans.debug("Criteria Choice not matching because of no action defined");
                return xrs;
            }
            
            if (!block.type.equals("field")) {
                Engine.logBeans.debug("Criteria Choice not matching because it is not a field block");
                return xrs;
            }
            
            block.type = "choice";
            if (tagName.equals("")) {
                block.tagName = "choice";
            }
            else {
                block.tagName = tagName;
            }
            String currentAction= block.getText().trim();
            block.setText("");
			block.setOptionalAttribute("radio", "false");

            boolean currentActionExist= false;
            
            for(List<String> myAction : actions){
            	Element element = dom.createElement("item");
                element.setAttribute("type", "choiceItem");
                element.setAttribute("value", myAction.get(0));
                element.setAttribute("action", myAction.get(1));
                if(currentAction.equalsIgnoreCase(myAction.get(0)) ||
                    currentAction.equalsIgnoreCase(myAction.get(1))    ){
                    element.setAttribute("selected", "true");
                    currentActionExist= true;
                } else element.setAttribute("selected", "false");
                block.addOptionalChildren(element);
            }
            if(!currentActionExist){
            	Element element = dom.createElement("item");
                element.setAttribute("type", "choiceItem");
                element.setAttribute("value", currentAction);
                element.setAttribute("action", currentAction);
                element.setAttribute("selected", "true");
                block.addOptionalChildren(element);
            }
        }
        
        xrs.hasMatched = true;
        return xrs;
    }
    
    /** Getter for property tagName.
     * @return Value of property tagName.
     */
    public java.lang.String getTagName() {
        return tagName;
    }
    
    /** Setter for property tagName.
     * @param tagName New value of property tagName.
     */
    public void setTagName(java.lang.String tagName) {
        this.tagName = tagName;
    }
    
    /** Getter for property startPattern.
     * @return Value of property startPattern.
     */
    public java.lang.String getStartPattern() {
        return startPattern;
    }
    
    /** Setter for property startPattern.
     * @param startPattern New value of property startPattern.
     */
    public void setStartPattern(java.lang.String startPattern) {
        this.startPattern = startPattern;
    }
    
    /** Getter for property endPattern.
     * @return Value of property endPattern.
     */
    public java.lang.String getEndPattern() {
        return endPattern;
    }
    
    /** Setter for property endPattern.
     * @param endPattern New value of property endPattern.
     */
    public void setEndPattern(java.lang.String endPattern) {
        this.endPattern = endPattern;
    }
    
    /** Getter for property separatorChars.
     * @return Value of property separatorChars.
     */
    public java.lang.String getSeparatorChars() {
        return separatorChars;
    }
    
    /** Setter for property separatorChars.
     * @param separator New value of property separatorChars.
     */
    public void setSeparatorChars(java.lang.String separatorChars) {
        this.separatorChars = separatorChars;
    }
    
    /** Getter for property choiceCharacterPolicy.
     * @return Value of property choiceCharacterPolicy.
     */
    public int getChoiceCharacterPolicy() {
        return this.choiceCharacterPolicy;
    }
    
    /** Setter for property choiceCharacterPolicy.
     * @param choiceCharacterPolicy New value of property choiceCharacterPolicy.
     */
    public void setChoiceCharacterPolicy(int choiceCharacterPolicy) {
        this.choiceCharacterPolicy = choiceCharacterPolicy;
    }
    
    /** Getter for property separatorCharsForTokens.
     * @return Value of property separatorCharsForTokens.
     */
    public String getSeparatorCharsForTokens() {
        return this.separatorCharsForTokens;
    }
    
    /** Setter for property separatorCharsForTokens.
     * @param separatorCharsForTokens New value of property separatorCharsForTokens.
     */
    public void setSeparatorCharsForTokens(String separatorCharsForTokens) {
        this.separatorCharsForTokens = separatorCharsForTokens;
    }
    
    /** Getter for property actions.
     * @return Value of property actions.
     */
    public XMLVector<XMLVector<String>> getActions() {
        return this.actions;
    }
    
    /** Setter for property actions.
     * @param actions New value of property actions.
     */
    public void setActions(XMLVector<XMLVector<String>> actions) {
        this.actions = actions;
    }
    
    /** Getter for property actionsFromScreen.
     * @return Value of property actionsFromScreen.
     */
    public boolean isActionsFromScreen() {
        return this.actionsFromScreen;
    }
    
    /** Setter for property actionsFromScreen.
     * @param actionsFromScreen New value of property actionsFromScreen.
     */
    public void setActionsFromScreen(boolean actionsFromScreen) {
        this.actionsFromScreen = actionsFromScreen;
    }

    public boolean isRadio() {
		return radio;
	}

	public void setRadio(boolean radio) {
		this.radio = radio;
	}

}
