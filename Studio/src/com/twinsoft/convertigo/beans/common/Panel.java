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

import java.util.LinkedList;
import java.util.List;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.twinj.Javelin;
import com.twinsoft.twinj.iJavelin;

/**
 * This class manages Panels.
 */

class Coordinates {
	int column = 0;

	int line = 0;

	public Coordinates(int column, int line) {
		this.column = column;
		this.line = line;
	}

	public Coordinates() {
	}

	public String printCoordinates() {
		return ("point: column=" + column + ", line= " + line);
	}

	public boolean equals(Coordinates c) {
		if (c.column == this.column && c.line == this.line)
			return true;
		else
			return false;
	}
}

class PanelCandidate {
	// U* = coordinates of the upper left & right corners
	// L* = coordinates of the lower left & right corners
	// factor = number of corners found (from 1 to 4) this will help
	// to determinate the best panel
	Coordinates UL;
	Coordinates UR;
	Coordinates LR;
	Coordinates LL;
	int factor;
	String  topTitle;
	String  bottomTitle;

	public PanelCandidate(Coordinates UL, Coordinates UR, Coordinates LR, Coordinates LL, int factor, String topTitle, String bottomTitle) {
		this.UL = UL;
		this.UR = UR;
		this.LR = LR;
		this.LL = LL;
		this.factor = factor;
		this.topTitle = topTitle;
		this.bottomTitle = bottomTitle;
	}

	public PanelCandidate() {
		this.UL = new Coordinates();
		this.UR = new Coordinates();
		this.LR = new Coordinates();
		this.LL = new Coordinates();
		this.factor = 0;
		this.topTitle = "";
		this.bottomTitle = "";
	}
}

public class Panel extends JavelinExtractionRule {
	public static final long serialVersionUID = 4350644982787619412L;

	/** Holds value of property upperLeft. */
	private String upperLeft = "+.";

	/** Holds value of property lowerLeft. */
	private String lowerLeft = "+:";

	/** Holds value of property upperRight. */
	private String upperRight = "+.";

	/** Holds value of property lowerRight. */
	private String lowerRight = "+:";

	/** Holds value of property top. */
	private String top = "-.";

	/** Holds value of property left. */
	private String left = "!:";

	/** Holds value of property right. */
	private String right = "!:";

	/** Holds value of property bottom. */
	private String bottom = "-.";
	
	/** Holds value of property selectionAttribute. */
    private int titleAttribute = -1;

	private boolean allowTextInBorders = true;

	private int minSides = 4;

	private boolean removeBlocksInBorder = true;

	private boolean initialize = false;

	transient int screenHeight;

	transient int screenWidth;

	transient List<PanelCandidate> listCandidate = new LinkedList<PanelCandidate>();

	transient PanelCandidate lePanel;

	transient boolean checkAttributes;

	transient int panelAttribute;
	
	transient boolean spaceBeforeTitle;
	transient boolean spaceAfterTitle;
	transient boolean bTitle;
	transient String  topTitle;
	transient String  bottomTitle;

	public Panel() {
		super();
	}

	/**
	 * This will be called at each beginning of a transaction
	 */
	@Override
	public void init(int reason) {
		initialize = true;
		spaceBeforeTitle = false;
		spaceAfterTitle = false;
		bTitle = false;
		topTitle = "";
		bottomTitle = "";
	}

	/**
	 * Tests if a block is in the selection.
	 * 
	 * @param block
	 *            the block to be tested
	 * 
	 * @return true if the block is in the zone, false otherwize.
	 */
	private boolean isBlockInSelection(Block block,
			XMLRectangle mySelectionScreenZone) {
		if ((mySelectionScreenZone.y != -1)
				&& (block.line < mySelectionScreenZone.y))
			return false;
		if ((mySelectionScreenZone.x != -1)
				&& (block.column < mySelectionScreenZone.x))
			return false;
		if ((mySelectionScreenZone.width != -1)
				&& ((block.length + block.column) - 1 > mySelectionScreenZone.x
						+ mySelectionScreenZone.width))
			return false;
		if ((mySelectionScreenZone.height != -1)
				&& (block.line > (mySelectionScreenZone.y + mySelectionScreenZone.height)))
			return false;
		return true;
	}

