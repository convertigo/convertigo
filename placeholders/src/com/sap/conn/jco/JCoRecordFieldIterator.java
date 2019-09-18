package com.sap.conn.jco;

public abstract interface JCoRecordFieldIterator extends JCoFieldIterator {
	public abstract JCoRecordField nextRecordField();

	public abstract JCoRecordField previousRecordField();
}