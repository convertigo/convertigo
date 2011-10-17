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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
 
/**
 * The Criteria class is the base class for all criterias.
 */
public abstract class Criteria extends DatabaseObject {
	private static final long serialVersionUID = 4417148740041540014L;
	
	public static final String DATA_DIRECTORY = "ct";
    
	public Criteria() {
        super();
		databaseType = "Criteria";

		// Set priority to creation time since version 4.0.1
		this.priority = getNewOrderValue();
		this.newPriority = priority;
	}
	
    public String getPath() {
        return parent.getPath() + "/" + DATA_DIRECTORY;
    }
    
    /**
     * Get order for quick sort.
     */
    @Override
    public Object getOrderedValue() {
    	return new Long(priority);
    }
    
    private boolean reverseResult = false;
    
	public boolean isReverseResult() {
		return reverseResult;
	}

	public void setReverseResult(boolean reverseResult) {
		this.reverseResult = reverseResult;
	}

	/**
	 * Determines if a field matches with the criteria.
	 * 
	 * @param connector the connector object to examine.
	 * 
	 * @return <code>true</code> if the field matches with the
	 * criteria, <code>false</code> otherwise.
	 */
	public boolean isMatching(Connector connector) {
		boolean result = isMatching0(connector);
		return (reverseResult ? !result : result);
	}
	
	/**
	 * Determines if a field matches with the criteria.
	 * 
	 * @param connector the connector object to examine.
	 * 
	 * @return <code>true</code> if the field matches with the
	 * criteria, <code>false</code> otherwise.
	 */
	abstract protected boolean isMatching0(Connector connector);
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		Criteria clonedObject = (Criteria)super.clone();
		clonedObject.newPriority = newPriority;
		return clonedObject;
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#configure(org.w3c.dom.Element)
	 */
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		try {
			newPriority = new Long(element.getAttribute("newPriority")).longValue();
			if (newPriority != priority)
				hasChanged = true;
        }
        catch(Exception e) {
        	newPriority = getNewOrderValue();
        	Engine.logBeans.warn("The "+getClass().getName() +" object \"" + getName() + "\" has been updated to version \"4.0.1\"");
        	hasChanged = true;
        }
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#write(java.lang.String)
	 */
	@Override
	public void write(String databaseObjectQName) throws EngineException {
		long l = priority;
		if (hasChanged && !isImporting)
			priority = newPriority;
		try {
			super.write(databaseObjectQName);
		}
		catch (EngineException e) {
			priority = l;
			throw e;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#toXml(org.w3c.dom.Document)
	 */
	@Override
	public Element toXml(Document document) throws EngineException {
		Element element =  super.toXml(document);
		
        // Storing the object "newPriority" value
        element.setAttribute("newPriority", new Long(newPriority).toString());
		
		return element;
	}
	
	protected String processToString(String toString){
		if(reverseResult) return "Not "+toString;
		return toString;
	}
}