	/**
	 * Tests if a block is in the border.
	 * 
	 * @param block
	 *            the block to be tested
	 * 
	 * @return true if the block is in the border of the zone, false otherwize.
	 */
	private boolean isBlockInBorder(Block block,
			XMLRectangle mySelectionScreenZone) {
		// The block is the top border
		if ((mySelectionScreenZone.y != -1)
				&& (block.line == mySelectionScreenZone.y))
			return true;
		// The block is in the left border
		if ((mySelectionScreenZone.x != -1)
				&& (block.column == mySelectionScreenZone.x))
			return true;
		// The block is in the right border
		if ((mySelectionScreenZone.width != -1)
				&& (block.length + block.column - 1 == mySelectionScreenZone.x
						+ mySelectionScreenZone.width))
			return true;
		// The block is in the bottom border
		if ((mySelectionScreenZone.height != -1)
				&& (block.line == (mySelectionScreenZone.y + mySelectionScreenZone.height)))
			return true;
		return false;
	}

	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block,
			BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();

		/*
		 * consider any form of text :
		 * 
		 * UL t t t t t t t t t UR l R l R LL b b b b b b b b b LR
		 * 
		 * to be a panel.
		 * 
		 * UL : upperLeft UR : upperRight LL : lowerLeft LR : lowerRight l :
		 * left r : right t : top b : bottom
		 * 
		 */
		boolean isSelected = isSelectedPanel();
		if (initialize && isSelected) {
			initialize = false;
			Block myPanel = new Block();
			myPanel.name = this.name;
			myPanel.type = "panel";
			myPanel.setText("");
			myPanel.bRender = true;
			myPanel.line = getSelectionScreenZone().y;
			myPanel.column = getSelectionScreenZone().x;
			myPanel.setOptionalAttribute("width", "" + getSelectionScreenZone().width);
			myPanel.setOptionalAttribute("height", "" + getSelectionScreenZone().height);
			myPanel.setOptionalAttribute("topTitle", topTitle);
			myPanel.setOptionalAttribute("bottomTitle", bottomTitle);
			
			Block curBlock = blockFactory.getFirstBlock();
			block = null;
			while ((curBlock = blockFactory.getNextBlock(curBlock)) != null) {
				if (isBlockInSelection(curBlock, getSelectionScreenZone())) {
					myPanel.addOptionalChildren(curBlock);
					// we mark the first block that goes inside the panel
					if (block == null)
						block = curBlock;
				}
			}
			// we insert the panel to the blockfactory, before the first block
			// inside the panel
			blockFactory.insertBlock(myPanel, blockFactory
					.getPreviousBlock(block));
			xrs.newCurrentBlock = blockFactory.getLastBlock();
			return xrs;
		}
		// 1-pass only to find the panel
		// we work on the iJavelin object (character) rather than on the
		// blockFactory
		if (initialize) {
			listCandidate = new LinkedList<PanelCandidate>();
			lePanel = null;
			screenHeight = javelin.getScreenHeight();
			screenWidth = javelin.getScreenWidth();
			int curLine = 0, curColumn = 0;
			while (curLine < screenHeight) {
				while (curColumn < screenWidth) {
					// Engine.logBeans.trace("-----> L=" + curLine + ", C=" +
					// curColumn + ", " + javelin.getChar(curColumn, curLine));

					// check if current character matches UL corner of a panel
					if (charMatchesUpperLeft(javelin, 
											javelin.getChar(curColumn, curLine), 
											javelin.getCharAttribute(curColumn, curLine))
							&& !coordinateInExistantPanel((new Coordinates(curColumn, curLine)))) {
						// Engine.logBeans.trace("PANEL TROUVE en " + curColumn + " " +curLine);
						checkClosedContours(javelin, curColumn, curLine);
					}
					curColumn++;
				}
				curColumn = 0;
				curLine++;
			}
			// do not execute the instructions above next time (on the other
			// blocks)
			initialize = false;

			// print the found panel
			for(PanelCandidate unPanel : listCandidate) {
				
				if (unPanel.factor >= minSides) {
					Engine.logBeans.trace("Found closed panel : l=" + unPanel.UL.line + ", c=" + unPanel.UL.column);
					lePanel = unPanel;
					xrs.hasMatched = true;
					// now that we found a panel, add the blocks inside the
					// panel
					if (lePanel != null) {
						XMLRectangle laScreenZone = new XMLRectangle(
								lePanel.UL.column, lePanel.UL.line,
								lePanel.UR.column - lePanel.UL.column,
								lePanel.LR.line - lePanel.UR.line);
						Engine.logBeans.trace("Entrée avec " + block.getText());
						Engine.logBeans.trace("Panel : " + "column="
								+ lePanel.UL.column + ", line="
								+ lePanel.UL.line + ", width="
								+ (lePanel.UR.column - lePanel.UL.column)
								+ ", height="
								+ (lePanel.LR.line - lePanel.UR.line));
						if (laScreenZone.width > 1 && laScreenZone.height > 1) {
							Block myPanel = new Block();
							myPanel.name = this.name;
							myPanel.type = "panel";
							myPanel.setText("");
							myPanel.bRender = true;
							myPanel.line = laScreenZone.y;
							myPanel.column = laScreenZone.x;
							myPanel.setOptionalAttribute("width", "" + laScreenZone.width);
							myPanel.setOptionalAttribute("height", ""+ laScreenZone.height);
							myPanel.setOptionalAttribute("topTitle", lePanel.topTitle);
							myPanel.setOptionalAttribute("bottomTitle", lePanel.bottomTitle);

							Block curBlock = blockFactory.getFirstBlock();
							block = null;
							do {
								Engine.logBeans.trace("Testing " + curBlock);
								if (isBlockInSelection(curBlock, laScreenZone)) {
									// if (curBlock.getText().length() != 0 &&
									// charMatches(curBlock.getText().charAt(0),
									// right + left + upperLeft))
									if (isBlockInBorder(curBlock, laScreenZone) && removeBlocksInBorder) {
										Block bToRemove = curBlock;
										curBlock = blockFactory
												.getPreviousBlock(curBlock);
										blockFactory.removeBlock(bToRemove);
										Engine.logBeans.trace("Removing " + bToRemove);
									} else {
										myPanel.addOptionalChildren(curBlock);
										// we mark the first block that goes
										// inside the panel
										if (block == null)
											block = curBlock;
									}
								}
							} while ((curBlock = blockFactory
									.getNextBlock(curBlock)) != null);
							// we insert the panel to the blockfactory, before
							// the first block inside the panel
							blockFactory.insertBlock(myPanel, blockFactory
									.getPreviousBlock(block));
							xrs.newCurrentBlock = blockFactory.getLastBlock();
							return xrs;
						} else {
							Engine.logBeans.debug("Invalid panel found (width = 0 or height = 0)");
							continue;
						}
					} else return xrs;
				}
			}
			return xrs;
		}
		return xrs;
	}

	private boolean charMatchesUpperLeft(iJavelin javelin, char char1, int attributes) {
		if ((attributes & Javelin.AT_PAPER) >> 3 != Javelin.AT_COLOR_BLACK) {
			// we detected a character with a background attribute different from
			// black => assume it is the UL of a panel
			checkAttributes = true;
			// we assume that all the panel will be of the same attribute
			panelAttribute = attributes;
			return true;
		} else {
			checkAttributes = false;
			return (upperLeft.indexOf(char1) != -1);
		}
	}

	/*
	 * private boolean charMatchesUpperRight(char char1){ return
	 * (upperRight.indexOf(char1)!=-1); }
	 * 
	 * private boolean charMatchesLowerLeft(char char1){ return
	 * (lowerLeft.indexOf(char1)!=-1); }
	 * 
	 * private boolean charMatchesLowerRight(char char1){ return
	 * (lowerRight.indexOf(char1)!=-1); }
	 * 
	 * private boolean charMatchesLeft(char char1){ return
	 * (left.indexOf(char1)!=-1); }
	 * 
	 * private boolean charMatchesRight(char char1){ return
	 * (right.indexOf(char1)!=-1); }
	 * 
	 * private boolean charMatchesTop(char char1){ return
	 * (top.indexOf(char1)!=-1); }
	 * 
	 * private boolean charMatchesBottom(char char1){ return
	 * (bottom.indexOf(char1)!=-1); }
	 */
	
	private boolean charMatchesWithTitle(char testedChar, String patternChars, int attributes, int borderAttributes, 
											char nextChar, int nextCharAttributes, boolean backward) {
		if (titleAttribute == -1) {
			return charMatches(testedChar, patternChars, attributes, borderAttributes);
		}
		
		if (patternChars == null) {
			return false;
		} 
		if (!spaceBeforeTitle) { // if before title
			if (testedChar == ' ' && nextCharAttributes == titleAttribute) {
				spaceBeforeTitle = true;
				return true;
			}
			return (patternChars.indexOf(testedChar) != -1) && (borderAttributes == attributes);
		} 
		if (spaceAfterTitle) { // if after title
			return (patternChars.indexOf(testedChar) != -1) && (borderAttributes == attributes);
		} 
		// between the two spaces
		if (bTitle) { // if the title is found
			if ( testedChar == ' ' && (patternChars.indexOf(nextChar) != -1 && nextCharAttributes == borderAttributes) ) {
				spaceAfterTitle = true;
				return true;
			} 
			if (attributes == titleAttribute) {
				bTitle = true;
				if (!backward)
					topTitle += testedChar;
				else
					bottomTitle = testedChar + bottomTitle;
				return true;
			}
			return false;
		} 
		// must find a title
		if (attributes == titleAttribute) {
			bTitle = true;
			if (!backward)
				topTitle += testedChar;
			else
				bottomTitle = testedChar + bottomTitle;
			return true;
		}
		return false;
	}
	
	private boolean charMatches(char testedChar, String patternChars, int attributes, int borderAttributes) {
		if (patternChars == null)
			return false;
		else {
			return (patternChars.indexOf(testedChar) != -1) && (borderAttributes == attributes);
		}
	}

