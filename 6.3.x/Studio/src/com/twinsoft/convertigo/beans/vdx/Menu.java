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
 * This class manages menus.
 */
public class Menu extends JavelinExtractionRule {
    
	public static final long serialVersionUID = -4275848733853605435L;
    
    private static final int DIGIT = 0;
    private static final int LETTER = 1;
    private static final int LETTER_OR_DIGIT = 2;
    
    private int startLine = 1;
    private int menuWidth = 2;
    private int charType = DIGIT;
    
    transient int refColumn;
    transient int refAttrib;
    
    public Menu() {
        super();
        setFinal(true);
    }
    
    /**
     * Checks if menu string contains allowed menu chars.
     *
     * @param str the menu text to be checked.
     *
     * @return true if str compatible.
     */
    public boolean isCharTypeOK(String str) {
        int len = str.length();
        
        switch(charType) {
            case LETTER:
                for(int i = 0 ; i < len ; i++)
                    if (!Character.isLetter(str.charAt(i)))
                        return false;
                return true;
                
            case DIGIT:
                for(int i = 0 ; i < len ; i++)
                    if (!Character.isDigit(str.charAt(i)))
                        return false;
                return true;
                
            case LETTER_OR_DIGIT:
                for(int i = 0 ; i < len ; i++)
                    if (!Character.isLetterOrDigit(str.charAt(i)))
                        return false;
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Checks if line bigger or equal to starting line.
     *
     * @param lin the line to be checked.
     *
     * @return true if line compatible.
     */
    public boolean isLineOK(int lin) {
        return (lin >= startLine);
    }
    
    /**
     * Checks if the block column is in the reference column area.
     *
     * @param block the block to check.
     *
     * @return true if block in column area.
     */
    public boolean isColumnOK(Block block) {
        if (block.length > menuWidth)
            return false;
        
        if (refColumn == block.column) // best case
            return true;
        
        if (refColumn == block.column+block.length-1)
            return true;
        
        return false;
    }
    
    /**
     * Checks if string length compatible with a menu type text.
     *
     * @param str the string to be checked.
     *
     * @return true if length compatible.
     */
    public boolean isLengthOK(String str) {
        int len = str.length();
        return ((len >= 1) && (len <= menuWidth));
    }
    
    /**
     * Checks if string length compatible with a menu type text.
     *
     * @param str the string to be checked.
     *
     * @return true if length compatible.
     */
    public boolean isAttributeOK(int Attrib) {
        char ink, rink;
        char pap, rpap;
        
        // handle INVERT attribute
        if ((Attrib & iJavelin.AT_INVERT) == iJavelin.AT_INVERT) {
            pap = (char)(Attrib & iJavelin.AT_INK);
            ink = (char)((Attrib & iJavelin.AT_PAPER) >> 3);
        }
        else {
            ink = (char)(Attrib & iJavelin.AT_INK);
            pap = (char)((Attrib & iJavelin.AT_PAPER) >> 3);
        }
        
        // handle INVERT attribute
        if ((refAttrib & iJavelin.AT_INVERT) == iJavelin.AT_INVERT) {
            rpap = (char)(refAttrib & iJavelin.AT_INK);
            rink = (char)((refAttrib & iJavelin.AT_PAPER) >> 3);
        }
        else {
            rink = (char)(refAttrib & iJavelin.AT_INK);
            rpap = (char)((refAttrib & iJavelin.AT_PAPER) >> 3);
        }
        
        return((ink == rink) && (pap == rpap));
    }
    
    /**
     * Sets the max width for a menu.
     *
     * @param width the max width allowed.
     */
    public void setMenuWidth(int width) {
        menuWidth = width;
    }
    
    /**
     * Returns the current width.
     *
     * @return the actual width value.
     */
    public int getMenuWidth() {
        return menuWidth;
    }
    
    
    /**
     * Sets the char types allowed in menu strings.
     *
     * @param type IS_DIGIT=0, IS_LETTER=1 or IS_LETTER_OR_DIGIT=2.
     */
    public void setCharType(int type) {
    	charType = type;
    }
    
    /**
     * Returns the current startline.
     *
     * @return the actual line value.
     */
    public int getCharType() {
        return charType;
    }
    
    /**
     * Sets the line from which this rule applies.
     *
     * @param line the starting line.
     */
    public void setStartLine(int line) {
        startLine = line;
    }
    
    /**
     * Returns the current startline.
     *
     * @return the actual line value.
     */
    public int getStartLine() {
        return startLine;
    }
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        xrs.newCurrentBlock = block;
        boolean menuDetected = false;
        
        Block blk = null;
/*
        if ((blk = blockFactory.getBlockByType("field")) == null)
            return xrs;

        // if more than 3,  don't use menus
        menuWidth = Math.min(3, (int)Integer.parseInt(blk.getOptionalAttribute("size")));
*/
        String	str1 = block.getText();

        // if we have find a single char string
        // check with line & column for other 1 char strings
        if (isLengthOK(str1) && isLineOK(block.line) && isCharTypeOK(str1)) {
            String str;
            refColumn = block.column;
            refAttrib = block.attribute;

            blk = block;

            while ((blk = blockFactory.getNextBlock(blk)) != null) {
                str = blk.getText();
                if (isLengthOK(str) && isLineOK(block.line) && isCharTypeOK(str) && isColumnOK(blk) && isAttributeOK(blk.attribute)) {
                    menuDetected = true;
                    blk.setOptionalAttribute("item", ' ' + str + ' ');
                    // decrease colum because we gonna add a space before
                    if (refColumn > 0) blk.column--;
                    blk.type = "menu";
                }
            }

            if (menuDetected) {
                block.setOptionalAttribute("item", ' ' + str1 + ' ');
                // decrease colum because we gonna add a space before
                if (refColumn > 0) block.column--;
                block.type = "menu";

                xrs.newCurrentBlock = block;
                xrs.hasMatched = true;
            }
        }
        
        return xrs;
    }
}
