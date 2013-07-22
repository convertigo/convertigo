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
import com.twinsoft.convertigo.beans.core.BlockFactoryWithVector;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.twinj.iJavelin;

/**
 * This is the default block factory for Videotex.
 */
public class DefaultBlockFactory extends BlockFactoryWithVector {
    
	private static final long serialVersionUID = -6876593735009331388L;

	transient int nbLines;
    transient int nbColumns;
    transient int nbItems;
    transient iJavelin javelin;
    
    public DefaultBlockFactory() {
        super();
    }
    
    public void make(iJavelin javelin) {
        this.javelin = javelin;
        nbColumns = javelin.getScreenWidth();
        nbLines = javelin.getScreenHeight();
        
        list = new LinkedList<Block>();
		fields = new HashMap<String, Block>();
        
        nbItems = javelin.getNumberOfFields();
        if (nbItems == 0) {
            convertUnformattedScreen();
        }
        else {
            convertFormattedScreen();
        }
    }
    
    private char formatChar(char c) {
        switch(c) {
            case '\0':
            case '\256':
            case '\u2400':
                return ' ';
            default: return c;
        }
    }
    
    private String formatString(String s) {
        int len = s.length();
        char[] sc = new char[len];
        s.getChars(0, len, sc, 0);
        for (int i = 0 ; i < len ; i++) {
            sc[i] = formatChar(sc[i]);
        }
        
        return new String(sc);
    }

    private static final int STATE_SAME_ATTRIBUTE = 1;
    private static final int STATE_NEW_BLOCK      = 2;
    private static final int STATE_GOT_SPACE      = 3;
    
    private void splitProtectedField(int i, boolean penSelectable) {
        StringBuffer sb = null;
        char c;
        String s;
        
        boolean spaceBlock = false;
        
        int state = STATE_NEW_BLOCK;
        
        int x0 = javelin.getFieldColumn(i);
        int y0 = javelin.getFieldLine(i);

        int x = x0;
        int y = y0;        
        
        int initialFieldLen = javelin.getFieldLength(i);
        int fieldLen = initialFieldLen;
        
        int attribute = javelin.getCharAttribute(x0, y0);
        
        Block block = null;
        
        while (fieldLen > 0) {
            switch (state) {
                case STATE_NEW_BLOCK:
                    if (block != null) {
                        s = sb.toString();
                        block.setText(s);
                        list.add(block);
                    }
                    sb = new StringBuffer(initialFieldLen);
                    block = new Block();
                    block.column    = x;
                    block.line      = y;
                    block.attribute = attribute;
                    block.type      = "static";
                    block.length    = 1;
                    if (penSelectable)
                    	block.setOptionalAttribute("penselectable", "true");
                    
                    c = formatChar(javelin.getChar(x, y));
                    sb.append(c);
                    if (c == ' ')
                        spaceBlock = true;
                    else
                        spaceBlock = false;
                    
                    state = STATE_SAME_ATTRIBUTE;
                    x++;
                    fieldLen--;
                    break;
                    
                case STATE_SAME_ATTRIBUTE:
                    if (x >= nbColumns) {
                        y++;
                        if (y == javelin.getScreenHeight()) y = 0;
                        x = 0;
                        attribute = javelin.getCharAttribute(x, y);
                        state = STATE_NEW_BLOCK;
                        break;
                    }

                    int newAttribute = javelin.getCharAttribute(x, y);
                    if (attribute != newAttribute) {
                        attribute = newAttribute;
                        state = STATE_NEW_BLOCK;
                        break;
                    }
                    
                    c = formatChar(javelin.getChar(x, y));
                    if (spaceBlock) {
                        if (c != ' ') {
                            state = STATE_NEW_BLOCK;
                            spaceBlock = false;
                            break;
                        }
                    }
                    else {
                        if (c == ' ') {
                            state = STATE_GOT_SPACE;
                            break;
                        }
                    }
                    sb.append(c);
                    x++;
                    fieldLen--;
                    break;
                    
                case STATE_GOT_SPACE:
                    // System.out.println("STATE_GOT_SPACE");
                    // is the next char a space ?
                    // yes two spaces so do a new block
                    state = STATE_NEW_BLOCK;
                    spaceBlock = true;
                    break;
            }
        }
        
        if (sb != null) {
            s = sb.toString();
            block.setText(s);
            list.add(block);
        }
    }
    
    void createFiller(int index) {
        int line = javelin.getFieldLine(index);
        int column = javelin.getFieldColumn(index);
        
        Block block = new Block();
        block.setText(" ");
        
        if (column == 0) {
            block.line = line - 1;
            if (block.line == -1) block.line = javelin.getScreenHeight() - 1;
            block.column = javelin.getScreenWidth() - 1;
        }
        else {
            block.line = line;
            block.column = column - 1;
        }

        block.attribute = javelin.getCharAttribute(block.column, block.line);
        block.type = "filler";
        block.length = 1;
        setBlockName(block);
        list.add(block);
    }
    
