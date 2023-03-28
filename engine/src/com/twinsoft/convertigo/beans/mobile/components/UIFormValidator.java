/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import com.twinsoft.convertigo.engine.enums.FolderType;

public abstract class UIFormValidator extends UIComponent implements IValidator {

	private static final long serialVersionUID = -1290023592270052973L;

	public UIFormValidator() {
		super();
	}

	@Override
	public UIFormValidator clone() throws CloneNotSupportedException {
		UIFormValidator cloned = (UIFormValidator) super.clone();
		return cloned;
	}
	
	private boolean async = false;

	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	@Override
	public FolderType getFolderType() {
		return FolderType.VALIDATOR;
	}
}
