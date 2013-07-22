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
 * 
 */
public class MoveBlocks extends JavelinExtractionRule {
	
	private static final long serialVersionUID = -2478273073722142656L;

	/* Properties */
	/** Holds value of property fieldDesc. */
	private XMLRectangle fieldDesc 	= new XMLRectangle(-1, -1, -1, -1);

	/**
     * Determines if the move applied on blocks is relative or at the exact position.
     */
    private boolean isRelative 	= true;
    
        
    /* Working variables */
	transient boolean bFirstTime;
	
    
	public MoveBlocks() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.twinsoft.convertigo.beans.core.ExtractionRule#init(int)
	 */
	public void init(int reason) {
		// TODO Auto-generated method stub
		super.init(reason);
		bFirstTime = true;
	}

	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        
        if (bFirstTime) {
			bFirstTime = false;
			
			if (fieldDesc.x == -1) 		fieldDesc.x = 0;
			if (fieldDesc.y == -1) 		fieldDesc.y = 0;
			if (fieldDesc.width == -1) 	fieldDesc.width = 6;
			if (fieldDesc.height == -1) fieldDesc.height = 1;
		}
        
        // block's move
    	if (isRelative()) {
    		// search the move to apply
    		int x = ( getSelectionScreenZone().x != -1 ? getSelectionScreenZone().x : 0);
    		int y = ( getSelectionScreenZone().y != -1 ? getSelectionScreenZone().y : 0);
    		// moves the block itself
    		block.column = block.column + fieldDesc.x - x;
    		block.line = block.line + fieldDesc.y - y;
//    		// moves the sub-elements
// disabled because extended mode panels children are treated by selecting the apply extraction rules in panels property
// and old fashioned panels children are treated because they are still referenced into the block factory 
//    		Iterator ite = block.optionalChildren.iterator();
//    		Element elem;
//    		Block blk;
//    		Object obj;
//    		while (ite.hasNext()) {
//    			obj = ite.next();
//    			if (obj instanceof Element) {
//    				elem = (Element)obj;
//	    			// moves element's line
//	    			if (elem.hasAttribute("line"))
//	    				elem.setAttribute("line", "" + (Integer.parseInt(elem.getAttribute("line")) + fieldDesc.y - y));
//	    			// moves element's column
//	    			if (elem.hasAttribute("column"))
//	    				elem.setAttribute("column", "" + (Integer.parseInt(elem.getAttribute("column")) + fieldDesc.x - x));
//    			} else if (obj instanceof Block) {
//    				blk = (Block)obj;
//	    			// moves element's line
//	    			blk.line = blk.line + fieldDesc.y - y;
//	    			// moves element's column
//	    			blk.column = blk.column + fieldDesc.x - x;
//    			}
//    		}
    	} else {
    		// moves the block itself
    		block.column = fieldDesc.x;
    		block.line = fieldDesc.y;
//    		// moves the sub-elements
// disabled because extended mode panels children are treated by selecting the apply extraction rules in panels property
// and old fashioned panels children are treated because they are still referenced into the block factory
//    		Iterator ite = block.optionalChildren.iterator();
//    		Element elem;
//    		Block blk;
//    		Object obj;
//    		while (ite.hasNext()) {
//    			obj = ite.next();
//				if (obj instanceof Element) {
//	    			elem = (Element)obj;
//	    			// moves element's line
//	    			if (elem.hasAttribute("line")) {
//	    				elem.removeAttribute("line");
//	    				elem.setAttribute("line", "" + fieldDesc.x);
//	    			}
//	    			// moves element's column
//	    			if (elem.hasAttribute("column")) {
//	    				elem.removeAttribute("column");
//	    				elem.setAttribute("column", "" + fieldDesc.y);
//	    			}
//				} else if (obj instanceof Block) {
//    				blk = (Block)obj;
//	    			// moves element's line
//	    			blk.line = blk.line + fieldDesc.y;
//	    			// moves element's column
//	    			blk.column = blk.column + fieldDesc.x;
//    			}
//    		}
    	}
    	
    	xrs.hasMatched = true;
    	xrs.newCurrentBlock = block;
        return xrs;
	}

	/**
	 * @return Returns the fieldDesc.
	 */
	public XMLRectangle getFieldDesc() {
		return fieldDesc;
	}

	/**
	 * @param fieldDesc
	 *            The fieldDesc to set.
	 */
	public void setFieldDesc(XMLRectangle fieldDesc) {
		this.fieldDesc = fieldDesc;
	}

	/**
     * Determines if the move applied to blocks is relative or not.
     *
     * @return <code>true</code> if the move is relative,
     * <code>false</code> otherwise.
     */
    public boolean isRelative() {
        return isRelative;
    }
    
    /**
     * @param isRelative
     * 			The isRelative to set.
     */
    public void setRelative(boolean isRelative) {
        this.isRelative = isRelative;
    }
    
}
