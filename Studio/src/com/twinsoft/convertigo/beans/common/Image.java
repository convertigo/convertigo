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
public class Image extends JavelinMashupEventExtractionRule {
    public static final long serialVersionUID = 4350644982787619412L;
    
    /* Properties */
    private 	String			label 			= "";
    private 	String			action 			= "";
    private 	boolean			doTransaction 	= false;
    private 	XMLRectangle	imageDesc 		= new XMLRectangle(-1, -1, -1, -1);
    private		String 			url 			= "";
    private 	boolean			keepSize 		= true;
    private 	String			zOrder 			= "";
    
    /* Working variables */
    transient	boolean			bFirstTime;
    transient	boolean			bAdded;
    
    
    public Image() {
        super();
    }

	/* (non-Javadoc)
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
    		if (imageDesc.x == -1) 		imageDesc.x = 0;
    		if (imageDesc.y == -1) 		imageDesc.y = 0;
    		if (!keepSize) {
        		if (imageDesc.width == -1) 	imageDesc.width = -1;
        		if (imageDesc.height == -1) imageDesc.height = -1;
        	}
        	bFirstTime = false;
        }
        
        // if image has already been added to the blocks
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
	    	// test if block matches to add the block image just before
	    	if (block.line < imageDesc.y) {
	        	xrs.hasMatched = false;
	        	xrs.newCurrentBlock = block;
	        	return xrs;
	        } else if (block.line == imageDesc.y) {
	        	if (block.column < imageDesc.x) {
	        		xrs.hasMatched = false;
	            	xrs.newCurrentBlock = block;
	            	return xrs;
	        	}
	        }
    	}
    	       
        // block matches : create image block
        Block myImage = new Block();
        myImage.name = "untitled";
        myImage.type = "image";
        myImage.setText("");
		myImage.bRender = true;
        myImage.column = imageDesc.x;
        myImage.line = imageDesc.y;
        myImage.setOptionalAttribute("url", url);
		myImage.setOptionalAttribute("alt", label);
        
        if (!keepSize) {
        	myImage.setOptionalAttribute("width", "" + imageDesc.width);
        	myImage.setOptionalAttribute("height", "" + imageDesc.height);
        }
		
		if (!action.equals(""))
			myImage.setOptionalAttribute("action", action);
		if (doTransaction)
			myImage.setOptionalAttribute("dotransaction", "true");
		if (!zOrder.equals(""))
			myImage.setOptionalAttribute("z-order", zOrder);
		
		addMashupAttribute(myImage);
		
		if (bLastBlock) {
			blockFactory.insertBlock(myImage, block);
		} else {
			blockFactory.insertBlock(myImage, blockFactory.getPreviousBlock(block));
		}
		bAdded = true;
		xrs.hasMatched = true;
		xrs.newCurrentBlock = block;
		return xrs;
    }

    
    /**
	 * @return Returns the action.
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param action The action to set.
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * @return Returns the doTransaction.
	 */
	public boolean isDoTransaction() {
		return doTransaction;
	}

	/**
	 * @param doTransaction The doTransaction to set.
	 */
	public void setDoTransaction(boolean doTransaction) {
		this.doTransaction = doTransaction;
	}

	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label The label to set.
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	public XMLRectangle getImageDesc() {
		return imageDesc;
	}

	public void setImageDesc(XMLRectangle imageDesc) {
		this.imageDesc = imageDesc;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isKeepSize() {
		return keepSize;
	}

	public void setKeepSize(boolean keepSize) {
		this.keepSize = keepSize;
	}

	public void setZOrder(String zOrder) {
		this.zOrder = zOrder;
	}

	public String getZOrder() {
		return zOrder;
	}
	
}
