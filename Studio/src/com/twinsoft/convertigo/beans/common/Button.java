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
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule;
import com.twinsoft.twinj.iJavelin;

/**
 * 
 */
public class Button extends JavelinMashupEventExtractionRule {
    public static final long serialVersionUID = 4350644982787619412L;
    
    /* Properties */
    private 	String			Label 			= "";
    private 	String			Action 			= "";
    private 	boolean			doTransaction 	= false;
    private 	XMLRectangle	buttonDesc 		= new XMLRectangle(-1, -1, -1, -1);
    public 		String 			startPattern 	= "<";
    public 		String 			endPattern 		= ">";
    
    /* Working variables */
    transient	boolean			bFirstTime;
    transient	boolean 		bSearchPos;
    transient	boolean			bAdded;
    
    
    public Button() {
        super();
    }

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.ExtractionRule#init(int)
	 */
    @Override
	public void init(int reason) {
		// TODO Auto-generated method stub
		super.init(reason);
		bFirstTime = true;
		bSearchPos = false;
		bAdded = false;
	}
    
    @Override
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        
        Block startBlock 	= null;
        Block blockTmp 		= null;
        Collection<Block> blocksToDelete = new LinkedList<Block>();
        boolean bEndBlockFound = false;
        XMLRectangle position = new XMLRectangle(-1, -1, -1, -1);
        String label = "";
        boolean bPenSelectable = false;
        boolean bLastBlock = false;
        
        if (bFirstTime) {
        	if (buttonDesc.x != -1 && buttonDesc.y != -1) { // if position is defined
        		// defined button
        		if (buttonDesc.width == -1) { 
        			if (!Label.equals("")) {
        				buttonDesc.width = Label.length();
        			} else {
        				buttonDesc.width = 6;
        			}
        		}
        		if (buttonDesc.height == -1) buttonDesc.height = 1;
        	} else if (!startPattern.equals("") && !endPattern.equals("")) { // no position is defined and start and end patterns are defined
        		// searching all buttons on the screen thanks to a start pattern and an end pattern
	        	bSearchPos = true;
	        	bFirstTime = false;
        	} else if (!Label.equals("")) { // no position, no start pattern and no end patterns are defined but label is defined
        			// defined button
        			// initialize the position of the button
    	        	if (buttonDesc.x == -1) 		buttonDesc.x = 0;
    				if (buttonDesc.y == -1) 		buttonDesc.y = 0;
    				if (buttonDesc.width == -1) 	buttonDesc.width = 6;
    				if (buttonDesc.height == -1) 	buttonDesc.height = 1;
        	} else { 
        		// use default start and end pattern
        		startPattern = "<";
        		endPattern = ">";
        		// searching all buttons on the screen
	        	bSearchPos = true;
	        	bFirstTime = false;
        	}
        }
        
        if (bSearchPos) {	
        	// search of the start block of a button
        	if (block.getText().trim().indexOf(startPattern) == 0) {
        		startBlock = block;
        		if (startBlock.getOptionalAttribute("penselectable")!= null && startBlock.getOptionalAttribute("penselectable").equals("true"))
        			bPenSelectable = true;
        		blocksToDelete.add(startBlock);
        		position.x = startBlock.column;
        		position.y = startBlock.line;
    			
        		// search of the end pattern of the button in the same block
        		if (startBlock.getText().trim().indexOf(endPattern) != -1 // block contains endPattern 
        			&& startBlock.getText().trim().indexOf(endPattern) == startBlock.getText().trim().length() - endPattern.length() // and endPattern is at the end of the block
        			) { 
        			
        			label = startBlock.getText().trim().substring(startPattern.length(), startBlock.getText().trim().indexOf(endPattern));
        			position.width = startBlock.length;
        			position.height = 1;
        		} else {
        			label = startBlock.getText().trim().substring(startPattern.length());
        			
        			// search of the end pattern in the next blocks
	        		blockTmp = block;
	        		while ( blockFactory.getNextBlock(blockTmp) != null  && !bEndBlockFound) {
	        			blockTmp = blockFactory.getNextBlock(blockTmp);
	        			if (blockTmp.getText().trim().indexOf(startPattern) != 0) { // block does not match with startPattern)
	        				
	        				if (blockTmp.getOptionalAttribute("penselectable")!= null && blockTmp.getOptionalAttribute("penselectable").equals("true"))
	                			bPenSelectable = true;
	        				blocksToDelete.add(blockTmp);
	        				
	        				if (blockTmp.getText().trim().indexOf(endPattern) != -1 // block contains endPattern 
	    	        				&& blockTmp.getText().trim().indexOf(endPattern) == blockTmp.getText().trim().length() - endPattern.length() // and endPattern is at the end of the block
	    	        				) { 
	    	        			
	        					label += " " + blockTmp.getText().trim().substring(0, blockTmp.getText().trim().indexOf(endPattern));
	        					bEndBlockFound = true;
	        					position.width = blockTmp.column - startBlock.column + 1;
	        					position.height = blockTmp.line - startBlock.line + 1;
	    	        		} else {
	    	        			label += " " + blockTmp.getText().trim();
	    	        		}
	        			} else {
	        				xrs.hasMatched = false;
	        				xrs.newCurrentBlock = blockTmp;
	        				return xrs;
	        			}
	        		}
	        		if (! bEndBlockFound) {
	        			xrs.hasMatched = false;
        				xrs.newCurrentBlock = blockTmp;
        				return xrs;
	        		}
        		}
        	}
			else {
				xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
			}
        } else if (bAdded) {
        	xrs.hasMatched = false;
        	xrs.newCurrentBlock = block;
        	return xrs;
        }
        
