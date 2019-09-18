package com.sap.conn.jco;

public abstract interface JCoListMetaData extends JCoMetaData {
	public static final int OPTIONAL_PARAMETER = 1;
	public static final int IMPORT_PARAMETER = 2;
	public static final int EXPORT_PARAMETER = 4;
	public static final int CHANGING_PARAMETER = 8;

	public abstract void add(String paramString, int paramInt1, int paramInt2,
			int paramInt3, int paramInt4);

	public abstract void add(String paramString, int paramInt1,
			JCoRecordMetaData paramJCoRecordMetaData, int paramInt2);

	public abstract void add(String paramString1, int paramInt1, int paramInt2,
			int paramInt3, int paramInt4, String paramString2,
			String paramString3, int paramInt5, Object paramObject,
			JCoExtendedFieldMetaData paramJCoExtendedFieldMetaData);

	public abstract void add(String paramString1, int paramInt1, int paramInt2,
			int paramInt3, int paramInt4, String paramString2,
			String paramString3, int paramInt5, Object paramObject,
			String paramString4,
			JCoExtendedFieldMetaData paramJCoExtendedFieldMetaData);

	public abstract boolean isException(int paramInt);

	public abstract boolean isException(String paramString);

	public abstract String getDefault(int paramInt);

	public abstract String getDefault(String paramString);

	public abstract String getRecordFieldName(int paramInt);

	public abstract String getRecordFieldName(String paramString);

	public abstract boolean isOptional(int paramInt);

	public abstract boolean isOptional(String paramString);

	public abstract boolean isImport(int paramInt);

	public abstract boolean isImport(String paramString);

	public abstract boolean isExport(int paramInt);

	public abstract boolean isExport(String paramString);

	public abstract boolean isChanging(int paramInt);

	public abstract boolean isChanging(String paramString);
}
