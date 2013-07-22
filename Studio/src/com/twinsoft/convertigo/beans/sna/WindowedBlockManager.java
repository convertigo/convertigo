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

package com.twinsoft.convertigo.beans.sna;

import java.util.List;

import com.twinsoft.convertigo.beans.common.XMLRectangle;
import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.engine.Engine;

/**
 * This class handles a block factory managing blocks in a vector.
 * The WindowedBlockManager manage blocks only in a Panel if it exists.   
 */
public class WindowedBlockManager {
	
	/**
	 * The BlockFactory emuled by the WinodowedBlockManager
	 */
	private transient BlockFactory 	theBlockFactory;
	
	/**
	 * The Panel containing the subfile
	 */
	private transient Block			panel 			= null;
	
	/**
	 * Constructs and initialises the WindowedBlockManager. 
	 * Searches for a panel containing the end block of the sub file. 
	 *  
	 * @param blockFactory The initial BlockFactory.
	 */
	public WindowedBlockManager(BlockFactory blockFactory) {
		theBlockFactory = blockFactory;
	}
	
	/**
	 * Looks for the panel containing the endBlock (Suite..., Fin, +) in the BlockFactory 
	 * and initializes the range of the WindowedBlockFactory with the range of the Panel.
	 *  
	 * @param endBlock The end block (Suite..., Fin, +) of the subfile.
	 * @return boolean indicating if we have to conntine the process of the rule SubFile
	 */
	public boolean searchPanel(Block endBlock){
		Block currentBlock = null;
		while ( (currentBlock = theBlockFactory.getNextBlock(currentBlock)) != null)
			if ("panel".equals(currentBlock.type) ) {
				// We've found a panel. Trying to find the endBlock in this panel.
				panel = currentBlock;
				Engine.logBeans.debug("Found a panel : c=" + panel.column + ", l=" +panel.line+ ", w=" + (panel.getOptionalAttribute("width")) + ", h=" + (panel.getOptionalAttribute("height")));
				
				for(Block block : panel.getOptionalBlockChildren())
					if (block == endBlock) { // The object pointing by "childBlock" and "endBlock" must be the same
						// We've found the endBlock in this panel. 
						Engine.logBeans.debug("The endBlock was found in this panel. Stopping the search.");
						return true;
					}
				
				Engine.logBeans.debug("The endBlock was not found in this panel. Stopping the process of the subfile rule.");
				return false;
			}
		Engine.logBeans.debug("The endBlock was not found in a panel.");
		return true;
	}
	
	private boolean isBlockInPanel(Block block) {
		XMLRectangle panelRect = new XMLRectangle(panel.column, panel.line, Integer.parseInt(panel.getOptionalAttribute("width")), Integer.parseInt(panel.getOptionalAttribute("height")));
		if (panelRect.contains(block.column, block.line, block.column + block.length, 1))
			return true;
		return false;
	}

	public int insertBlock(Block block, Block after) {
		return theBlockFactory.insertBlock(block, after);
	}

	public Block getPreviousBlock(Block block) {
		return theBlockFactory.getPreviousBlock(block);
	}

	public Block getPreviousBlockInPanel(Block block) {
		
		Block previousBlock = theBlockFactory.getPreviousBlock(block);
		if (panel != null)
			while (previousBlock != null) {
				if(isBlockInPanel(previousBlock))
					break;
				previousBlock = theBlockFactory.getPreviousBlock(previousBlock);
			}
		return previousBlock;
	}
	
	public Block getNextBlock(Block block) {
		return theBlockFactory.getNextBlock(block);
	}

	public List<Block> getBlocksByType(String blockType) {
		return theBlockFactory.getBlocksByType(blockType);
	}

	public void removeBlock(Block block) {
		theBlockFactory.removeBlock(block);
	}

	public Block getLastBlock() {
		return theBlockFactory.getLastBlock();
	}

	/**
	 * Retrieves ,in the panel, the block above(north) the one provided.
	 * 
	 * @param block The current block.
     * @return The next block above if found, <code>null</code> otherwise.
	 */
	public Block getNorthBlock(Block block) {
		Block resBlock = theBlockFactory.getNorthBlock(block);
		if (resBlock!=null)
			if (panel == null)
				return resBlock;
			else if (resBlock.line <= panel.line) {
				// The north block is out of the panel... 
				return null;
			}
		return resBlock;
	}

	/**
	 * Retrieves ,in the panel, the block under(south) the one provided.
	 * 
	 * @param block The current block.
	 * @return The next block under if found, <code>null</code> otherwise.
	 */
	public Block getSouthBlock(Block block) {
		Block resBlock = theBlockFactory.getSouthBlock(block);
		if (resBlock!=null)
			if (panel == null)
				return resBlock;
			else if (resBlock.line >= panel.line + Integer.parseInt(panel.getOptionalAttribute("height")) -1) {
				// The south block is out of the panel... 
				return null;
			}
		return resBlock;
	}

	/**
	 * Retrieves ,in the panel, the block on the left(west) of the one provided.
	 * 
	 * @param block The current block.
	 * @return The next block on the left if found, <code>null</code> otherwise.
	 */
	public Block getWestBlock(Block block) {
		Block resBlock = theBlockFactory.getWestBlock(block);
		if (resBlock!=null)
			if (panel == null)
				return resBlock;
			else if (resBlock.column <= panel.column) {
				// The east block is out of the panel... 
				return null;
			}
		return resBlock;
	}

	/**
	 * Retrieves ,in the panel, the block on the right(east) of the one provided.
	 * 
	 * @param block The current block.
	 * @return The next block on the right if found, <code>null</code> otherwise.
	 */
	public Block getEastBlock(Block block) {
		Block resBlock = theBlockFactory.getEastBlock(block);
		if (resBlock!=null)
			if (panel == null)
				return resBlock;
			else if (resBlock.column >= panel.column + Integer.parseInt(panel.getOptionalAttribute("width")) -1)
				// The west block is out of the panel... 
				return null;
		return resBlock;
	}

	/**
	 * @return The panel block containing the sub file if exits, <code>null</code> otherwise. 
	 */
	public Block getPanel() {
		return panel;
	}
}