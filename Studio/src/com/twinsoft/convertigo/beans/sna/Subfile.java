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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import com.twinsoft.convertigo.beans.common.Table;
import com.twinsoft.convertigo.beans.common.XMLRectangle;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.JavelinUtils;
import com.twinsoft.twinj.iJavelin;

/**
 * This class constructs a subfile for the SNA Emulator,
 * it extends the com.twinsoft.convertigo.beans.common.Table class.
 * This class checks if a block matches a list keyword (subfile marker string)
 * It finds automatically the sreen zone of the table and constructs automaticly the columns.
 * These informations are passed to the Table::Execute(...) method to construct the table.
 */

public class Subfile extends Table {
    
	private static final long serialVersionUID = 5187400242784350958L;

	/** Holds value of property endString. */
    private String endString = "A suivre...,Fin,+";
       
	/** Holds value of attribute of title row (white on black by default) */
	private int titleRowAttribute = 7;
	
	/** Holds value of attribute  of action line row (blue on black by default) */
	private int actionLineAttribute = 4;
	
	/** Holds value of property endStringAttribute. (white on black by default)*/
    private int endStringAttribute = 7;

    private int subFileDetectionStartLine = 0;
    
    /** size in lines of the title row of the subfile */
    transient int titleHeight = 0;
    
    public Subfile() {
        super();
    }

