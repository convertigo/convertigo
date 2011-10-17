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
 * This class tests if the block is in the desired zone 
 * and deletes the spaces at the begining and the end of the block
 * keeps the original size of the block
 */
public class TrimBlock extends JavelinExtractionRule {
    
	private static final long serialVersionUID = 4353717962836088588L;

	/* Properties */
	private boolean bTrimRight 	= true;
	private boolean bTrimLeft 	= true;
	
	
	/** Creates a new instance of Trim */
    public TrimBlock() {
        super();
    }
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        
        String temp = block.getText();
        int len = temp.length();
        int firstNonSpace = 0;
        
        for (firstNonSpace = 0 ; firstNonSpace < len ; firstNonSpace++) {
            if (temp.charAt(firstNonSpace) != ' ')
                break;
        }
            
        if (bTrimRight && bTrimLeft) {
	        block.setText(temp.trim());
	        // block.length = block.getText().length(); 
	        // ==> block.setText already changes the length
	        block.column += firstNonSpace;
	        
	        xrs.hasMatched = true; 
	        xrs.newCurrentBlock = block;
	        return xrs;
        } 
        if (bTrimRight) {
        	block.setText(rtrim(temp));
        	
        	xrs.hasMatched = true; 
	        xrs.newCurrentBlock = block;
	        return xrs;
        }
        if (bTrimLeft) {
        	block.setText(ltrim(temp));
        	block.column += firstNonSpace;
	        
        	xrs.hasMatched = true; 
	        xrs.newCurrentBlock = block;
	        return xrs;
        }
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        return xrs;
    }

	public boolean isBTrimLeft() {
		return bTrimLeft;
	}

	public void setBTrimLeft(boolean trimLeft) {
		bTrimLeft = trimLeft;
	}

	public boolean isBTrimRight() {
		return bTrimRight;
	}

	public void setBTrimRight(boolean trimRight) {
		bTrimRight = trimRight;
	}
	
	/* remove left whitespace */
	private String ltrim(String source) {
		return source.replaceAll("^\\s+", "");
	}

	/* remove right whitespace */
	private String rtrim(String source) {
		return source.replaceAll("\\s+$", "");
	}
}
