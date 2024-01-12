/*
 * Copyright (c) 2001-2024 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

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
