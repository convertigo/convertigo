package com.sap.conn.jco;

public abstract interface JCoTable extends JCoRecord {
	public abstract JCoRecordMetaData getRecordMetaData();

	public abstract void ensureBufferCapacity(int paramInt);

	public abstract void trimToRows();

	public abstract boolean isEmpty();

	public abstract boolean isFirstRow();

	public abstract boolean isLastRow();

	public abstract int getNumRows();

	public abstract int getNumColumns();

	public abstract void clear();

	public abstract void deleteAllRows();

	public abstract void firstRow();

	public abstract void lastRow();

	public abstract boolean nextRow();

	public abstract boolean previousRow();

	public abstract int getRow();

	public abstract void setRow(int paramInt);

	public abstract void appendRow();

	public abstract void appendRows(int paramInt);

	public abstract void insertRow(int paramInt);

	public abstract void deleteRow();

	public abstract void deleteRow(int paramInt);

	public abstract JCoRecordFieldIterator getRecordFieldIterator();
}
