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

import java.util.StringTokenizer;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.twinj.iJavelin;

/**
 * This class manages command for the SNA Emulator,
 * it extends the com.twinsoft.convertigo.beans.common.Command class.
 * This class checks if the begining of the text of a block matches a keyword.
 * It can also automatically replace the text of the block.
 * For example, on an IBM SNA Emulator we often found this kind of command :
 *   - PF10=Menu
 *   - F9=Quit, etc...
 * In this class you can specify keyword separators (default is space, : and =)
 * if a command PF10=Menu is found, it will automatcally replace the text with "Menu"
 * the "action" and "data" attribute are handled normally
 * if a replacement text is specified it uses it and not the automatic replacement text
 */
public class CommandSNA extends com.twinsoft.convertigo.beans.common.Command {

	public static final long serialVersionUID = 4350644982787619412L;
    
    /** Holds value of property keywordSeparator. */
    private String 	keywordSeparator = " =:";

    public static final int LABEL_LOCATION_WEST = 0;
    public static final int LABEL_LOCATION_NORTH = 1;
    public static final int LABEL_LOCATION_SOUTH = 2;
	public static final int LABEL_LOCATION_EAST = 3;
    
    /** Holds value of property labelLocation.*/
    private int	labelLocation = CommandSNA.LABEL_LOCATION_EAST;
    
    /** Holds value of property separatorMendatory.*/
    private boolean separatorMendatory = false;
    
   
	/** Setter for property labelLocation.
	 * @param labelLocation New value of property labelLocation.
	 */
	public void setLabelLocation(int labelLocation) {
		this.labelLocation = labelLocation;
	}
	/** Getter for property labelLocation.
	 * @return Value of property caseDependency.
	 */
	public int getLabelLocation() {
		return labelLocation;
	}
    

    public CommandSNA() {
        super();
        
        addNewKeyword("PA1", "", "", "KEY_PA1");
        addNewKeyword("PA2", "", "", "KEY_PA2");
        addNewKeyword("PA3", "", "", "KEY_PA3");
        addNewKeyword("PF10", "", "", "KEY_PF10");
        addNewKeyword("PF11", "", "", "KEY_PF11");
        addNewKeyword("PF12", "", "", "KEY_PF12");
        addNewKeyword("PF13", "", "", "KEY_PF13");
        addNewKeyword("PF14", "", "", "KEY_PF14");
        addNewKeyword("PF15", "", "", "KEY_PF15");
        addNewKeyword("PF16", "", "", "KEY_PF16");
        addNewKeyword("PF17", "", "", "KEY_PF17");
        addNewKeyword("PF18", "", "", "KEY_PF18");
        addNewKeyword("PF19", "", "", "KEY_PF19");
        addNewKeyword("PF20", "", "", "KEY_PF20");
        addNewKeyword("PF21", "", "", "KEY_PF21");
        addNewKeyword("PF22", "", "", "KEY_PF22");
        addNewKeyword("PF23", "", "", "KEY_PF23");
        addNewKeyword("PF24", "", "", "KEY_PF24");
        addNewKeyword("PF1", "", "", "KEY_PF1");
        addNewKeyword("PF2", "", "", "KEY_PF2");
        addNewKeyword("PF3", "", "", "KEY_PF3");
        addNewKeyword("PF4", "", "", "KEY_PF4");
        addNewKeyword("PF5", "", "", "KEY_PF5");
        addNewKeyword("PF6", "", "", "KEY_PF6");
        addNewKeyword("PF7", "", "", "KEY_PF7");
        addNewKeyword("PF8", "", "", "KEY_PF8");
        addNewKeyword("PF9", "", "", "KEY_PF9");
        addNewKeyword("F10", "", "", "KEY_PF10");
        addNewKeyword("F11", "", "", "KEY_PF11");
        addNewKeyword("F12", "", "", "KEY_PF12");
        addNewKeyword("F13", "", "", "KEY_PF13");
        addNewKeyword("F14", "", "", "KEY_PF14");
        addNewKeyword("F15", "", "", "KEY_PF15");
        addNewKeyword("F16", "", "", "KEY_PF16");
        addNewKeyword("F17", "", "", "KEY_PF17");
        addNewKeyword("F18", "", "", "KEY_PF18");
        addNewKeyword("F19", "", "", "KEY_PF19");
        addNewKeyword("F20", "", "", "KEY_PF20");
        addNewKeyword("F21", "", "", "KEY_PF21");
        addNewKeyword("F22", "", "", "KEY_PF22");
        addNewKeyword("F23", "", "", "KEY_PF23");
        addNewKeyword("F24", "", "", "KEY_PF24");
        addNewKeyword("F1", "", "", "KEY_PF1");
        addNewKeyword("F2", "", "", "KEY_PF2");
        addNewKeyword("F3", "", "", "KEY_PF3");
        addNewKeyword("F4", "", "", "KEY_PF4");
        addNewKeyword("F5", "", "", "KEY_PF5");
        addNewKeyword("F6", "", "", "KEY_PF6");
        addNewKeyword("F7", "", "", "KEY_PF7");
        addNewKeyword("F8", "", "", "KEY_PF8");
        addNewKeyword("F9", "", "", "KEY_PF9");
    }
    
