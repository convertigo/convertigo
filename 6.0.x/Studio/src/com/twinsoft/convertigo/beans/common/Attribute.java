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
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule;
import com.twinsoft.twinj.iJavelin;

public class Attribute extends JavelinMashupEventExtractionRule {

	public static final long serialVersionUID = -6735666205939933877L;
    
	/* Properties */
    /** Holds value of property attributeName. */
    private String attributeName = "";
    
    /** Holds value of property attributeValue. */
    private String attributeValue = "";
    
    
    public Attribute() {
        super();
    }

    
    /**
     * Applies the extraction rule to the current iJavelin object.
     *
     * @param javelin the Javelin object.
     * @param block the current block to analyze.
     * @param blockFactory the block context of the current block.
     *
     * @return an ExtractionRuleResult object containing the result of
     * the query.
     */
    @Override
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        
        block.setOptionalAttribute(attributeName, attributeValue);
        addMashupAttribute(block);
        
        xrs.hasMatched = true;
        xrs.newCurrentBlock = block;
        return xrs;
    }

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}


	public String getAttributeValue() {
		return attributeValue;
	}


	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}
     
}
