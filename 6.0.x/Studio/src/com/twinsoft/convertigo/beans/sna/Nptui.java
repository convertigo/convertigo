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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.eicon.iConnect.acl.ACLNptuiChoice;
import com.eicon.iConnect.acl.ACLNptuiComponent;
import com.eicon.iConnect.acl.ACLNptuiScreen;
import com.eicon.iConnect.acl.ACLNptuiScrollBar;
import com.eicon.iConnect.acl.ACLNptuiSelectionField;
import com.eicon.iConnect.acl.ACLNptuiWindow;
import com.twinsoft.convertigo.beans.common.XMLRectangle;
import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.twinj.iJavelin;

/**
 * This class extracts 5250 extended objects for the SNA Emulator.
 * 
 */

public class Nptui extends JavelinExtractionRule {
    
	private static final long serialVersionUID = 1L;
	
	/* Properties */
	/** Holds value of property bWindow. */
    private boolean bWindow = true;
    /** Holds value of property bChoice. */
    private boolean bChoice = true;
    /** Holds value of property bScrollBar. */
    private boolean bScrollBar = true;
    /** Holds value of property bButton. */
    private boolean bButton = true;
    /** Holds value of property bCheckbox. */
    private boolean bCheckbox = true;
    /** Holds value of property bMenu. */
    private boolean bMenu = true;
    /** Holds value of property bRadio. */
    private boolean bRadio = true;
    
    /* Working variables */
    transient boolean 	bFirstTime;
    transient boolean[] attributes;
    transient String[] 	colors;
    
    public Nptui() {
        super();
    }

    public void init(int reason) {
		// TODO Auto-generated method stub
		super.init(reason);
		bFirstTime 	= true;
		attributes 	= new boolean[] { true, true, true, true, true, true, true, true, true, true, true };
		colors 		= new String[] {"black", "red", "green", "yellow", "blue", "magenta", "cyan", "white"};
	}
        
    /**
     * Applies the extraction rule to the current iJavelin object.
     *
     * @param javelin the Javelin object.
     * @param block the current block to analyze.
     * @param blockFactory the block context of the current block.
     *
     * @return an ExtractionRuleResult object containing the result of
     * the query.
     */
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
    	
    	int screenWidth = javelin.getScreenWidth();
    	