    private void convertFormattedScreen() {
        int attribute;
        Block block;
        String text;
        
        for (int i = 0 ; i < nbItems ; i++) {
            createFiller(i);
            attribute = javelin.getFieldAttribute(i);
            if ((attribute & iJavelin.AT_FIELD_PROTECTED) > 0) {
                splitProtectedField(i, (attribute & iJavelin.AT_FIELD_PEN_SELECTABLE) != 0);
            }
            else {
                block = new Block();
                text = javelin.getFieldText(i);
                text = formatString(text);
            	block.setText(rightTrim(text));
                block.line = javelin.getFieldLine(i);
                block.column = javelin.getFieldColumn(i);
                block.attribute = attribute;
                block.type = "field";
                block.length = javelin.getFieldLength(i);
                setBlockName(block);
                
                setupFieldBlock(block, attribute);
                setupContinuousFields(block,  i);
                
                list.add(block);
				fields.put(block.name, block);
            }
        }
    }

    private void setupContinuousFields(Block block, int fieldIndex) {
    	if (javelin.getPreviousContinuousColumn(fieldIndex) != -1) {
	    	String previousFieldName =  Parameter.JavelinField.getName()+"c" + javelin.getPreviousContinuousColumn(fieldIndex) + "_l" + javelin.getPreviousContinuousFieldLine(fieldIndex);
			block.setOptionalAttribute("previousContinuous", previousFieldName);
    	}
    	if (javelin.getNextContinuousColumn(fieldIndex) != -1) {
	    	String nextFieldName     =  Parameter.JavelinField.getName()+"c" + javelin.getNextContinuousColumn(fieldIndex) + "_l" + javelin.getNextContinuousFieldLine(fieldIndex);
			block.setOptionalAttribute("nextContinuous", nextFieldName);
    	}
    }
    
    private void setupFieldBlock(Block block, int attribute) {
        block.setOptionalAttribute("size", (new Integer(block.length)).toString());
        
		if ((javelin.getCurrentLine() == block.line ) && (javelin.getCurrentColumn() == block.column)) {
			block.setOptionalAttribute("hasFocus", "true");
		}
		
		if ((attribute & iJavelin.AT_FIELD_RESERVED) == iJavelin.AT_FIELD_RESERVED) {
			block.setOptionalAttribute("reserved", "true");
		}
		
		if ((attribute & iJavelin.AT_FIELD_PEN_SELECTABLE) == iJavelin.AT_FIELD_PEN_SELECTABLE) {
			block.setOptionalAttribute("penselectable", "true");
		}
		
		if ((attribute & iJavelin.AT_FIELD_NUMERIC) == iJavelin.AT_FIELD_NUMERIC) {
			block.setOptionalAttribute("numeric", "true");
		}
		
		if ((attribute & iJavelin.AT_AUTO_ENTER) == iJavelin.AT_AUTO_ENTER) {
			block.setOptionalAttribute("autoenter", "true");
		}
		
		if ((attribute & iJavelin.AT_AUTO_TAB) == iJavelin.AT_AUTO_TAB) {
			block.setOptionalAttribute("autotab", "true");
		}
		
		if ((attribute & iJavelin.AT_HIDDEN) == iJavelin.AT_HIDDEN) {
			block.setOptionalAttribute("hidden", "true");
		}
		
		if ((attribute & iJavelin.AT_INK) == ((attribute & iJavelin.AT_PAPER) >> 3)) {
			// Ink and paper are the same, assume block is hidden (password)
			block.setOptionalAttribute("hidden", "true");
		}
    }
    
    private void convertUnformattedScreen() {
        int lastAttribute,currentAttribute;
        int startx;
        String text;
        
        for (int y = 0 ; y < nbLines ; y++) {
            startx = 0;
            lastAttribute = javelin.getCharAttribute(0, y);
            for (int x = 1 ; x < nbColumns ; x++) {
                currentAttribute = javelin.getCharAttribute(x, y);
                if (currentAttribute != lastAttribute) {
                    // Inserting a new block
                    Block block = new Block();
                	block.line = y;
                	block.column = x;
                    block.length = x - startx;
                    block.attribute = lastAttribute;
                    text = formatString(javelin.getString(startx, y, block.length));
                    if (((javelin.getTerminalClass().equals(iJavelin.VDX)) ||
                            (javelin.getTerminalClass().equals(iJavelin.VT)) ||
                    		(block.attribute & iJavelin.AT_FIELD_PROTECTED) > 0)) {
                    	block.type = "static";
                    	block.setText(text);
                    }
                    else {
                    	block.type = "field";
                    	block.setText(rightTrim(text));
                    	setBlockName(block);
                        setupFieldBlock(block, lastAttribute);
                    }
                    block.line = y;
                    block.column = startx;
                    list.add(block);
                    
                    // Updating temporary variables
                    lastAttribute = currentAttribute;
                    startx = x;
                }
            }
            
            // Inserting the last block
            Block block = new Block();
        	block.line = y;
        	block.column = startx;
            block.attribute = lastAttribute;
            block.length = nbColumns - startx;
            block.setText(javelin.getString(startx, y, block.length));
            if (((javelin.getTerminalClass().equals(iJavelin.VDX)) ||
                    (javelin.getTerminalClass().equals(iJavelin.VT)) ||
            		(block.attribute & iJavelin.AT_FIELD_PROTECTED) > 0)) {
            	block.type = "static";
                block.setText(javelin.getString(startx, y, block.length));
            }
            else {
            	block.type = "field";
            	setBlockName(block);
            	block.setOptionalAttribute("size", "" + block.length);
            }
            list.add(block);
        }
    }
    
    public static String rightTrim(String s) {
		int len = s.length();
		if (len == 0) return s;
		
		while ((len > 0) && (s.charAt(len - 1) == ' ')) {
			len--;
		}
		
		return (len > 0 ? s.substring(0, len) : "");
	}
   
}