//	private boolean charMatches(char char1, String char2) {
//		if (char2 == null)
//			return false;
//		else {
//			return (char2.indexOf(char1) != -1);
//		}
//	}

	private boolean charMatchesAttributesWithTitle(iJavelin javelin, char testedChar, int attributes, boolean checkSpace, 
													char nextChar, int nextCharAttributes, boolean backward) {
		if (titleAttribute == -1 || checkSpace) {
			return charMatchesAttributes(javelin, testedChar, attributes, checkSpace);
		}
		
		if (testedChar == ' ') {
			if (!spaceBeforeTitle) { // if before title
				if (nextChar == ' ') {
					return ( (attributes & Javelin.AT_PAPER) >> 3 == (panelAttribute & Javelin.AT_PAPER) >> 3 );
				}
				if (nextCharAttributes == titleAttribute){
					spaceBeforeTitle = true;
					return true;
				}
				return ((attributes & Javelin.AT_PAPER) >> 3 == (panelAttribute & Javelin.AT_PAPER) >> 3);
			}
			if (spaceAfterTitle) { // if after title
				return ((attributes & Javelin.AT_PAPER) >> 3 == (panelAttribute & Javelin.AT_PAPER) >> 3);
			}
			// between the two spaces
			if (bTitle) { // if the title is found
				if ( nextChar == ' ' && (nextCharAttributes & Javelin.AT_PAPER) >> 3 == (panelAttribute & Javelin.AT_PAPER) >> 3 ) {
					spaceAfterTitle = true;
					return true;
				}
				if (attributes == titleAttribute) {
					bTitle = true;
					if (!backward)
						topTitle += testedChar;
					else
						bottomTitle = testedChar + bottomTitle;
					return true;
				}
				return false;
			}
		}

		// must find a title
		if (attributes == titleAttribute) {
			bTitle = true;
			if (!backward)
				topTitle += testedChar;
			else
				bottomTitle = testedChar + bottomTitle;
			return true;
		}
		return false;
	}

	private boolean charMatchesAttributes(iJavelin javelin, char char1, int attributes, boolean checkSpace) {
		if (checkSpace) {
			if ((char1 == ' ') && ((attributes & Javelin.AT_PAPER) >> 3 == (panelAttribute & Javelin.AT_PAPER) >> 3)) {
				return true;
			}
		} else {
			if (((attributes & Javelin.AT_PAPER) >> 3 == (panelAttribute & Javelin.AT_PAPER) >> 3)) {
				return true;
			}
		}
		return false;
	}

	// this method takes the coordinates of the upper left corner
	// and checks if the panel exists and if it has closed contours
	private boolean checkClosedContours(iJavelin javelin, int col, int line) {
		int curLine = line;
		int curColumn = col;
		PanelCandidate candidate;
		Coordinates UL, UR, LL, LR;
		boolean ULCornerFound = false, URCornerFound = false, LRCornerFound = false, LLCornerFound = false;
		Engine.logBeans.trace("check if the panel has closed contours (called with line=" + curLine + 
							", col=" + curColumn + ")");
		int borderAttributes = javelin.getCharAttribute(curColumn, curLine);
		// we create upperleft coordinate for this panel
		UL = new Coordinates(curColumn, curLine);
		ULCornerFound = true;
		
		// reinitialize booleans for title detection
		spaceBeforeTitle = false;
		spaceAfterTitle = false;
		bTitle = false;
		topTitle = "";
		bottomTitle = "";

		try {
			if (!checkAttributes) {
				// we go right to find the end of the up side
				while (charMatchesWithTitle(javelin.getChar(curColumn, curLine), 
											upperLeft + top + upperRight, 
											javelin.getCharAttribute(curColumn,	curLine), 
											borderAttributes,
											javelin.getChar(curColumn+1, curLine),
											javelin.getCharAttribute(curColumn+1, curLine),
											false
											)) {
					Engine.logBeans.trace("top:" + javelin.getChar(curColumn, curLine) 
									+ "line=" + curLine + ", col=" + curColumn);
					curColumn++;
					URCornerFound = true;
				}
				curColumn--;

				// we create upperRight coordinate for this panel
				UR = new Coordinates(curColumn, curLine);

				// we go down to find the end of the right side
				curLine++;
				while (charMatches(javelin.getChar(curColumn, curLine), 
									right + lowerRight, 
									javelin.getCharAttribute(curColumn,	curLine), 
									borderAttributes
									)) {
					Engine.logBeans.trace("right:" + javelin.getChar(curColumn, curLine) 
									+ "line=" + curLine + ", col=" + curColumn);
					curLine++;
					LRCornerFound = true;
				}
				curLine--;

				// we create lowerRight coordinate for this panel
				LR = new Coordinates(curColumn, curLine);

				// we go left to find the end of the bottom side
				curColumn--;
				while (charMatchesWithTitle(javelin.getChar(curColumn, curLine), 
											bottom + lowerLeft,  
											javelin.getCharAttribute(curColumn,	curLine), 
											borderAttributes,
											javelin.getChar(curColumn-1, curLine),
											javelin.getCharAttribute(curColumn+1, curLine),
											true
											)) {
					Engine.logBeans.trace("bottom:"	+ javelin.getChar(curColumn, curLine) 
									+ "line=" + curLine + ", col=" + curColumn);
					curColumn--;
					LLCornerFound = true;

				}
				curColumn++;

				// we create lowerRight coordinate for this panel
				LL = new Coordinates(curColumn, curLine);

				// we go up to find the end of the left side
				curLine--;
				if (curLine < 0)
					curLine = 0;
				while (charMatches(javelin.getChar(curColumn, curLine), 
									left + upperLeft, 
									javelin.getCharAttribute(curColumn, curLine), 
									borderAttributes
									)) {
					Engine.logBeans.trace("left:" + javelin.getChar(curColumn, curLine) 
									+ "line=" + curLine + ", col=" + curColumn);
					curLine--;
					if (curLine < 0) {
						curLine = -1;
						break;
					}
				}
				curLine++;
				Engine.logBeans.trace("APRES AVOIR FAIT LE TOUR c=" + curColumn + ", l=" + curLine);
			} else {
				// find the contour by checking attributes
				Engine.logBeans.trace("find the contour by checking attributes");
				// we go right to find the end of the up side
				while (charMatchesAttributesWithTitle(	javelin, 
														javelin.getChar(curColumn, curLine), 
														javelin.getCharAttribute(curColumn, curLine), 
														!allowTextInBorders,
														javelin.getChar(curColumn+1, curLine),
														javelin.getCharAttribute(curColumn+1, curLine),
														false
														)) {
					Engine.logBeans.trace("top:" + javelin.getChar(curColumn, curLine) 
									+ "line="	+ curLine + ", col=" + curColumn);
					curColumn++;
					URCornerFound = true;
				}
				curColumn--;

				// we create upperRight coordinate for this panel
				UR = new Coordinates(curColumn, curLine);

				// we go down to find the end of the right side
				curLine++;
				while (charMatchesAttributes(javelin, 
											javelin.getChar(curColumn, curLine), 
											javelin.getCharAttribute(curColumn, curLine), 
											!allowTextInBorders
											)) {
					Engine.logBeans.trace("right:" + javelin.getChar(curColumn, curLine) 
									+ "line=" + curLine + ", col=" + curColumn);
					curLine++;
					LRCornerFound = true;
				}
				curLine--;

				// we create lowerRight coordinate for this panel
				LR = new Coordinates(curColumn, curLine);

				// we go left to find the end of the bottom side
				curColumn--;
				while (charMatchesAttributesWithTitle(	javelin, 
														javelin.getChar(curColumn, curLine), 
														javelin.getCharAttribute(curColumn, curLine), 
														!allowTextInBorders,
														javelin.getChar(curColumn-1, curLine),
														javelin.getCharAttribute(curColumn+1, curLine),
														true
														)) {
					Engine.logBeans.trace("bottom:" + javelin.getChar(curColumn, curLine) 
									+ "line=" + curLine + ", col=" + curColumn);
					curColumn--;
					LLCornerFound = true;
				}
				curColumn++;
				// we create lowerRight coordinate for this panel
				LL = new Coordinates(curColumn, curLine);

				// we go up to find the end of the left side
				curLine--;
				if (curLine < 0)
					curLine = 0;
				while (charMatchesAttributes(javelin, 
											javelin.getChar(curColumn, curLine), 
											javelin.getCharAttribute(curColumn, curLine), 
											!allowTextInBorders
											)) {
					Engine.logBeans.trace("left:" + javelin.getChar(curColumn, curLine) 
									+ "line=" + curLine + ", col=" + curColumn);
					curLine--;
					if (curLine < 0)
						break;
				}
				curLine++;
				Engine.logBeans.trace("APRES AVOIR FAIT LE TOUR c=" + curColumn	+ ", l=" + curLine);
			}

			// calculation of the number of corners found
			int factor = 1;
			if (ULCornerFound && URCornerFound) {
				factor = 2;
				if (LRCornerFound) {
					factor++;
					if (LLCornerFound)
						factor++;
				}
			}
			if (curLine != line || curColumn != col) {
				factor = 3;
			}
			Engine.logBeans.trace("factor of the panel candidate = " + factor);
			candidate = new PanelCandidate(UL, UR, LR, LL, factor, topTitle, bottomTitle);
			if (factor >= minSides)
				listCandidate.add((PanelCandidate) candidate);
			if (curLine == line && curColumn == col) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	private boolean coordinateInExistantPanel(Coordinates c) {
		for (PanelCandidate unPanel : listCandidate) {
			// the point is on the frontier of a panel already found
			if (   (c.line == unPanel.UL.line && c.column >= unPanel.UL.column && c.column <= unPanel.UR.column)
				|| (c.column == unPanel.UR.column && c.line >= unPanel.UR.line && c.line <= unPanel.LR.line)
				|| (c.line == unPanel.LR.line && c.column >= unPanel.UL.column && c.column <= unPanel.LR.column)
				|| (c.column == unPanel.UL.column && c.line >= unPanel.UR.line && c.line <= unPanel.LR.line)
			) {
				// Engine.logBeans.trace("pt: l=" + c.line + ", c=" +c.column +
				// "coordinateInExistantPanel (on the frontier)");
				return true;
			}
			// the point is inside a closed panel already found
			if (unPanel.factor >= minSides
					&& c.column >= unPanel.UL.column
					&& c.column <= unPanel.UR.column
					&& c.line >= unPanel.UL.line
					&& c.line <= unPanel.LL.line) {
				Engine.logBeans.trace(c.printCoordinates() + "Panel beginning inside a closed panel already found");
				return true;
			}
		}
		return false;
	}

	/**
	 * Tests if it is a selection.
	 * 
	 * @return true if it is a selection, false otherwize.
	 */
	private boolean isSelectedPanel() {
		XMLRectangle mySelectionScreenZone = getSelectionScreenZone();
		if ((mySelectionScreenZone.y == -1) && (mySelectionScreenZone.x == -1)
				&& (mySelectionScreenZone.width == -1)
				&& (mySelectionScreenZone.height == -1))
			return false;
		return true;
	}

	/**
	 * Getter for property upperLeft.
	 * 
	 * @return Value of property upperLeft.
	 */
	public String getUpperLeft() {
		return upperLeft;
	}

	/**
	 * Setter for property upperLeft.
	 * 
	 * @param upperLeft
	 *            New value of property upperLeft.
	 */
	public void setUpperLeft(String upperLeft) {
		this.upperLeft = upperLeft;
	}

	/**
	 * Getter for property lowerLeft.
	 * 
	 * @return Value of property lowerLeft.
	 */
	public String getLowerLeft() {
		return lowerLeft;
	}

	/**
	 * Setter for property lowerLeft.
	 * 
	 * @param lowerLeft
	 *            New value of property lowerLeft.
	 */
	public void setLowerLeft(String lowerLeft) {
		this.lowerLeft = lowerLeft;
	}

	/**
	 * Getter for property upperRight.
	 * 
	 * @return Value of property upperRight.
	 */
	public String getUpperRight() {
		return upperRight;
	}

	/**
	 * Setter for property upperRight.
	 * 
	 * @param upperRight
	 *            New value of property upperRight.
	 */
	public void setUpperRight(String upperRight) {
		this.upperRight = upperRight;
	}

	/**
	 * Getter for property lowerRight.
	 * 
	 * @return Value of property lowerRight.
	 */
	public String getLowerRight() {
		return lowerRight;
	}

	/**
	 * Setter for property lowerRight.
	 * 
	 * @param lowerRight
	 *            New value of property lowerRight.
	 */
	public void setLowerRight(String lowerRight) {
		this.lowerRight = lowerRight;
	}

	/**
	 * Getter for property top.
	 * 
	 * @return Value of property top.
	 */
	public String getTop() {
		return top;
	}

	/**
	 * Setter for property top.
	 * 
	 * @param top
	 *            New value of property top.
	 */
	public void setTop(String top) {
		this.top = top;
	}

	/**
	 * Getter for property left.
	 * 
	 * @return Value of property left.
	 */
	public String getLeft() {
		return left;
	}

	/**
	 * Setter for property left.
	 * 
	 * @param left
	 *            New value of property left.
	 */
	public void setLeft(String left) {
		this.left = left;
	}

	/**
	 * Getter for property right.
	 * 
	 * @return Value of property right.
	 */
	public String getRight() {
		return right;
	}

	/**
	 * Setter for property right.
	 * 
	 * @param right
	 *            New value of property right.
	 */
	public void setRight(String right) {
		this.right = right;
	}

	/**
	 * Getter for property bottom.
	 * 
	 * @return Value of property bottom.
	 */
	public String getBottom() {
		return bottom;
	}

	/**
	 * Setter for property bottom.
	 * 
	 * @param bottom
	 *            New value of property bottom.
	 */
	public void setBottom(String bottom) {
		this.bottom = bottom;
	}

	public void setAllowTextInBorders(boolean flag) {
		allowTextInBorders = flag;
	}

	public boolean isAllowTextInBorders() {
		return allowTextInBorders;
	}

	public int getMinSides() {
		return minSides;
	}

	public void setMinSides(int minSides) {
		this.minSides = minSides;
	}

	public boolean isRemoveBlocksInBorder() {
		return removeBlocksInBorder;
	}

	public void setRemoveBlocksInBorder(boolean removeBlocksInBorder) {
		this.removeBlocksInBorder = removeBlocksInBorder;
	}

	public int getTitleAttribute() {
		return titleAttribute;
	}

	public void setTitleAttribute(int titleAttribute) {
		this.titleAttribute = titleAttribute;
	}

	
}
