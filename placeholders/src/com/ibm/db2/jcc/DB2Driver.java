package com.ibm.db2.jcc;

import com.convertigo.jdbc.CommonDriver;

public class DB2Driver {

	final static String classPath = "com.ibm.db2.jcc.DB2Driver";
	final static String jarName = "db2jcc.jar (+ db2jcc_licence.jar)";
	
	static {
		init();
	}
	
	static void init() {
		CommonDriver.init(classPath, jarName, "");
	}
}
