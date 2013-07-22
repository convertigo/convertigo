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

import java.util.HashMap;
import java.util.LinkedList;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.engine.EngineException;

public class PanelBlockFactory extends DefaultBlockFactory {

	private static final long serialVersionUID = 1L;

	private Block panel;
	
	public PanelBlockFactory() {
		super();
	}
	
	public PanelBlockFactory(Block panelBlock) {
		super();
		make(panelBlock);
	}

	/**
	 * Makes all blockFactory's blocks from a panel type Block.
	 * These blocks should be rearranged by application of extraction rules.
	 *
	 * @param panelBlock the block from which extract the blockFactory's blocks (only a panel, tabBox or container type block).
	 */
	public void make(Block panelBlock) {
    	setPanel(panelBlock);
		
		nbColumns = Integer.parseInt(panelBlock.getOptionalAttribute("width"));
    	nbLines = Integer.parseInt(panelBlock.getOptionalAttribute("height"));
    	
    	list = new LinkedList<Block>();
    	fields = new HashMap<String, Block>();
        
    	nbItems = panelBlock.getNumberOfOptionalChildren();

    	Block lastBlock = null;
    	for(Block block : panelBlock.getOptionalBlockChildren()){
	    	insertBlock(block, lastBlock);
    		if (block.type.equals("field"))
    			fields.put(block.name, block);
			lastBlock = block;
    	}
    }
    
	/**
	 * Creates a new PanelBlockFactory object, clone of a DefaultBlockFactory object.
	 * 
	 * @param blockFactory the BlockFactory to clone
	 * @return the new PanelBlockFactory
	 * @throws CloneNotSupportedException
	 * @throws EngineException
	 */
	public static PanelBlockFactory cloneBlockFactory(DefaultBlockFactory blockFactory) throws CloneNotSupportedException, EngineException {
    	PanelBlockFactory clone = new PanelBlockFactory();
    	
    	clone.bNew = blockFactory.bNew;
    	clone.fields = blockFactory.fields;
    	clone.blockCount = blockFactory.blockCount;
    	clone.blockIndex = blockFactory.blockIndex;
    	clone.javelin = blockFactory.javelin;
    	clone.setName(blockFactory.getName());
    	clone.nbColumns = blockFactory.nbColumns;
    	clone.nbItems = blockFactory.nbItems;
    	clone.nbLines = blockFactory.nbLines;
    	clone.setParent(blockFactory.getParent());
    	clone.list = blockFactory.list;
    	
    	return clone;
    }

	public Block getPanel() {
		return panel;
	}

	public void setPanel(Block panel) {
		this.panel = panel;
	}
}
