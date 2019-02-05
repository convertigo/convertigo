package com.mysql.jdbc;

import com.convertigo.jdbc.CommonDriver;

public class Driver {
	final static String classPath = "com.mysql.jdbc.Driver";
	final static String jarName = "mysql-connector.jar";
	
	static {
		init();
	}
	
	static void init() {
		CommonDriver.init(classPath, jarName, "you can download it for free at http://www.mysql.com/downloads/connector/j/.");
	}
}
