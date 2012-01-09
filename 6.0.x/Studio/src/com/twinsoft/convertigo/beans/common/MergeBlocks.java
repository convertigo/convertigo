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

import java.awt.Rectangle;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.ComplexExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.twinj.Javelin;
import com.twinsoft.twinj.iJavelin;

public class MergeBlocks extends ComplexExtractionRule {

	private static final long serialVersionUID = -3546352101531515001L;

	/** Holds value of property separationPattern. */
	private String separationPattern = " ";

	/** Holds value of property multilineSeparatorChar. */
	private String multilineSeparatorChar = " ";

	/** Holds value of property bMultilineMerge. */
	private boolean bMultilineMerge = false;

	/** Creates new RemoveBlankBlock */
	public MergeBlocks() {
		super();
	}

	public boolean canBlockBeSelected(Block block) {
		if ((!bMultilineMerge) || (!bAccumulating)) {
			return super.canBlockBeSelected(block);
		}
		else {
			Rectangle selectionScreenZone = getSelectionScreenZone();
			int selectionAttribute = getSelectionAttribute();
			String selectionType = getSelectionType();
			
			if ((selectionScreenZone.y != -1) && (block.line < selectionScreenZone.y)) {
				Engine.logBeans.trace("Block not selected because of wrong selection screen zone (line out of range)");
				return false;
			}
			if ((selectionScreenZone.x != -1) && (block.column < selectionScreenZone.x)) {
				Engine.logBeans.trace("Block not selected because of wrong selection screen zone (column out of range)");
				return false;
			}
			if ((selectionScreenZone.height != -1) && (block.line >= (selectionScreenZone.y + selectionScreenZone.height))) {
				Engine.logBeans.trace("Block not selected because of wrong selection screen zone (height out of range)");
				return false;
			}
			if (selectionAttribute != -1) {
				boolean b1 = ((selectionAttribute & DONT_CARE_FOREGROUND_ATTRIBUTE) != 0 ? true : (block.attribute & Javelin.AT_INK) == (selectionAttribute & Javelin.AT_INK));
				boolean b2 = ((selectionAttribute & DONT_CARE_BACKGROUND_ATTRIBUTE) != 0 ? true : (block.attribute & Javelin.AT_PAPER) == (selectionAttribute & Javelin.AT_PAPER));
				boolean b3 = ((selectionAttribute & DONT_CARE_INTENSE_ATTRIBUTE) != 0 ? true : (block.attribute & Javelin.AT_BOLD) == (selectionAttribute & Javelin.AT_BOLD));
				boolean b4 = ((selectionAttribute & DONT_CARE_UNDERLINED_ATTRIBUTE) != 0 ? true : (block.attribute & Javelin.AT_UNDERLINE) == (selectionAttribute & Javelin.AT_UNDERLINE));
				boolean b5 = ((selectionAttribute & DONT_CARE_REVERSE_ATTRIBUTE) != 0 ?  true : (block.attribute & Javelin.AT_INVERT) == (selectionAttribute & Javelin.AT_INVERT));
				boolean b6 = ((selectionAttribute & DONT_CARE_BLINK_ATTRIBUTE) != 0 ? true : (block.attribute & Javelin.AT_BLINK) == (selectionAttribute & Javelin.AT_BLINK));

				if (!(b1 && b2 && b3 && b4 && b5 && b6)) {
					Engine.logBeans.trace("Block not selected because of wrong attributes\n" +
						"AT_PAPER=" + b1 + "\n" +
						"AT_INK=" + b2 + "\n" +
						"AT_BOLD=" + b3 + "\n" +
						"AT_INVERT=" + b4 + "\n" +
						"AT_UNDERLINE=" + b5 + "\n" +
						"AT_BLINK=" + b6 + "\n"
					);
					return false;
				}
			}

			try {
				RE regexp = new RE(selectionType);
				if (!regexp.match(block.type)) return false;
			}
			catch(RESyntaxException e) {
				Engine.logBeans.error("Unable to execute the regular expression for the selection of the block", e);
			}
        
			return true;
		}
	}

	private transient boolean bAccumulating = false;

	/**
	  * Applies the extraction rule to the current iJavelin object.
	  *
	  * @param javelin the Javelin object.
	  * @param block the current block to analyze.
	  * @param blockFactory the block context of the current block.
	  * @param dom the XML DOM.
	  *
	  * @return an ExtractionRuleResult object containing the result of
	  * the query.
	  */

