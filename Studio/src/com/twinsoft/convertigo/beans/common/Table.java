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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.twinj.iJavelin;
public class Table extends JavelinMashupEventExtractionRule {

	private static final long serialVersionUID = 4145737008284937675L;

	transient List<Block> colonDispatch;
	transient Block accumulatedData;
	transient int indexRow;
	transient int firstLine;
	transient int max_index;
	transient int numPage;
	transient Block actionsDefined;
	transient int titleH;

	class TableRow implements Serializable {
		private static final long serialVersionUID = -2949907612433661806L;

		String title = "";
		int startRow = 0;
		int endRow = 0;

		public TableRow(String title, int startRow, int endRow) {
			this.title = title;
			this.startRow = startRow;
			this.endRow = endRow;
		}
	}

	class TableColumn implements Serializable {
		private static final long serialVersionUID = -6384167189301636639L;

		String title = "";
		int startColumn = 0;
		int endColumn = 0;
		int lineIndex = 0;

		public TableColumn(String title, int startColumn, int endColumn, int lineIndex) {
			this.title = title;
			this.startColumn = startColumn;
			this.endColumn = endColumn;
			this.lineIndex = lineIndex;
		}
	}

	/** Holds value of property columns. */
	private XMLVector<TableColumn> columns = new XMLVector<TableColumn>();

	/** Holds value of property columns. */
	protected transient List<Block> columnsBlocks = null;

	/** Holds value of property actions. */
	private XMLVector<XMLVector<String>> actions = new XMLVector<XMLVector<String>>();

	/** Holds value of property lineActions. */
	private int lineActions = 0;

	/** Holds value of property columnSelection. */
	private int columnSelection = -1;

	private String tagName = "table";

	/** Holds value of property offset. */
	private int offset = 0;

	/** Holds value of property startPattern. */
	private String startPattern = "";

	/** Holds value of property endPattern. */
	private String endPattern = "";

	/** Holds value of property separatorChars. */
	private String separatorChars = "";

	/** Holds value of property separatorCharsForTokens. */
	private String separatorCharsForTokens = "=";

	/** Holds value of property doNotIncludeTitles. */
	private boolean doNotIncludeTitles = false;

	/** Holds value of property doNotAccumulate. */
	private boolean doNotAccumulate = false;

	/** Holds value of property autoValidate. */
	private boolean autoValidate = false;

	/** Holds value of property removeActionLines. (true by default)*/
	private boolean removeActionLines = true;

	/** Holds value of property keepEmptyLines. (false by default)*/
	private boolean keepEmptyLines = false;

	/** Holds value of property resize. */
	private int resize = 0;


	/**
	 * This will be called at each beginning of a transaction
	 */
	@Override
	public void init(int reason) {
		// We set our accumulated data Element to null. Other calls to the rule within
		// the same transaction will cause data to be accumulated in the same Table
		Engine.logBeans.trace("### Initializing table (reason = " + reason + ")...");

		if (reason == ExtractionRule.INITIALIZING) {
			accumulatedData = null;
			indexRow = 0;
			numPage = 1;
			titleH = 1;
		}
	}

	public Table() {
		super();
	}

	private void dispatchBlockInColumn(Block block) {
		Block blockTmp;
		int overlapFactor, bestOverlapFactor = 0, indexRightColumn = 0;
		int relLineInTable;

		GenericUtils.setListSize(colonDispatch, columns.size());

		max_index = getMax_index();
		int nbLineForARecord = max_index + 1;
		int i = 0;
		for (TableColumn tc : columns) {
			Engine.logBeans.trace("");
			overlapFactor = getOverlapFactor(block, tc);
			relLineInTable = block.line - firstLine;

			Engine.logBeans.trace("OverlapFactor: "+ overlapFactor + " for this block " + block.getText() + " and the column " + tc.title);
			Engine.logBeans.trace("BLOCK = " + block.getText() + ", relLineInTable=" + relLineInTable + ", nbLineForARecord=" + nbLineForARecord + ", (relLineInTable % nbLineForARecord)=" + (relLineInTable % nbLineForARecord) + ", tc.lineIndex=" + tc.lineIndex);
			if ((tc.lineIndex == (relLineInTable % nbLineForARecord)) && (overlapFactor>bestOverlapFactor)) {
				bestOverlapFactor = overlapFactor;
				indexRightColumn = i;
			}
			++i;
		}

		if (bestOverlapFactor != 0) {
			if (colonDispatch.get(indexRightColumn) != null){
				// there is already a block dispatched in this column, append the text of the new block to the existing one
				blockTmp = colonDispatch.get(indexRightColumn);
				colonDispatch.remove(indexRightColumn);
				block.setText(blockTmp.getText() + " " + block.getText());

				// update the block column to the initial column
				block.column = blockTmp.column;
			}
			Engine.logBeans.trace("insert block " + block.getText() + " in column " + columns.get(indexRightColumn).title);
			colonDispatch.add(indexRightColumn, block);
		} else Engine.logBeans.trace("block " + block.getText() + " inserted nowhere (bestOvelapFactor = 0)");
	}

