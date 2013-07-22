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

/**
 * 
 */
public class Container extends JavelinMashupEventExtractionRule {
    public static final long serialVersionUID = 4350644982787619412L;
    
    /* Properties */
    private 	String 			tagName 		= "";
    private 	XMLRectangle	containerDesc 	= new XMLRectangle(-1, -1, -1, -1);
    
    /* Working variables */
    transient 	boolean[] 		attributes;
    transient 	boolean 		bFirstTime;
    
    
    public Container() {
        super();
    }
    
    /* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.ExtractionRule#init(int)
	 */
	public void init(int reason) {
		// TODO Auto-generated method stub
		super.init(reason);
		bFirstTime = true;
		attributes = new boolean[] { true, true, true, true, true, true, true, true, true, true, true };
	}
	
	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;

		if (!bFirstTime) {
			xrs.hasMatched = false;
        	xrs.newCurrentBlock = block;
        	return xrs;
		}
		
		if (containerDesc.x == -1) 		containerDesc.x = 0;
    	if (containerDesc.y == -1) 		containerDesc.y = 0;
    	bFirstTime = false;
        
        
        // create the container
        Block container = new Block();
    	if (tagName.length() != 0)
            container.tagName = this.tagName;
        else
        	container.tagName = "container";
		container.bRender = true;
		container.type = "container";
		container.setText("");
		container.column = containerDesc.x;
		container.line = containerDesc.y;
		container.setOptionalAttribute("width", "" + containerDesc.width);
		container.setOptionalAttribute("height", "" + containerDesc.height);
		
		addMashupAttribute(container);
		
		// block to go through the block factory
		Block tmpBlock 		= blockFactory.getFirstBlock();
		Block saveBlock 	= null;
		// block to memorize container's place in block factory
		Block blockBeforeContainer 	= null;
		
		while (tmpBlock != null) {
        	// look for the container's place in blockFactory
			
//        	if (tmpBlock.line > containerDesc.y // if tmpBlock is on a line superior to container  
//        		|| tmpBlock.line == containerDesc.y && tmpBlock.column >= containerDesc.x) { // or if tmpBlock is on the same line as container but on a column superior
//    			// tmpBlock is after the container
//        		blockBeforeContainer = null;
//    		} else {
    			if ( tmpBlock.line < containerDesc.y // if tmpBlock is on a line inferior to container's
    				|| (tmpBlock.line == containerDesc.y && tmpBlock.column < containerDesc.x) ) { // or tmpBlock is on the good line but on a column inferior to container's
    				// the container must be set after this block 
    				blockBeforeContainer = tmpBlock;
    			}
//    		}
        	
        	// add block to the container if it matches selection zone and attributes
        	// skip final blocks
            if (tmpBlock.bFinal) {
            	tmpBlock = blockFactory.getNextBlock(tmpBlock);
            	continue;
            }
            // test if the block is matching selection and attributes criterias
        	if (canBlockBeSelected(tmpBlock)) {
				// add the block to the container
				container.addOptionalChildren(tmpBlock);
//				container.addOptionalChildren(tmpBlock.toXML(dom, attributes));
				// go on to the next block
				saveBlock = tmpBlock;
	        	tmpBlock = blockFactory.getNextBlock(tmpBlock);
	        	// remove the old block from the block factory
	        	blockFactory.removeBlock(saveBlock);
        	} else {
        		// go on to the next block
            	tmpBlock = blockFactory.getNextBlock(tmpBlock);
        	}
        }
        
    	// add the container to the block factory
    	blockFactory.insertBlock(container, blockBeforeContainer);
		
        xrs.hasMatched = true;
		xrs.newCurrentBlock = blockFactory.getNextBlock(container);
		return xrs;
    }

    
    public XMLRectangle getContainerDesc() {
		return containerDesc;
	}

	public void setContainerDesc(XMLRectangle containerDesc) {
		this.containerDesc = containerDesc;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	
}
