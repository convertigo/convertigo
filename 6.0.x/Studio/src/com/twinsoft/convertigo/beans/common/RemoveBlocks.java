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

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.twinj.iJavelin;

// TODO: propriété tagName à mettre en reg exp ?

public class RemoveBlocks extends JavelinExtractionRule {

	public static final long serialVersionUID = -6735666205939933877L;
    
    /**
     * The regular expression object.
     */
    transient protected RE regexp;
    
    /** Holds value of property regularExpression. */
    private String regularExpression = "";
    
    /** Holds value of property length. */
    private int length = -1;
    
    /** Holds value of property blockTag. */
    private String blockTag = "";
    
    /** Creates new RemoveBlankBlock */
    public RemoveBlocks() {
        super();
    }
    
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
    protected JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
        Block curBlock;
        Block tempBlock;
        int blockLen;
        
        xrs.hasMatched = false;
        
		// If the object has just been deserialized, we must
		// recreate the regular expression object.
		if (regexp == null) {
			try {
				Engine.logBeans.debug("Setting regular expression to : " + regularExpression);
				setRegularExpression(regularExpression);
			}
			catch (RESyntaxException e) {
				Engine.logBeans.error("Unable to create the regular expression object", e);
			}
		}

        curBlock = block;
        do {
            tempBlock = blockFactory.getNextBlock(curBlock);
            if (canBlockBeSelected(curBlock)) {
                if ((blockTag.length() == 0) || blockTag.equalsIgnoreCase(curBlock.tagName)) {
                    blockLen = curBlock.getText().trim().length();
                    if ((length == -1) || (blockLen == length)) {
						// Avoid to try to match on empty strings
                        if (blockLen != 0 ) {
                            if (regexp.match(curBlock.getText())) {
                                blockFactory.removeBlock(curBlock);
                                xrs.hasMatched = true;
                            }
                        }
                        else {
                            blockFactory.removeBlock(curBlock);
                            xrs.hasMatched = true;
                        }
                    }
                }
            }
            if (tempBlock == null)
                break;
            
            curBlock = tempBlock;
        } while (true);
        
        xrs.newCurrentBlock = curBlock;
        return xrs;
    }
    
    /** Getter for property regularExpression.
     * @return Value of property regularExpression.
     */
    public String getRegularExpression() {
        return regularExpression;
    }
    
    /** Setter for property regularExpression.
     * @param regularExpression New value of property regularExpression.
     */
    public void setRegularExpression(String regularExpression) throws RESyntaxException {
        regexp = new RE(regularExpression);
        this.regularExpression = regularExpression;
    }
    
    /** Getter for property length.
     * @return Value of property length.
     */
    public int getLength() {
        return length;
    }
    
    /** Setter for property length.
     * @param length New value of property length.
     */
    public void setLength(int length) {
        this.length = length;
    }
    
    /** Getter for property blockTag.
     * @return Value of property blockTag.
     */
    public String getBlockTag() {
        return blockTag;
    }
    
    /** Setter for property blockTag.
     * @param blockTag New value of property blockTag.
     */
    public void setBlockTag(String blockTag) {
        this.blockTag = blockTag;
    }
    
    /**
     * Performs custom configuration for this database object.
     */
    public void configure(Element element) throws Exception {
        super.configure(element);
        
        String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException("Unable to find version number for the database object \"" + getName() + "\".\nXML data: " + s);
            throw ee;
        }

        if (VersionUtils.compare(version, "1.2.2") <= 0) {
            NodeList properties = element.getElementsByTagName("property");

            Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "removeFields");

            Node xmlNode = null;
            NodeList nl = propValue.getChildNodes();
            int len_nl = nl.getLength();
            Boolean bRemoveFields = null;
            for (int j = 0 ; j < len_nl ; j++) {
                xmlNode = nl.item(j);
                if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
                    bRemoveFields = (Boolean) XMLUtils.readObjectFromXml((Element) xmlNode);
                    continue;
                }
            }

            setSelectionType((bRemoveFields == null) ? "[^(field)]" : (bRemoveFields.booleanValue() ? "" : "[^(field)]"));

            hasChanged = true;
            Engine.logBeans.warn("[RemoveBlocks] The object \"" + getName() + "\" has been updated to version 1.2.3");
        }
    }
}