    public void init(int reason) {
		Engine.logBeans.trace("### Initializing subfile (reason = " + reason + ")...");
			
        if (reason == ExtractionRule.INITIALIZING) {
        	titleHeight = 0;
        }
        super.init(reason);
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
    	// save the screenZone so that we can restore it after the extraction of tableCUA
		XMLRectangle oldScreenZone = getSelectionScreenZone();
		XMLVector<XMLVector<Object>> oldColumns = getColumns();
		
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		Block downRightBlock, topLeftBlock, blockTmp, newBlock, topActionBlock;
		int columnDataLastCharEast, columnDataLastCharWest, dataLastLine;
		int subfileMarkerLine;

		// attributes of the field of action in the table
		int actionLength = 0;
		int actionColumn = 0;
//		int actionColumnAttributes = 0;
		
		boolean actionColumnExists = false;
		Block lastField = null;
		
		boolean screenZoneSet = false;
		
		StringTokenizer st = new StringTokenizer(endString, ",", false);
		ArrayList<String> token = new ArrayList<String>(st.countTokens());
		
		// if screen zone has been set, use it...
		if (oldScreenZone.x != -1 || oldScreenZone.y != -1 ||oldScreenZone.height != -1 ||oldScreenZone.width != -1){
			screenZoneSet = true;
		}
		
		// for each string 
		while (st.hasMoreTokens()) {
			token.add((String) st.nextToken());
		}
		
		try {
			//search of the endString among the blocks of the blockFactory
			if (token.contains(block.getText().trim()) && JavelinUtils.isSameAttribute(block.attribute, endStringAttribute) && block.line >= subFileDetectionStartLine){
				WindowedBlockManager windowedBlockManager = new WindowedBlockManager(blockFactory);
				Engine.logBeans.trace("Searching the end block in a panel.");
				if (!windowedBlockManager.searchPanel(block)) {
					xrs.hasMatched = false;
					xrs.newCurrentBlock = block;
					return xrs;
				}
				
				// this line of code was not commented. I believe this was to prevent SubFile rule to be run twice. I prefer to set the xrs.newCurrentBlock 
				// to the correct value. This also corrects the bug that prevented the delete blocks rule to act on the subfile marker as it was marked
				// as bFinal to true.
				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!
				// !! block.bFinal = true;   !!
				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!
				
				subfileMarkerLine = block.line;
				
				//if endString = "+" -> special case (Non-CUA but we can try to define the subfile) 
				if (block.getText().equals("+")){	
					/** CASE "+" in the last row, in the last column*/
					downRightBlock = block;
					dataLastLine = downRightBlock.line;
					// as '+' is on same line as the alst row of data , add one line to subFileMarkerLine
					subfileMarkerLine++;
					
					// do not include marker block in data
					downRightBlock = windowedBlockManager.getPreviousBlockInPanel(block);
					Engine.logBeans.trace("Down Right Block = " + downRightBlock.getText());
					block.line = block.line + 1;
				}
				//if endString != "+" (most common case)
				else {
					//we look for a block directly above the endString
					downRightBlock = windowedBlockManager.getPreviousBlockInPanel(block);
					Engine.logBeans.trace("Down Right Block = " + downRightBlock.toString());
					while (downRightBlock != null && downRightBlock.line == block.line)
					{
						downRightBlock = windowedBlockManager.getPreviousBlockInPanel(downRightBlock);
						Engine.logBeans.trace("New Down Right Block = " + downRightBlock.toString());
					}			
					dataLastLine = downRightBlock.line;
				}
				//if previous block do not contain any text, we give up creation of the table
				if (downRightBlock.length == 0) 
				{
					Engine.logBeans.info("Give up subfile creation => Down Right Block length is 0 : " + downRightBlock.toString());
					xrs.hasMatched = false;
					xrs.newCurrentBlock = block;
					return xrs;
				}
				
				if ( (block.column + block.getText().length()) <= (downRightBlock.column + downRightBlock.getText().length()) )
					columnDataLastCharEast = downRightBlock.column + downRightBlock.getText().length();
				else
					columnDataLastCharEast = block.column + block.getText().length();
	 			
	 			//we move to west and search for the last block on the line (last row)
				blockTmp = downRightBlock;
				while (windowedBlockManager.getWestBlock(blockTmp) != null 
						&& windowedBlockManager.getWestBlock(blockTmp).line == dataLastLine){
					blockTmp = windowedBlockManager.getWestBlock(blockTmp);
				} 
				Engine.logBeans.trace("last block on the line (west) = " + blockTmp.toString());
				columnDataLastCharWest = blockTmp.column;
				
				// now blockTmp is positionned on last block of data on the west
				//we check if action column exists
				if (blockTmp.type.equalsIgnoreCase("field")){
					actionColumnExists = true;
					
					actionLength = blockTmp.length;
					actionColumn = blockTmp.column;
//					actionColumnAttributes = blockTmp.attribute;
				}
				//maybe action column exists but not on this line
				else {
					List<Block> blockField = windowedBlockManager.getBlocksByType("field");
					
					for (Block blockF : blockField) {
						Engine.logBeans.trace("block field found = " + blockF.toString());
						if (lastField == null) {
							if (blockF.column <=  blockTmp.column && blockF.line <  blockTmp.line) {
								lastField = blockF;
							}
						} else {
							if ( blockF.line > lastField.line || blockF.column > lastField.column ) {
								lastField = blockF;
							}
						}
					}
				}

				//now we are looking for the title row of the table, but if there is
				//an action column, it can happen that it doesn't have a title
				if (actionColumnExists){
					//if we are in the action column
					Engine.logBeans.trace("+++ action column exists +++");
					if (columnDataLastCharWest > blockTmp.column){
						columnDataLastCharWest = blockTmp.column;
					}
					topLeftBlock = null;
				} else {
					// we are in a data column
					Engine.logBeans.trace("Searching title row in data column : " + blockTmp.getText());
					
					while ((windowedBlockManager.getNorthBlock(blockTmp) != null )
							&& (!JavelinUtils.isSameAttribute(windowedBlockManager.getNorthBlock(blockTmp).attribute, titleRowAttribute))) {
						blockTmp = windowedBlockManager.getNorthBlock(blockTmp);
						Engine.logBeans.trace("Moving to north block : " + blockTmp.getText());
					}
										
					// maybe an action column exists
					// check if the last field is in the data
					if (lastField == null || lastField.line < blockTmp.line) {
						Engine.logBeans.trace("---actionColumnDoesNotExists");
						blockTmp = windowedBlockManager.getNorthBlock(blockTmp);
						while (windowedBlockManager.getWestBlock(blockTmp) != null) {
							blockTmp = windowedBlockManager.getWestBlock(blockTmp);
							Engine.logBeans.trace("Moving to west block : " + blockTmp.getText());
							if (columnDataLastCharWest > blockTmp.column){
								columnDataLastCharWest = blockTmp.column;
							}
						}
						topLeftBlock = blockTmp;
						Engine.logBeans.trace("Top Left Block = " + (topLeftBlock == null ? "null" : topLeftBlock.getText()) );
					} else {
						Engine.logBeans.trace("+++ action column exists +++");
						actionColumnExists = true;
						if (columnDataLastCharWest > lastField.column){
							columnDataLastCharWest = lastField.column;
						}
						blockTmp = lastField;
						topLeftBlock = null;
					}
				}
				
				if (actionColumnExists) {
					//we search the bottom of the title rows (but maybe action column doesn't have a title)
					while ( (windowedBlockManager.getNorthBlock(blockTmp) != null ) // while north block of blocktmp not null
							&& (!JavelinUtils.isSameAttribute(windowedBlockManager.getNorthBlock(blockTmp).attribute, titleRowAttribute)) // and north block of blocktmp is not same attribute as titlerow
							&& (!JavelinUtils.isSameAttribute(windowedBlockManager.getNorthBlock(blockTmp).attribute, actionLineAttribute))){ // and north block of blocktmp is not same attribute as actionsline
						Engine.logBeans.trace("Wrong title line found (" + blockTmp.line+ ") ... keep searching...");
						blockTmp = windowedBlockManager.getNorthBlock(blockTmp);
					}
					// blocktmp is the last top data block of the first column
					
					// is north block of blocktmp the first title block of first column ?
					if ((windowedBlockManager.getNorthBlock(blockTmp) != null ) // if north block of blocktmp not null
					 && (JavelinUtils.isSameAttribute(windowedBlockManager.getNorthBlock(blockTmp).attribute, titleRowAttribute)) // and north block of blocktmp has titlerow attribute
					 && (goodTitleLine(windowedBlockManager, windowedBlockManager.getNorthBlock(blockTmp)))) { // and north bolck of blocktmp is on a good title line
							// blocktmp is the last top data block of the first column 
							Engine.logBeans.trace("Top of the action column= " + blockTmp.toString());
							topActionBlock = blockTmp;
					} else {
						 // action column is not filled until top, blocktmp is not the last top line of data
						// or action column doesn't have a title
						Block actiontop = blockTmp;
						
						// Searching a title block
						blockTmp = windowedBlockManager.getEastBlock(blockTmp);
						blockTmp = windowedBlockManager.getNorthBlock(blockTmp);
						
						// look for a title block of the second column
						while ( windowedBlockManager.getNorthBlock(blockTmp) != null // while north block of blocktmp not null
							&& JavelinUtils.isSameAttribute(windowedBlockManager.getNorthBlock(blockTmp).attribute, titleRowAttribute) // and north block of blocktmp is the same attribute as titlerow 
						) { 
							Engine.logBeans.trace("Wrong title line found (" + blockTmp.line+ ") ... keep searching...");
							blockTmp = windowedBlockManager.getNorthBlock(blockTmp);
						}
						// blocktmp is a title block of the second column
						
						if (blockTmp == null) {
							Engine.logBeans.info("Give up subfile creation => Looking for title block : blockTmp is null, no more block to test.");
							xrs.hasMatched = false;
							xrs.newCurrentBlock = block;
							return xrs;
						} else {
							if (JavelinUtils.isSameAttribute(blockTmp.attribute, titleRowAttribute) // if blocktmp has same attribute as titlerow
								&& (goodTitleLine(windowedBlockManager, blockTmp))) { // and blocktmp is on a good title line
								Engine.logBeans.trace("Top of the action column= " + actiontop.toString());
								topActionBlock = actiontop;
							} else {
								Engine.logBeans.info("Give up subfile creation => blockTmp is not of title attribute or is not on a good title line : "+ blockTmp.toString());
								xrs.hasMatched = false;
								xrs.newCurrentBlock = block;
								return xrs;
							}
						}
					}
					
					//go one column to the east of top action line block, and one block to the north
					blockTmp = windowedBlockManager.getEastBlock(topActionBlock);
					blockTmp = windowedBlockManager.getNorthBlock(blockTmp);
					// block tmp is the first down title block of second column
					// or another column if there is no data on the east of top action line block
					Block firstTitleBlock = blockTmp;
					
					// go to the first row of the title rows (if many)
					while (windowedBlockManager.getNorthBlock(blockTmp) != null 
							&& JavelinUtils.isSameAttribute(windowedBlockManager.getNorthBlock(blockTmp).attribute, titleRowAttribute)
							&& blockTmp.line - windowedBlockManager.getNorthBlock(blockTmp).line == 1)
						blockTmp = windowedBlockManager.getNorthBlock(blockTmp);
					
					// blocktmp is the first line of second column title (or another column)
					
					// we check if action column has a title
					while (topActionBlock != null // while no more block is on the top
							&& !JavelinUtils.isSameAttribute(topActionBlock.attribute,titleRowAttribute) // and the title row is not found
							&& !JavelinUtils.isSameAttribute(topActionBlock.attribute,actionLineAttribute)) // and an action line is not found (we suppose that the title row must be under the actions line) 
						topActionBlock = windowedBlockManager.getNorthBlock(topActionBlock);

					if (topActionBlock != null && JavelinUtils.isSameAttribute(topActionBlock.attribute,titleRowAttribute)){
						Engine.logBeans.trace("Action column has a title");
						topLeftBlock = topActionBlock;
					} else { // we arrived on the top of the screen or on the actions line
						Engine.logBeans.trace("Action column does not have a title");
						Engine.logBeans.trace("Value of the first block of the title row : "+ blockTmp.toString());
						newBlock = new Block();
						newBlock.column = actionColumn;
						newBlock.length = actionLength;
						newBlock.line = firstTitleBlock.line;
						newBlock.attribute = titleRowAttribute;
						newBlock.setText("A");
						newBlock.bRender = false;
						Engine.logBeans.trace("Creation of the title block of the action column : " + newBlock.toString());
						
						// maybe firstTitleBlock is not the second column title, look to the west to see if there are other titles
						while (windowedBlockManager.getWestBlock(firstTitleBlock) != null)
							firstTitleBlock = windowedBlockManager.getWestBlock(firstTitleBlock);
						// now firstTitleBlock is the second column title, can insert the created block after its preceding block
						Engine.logBeans.trace("Insert new title block after : " + windowedBlockManager.getPreviousBlock(firstTitleBlock).toString());
						windowedBlockManager.insertBlock(newBlock, windowedBlockManager.getPreviousBlock(firstTitleBlock));
						Engine.logBeans.trace("Next of the new Block : " + windowedBlockManager.getNextBlock(newBlock).toString());
						//if getNextBlock(newBlock) and newBlock are not on the same line, we've inserted the new block in the wrong place
						if (windowedBlockManager.getNextBlock(newBlock).line != newBlock.line){
							Engine.logBeans.trace("The new block and its next block are not on the same line. Trying to insert it on the right place.");
							Block newBlockTmp = newBlock;
							while (windowedBlockManager.getNextBlock(newBlockTmp).line != newBlock.line ){
								newBlockTmp = windowedBlockManager.getNextBlock(newBlockTmp);
							}
							Engine.logBeans.trace("The previous block of the right place is : "+newBlockTmp.toString());
							windowedBlockManager.removeBlock(newBlock);
							windowedBlockManager.insertBlock(newBlock, newBlockTmp);
		
						}
						topLeftBlock = newBlock;
					}
				}
				
				// At this point : we found the coordinates of the screenZone of the table. So now we define the screen zone of the table
				// Engine.logBeans.trace("---topLeftBlock.text="+ topLeftBlock.getText());
				// Engine.logBeans.trace("---topLeftBlock.attribute="+ topLeftBlock.attribute);
				if (topLeftBlock != null && downRightBlock != null && goodTitleLine(windowedBlockManager, topLeftBlock)
				    && topLeftBlock.line != 0 && JavelinUtils.isSameAttribute(topLeftBlock.attribute, titleRowAttribute)) {
				
					// We search the first row of data, to give to super.execute method
					Block topLeftDataRow = topLeftBlock;
					while (windowedBlockManager.getNextBlock(topLeftDataRow) != null && windowedBlockManager.getNextBlock(topLeftDataRow).line == topLeftBlock.line) {
						topLeftDataRow = windowedBlockManager.getNextBlock(topLeftDataRow);
					}
					topLeftDataRow = windowedBlockManager.getNextBlock(topLeftDataRow);
					
					
					// Set line of actions and finding columns of the table
					if (actionColumnExists) {
						setColumnSelection(0);
						int checkAction = extractActionsLine(windowedBlockManager, topLeftBlock);
						if (checkAction == 0)
							setColumnSelection(-1);
						else 
							setLineActions(checkAction);
					} 
					else 
						setColumnSelection(-1);
					
					columnTitleWest = javelin.getScreenWidth();
					columnTitleEast = 0;

					setColumns(createTitleColumns(windowedBlockManager, topLeftBlock, downRightBlock.line, columnDataLastCharEast));
					Engine.logBeans.trace("##### Columns: " + getColumns());

					XMLRectangle mySelectionScreenZone = new XMLRectangle(Math.min(columnDataLastCharWest, columnTitleWest), topLeftBlock.line + 1, (Math.max(columnDataLastCharEast, columnTitleEast) - topLeftBlock.column) + 1, subfileMarkerLine - topLeftBlock.line -1);
					if (screenZoneSet)
						setSelectionScreenZone(oldScreenZone);
					else
						setSelectionScreenZone(mySelectionScreenZone);

					// set the offset to 1 this will result in the table to be shifted up one line 
					if (actionColumnExists)
						this.setOffset(2);
					else
						this.setOffset(1);
					
					// removing the subfile blocks from their panel.
					Block panel = windowedBlockManager.getPanel();
					if (panel != null) {
						Engine.logBeans.trace("SelectionScreenZone: " + getSelectionScreenZone().toString());
						
						for(Block panelChildBlock : new ArrayList<Block>(panel.getOptionalBlockChildren())){
							Engine.logBeans.trace("Trying to remove from Panel: " + panelChildBlock.toString());
							if (getSelectionScreenZone().contains(panelChildBlock.column, panelChildBlock.line, panelChildBlock.length, 1) || // data line
								(JavelinUtils.isSameAttribute(panelChildBlock.attribute, actionLineAttribute) 
								&& panelChildBlock.line < getSelectionScreenZone().y 
								&& panelChildBlock.line >= getSelectionScreenZone().y + getLineActions())) { // action line
										panel.removeOptionalChildren(panelChildBlock);
										Engine.logBeans.trace("Removed.");
							}
						}
					}

					Engine.logBeans.trace("columnDataLastCharWest=" + columnDataLastCharWest + "; topLeftBlock-line=" + topLeftBlock.line + "; downright-col= " + columnDataLastCharEast + "; downright-line= " + downRightBlock.line);		
					xrs = super.execute(javelin, topLeftDataRow, blockFactory, dom);
					table.setOptionalAttribute("subfile", "true");
					table.setOptionalAttribute("titleheight", Integer.toString(titleHeight+1));

					setSelectionScreenZone(oldScreenZone);
					setColumns(oldColumns);
					
					// block still contains the end of sub file marker .. set the newCurrentBlock to this block
					xrs.newCurrentBlock = block;
					return xrs;
				} else {
					Engine.logBeans.info("Give up subfile creation => Down Right Block is null              				: "+ downRightBlock.toString());
					Engine.logBeans.info("                         OR Top Left Block is null or not of title attribute 	: "+ (topLeftBlock == null ? "null" : topLeftBlock.toString()) );
					xrs.hasMatched = false;
					xrs.newCurrentBlock = block;
					return xrs;
				}
			} else {
				xrs.hasMatched = false;
				xrs.newCurrentBlock = block;
				return xrs;
			}
		}
		catch(NullPointerException e){
			Engine.logBeans.error("NullPointerException while executing the Subfile extraction rule", e);
			xrs.hasMatched = false;
			xrs.newCurrentBlock = blockFactory.getLastBlock();
			return xrs;
		}
	}
    
