/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
import com.twinsoft.convertigo.engine.enums.FolderType;

/**
 * This class defines the base class for extraction rules.
 *
 * <p>An extraction rule is seen as a JavaBean. So it is serializable,
 * and can have its own properties editor.</p>
 */
@DboCategoryInfo(
		getCategoryId = "ExtractionRule",
		getCategoryName = "Extraction rule",
		getIconClassCSS = "convertigo-action-newExtractionRule"
	)
public abstract class ExtractionRule extends DatabaseObject implements IEnableAble {
	private static final long serialVersionUID = -7322067869844724239L;
    
    /**
     * Indicates if this object is enable or not.
     */
    private boolean isEnabled = true;
    
    @Override
	public boolean isEnabled() {
		return isEnabled;
	}
    
	@Override
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
    }
    
    /**
     * Get order for quick sort.
     */
    @Override
    public Object getOrderedValue() {
    	return priority;
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
		return clonedObject;
	}

	@Override
	public boolean testAttribute(String name, String value) {
		if (name.equals("isEnabled")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isEnabled()));
		}
		return super.testAttribute(name, value);
	}

	@Override
	public FolderType getFolderType() {
		return FolderType.EXTRACTION_RULE;
	}
}
