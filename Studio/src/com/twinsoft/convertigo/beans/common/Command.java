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

import java.util.Arrays;
import java.util.List;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule;
import com.twinsoft.twinj.iJavelin;

/**
 * This class manages commands and keywords
 * keywordtable is an XMLVector, its elements are keywords (XMLVector)
 * Keywords are XMLVectors with four elements : 
 *   - 1 label (the text to search)
 *   - 2 data (the data to send before the command/keyword action)
 *   - 3 replaceText (the replacement text for the XMLisation)
 *   - 4 action (the action to be performed)
 * When applying throws extraction rule, we first etst if the block is in the specified zone
 * then for each keyword in the keywordTable we test if the text of the block (trimed) equals
 * the keyword (w or w/o case sensitivity)
 * This extraction rule matches only if the text of the block matches a keyword
 * If a keyword has been found, it changes the type of the block
 * sets the attributes "data" and action to their specified value or to ""
 * replace the text of the block with the specified one
 */
public class Command extends JavelinMashupEventExtractionRule {
	
    public static final long serialVersionUID = 4350644982787619412L;
    
    private boolean caseDependency = false;
    
    /** Holds value of property keywordTable. */
    private XMLVector<XMLVector<String>> keywordTable = new XMLVector<XMLVector<String>>();
    
    public Command() {
        super();
    }
    
    /** Getter for property caseDependency.
     * @return Value of property caseDependency.
     */
    public boolean isCaseDependency() {
        return caseDependency;
    }
    
    /** Setter for property caseDependency.
     * @param caseDependency New value of property caseDependency.
     */
    public void setCaseDependency(boolean caseDependency) {
        this.caseDependency = caseDependency;
    }
    
    /**
     * Adds a new keyword in the list
     *
     * @param label text of keyword on screen.
     *
     * @param data text to be sent if keyword is clicked.
     *
     * @param action action to be done after sending data.
     *
     */
    public void addNewKeyword(String label, String data, String replaceText, String action) {
        XMLVector<String> keyword = new XMLVector<String>();
        keyword.add(label);
        keyword.add(data);
        keyword.add(replaceText);
        keyword.add(action);
        keywordTable.add(keyword);
    }
    
    /**
     * Removes a keyword in the list
     *
     * @param label text of keyword on screen.
     *
     * @param data text to be sent if keyword is clicked.
     *
     * @param action action to be done after sending data.
     *
     */
    public void removeKeyword(String label, String data, String replaceText, String action) {
    	XMLVector<String> keyword = new XMLVector<String>();
        keyword.add(label);
        keyword.add(data);
        keyword.add(replaceText);
        keyword.add(action);
        
        keywordTable.removeAll(Arrays.asList(new XMLVector[]{keyword}));
    }
    
    /** Getter for property keywordTable.
     * @return Value of property keywordTable.
     */
    public XMLVector<XMLVector<String>> getKeywordTable() {
        return keywordTable;
    }
    
    /** Setter for property keywordTable.
     * @param keywordTable New value of property keywordTable.
     */
    public void setKeywordTable(XMLVector<XMLVector<String>> keywordTable) {
        this.keywordTable = keywordTable;
    }
    
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        
        String textBlock = block.getText().trim();
        
        // test if the current block matches a keyword
        for (List<String> keyword : keywordTable) {
            String textKeyword = keyword.get(0);
            String dataKeyword = keyword.get(1);
            String replaceTextKeyword = keyword.get(2);
            String actionKeyword = keyword.get(3);
            if (textBlock.equals(textKeyword) || (textBlock.equalsIgnoreCase(textKeyword) && !caseDependency)) {
                // set its type to "keyword"
                block.type = "keyword";
                
                // sets the "data" attribute
                if (dataKeyword.length() != 0) {
                    block.setOptionalAttribute("data", dataKeyword);
                }
                // set to "" if the attribute doesn't exist
                // otherwise it keeps the old value
                else if (block.getOptionalAttribute("data") == null)
                    block.setOptionalAttribute("data", "");
                
                // replace the text with the specified one
                // keep the original size
                if (replaceTextKeyword.length() != 0){
                    int len = block.length;
                    block.setText(replaceTextKeyword);
                    block.length = len;
                }
                
                // set the "action" attribute
                if (actionKeyword.length() != 0) {
                    block.setOptionalAttribute("action", actionKeyword);
                }
                // set to "" if the attribute doesn't exist
                // otherwise it keeps the old value
                else if (block.getOptionalAttribute("action") == null)
                    block.setOptionalAttribute("action", "");
                
                addMashupAttribute(block);
                
                xrs.hasMatched = true;
                xrs.newCurrentBlock = block;
                return (xrs);
            }
        }
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        return xrs;
    }
}