        if (!bAdded || bSearchPos) {
        	bFirstTime = false;
        	
        	if (!bSearchPos) {
        		if (blockFactory.getNextBlock(block) == null) {
    	    	// case block is the last one : insert block after !
    	    		bLastBlock = true;
    	    	} else {
        	    // block is not the last one in block factory
	        		// test if the block matches to add the block button just before
		            if (block.line < buttonDesc.y) {
		            	xrs.hasMatched = false;
		            	xrs.newCurrentBlock = block;
		            	return xrs;
		            } else if (block.line == buttonDesc.y) {
		            	if (block.column < buttonDesc.x) {
		            		xrs.hasMatched = false;
		                	xrs.newCurrentBlock = block;
		                	return xrs;
		            	}
		            }
    	    	}
        	}
        	
        	Block myButton;
        	
        	if (bSearchPos) {
        		myButton = createButton(label, position, false);
        		if (bPenSelectable)
        			myButton.setOptionalAttribute("penselectable", "true");
        		else
        			myButton.setOptionalAttribute("penselectable", "false");
    		} else {
    			myButton = createButton(Label, buttonDesc, doTransaction);
    		}
        	
        	addMashupAttribute(myButton);
        	
        	if (bLastBlock) {
    			blockFactory.insertBlock(myButton, block);
    		} else {
    			blockFactory.insertBlock(myButton, blockFactory.getPreviousBlock(block));
    		}
			blockFactory.removeBlocks(blocksToDelete);
			bAdded = true;
			if (bSearchPos) {
				xrs.newCurrentBlock = myButton;
			} else {
				xrs.newCurrentBlock = block;
			}
	        xrs.hasMatched = true;
        }
        return xrs;
    }

    /**
     * Creates the button block
     * @param label
     * @param zone
     * @param bTransaction
     * @return the built block
     */
    private Block createButton(String label, XMLRectangle zone, boolean bTransaction) {
    	Block myButton = new Block();
		myButton.name = "untitled";
		myButton.type = "keyword";
		myButton.setText(label);
		myButton.bRender = true;
		
		myButton.setOptionalAttribute("action", Action);
		
		myButton.line = zone.y;
		myButton.column =  zone.x;
		myButton.setOptionalAttribute("width", "" +  zone.width);
		myButton.setOptionalAttribute("height", "" +  zone.height);
    	
		if (bTransaction)
			myButton.setOptionalAttribute("dotransaction", "true");
		
		return myButton;
    }
    
	/**
	 * @return Returns the action.
	 */
	public String getAction() {
		return Action;
	}

	/**
	 * @param action The action to set.
	 */
	public void setAction(String action) {
		Action = action;
	}

	/**
	 * @return Returns the doTransaction.
	 */
	public boolean isDoTransaction() {
		return doTransaction;
	}

	/**
	 * @param doTransaction The doTransaction to set.
	 */
	public void setDoTransaction(boolean doTransaction) {
		this.doTransaction = doTransaction;
	}

	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return Label;
	}

	/**
	 * @param label The label to set.
	 */
	public void setLabel(String label) {
		Label = label;
	}

	/**
	 * @return Returns the buttonDesc.
	 */
	public XMLRectangle getButtonDesc() {
		return buttonDesc;
	}

	/**
	 * @param buttonDesc The buttonDesc to set.
	 */
	public void setButtonDesc(XMLRectangle buttonDesc) {
		this.buttonDesc = buttonDesc;
	}

	public String getEndPattern() {
		return endPattern;
	}

	public void setEndPattern(String endPattern) {
		this.endPattern = endPattern;
	}

	public String getStartPattern() {
		return startPattern;
	}

	public void setStartPattern(String startPattern) {
		this.startPattern = startPattern;
	}
	
}