	private int getMax_index(){
		int maxIndex = 0;
		for (TableColumn tc : columns)
			if (tc.lineIndex > maxIndex)
				maxIndex = tc.lineIndex;
		return maxIndex;
	}

	/** Added by E.Digiaro 
	 * 10/10/2003
	 * @param block : current block
	 * @param tc : current TableColumn
	 * @return the percentage of overlap (nb column in common) between the block and the TableColumn 
	 */
	private int getOverlapFactor(Block block, TableColumn  tc){
		int blockStart = block.column;
		int blockEnd = block.column + block.length;
		int nbColInCommon;
		int nbColNotInCommon = 0;
		/*		
				|         |
		   ___  |         |
		  |___| |         |           (1)
		   _____|_________|____
		  |____________________|      (2)
		     ___|___      |
		    |_______|     |           (3)
		        |    ___  |
		        |   |___| |           (4)
		        |       __|___  
		        |      |______|       (5)
		        |         |    ___  
				|         |   |___|   (6)
		 */		

		//(1)
		if (blockStart < tc.startColumn && blockEnd <= tc.startColumn) {
			nbColInCommon = 0;
			Engine.logBeans.trace("cas1");
		}
		//(6)
		else if (blockStart > tc.endColumn) {
			nbColInCommon = 0;
			Engine.logBeans.trace("cas6");
		}
		//(2)
		else if (blockStart < tc.startColumn && blockEnd >= tc.endColumn) {
			nbColInCommon = tc.endColumn - tc.startColumn;
			Engine.logBeans.trace("cas2");
		}
		//(3)
		else if (blockStart < tc.startColumn && blockEnd <= tc.endColumn) {
			nbColNotInCommon = tc.startColumn - blockStart;
			nbColInCommon = block.length - nbColNotInCommon;
			Engine.logBeans.trace("cas3");
		}
		//(5)
		else if (blockStart >= tc.startColumn && blockEnd > tc.endColumn) {
			nbColNotInCommon = blockEnd - tc.endColumn;
			nbColInCommon = block.length - nbColNotInCommon;
			if (block.length == 1)
				nbColInCommon = 1;
			Engine.logBeans.trace("cas5");
		}
		//(4)
		else if (blockStart >= tc.startColumn && blockEnd <= tc.endColumn) {
			nbColInCommon = block.length;
			Engine.logBeans.trace("cas4");
		}
		else {
			Engine.logBeans.trace("BLOCK RANGE NO WHERE");
			nbColInCommon = 0;
		}

		Engine.logBeans.trace("nbColInCommon = " + nbColInCommon);
		if (nbColInCommon <= 0)
			return 0;
		else 
			return ((nbColInCommon * 100) / block.length);
	}

	private void writeColumns(Block row) {
		if (!isEmpty() && columns.size() <= colonDispatch.size()){
			Block item;
			int i = 0;
			for(TableColumn tc : columns) {
				Block blk = colonDispatch.get(i);


				if (blk == null){
					item = new Block();
					item.name = "";
					item.type = "static";
					item.setText("");
				} else item = blk;

				String columnTagName = "undefined";
				if (tc.title.length() > 0) {
					columnTagName = StringUtils.normalize(tc.title, false);
					if (columnTagName.equals(""))
						columnTagName = "col"+i;
				}

				item.tagName = columnTagName;

				if (columnSelection == i) item.setOptionalAttribute("columnSelection", "true");
				row.addOptionalChildren(item);
				++i;
			}
		}
	}

