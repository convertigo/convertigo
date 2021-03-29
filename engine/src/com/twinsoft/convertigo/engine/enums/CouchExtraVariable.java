/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.engine.enums;

public enum CouchExtraVariable {
	_id("_id", false, "Document id"),
	_ids("_id", true, "Document ids"),
	data("data", false, "Document content"),
	datas("data", true, "Documents contents"),
	_rev("_rev", false, "Document revision"),
	_revs("_rev", true, "Documents revisions"),
	_deleted("_deleted", false, "Delete document with 'true'"),
	_deleteds("_deleted", true, "Delete documents with 'true'"),
	_c8oAcl("_" + CouchKey.c8oAcl.name(), false, "Set the owner of the document if the \"Acl Policy\"=\"From _c8oACL variable\""),
	_c8oAcls("_" + CouchKey.c8oAcl.name(), true, "Set owners of documents if the \"Acl Policy\"=\"From _c8oACL variable\""),
	c8oGrp(CouchKey.c8oGrp.name(), false, "Set the group of the document (see lib_FullSyncGrp)"),
	c8oGrps(CouchKey.c8oGrp.name(), true, "Set groups of documents (see lib_FullSyncGrp)"),
	keys("q_keys", true, "(string[]) – Return only documents where the key matches one of the keys specified in the array. Optional");
	
	private String variableName = "";
	private String variableDescription = "";
	private boolean multiValued = false;
	
	CouchExtraVariable(String variableName, boolean multiValued, String variableDescription) {
		this.variableName = variableName;
		this.multiValued = multiValued;
		this.variableDescription = variableDescription;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableDescription() {
		return variableDescription;
	}

	public void setVariableDescription(String variableDescription) {
		this.variableDescription = variableDescription;
	}

	public boolean isMultiValued() {
		return multiValued;
	}

	public void setMultiValued(boolean multiValued) {
		this.multiValued = multiValued;
	}
		
}