	public JavelinExtractionRuleResult execute(
		iJavelin javelin,
		Block block,
		BlockFactory blockFactory,
		org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		Block curBlock;
		Block tempBlock;

		curBlock = block;
		xrs.hasMatched = false;

		if (canBlockBeSelected(curBlock)) {
			tempBlock = blockFactory.getNextBlock(curBlock);
			if ((tempBlock == null)
				|| (tempBlock.line != curBlock.line && !bMultilineMerge)
				|| (!canBlockBeSelected(tempBlock))
				|| (tempBlock.bFinal)) {
				bAccumulating = false;
				xrs.newCurrentBlock = curBlock;
				return xrs;
			} else {
				if (tempBlock.line != curBlock.line) {
					curBlock.setText(
						curBlock.getText() + multilineSeparatorChar);
				}
				if (separationPattern.equals("")) {
					// Ignore separator                 
					Engine.logBeans.trace(
						"merging curBlock : line,col "
							+ curBlock.line
							+ ","
							+ curBlock.column
							+ " text ["
							+ curBlock.getText()
							+ "]");
					Engine.logBeans.trace(
						"and    tempBlock : line,col "
							+ tempBlock.line
							+ ","
							+ tempBlock.column
							+ " text ["
							+ tempBlock.getText()
							+ "]");
					blockFactory.mergeBlocks(curBlock, tempBlock);
					bAccumulating = true;
					xrs.hasMatched = true;
					xrs.newCurrentBlock =
						blockFactory.getPreviousBlock(curBlock);
					return xrs;
				} else if (
					tempBlock.getText().equalsIgnoreCase(separationPattern)) {
					// next block is a separator block so merge it in our block
					Engine.logBeans.trace(
						"merging curBlock : line,col "
							+ curBlock.line
							+ ","
							+ curBlock.column
							+ " text ["
							+ curBlock.getText()
							+ "]");
					Engine.logBeans.trace(
						"and    tempBlock : line,col "
							+ tempBlock.line
							+ ","
							+ tempBlock.column
							+ " text ["
							+ tempBlock.getText()
							+ "]");
					blockFactory.mergeBlocks(curBlock, tempBlock);
					tempBlock = blockFactory.getNextBlock(curBlock);
					if ((tempBlock != null)
						&& ((tempBlock.line == curBlock.line)
							|| (tempBlock.line != curBlock.line
								&& bMultilineMerge))
						&& (canBlockBeSelected(tempBlock))
						&& (!tempBlock.bFinal)
						&& (!tempBlock
							.getText()
							.equalsIgnoreCase(separationPattern))) {
						blockFactory.mergeBlocks(curBlock, tempBlock);
						// the engine will fetch the nect block but this rule
						// needs to restart with the same block !!
						// so  position the currentBlock on the prevous block
						bAccumulating = true;
						xrs.hasMatched = true;
						xrs.newCurrentBlock =
							blockFactory.getPreviousBlock(curBlock);
						return xrs;
					}
				}
			}
		}

		bAccumulating = false;
		xrs.newCurrentBlock = curBlock;

		return xrs;
	}

	/** Getter for property separationPattern.
	 * @return Value of property separationPattern.
	 */
	public String getSeparationPattern() {
		return separationPattern;
	}

	/** Setter for property separationPattern.
	 * @param separationPattern New value of property separationPattern.
	 */
	public void setSeparationPattern(String separationPattern) {
		this.separationPattern = separationPattern;
	}

	/** Getter for property bMultilineMerge.
	 * @return Value of property bMultilineMerge.
	 */
	public boolean isMultilineMerge() {
		return bMultilineMerge;
	}

	/** Setter for property bMultilineMerge.
	 * @param bMultilineMerge New value of property bMultilineMerge.
	 */
	public void setMultilineMerge(boolean bMultilineMerge) {
		this.bMultilineMerge = bMultilineMerge;
	}
	
	/** Getter for property multilineSeparatorChar.
	 * @return Value of property multilineSeparatorChar.
	 */
	public String getMultilineSeparatorChar() {
		return multilineSeparatorChar;
	}

	/** Setter for property multilineSeparatorChar.
	 * @param multilineSeparatorChar New value of property multilineSeparatorChar.
	 */
	public void setMultilineSeparatorChar(String multilineSeparatorChar) {
		this.multilineSeparatorChar = multilineSeparatorChar;
	}

	/**
	 * Performs custom configuration for this database object.
	 */
	public void configure(Element element) throws Exception {
		super.configure(element);

		String version = element.getAttribute("version");

		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee =
				new EngineException(
					"Unable to find version number for the database object \""
						+ getName()
						+ "\".\nXML data: "
						+ s);
			throw ee;
		}

		if (VersionUtils.compare(version, "1.2.2")
			<= 0) {
			setSelectionType("static|delimiters");

			hasChanged = true;
			Engine.logBeans.warn(
				"[MergeBlocks] The object \""
					+ getName()
					+ "\" has been updated to version 1.2.3");
		}
	}
}