    	if (bFirstTime) {
    		bFirstTime = false;
    		ACLNptuiScreen mptuiScreen = null;
	    	
    		try {
	    		mptuiScreen = (ACLNptuiScreen) javelin.getInternalDataStructure();
	    	} catch (ClassCastException e) {
	    		xrs.hasMatched = false;
	            xrs.newCurrentBlock = block;
	            return (xrs);
	    	}
	    	
	    	ACLNptuiComponent[] components = mptuiScreen.getComponents();
	    	if (components.length == 0) {
	    		// no Nptui component on this screen
	    		xrs.hasMatched = false;
	            xrs.newCurrentBlock = block;
	            return (xrs);
	    	}
	    	
	    	ACLNptuiWindow[] windows = mptuiScreen.getWindows();
	    	// inspect all windows to handle them first
	    	for (int i = 0 ; i < windows.length ; i++) {
	    		Engine.logBeans.trace("Treating window " + i + " : z-order " + windows[i].getZOrder());
				treatAndAddComponent(windows[i], screenWidth, dom, blockFactory, null, javelin);
	    	}
	    	
	    	// windows have been handled, 
	    	// inspect all other components
			for (int i = 0 ; i < components.length ; i++) {
				if (components[i] instanceof ACLNptuiWindow) {
					Engine.logBeans.trace("Not treating component " + i + " : " + components[i].getClass());
				} else {
					Engine.logBeans.trace("Treating component " + i + " : " + components[i].getClass());
					treatAndAddComponent(components[i], screenWidth, dom, blockFactory, null, javelin);
				}
			}
			xrs.hasMatched = true;
	        xrs.newCurrentBlock = block;
	        return (xrs);
    	} else {    	
	    	xrs.hasMatched = false;
	        xrs.newCurrentBlock = block;
	        return (xrs);
    	}
    }
    
    /**
     * Treat a NPTUI component, create a block corresponding to it, 
     * add this block to the block factory, 
     * remove old blocks corresponding to it from block factory, 
     * treat window's children components,
     * copy window's children blocks in optionnal children,
     * etc.
     *  
     * @param component : the NPTUI component to treat
     * @param screenWidth : the screen width
     * @param dom : the DOM
     * @param blockFactory : the block factory
     * @param parent : the parent Block, the window to which add the component if it is a window's child, null if no parent window exists.
     * @param javelin : the javelin emulator object
     */
    private void treatAndAddComponent(ACLNptuiComponent component, int screenWidth, org.w3c.dom.Document dom, BlockFactory blockFactory, Block parent, iJavelin javelin) {
    	int offset = component.getScreenOffset();
		int compLine, compCol;
		if (offset % screenWidth != 0) {
			compLine = offset / screenWidth;
			compCol = offset % screenWidth;
		} else {
			compLine = (offset / screenWidth) - 1;
			compCol = 0;
		}
		
		int cursorLine = javelin.getCurrentLine();
		int cursorColumn = javelin.getCurrentColumn();
		
		if (blockFactory == null) {
			// no parent block defined and no blockFactory
			Engine.logBeans.trace("(treatAndAddComponent) no parent block defined and no blockFactory");
			return;
		}
		// look for the object place in blockFactory
		Block beginBlock = blockFactory.getFirstBlock();
		Block afterBlock = null;
		if (beginBlock == null) {
			// no block in blockFactory
			Engine.logBeans.trace("(treatAndAddComponent) no block in blockFactory");
			return;
		} 
		// there is at least one block in blockFactory
		if (beginBlock.line == compLine && beginBlock.column >= compCol) {
			// the block matching component's line and column is the first block of blockFactory
			afterBlock = null;
		} else {
			while ( beginBlock.line < compLine // while beginBlock is on a line inferior to compLine
				|| (beginBlock.line == compLine && beginBlock.column < compCol) ) { // or beginBlock is on the good line but on a column inferior to compCol
				afterBlock = beginBlock;
				beginBlock = blockFactory.getNextBlock(beginBlock);
				
				if (beginBlock == null) {
					// no block in blockFactory is matching component's line and column
					Engine.logBeans.trace("(treatAndAddComponent) no block in blockFactory is matching component's line and column");
					return;
				}
			}
		}
		// beginBlock.line = compLine and beginBlock.column >= compCol
		// afterBlock = getPreviousBlock(beginBlock)
		// the created block will be added after afterBlock in blockFactory
		
		if (component instanceof ACLNptuiWindow ) {
			if (bWindow) {
				Engine.logBeans.trace("(treatAndAddComponent) window detected and extraction allowed");
				ACLNptuiWindow comp = (ACLNptuiWindow) component; 
				
				Block myPanel = new Block();
				myPanel.name = "untitled";
				myPanel.type = "panel";
				myPanel.setText("");
				myPanel.bRender = true;
				myPanel.line = compLine;
				myPanel.column = compCol;
				int height = comp.getHeight() - 1;
				myPanel.setOptionalAttribute("width", "" + comp.getWidth());
				myPanel.setOptionalAttribute("height", "" + height);
				myPanel.setOptionalAttribute("shadow", "" + comp.isShadow());
				if (comp.getHeader() != null) {
					myPanel.setOptionalAttribute("topTitle", "" + comp.getHeader());
					myPanel.setOptionalAttribute("topTitleColumn", "" + comp.getHeaderJustification());
				} else {
					myPanel.setOptionalAttribute("topTitle", "");
					myPanel.setOptionalAttribute("topTitleColumn", "");
				}
				if (comp.getFooter() != null) {
					myPanel.setOptionalAttribute("bottomTitle", "" + comp.getFooter());
					myPanel.setOptionalAttribute("bottomTitleColumn", "" + comp.getFooterJustification());
				} else {
					myPanel.setOptionalAttribute("bottomTitle", "");
					myPanel.setOptionalAttribute("bottomTitleColumn", "");
				}
				myPanel.setOptionalAttribute("zOrder", "" + comp.getZOrder());

// comp.isPullDownMenu());
				
				// treat every contained components
				ACLNptuiComponent[] childrenComp = comp.getComponents();
				
				for (int i=0; i < childrenComp.length; i++) {
					Engine.logBeans.trace("Treating window child component " + i + " : " + childrenComp[i].getClass());
					treatAndAddComponent(childrenComp[i], screenWidth, dom, blockFactory, myPanel, javelin);
				}
				
				// remove blocks from the existant window, and copy the content blocks to the new window
				removeBlocksFromWindow(blockFactory, afterBlock, compCol, compLine, comp.getWidth() + 2, comp.getHeight(), myPanel, dom);
				
				if (parent == null) {
					blockFactory.insertBlock(myPanel, afterBlock);
				} else {
					// window in another component : impossible
					Engine.logBeans.trace("(treatAndAddComponent) window in another component : error, don't add this component");
				}
			} else {
				Engine.logBeans.trace("(treatAndAddComponent) window detected but extraction not allowed");
			}
		} else if (component instanceof ACLNptuiScrollBar ) {
			if (bScrollBar) {
				Engine.logBeans.trace("(treatAndAddComponent) scrollbar detected and extraction allowed");
				ACLNptuiScrollBar comp = (ACLNptuiScrollBar)component;
				
				Block mySlider = new Block();
				mySlider.name = "untitled";
				mySlider.type = "slider";
				mySlider.setText("");
				mySlider.line = compLine;
				mySlider.column = compCol;
				if (comp.isHorizontal()) {
					mySlider.setOptionalAttribute("width", "" + comp.getSize());
					mySlider.setOptionalAttribute("height", "1");
				} else {
					mySlider.setOptionalAttribute("width", "1");
					mySlider.setOptionalAttribute("height", "" + comp.getSize());
				}
				mySlider.setOptionalAttribute("shadow", "" + comp.isShadow());
				mySlider.setOptionalAttribute("hasArrows", "" + comp.hasArrows());
				mySlider.setOptionalAttribute("itemsCount", "" + comp.getItemsCount());
				mySlider.setOptionalAttribute("itemsCountBeforeSlider", "" + comp.getItemsCountBeforeSlider());
				mySlider.setOptionalAttribute("sliderSize", "" + comp.getSliderSize());
				mySlider.setOptionalAttribute("sliderPos", "" + comp.getSliderPosInShaft());

				// remove blocks from blockFactory
				if (comp.isHorizontal()) {
					removeBlocksHorizontaly(blockFactory, afterBlock, compCol, compLine, comp.getSize());
				} else {
					removeBlocksVerticaly(blockFactory, afterBlock, compCol, compLine, comp.getSize());
				}
				
				// add generated blocks
				if (parent == null) {
					blockFactory.insertBlock(mySlider, afterBlock);
				} else {
					// scrollbar in window
					parent.addOptionalChildren(mySlider);
				}
			} else {
				Engine.logBeans.trace("(treatAndAddComponent) scrollbar detected but extraction not allowed");
			}
		} else if (component instanceof ACLNptuiSelectionField ) {
			ACLNptuiSelectionField comp = (ACLNptuiSelectionField)component; 
			
			Block myContainer = new Block();
			myContainer.name = "untitled";
			
			myContainer.setText("");
			myContainer.line = compLine;
			myContainer.column = compCol;
			myContainer.setOptionalAttribute("shadow", "" + comp.isShadow());
			myContainer.setOptionalAttribute("choicesTextSize", "" + comp.getChoiceTextSize());
			myContainer.setOptionalAttribute("choicesCols", "" + comp.getChoiceColumns());
			myContainer.setOptionalAttribute("choicesRows", "" + comp.getChoiceRows());
			if (comp.isAutoEnter())
				myContainer.setOptionalAttribute("autoEnter", "" + comp.isAutoEnter());
			if (comp.isAutoSelect())
				myContainer.setOptionalAttribute("autoSelect", "" + comp.isAutoSelect());
			
			ACLNptuiChoice[] choices = comp.getChoices();
			int type = ((ACLNptuiSelectionField)component).getType();
			
			if (type == ACLNptuiSelectionField.TYPE_BUTTONS) {
				if (bButton) {
					Engine.logBeans.trace("(treatAndAddComponent) buttons detected and extraction allowed");
					myContainer.type = "buttonsPanel";
					
					for (int j =0; j < choices.length; j++) {
						ACLNptuiChoice choice = choices[j];
						int choiceOffset = choice.getScreenOffset();
						
						Element element = dom.createElement("block");
						element.setAttribute("name", "untitled");
						element.setAttribute("type", "keyword");
						
						Text textNode = dom.createTextNode(choice.getText());
				        element.appendChild(textNode);
						
				        int choiceLine, choiceColumn;
				        if (choiceOffset % screenWidth != 0) {
				        	choiceLine = choiceOffset / screenWidth;
				        	choiceColumn = choiceOffset % screenWidth;
		            	} else {
		            		choiceLine = (choiceOffset / screenWidth) - 1;
		            		choiceColumn = 0;
		            	}
				        element.setAttribute("line", "" + choiceLine);
	            		element.setAttribute("column", "" + choiceColumn);
	            		
						element.setAttribute("action", "KEY_NPTUI");
						element.setAttribute("width", "" +  choice.getLength());
						element.setAttribute("height", "1");
						element.setAttribute("dotransaction", "false");
						element.setAttribute("available", "" + choice.isAvailable());
						
// choice.isSelected()
// choice.getMnemonicOffset()
// choice.isHilighted()

						// handle cursor focus
						if (cursorLine == choiceLine && cursorColumn >= choiceColumn && cursorColumn < choiceColumn + choice.getLength())
							element.setAttribute("hasFocus", "true");
						
						// handle attributes and remove old blocks from block factory
						int attribute = removeBlocksHorizontaly(blockFactory, afterBlock, choiceColumn, choiceLine, choice.getLength());
						addAttributes(attribute, element);

						// add the element to container
						myContainer.addOptionalChildren(element);
					}
				} else {
					Engine.logBeans.trace("(treatAndAddComponent) buttons detected but extraction not allowed");
				}
			} else if (type == ACLNptuiSelectionField.TYPE_CHECK) {
				if (bCheckbox) {
					Engine.logBeans.trace("(treatAndAddComponent) checkboxes detected and extraction allowed");
					myContainer.type = "checkboxesPanel";
					
					for (int j =0; j < choices.length; j++) {
						ACLNptuiChoice choice = choices[j];
						int choiceOffset = choice.getScreenOffset();

						Element element = dom.createElement("item");
	    	        	element.setAttribute("name", "untitled");
	    	        	element.setAttribute("type", "checkbox");
	    	        	
	    	        	Text textNode = dom.createTextNode(choice.getText());
				        element.appendChild(textNode);
						
				        int choiceLine, choiceColumn;
				        if (choiceOffset % screenWidth != 0) {
				        	choiceLine = choiceOffset / screenWidth;
				        	choiceColumn = choiceOffset % screenWidth;
		            	} else {
		            		choiceLine = (choiceOffset / screenWidth) - 1;
		            		choiceColumn = 0;
		            	}
				        element.setAttribute("line", "" + choiceLine);
	            		element.setAttribute("column", "" + choiceColumn);
	            		
	            		element.setAttribute("checked", "" + choice.isSelected());
						
// choices[j].getLength()
// choices[j].getMnemonicOffset()
// choices[j].isAvailable()
// choices[j].isHilighted()
	    	        	
	            		// handle cursor focus
						if (cursorLine == choiceLine && cursorColumn >= choiceColumn && cursorColumn < choiceColumn + choice.getLength())
							element.setAttribute("hasFocus", "true");
						
						// handle attributes and remove old blocks from block factory
						int attribute = removeBlocksHorizontaly(blockFactory, afterBlock, choiceColumn, choiceLine, choice.getLength());
						addAttributes(attribute, element);
						
						// add the element to container
						myContainer.addOptionalChildren(element);
					}
				} else {
					Engine.logBeans.trace("(treatAndAddComponent) checkboxes detected but extraction not allowed");
				}
			} else if (type == ACLNptuiSelectionField.TYPE_MENU) {
				if (bMenu) {
					Engine.logBeans.trace("(treatAndAddComponent) menu detected and extraction allowed");
					myContainer.type = "menu";
					
					for (int j =0; j < choices.length; j++) {
						ACLNptuiChoice choice = choices[j];
						int choiceOffset = choice.getScreenOffset();
						
						Element element = dom.createElement("item");
						element.setAttribute("name", "untitled");
	    	        	element.setAttribute("type", "menuItem");
	    	        	element.setAttribute("value", choice.getText());
	    	        	
	    	        	int choiceLine, choiceColumn;
				        if (choiceOffset % screenWidth != 0) {
				        	choiceLine = choiceOffset / screenWidth;
				        	choiceColumn = choiceOffset % screenWidth;
		            	} else {
		            		choiceLine = (choiceOffset / screenWidth) - 1;
		            		choiceColumn = 0;
		            	}
				        element.setAttribute("line", "" + choiceLine);
	            		element.setAttribute("column", "" + choiceColumn);
						
	            		element.setAttribute("selected", "" + choice.isSelected());
						element.setAttribute("available", "" + choice.isAvailable());
						element.setAttribute("hilighted", "" + choice.isHilighted());

// choices[j].getLength()
// choices[j].getMnemonicOffset()

						// handle cursor focus
						if (cursorLine == choiceLine && cursorColumn >= choiceColumn && cursorColumn < choiceColumn + choice.getLength())
							element.setAttribute("hasFocus", "true");
						
						// handle attributes and remove old blocks from block factory
	    	        	int attribute = removeBlocksHorizontaly(blockFactory, afterBlock, choiceColumn, choiceLine, choice.getLength());
	    	        	addAttributes(attribute, element);
	    	        	
	    	        	// add the element to container
	    	        	myContainer.addOptionalChildren(element);
					}
				} else {
					Engine.logBeans.trace("(treatAndAddComponent) menu detected but extraction not allowed");
				}
			} else if (type == ACLNptuiSelectionField.TYPE_RADIO) {
				if (bRadio) {
					Engine.logBeans.trace("(treatAndAddComponent) radio buttons detected and extraction allowed");
					myContainer.type = "radioPanel";
	            				
					for (int j =0; j < choices.length; j++) {
						ACLNptuiChoice choice = choices[j];
						int choiceOffset = choice.getScreenOffset();
						
						Element element = dom.createElement("item");
						element.setAttribute("name", "untitled");
	    	        	element.setAttribute("type", "radio");
	    	        	element.setAttribute ("value", 	choice.getText());
	    	        	
	    	        	int choiceLine, choiceColumn;
				        if (choiceOffset % screenWidth != 0) {
				        	choiceLine = choiceOffset / screenWidth;
				        	choiceColumn = choiceOffset % screenWidth;
		            	} else {
		            		choiceLine = (choiceOffset / screenWidth) - 1;
		            		choiceColumn = 0;
		            	}
				        element.setAttribute("line", "" + choiceLine);
	            		element.setAttribute("column", "" + choiceColumn);
						
						element.setAttribute ("selected", "" + choice.isSelected());

// choices[j].getLength()
// choices[j].getMnemonicOffset()
// choices[j].isAvailable()
// choices[j].isHilighted()
	    	        	
						// handle cursor focus
						if (cursorLine == choiceLine && cursorColumn >= choiceColumn && cursorColumn < choiceColumn + choice.getLength())
							element.setAttribute("hasFocus", "true");
						
						// handle attributes and remove old blocks from block factory
	    	        	int attribute = removeBlocksHorizontaly(blockFactory, afterBlock, choiceColumn, choiceLine, choice.getLength());
						addAttributes(attribute, element);
						
						// add the element to container
						myContainer.addOptionalChildren(element);
					}
				} else {
					Engine.logBeans.trace("(treatAndAddComponent) radio buttons detected but extraction not allowed");
				}
			} else {
				// type is unknown or not handled
				Engine.logBeans.trace("(treatAndAddComponent) Type is unknown or not handled");
				return;
			}
			
			// if the children have been added (if the boolean was true)
			if (myContainer.hasOptionalChildren()) {
				// add generated blocks
				if (parent == null) {
					blockFactory.insertBlock(myContainer, afterBlock);
				} else {
					// SelectionField in window
					parent.addOptionalChildren(myContainer);
				}
			}
		}
	}

    /**
     * Remove blocks horizontaly, from a column/line position, and horizontaly on a length, from the block factory.
     * 
     * @param blockFactory : the block factory
     * @param blockBefore : the block just before the blocks to delete in the block factory, search begins from this block. 
     * @param column : the column from which delete blocks
     * @param line : the line from which delete blocks
     * @param length : the length on which delete blocks
     */
    private int removeBlocksHorizontaly (	BlockFactory blockFactory, Block blockBefore, 
    										int column, int line, int length) {
    	if (blockBefore == null) {
    		// the block to delete is the first of the block factory (no block before)
    		// set blockBefore to the block factory's first block
    		blockBefore = blockFactory.getFirstBlock();
    	}
    	Block tmpBlock = blockBefore;
    	// blockBefore is not the first block to remove, 
    	// advance in blockFactory to find the first block to remove
    	while ( tmpBlock != null 
    		&&	(tmpBlock.line < line // while tmpBlock is on a line inferior to good line 
    		|| (tmpBlock.line == line && tmpBlock.column < column)) ) { // or blockTmp is on the good line but has a column inferior to good column
    		tmpBlock = blockFactory.getNextBlock(tmpBlock);
    	}
    	
    	// if no block in the block factory corresponds to the treated item
    	// or if first block to remove is not on the good line 
    	// or if the block to remove is a panel => exit
    	if (tmpBlock == null || tmpBlock.line != line || tmpBlock.type.equals("panel")) {
    		return -1;
    	}
    	
    	// save the attribute value to return before removing blocks
    	int attribute = tmpBlock.attribute;
    	
    	Block blockToRemove = null;
    	int maxColumn = column + length - 1;
    	while (tmpBlock != null
    		&&	tmpBlock.line == line // while blockTmp is on the good line
    		&& tmpBlock.column >= column // and tmpBlock column is superior or equal to good column
    		&& tmpBlock.column + tmpBlock.length - 1 <= maxColumn ) { // and column of last char of tmpBlock is inferior to maxColumn 
    		blockToRemove = tmpBlock;
    		tmpBlock = blockFactory.getNextBlock(blockToRemove);
    		blockFactory.removeBlock(blockToRemove);
    	}
	
    	return attribute;
    }
    
    /**
     * Remove blocks verticaly, from a column/line position and verticaly on a height, from the block factory.
     * 
     * @param blockFactory : the block factory
     * @param blockBefore : the block just before the blocks to delete in the block factory, search begins from this block. 
     * @param column : the column from which delete blocks
     * @param line : the line from which delete blocks
     * @param heigth : the height on which delete blocks
     */
    private void removeBlocksVerticaly (BlockFactory blockFactory, Block blockBefore, 
    									int column, int line, int height) {
    	if (blockBefore == null) {
    		// the block to delete is the first of the block factory (no block before)
    		// set blockBefore to the block factory's first block
    		blockBefore = blockFactory.getFirstBlock();
    	}
    	Block tmpBlock = blockBefore;
    	// blockBefore is not the first block to remove, 
    	// advance in blockFactory to find the first block to remove
    	while ( tmpBlock.line < line // while tmpBlock is on a line inferior to good line 
    		|| (tmpBlock.line == line && tmpBlock.column < column) ) { // or blockTmp is on the good line but has a column inferior to good column
    		tmpBlock = blockFactory.getNextBlock(tmpBlock);
    	}
    	
    	// if first block to remove is not on the good line => exit
    	if (tmpBlock.line == line) {
	    	Block blockToRemove = null;
	    	int maxLine = line + height - 1;
	    	while (tmpBlock != null
	    		&&	tmpBlock.column == column // while blockTmp is on the good column
	    		&& tmpBlock.line >= line // and tmpBlock line is superior or equal to good line
	    		&& tmpBlock.line <= maxLine ) { // and tmpBlock line is inferior to maxLine 
	    		blockToRemove = tmpBlock;
	    		tmpBlock = blockFactory.getSouthBlock(blockToRemove);
	    		blockFactory.removeBlock(blockToRemove);
	    	}
    	}
    }
    
    /**
     * Remove blocks corresponding to a window, described by coordinates, from the block factory.
     * Copy blocks inside the window to the myPanel block, remove definitively blocks from the window border.
     * 
     * @param blockFactory : the block factory
     * @param blockBefore : the block just before the blocks corresponding to the window in the block factory, search begins from this block. 
     * @param column : the column of the window's top left corner
     * @param line : the line of the window's top left corner
     * @param width : the window's width
     * @param heigth : the window's height
     * @param myPanel : the block representing the window
     * @param dom : the DOM
     */
    private void removeBlocksFromWindow (BlockFactory blockFactory, Block blockBefore, 
    									int column, int line, int width, int heigth,
    									Block myPanel, Document dom) {
    	if (blockBefore == null) {
    		// the block to delete is the first of the block factory (no block before)
    		// set blockBefore to the block factory's first block
    		blockBefore = blockFactory.getFirstBlock();
    	}
    	Block curBlock = blockBefore;
    	Block blockToRemove = null;
    	XMLRectangle laScreenZone = new XMLRectangle(column, line, width, heigth);
    	do {
			if (!curBlock.type.equals("panel")) {
				if (isBlockInScreenZone(curBlock, laScreenZone)) { // if the block is in the panel
					if (! isBlockInBorder(curBlock, laScreenZone)) { // if the block is not in the border
					
						// clone the block and add the clone to the window
						Block clone = null;
						try {
							clone = (Block) curBlock.clone();
						} catch (CloneNotSupportedException e) {
							Engine.logBeans.trace("Block from a window not clonable : " + curBlock.toString() + "\n" +
												"Remove it from block factory.");
						}
						if (clone != null)
							myPanel.addOptionalChildren(clone);
					}
					// in both cases (block in the border or not) remove the block from the block factory
					blockToRemove = curBlock;
					curBlock = blockFactory.getPreviousBlock(curBlock);
					blockFactory.removeBlock(blockToRemove);
				}
			}
		} while ((curBlock = blockFactory.getNextBlock(curBlock)) != null);
    }

    /**
	 * Tests if a block is in a screen zone.
	 * 
	 * @param block the block to be tested
	 * @param mySelectionScreenZone the screen zone to be tested
	 *            
	 * @return true if the block is in the zone, false otherwize.
	 */
	private boolean isBlockInScreenZone(Block block, XMLRectangle mySelectionScreenZone) {
		if (mySelectionScreenZone.y != -1 && block.line < mySelectionScreenZone.y)
			return false;
		if (mySelectionScreenZone.x != -1 && block.column < mySelectionScreenZone.x)
			return false;
		if (mySelectionScreenZone.width != -1 
		&& 	block.column + block.length - 1 > mySelectionScreenZone.x + mySelectionScreenZone.width - 1)
			return false;
		if (mySelectionScreenZone.height != -1
				&& block.line > mySelectionScreenZone.y + mySelectionScreenZone.height - 1)
			return false;
		return true;
	}

	/**
	 * Tests if a block is in the border of a screen zone.
	 * 
	 * @param block the block to be tested
	 * @param mySelectionScreenZone the screen zone to be tested
	 * 
	 * @return true if the block is in the border of the zone, false otherwize.
	 */
	private boolean isBlockInBorder(Block block, XMLRectangle mySelectionScreenZone) {
		// if the block is on the top border line
		if (mySelectionScreenZone.y != -1 && block.line == mySelectionScreenZone.y) {
			// if the block column is between left border and right border columns
			if (mySelectionScreenZone.x != -1 && mySelectionScreenZone.width != -1
					&& block.column >= mySelectionScreenZone.x
					&& block.column <= mySelectionScreenZone.x + mySelectionScreenZone.width - 1) {
				return true;	
			}
		}
			
		// if the block is on the left border column
		if (mySelectionScreenZone.x != -1 && block.column == mySelectionScreenZone.x) {
			// if the block line is between top border and bottom border lines
			if (mySelectionScreenZone.y != -1 && mySelectionScreenZone.height != -1
					&& block.line >= mySelectionScreenZone.y
					&& block.line <= mySelectionScreenZone.y + mySelectionScreenZone.height - 1) {
				return true;	
			}
		}
		
		// if the block is on the right border column
		if (mySelectionScreenZone.x != -1 && mySelectionScreenZone.width != -1 
				&& block.column + block.length - 1 == mySelectionScreenZone.x + mySelectionScreenZone.width - 1) {
			// if the block line is between top border and bottom border lines
			if (mySelectionScreenZone.y != -1 && mySelectionScreenZone.height != -1
					&& block.line >= mySelectionScreenZone.y
					&& block.line <= mySelectionScreenZone.y + mySelectionScreenZone.height - 1) {
				return true;	
			}
		}
		
		// if the block is on the bottom border line
		if (mySelectionScreenZone.y != -1 && mySelectionScreenZone.height != -1 
				&& block.line == mySelectionScreenZone.y + mySelectionScreenZone.height - 1) {
			// if the block column is between left border and right border columns
			if (mySelectionScreenZone.x != -1 && mySelectionScreenZone.width != -1
					&& block.column >= mySelectionScreenZone.x
					&& block.column <= mySelectionScreenZone.x + mySelectionScreenZone.width - 1) {
				return true;	
			}
		}
		
		return false;
	}
	
	/**
	 * Add XML attributes, deduced from the int attribute value, to the Element.
	 * @param attribute : int representing the attributes
	 * @param element : the Element to which add the XML attributes.
	 */
	private void addAttributes(int attribute, Element element) {
		// set the good attributes to the element parsing the attribute value
		if ((attribute & iJavelin.AT_INVERT) == 0) {
            if (attributes[Transaction.ATTRIBUTE_REVERSE]) 
            	element.setAttribute("reverse", "false");
            if (attributes[Transaction.ATTRIBUTE_FOREGROUND]) 
            	element.setAttribute("foreground", colors[attribute & iJavelin.AT_INK]);
            if (attributes[Transaction.ATTRIBUTE_BACKGROUND]) 
            	element.setAttribute("background", colors[(attribute & iJavelin.AT_PAPER) >> 3]);
        }
        else {
            if (attributes[Transaction.ATTRIBUTE_REVERSE]) element.setAttribute("reverse", "true");
            if (attributes[Transaction.ATTRIBUTE_FOREGROUND]) element.setAttribute("foreground", colors[(attribute & iJavelin.AT_PAPER) >> 3]);
            if (attributes[Transaction.ATTRIBUTE_BACKGROUND]) element.setAttribute("background", colors[attribute & iJavelin.AT_INK]);
        }

        if (attributes[Transaction.ATTRIBUTE_BLINK]) if ((attribute & iJavelin.AT_BLINK) != 0) element.setAttribute("blink", "true");
        if (attributes[Transaction.ATTRIBUTE_UNDERLINE]) if ((attribute & iJavelin.AT_UNDERLINE) != 0) element.setAttribute("underline", "true");
        if (attributes[Transaction.ATTRIBUTE_INTENSE]) if ((attribute & iJavelin.AT_BOLD) != 0) element.setAttribute("intense", "true");
	}
	
	/**
	 * Sort windows table by z-order.
	 * @param windows : a table of windows.
	 */
//	private void sortWindows (ACLNptuiWindow[] windows) {
//		// TODO 
//	}
	
    public boolean isBCheckbox() {
		return bCheckbox;
	}

	public void setBCheckbox(boolean checkbox) {
		this.bCheckbox = checkbox;
	}

	public boolean isBWindow() {
		return bWindow;
	}

	public void setBWindow(boolean window) {
		bWindow = window;
	}

	public boolean isBChoice() {
		return bChoice;
	}

	public void setBChoice(boolean choice) {
		bChoice = choice;
	}

	public boolean isBScrollBar() {
		return bScrollBar;
	}

	public void setBScrollBar(boolean scrollBar) {
		bScrollBar = scrollBar;
	}

	public boolean isBButton() {
		return bButton;
	}

	public void setBButton(boolean button) {
		bButton = button;
	}

	public boolean isBMenu() {
		return bMenu;
	}

	public void setBMenu(boolean menu) {
		bMenu = menu;
	}

	public boolean isBRadio() {
		return bRadio;
	}

	public void setBRadio(boolean radio) {
		bRadio = radio;
	}

}
