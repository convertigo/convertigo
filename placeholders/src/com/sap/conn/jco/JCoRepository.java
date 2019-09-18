package com.sap.conn.jco;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public abstract interface JCoRepository {
	public abstract String getName();

	public abstract JCoFunctionTemplate getFunctionTemplate(String paramString)
			throws JCoException;

	public abstract JCoFunction getFunction(String paramString)
			throws JCoException;

	public abstract JCoListMetaData getFunctionInterface(String paramString)
			throws JCoException;

	public abstract JCoRecordMetaData getRecordMetaData(String paramString)
			throws JCoException;

	public abstract JCoRecordMetaData getStructureDefinition(String paramString)
			throws JCoException;

	public abstract void removeFunctionTemplateFromCache(String paramString);

	public abstract void removeRecordMetaDataFromCache(String paramString);

	public abstract void removeClassMetaDataFromCache(String paramString);

	public abstract String[] getCachedFunctionTemplateNames();

	public abstract String[] getCachedRecordMetaDataNames();

	public abstract String[] getCachedClassMetaDataNames();

	public abstract boolean isUnicode();

	public abstract void clear();

	public abstract void load(Reader paramReader) throws IOException;

	public abstract void save(Writer paramWriter) throws IOException;
}
