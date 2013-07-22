/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.statistics;

import java.io.*;
import com.twinsoft.convertigo.engine.*;

public class MonthStatistics {
	public static double[] getContextsCreationPerDay(String month) throws IOException {
		double[] data = new double[31];

		File dir = new File(Engine.LOG_PATH);
		File file;
		File[] files;

		final String _month = month;
	
		files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.startsWith(_month) && name.endsWith("connections.log"));
			}
		});
	
		int len = files.length;
		BufferedReader br;
		String line, day;
		int nbSessions;

		for (int i = 0 ; i < len ; i++) {
			file = (File) files[i];
			day = file.getName().substring(8,10);
			br = new BufferedReader(new FileReader(file));
			nbSessions = 0;
			while ((line = br.readLine()) != null) {
				if (line.endsWith("HTTP session started")) {
					nbSessions++;
				}
			}
			br.close();
			data[Integer.parseInt(day)-1] = nbSessions;
		}
		
		return data;
	}

	public static double[] getTransactionsPerDay(String month, String project) throws IOException {
		double[] data = new double[31];

		File dir = new File(Engine.LOG_PATH);
		File file;
		File[] files;

		final String _month = month;
	
		files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.startsWith(_month) && name.endsWith("connections.log"));
			}
		});
	
		int len = files.length;
		BufferedReader br;
		String line, day;
		int nbSessions;
		boolean bCondition;
	
		for (int i = 0 ; i < len ; i++) {
			file = (File) files[i];
			day = file.getName().substring(8,10);
			br = new BufferedReader(new FileReader(file));
			nbSessions = 0;
			while ((line = br.readLine()) != null) {
				bCondition = (line.indexOf("Transaction requested") != -1);
				if (!project.equals("*")) bCondition = (bCondition && (line.indexOf("project: " + project) != -1)); 
				if (bCondition) {
					nbSessions++;
				}
			}
			br.close();
			data[Integer.parseInt(day)-1] = nbSessions;
		}
		
		return data;
	}	
	
	public static double[] getMaximumSimultaneousContextsPerDay(String month) throws IOException {
		double[] data = new double[31];

		File dir = new File(Engine.LOG_PATH);
		File file;
		File[] files;

		final String _month = month;
	
		files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.startsWith(_month) && name.endsWith("connections.log"));
			}
		});
	
		int len = files.length;
		BufferedReader br;
		String line, day;
		int nbSessions;
		int nbMaxCV = 0;
	
		for (int i = 0 ; i < len ; i++) {
			file = (File) files[i];
			day = file.getName().substring(8,10);
			br = new BufferedReader(new FileReader(file));
			nbSessions = 0;
			while ((line = br.readLine()) != null) {
				if (line.indexOf("Context created") != -1) {
					nbSessions++;
					nbMaxCV = Math.max(nbMaxCV, Math.abs(nbSessions));
				}
				else if (line.indexOf("Context removed") != -1) {
					nbSessions--;
					nbMaxCV = Math.max(nbMaxCV, Math.abs(nbSessions));
				}
			}
			br.close();
			data[Integer.parseInt(day)-1] = nbMaxCV;
			nbMaxCV = 0;
		}
		
		return data;
	}	
}
