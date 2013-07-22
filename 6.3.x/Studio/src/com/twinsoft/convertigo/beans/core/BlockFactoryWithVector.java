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
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class handles a block factory managing blocks in a vector.
 */
public abstract class BlockFactoryWithVector extends BlockFactory {
    
	private static final long serialVersionUID = -2732486387311463189L;

	transient public List<Block> list = null;
    transient public int blockIndex = -1;
    transient public int blockCount = 0;
    
    @Override
    public void destroy() {
        // Nothing special to do
    }
    
    @Override
    public int getIndexOf(Block block) {
    	return list.indexOf(block);
    }
    
    @Override
    public int insertBlock(Block block, Block after) {
        int index;
        
        if (after == null) {
            list.add(0, block);
            return 0;
        }
        
        if ((index = getIndexOf(after)) != -1) {
            list.add(index+1, block);
            return index;
        }
        else {
            list.add(block);
            return list.size();
        }
    }
    
    @Override
    public int mergeBlocks(Block first, Block second) {
        first.setText(first.getText() + second.getText());
        list.remove(second);
        return list.size();
    }
    
    @Override
    public int mergeBlocks(Block first, Block second, Block third) {
        first.setText(first.getText() + second.getText() + third.getText());
        list.remove(second);
        list.remove(third);
        return list.size();
    }
    
    @Override
    public void moveToFirstBlock() {
        blockIndex = (list != null) && (list.size() > 0) ? 0:-1;
    }
    
    @Override
    public Block getPreviousBlock(Block block) {
        int index;
        
        if ((block == null)
        || (list == null)
        || ((index = getIndexOf(block)) < 1))
            return null;
        
        return list.get(index-1);
    }
    
    @Override
    public Block getNextBlock(Block block) {
        int index;
        
        if (list != null) {
            if (list.size() == 0)
                return null;
            
            if (block == null)
                return list.get(0);
            
            if (((index = getIndexOf(block)) == -1) || (index == list.size()-1))
                return null;
            
            return list.get(index+1);
        }
        
        return null;
    }
    
    @Override
    public Block getNextBlock() {
        if ((list != null) && (list.size() != 0) && (blockIndex < list.size()))
            return list.get(blockIndex++);
        
        return null;
    }
    
    @Override
    public Block getBlockByName(String blockName) {
        for (Block block : list) {
            if (block.name.equals(blockName)) {
                return block;
            }
        }
        return null;
    }
    
    @Override
    public Block getBlockByType(String blockType) {
        for (Block block : list) {
            if (block.type.equalsIgnoreCase(blockType)) {
                return block;
            }
        }
        return null;
    }
    
    @Override
	public List<Block> getBlocksByType(String blockType) {
		List<Block> result = new LinkedList<Block>();
        for (Block block : list) {
		   if (block.type.equalsIgnoreCase(blockType))
			    result.add(block);
		}
		return result;
	}
	   
	@Override
	public void removeBlocks(Collection<Block> blocksToDelete) {
		list.removeAll(blocksToDelete);
	}
	
	@Override
    public void removeBlock(Block block) {
        list.remove(block);
    }
    
    /** Retrieves all the blocks
     */
	@Override
    public Collection<Block> getAllBlocks() {
        return list;
    }
    
	@Override
	public Block getLastBlock() {
		return list.get(list.size()-1);
	}
    
	@Override
	public Block getFirstBlock() {
		if(list.size()==0) throw new NoSuchElementException(); // #611 : simulate vector.firstElement(), like r19919
		return list.get(0);
	}
	
    /** Added by edigiaro 09/16/2003
     * 
     */
	@Override
	public Block getNorthBlock (Block block){
		int index;
		Block candidate;
		Block theOne = getFirstBlock();
		boolean exist = false;
		
		if (block != null){
			while (theOne != null
				&& theOne.line >= block.line) {
				theOne = getNextBlock(theOne);
			}
			if (theOne != null) {
				if (list != null) {
					if (list.size() == 0)
						return null;
	            
					if (((index = getIndexOf(block)) == -1) || (index == list.size()-1))
						return null;
	            
					//the block we are looking for is before in the block list
					for (int i=getIndexOf(block); i>0; i--){
						candidate = list.get(i-1);
						//we search a block that is in the same column and as close as possible of the block (just above)
						if (isInSameColumn(block, candidate)){						
							if (candidate.line >= theOne.line && candidate.line < block.line) {
								theOne = candidate;
								exist = true;
							}
						}
					}
					if (exist) return (Block)theOne;
				}
		     }
		}
		return null;
	}
	
	/** Added by edigiaro 09/16/2003
	 * 
	 */
	@Override
	public Block getSouthBlock (Block block){
		int index;
		Block candidate;
		Block theOne = getLastBlock();
		boolean exist = false;
		
		if (block != null){
			if (list != null) {
				if (list.size() == 0)
					return null;
            
				if (((index = getIndexOf(block)) == -1) || (index == list.size()-1))
					return null;
            
				//the block we are looking for is after in the block list
				int i=getIndexOf(block)+1;
				while (i<list.size() && exist == false){
					candidate = list.get(i);
					
					//we search a block that is in the same column and as close as possible of the block (just under)
					if (isInSameColumn(block, candidate)) {
						if (candidate.line <= theOne.line && candidate.line != block.line) {
							theOne = candidate;
							exist = true;
						}
					}
					i++;
				}
				if (!exist) return null;
				else return (Block)theOne;
			 }
		}
		return null;
	}
	
	/** Added by edigiaro 09/16/2003
	 * 
	 */
	@Override
	public Block getEastBlock (Block block){
		int index;
		Block candidate;
		
		if (block != null){
			if (list != null) {
				if (list.size() == 0)
					return null;     
				if (((index = getIndexOf(block)) == -1) || (index == list.size()-1))
					return null;
				candidate = (Block) list.get(index+1);
				if ((candidate.line == block.line) && (candidate.column>block.column))
				return candidate;
			 }
		}
		return null;
	}
	
	/** Added by edigiaro 09/16/2003
	 * 
	 */
	@Override
	public Block getWestBlock (Block block){
		int index;
		Block candidate;
		
		if (block != null){
			if (list != null) {
				if (list.size() == 0)
					return null;
				if (((index = getIndexOf(block)) == 0) || (index == list.size()-1))
					return null;
				candidate = (Block) list.get(index-1);
				if ((candidate.line == block.line) && (candidate.column<block.column))
				return candidate;
			 }
		}
		return null;
	}
	
	/** Added by edigiaro 09/06/2003
	 * 
	 * @author edigiaro
	 *
	 */
	@Override
	public boolean isInSameColumn(Block block1, Block block2){
		//two blocks are in the same column if they have at least 1 jointly column 
		int min1, min2, max1, max2;
	
		if (block1 == null || block2 == null) return false;
		
		min1 = block1.column;
		max1 = block1.column + block1.getText().length();
		min2 = block2.column;
		max2 = block2.column + block2.getText().length();
		
		if (min1==min2) return true;
		if ((min1 >= min2) && (max1>=max2) && (max2>=min1)) return true;
		if ((min1 >= min2) && (max1<=max2) && (max1>=min2)) return true;
		if ((min1 <= min2) && (max1>=max2)) return true;
		if ((min1 >= min2) && (max1<=max2)) return true;
		
		return false;
	}
}

