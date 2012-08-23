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
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.beans.extractionrules.JavelinMashupEventExtractionRule;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.twinj.iJavelin;

public class TagName extends JavelinMashupEventExtractionRule {

	public static final long serialVersionUID = -6735666205939933877L;
    
    class FieldDesc implements Serializable {
        private static final long serialVersionUID = 2166144641789914092L;
		
		int     line;
        int     colon;
        String  literal;
        String  tagName;
        
        public FieldDesc(int l, int c, String lit, String tn) {
            line        = l;        // line of a field
            colon       = c;        // column of a field
            literal     = lit;      // literal of the block ie. text that describes the field in the screen
            tagName     = tn;       // tagName that must be used if this field matches.
        }
    }
    
    transient private XMLVector<FieldDesc> fieldList = new XMLVector<FieldDesc>();
    
    /** Holds value of property tagName. */
    private String tagName = "";
    
    public static final int LABEL_POLICY_EXPLICIT = 0;
    public static final int LABEL_POLICY_FROM_PREVIOUS_BLOCK = 1;
    public static final int LABEL_POLICY_FROM_NEXT_BLOCK = 2;
    
    /** Holds value of property labelPolicy. */
    private int labelPolicy;
    
    /** Holds value of property bSaveHistory. */
    private boolean bSaveHistory = false;
    
    /** Creates new TagName */
    public TagName() {
        super();
    }

    /**
     * Scans the fieldList to find one that matches the coordinates
     */
    private String findFieldByCoord(Block block) {
        for (int i=0; i < fieldList.size(); i++) {
            FieldDesc fd = (FieldDesc)fieldList.get(i);
            if ((fd.line == block.line) && (fd.colon == block.column))
                return (fd.tagName);
        }
        return ("");
    }
    
    /**
     * Scans the fieldList to find one that matches the literal
     */
    private String findFieldByLiteral(String literal) {
        for (int i=0; i < fieldList.size(); i++) {
            FieldDesc fd = (FieldDesc)fieldList.get(i);
            if (fd.literal != null) {
                if (fd.literal.equalsIgnoreCase(literal))
                    return (fd.tagName);
            }
        }
        return ("");
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
        String tagName = "";
        Block prevBlock;
        
        xrs.hasMatched = false;
        xrs.newCurrentBlock = block;
        
        // test if we have a tag name explicitly defined, if it is the case return the tag name
        if (this.tagName.length() != 0) {
            Engine.logBeans.trace("FieldName extraction rule matched by explicitly defined tag : " + this.tagName);
            block.tagName = this.tagName;
            block.setOptionalAttribute("history", (bSaveHistory ? "true" : "false"));
            
            addMashupAttribute(block);
            
            xrs.hasMatched = true;
            xrs.newCurrentBlock = block;
            return xrs;
        }
        
        // first search in our list of described fields for a field matching with
        // coordinates
        tagName = findFieldByCoord(block);
        if (labelPolicy == TagName.LABEL_POLICY_EXPLICIT) {
        	if (tagName.length() == 0) {
        		throw new IllegalArgumentException("TagName extraction rule: \"" + getName() + "\"\nThe label policy is set to 'Explicit' while the tag name property is empty!");
        	}
            block.tagName = tagName;
            Engine.logBeans.trace("FieldName extraction rule matched by coord. tag is : " + block.tagName);
            
			block.setOptionalAttribute("history", (bSaveHistory ? "true" : "false"));
			
			addMashupAttribute(block);
			
            xrs.hasMatched = true;
            xrs.newCurrentBlock = block;
            return xrs;
        }
        
        // we did not match by coordinates, so try to get a valid tag name by scanning all
        // the prevous or next blocks on the same line according to the scanForward setting
        prevBlock = block;
        String blockName;
        while (prevBlock.line == block.line) {
            if (labelPolicy == TagName.LABEL_POLICY_FROM_PREVIOUS_BLOCK)
                prevBlock = blockFactory.getPreviousBlock(prevBlock);
            else if (labelPolicy == TagName.LABEL_POLICY_FROM_NEXT_BLOCK)
                prevBlock = blockFactory.getNextBlock(prevBlock);
            
            if (prevBlock != null) {
                if (prevBlock.type.equalsIgnoreCase("static") ) {
                	blockName = prevBlock.getText();
                    tagName = StringUtils.normalize(blockName, false);
                    Engine.logBeans.trace("Found tag name: " + tagName + " (from " + blockName + ")");
                    if (tagName.length() != 0) {
                        if (prevBlock.line == block.line)  {
                            // we created a valid tag, so stop here
                            break;
                        }
                        else {
                            // the block is not on the same line, prepare for artificial tag
                            tagName = "";
                        }
                    }
                    else {
                        tagName = "default-tagname";
                    }
                }
            }
            else {
                tagName = "block";
                break;
            }
        }

        if (tagName.length() == 0) {
            // we did not find on the same line a valid tag, so create one artificially
            tagName = "default-tagname";
        }
        
        // we have a valid tagName, search in our Field list if we  have a literal
        String temp = findFieldByLiteral(tagName);
        if (temp.length() != 0) {
            block.tagName = temp;
            Engine.logBeans.trace("FieldName extraction rule matched by literal tag is : " + block.tagName);
        }
        else {
            block.tagName = tagName;
            Engine.logBeans.trace("FieldName extraction rule matched tag is : " + block.tagName);
        }
        
        addMashupAttribute(block);
		block.setOptionalAttribute("history", (bSaveHistory ? "true" : "false"));
        xrs.hasMatched = true;
        xrs.newCurrentBlock = block;
        return xrs;
    }
    