    /** Getter for property keywordSeparator.
     * @return Value of property keywordSeparator.
     */
    public String getKeywordSeparator() {
        return keywordSeparator;
    }
    
    /** Setter for property keywordSeparator.
     * @param keywordSeparator New value of property keywordSeparator.
     */
    public void setKeywordSeparator(String keywordSeparator) {
        this.keywordSeparator = keywordSeparator;
    }
    
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;

        String  	textBlock 	= block.getText().trim();
        int 		len 		= block.length;
        XMLVector<XMLVector<String>> 	keywordTable = getKeywordTable();
        
        // test if the current block matches a keyword
        for (int i = 0; i < keywordTable.size(); i++) {
        	XMLVector<String> keyword = keywordTable.get(i);
            String textKeyword = keyword.get(0);
            String dataKeyword = keyword.get(1);
            String replaceTextKeyword = keyword.get(2);
            String actionKeyword = keyword.get(3);
            
            if (labelLocation != CommandSNA.LABEL_LOCATION_WEST){
            	// key is the first element ex : PF3=EXIT
	            if (textBlock.startsWith(textKeyword) || (textBlock.toLowerCase().startsWith(textKeyword.toLowerCase()) && !isCaseDependency())) {
	            	// block matches
	            	
	            	if (labelLocation == CommandSNA.LABEL_LOCATION_EAST) {
	            		Engine.logBeans.debug("LabelLocation = east");
	            		
	            		// look for the original label if a keyword separator is specified
	            		if (keywordSeparator.length() != 0) {
		                    int keywordLen = textKeyword.length();
		                    boolean separatorFound = false;
	                    	String oldLabel = "";
//	                    	String keywordFound = textBlock.substring(0, keywordLen);
	                    	
		                    if (textBlock.length() > keywordLen + 1){
		                    	// The label should be in the same block
		                    	
		                    	// look for the separator char
		                        for (int j = 0 ; j < keywordSeparator.length() ; j++){
		                            if (textBlock.charAt(keywordLen) == keywordSeparator.charAt(j)) {
		                            	separatorFound = true;
		                            	oldLabel = textBlock.substring(keywordLen + 1);
		                            }
		                        }
		                        
		                        if (separatorFound) {
		                        	// try to parse old label to see if the keyword is the only one of the block
		                        	int index = -1;
		                        	for (int k = 0 ; k < keywordTable.size() ; k++) {
		                        		XMLVector<String> keywordBis = keywordTable.get(k);
		                                String textKeywordBis = keywordBis.get(0);
		                                if (oldLabel.indexOf(textKeywordBis) != -1 && (oldLabel.indexOf(textKeywordBis) < index || index == -1))
		                                	index = oldLabel.indexOf(textKeywordBis);
		                        	}
		                        	
		                        	if (index != -1) {
		                        		// another keyword is found in the label : separate in 2 blocks
		                        		Block tmp;
										try {
											tmp = (Block) block.clone();
										} catch (CloneNotSupportedException e) {
											Engine.logBeans.error("Exception thrown during block clone", e);
											tmp = new Block();
										}
		                        		block.setText(oldLabel.substring(0, index));
		                        		tmp.setText(oldLabel.substring(index));
		                        		tmp.column = tmp.column + index + keywordLen+1;
		                        		blockFactory.insertBlock(tmp, block);
		                        	} else {
		                        		block.setText(oldLabel);
		                        	}
		                        } else {
		                        	if (separatorMendatory) {
		                        		xrs.hasMatched = false;
		            	                xrs.newCurrentBlock = block;
		            	                return (xrs);
		                        	}
		                        }
		                    } else {
								// The label should be in the east block
		                    	
								// Searching the separator char in the current block
								if ( textBlock.length() == keywordLen+1 ) {
									for (int j = 0 ; j < keywordSeparator.length() ; j++) {
										if (textBlock.charAt(keywordLen) == keywordSeparator.charAt(j))
											// Found the separator char at the end of the current block
											separatorFound = true;
									}
								}

								Block eastBlock = blockFactory.getEastBlock(block);
								if (eastBlock != null && eastBlock.getText().length() != 0) {
									if (separatorFound) {
										// east block contains label
										oldLabel = eastBlock.getText().trim();
									} else {
										// Searching the separator char in the east block
										String eastBlockText = eastBlock.getText().trim();
										for (int j = 0 ; j < keywordSeparator.length() ; j++) {
											if (!eastBlockText.equals("") && eastBlockText.charAt(0) == keywordSeparator.charAt(j)) { 
												// Found the separator char at the beginning of the east block
												separatorFound = true;
												if (eastBlockText.length() > 1)
													oldLabel = eastBlockText.substring(1).trim();
											}
										}
									}
									if (separatorFound) {
										// try to parse old label to see if the keyword is the only one of the block
			                        	int index = -1;
			                        	for (int k = 0 ; k < keywordTable.size() ; k++) {
			                        		XMLVector<String> keywordBis = keywordTable.get(i);
			                                String textKeywordBis = keywordBis.get(0);
			                                if (oldLabel.indexOf(textKeywordBis) != -1 && oldLabel.indexOf(textKeywordBis) < index)
			                                	index = oldLabel.indexOf(textKeywordBis);
			                        	}
			                        	
			                        	if (index != -1) {
			                        		// another keyword is found in the label : 
			                        		// remove the first label from this east block and put it in the first block
			                        		block.setText(oldLabel.substring(0, index));
			                        		eastBlock.setText(oldLabel.substring(index));
			                        	}	
									} else {
										if (separatorMendatory) {
			                        		xrs.hasMatched = false;
			            	                xrs.newCurrentBlock = block;
			            	                return (xrs);
			                        	}
									}
								}
		                    }
	            		}
	            		
	            		// if replace text is specified
		                if (replaceTextKeyword.length() != 0){
		                	// replace the text with the specified one
			                // but keep the original size
		                	int originalLen = block.length;
		                    block.setText(replaceTextKeyword);
		                    block.length = originalLen;
		                }	
		                
	            	} else if (labelLocation == CommandSNA.LABEL_LOCATION_NORTH) {
	            		Engine.logBeans.debug("LabelLocation = north");
						
	            		// if replace text is specified
		                if (replaceTextKeyword.length() != 0){
		                	// replace the text with the specified one
			                // but keep the original size
		                	int originalLen = block.length;
		                    block.setText(replaceTextKeyword);
		                    block.length = originalLen;
		                } else {
		                	// look for label in north block
		            		Block northBlock = blockFactory.getNorthBlock(block);
							if (northBlock != null && northBlock.line == block.line-1 && northBlock.getText().length() != 0) {
								block.setText(northBlock.getText()); //setText changes the length!
								block.length = len;
								blockFactory.removeBlock(northBlock);
							}
						}
	            	} else if (labelLocation == CommandSNA.LABEL_LOCATION_SOUTH) {
	            		Engine.logBeans.debug("LabelLocation = south");
	            		
	            		// if replace text is specified
		                if (replaceTextKeyword.length() != 0){
		                	// replace the text with the specified one
			                // but keep the original size
		                	int originalLen = block.length;
		                    block.setText(replaceTextKeyword);
		                    block.length = originalLen;
		                } else {
		                	// look for label in south block
		            		Block southBlock = blockFactory.getSouthBlock(block);
							if (southBlock!=null && southBlock.line == block.line+1 && southBlock.getText().length()!=0) {
								block.setText(southBlock.getText()); //setText changes the length!
								block.length = len;
								blockFactory.removeBlock(southBlock);
							}
		                }
	            	}
	            	
	            	// set block's type to "keyword"
	                block.type = "keyword";
	                
	                // "data" attribute
	                if (dataKeyword.length() != 0) {
	                	// if data is specified for this keyword, set the "data" attribute 
	                	block.setOptionalAttribute("data", dataKeyword);
	                } else if (block.getOptionalAttribute("data") == null) {
	                	// else, if the attribute doesn't exist, set to "" 
	                    block.setOptionalAttribute("data", "");
	                } 	// otherwise it keeps the old value
	                
	                // "action" attribute
	                if (actionKeyword.length() != 0) {
	                	// if action is specified, set the "action" attribute
	                	block.setOptionalAttribute("action", actionKeyword);
	                } else if (block.getOptionalAttribute("action") == null) {
	                	// else, if the attribute doesn't exist, set to "" 
	                    block.setOptionalAttribute("action", "");
	                }	// otherwise it keeps the old value
	                
	                addMashupAttribute(block);
	                
	                xrs.hasMatched = true;
	                xrs.newCurrentBlock = block;
	                return (xrs);
	            }
            
            } else {
            	// label is the first element ex : EXIT=PF3
				if (textBlock.endsWith(textKeyword) || (textBlock.toLowerCase().endsWith(textKeyword.toLowerCase()) && !isCaseDependency())) {
					// set its type to "keyword"
					block.type = "keyword";
	                
					// set the "data" attribute
					if (dataKeyword.length() != 0) {
						block.setOptionalAttribute("data", dataKeyword);
					}
					// else set to "" if the attribute doesn't exist
					// otherwise it keeps the old value
					else if (block.getOptionalAttribute("data") == null)
						block.setOptionalAttribute("data", "");
	                
					// replace the text with the specified one
					// keep the original size
					if (replaceTextKeyword.length() != 0){
						block.setText(replaceTextKeyword);
						block.length = len;
					}
					// else use the text BEFORE the separator character
					else { 
						if (textBlock.length() > textKeyword.length() + 1) {
							if (keywordSeparator.length() != 0) {
								StringTokenizer st = new StringTokenizer(textBlock, keywordSeparator);
								block.setText(st.nextToken());
								block.length = len;
							}
						} else {
							// Searching the label in the west block
							boolean separatorCharFound = false;
							int keywordLen = textKeyword.length();
		                    	
							// Searching the separator char in the current block
							if ( textBlock.length() == keywordLen+1 )
								for (int j = 0; j < keywordSeparator.length(); j++)
									if (textBlock.charAt(0) == keywordSeparator.charAt(j))
										// Found the separator char at the beginning of the current block
										separatorCharFound=true;

							Block westBlock = blockFactory.getWestBlock(block);

							if (westBlock != null) {
								// Searching the separator char in the west block
								String westBlockText = westBlock.getText().trim();
								for (int j = 0; j < keywordSeparator.length(); j++)
									if (!westBlockText.equals("") && westBlockText.charAt(westBlockText.length()-1) == keywordSeparator.charAt(j)) { 
										// Found the separator char at the end of the east block
										separatorCharFound=true;
										if (westBlockText.length() > 1)
											westBlockText = westBlockText.substring(0,westBlockText.length()-1).trim();
									}
	
								if (westBlock != null && westBlock.getText().length()!=0 && separatorCharFound) {
									block.setText(westBlockText); //setText changes the length!
									block.length = len;
									blockFactory.removeBlock(westBlock);
								}
							}
						}
					}
	                
					// set the "action" attribute
					if (actionKeyword.length() != 0) {
						block.setOptionalAttribute("action", actionKeyword);
					}
					// else set to "" if the attribute doesn't exist
					// otherwise it keeps the old value
					else if (block.getOptionalAttribute("action") == null)
						block.setOptionalAttribute("action", "");
	                
					addMashupAttribute(block);
					
					xrs.hasMatched = true;
					xrs.newCurrentBlock = block;
					return (xrs);
				}
            }
        }
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        return xrs;
    }

	/**
	 * Performs custom configuration for this database object.
	 */
	public void configure(Element element) throws Exception {
		super.configure(element);
        
		String version = element.getAttribute("version");
        
		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException(
				"Unable to find version number for the database object \"" + getName() + "\".\n" +
				"XML data: " + s
			);
			throw ee;
		}

		if (VersionUtils.compare(version, "2.1.0") <= 0) {
			NodeList properties = element.getElementsByTagName("property");
			Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "labelFirst");

			if (propValue == null) {
				labelLocation = CommandSNA.LABEL_LOCATION_EAST;
			}

			hasChanged = true;
			Engine.logBeans.warn("[CommandSNA] The object \"" + getName() + "\" has been updated to version 2.1.1");
		}

		if (VersionUtils.compare(version, "2.1.0") <= 0) {
			NodeList properties = element.getElementsByTagName("property");

			// attribute -> selectionAttribute
			Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "labelFirst");

			if (propValue != null) {
				boolean bLabelFirst = false;
				Node xmlNode = null;
				NodeList nl = propValue.getChildNodes();
				int len_nl = nl.getLength();
				for (int j = 0 ; j < len_nl ; j++) {
					xmlNode = nl.item(j);
					if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
						bLabelFirst = new Boolean(XMLUtils.readObjectFromXml((Element) xmlNode).toString()).booleanValue();
						continue;
					}
				}

				labelLocation = bLabelFirst ? CommandSNA.LABEL_LOCATION_WEST
											: CommandSNA.LABEL_LOCATION_EAST;
			}

			hasChanged = true;
			Engine.logBeans.warn("[CommandSNA] The object \"" + getName() + "\" has been updated to version 2.1.1");
		}
	}
	public boolean isSeparatorMendatory() {
		return separatorMendatory;
	}
	public void setSeparatorMendatory(boolean separatorMendatory) {
		this.separatorMendatory = separatorMendatory;
	}
	
    
}
