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

import java.io.Serializable;
import java.util.Vector;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.twinj.iJavelin;

public class Menu extends JavelinExtractionRule {

	private static final long serialVersionUID = 6241962841861143908L;

	/** Holds value of property keywordSeparator. */
	private String keywordSeparator = ".-";
	private final String colors[] = { "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white" };

	class Item implements Serializable {

		private static final long serialVersionUID = -3965263987133870659L;

		transient Block block;
		transient String id;
		transient char separator;
		transient int positionSeparator;
		transient String command;

		public Item(Block block, String id, char separator, int positionSeparator, String command) {
			this.block = block;
			this.id = id;
			this.separator = separator;
			this.positionSeparator = positionSeparator;
			this.command = command;			
		}

		public boolean IsSeparatorAligned(Item it2) {
			int dif = this.positionSeparator - it2.positionSeparator;
			
			if ((dif == 0) || (dif == -1) || (dif == 1))
				return true;
						
			return false;
		}
	}

	public Menu() {
		super();
	}

	transient private Block	curBlock;
	transient private Vector<Item>	listItem;

	/*
	 * private String findNonEmptyBlockOnTheSameLine(BlockFactory bf) { String
	 * res; int line; Block tempBlock;
	 * 
	 * line = curBlock.line; tempBlock = curBlock; while (((curBlock =
	 * bf.getNextBlock(curBlock)) != null) && curBlock.line == line) { if
	 * (curBlock.length != 0) { res = curBlock.getText();
	 * bf.removeBlock(curBlock); curBlock = tempBlock; return (res); } }
	 * curBlock = tempBlock; return null; }
	 */

	/**
	 * Applies the extraction rule to the current iJavelin object.
	 * 
	 * @param javelin
	 *            the Javelin object.
	 * @param block
	 *            the current block to analyze.
	 * @param blockFactory
	 *            the block context of the current block.
	 * 
	 * @return an ExtractionRuleResult object containing the result of the
	 *         query.
	 */
	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		String text;
		int index;
		int separatorCharactersLength = keywordSeparator.length();
		String itemID;
		String literal;

		Block blockFirst = block;
		Block blockAfter = block;

		org.w3c.dom.Element menu;
		org.w3c.dom.Element item;
		listItem = new Vector<Item>();

		// Applies only on non empty blocks
		text = block.getText();
		if (text.length() == 0) {
			xrs.hasMatched = false;
			xrs.newCurrentBlock = block;
			return (xrs);
		}

		// A 5250 menu is in the form :
		// <NUM>.<SPACE><Literal> .... <command>
		// test is the first item of the block is a digit.
		if (!Character.isDigit(text.charAt(0))) {
			xrs.hasMatched = false;
			xrs.newCurrentBlock = block;
			return (xrs);
		}

		// yes it is so scan to get the [separator]
		boolean found = false;
		int positionSeparator = -1;
		index = 0;

		for (int i = 0; i < separatorCharactersLength; i++) {
			index = text.indexOf(keywordSeparator.charAt(i));
			
			if (index != -1) {
				positionSeparator = index;
				itemID = text.substring(0, index).trim();
				literal = text.substring(index + 1).trim();
				
				if (!isDigits(itemID))
					break;
				
				if (isDigits(literal))
					break;

				listItem.add(new Item(block, itemID, keywordSeparator.charAt(i), positionSeparator, literal));
				
				Engine.logBeans.trace("'" + keywordSeparator.charAt(i) + "' found, registering menu item [" + itemID + "][" + literal + "]");
				
				curBlock = block;
				blockFirst = block;
				found = true;
				
				break;	// because found is true  by jmc 13/07/2010
			}
		}

		if (!found) {
			xrs.hasMatched = false;
			xrs.newCurrentBlock = block;
			return (xrs);
		}

		// we found a matching menu item scan all other
		// blocks to get other menu items
		while ((curBlock = blockFactory.getNextBlock(curBlock)) != null) {
			if (!this.canBlockBeSelected(curBlock))
				continue;

			// Applies only on non empty blocks
			text = curBlock.getText();
			if (text.length() == 0)
				continue;

			// A 5250 menu is in the form :
			// <NUM>.<SPACE><Literal>
			// test is the first item of the block is a digit.
			if (!Character.isDigit(text.charAt(0)))
				continue;

			// yes it is so scan to get the [separator]
			index = 0;
			for (int i = 0; i < separatorCharactersLength; i++) {
				index = text.indexOf(keywordSeparator.charAt(i));
				if (index != -1) {
					// if the position of the separator is the same that the
					// separator of the previous item (=, +1, -1)
					if ((positionSeparator == index) || (positionSeparator == index - 1) || (positionSeparator == index + 1)) {
						itemID = text.substring(0, index).trim();
						literal = text.substring(index + 1).trim();
						
						Engine.logBeans.trace(itemID);
											
						if (!isDigits(itemID)) {
							Engine.logBeans.trace("1" + text);
							break;
						}
						
						if (isDigits(literal)) {
							Engine.logBeans.trace("2" + text);
							break;
						}
						
						listItem.add(new Item(curBlock, itemID, keywordSeparator.charAt(i), positionSeparator, literal));
						
						Engine.logBeans.trace("'" + keywordSeparator.charAt(i) + "' found, registering menu item[" + itemID + "][" + literal + "]");
						
						blockAfter = blockFactory.getNextBlock(curBlock);
					
						break;	// because found is true  by jmc 13/07/2010
					}

				}
			}
		}

