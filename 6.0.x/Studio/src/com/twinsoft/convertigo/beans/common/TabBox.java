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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.twinj.iJavelin;

/**
 * 
 */
public class TabBox extends JavelinExtractionRule {
	
	private static final long serialVersionUID = 7729475266188640099L;

	/* Properties */
	private boolean	extendedMode = false;

	/* Working variables */
    transient boolean[] attributes;
    
    
	public TabBox() {
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
		attributes = new boolean[] { true, true, true, true, true, true, true, true, true, true, true };
	}

	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		int activeTab = 0;
		Block blockTmp, dataBlock = null;
		Collection<Block> tabBlocks = new LinkedList<Block>();
		org.w3c.dom.Element tabBox;
        org.w3c.dom.Element item;
        
		xrs.hasMatched = false;
        
		if (!extendedMode) {
	        if (!block.type.equals("keyword") || block.getOptionalAttribute("penselectable") == null) {
	        	// block not matching first button
	        	xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
	        }
        	// seams first button found
        	blockTmp = block;
        	tabBlocks.add(blockTmp);
    		if (blockTmp.getOptionalAttribute("penselectable").equals("false"))
        		activeTab++;
    		// look for other buttons
    		while ( blockFactory.getEastBlock(blockTmp) != null 
        			&& canBlockBeSelected(blockFactory.getEastBlock(blockTmp)) 
        			&& blockFactory.getEastBlock(blockTmp).type.equals("keyword") 
        			&& blockFactory.getEastBlock(blockTmp).getOptionalAttribute("penselectable") != null
        			&& activeTab <= 1) {
        		tabBlocks.add(blockFactory.getEastBlock(blockTmp));
        		if (blockFactory.getEastBlock(blockTmp).getOptionalAttribute("penselectable").equals("false"))
            		activeTab++;
        		blockTmp = blockFactory.getEastBlock(blockTmp);
        	} 
        	if (activeTab != 1) {
        		// more than one button not penselectable => not a tabBox
        		xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
        	}
        	
        	// creates the tab box element
        	tabBox = dom.createElement("tabBox");
        	// creates the tab box items
        	for(Block itemBlock : tabBlocks){
        		// create item element
	        	item = dom.createElement("block");
	        	item.appendChild(dom.createTextNode(itemBlock.getText()));
	        	item.setAttribute("line", "" + itemBlock.line);
				item.setAttribute("column", "" + itemBlock.column);
				item.setAttribute("width", "" + itemBlock.getOptionalAttribute("width"));
				item.setAttribute("height", "" + itemBlock.getOptionalAttribute("height"));
				item.setAttribute("type", "tabBoxItem");
				item.setAttribute("selected", itemBlock.getOptionalAttribute("penselectable").equals("false")?"true":"false");
				// add the item to the tabBox element
				tabBox.appendChild(item);
	        }
		} else {
			// extended mode : we look for buttonsPanel type block
			if (!block.type.equals("buttonsPanel")) {
				// block not matching buttons panel
	        	xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
			}
			// buttonsPanel found
			// change tagname
			block.tagName = "tabBox";
			// save tabBox
			tabBox = block.toXML(dom, attributes);
			
			// and remove all attributes to match tabBoxes'set
			tabBox.removeAttribute("background");
			tabBox.removeAttribute("choicesCols");
			tabBox.removeAttribute("choicesRows");
			tabBox.removeAttribute("choicesTextSize");
			tabBox.removeAttribute("column");
			tabBox.removeAttribute("foreground");
			tabBox.removeAttribute("line");
			tabBox.removeAttribute("name");
			tabBox.removeAttribute("reverse");
			tabBox.removeAttribute("type");
			
			// modify every child node to match tabBoxes'items
			NodeList items = tabBox.getChildNodes();
			for (int i = 0 ; i < items.getLength() ; i++) {
				Node node = items.item(i);
				if (node instanceof Element) {
					Element elem = (Element) node;
					elem.setAttribute("type", "tabBoxItem");
					elem.removeAttribute("action");
					if (elem.getAttribute("available").equals("true"))
						elem.setAttribute("selected", "false");
					else
						elem.setAttribute("selected", "true");
					elem.removeAttribute("available");
					elem.removeAttribute("background");
					elem.removeAttribute("dotransaction");
					elem.removeAttribute("foreground");
					elem.removeAttribute("name");
					elem.removeAttribute("reverse");
				}
			}
			
			// set dataBlock to the first next block
			blockTmp = blockFactory.getNextBlock(block);
			
			// put the block in the vector to be removed
	    	tabBlocks.add(block);
		}
    	
		// look for the first data block : the first selectable block of the next line
    	while (blockFactory.getNextBlock(blockTmp) != null 
    			&& dataBlock == null ) {
    		if (canBlockBeSelected(blockFactory.getNextBlock(blockTmp))
    			&& blockFactory.getNextBlock(blockTmp).line > blockTmp.line) {
    			dataBlock = blockFactory.getNextBlock(blockTmp);
    		} else {
    			blockTmp = blockFactory.getNextBlock(blockTmp);
    		}
    	}
    	
		// create the tab box block 
    	Block tabBoxBlock = new Block();
    	tabBoxBlock.type = "tabBox";
    	tabBoxBlock.setText("");
    	tabBoxBlock.bRender = true;
    	tabBoxBlock.column = block.column;
    	tabBoxBlock.line = block.line;
    	tabBoxBlock.name = "untitled";
    	tabBoxBlock.setOptionalAttribute("width", "" + getSelectionScreenZone().width);
    	tabBoxBlock.setOptionalAttribute("height", "" + getSelectionScreenZone().height);
    	
    	// attach the tabBox element
    	tabBoxBlock.addOptionalChildren(tabBox);
    	
    	// add the block to the block factory
    	blockFactory.insertBlock(tabBoxBlock, block);
    	
   		// remove other blocks from the block factory
       	blockFactory.removeBlocks(tabBlocks);
    	
    	// now attach all data blocks to the tabBoxBlock
    	// and remove them from the blockFactory
    	Block dataNextBlock = dataBlock;
    	do {
    		if (canBlockBeSelected(dataBlock)) {
    			tabBoxBlock.addOptionalChildren(dataBlock);
    			dataNextBlock = blockFactory.getNextBlock(dataBlock);
    			blockFactory.removeBlock(dataBlock);
    			dataBlock = dataNextBlock;
    		} else {
    			dataBlock = blockFactory.getNextBlock(dataBlock);
    		}
    	} while (dataBlock != null);
		
    	xrs.hasMatched = true;
    	xrs.newCurrentBlock = block;
        return xrs;
	}

	public boolean isExtendedMode() {
		return extendedMode;
	}

	public void setExtendedMode(boolean extendedMode) {
		this.extendedMode = extendedMode;
	}

}
