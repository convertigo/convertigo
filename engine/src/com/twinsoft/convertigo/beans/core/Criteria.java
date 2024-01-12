/*
 * Copyright (c) 2001-2024 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.engine.enums.FolderType;
 
/**
 * The Criteria class is the base class for all criterias.
 */
@DboCategoryInfo(
		getCategoryId = "Criteria",
		getCategoryName = "Criteria",
		getIconClassCSS = "convertigo-action-newCriteria"
	)
@DboFolderType(type = FolderType.CRITERIA)
public abstract class Criteria extends DatabaseObject {
	private static final long serialVersionUID = 4417148740041540014L;
    
	public Criteria() {
        super();
		databaseType = "Criteria";

		// Set priority to creation time since version 4.0.1
		this.priority = getNewOrderValue();
	}
    
    /**
     * Get order for quick sort.
     */
    @Override
    public Object getOrderedValue() {
    	return priority;
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
	public Criteria clone() throws CloneNotSupportedException {
		Criteria clonedObject = (Criteria) super.clone();
		return clonedObject;
	}
	
	protected String processToString(String toString){
		if(reverseResult) return "Not "+toString;
		return toString;
	}
}