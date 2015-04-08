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

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.ComplexExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.twinj.iJavelin;

/**
 * This class manages records located in a single page.
 * This extraction rule must be one of the last rule to be applied
 * because it removes blocks from the block factory.
 * There are two ways of defining a record (with the boolean perPage):
 *   - either between to separators (using extraction rule Separator)
 *     (perPage is false)
 *   - or we choose to define one record per page
 *     (all the blocks of a page will be put in a record) (perPage is true)
 * In normal mode, we test if the block is a bor block (Begining of Record Block)
 * ie: we verify its tagname corresponds to the specified one and if its type is separator
 * once we've found the bor block we scan the other blocks of the page
 * until we find a eor block (End of Record Block)
 * if both bor and eor are found we modify the bor block to make it the parent block of the record
 * and we set the blocks between the bor and eor blocks in the optionalChildren of the parent block
 * and remove them from the blockFactory
 * at the end, we test if the er blcok is followed by a bor block
 * or if its itself the next bor block and then decide to remove it or not from the blockFactory
 *
 * In perPage mode, we test if the block is the first of the page,
 * then we create the parent block of the record
 * we scan all blocks of the page, set them in the optionalChildren Vector of the parent block
 * and then remove them from the block factory.
 */
public class Record extends ComplexExtractionRule {
    
	private static final long serialVersionUID = -5487160772792595720L;

	// Begining of Record : tag name of the bloc defined as the begining of the record
    private String bor = "";
    
    // End of Record : tag name of the bloc defined as the begining of the record
    private String eor = "";
    
    // The tag name of the record
    private String tagName = "record";
    
    // Indicates if there is one record per page
    private boolean perPage = false;
    
	/**
	 * The regular expression object for bor / eor type.
	 */
	transient protected RE borRE, eorRE;

    /** Holds value of property borType. */
    private String borType = "separator";
    
    /** Holds value of property eorType. */
    private String eorType = "separator";
    
    /** Creates a new instance of Record */
    public Record() {
        super();
        setFinal(true);
    }
    
    /** Getter for property tagName.
     * @return Value of property tagName.
     */
    public String getTagName() {
        return tagName;
    }
    
    /** Setter for property tagName.
     * @param name New value of property tagName.
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
    
    /** Getter for property eor.
     * @return Value of property eor.
     */
    public String getEor() {
        return eor;
    }
    
    /** Setter for property eor.
     * @param eor New value of property eor.
     */
    public void setEor(String eor) {
        this.eor = eor;
    }
    
    /** Getter for property bor.
     * @return Value of property bor.
     */
    public String getBor() {
        return bor;
    }
    
    /** Setter for property bor.
     * @param bor New value of property bor.
     */
    public void setBor(String bor) {
        this.bor = bor;
    }
    
    // Returns true if block matches the bor characteristics
    private boolean isBor(Block block) {
        if (block == null) return false;
        boolean b1 = borRE.match(block.type);
        boolean b2 = block.tagName.equals(bor);
        Engine.logBeans.trace("borRE.match(" + block.type + "): " + b1 + " '" + borType + "'");
        Engine.logBeans.trace("block.tagName.equals(bor): " + b2);
        return (b1 && b2);
    }
    
    // Returns true if block matches the eor characteristics
    private boolean isEor(Block block) {
        if (block == null) return false;
		boolean b1 = eorRE.match(block.type);
		boolean b2 = block.tagName.equals(eor);
        Engine.logBeans.trace("eorRE.match(" + block.type + "): " + b1 + " '" + eorType + "'");
        Engine.logBeans.trace("block.tagName.equals(eor): " + b2);
		return (b1 && b2);
    }
    
    /** Getter for property perPage.
     * @return Value of property perPage.
     */
    public boolean isPerPage() {
        return perPage;
    }
    
    /** Setter for property perPage.
     * @param perPage New value of property perPage.
     */
    public void setPerPage(boolean perPage) {
        this.perPage = perPage;
    }
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        Block curBlock = block, eorBlock = block, temp;
        boolean found = false;
        
        // If the object has just been deserialized, we must
        // recreate the regular expression object.
        if (borRE == null) {
            try {
                setBorType(borType);
            }
            catch (RESyntaxException e) {
                Engine.logBeans.error("Unable to create the regular expression object for BOR type", e);
            }
        }
        if (eorRE == null) {
            try {
                setEorType(eorType);
            }
            catch (RESyntaxException e) {
                Engine.logBeans.error("Unable to create the regular expression object for EOR type", e);
            }
        }
        
