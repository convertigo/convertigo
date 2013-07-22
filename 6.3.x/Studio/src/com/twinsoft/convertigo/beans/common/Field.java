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

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule;
import com.twinsoft.twinj.iJavelin;

/**
 * 
 */
public class Field extends JavelinMashupEventExtractionRule {
	public static final long serialVersionUID = 4350644982787619412L;

	private String Value = "";
	private String Type = "field";
	private String Name = "fieldName";
	transient private boolean fieldFocus = false;
	private int fieldAttrb = 7;
	private XMLRectangle fieldDesc = new XMLRectangle(-1, -1, -1, -1);

	transient boolean bFirstTime;
	transient boolean bAdded;

	public Field() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.twinsoft.convertigo.beans.core.ExtractionRule#init(int)
	 */
	public void init(int reason) {
		// TODO Auto-generated method stub
		super.init(reason);
		bFirstTime = true;
		bAdded = false;
	}

	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		xrs.hasMatched = false;
		xrs.newCurrentBlock = block;

		boolean bLastBlock = false;
		
		if (bFirstTime) {
			bFirstTime = false;
			
			if (fieldDesc.x == -1)
				fieldDesc.x = 0;
			if (fieldDesc.y == -1)
				fieldDesc.y = 0;
			if (fieldDesc.width == -1)
				fieldDesc.width = 6;
			if (fieldDesc.height == -1)
				fieldDesc.height = 1;
		}
		
		// if field has already been added to the blocks
        if (bAdded) {
        	xrs.hasMatched = false;
        	xrs.newCurrentBlock = block;
        	return xrs;
        }	
        
        if (blockFactory.getNextBlock(block) == null) {
    	// case block is the last one : insert block after !
    		bLastBlock = true;
    	} else {
        // block is not the last one in block factory
	    	// test if the block matches to add the block field just before
	        if (block.line < fieldDesc.y) {
	        	xrs.hasMatched = false;
	        	xrs.newCurrentBlock = block;
	        	return xrs;
	        } else if (block.line == fieldDesc.y) {
	        	if (block.column < fieldDesc.x) {
	        		xrs.hasMatched = false;
	            	xrs.newCurrentBlock = block;
	            	return xrs;
	        	}
	        }
    	}
        
        // block matches : add the field block
		Block myField = new Block();
		myField.name = Name;
		myField.type = Type;
		myField.setText(Value);
		myField.bRender = true;
		myField.line = fieldDesc.y;
		myField.column = fieldDesc.x;
		myField.setOptionalAttribute("hasFocus", "false");
		myField.setOptionalAttribute("size", Integer.toString(fieldDesc.width));
		myField.attribute = fieldAttrb;
		addMashupAttribute(myField);
		if (bLastBlock) {
			blockFactory.insertBlock(myField, block);
		} else {
			blockFactory.insertBlock(myField, blockFactory.getPreviousBlock(block));
		}
		
		bAdded = true;
		xrs.newCurrentBlock = block;
		xrs.hasMatched = true;
		return xrs;
	}

	/**
	 * @return Returns the fieldName.
	 */
	public String getFieldName() {
		return Name;
	}

	/**
	 * @param fieldName
	 *            The fieldName to set.
	 */
	public void setFieldName(String fieldName) {
		this.Name = fieldName;
	}

	/**
	 * @return Returns the fieldType.
	 */
	public String getFieldType() {
		return Type;
	}

	/**
	 * @param fieldType
	 *            The fieldType to set.
	 */
	public void setFieldType(String fieldType) {
		this.Type = fieldType;
	}

	/**
	 * @return Returns the fieldValue.
	 */
	public String getFieldValue() {
		return Value;
	}

	/**
	 * @param fieldValue
	 *            The fieldValue to set.
	 */
	public void setFieldValue(String fieldValue) {
		this.Value = fieldValue;
	}

	/**
	 * @return Returns the fieldDesc.
	 */
	public XMLRectangle getFieldDesc() {
		return fieldDesc;
	}

	/**
	 * @param fieldDesc
	 *            The fieldDesc to set.
	 */
	public void setFieldDesc(XMLRectangle fieldDesc) {
		this.fieldDesc = fieldDesc;
	}

	/**
	 * @return Returns the fieldFocus.
	 */
	public boolean isFieldFocus() {
		return fieldFocus;
	}

	/**
	 * @param fieldFocus
	 *            The fieldFocus to set.
	 */
	public void setFieldFocus(boolean fieldFocus) {
		this.fieldFocus = fieldFocus;
	}

	/**
	 * @return Returns the fieldAttrb.
	 */
	public int getFieldAttrb() {
		return fieldAttrb;
	}

	/**
	 * @param fieldAttrb The fieldAttrb to set.
	 */
	public void setFieldAttrb(int fieldAttrb) {
		this.fieldAttrb = fieldAttrb;
	}
}