    /** Getter for property fieldList.
     * @return Value of property fieldList.
     */
    public List<List<Object>> getFieldList() {
    	List<List<Object>> rows = new ArrayList<List<Object>>(fieldList.size());
        
        for(FieldDesc fd : fieldList) {
            List<Object> element = new ArrayList<Object>(4);
            element.add(new Integer(fd.line));
            element.add(new Integer(fd.colon));
            element.add(new String(fd.literal));
            element.add(new String(fd.tagName));
            rows.add(element);
        }
        return rows;
    }
    
    /** Setter for property fieldList.
     * @param fieldList New value of property fieldList.
     */
    public void setFieldList(List<List<Object>> columns) {
        XMLVector<FieldDesc>  cols = new XMLVector<FieldDesc>();
        
        for(List<Object> v : columns){
            int     line, colon;
            if (v.get(0) instanceof String)
                line = Integer.parseInt((String)v.get(0));
            else line = ((Integer)v.get(0)).intValue();
            if (v.get(1) instanceof String)
                colon = Integer.parseInt((String)v.get(1));
            else colon = ((Integer)v.get(1)).intValue();
            String  literal = (String)v.get(2);
            String  tagName = (String)v.get(3);
            cols.add(new FieldDesc(line, colon, literal, tagName));
        }
        this.fieldList = cols;
    }
    
    /** Getter for property tagName.
     * @return Value of property tagName.
     */
    public String getTagName() {
        return tagName;
    }
    
    /** Setter for property tagName.
     * @param tagName New value of property tagName.
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
    
    /** Getter for property bSaveHistory.
     * @return Value of property bSaveHistory.
     */
    public boolean isSaveHistory() {
        return bSaveHistory;
    }
    
    /** Setter for property bSaveHistory.
     * @param bSaveHistory New value of property bSaveHistory.
     */
    public void setSaveHistory(boolean bSaveHistory) {
        this.bSaveHistory = bSaveHistory;
    }
    
    /**
     * Performs custom configuration for this database object.
     */
    @Override
    public void configure(Element element) throws Exception {
        super.configure(element);
        
        String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException("Unable to find version number for the database object \"" + getName() + "\".\nXML data: " + s);
            throw ee;
        }

        if (VersionUtils.compare(version, "1.2.0") <= 0) {
            if (tagName.length() != 0) {
                labelPolicy = TagName.LABEL_POLICY_EXPLICIT;
            }
            else {
                NodeList properties = element.getElementsByTagName("property");
                Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "scanForward");

                Node xmlNode = null;
                NodeList nl = propValue.getChildNodes();
                int len_nl = nl.getLength();
                Boolean bScanForward = null;
                for (int j = 0 ; j < len_nl ; j++) {
                    xmlNode = nl.item(j);
                    if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
                        bScanForward = (Boolean) XMLUtils.readObjectFromXml((Element) xmlNode);
                        continue;
                    }
                }
                
                if (bScanForward == null) {
                    labelPolicy = TagName.LABEL_POLICY_EXPLICIT;
                }
                else {
                    boolean scanForward = bScanForward.booleanValue();
                    if (scanForward) {
                        labelPolicy = TagName.LABEL_POLICY_FROM_NEXT_BLOCK;
                    }
                    else {
                        labelPolicy = TagName.LABEL_POLICY_FROM_PREVIOUS_BLOCK;
                    }
                }
            }

            hasChanged = true;
            Engine.logBeans.warn("[TagName] The object \"" + getName() + "\" has been updated to version 1.2.1");
        }

        if (VersionUtils.compare(version, "1.2.2") <= 0) {
            NodeList properties = element.getElementsByTagName("property");

            Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "editFieldsOnly");

            Node xmlNode = null;
            NodeList nl = propValue.getChildNodes();
            int len_nl = nl.getLength();
            Boolean bEditFieldsOnly = null;
            for (int j = 0 ; j < len_nl ; j++) {
                xmlNode = nl.item(j);
                if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
                    bEditFieldsOnly = (Boolean) XMLUtils.readObjectFromXml((Element) xmlNode);
                    continue;
                }
            }

            setSelectionType(((bEditFieldsOnly == null) ? "" : (bEditFieldsOnly.booleanValue() ? "field" : "")));

            hasChanged = true;
            Engine.logBeans.warn("[TagName] The object \"" + getName() + "\" has been updated to version 1.2.3");
        }
    }
    
    /** Getter for property labelPolicy.
     * @return Value of property labelPolicy.
     */
    public int getLabelPolicy() {
        return this.labelPolicy;
    }
    
    /** Setter for property labelPolicy.
     * @param labelPolicy New value of property labelPolicy.
     */
    public void setLabelPolicy(int labelPolicy) {
        this.labelPolicy = labelPolicy;
    }

	@Override
	public String toString() {
		String label = "";
		switch (labelPolicy) {
			case LABEL_POLICY_EXPLICIT: label = getTagName(); break;
			case LABEL_POLICY_FROM_NEXT_BLOCK: label = "{from next block}"; break;
			case LABEL_POLICY_FROM_PREVIOUS_BLOCK: label = "{from previous block}"; break;
		}
		if (label.equals("")) label = "{default-tagname}";
		return label;
	}
    
    
}
