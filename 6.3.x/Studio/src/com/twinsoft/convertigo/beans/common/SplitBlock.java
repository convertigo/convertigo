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

import java.util.StringTokenizer;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.twinj.iJavelin;

/**
 *
 * @author  romainm
 * This class allows to split blocks using the java.util.StringTokenizer class.
 * It looks for delimiters in the string and then cut off the string in tokens around the delimiter index.
 * You can specify your own delimiters (space is the delimiter by default).
 * You can choose to treat delimiters as tokens or not.
 * Here is an exemple :
 *   - if you keep delimiters and specify | for delimiter
 *     the block [abc|d | ef|gh] will be split in [abc][|][d ][|][ ef][|][gh]
 *   - if you don't keep delimiters
 *     the block will be split in [abc] [d ] [ ef] [gh]
 * This class has been implemented to adjust automatically the column attribute of each sub block, using the variable lastIndex.
 * Each new sub block is inserted after the block to be splitted.
 * Once all the sub blocks have been inserted, we remove the original block.
 * If no tokens can be found in the string the extraction rule doesn't match, the original block isn't removed
 */
public class SplitBlock extends JavelinExtractionRule {
    
	private static final long serialVersionUID = -6563690761722467896L;

	private String delimiters = " ";
    
    private boolean keepDelimiters = false;
    
    /** Getter for property delimiters.
     * @return Value of property delimiters.
     */
    public java.lang.String getDelimiters() {
        return delimiters;
    }
    
    /** Setter for property delimiters.
     * @param delimiters New value of property delimiters.
     */
    public void setDelimiters(java.lang.String delimiters) {
        this.delimiters = delimiters;
    }
    
    /** Getter for property keepDelimiters.
     * @return Value of property keepDelimiters.
     */
    public boolean isKeepDelimiters() {
        return keepDelimiters;
    }
    
    /** Setter for property keepDelimiters.
     * @param keepDelimiters New value of property keepDelimiters.
     */
    public void setKeepDelimiters(boolean keepDelimiters) {
        this.keepDelimiters = keepDelimiters;
    }
    
    /** Creates a new instance of SplitBlock */
    public SplitBlock() {
        super();
    }
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        
        Engine.logBeans.trace("Entering SplitBlock : " + block.getText()+ " line :"+block.line+" col :" +block.column);

        //create the tokenizer
        StringTokenizer st = new StringTokenizer(block.getText(), delimiters, true);
        if (st.countTokens() > 0){
            Block curBlock = blockFactory.getPreviousBlock(block);
            Block tempBlock;
            // variable lastIndex is used to adjust the column attribute of the created blocks
            int lastIndex = 0;
            if (keepDelimiters) {
                while(st.hasMoreTokens()){
                    // clones the original blocks in order to get the same attributes
                    try{
                        tempBlock = (Block) block.clone();
                    }
                    catch(CloneNotSupportedException e){
                        xrs.hasMatched = false;
                        xrs.newCurrentBlock = block;
                        return (xrs);
                    }
                    tempBlock.setText(st.nextToken());
                    // adjust the colum attribute
                    tempBlock.column = block.column + lastIndex;

                    // set lastIndex juste before the next token or delimiter
                    lastIndex += tempBlock.getText().length();

                    // insert the new sub block
                    blockFactory.insertBlock(tempBlock, curBlock);
                    Engine.logBeans.trace("SplitBlock new block: " + tempBlock.getText()+" line :"+tempBlock.line+" col :" + tempBlock.column);
                    // update the current block
                    curBlock = tempBlock;
                }
            }
            else {
                String pattern;

                while(st.hasMoreTokens()){
                    pattern = st.nextToken();
                    if (delimiters.indexOf(pattern.charAt(0)) == -1) {
                        // clones the original blocks in order to get the same attributes
                        try{
                            tempBlock = (Block) block.clone();
                        }
                        catch(CloneNotSupportedException e){
                            xrs.hasMatched = false;
                            xrs.newCurrentBlock = block;
                            return (xrs);
                        }
                        tempBlock.setText(pattern);
                        // adjust the colum attribute
                        tempBlock.column = block.column + lastIndex;

                        // insert the new sub block
                        blockFactory.insertBlock(tempBlock, curBlock);
                        Engine.logBeans.trace("SplitBlock new block: " + tempBlock.getText()+" line :"+tempBlock.line+" col :" + tempBlock.column);
                        // update the current block
                        curBlock = tempBlock;
                    }

                    // set lastIndex juste before the next token or delimiter
                    lastIndex += pattern.length();
                }
            }
            // once all tokens have been inserted, remove the original block

            Engine.logBeans.trace("Exiting SplitBlock : " + block.getText());
            if (curBlock != null)
                Engine.logBeans.trace("currentBlock : " + curBlock.getText()+ " line :"+curBlock.line+" col :" +curBlock.column);

            blockFactory.removeBlock(block);
            xrs.hasMatched = true;
            xrs.newCurrentBlock = curBlock;
            return (xrs);
            }
        Engine.logBeans.trace("Exiting SplitBlock without change : " + block.getText());
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        return (xrs);
    }
}