        if (perPage) {
            Engine.logBeans.trace("Applying Record rule to the current page");
            
            // Test if it's the first block of the page
            if (blockFactory.getPreviousBlock(block) != null) {
                Engine.logBeans.trace("The block isn't the first block of the page, aborting ...");
                xrs.hasMatched = false;
                xrs.newCurrentBlock = block;
                return xrs;
            }
            
            // Creation and initialisation of the parent bloc of the record
            Block record = new Block();
            Engine.logBeans.trace("Creation du bloc parent");
            record.type = "record";
            record.setText("");
            record.name = "";
            if (this.tagName.length() != 0)
                record.tagName = this.tagName;
            else
                record.tagName = "record";
            
            // Insert the main bloc in the blockFactory in first position
            blockFactory.insertBlock(record, null);
            Engine.logBeans.trace("Insertion du bloc parent");
            
            // Adds each block to the record until the end of the page
            // After being added, each block is deleted
            while(curBlock != null){
                record.addOptionalChildren(curBlock);
                Engine.logBeans.trace("appending child " + curBlock.tagName + " to record " + record.tagName);
                temp = blockFactory.getNextBlock(curBlock);
                blockFactory.removeBlock(curBlock);
                curBlock = temp;
            }
            
            xrs.hasMatched = true;
            xrs.newCurrentBlock = record;
            return xrs;
        }
        else {
            Engine.logBeans.trace("Applying Record rule to block " + block.name);
            
            // Tests if the block is in the specified zone and if it's a bor block
            if (isBor(block)) {
                // When a bor block is found
                // we look for an eor block
                while ((curBlock = blockFactory.getNextBlock(curBlock)) != null) {
                    if (!canBlockBeSelected(curBlock))
                        continue;
                    if (isEor(curBlock)) {
                        eorBlock = curBlock;
                        found = true;
                        break;
                    }
                }
                
                // If the end of the screen has been reached, set the 'found' flag to true.
                if (curBlock == null) {
                	found = true;
                }
            }
            
            // If both bor and eor has been found, we proceed to the construction
            // of the main block of the record
            if (found) {
                Engine.logBeans.trace("Begining and End of Record " + this.tagName + " found");
                
                Block recordBlock;
				recordBlock = new Block();
                Engine.logBeans.trace("Creation du bloc BOR");
				recordBlock.type = "record";
				recordBlock.setText("");
				recordBlock.name = "";
				recordBlock.line = block.line;
				recordBlock.column = block.column;
                
                if (this.tagName.length() != 0)
					recordBlock.tagName = this.tagName;

                // Insert the main block in the blockFactory in first position
                blockFactory.insertBlock(recordBlock, blockFactory.getPreviousBlock(block));
                Engine.logBeans.trace("BOR block inserted ...");
                
                // Starts the block just after the bor
                curBlock = blockFactory.getNextBlock(recordBlock);
				Block nextBlock = null;
                if (eorBlock != null) {
					nextBlock = blockFactory.getNextBlock(eorBlock);
                }
                
                // Each block between the bor and the eor is added to the record
                // and deleted from memory
                while (curBlock != null){
                    if (isEor(curBlock)) {
                    	if (bor.equals(eor)) {
                    		if (recordBlock.hasOptionalChildren()) {
                    			break;
                    		}
                    	}
                    	else {
							break;
                    	}
                    }
                    
					recordBlock.addOptionalChildren(curBlock);
					Engine.logBeans.trace("Appending child " + curBlock.tagName + " to record " + recordBlock.tagName);
                    
					temp = blockFactory.getNextBlock(curBlock);
					blockFactory.removeBlock(curBlock);
                    curBlock = temp;
                }
                
                // Deletes the eor separator only if it's followed  by a bor separator
                // and sets the new current block
				xrs.newCurrentBlock = recordBlock;
                if (isBor(nextBlock) || (nextBlock == null)){
                    if (eorBlock != null) {
						blockFactory.removeBlock(eorBlock);
                    }
                }
                xrs.hasMatched = true;
                return xrs;
            }
        }
        
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        return xrs;
        
    }
    
    /** Getter for property borType.
     * @return Value of property borType.
     */
    public String getBorType() {
        return this.borType;
    }
    
    /** Setter for property borType.
     * @param borType New value of property borType.
     */
    public void setBorType(String borType) throws RESyntaxException {
        borRE = new RE(borType);
        this.borType = borType;
    }
    
    /** Getter for property eorType.
     * @return Value of property eorType.
     */
    public String getEorType() {
        return this.eorType;
    }
    
    /** Setter for property eorType.
     * @param eorType New value of property eorType.
     */
    public void setEorType(String eorType) throws RESyntaxException {
        eorRE = new RE(eorType);
        this.eorType = eorType;
    }
}
