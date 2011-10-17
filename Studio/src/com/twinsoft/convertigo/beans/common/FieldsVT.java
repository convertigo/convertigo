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
 * This class manages command for the VT Emulator,
 * it extends the com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule class.
 * 
 */
public class FieldsVT extends JavelinExtractionRule {

	private static final long serialVersionUID = -6812939791692598667L;

    public FieldsVT() {
        super();
    }
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
     	// the block is a field
        xrs.hasMatched = true;
        xrs.newCurrentBlock = block;
    	
    	// setting type attribute
    	block.type = "field";
    	// setting name attribute
    	block.name = "field_c" + block.column + "_l" + block.line;
    	// setting size attribute
    	block.setOptionalAttribute("size", Integer.toString(block.length));
    	
    	return (xrs);
    }
    
}
