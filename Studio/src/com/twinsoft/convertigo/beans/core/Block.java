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

package com.twinsoft.convertigo.beans.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.twinj.iJavelin;

/**
 * This class represents an entity used to be exported to a XML block.
 */
public class Block implements Cloneable {
	
    private final String colors[] = {"black", "red", "green", "yellow", "blue", "magenta", "cyan", "white"};

    /**
     * Indicates if other extraction rules should be applied on this block.
     */
    public boolean bFinal = false;

    /**
     * Indicates the blocks should be rendered ("XMLized") or not.
     */
    public boolean bRender = true;

    /**
     * The tag name (XML model).
     */
    public String tagName = "block";

    /**
     * The block attribute bits.
     */	
    public int attribute;

    /**
     * The block text (i.e. the block value).
     */
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        length = 0;
        this.text = text;

        if (text != null)
            length = text.length();
    }

    /**
     * The length of the text string.
     */
    public int length;

    /**
     * The horizontal coordinate of the block.
     */
    public int column;

    /**
     * The vertical coordinate of the block.
     */
    public int line;

    /**
     * The block type (i.e. static, field, action...).
     * Each extraction rule must set the block type according to the
     * associated XSL directive.
     */
    public String type = "static";

    /**
     * The block name.
     */
    public String name = "untitled";

    /**
     * The optional children (XML DOM).
     */
    private List<Block> optionalBlockChildren = new LinkedList<Block>();
    private List<Element> optionalElementChildren = new LinkedList<Element>();

    /**
     * The optional attributes list.
     */
    private Map<String,String> optionalAttributes = new HashMap<String, String>();

    /**
     * Retrieve an optional attribute.
     * 
     * @param name the attribute name.
     * 
     * @return the value of the attribute, or <code>null</code>
     * if the attribute name doesn't exist.
     */
    public String getOptionalAttribute(String name) {
        return optionalAttributes.get(name);
    }

    /**
     * Adds an optional attribute (or updates it if it has already
     * been added) .
     * 
     * @param name the attribute name.
     * @param value the value of the attribute.
     */
    public void setOptionalAttribute(String name, String value) {
        optionalAttributes.put(name, value);
    }

    public String xsdType = null;
    public String xsdTypes = null;
    private Map<String, String> types = null;
    public String maxOccurs = null;
    
    public Element toXML(Document document, boolean[] attributes) {
    	Element documentRootElement = document.getDocumentElement();
    	String transactionName = documentRootElement.getAttribute("transaction");
    	String connectorName = documentRootElement.getAttribute("connector");
    	return toXML(document, attributes, connectorName + "__" + transactionName);
    }
    
    /**
     * Returns the XML code associated with this block.
     *
     * @return the XML code associated with this block.
     */
    public Element toXML(Document document, boolean[] attributes, String prefix) {
    	
        Element item = document.createElement(tagName);

        if (attributes[Transaction.ATTRIBUTE_NAME]) item.setAttribute("name", name);
        if (attributes[Transaction.ATTRIBUTE_TYPE]) item.setAttribute("type", type);
        if (attributes[Transaction.ATTRIBUTE_COLUMN]) item.setAttribute("column", new Integer(column).toString());
        if (attributes[Transaction.ATTRIBUTE_LINE]) item.setAttribute("line", new Integer(line).toString());
        if ((attribute & iJavelin.AT_INVERT) == 0) {
            if (attributes[Transaction.ATTRIBUTE_REVERSE]) item.setAttribute("reverse", "false");
            if (attributes[Transaction.ATTRIBUTE_FOREGROUND]) item.setAttribute("foreground", colors[attribute & iJavelin.AT_INK]);
            if (attributes[Transaction.ATTRIBUTE_BACKGROUND]) item.setAttribute("background", colors[(attribute & iJavelin.AT_PAPER) >> 3]);
        }
        else {
            if (attributes[Transaction.ATTRIBUTE_REVERSE]) item.setAttribute("reverse", "true");
            if (attributes[Transaction.ATTRIBUTE_FOREGROUND]) item.setAttribute("foreground", colors[(attribute & iJavelin.AT_PAPER) >> 3]);
            if (attributes[Transaction.ATTRIBUTE_BACKGROUND]) item.setAttribute("background", colors[attribute & iJavelin.AT_INK]);
        }

        if (attributes[Transaction.ATTRIBUTE_BLINK]) if ((attribute & iJavelin.AT_BLINK) != 0) item.setAttribute("blink", "true");
        if (attributes[Transaction.ATTRIBUTE_UNDERLINE]) if ((attribute & iJavelin.AT_UNDERLINE) != 0) item.setAttribute("underline", "true");
        if (attributes[Transaction.ATTRIBUTE_INTENSE]) if ((attribute & iJavelin.AT_BOLD) != 0) item.setAttribute("intense", "true");

        // Optional attributes
        if (attributes[Transaction.ATTRIBUTE_OPTIONAL])
        	for(Map.Entry<String, String> entry : optionalAttributes.entrySet())
        		item.setAttribute(entry.getKey(), entry.getValue());

        if (text == null) text = "";
        
        Text textNode = document.createTextNode(text);
        item.appendChild(textNode);                    

       
        NamedNodeMap map = item.getAttributes();
    	boolean isComplexContent = false;
    	boolean isSimpleContent = false;
    	boolean isComplex = false;
    	
        if (Engine.isStudioMode()) {
	        xsdType = "";
	        xsdTypes = "";
	        maxOccurs = type.equals("row") ? "unbounded":"1";
	        types = new HashMap<String, String>();
	        
	        isComplexContent = hasOptionalChildren();
	        isSimpleContent = ((map != null) && (map.getLength() > 0)) && (!isComplexContent);
	    	isComplex = isSimpleContent || isComplexContent;
	    	
	    	xsdType = "<xsd:element name=\""+ tagName +"\" minOccurs=\"0\" maxOccurs=\""+ maxOccurs +"\"" + (isComplex ? " type=\"p_ns:"+ prefix +"_"+ tagName+"Type\"/>":" type=\"xsd:string\"/>");
	    	
	    	xsdTypes += isComplex ? "<xsd:complexType name=\""+ prefix +"_"+ tagName+"Type\">\n":"";
	        xsdTypes += isComplexContent ? "\t<xsd:sequence>\n":"";
        }
        
        for(Element element : optionalElementChildren){
        	item.appendChild(element);
            if (isComplexContent) {
            	xsdTypes += "\t\t<xsd:element name=\""+ element.getTagName() +"\"";
            	// some optional childs may not have a type attribute. 
            	if (element.getAttributeNode("type") != null)
            		xsdTypes += " type=\""+element.getAttributeNode("type").getNodeValue()+"\"/>";
            	else xsdTypes += "/>";
            }
        }
        for(Block block : optionalBlockChildren){
        	com.twinsoft.convertigo.engine.Engine.logBeans.trace("Block[optionalChildren]: " + block.toString());
            Element subItem = block.toXML(document, attributes, prefix +"_"+ tagName);
			item.appendChild(subItem);
			if (isComplexContent) {
				if (types.get(block.tagName) == null) {
					xsdTypes += "\t\t\t" + block.xsdType + "\n";
					types.put(block.tagName, block.xsdTypes);
				}
			}
        }

        if (Engine.isStudioMode()) {
	        xsdTypes += isComplexContent ? "\t</xsd:sequence>\n":"";
	    	xsdTypes += isSimpleContent ? "\t<xsd:simpleContent>\n":"";
	    	xsdTypes += isSimpleContent ? "\t\t<xsd:extension base=\"xsd:string\">\n":"";
	    	if (isComplex) {
	    		for (int i=0; i<map.getLength(); i++) {
	    			xsdTypes += (isSimpleContent ? "\t\t\t": "\t")+ "<xsd:attribute name=\""+ map.item(i).getNodeName()+"\"/>\n";
	    		}
	    	}
	    	xsdTypes += isSimpleContent ? "\t\t</xsd:extension>\n":"";
	    	xsdTypes += isSimpleContent ? "\t</xsd:simpleContent>\n":"";
	        xsdTypes += isComplex ? "</xsd:complexType>\n":"";
	        
	        for(String str : types.values())
	        	xsdTypes += isComplex ? str:"";
        }
        
        bRender = false;
        return item;
    }

    @Override
    public String toString() {
        return "tagName=\"" + tagName + "\", text=\"" + text + "\", column=" + column + ", line=" + line + ", length=" + length + ", attributes=" + attribute;
    }
    
    public boolean equals(Block block) {
    	// compare block by each field element
        boolean isEqual = 	(block.line == line) 			&&
			        		(block.column == column) 		&&
			        		 block.text.equals(text) 		&& 
			        		(block.attribute == attribute)	&&
			        		 block.name.equals(name) 		&&
			        		 block.type.equals(type);
    	
        // compare blocks by hash code
    	boolean isHashEqual = hashCode() == block.hashCode();
    	
    	// both comparison methods must match to have an 'equal' block
    	return (isEqual && isHashEqual);
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        Block clonedBlock = (Block) super.clone();
        clonedBlock.optionalBlockChildren = GenericUtils.clone(optionalBlockChildren);
        clonedBlock.optionalElementChildren = GenericUtils.clone(optionalElementChildren);
        clonedBlock.optionalAttributes = GenericUtils.clone(optionalAttributes);
        
        return clonedBlock;
    }
    
    public boolean addOptionalChildren(Element element){
    	return optionalElementChildren.add(element);
    }
    
    public boolean addOptionalChildren(Block block){
    	return optionalBlockChildren.add(block);
    }

    public boolean removeOptionalChildren(Element element){
    	return optionalElementChildren.remove(element);
    }
    
    public boolean removeOptionalChildren(Block block){
    	return optionalBlockChildren.remove(block);
    }
    
    public boolean hasOptionalChildren(){
    	return !optionalBlockChildren.isEmpty() || !optionalElementChildren.isEmpty();
    }
    
    public int getNumberOfOptionalChildren(){
    	return optionalBlockChildren.size() + optionalElementChildren.size();
    }
    
    public List<Block> getOptionalBlockChildren(){
    	return Collections.unmodifiableList(optionalBlockChildren);
    }
    
    public List<Element> getOptionalElementChildren(){
    	return Collections.unmodifiableList(optionalElementChildren);
    }
    
    public void setOptionalBlockChildren(int index, Block block){
    	optionalBlockChildren.set(index, block);
    }
    
    public void clearOptionalChildren(){
    	optionalBlockChildren.clear();
    	optionalElementChildren.clear();
    }
}
