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

package com.twinsoft.convertigo.beans.vdx;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.twinj.iJavelin;

/**
 * This class manages commands,
 * it extends the com.twinsoft.convertigo.beans.common.Command class
 * It's recommended to use the spiltBlock Extraction rule before this one
 * This extraction rule manages keywords ending with a dot like Keyword. instead of Keyword
 * and combinaisons such as "*Keyword", "* Keyword", "*+Keyword" and "* + Keyword"
 * in one or several blocks.
 * if a star (*) is found before a keyword, the "data" attribute will be set to *
 * if data is specified and a star is found the "data" attribute will be set to "*<data specified>"
 *
 */
public class CommandVdx extends com.twinsoft.convertigo.beans.common.Command {
    public static final long serialVersionUID = 4350644982787619412L;
    
    private boolean starFlag = true;
    
    public CommandVdx() {
        super();
        
        addNewKeyword("annulation", "", "", "KAnnulation");
        addNewKeyword("correction", "", "", "KCorrection");
        addNewKeyword("repetition", "", "", "KRepetition");
        addNewKeyword("sommaire", "", "", "KSommaire");
        addNewKeyword("retour", "", "", "KRetour");
        addNewKeyword("envoi", "", "", "KEnvoi");
        addNewKeyword("guide", "", "", "KGuide");
        addNewKeyword("suite", "", "", "KSuite");
        addNewKeyword("annul", "", "", "KAnnulation");
        addNewKeyword("correc", "", "", "KCorrection");
        addNewKeyword("repet", "", "", "KRepetition");
        addNewKeyword("somm", "", "", "KSommaire");
        addNewKeyword("cnx/fin", "", "", "KConnection");
    }
    
    
    /** Getter for property bStarFlag.
     * @return Value of property bStarFlag.
     */
    public boolean isStarFlag() {
        return starFlag;
    }
    
    /** Setter for property bStarFlag.
     * @param bStarFlag New value of property bStarFlag.
     */
    public void setStarFlag(boolean bStarFlag) {
        this.starFlag = bStarFlag;
    }
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        String  textBlock = block.getText().trim();
        XMLVector<XMLVector<String>> keywordTable = getKeywordTable();
        boolean foundStar = false;
        
        // handle keyword ending with a dot, ie : "Somm."
        if (textBlock.endsWith("."))
            textBlock = textBlock.substring(0, textBlock.length() -1);
        
        // handle keyword like "*Envoi", "*+Envoi", "* Envoi", "* + Envoi"
        if (isStarFlag()){
            Block prevBlock = blockFactory.getPreviousBlock(block);
            
            // in one block
            if (textBlock.startsWith("*")){
                foundStar = true;
                textBlock = textBlock.substring(1, textBlock.length());
                if (textBlock.startsWith(" ")){
                    textBlock = textBlock.substring(1, textBlock.length());
                }
                if (textBlock.startsWith("+")){
                    textBlock = textBlock.substring(1, textBlock.length());
                }
                if (textBlock.startsWith(" ")){
                    textBlock = textBlock.substring(1, textBlock.length());
                }
            }
            
            // in several blocks
            if ((prevBlock != null) && (prevBlock.getText().equals(" "))){
                prevBlock = blockFactory.getPreviousBlock(prevBlock);
            }
            if ((prevBlock != null) && (prevBlock.getText().equals("+"))){
                prevBlock = blockFactory.getPreviousBlock(prevBlock);
            }
            if ((prevBlock != null) && (prevBlock.getText().equals(" "))){
                prevBlock = blockFactory.getPreviousBlock(prevBlock);
            }
            if ((prevBlock != null) && (prevBlock.getText().equals("*"))){
                foundStar = true;
            }
        }
        
        // test if the current block matches a keyword
        for (int i = 0; i < keywordTable.size(); i++) {
            XMLVector<String> keyword = keywordTable.get(i);
            String textKeyword = keyword.get(0);
            String dataKeyword = keyword.get(1);
            String replaceTextKeyword = keyword.get(2);
            String actionKeyword = keyword.get(3);
            if (textBlock.equals(textKeyword) || (textBlock.equalsIgnoreCase(textKeyword) && !isCaseDependency())) {
                // set its type to "keyword"
                block.type = "keyword";
                
                // set the "data" attribute to *
                if (foundStar)
                    block.setOptionalAttribute("data", "*");
                
                // set or update the "data" attribute
                // may contains a "*"
                if (dataKeyword.length() != 0) {
                    String data = block.getOptionalAttribute("data");
                    if (data != null)
                        block.setOptionalAttribute("data", data + dataKeyword);
                    else
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
                
                xrs.hasMatched = true;
                xrs.newCurrentBlock = block;
                return xrs;
            }
        }
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        return xrs;
    }
    
    
}