	private boolean extractActions(BlockFactory blockFactory, org.w3c.dom.Document dom, Block table){
		XMLRectangle mySelectionScreenZone = getSelectionScreenZone();
		int relativePosition;
		if(lineActions == 0)
			return false;
		else if(lineActions < 0)
			relativePosition = mySelectionScreenZone.y + lineActions;
		else
			relativePosition = mySelectionScreenZone.y + mySelectionScreenZone.height + lineActions - 1;

		if (accumulatedData == null)
			// no accumulated data, start a new actionsDefined block
			actionsDefined = null;

		String textBlock;
		int currentBlockLine;
		int currentBlockColumn;
		int beginIndex= startPattern.length();
		int endIndex;
		Block blockTmp ;
		Block currentBlock = blockFactory.getFirstBlock();

		while ( blockFactory.getNextBlock(currentBlock) != null){
			if((currentBlock.line >= relativePosition) && (currentBlock.line < mySelectionScreenZone.y)) {
				textBlock = currentBlock.getText().trim();
				currentBlockLine = currentBlock.line;
				currentBlockColumn = currentBlock.column;

				if ( (!textBlock.startsWith(startPattern) && startPattern.length() != 0) || 
						(!textBlock.endsWith(endPattern) && endPattern.length() != 0) ) {
					currentBlock = blockFactory.getNextBlock(currentBlock);
					continue;
				}
				StringTokenizer test = new StringTokenizer(textBlock, separatorCharsForTokens, false);
				if(test.countTokens() < 2) {
					// This is not a valid Action line, bump to nect block
					currentBlock = blockFactory.getNextBlock(currentBlock);
					continue;
				}

				if (removeActionLines) {
					//as the block is an action, delete it, position on the next block
					blockTmp = blockFactory.getNextBlock(currentBlock);
					blockFactory.removeBlock(currentBlock);
					currentBlock =  blockTmp;
				} else {
					// We do not have to remove the action from the block, just bump to nect block
					currentBlock = blockFactory.getNextBlock(currentBlock);
				}

				if(actionsDefined == null){
					actionsDefined = new Block();
					actionsDefined.name = "";
					actionsDefined.type = "actionsTable";
					actionsDefined.tagName = "actionsTable";
					actionsDefined.setText("");
					actionsDefined.line = currentBlockLine;
					actionsDefined.column = currentBlockColumn;
				}

				if(endPattern.length() == 0)
					endIndex= textBlock.length();
				else
					endIndex= textBlock.indexOf(endPattern);

				textBlock = textBlock.substring(beginIndex, endIndex);
				StringTokenizer st = new StringTokenizer(textBlock, separatorChars, false);
				Element element;
				String token;
				while (st.hasMoreTokens()) {
					token = st.nextToken().trim();
					element = dom.createElement("action");

					try {
						StringTokenizer st2 = new StringTokenizer(token, separatorCharsForTokens, false);
						String action = st2.nextToken().trim();
						element.setAttribute("char", action);
						if (autoValidate)
							element.setAttribute("key", "KEY_ENTER");
						else
							element.setAttribute("key", "null");
						String value = st2.nextToken().trim();
						element.setAttribute("label", value);
					}
					catch(Exception e) {
						Engine.logBeans.error("Unable to get the token.", e);
					}

					// add the element only if it dose not exists
					int i=0;
					List<Element> optionalChildren = actionsDefined.getOptionalElementChildren();
					for(Element elt : optionalChildren){
						if(elt.getAttribute("char").equals(element.getAttribute("char"))) break;
						i++;
					}
					if (i == actionsDefined.getNumberOfOptionalChildren())
						actionsDefined.addOptionalChildren(element);
				}
			} else
				currentBlock = blockFactory.getNextBlock(currentBlock);
		}

		if(actionsDefined == null){
			Engine.logBeans.debug("Warning : no actions defined in the action line.");
			return false;
		}

		if (accumulatedData == null)
			table.addOptionalChildren(actionsDefined);
		else {
			// we are in accumulate Mode, search in the table the existing actionsTable and replace it with the new One
			int i=0;
			List<Block> optionalChildren = table.getOptionalBlockChildren();
			for(Block block : optionalChildren){
				if(block.type.equalsIgnoreCase("actionsTable")){
					table.setOptionalBlockChildren(i, actionsDefined);
					break;
				}else i++;
			}
		}

		return true;
	}

