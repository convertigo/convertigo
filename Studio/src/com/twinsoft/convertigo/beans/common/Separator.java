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

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.twinj.iJavelin;

/**
 * This class manages separator breaks.
 */
public class Separator extends JavelinExtractionRule {
	
    public static final long serialVersionUID = -5429197512081853261L;
    
    /**
     * The list of characters that match with separator breaks.
     */
    private String separatorCharacters = "-_";
    
    /** Holds value of property tagName. */
    private String tagName = "";
    
    /** The minimum number of occurences of the character to find a separator. */
    private int nbOccurrences = 10;
    
    public String getSeparatorCharacters() {
        return separatorCharacters;
    }
    
    public void setSeparatorCharacters(String separatorCharacters) {
        this.separatorCharacters = separatorCharacters;
    }
    
    public Separator() {
        super();
        setFinal(true);
    }
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        xrs.newCurrentBlock = block;
        xrs.hasMatched = false;
        
        String blockText = block.getText();
        int len = blockText.length();
        
        if (len <= nbOccurrences)
            return xrs;
        
        boolean found;
        int separatorCharactersLength = separatorCharacters.length();
        char c1, c2;
        int nbOcc = 0;

        for (int i = 0 ; i < separatorCharactersLength ; i++) {
            found = true;
            c1 = separatorCharacters.charAt(i);
            for (int j = 0 ; j < len ; j++) {
                c2 = blockText.charAt(j);
                if (c1 != c2) {
                    found = false;
                    if (j != 0)
                        return xrs;
                    else
                        break;
                } else {
                	nbOcc ++;
                }
            }
            if (found && nbOcc >= nbOccurrences) {
                if (this.tagName.length() != 0)
                    block.tagName = this.tagName;
                
                block.type = "separator";
                block.setOptionalAttribute("width", String.valueOf(nbOcc));
                block.setText("");
                xrs.hasMatched = true;
                xrs.newCurrentBlock = block;
                return xrs;
            }
        }
        
        return xrs;
    }
    
    /** Getter for property tagName.
     * @return Value of property tagName.
     */
    public String getTagName() {
        return this.tagName;
    }
    
    /** Setter for property tagName.
     * @param tagName New value of property tagName.
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

	public int getNbOccurrences() {
		return nbOccurrences;
	}

	public void setNbOccurrences(int nbOccurrences) {
		this.nbOccurrences = nbOccurrences;
	}
}
