package com.twinsoft.convertigo.engine.enums;

public enum DatabaseObjectTypes {
	BlockFactory,
	Connector,
	Criteria,
	Document,
	ExtractionRule,
	Index,
	Listener,
	MobileApplication,
	MobileComponent,
	MobileObject,
	MobilePlatform,
	Pool,
	Project,
	Reference,
	ScreenClass,
	Sequence,
	Sheet,
	Step,
	TestCase,
	Transaction,
	TransactionWithVariables,
	UrlAuthentication,
	UrlMapper,
	UrlMapping,
	UrlMappingOperation,
	UrlMappingParameter,
	UrlMappingResponse,
	Variable;

	public static DatabaseObjectTypes fromString(String type) {
		try {
			return DatabaseObjectTypes.valueOf(type);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
