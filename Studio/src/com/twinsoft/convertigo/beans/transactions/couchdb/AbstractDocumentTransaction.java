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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */
package com.twinsoft.convertigo.beans.transactions.couchdb;


public abstract class AbstractDocumentTransaction extends AbstractDatabaseTransaction {

	private static final long serialVersionUID = 3030579754950212900L;
	
	private String p_docid = "";
	
	public AbstractDocumentTransaction() {
		super();
	}
	
	@Override
	public AbstractDocumentTransaction clone() throws CloneNotSupportedException {
		AbstractDocumentTransaction clonedObject = (AbstractDocumentTransaction) super.clone();
		return clonedObject;
	}

	public String getP_docid() {
		return p_docid;
	}

	public void setP_docid(String p_docid) {
		this.p_docid = p_docid;
	}
}