	private int extractActionsLine(WindowedBlockManager windowedBlockManager, Block table){
		Block blockTmp;
		int relativeLineActions = 0;
		boolean foundToken = false;
		
		Engine.logBeans.trace("Entering extractActionsLine");

		//search of lineActions
		blockTmp = windowedBlockManager.getPreviousBlock(table);
		while (blockTmp != null && !JavelinUtils.isSameAttribute(blockTmp.attribute, actionLineAttribute)){
			Engine.logBeans.trace("AAA " + blockTmp.getText());
			blockTmp = windowedBlockManager.getPreviousBlock(blockTmp);
		}
		
		// We've found a block of the line action or there is no action line 
		if (blockTmp != null) {
			String separatorCharsForTokens = getSeparatorCharsForTokens();
			if (new StringTokenizer( blockTmp.getText().trim(), separatorCharsForTokens, false).countTokens() >= 2) {
				Engine.logBeans.trace("Block : " + blockTmp.getText().trim() + " matches token;");
				foundToken = true;
			}
		
		    while ( windowedBlockManager.getPreviousBlock(blockTmp) != null 
		    	&&  JavelinUtils.isSameAttribute(windowedBlockManager.getPreviousBlock(blockTmp).attribute, actionLineAttribute) 
		    	&& (new StringTokenizer( windowedBlockManager.getPreviousBlock(blockTmp).getText().trim(), separatorCharsForTokens, false)).countTokens() >= 2)
		    	{
					blockTmp = windowedBlockManager.getPreviousBlock(blockTmp);
					Engine.logBeans.trace("Block : " + blockTmp.getText().trim() + " matches token;");
					foundToken = true;
				}		
		}	
		if (blockTmp == null) {
			relativeLineActions = 0;
		} else if (foundToken){
			relativeLineActions = blockTmp.line - (table.line + 1);
		} else {
			relativeLineActions = 0;
		}
		Engine.logBeans.trace("relativeLineActions : " + relativeLineActions);
		return relativeLineActions;
	}
	
