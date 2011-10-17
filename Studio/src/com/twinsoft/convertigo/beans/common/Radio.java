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
import java.util.List;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.twinj.iJavelin;

/**
 * 
 */
public class Radio extends JavelinExtractionRule {
	
	private static final long serialVersionUID = 6396795057674382063L;
	
	/* Properties */
	private 	String 			separatorChars 	= ". ";
	private 	XMLRectangle 	radioDesc 		= new XMLRectangle(-1, -1, -1, -1);
    private 	XMLVector<XMLVector<Object>> options	= new XMLVector<XMLVector<Object>>();
    
    /* Working variables */
	transient 	boolean 		bFirstTime;
	transient 	boolean 		bSearch;
	transient	boolean		bAdded;
	
	public Radio() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.twinsoft.convertigo.beans.core.ExtractionRule#init(int)
	 */
	@Override
	public void init(int reason) {
		// TODO Auto-generated method stub
		super.init(reason);
		bFirstTime = true;
		bSearch = false;
		bAdded = false;
	}

	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        
		Block nextBlock;
        XMLVector<XMLVector<Object>> foundOptions = new XMLVector<XMLVector<Object>>();
        Collection<Block> blocksToDelete = new LinkedList<Block>();
        String currentOption = "";
        String direction = "horizontal";
        boolean bLastBlock = false;
        
        if (bFirstTime) {
        	if (radioDesc.x != -1 && radioDesc.y != -1) { // if position is defined
        		// defined radio button
        		if (radioDesc.width == -1) 	radioDesc.width = 1;
				if (radioDesc.height == -1) 	radioDesc.height = 1;
        	} else if (!separatorChars.equals("")) { // if no position is defined and separator is defined
        		// searching all radio buttons on the screen
        		bSearch = true;
	        	bFirstTime = false;
        	} else if (!options.isEmpty()) { // no position and no separator but options defined
        		// defined radio button
        		// initialize the position of the radio button
	        	if (radioDesc.x == -1) 		radioDesc.x = 0;
				if (radioDesc.y == -1) 		radioDesc.y = 0;
        		if (radioDesc.width == -1) 	radioDesc.width = 1;
				if (radioDesc.height == -1) radioDesc.height = 1;
        	} else { // no position, no separator, no options defined
        		// use default check pattern
        		separatorChars = ". ";
        		// searching all radio buttons on the screen
        		bSearch = true;
        		bFirstTime = false;
        	}
        }
        
        if (bSearch) {
	    	nextBlock = blockFactory.getNextBlock(block);
	    	
	    	// if next block = null or is not on the same line : not matching
	    	if (nextBlock == null || nextBlock.line != block.line) {
	    		xrs.hasMatched = false;
        		xrs.newCurrentBlock = block;
        		return xrs;
	    	}
	    	
	    	// if block is a field and next block is a matching option
	        if (block.type.equals("field")
	        	&& nextBlock != null
	        	&& canBlockBeSelected(nextBlock)
	        	&& nextBlock.type.equals("static")
	        	&& nextBlock.getText().indexOf(separatorChars) != -1) {
	        		
        		// first option found
	        	foundOptions.add(createOption(nextBlock));
        		
	        	blocksToDelete.add(nextBlock);
	        	
	        	currentOption = block.getText().trim();
        		
        		// searching for the direction of options
        		if (blockFactory.getEastBlock(nextBlock) != null
        			&& canBlockBeSelected(blockFactory.getEastBlock(nextBlock))
        			&& blockFactory.getEastBlock(nextBlock).type.equals("static")
    	        	&& blockFactory.getEastBlock(nextBlock).getText().indexOf(separatorChars) != -1) {
        			
        			// options are on the same line
        			direction = "horizontal";
        			do {
        				nextBlock = blockFactory.getEastBlock(nextBlock);
        				foundOptions.add(createOption(nextBlock));
        				blocksToDelete.add(nextBlock);
        			} while(blockFactory.getEastBlock(nextBlock) != null
        					&& canBlockBeSelected(blockFactory.getEastBlock(nextBlock))
        					&& blockFactory.getEastBlock(nextBlock).type.equals("static")
            	        	&& blockFactory.getEastBlock(nextBlock).getText().indexOf(separatorChars) != -1 );

        		} else if ( blockFactory.getSouthBlock(nextBlock) != null
	        				&& canBlockBeSelected(blockFactory.getSouthBlock(nextBlock))
	        				&& blockFactory.getSouthBlock(nextBlock).type.equals("static")
	        				&& blockFactory.getSouthBlock(nextBlock).getText().indexOf(separatorChars) != -1) {
        			
        			// options are on the same column
        			direction = "vertical";
        			do {
        				nextBlock = blockFactory.getSouthBlock(nextBlock);
        				foundOptions.add(createOption(nextBlock));
        				blocksToDelete.add(nextBlock);
        			} while(blockFactory.getSouthBlock(nextBlock) != null
        					&& canBlockBeSelected(blockFactory.getSouthBlock(nextBlock))
        					&& blockFactory.getSouthBlock(nextBlock).type.equals("static")
            	        	&& blockFactory.getSouthBlock(nextBlock).getText().indexOf(separatorChars) != -1 );
        			
        		} else { } // no other option
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
		            if (block.line < radioDesc.y) {
		            	xrs.hasMatched = false;
		            	xrs.newCurrentBlock = block;
		            	return xrs;
		            } else if (block.line == radioDesc.y) {
		            	if (block.column < radioDesc.x) {
		            		xrs.hasMatched = false;
		                	xrs.newCurrentBlock = block;
		                	return xrs;
		            	}
		            }
    	    	}
        	}
        	