	protected transient Block table;

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
		try {
			JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
			Block row;
			Block item;
			Block curBlock;
			int curLine, firstColumn;
			boolean automaticColumn = false;
			table = null;

			colonDispatch = new LinkedList<Block>();

			firstLine = getSelectionScreenZone().y;
			firstColumn = getSelectionScreenZone().x;

			if (accumulatedData == null) {
				// not accumulating, create a new table element
				try {
					// create the main block of the table
					table = new Block();
					table.tagName = this.tagName;
					table.name = "table";
					table.type = "table";
					table.setText("");
					table.bRender = true;
					table.line = firstLine;
					table.column = firstColumn;
					table.setOptionalAttribute("offset", "" + getOffset());
					//					table.setOptionalAttribute("autoValidate", "" + isAutoValidate()); 
					//insert this block before the current block
					blockFactory.insertBlock(table, blockFactory.getPreviousBlock(block));
					curBlock = block;
					//create the table of actions if needed
					if(columnSelection >= 0){
						boolean hasExtractActions = extractActions(blockFactory, dom, table);
						if (!hasExtractActions) {
							if (actions == null || actions.isEmpty())
								Engine.logBeans.debug("Warning: no actions defined.");
							else {
								Block actionsDefined = new Block();
								actionsDefined.name = "";
								actionsDefined.type = "actionsTable";
								actionsDefined.tagName = "actionsTable";
								actionsDefined.setText("");

								for(List<String> myAction : actions){
									Element element = dom.createElement("action");
									element.setAttribute("char", myAction.get(0));
									element.setAttribute("key", myAction.get(1));
									element.setAttribute("label", myAction.get(2));
									actionsDefined.addOptionalChildren(element);
								}
								table.addOptionalChildren(actionsDefined);
							}
						}
					}

					// create the first row
					row = new Block();
					row.name = "";
					row.type = "row";
					row.tagName = "row";
					row.setText("");
					row.setOptionalAttribute("index", Integer.toString(indexRow));
					indexRow++;
					row.setOptionalAttribute("page", Integer.toString(numPage));

					// append the row block as a child to the main block only if we include titles
					if (!doNotIncludeTitles){
						table.addOptionalChildren(row);
					}
					// note the current line.
					curLine = curBlock.line;
				}
				catch (Exception e) {
					xrs.hasMatched = false;
					xrs.newCurrentBlock = block;
					return xrs;
				}
			}
			else {
				// there is accumulated data, use it
				table = accumulatedData;

				// As we are in accumulate mode, some new actions may have been defined, exract them
				extractActions(blockFactory, dom, table);

				// prepare a new row in the table
				row = new Block();
				row.name = "";
				row.type = "row";
				row.tagName = "row";
				row.setText("");
				row.line = block.line;
				row.column = block.column;
				row.setOptionalAttribute("index", Integer.toString(indexRow));
				indexRow++;
				row.setOptionalAttribute("page", Integer.toString(numPage));

				// set the first row in the table
				// table.appendChild(row);
				table.addOptionalChildren(row);
				curBlock = block;
				curLine = curBlock.line;

			}
			Engine.logBeans.trace("First block in the table, line: " + curBlock.line + " col: " + curBlock.column + " len : " + curBlock.length);

			if (columns.size() == 0) {
				Block temp;

				// no columns were defined, so assume the first row will define the columns
				// set the automatic Column mode flag.
				automaticColumn = true;

				curBlock = block;
				while (curBlock != null && (curBlock.line == curLine)) {
					if (!canBlockBeSelected(curBlock)){
						curBlock= blockFactory.getNextBlock(curBlock);
						continue;
					}
					String title = StringUtils.normalize(curBlock.getText());
					columns.add(new TableColumn(title, curBlock.column, curBlock.column + curBlock.length, 0));
					Engine.logBeans.trace("Table defining '" + curBlock.getText() + "' from column: " + curBlock.column + " to column: " + (curBlock.column + curBlock.length));

					temp = blockFactory.getNextBlock(curBlock);
					blockFactory.removeBlock(curBlock);
					curBlock = temp;
				}
				curLine= curBlock.line;
			}

			// columns are explicitly defined so generate the table header only if
			// we do not accumulate
			if (accumulatedData == null) {
				if (!doNotIncludeTitles) {
					if (columnsBlocks == null) {
						int tableSize = 0;
						for (TableColumn tc : columns) {
							item = new Block();
							item.setText("");
							item.name = "";
							item.type = "item";
							item.tagName = "Title";
							item.column = tc.startColumn;
							item.line = curBlock.line - 1;
							item.setOptionalAttribute("size", Integer.toString(tc.endColumn - tc.startColumn + 1));
							tableSize += tc.endColumn - tc.startColumn + 1;
							row.addOptionalChildren(item);
							String title = tc.title;
							StringTokenizer st = new StringTokenizer(title, "\\", false);
							if (st.countTokens() > titleH) {
								titleH = st.countTokens();
							}
							String titleLabel;
							Block titleBlock;
							try {
								while (true) {
									titleLabel = st.nextToken();
									titleBlock = new Block();
									titleBlock.setText(titleLabel);
									titleBlock.line = item.line - st.countTokens();
									titleBlock.column = item.column;
									item.addOptionalChildren(titleBlock);
								}
							} catch (NoSuchElementException e) {
								// normal exit
							}
						}
						table.setOptionalAttribute("size", "" + tableSize);
					}
					else {
						int tableSize = 0;
						for (TableColumn tc : columns) {
							item = new Block();
							item.setText("");
							item.name = "";
							item.type = "item";
							item.tagName = "Title";
							item.column = tc.startColumn;
							item.setOptionalAttribute("size", Integer.toString(tc.endColumn - tc.startColumn + 1));
							tableSize += tc.endColumn - tc.startColumn + 1;
							row.addOptionalChildren(item);
							for(Block columnBlock : columnsBlocks) {
								if ((tc.startColumn <= columnBlock.column) && (columnBlock.column <= tc.endColumn)) {
									item.line = Math.min(item.line, columnBlock.line);
									item.addOptionalChildren(columnBlock);
								}
							}
						}
						table.setOptionalAttribute("size", "" + tableSize);
					}
				}
				row = new Block();
				row.name = "";
				row.type = "row";
				row.tagName = "row";
				row.setText("");
				row.line = block.line;
				row.column = block.column;
				// set the first row in the table
				table.addOptionalChildren(row);
				row.setOptionalAttribute("index", Integer.toString(indexRow));
				indexRow++;
				row.setOptionalAttribute("page", Integer.toString(numPage));

				// store table in accumulatedData
				accumulatedData = table;
			}

			// we found a matching block; scan all other blocks to get other table items
			while (curBlock != null) {
				Block temp;

				if (!canBlockBeSelected(curBlock)) {
					curBlock = blockFactory.getNextBlock(curBlock);
					continue;
				}

				if ((curBlock.line - row.line) > max_index) {
					// calc the number of lines skipped
					// this will handle the empty lines...
					int skip = (curBlock.line - row.line) / (max_index +1);
					for (int i=0; i< skip; i++) {
						// we changed line, so write all the columns builded in the prevous row held in colonDispatch
						writeColumns(row);

						// clear the colonDispatch vector for next row
						colonDispatch = new LinkedList<Block>();

						// set the line reference
						curLine = curBlock.line;

						// build a new row
						row = new Block();
						row.name 	= "";
						row.type 	= "row";
						row.tagName = "row";
						row.setText("");
						row.line 	= curBlock.line - skip + i +1; // row line depends on  lines skipped
						row.column 	= curBlock.column;
						table.addOptionalChildren(row);
						row.setOptionalAttribute("index", Integer.toString(indexRow));
						indexRow++;
						row.setOptionalAttribute("page", Integer.toString(numPage));


						if (keepEmptyLines && skip > 1) {
							// if we skipped lines (Empty lines) insert a fake block as the writeColumns function
							// will discard any empty block.

							Block fakeBlock = new Block();
							fakeBlock.line = row.line;
							fakeBlock.column = row.column;
							fakeBlock.setText("");
							fakeBlock.name = "";
							fakeBlock.type = "static";
							dispatchBlockInColumn(fakeBlock);
						}

					}
				}

				dispatchBlockInColumn(curBlock);

				temp = blockFactory.getNextBlock(curBlock);
				blockFactory.removeBlock(curBlock);
				curBlock = temp;
			}


			// write the last row
			writeColumns(row);

			if (automaticColumn)
				columns = new XMLVector<TableColumn>();

			table.setOptionalAttribute("rows", Integer.toString(indexRow - 1));
			table.setOptionalAttribute("titleheight", "" + titleH);

			int tableWidth = getSelectionScreenZone().width;
			int tableHeight = getSelectionScreenZone().height;

			Engine.logBeans.debug("SelectionScreenZone tableWidth : " + tableWidth + "tableHeight" + tableHeight);

			if (tableWidth != -1)
				table.setOptionalAttribute("width", Integer.toString(tableWidth));
			else
				table.setOptionalAttribute("width", Integer.toString(javelin.getScreenWidth()));            

			if (tableHeight != -1)
				table.setOptionalAttribute("height", Integer.toString(tableHeight + resize));
			else
				table.setOptionalAttribute("height", Integer.toString(javelin.getScreenHeight() + resize));            


			addMashupAttribute(table);

			xrs.hasMatched = true;
			xrs.newCurrentBlock = block;

			return xrs;
		}
		finally {
			if (doNotAccumulate) {
				accumulatedData = null;
			} else {
				numPage ++;
			}
		}
	}

	/** Getter for property columns.
	 * @return Value of property columns.
	 */
	public XMLVector<XMLVector<Object>> getColumns() {
		XMLVector<XMLVector<Object>> cols = new XMLVector<XMLVector<Object>>();

		for(TableColumn tc : columns){
			XMLVector<Object> element = new XMLVector<Object>();
			element.add(new String(tc.title));
			element.add(new Integer(tc.startColumn));
			element.add(new Integer(tc.endColumn));
			element.add(new Integer(tc.lineIndex));
			cols.add(element);
		}
		return cols;
	}

	/** Setter for property columns.
	 * @param columns New value of property columns.
	 */
	public void setColumns(XMLVector<XMLVector<Object>> columns) {
		XMLVector<TableColumn> cols = new XMLVector<TableColumn>();

		for(List<Object> v :columns){
			int start, end, index;
			String title = (String)v.get(0);
			if (v.get(1) instanceof String)
				start = Integer.parseInt((String) v.get(1));
			else start = ((Integer) v.get(1)).intValue();

			if (v.get(2) instanceof String)
				end = Integer.parseInt((String) v.get(2));
			else end = ((Integer) v.get(2)).intValue();

			if (v.size() == 4) {
				// we have an index property, use it
				if (v.get(3) instanceof String)
					index = Integer.parseInt((String) v.get(3));
				else index = ((Integer) v.get(3)).intValue();
			} else {
				// old version of tables do not support the index property in column
				// object so fix it to 0
				index = 0;            
			}
			cols.add(new TableColumn(title, start, end, index));
		}
		this.columns = cols;
	}

	/** Getter for property tagName.
	 * @return Value of property tagName.
	 */
	public java.lang.String getTagName() {
		return tagName;
	}

	/** Setter for property tagName.
	 * @param tagName New value of property tagName.
	 */
	public void setTagName(java.lang.String tagName) {
		this.tagName = tagName;
	}

	private synchronized boolean isEmpty(){
		if (!keepEmptyLines) {
			for (Block block : colonDispatch){
				// block may be null if the page isn't fully loaded
				if (block == null)
					continue;
				String text = block.getText();
				if (((text != null) && (text.trim().length() != 0)) || (block.type.compareToIgnoreCase("field") == 0))
					return false;
			}
			Engine.logBeans.trace("Skipping empty row");
			return true;
		} else {
			Engine.logBeans.trace("keepEmptyLines set to true, isEmpty returns false");
			return false;
		}
	}

	/** Getter for property offset.
	 * @return Value of property offset.
	 */
	public int getOffset() {
		return this.offset;
	}

	/** Setter for property offset.
	 * @param offset New value of property offset.
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}
	/** Getter for property actions.
	 * @return Value of property actions.
	 */
	public XMLVector<XMLVector<String>> getActions() {
		return this.actions;
	}

	/** Setter for property actions.
	 * @param actions New value of property actions.
	 */
	public void setActions(XMLVector<XMLVector<String>> actions) {
		this.actions = actions;
	}

	/** Getter for property columnSelection.
	 * @return Value of property columnSelection.
	 */
	public int getColumnSelection() {
		return this.columnSelection;
	}

	/** Setter for property columnSelection.
	 * @param columnSelection New value of property columnSelection.
	 */
	public void setColumnSelection(int columnSelection) {
		this.columnSelection= columnSelection;
	}

	/** Getter for property lineActions.
	 * @return Value of property lineActions.
	 */
	public int getLineActions() {
		return this.lineActions;
	}

	/** Setter for property lineActions.
	 * @param columnSelection New value of property lineActions.
	 */
	public void setLineActions(int lineActions) {
		this.lineActions= lineActions;
	}

	/** Getter for property startPattern.
	 * @return Value of property startPattern.
	 */
	public java.lang.String getStartPattern() {
		return startPattern;
	}

	/** Setter for property startPattern.
	 * @param startPattern New value of property startPattern.
	 */
	public void setStartPattern(java.lang.String startPattern) {
		this.startPattern = startPattern;
	}

	/** Getter for property endPattern.
	 * @return Value of property endPattern.
	 */
	public java.lang.String getEndPattern() {
		return endPattern;
	}

	/** Setter for property endPattern.
	 * @param endPattern New value of property endPattern.
	 */
	public void setEndPattern(java.lang.String endPattern) {
		this.endPattern = endPattern;
	}

	/** Getter for property separatorChars.
	 * @return Value of property separatorChars.
	 */
	public java.lang.String getSeparatorChars() {
		return separatorChars;
	}

	/** Setter for property separatorChars.
	 * @param separator New value of property separatorChars.
	 */
	public void setSeparatorChars(java.lang.String separatorChars) {
		this.separatorChars = separatorChars;
	}

	/** Getter for property separatorCharsForTokens.
	 * @return Value of property separatorCharsForTokens.
	 */
	public String getSeparatorCharsForTokens() {
		return this.separatorCharsForTokens;
	}

	/** Setter for property separatorCharsForTokens.
	 * @param separatorCharsForTokens New value of property separatorCharsForTokens.
	 */
	public void setSeparatorCharsForTokens(String separatorCharsForTokens) {
		this.separatorCharsForTokens = separatorCharsForTokens;
	}    

	/** Getter for property doNotIncludeTitles.
	 * @return Value of property doNotIncludeTitles.
	 */
	public boolean isDoNotIncludeTitles() {
		return this.doNotIncludeTitles;
	}

	/** Setter for property doNotIncludeTitles.
	 * @param doNotIncludeTitles New value of property doNotIncludeTitles.
	 */
	public void setDoNotIncludeTitles(boolean doNotIncludeTitles) {
		this.doNotIncludeTitles = doNotIncludeTitles;
	}

	/** Getter for property doNotAccumulate.
	 * @return Value of property doNotAccumulate.
	 */
	public boolean isDoNotAccumulate() {
		return this.doNotAccumulate;
	}

	/** Setter for property doNotAccumulate.
	 * @param doNotAccumulate New value of property doNotAccumulate.
	 */
	public void setDoNotAccumulate(boolean doNotAccumulate) {
		this.doNotAccumulate = doNotAccumulate;
	}

	/** Getter for property autoEnter.
	 * @return Value of property autoEnter.
	 */
	public boolean isAutoValidate() {
		return this.autoValidate;
	}

	/** Setter for property autoEnter.
	 * @param autoEnter New value of property autoEnter.
	 */
	public void setAutoValidate(boolean autoValidate) {
		this.autoValidate = autoValidate;
	}

	/** Getter for property removeActionLines.
	 * @return Value of property removeActionLines.
	 */
	public boolean isRemoveActionLines() {
		return this.removeActionLines;
	}

	/** Setter for property removeActionLines.
	 * @param removeActionLines New value of property removeActionLines.
	 */
	public void setRemoveActionLines(boolean removeActionLines) {
		this.removeActionLines = removeActionLines;
	}

	/** Getter for property keepEmptyLines.
	 * @return Value of property keepEmptyLines.
	 */
	public boolean isKeepEmptyLines() {
		return this.keepEmptyLines;
	}

	/** Setter for property keepEmptyLines.
	 * @param keepEmptyLines New value of property keepEmptyLines.
	 */
	public void setKeepEmptyLines(boolean keepEmptyLines) {
		this.keepEmptyLines = keepEmptyLines;
	}

	public int getResize() {
		return resize;
	}

	public void setResize(int resize) {
		this.resize = resize;
	}

	/**
	 * Tests if a block can be selected for being executing.
	 *
	 * @param block the block to be tested
	 *
	 * @return true if the block is in the zone, false otherwize.
	 */
	@Override
	public boolean canBlockBeSelected(Block block) {
		return ((!block.bFinal) && super.canBlockBeSelected(block));
	}
}