	private boolean goodTitleLine(WindowedBlockManager windowedBlockManager, Block block){
		Engine.logBeans.trace("goodTitleLine enter with : " + block.getText() );
		while (block != null && JavelinUtils.isSameAttribute(block.attribute, titleRowAttribute)) {
			Engine.logBeans.trace("goodTitleLine : " + block.getText());
			block = windowedBlockManager.getEastBlock(block);
		}
		if (block == null){
			return true;
		}
		else 
			return (JavelinUtils.isSameAttribute(block.attribute, titleRowAttribute));
	}
	
	int columnTitleWest, columnTitleEast;
	
	private XMLVector<XMLVector<Object>> createTitleColumns(WindowedBlockManager windowedBlockManager, Block block, int bottomScreenZone, int columnDataLastCharEast) {		
		XMLVector<XMLVector<Object>> columns = new XMLVector<XMLVector<Object>>();
		XMLVector<Object> oldCol = null;
		Block curBlock;
//		int curLine = block.line;
		ArrayList<Block> blockToRemove = new ArrayList<Block>();
		int bottomLineOfTitle = block.line;

		int firstLine = getSelectionScreenZone().y;
		
		int nbUp = 0;
   
   		curBlock = block;
		Engine.logBeans.trace("Entering createTitleColumns() with " + curBlock.toString());

		columnsBlocks = new Vector<Block>();
		
		// For each column of the table
		while (curBlock != null) {
			if (!canBlockBeSelected(curBlock)) {
				curBlock = windowedBlockManager.getNextBlock(curBlock);
				continue;
			}
			Block topBlock = curBlock;
			
			// We go up to the first block of the title rows. (The top block of this column can be higher than the top block of the first column.) 
			while (windowedBlockManager.getNorthBlock(topBlock) != null 
					&& (JavelinUtils.isSameAttribute(windowedBlockManager.getNorthBlock(topBlock).attribute, titleRowAttribute)) 
					&& (windowedBlockManager.getNorthBlock(topBlock).line == topBlock.line - 1)) {
				topBlock = windowedBlockManager.getNorthBlock(topBlock);
				nbUp++;
				
				/* update the titleSize variable representing the size of the title row */
				if (nbUp > titleHeight)
					titleHeight = nbUp;
			}
			Engine.logBeans.trace("Top block is " + topBlock);
			
			// Now we append the text of the other blocks of the title rows
			String title = "";
			Block blockTmp = topBlock;
			blockTmp.setText(blockTmp.getText() + " ");
//			int maxLine = blockTmp.line;
			while ((blockTmp != null) && (JavelinUtils.isSameAttribute(blockTmp.attribute, titleRowAttribute))) {
				if (title == "")
					title = blockTmp.getText();
				else {
					title = title + " " + blockTmp.getText();
				}

				if (!columnsBlocks.contains(blockTmp)) columnsBlocks.add(blockTmp);

				columnTitleWest = Math.min(columnTitleWest, block.column);
				columnTitleEast = Math.max(columnTitleEast, block.column + block.length);
				
				blockToRemove.add(blockTmp);
				
				if (windowedBlockManager.getSouthBlock(blockTmp) != null
				&& windowedBlockManager.getSouthBlock(blockTmp).line == blockTmp.line + 1)
					blockTmp = windowedBlockManager.getSouthBlock(blockTmp);
				else
					blockTmp = null;  
			}

			// creation of the column vector
			XMLVector<Object> col = new XMLVector<Object>();
			col.add(new String(title));
			col.add(new Integer(curBlock.column));
			col.add(new Integer(columnDataLastCharEast));
			col.add(new Integer(firstLine - 1));
			col.add(new Integer(0));
			columns.add(col);
			
			// Update the endColumn of the previous column
			if (oldCol != null) {
				oldCol.set(2, new Integer(curBlock.column - 1));
				Engine.logBeans.trace("Updating the endColumn of the previous column to: " + (curBlock.column - 1));
			}
			
			// Save the col in order to update the endColumn with the begin column - 1 of the next column  
			oldCol = col;
			
			Engine.logBeans.trace("Table defining '" + title + "' from column: " + curBlock.column + " to column: " + columnDataLastCharEast);

			// We choose the next block
			blockTmp = topBlock;
			while (blockTmp != null
					&& (windowedBlockManager.getSouthBlock(blockTmp) != null
					&& JavelinUtils.isSameAttribute(blockTmp.attribute, titleRowAttribute)
					&& (windowedBlockManager.getSouthBlock(blockTmp).line == blockTmp.line + 1)
					&& (blockTmp.line != bottomLineOfTitle))) {
				if (windowedBlockManager.getEastBlock(blockTmp) != null)
					if (curBlock == null || (windowedBlockManager.getEastBlock(blockTmp).column < curBlock.column))
						curBlock = windowedBlockManager.getEastBlock(blockTmp);
				blockTmp = windowedBlockManager.getSouthBlock(blockTmp);
			}
			curBlock = windowedBlockManager.getEastBlock(blockTmp);
			
			// We remove title blocks (and the ones above last row of title) now that they are defined
			Block blockRem;
			Block panel = windowedBlockManager.getPanel();
			Engine.logBeans.trace("There are " + blockToRemove.size() + " blocks to remove.");
			while (!blockToRemove.isEmpty()) {
				blockRem = blockToRemove.get(0);
				Engine.logBeans.trace("Remove of title block = " + blockRem.getText());
				windowedBlockManager.removeBlock(blockRem);

				// Removing the block from the optional children of the panel containing
				// the subfile (if exists).
				if (panel != null)
					panel.removeOptionalChildren(blockRem);

				blockToRemove.remove(0);
			}
			
			nbUp = 0;
			Engine.logBeans.trace("The next block to be analysed is " + curBlock + "\n");
		}
		return columns;
	}

