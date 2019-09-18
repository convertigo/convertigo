package com.sap.conn.jco;

import java.util.List;

public abstract class JCo {
	public static List<String> getDestinationIDs() { return null; }
	public static List<String> getCustomDestinationIDs(String destinationID) throws JCoRuntimeException { return null; }
	public static JCoStructure createStructure(JCoRecordMetaData metaData) { return null; }
	public static JCoTable createTable(JCoRecordMetaData metaData) { return null; }
}
