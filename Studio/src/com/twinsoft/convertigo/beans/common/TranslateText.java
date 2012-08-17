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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.1.3/Studio/src/com/twinsoft/convertigo/beans/common/ReplaceText.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.beans.common;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.twinj.iJavelin;

public class TranslateText extends JavelinExtractionRule {

	public static final long serialVersionUID = -6735666205939933877L;
    
    /** Holds value of property dictionary. */
    private String dictionary = " ";
    
    /** Creates new ReplaceText */
    public TranslateText() {
        super();
    }
    
    /**
     * Applies the extraction rule to the current iJavelin object.
     *
     * @param javelin the Javelin object.
     * @param block the current block to analyze.
     * @param blockFactory the block context of the current block.
     * @param dom the XML DOM.
     *
     * @return an ExtractionRuleResult object containing the result of
     * the query.
     */
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs  =  new JavelinExtractionRuleResult();
    	
    	// Translate the block text
        //block.setText(strEx.toString());
        
        xrs.hasMatched = true;
        xrs.newCurrentBlock = block;
        return xrs;
    }
    
    /** Getter for property dictionary.
     * @return Value of property dictionary.
     */
    public String getDictionary() {
        return dictionary;
    }
    
    /** Setter for property dictionary.
     * @param dictionary New value of property dictionary.
     */
    public void setDictionary(String dictionary) {
        this.dictionary = dictionary;
    }
}
