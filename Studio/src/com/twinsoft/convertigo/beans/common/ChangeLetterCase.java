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

public class ChangeLetterCase extends JavelinExtractionRule {
	private static final long serialVersionUID = -7866771811553991135L;

	public static final int LETTER_CASE_POLICY_UPPERCASE = 0;
    public static final int LETTER_CASE_POLICY_LOWERCASE = 1;
    public static final int LETTER_CASE_POLICY_LOWERCASE_FIRST_UPPERCASE = 2;

    /** Holds value of property letterCase. */
    private int letterCasePolicy = LETTER_CASE_POLICY_UPPERCASE;
    
    /** Creates new Date */
    public ChangeLetterCase() {
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
    @Override
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
        String text = block.getText();
        switch(letterCasePolicy) {
            default:
            case LETTER_CASE_POLICY_UPPERCASE:
                text = text.toUpperCase();
                break;
            case LETTER_CASE_POLICY_LOWERCASE:
                text = text.toLowerCase();
                break;
                
            case LETTER_CASE_POLICY_LOWERCASE_FIRST_UPPERCASE:
            	if (text.length() > 1) {
	                String temp = text.substring(1);
	                temp = temp.toLowerCase();
	                text = text.charAt(0) + temp;
            	}
                break;
        }
        block.setText(text);
        
        JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        xrs.hasMatched = true;
        xrs.newCurrentBlock = block;
        return xrs;
    }
    
    /** Getter for property letterCasePolicy.
     * @return Value of property letterCasePolicy.
     */
    public int getLetterCasePolicy() {
        return this.letterCasePolicy;
    }
    
    /** Setter for property letterCasePolicy.
     * @param letterCase New value of property letterCasePolicy.
     */
    public void setLetterCasePolicy(int letterCasePolicy) {
        this.letterCasePolicy = letterCasePolicy;
    }
    
}
