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

package com.twinsoft.convertigo.beans.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.twinj.iJavelin;

/**
 * This is the interface that a block factory must satisfy.
 *
 * <p>Each map is first cut into entities called blocks. Then, we
 * try to apply extraction rules for a given screen class on these
 * blocks; so several blocks can be aggregated into one, in function
 * of rules application.</p>
 */
public abstract class BlockFactory extends DatabaseObject {

	private static final long serialVersionUID = -2864266703358537062L;

	public static final String DATA_DIRECTORY = "bf";
    
    public BlockFactory() {
        super();
        databaseType = "BlockFactory";
    }
    
    public String getPath() {
        return parent.getPath() + "/" + DATA_DIRECTORY;
    }
    
    /**
     * Performs all necessary clean up.
     */
    public abstract void destroy();
    
    /**
     * Compares the attributes of two different blocks.
     *
     * @param first the first block.
     * @param second the second block.
     *
     * @return <code>true</code> if paper or ink are equals taking AT_INVERT in account.
     */
    public boolean isSamePaper(Block first, Block second) {
        if ((first.attribute & iJavelin.AT_INVERT) == (second.attribute & iJavelin.AT_INVERT)) {
            if ((first.attribute & iJavelin.AT_INVERT) == iJavelin.AT_INVERT)
                return ((first.attribute & iJavelin.AT_INK) == (second.attribute & iJavelin.AT_INK));
            
            return ((first.attribute & iJavelin.AT_PAPER) == (second.attribute & iJavelin.AT_PAPER));
        }
        
        return false;
    }
    
    /**
     * Compares the line numbers of two different blocks.
     *
     * @param first the first block.
     * @param second the second block.
     *
     * @return <code>true</code> if lines are same.
     */
    public boolean isSameLine(Block first, Block second) {
        return (first.line == second.line);
    }
    
    
    /**
     * Merges two blocks into the first one.
     *
     * @param first the first block to merge.
     * @param second the second block to merge.
     *
     * @return the new number of blocks in the blocks list.
     */
    public abstract int mergeBlocks(Block first, Block second);
    
    /**
     * Merges three blocks into the first one.
     *
     * @param first the first block to merge.
     * @param second the second block to merge.
     * @param third the third block to merge.
     *
     * @return the new number of blocks in the blocks list.
     */
    public abstract int mergeBlocks(Block first, Block second, Block third);
    
    /**
     * Names a block using the block informations.
     *
     * @param block the block to be named.
     */
    public void setBlockName(Block block) {
        if (block.type.equalsIgnoreCase("field")) {
            block.name = Parameter.JavelinField.getName()+"c" + block.column + "_l" + block.line;
        }
        else {
            block.name = Parameter.JavelinBlock.getName()+"c" + block.column + "_l" + block.line;
        }
    }
    
    transient public Map<String, Block> fields;
    
    /**
     * Makes all blocks from a Javelin map.
     *
     * These blocks should be rearranged by application of extraction
     * rules.
     *
     * @param javelin the Javelin object.
     */
    public abstract void make(iJavelin javelin);
    
    /**
     * Gets the index of block in vector.
     *
     * @param block the block to find.
     *
     * @return the index or -1 if not found.
     */
    public abstract int getIndexOf(Block block);
    
    /**
     * Inserts the block after the specified one.
     *
     * @param block the block to be inserted.
     *
     * @param after the reference block.
     *
     * @return the insertion index.
     */
    public abstract int insertBlock(Block block, Block after);
    
    /**
     * Moves to the first block of the list.
     */
    public abstract void moveToFirstBlock();
    
    /**
     * Retrieves the previous block in the block list.
     *
     * @param block the reference block (ie the current block).
     *
     * @return the new number of blocks in the blocks list.
     */
    public abstract Block getPreviousBlock(Block block);
    
    /**
     * Retrieves the next block in the block list.
     *
     * If this method is called for the first time, it returns the
     * first block.
     *
     * @return the next block in the block list or <code>null</code>
     * if there is no more block.
     */
    public abstract Block getNextBlock();
    
    /**
     * Retrieves the next block in the block list following the block provided.
     *
     * @param block the reference block.
     *
     * @return the next block in the block list or <code>null</code>
     * if there is no more block.
     */
    public abstract Block getNextBlock(Block block);
    
    /**
     * Retrieves a given block by its name.
     *
     * @param block the block.
     *
     * @return the given block if found, <code>null</code> otherwise.
     */
    public abstract Block getBlockByName(String blockName);
    
    /**
     * Retrieves a given block by its type.
     *
     * @param the block type.
     *
     * @return the given block if found, <code>null</code> otherwise.
     */
    public abstract Block getBlockByType(String blockType);
    
	/**
	 * Retrieves a vector of blocks by their type.
	 *
	 * @param the block type.
	 *
	 * @return a vector containing the blocks if any found.
	 */
	public abstract List<Block> getBlocksByType(String blockType);
    
	/**
     * Removes a list of blocks
     * 
     * @param blocksToDelete the list of blocks to be removed.
     */
	public abstract void removeBlocks(Collection<Block> blocksToDelete);
	
    /**
     * Removes a given block.
     *
     * @param block the block to be removed.
     */
    public abstract void removeBlock(Block block);
    
    /**
     * Retrieves all the blocks.
     *
     * @return all the blocks.
     */
    public abstract Collection<Block> getAllBlocks();
    
	/**
	 * Retrieves the last block.
	 *
	 * @return the last block in the blockFactory.
	 */
	public abstract Block getLastBlock();
	
	/**
	 * Retrieves the first block.
	 *
	 * @return the first block in the blockFactory.
	 */	   
	public abstract Block getFirstBlock();
    
    /**
	 * Retrieves block above(north) of the one provided
	 * @author edigiaro
	 * 
	 * @param block the block.
     *
     * @return the next block above if found, <code>null</code> otherwise.
     */
    public abstract Block getNorthBlock (Block block);
    
    
    
	/**
	 * Retrieves block under(south) of the one provided
	 * @author edigiaro
	 * 
	 * @param block the block.
     *
     * @return the next block under if found, <code>null</code> otherwise.
     */
	
	public abstract Block getSouthBlock (Block block);
	
	
	/**
	 * Retrieves block on the right(east) of the one provided
	 * @author edigiaro
	 * 
	 * @param block the block.
     *
     * @return the next block on the right if found, <code>null</code> otherwise.
     */
		public abstract Block getEastBlock (Block block);
	
	
	/**
	 * Retrieves block  on the left(west) on the one provided
	 * @author edigiaro
	 * 
	 * @param block the block.
     *
     * @return the next block on the left if found, <code>null</code> otherwise.
     */
		public abstract Block getWestBlock (Block block);
		

	/**
	 * Check the column numbers of two different blocks.
	 *
	 * @param first the first block.
	 * @param second the second block.
	 *
	 * @return <code>true</code> if they have a common column.
	 */
		public abstract boolean isInSameColumn(Block block1, Block block2);
}
