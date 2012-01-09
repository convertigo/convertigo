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

package com.twinsoft.convertigo.beans.vdx;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.twinj.iJavelin;

/**
 * This class manages editable fields.
 */
public class EditField extends JavelinExtractionRule {
	
    public static final long serialVersionUID = -6735666205939933877L;
    
    transient private int numberOfLines;
    transient private JavelinExtractionRuleResult xrs;
    
    public EditField() {
        super();
        setFinal(true);
    }
    
    public boolean isCursorInField(Block block, int Line, int Column) {
        int len = block.length;
        
        if ((block.line != Line) || ((Column < block.column) || (Column >= block.column+len)))
            return false;
        
        return true;
    }
    
    public void trimEndingDotsAndSpaces(Block block) {
        block.setOptionalAttribute("size", (new Integer(block.length)).toString());
        
        String blockText = block.getText();
        int index = blockText.indexOf('.');
        
        block.setOptionalAttribute("nblines", (new Integer(numberOfLines)).toString());
        
        if (index == 0) {
            block.setText("");
            return;
        }
        
        if (index != -1)	// there are dots
            block.setText(blockText.substring(0, index));
        
        block.setText(block.getText().trim());
    }
    
    public void groupDotedLines(Block curBlock, BlockFactory blockFactory) {
        char  c;
        Block nxtBlock;
        
        numberOfLines = 1;
        
        while((c = curBlock.getText().charAt(0)) == '.') {
            nxtBlock = blockFactory.getNextBlock(curBlock);
            
            if (nxtBlock.getText().charAt(0) != c)
                break;
            
            if (curBlock.line != nxtBlock.line)
                numberOfLines++;
            
            blockFactory.mergeBlocks(curBlock, nxtBlock);
            xrs.newCurrentBlock = curBlock;
        }
        
        if (numberOfLines != 1)
            curBlock.setOptionalAttribute("nblines", (new Integer(numberOfLines)).toString());
    }
    
    public boolean isEmptySpace(Block block) {
        String str = block.getText();
        int	len = str.length();
        
        for(int i=0; i<len; i++)
            if (str.charAt(i) != ' ')
                return false;
        
        return true;
    }
    
    public Block buildBlock(Block curBlock, BlockFactory blockFactory) {
        Block	prvBlock, nxtBlock;
        
        groupDotedLines(curBlock, blockFactory);
        
        for(;;) {
            if (((nxtBlock = blockFactory.getNextBlock(curBlock)) != null)) {
                if (nxtBlock.attribute == curBlock.attribute) {
                    if (nxtBlock.getText().startsWith(".")
                    || (nxtBlock.getText().startsWith(" ")
                    && (!isEmptySpace(nxtBlock) || (nxtBlock.getText().length() < 3)))) {
                        blockFactory.mergeBlocks(curBlock, nxtBlock);
                        continue;
                    }
                }
            }
            break;
        }
        
        xrs.newCurrentBlock = curBlock;
        
        for(;;) {
            if ((prvBlock = blockFactory.getPreviousBlock(xrs.newCurrentBlock)) != null) {
                if ((prvBlock.type.equalsIgnoreCase("static") == true)) {
                    if ((prvBlock.attribute == xrs.newCurrentBlock.attribute)) {
                        if ((prvBlock.line > 0)) {
                            prvBlock.type = curBlock.type;
                            blockFactory.mergeBlocks(prvBlock, xrs.newCurrentBlock);
                            xrs.newCurrentBlock = prvBlock;
                            continue;
                        }
                    }
                }
            }
            
            break;
        }
        
        if (((prvBlock = blockFactory.getPreviousBlock(curBlock)) != null) && (prvBlock.type.equalsIgnoreCase("static") == true)) {
            // same line
            if (Character.isLetterOrDigit(prvBlock.getText().charAt(prvBlock.length-1)) && blockFactory.isSameLine(prvBlock, curBlock)) {
                prvBlock.type = curBlock.type;
                blockFactory.mergeBlocks(prvBlock, curBlock);
                xrs.newCurrentBlock = prvBlock;
            }
        }
        
        return xrs.newCurrentBlock;
    }
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
        xrs = new JavelinExtractionRuleResult();
        xrs.newCurrentBlock = block;
        xrs.hasMatched = false;

        if (javelin.isConnected() && isCursorInField(block, javelin.getCurrentLine(), javelin.getCurrentColumn())) {
            // must be first
            block.type = "field";

            String param = block.getOptionalAttribute("nbLines");
            if (param == null) {
                param = "1";
            }

            numberOfLines = (int)Integer.parseInt(param);

            trimEndingDotsAndSpaces(xrs.newCurrentBlock = buildBlock(block, blockFactory));
            blockFactory.setBlockName(block);
            block.setOptionalAttribute("hasFocus", "true");

            xrs.hasMatched = true;
        }
        
        return xrs;
    }
}
