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
 * This class defines the base class for extraction rules.
 *
 * <p>An extraction rule is seen as a JavaBean. So it is serializable,
 * and can have its own properties editor.</p>
 */
public abstract class ExtractionRule extends DatabaseObject {
	private static final long serialVersionUID = -7322067869844724239L;
    
    /**
     * Indicates if this object is enable or not.
     */
    private boolean isEnabled = true;
    
	public boolean isEnabled() {
		return isEnabled;
	}
    
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
    
    /**
     * Constructs a new ExtractionRule object.
     */
    public ExtractionRule() {
        super();
        databaseType = "ExtractionRule";
        
		// Set priority to creation time since version 4.0.1
		this.priority = getNewOrderValue();
		this.newPriority = priority;
    }
    
    /**
     * Get order for quick sort.
     */
    @Override
    public Object getOrderedValue() {
    	return new Long(priority);
    }
    
    public static final int INITIALIZING = 0;
    public static final int ACCUMULATING = 1;
    
    /**
     * Initializes the extraction rule. You have to override this method if
     * your extraction rule uses internal data that should be initialized
     * before the transaction runs.
     *
     * @param reason gives the init reason.
     */
    public void init(int reason) {
    }
    
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#clone()
	 */
    @Override
	public ExtractionRule clone() throws CloneNotSupportedException {
		ExtractionRule clonedObject = (ExtractionRule) super.clone();
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
}
