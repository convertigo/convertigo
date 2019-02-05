package com.convertigo.jdbc;

import java.util.Arrays;


public class Tester {

	public static void main(String[] args) {		
		for (String driverClass : Arrays.asList(
				"com.ibm.db2.jcc.DB2Driver",
				"com.mysql.jdbc.Driver")) {
			
			try {
				System.out.println("Trying to load JDBC driver : " + driverClass);
				Class.forName(driverClass) ;
				System.out.println("JDBC driver loaded ok.");
			} catch (Exception e) {
				System.err.println("Exception: " + e.getMessage());
			} catch (Throwable e) {
				System.err.println("Exception: " + e.getCause().getMessage());
			}
			
		}
	}
}
