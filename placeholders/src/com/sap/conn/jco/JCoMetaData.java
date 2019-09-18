package com.sap.conn.jco;

import java.io.Serializable;

public abstract interface JCoMetaData extends Serializable, Cloneable {
	public static final int UNINITIALIZED = -1;
	public static final int TYPE_INVALID = -1;
	public static final int TYPE_CHAR = 0;
	public static final int TYPE_DATE = 1;
	public static final int TYPE_BCD = 2;
	public static final int TYPE_TIME = 3;
	public static final int TYPE_BYTE = 4;
	public static final int TYPE_ITAB = 5;
	public static final int TYPE_NUM = 6;
	public static final int TYPE_FLOAT = 7;
	public static final int TYPE_INT = 8;
	public static final int TYPE_INT2 = 9;
	public static final int TYPE_INT1 = 10;
	public static final int TYPE_STRUCTURE = 17;
	public static final int TYPE_DECF16 = 23;
	public static final int TYPE_DECF34 = 24;
	public static final int TYPE_STRING = 29;
	public static final int TYPE_XSTRING = 30;
	public static final int TYPE_TABLE = 99;
	public static final int TYPE_EXCEPTION = 98;
	public static final int TYPE_ABAPOBJECT = 16;
	public static final int TYPE_BOX = 31;
	public static final int TYPE_GENERIC_BOX = 32;

	public abstract int indexOf(String paramString);

	public abstract boolean hasField(String paramString);

	public abstract String getName();

	public abstract void setName(String paramString);

	public abstract int getFieldCount();

	public abstract String getName(int paramInt);

	public abstract String getRecordTypeName(int paramInt);

	public abstract String getRecordTypeName(String paramString);

	public abstract JCoRecordMetaData getRecordMetaData(int paramInt);

	public abstract JCoRecordMetaData getRecordMetaData(String paramString);

	public abstract JCoExtendedFieldMetaData getExtendedFieldMetaData(
			int paramInt);

	public abstract JCoExtendedFieldMetaData getExtendedFieldMetaData(
			String paramString);

	public abstract int getUnicodeByteLength(int paramInt);

	public abstract int getUnicodeByteLength(String paramString);

	public abstract int getByteLength(int paramInt);

	public abstract int getByteLength(String paramString);

	public abstract int getLength(int paramInt);

	public abstract int getLength(String paramString);

	public abstract int getType(int paramInt);

	public abstract int getType(String paramString);

	public abstract String getTypeAsString(int paramInt);

	public abstract String getTypeAsString(String paramString);

	public abstract String getClassNameOfField(String paramString);

	public abstract String getClassNameOfField(int paramInt);

	public abstract int getDecimals(int paramInt);

	public abstract int getDecimals(String paramString);

	public abstract String getDescription(int paramInt);

	public abstract String getDescription(String paramString);

	public abstract boolean isStructure(int paramInt);

	public abstract boolean isStructure(String paramString);

	public abstract boolean isNestedType1Structure(int paramInt);

	public abstract boolean isNestedType1Structure(String paramString);

	public abstract boolean isTable(int paramInt);

	public abstract boolean isTable(String paramString);

	public abstract boolean isAbapObject(String paramString);

	public abstract boolean isAbapObject(int paramInt);

	public abstract void lock();

	public abstract boolean isLocked();
}
