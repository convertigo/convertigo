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
public class Slider extends JavelinExtractionRule {
    
	private static final long serialVersionUID = -1471497615362991697L;

	/* Properties */
    private String startPattern 		= "A";
    private String endPattern 			= "V";
    private String screenShowedPattern 	= "*";
    private String screenHiddenPattern 	= ":";
    
    
    public Slider() {
        super();
    }

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.ExtractionRule#init(int)
	 */
	public void init(int reason) {
		// TODO Auto-generated method stub
		super.init(reason);
	}
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        
        Block startBlock 	= null;
        Block blockTmp 		= null;
        Collection<Block> blocksToDelete = new LinkedList<Block>();
        boolean bEndBlockFound = false;
        int ratio = 0;
        int beginLine = -1;
        
    	// search of the start block
    	if (block.getText().trim().indexOf(startPattern) == 0
    		&& block.length == 1) {
    		
    		startBlock = block;
    		blocksToDelete.add(startBlock);
			
    		blockTmp = block;
    		// search of endPattern
    		while ( blockFactory.getSouthBlock(blockTmp) != null
    				&& canBlockBeSelected(blockFactory.getSouthBlock(blockTmp))
    				&& blockFactory.getSouthBlock(blockTmp).length == 1
    				&& ! bEndBlockFound ) {
    			
    			blockTmp = blockFactory.getSouthBlock(blockTmp);
    			// check if the block correspond to screenHiddenPattern or screenShowedPattern
    			if (blockTmp.getText().trim().indexOf(screenShowedPattern) == 0) {
    				ratio ++;
    				if (beginLine == -1)
    					beginLine = blockTmp.line;
    				blocksToDelete.add(blockTmp);
    			} else if (blockTmp.getText().trim().indexOf(screenHiddenPattern) == 0) {
    				blocksToDelete.add(blockTmp);
    			} else if (blockTmp.getText().trim().indexOf(endPattern) == 0) {
    				blocksToDelete.add(blockTmp);
    				bEndBlockFound = true;
    			} else {
    				xrs.hasMatched = false;
    				xrs.newCurrentBlock = block;
    				return xrs;
    			}
    		}
    		if (!bEndBlockFound) {
    			xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
    		}
    	}
		else {
			xrs.hasMatched = false;
			xrs.newCurrentBlock = block;
			return xrs;
		}
        
	    Block mySlider = new Block();
		mySlider.name = "untitled";
		mySlider.type = "slider";
		mySlider.setText("");
		mySlider.bRender = true;
		mySlider.line = startBlock.line;
		mySlider.column =  startBlock.column;
		mySlider.setOptionalAttribute("width", "1");
		mySlider.setOptionalAttribute("height", "" + (blockTmp.line - startBlock.line + 1));
		mySlider.setOptionalAttribute("ratio", "" + ratio);
		mySlider.setOptionalAttribute("begin", "" + beginLine);
		
		blockFactory.insertBlock(mySlider, blockFactory.getPreviousBlock(block));
		blockFactory.removeBlocks(blocksToDelete);
		
		xrs.newCurrentBlock = mySlider;
		xrs.hasMatched = true;
		return xrs;
    }
  
    
	public String getEndPattern() {
		return endPattern;
	}

	public void setEndPattern(String endPattern) {
		this.endPattern = endPattern;
	}

	public String getScreenHiddenPattern() {
		return screenHiddenPattern;
	}

	public void setScreenHiddenPattern(String screenHiddenPattern) {
		this.screenHiddenPattern = screenHiddenPattern;
	}

	public String getScreenShowedPattern() {
		return screenShowedPattern;
	}

	public void setScreenShowedPattern(String screenShowedPattern) {
		this.screenShowedPattern = screenShowedPattern;
	}

	public String getStartPattern() {
		return startPattern;
	}

	public void setStartPattern(String startPattern) {
		this.startPattern = startPattern;
	}

}