        	Block myRadio;
        	
        	// build result block
        	if (bSearch) {
        		myRadio = block;
        		// add the options
            	for(List<Object> op : foundOptions){
            		Element element = dom.createElement("item");
    	        	element.setAttribute ("action", (String) op.get(0));
    	        	element.setAttribute ("value", 	(String) op.get(1));
    	        	element.setAttribute ("column", String.valueOf((Integer)op.get(2)));
    	        	element.setAttribute ("line", 	String.valueOf((Integer)op.get(3)));
    	            element.setAttribute("selected", currentOption.equalsIgnoreCase((String) op.get(0))?"true":"false");
    	        	myRadio.addOptionalChildren(element);
            	}
    		} else {
    			myRadio = new Block();
    			myRadio.bRender = true;
    			myRadio.name 	= "untitled";
    			myRadio.line 	= radioDesc.y;
    			myRadio.column 	= radioDesc.x;
    			if (bLastBlock) {
    				blockFactory.insertBlock(myRadio, block);
    			} else {
    				blockFactory.insertBlock(myRadio, blockFactory.getPreviousBlock(block));
    			}
    			// add the options
            	int item = 0;
            	for(List<Object> op : options){
            		Element element = dom.createElement("item");
    	        	element.setAttribute ("action", (String) op.get(1));
    	        	element.setAttribute ("value", 	(String) op.get(0));
    	        	element.setAttribute ("column", String.valueOf(radioDesc.x));
    	        	element.setAttribute ("line", 	String.valueOf(radioDesc.y + item));
    	        	item ++;
    	        	element.setAttribute("selected", "false");
    	        	myRadio.addOptionalChildren(element);
            	}
    		}
        	myRadio.setText("");
        	myRadio.type = "radio";
    		myRadio.setOptionalAttribute("radio", "true");
    		myRadio.setOptionalAttribute("direction", direction);
    		
        	// remove old blocks
			blockFactory.removeBlocks(blocksToDelete);
			bAdded = true;
			xrs.newCurrentBlock = bSearch ? myRadio : block;
	        xrs.hasMatched = true;
        }
        return xrs;
	}

	private XMLVector<Object> createOption (Block block) {
		XMLVector<Object> tmpOption = new XMLVector<Object>(); 
		tmpOption.add( block.getText().substring( 0, block.getText().indexOf(separatorChars) ) );
		tmpOption.add( block.getText().substring( block.getText().indexOf(separatorChars) + separatorChars.length() ) );
		tmpOption.add(new Integer(block.column));
		tmpOption.add(new Integer(block.line));
		return tmpOption;
	}
	
	
	public String getSeparatorChars() {
		return separatorChars;
	}

	public void setSeparatorChars(String separatorChars) {
		this.separatorChars = separatorChars;
	}

	public XMLRectangle getRadioDesc() {
		return radioDesc;
	}

	public void setRadioDesc(XMLRectangle radioDesc) {
		this.radioDesc = radioDesc;
	}

	public XMLVector<XMLVector<Object>> getOptions() {
		return options;
	}

	public void setOptions(XMLVector<XMLVector<Object>> options) {
		this.options = options;
	}
    
}