		if (listItem.size() >= 2) {
			// create the menu element.
			menu = dom.createElement("menu");
			Block menublock = blockFirst;
			menublock.type = "snamenu";
			menublock.setText("");

			Item i1 = listItem.get(0);
			Item i2 = listItem.get(1);
			
			if (i1.IsSeparatorAligned(i2)) {
				block = i1.block;
				itemID = i1.id.trim();
				literal = i1.command;

				// create the first item in the menu
				item = dom.createElement("menuitem");
				item.setAttribute("type", "menuitem");
				item.setAttribute("id", itemID);
				item.setAttribute("literal", literal);
				item.setAttribute("line", "" + block.line);
				item.setAttribute("column", "" + block.column);

				int attribute = block.attribute;
				if ((attribute & iJavelin.AT_INVERT) == 0) {
					item.setAttribute("reverse", "false");
					item.setAttribute("foreground", colors[attribute & iJavelin.AT_INK]);
					item.setAttribute("background", colors[(attribute & iJavelin.AT_PAPER) >> 3]);
				} else {
					item.setAttribute("reverse", "true");
					item.setAttribute("foreground", colors[(attribute & iJavelin.AT_PAPER) >> 3]);
					item.setAttribute("background", colors[attribute & iJavelin.AT_INK]);
				}

				// place the menuitem in the menu object
				menu.appendChild(item);
				menublock.addOptionalChildren(menu);
				Engine.logBeans.trace("SnaMenu extraction rule ,item id : " + itemID + " literal[" + literal + "]");
				menublock = block;
			}
			
			for (int i = 1; i < listItem.size(); i++) {
				i1 = listItem.get(i - 1);
				i2 = listItem.get(i);
				
				if (i1.IsSeparatorAligned(i2)) {
					curBlock = i2.block;
					itemID = i2.id;
					literal = i2.command;
				
					// create the first item in the menu
					item = dom.createElement("menuitem");
					item.setAttribute("type", "menuitem");
					item.setAttribute("id", itemID);
					item.setAttribute("literal", literal);
					item.setAttribute("line", "" + curBlock.line);
					item.setAttribute("column", "" + curBlock.column);

					int attribute = curBlock.attribute;
					
					if ((attribute & iJavelin.AT_INVERT) == 0) {
						item.setAttribute("reverse", "false");
						item.setAttribute("foreground", colors[attribute & iJavelin.AT_INK]);
						item.setAttribute("background", colors[(attribute & iJavelin.AT_PAPER) >> 3]);
					} else {
						item.setAttribute("reverse", "true");
						item.setAttribute("foreground", colors[(attribute & iJavelin.AT_PAPER) >> 3]);
						item.setAttribute("background", colors[attribute & iJavelin.AT_INK]);
					}

					blockFactory.removeBlock(curBlock);
					// place the menuitem in the menu object
					menu.appendChild(item);
					menublock.addOptionalChildren(menu);
					Engine.logBeans.trace("SnaMenu extraction rule, item id : " + itemID + " literal[" + literal + "]");
				} else {
					itemID = i2.id;
					literal = i2.command;
					Engine.logBeans.trace("Not aligned, item id : " + itemID + " literal[" + literal + "]");
				}
			}
			xrs.hasMatched = true;
			xrs.newCurrentBlock = blockAfter;
			return (xrs);
		} else {
			Engine.logBeans.debug("SnaMenu extraction rule : not enough item to build a menu (" + listItem.size() + " item(s) found)");
			xrs.hasMatched = false;
			xrs.newCurrentBlock = blockAfter;
			return (xrs);
		}
	}

	private boolean isDigits(String st) {
		int i = 0;
		if (st.length() == 0)
			return false;

		while (i < st.length()) {
			if ((st.charAt(i) != ',') && (st.charAt(i) != ' ')
					&& (st.charAt(i) != '.') && (st.charAt(i) != '-')
					&& (st.charAt(i) != '+') && !Character.isDigit(st.charAt(i)))
				return false;
			i++;
		}
		
		return true;
	}

	/**
	 * Getter for property keywordSeparator.
	 * 
	 * @return Value of property keywordSeparator.
	 */
	public String getKeywordSeparator() {
		return keywordSeparator;
	}

	/**
	 * Setter for property keywordSeparator.
	 * 
	 * @param keywordSeparator
	 *            New value of property keywordSeparator.
	 */
	public void setKeywordSeparator(String keywordSeparator) {
		this.keywordSeparator = keywordSeparator;
	}
}
