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

import java.util.Collection;
import java.util.LinkedList;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.twinj.iJavelin;

/**
 * 
 */
public class CheckBox extends JavelinExtractionRule {
	private static final long serialVersionUID = -8008621110582912136L;
	
	/* Properties */
	private 	String 			checkPattern 	= "/";
	private 	String 			uncheckPattern 	= "";
	private 	String 			Label 			= "";
	private 	XMLRectangle 	checkBoxDesc 	= new XMLRectangle(-1, -1, -1, -1);
	
	/* Working variables */
	transient 	boolean 		bFirstTime;
	transient 	boolean 		bSearch;
	transient	boolean			bAdded;
	transient 	String 			name 			= "untitled";
    	
	
	public CheckBox() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.twinsoft.convertigo.beans.core.ExtractionRule#init(int)
	 */
	@Override
	public void init(int reason) {
		super.init(reason);
		bFirstTime = true;
		bSearch = false;
		bAdded = false;
	}

	@Override
	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        
		Block nextBlock;
        XMLRectangle position = new XMLRectangle(-1, -1, -1, -1);
        String label = "";
        boolean checked = false;
        Collection<Block> blocksToDelete = new LinkedList<Block>();
        boolean bLastBlock = false;
        
        if (bFirstTime) {
        	
        	// if position is defined
        	if (checkBoxDesc.x != -1 && checkBoxDesc.y != -1) { 
        		// defined checkbox
        		if (checkBoxDesc.width == -1) 	checkBoxDesc.width = 1;
				if (checkBoxDesc.height == -1) 	checkBoxDesc.height = 1;
			
			// if no position is defined and check pattern or uncheck pattern is defined
        	} else if (!checkPattern.equals("") || !uncheckPattern.equals("")) { 
        		// searching all checkboxes on the screen
        		bSearch = true;
	        	bFirstTime = false;
        	
	        // no position and no check/uncheck pattern but label defined
        	} else if (!Label.equals("")) { 
        		// defined checkbox
        		// initialize the position of the checkbox
	        	if (checkBoxDesc.x == -1) 		checkBoxDesc.x = 0;
				if (checkBoxDesc.y == -1) 		checkBoxDesc.y = 0;
        		if (checkBoxDesc.width == -1) 	checkBoxDesc.width = 1;
				if (checkBoxDesc.height == -1) 	checkBoxDesc.height = 1;
        	
			// no position, no check pattern, no label defined
        	} else { 
        		// use default check and uncheck patterns
        		checkPattern = "/";
        		uncheckPattern = "";
        		// searching all checkboxes on the screen
        		bSearch = true;
        		bFirstTime = false;
        	}
        }
        
        if (bSearch) {
	    	nextBlock = blockFactory.getNextBlock(block);
	    	
	        if (block.type.equals("field")
	        	&& block.length == 1
	        	&& (block.getText().equals(checkPattern) || block.getText().equals(uncheckPattern)) 
	        	) {
	            
	        	// test if the next block is a label
	        	if (nextBlock != null
	        		&& canBlockBeSelected(nextBlock)
	        		&& nextBlock.type.equals("static")) {
	        		
	        		// label found
	        		label = nextBlock.getText();
	        		
	        		name 		= block.name;	        		
	        		position.y 	= block.line;
	        		position.x 	= block.column;
	        		
	        		checked = block.getText().equals(checkPattern) ? true : false;
	        		
	        		blocksToDelete.add(block);
	        		blocksToDelete.add(nextBlock);
	        		
	        	} else {
	        		xrs.hasMatched = false;
	        		xrs.newCurrentBlock = block;
	        		return xrs;
	        	}
	        } else {
	        	xrs.hasMatched = false;
	    		xrs.newCurrentBlock = block;
	    		return xrs;
	        }
        } else if (bAdded) {
        	xrs.hasMatched = false;
        	xrs.newCurrentBlock = block;
        	return xrs;
        }
        
        if (!bAdded || bSearch) {
        	bFirstTime = false;
        	
        	if (!bSearch) {
        		if (blockFactory.getNextBlock(block) == null) {
    	    	// case block is the last one : insert block after !
    	    		bLastBlock = true;
    	    	} else {
    	        // block is not the last one in block factory
    		    	// test if the block matches to add the block checkbox just before
		            if (block.line < checkBoxDesc.y) {
		            	xrs.hasMatched = false;
		            	xrs.newCurrentBlock = block;
		            	return xrs;
		            } else if (block.line == checkBoxDesc.y) {
		            	if (block.column < checkBoxDesc.x) {
		            		xrs.hasMatched = false;
		                	xrs.newCurrentBlock = block;
		                	return xrs;
		            	}
		            }
    	    	}
        	}
        	
        	Block myCheckBox;
        	
        	if (bSearch) {
        		myCheckBox = createCheckBox(label, position, checked);
    		} else {
    			myCheckBox = createCheckBox(Label, checkBoxDesc, false);
    		}
				
        	if (bLastBlock) {
    			blockFactory.insertBlock(myCheckBox, block);
    		} else {
    			blockFactory.insertBlock(myCheckBox, blockFactory.getPreviousBlock(block));
    		}
			blockFactory.removeBlocks(blocksToDelete);
			bAdded = true;			
			if (bSearch) {
				xrs.newCurrentBlock = myCheckBox;
			} else {
				xrs.newCurrentBlock = block;
			}
	        xrs.hasMatched = true;
        }
        return xrs;
	}

	private Block createCheckBox(String label, XMLRectangle zone, boolean bChecked) {
		// build the block
		Block myCheckBox = new Block();
		myCheckBox.name = name;
		myCheckBox.type = "checkbox";
		myCheckBox.setText(label);
		myCheckBox.bRender = true;
		myCheckBox.line = zone.y;
		myCheckBox.column = zone.x;
		
		myCheckBox.setOptionalAttribute("checked", ""+bChecked);
		myCheckBox.setOptionalAttribute("checkingPattern", checkPattern);
		myCheckBox.setOptionalAttribute("uncheckingPattern", uncheckPattern);
		
		return myCheckBox;
	}
	
	public String getCheckPattern() {
		return checkPattern;
	}

	public void setCheckPattern(String checkPattern) {
		this.checkPattern = checkPattern;
	}

	public XMLRectangle getCheckBoxDesc() {
		return checkBoxDesc;
	}

	public void setCheckBoxDesc(XMLRectangle checkBoxDesc) {
		this.checkBoxDesc = checkBoxDesc;
	}

	public String getLabel() {
		return Label;
	}

	public void setLabel(String label) {
		Label = label;
	}

	public String getUncheckPattern() {
		return uncheckPattern;
	}

	public void setUncheckPattern(String uncheckPattern) {
		this.uncheckPattern = uncheckPattern;
	}
    
}
