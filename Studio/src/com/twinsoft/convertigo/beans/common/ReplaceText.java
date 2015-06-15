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
import com.twinsoft.util.StringEx;
import com.twinsoft.util.TextCodec;

public class ReplaceText extends JavelinExtractionRule {

	public static final long serialVersionUID = -6735666205939933877L;
    
    /** Holds value of property searchedText. */
    private String searchedText = " ";
    
    /** Holds value of property replacedText. */
    private String replacedText = UTF8Decode("&#160;");
    
    /** Holds value of property regExp. */
    private boolean regExp = false;
    
    /** Creates new ReplaceText */
    public ReplaceText() {
        super();
    }
    
    private String UTF8Decode(String str) {
        final int STATE_NORMAL   = 0;
        final int STATE_ESCAPE   = 1;
        final int STATE_GOTDIEZE = 2;
        
        StringBuffer res = new StringBuffer("");
        int state = STATE_NORMAL;
        char c ;
        String code="";
        
        for (int i =0; i< str.length() ; i++) {
            c = str.charAt(i);
            switch (state) {
                case STATE_NORMAL:
                    if (c == '&')
                        state = STATE_ESCAPE;
                    else
                        res.append(c);
                    break;
                    
                case STATE_ESCAPE:
                    if (c == '#') {
                        state = STATE_GOTDIEZE;
                        code = "";
                    }
                    break;
                    
                case STATE_GOTDIEZE:
                    if (c == ';') {
                        byte b[] = new byte[1];
                        try {
                            state = STATE_NORMAL;
                            b[0] = (byte)Integer.parseInt(code);
                            res.append(new String(b, "ISO8859-1"));
                        }
                        catch (Exception e) {
                            return (res.toString());
                        }
                    }
                    else
                        code += c;
                    break;
            }
        }
        return (res.toString());
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
    	
    	// save the original len of the block
        int len = block.length;
        
    	if (!regExp) {
	    	StringEx strEx;
	        
	        strEx = new StringEx(block.getText());
	        strEx.replaceAll(searchedText, replacedText);
	        
	        block.setText(strEx.toString());
	    } else {
    		block.setText(block.getText().replaceAll(searchedText, replacedText));
    	}
        // restore the len
        block.length = len;
	
        
        xrs.hasMatched = true;
        xrs.newCurrentBlock = block;
        return xrs;
    }
    
    /** Getter for property searchedText.
     * @return Value of property searchedText.
     */
    public String getSearchedText() {
        return TextCodec.UTF8Encode(searchedText);
    }
    
    /** Setter for property searchedText.
     * @param searchedText New value of property searchedText.
     */
    public void setSearchedText(String searchedText) {
        this.searchedText = UTF8Decode(searchedText);
    }
    
    /** Getter for property replacedText.
     * @return Value of property replacedText.
     */
    public String getReplacedText() {
        return TextCodec.UTF8Encode(this.replacedText);
    }
    
    /** Setter for property replacedText.
     * @param replacedText New value of property replacedText.
     */
    public void setReplacedText(String replacedText) {
        this.replacedText = UTF8Decode(replacedText);
    }

	public boolean isRegExp() {
		return regExp;
	}

	public void setRegExp(boolean regExp) {
		this.regExp = regExp;
	}
}
