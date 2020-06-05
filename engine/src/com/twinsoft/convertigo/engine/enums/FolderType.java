package com.twinsoft.convertigo.engine.enums;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public enum FolderType {
	NONE(null, null),
	ACTION("ac", "Actions"),
	ATTRIBUTE("at", "Attributes"),
	AUTHENTICATION("ah", "Authentications"),
	CONNECTOR("cn", "Connectors"),
	CONTROL("ct", "Controls"),
	CRITERIA("cr", "Criterias"),
	DOCUMENT("dc", "Documents"),
	EVENT("ev", "Events"),
	EXTRACTION_RULE("er", "Extraction Rules"),
	FUNCTION("fn", "Functions"),
	INDEX("id", "Indexes"),
	INHERITED_SCREEN_CLASS("in", "Inherited Screen Classes"),
	LISTENER("ls", "Listeners"),
	MAPPING("mp", "Mappings"),
	MENU("mn", "Menus"),
	OPERATION("op", "Operations"),
	PAGE("pg", "Pages"),
	PARAMETER("pr", "Parameters"),
	PLATFORM("pf", "Platforms"),
	POOL("pl", "Pools"),
	REFERENCE("rf", "References"),
	RESPONSE("rs", "Responses"),
	ROUTE("rt", "Routes"),
	SCREEN_CLASS("sc", "Screen Classes"),
	SEQUENCE("sq", "Sequences"),
	SHARED_ACTION("sa", "Shared Actions"),
	SHARED_COMPONENT("sp", "Shared Components"),
	SHEET("sh", "Sheets"),
	SOURCE("sr", "Sources"),
	STEP("st", "Steps"),
	STYLE("sl", "Styles"),
	TEMPLATE("tp", "Templates"),
	TESTCASE("tc", "Test Cases"),
	TRANSACTION("tr", "Transactions"),
	VALIDATOR("vl", "Validators"),
	VARIABLE("vr", "Variables"),
	;
	
	private String shortName;
	private String displayName;
	
	private FolderType(String shortName, String displayName) {
		this.shortName = shortName;
		this.displayName = displayName;
	}
	
	public String shortName() {
		return shortName;
	}
	
	public String displayName() {
		return displayName;
	}
	
	public String qnamePart(DatabaseObject dbo) {
		return shortName == null ?
				dbo.getName() :
				(shortName + ":" + dbo.getName());
	}
}
