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

package com.twinsoft.convertigo.beans.extractionrules;

import com.twinsoft.twinj.iJavelin;
import com.twinsoft.convertigo.engine.*;
import com.twinsoft.convertigo.engine.util.*;
import com.twinsoft.convertigo.beans.common.*;
import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.ExtractionRule;

import org.w3c.dom.*;
import org.apache.regexp.*;

/**
 * This class defines the base class for extraction rules.
 *
 * <p>An extraction rule is seen as a JavaBean. So it is serializable,
 * and can have its own properties editor.</p>
 */
public abstract class JavelinExtractionRule extends ExtractionRule {
	
	private static final long serialVersionUID = 842946109283937291L;

	public static final int DONT_CARE_FOREGROUND_ATTRIBUTE = 0x10000;
	public static final int DONT_CARE_BACKGROUND_ATTRIBUTE = 0x20000;
	public static final int DONT_CARE_REVERSE_ATTRIBUTE = 0x40000;
	public static final int DONT_CARE_INTENSE_ATTRIBUTE = 0x80000;
	public static final int DONT_CARE_UNDERLINED_ATTRIBUTE = 0x100000;
	public static final int DONT_CARE_BLINK_ATTRIBUTE = 0x200000;

    /**
     * Specifies the screenzone valid for applying this extraction rule any block not
     * in this selectionScreenZone will not be handled by the rules.
     */
    private XMLRectangle selectionScreenZone = new XMLRectangle(-1, -1, -1, -1);
    
    /**
     * Determines if the extration rule is final or not, i.e. if
     * the rules application algorithm must continue if the rule
     * matchs or must stop and pass on the next block.
     */
    private boolean isFinal = false;
    
    /**
     * Determines if the extration rule is final or not.
     *
     * @return <code>true</code> if the extration rule is final,
     * <code>false</code> otherwise.
     */
    public boolean isFinal() {
        return isFinal;
    }
    