    /** Getter for property endString.
     * @return Value of property endString.
     */
    public String getEndString() {
        return this.endString;
    }
    
    /** Setter for property endString.
     * @param endString New value of property endString.
     */
    public void setEndString(String endString) {
        this.endString = endString;
    }

    /** Getter for property titleRowAttribute.
     * @return Value of property titleRowAttribute.
     */
    public int getTitleRowAttribute() {
        return this.titleRowAttribute;
    }    
    
    /** Setter for property titleRowAttribute.
     * @param titleRowAttribute New value of property titleRowAttribute.
     */
    public void setTitleRowAttribute(int titleRowAttribute) {
        this.titleRowAttribute = titleRowAttribute;
    }
    
    /** Getter for property actionLineAttribute.
     * @return Value of property actionLineAttribute.
     */
    public int getActionLineAttribute() {
        return this.actionLineAttribute;
    }
    
    /** Setter for property actionLineAttribute.
     * @param actionLineAttribute New value of property actionLineAttribute.
     */
    public void setActionLineAttribute(int actionLineAttribute) {
        this.actionLineAttribute = actionLineAttribute;
    }
    
    /** Getter for property endStringAttribute.
     * @return Value of property endStringAttribute.
     */
    public int getEndStringAttribute() {
        return this.endStringAttribute;
    }
    
    /** Setter for property endStringAttribute.
     * @param endStringAttribute New value of property endStringAttribute.
     */
    public void setEndStringAttribute(int endStringAttribute) {
        this.endStringAttribute = endStringAttribute;
    }

	public int getSubFileDetectionStartLine() {
		return subFileDetectionStartLine;
	}

	public void setSubFileDetectionStartLine(int subFileDetectionStartLine) {
		this.subFileDetectionStartLine = subFileDetectionStartLine;
	}


}
