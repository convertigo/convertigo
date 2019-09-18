package com.sap.conn.jco;

public abstract interface JCoStructure extends JCoRecord {
	public abstract JCoRecordMetaData getRecordMetaData();

	public abstract JCoRecordFieldIterator getRecordFieldIterator();
}