    /**
     * Determines if the extration rule is final or not.
     *
     * @return <code>true</code> if the extration rule is final,
     * <code>false</code> otherwise.
     */
    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }
    
    /**
     * Constructs a new ExtractionRule object.
     */
    public JavelinExtractionRule() {
        super();
    }
    
    /**
     * Tests if a block can be selected for being executing.
     *
     * @param block the block to be tested
     *
     * @return true if the block can be selected, false otherwize.
     */
    public boolean canBlockBeSelected(Block block) {
    	// If the block has been marked as final by a previous extraction rule, skip it
    	if (block.bFinal) {
    		return false;
    	}
    	
    	// Zone checks
        if ((selectionScreenZone.y != -1) && (block.line < selectionScreenZone.y)) {
			Engine.logBeans.trace("Block not selected because of wrong selection screen zone (line out of range)");
			return false;
        }
        if ((selectionScreenZone.x != -1) && (block.column < selectionScreenZone.x)) {
			Engine.logBeans.trace("Block not selected because of wrong selection screen zone (column out of range)");
			return false;
        }
        if ((selectionScreenZone.width != -1) && ((block.length + block.column) > (selectionScreenZone.x + selectionScreenZone.width))) {
			Engine.logBeans.trace("Block not selected because of wrong selection screen zone (width out of range)");
			return false;
        }
        if ((selectionScreenZone.height != -1) && (block.line >= (selectionScreenZone.y + selectionScreenZone.height))) {
			Engine.logBeans.trace("Block not selected because of wrong selection screen zone (height out of range)");
			return false;
        }
        
        // Attribute check
        if (selectionAttribute != -1) {
    		boolean b1 = ((selectionAttribute & JavelinExtractionRule.DONT_CARE_FOREGROUND_ATTRIBUTE) == 0 ? (block.attribute & iJavelin.AT_INK) == (selectionAttribute & iJavelin.AT_INK) : true);
    		boolean b2 = ((selectionAttribute & JavelinExtractionRule.DONT_CARE_BACKGROUND_ATTRIBUTE) == 0 ? (block.attribute & iJavelin.AT_PAPER) == (selectionAttribute & iJavelin.AT_PAPER) : true);
    		boolean b3 = ((selectionAttribute & JavelinExtractionRule.DONT_CARE_INTENSE_ATTRIBUTE) == 0 ? (block.attribute & iJavelin.AT_BOLD) == (selectionAttribute & iJavelin.AT_BOLD) : true);
    		boolean b4 = ((selectionAttribute & JavelinExtractionRule.DONT_CARE_UNDERLINED_ATTRIBUTE) == 0 ? (block.attribute & iJavelin.AT_UNDERLINE) == (selectionAttribute & iJavelin.AT_UNDERLINE) : true);
    		boolean b5 = ((selectionAttribute & JavelinExtractionRule.DONT_CARE_REVERSE_ATTRIBUTE) == 0 ?  (block.attribute & iJavelin.AT_INVERT) == (selectionAttribute & iJavelin.AT_INVERT) : true);
    		boolean b6 = ((selectionAttribute & JavelinExtractionRule.DONT_CARE_BLINK_ATTRIBUTE) == 0 ? (block.attribute & iJavelin.AT_BLINK) == (selectionAttribute & iJavelin.AT_BLINK) : true);
        	
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

		// Type check
        try {
            RE regexp = new RE(selectionType);
            if (!regexp.match(block.type)) return false;
        }
        catch(RESyntaxException e) {
            Engine.logBeans.error("Unable to execute the regular expression for the selection of the block", e);
        }
        
        return true;
    }
    
    /**
     * Executes the extraction rule on the current iJavelin object.
     *
     * @param javelin the Javelin object.
     * @param block the current block to analyze.
     * @param blockFactory the block context of the current block.
     * @param dom the XML DOM.
     *
     * @return an ExtractionRuleResult object containing the result of
     * the query.
     */
    protected abstract JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom);
    
    /**
     * Applies the extraction rule.
     *
     * @param javelin the Javelin object.
     * @param block the current block to analyze.
     * @param blockFactory the block context of the current block.
     * @param dom the XML DOM.
     *
     * @return an ExtractionRuleResult object containing the result of
     * the query.
     */
    public JavelinExtractionRuleResult apply(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
        if (canBlockBeSelected(block)) {
            return execute(javelin, block, blockFactory, dom);
        }
        else {
            JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
            xrs.hasMatched = false;
            xrs.newCurrentBlock = block;
            return xrs;
        }
    }

    /** Getter for property selectionScreenZone.
     * @return Value of property selectionScreenZone.
     */
    public XMLRectangle getSelectionScreenZone() {
        return selectionScreenZone;
    }
    
    /** Setter for property selectionScreenZone.
     * @param selectionScreenZone New value of property selectionScreenZone.
     */
    public void setSelectionScreenZone(XMLRectangle selectionScreenZone) {
        this.selectionScreenZone = selectionScreenZone;
    }
    
    /** Holds value of property selectionAttribute. */
    private int selectionAttribute = -1;
    
    /** Holds value of property selectionType. */
    private String selectionType = "";
    
    /** Getter for property selectionAttribute.
     * @return Value of property selectionAttribute.
     */
    public int getSelectionAttribute() {
        return this.selectionAttribute;
    }
    
    /** Setter for property selectionAttribute.
     * @param selectionAttribute New value of property selectionAttribute.
     */
    public void setSelectionAttribute(int selectionAttribute) {
        this.selectionAttribute = selectionAttribute;
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

        if (VersionUtils.compare(version, "1.2.1") <= 0) {
            NodeList properties = element.getElementsByTagName("property");
            Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "attribute");

            if (propValue == null) {
                selectionAttribute = -1;
            }

            hasChanged = true;
            Engine.logBeans.warn("[ExtractionRule] The object \"" + getName() + "\" has been updated to version 1.2.2");
        }

        if (VersionUtils.compare(version, "1.2.2") <= 0) {
            NodeList properties = element.getElementsByTagName("property");

            // attribute -> selectionAttribute
            Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "attribute");

            if (propValue != null) {
                Node xmlNode = null;
                NodeList nl = propValue.getChildNodes();
                int len_nl = nl.getLength();
                Integer iAttribute = null;
                for (int j = 0 ; j < len_nl ; j++) {
                    xmlNode = nl.item(j);
                    if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
                        iAttribute = (Integer) XMLUtils.readObjectFromXml((Element) xmlNode);
                        continue;
                    }
                }

                selectionAttribute = ((iAttribute == null) ? -1 : iAttribute.intValue());
            }

            // screenZone -> selectionScreenZone
            propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "screenZone");

            if (propValue != null) {
                Node xmlNode = null;
                NodeList nl = propValue.getChildNodes();
                int len_nl = nl.getLength();
                XMLRectangle xRectangle = null;
                for (int j = 0 ; j < len_nl ; j++) {
                    xmlNode = nl.item(j);
                    if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
                        xRectangle = (XMLRectangle) XMLUtils.readObjectFromXml((Element) xmlNode);
                        continue;
                    }
                }

                selectionScreenZone = ((xRectangle == null) ? new XMLRectangle(-1, -1, -1, -1) : xRectangle);
            }
            
            // selectionType
            selectionType = "";

            hasChanged = true;
            Engine.logBeans.warn("[ExtractionRule] The object \"" + getName() + "\" has been updated to version 1.2.3");
        }
    }
    
    /** Getter for property selectionType.
     * @return Value of property selectionType.
     */
    public String getSelectionType() {
        return this.selectionType;
    }
    
    /** Setter for property selectionType.
     * @param selectionType New value of property selectionType.
     */
    public void setSelectionType(String selectionType) {
        this.selectionType = selectionType;
    }
}
