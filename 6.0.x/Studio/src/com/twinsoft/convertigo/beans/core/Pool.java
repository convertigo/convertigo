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

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.util.StringUtils;

/**
 * This class manages a host application.
 */
public class Pool extends DatabaseObject implements ITagsProperty{

	private static final long serialVersionUID = 6241651041599239082L;

	public static final String DATA_DIRECTORY = "po";
	
	/**
	 * The initial screen class in which every pooled context should be. 
	 */
	private String initialScreenClass = "";

	/**
	 * The number of contexts.
	 */
	private int numberOfContexts = 1;
    
	/**
	 * The start transaction.
	 */
	private String startTransaction = "";
    
    /** Holds value of property startTransactionVariables. */
    private XMLVector<XMLVector<String>> startTransactionVariables = new XMLVector<XMLVector<String>>();
    
    /** Holds value of property connectionsParameter. */
    private XMLVector<XMLVector<String>> connectionsParameter = new XMLVector<XMLVector<String>>();
    
	/** Holds value of property serviceCode. */
	private String serviceCode = "";
    

    /**
     * Construct a new Pool object.
     */
    public Pool() {
        super();
        databaseType = "Pool";
        vPropertiesForAdmin.add("numberOfContexts");
        vPropertiesForAdmin.add("initialScreenClass");
        vPropertiesForAdmin.add("startTransaction");
    }
    
	public String getPath() {
		return parent.getPath() + "/" + DATA_DIRECTORY;
	}
    
	public String getInitialScreenClass() {
		return initialScreenClass;
	}

	public void setInitialScreenClass(String initialScreenClass) {
		this.initialScreenClass = initialScreenClass;
	}
    
	public int getNumberOfContexts() {
		return numberOfContexts;
	}
    
	public void setNumberOfContexts(int numberOfContexts) {
		this.numberOfContexts = numberOfContexts;
	}

	public String getStartTransaction() {
		return startTransaction;
	}
    
	public void setStartTransaction(String startTransaction) {
		this.startTransaction = startTransaction;
	}
    
    /** Getter for property startTransactionVariables.
     * @return Value of property startTransactionVariables.
     */
    public XMLVector<XMLVector<String>> getStartTransactionVariables() {
        return this.startTransactionVariables;
    }
    
    /** Setter for property startTransactionVariables.
     * @param startTransactionVariables New value of property startTransactionVariables.
     */
    public void setStartTransactionVariables(XMLVector<XMLVector<String>> startTransactionVariables) {
        this.startTransactionVariables = startTransactionVariables;
    }
    
    /** Getter for property connectionsParameter.
     * @return Value of property connectionsParameter.
     */
    public XMLVector<XMLVector<String>> getConnectionsParameter() {
        return this.connectionsParameter;
    }
    
    /** Setter for property connectionsParameter.
     * @param connectionsParameter New value of property connectionsParameter.
     */
    public void setConnectionsParameter(XMLVector<XMLVector<String>> connectionsParameter) {
        this.connectionsParameter = connectionsParameter;
    }
    
    /** Getter for property serviceCode.
	 * @return Value of property serviceCode.
	 */
	public String getServiceCode() {
		return serviceCode;
	}
    
	/** Setter for property serviceCode.
	 * @param serviceCode New value of property serviceCode.
	 */
	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}
	
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("startTransaction")){
			Connector connector = (Connector) getParent();
			return getNamesWithFirstEmptyItem(connector.getTransactionsList());
		} else if(propertyName.equals("initialScreenClass")){
			Connector connector = (Connector) getParent();
			if (connector instanceof IScreenClassContainer<?>) {
				String[] sNames = getNamesWithFirstEmptyItem(((IScreenClassContainer<?>)connector).getAllScreenClasses());
				for (int i = 0 ; i < sNames.length ; i++) sNames[i] = StringUtils.normalize(sNames[i]);
				return sNames;
			}
		}
		return new String[0];
	}
	
	public String getNameWithPath(){
		return getProject().getName() + "/" + getConnector().getName() + "/" + getName();
	}


}