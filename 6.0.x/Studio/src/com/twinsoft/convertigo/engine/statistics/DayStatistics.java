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

public class DayStatistics {
	public static double[] getContextsCreationPerHour(String day) throws IOException {
		double[] data = new double[24];

		BufferedReader br = new BufferedReader(new FileReader(Engine.LOG_PATH + "/" + day + "_connections.log"));
		String line;
		int hour = 0, _hour;
		int nbSessions = 0;
		while ((line = br.readLine()) != null) {
			if (line.endsWith("HTTP session started")) {
				_hour = Integer.parseInt(line.substring(0, 2));
				if (_hour == hour) {
					nbSessions++;
					continue;
				}
				data[hour] = nbSessions;
				nbSessions = 1;
				hour = _hour;
			}
		}
		br.close();
		data[hour] = nbSessions;
		
		return data;
	}

	public static double[] getTransactionsPerHour(String day, String project) throws IOException {
		double[] data = new double[24];

		BufferedReader br = new BufferedReader(new FileReader(Engine.LOG_PATH + "/" + day + "_connections.log"));
		String line;
		int hour = 0, _hour;
		int nbSessions = 0;
		boolean bCondition;
		while ((line = br.readLine()) != null) {
			bCondition = (line.indexOf("Transaction requested") != -1);
			if (!project.equals("*")) bCondition = (bCondition && (line.indexOf("project: " + project) != -1)); 
			if (bCondition) {
				_hour = Integer.parseInt(line.substring(0, 2));
				if (_hour == hour) {
					nbSessions++;
					continue;
				}
				data[hour] = nbSessions;
				nbSessions = 1;
				hour = _hour;
			}
		}
		br.close();
		data[hour] = nbSessions;
		
		return data;
	}	
	
	public static double[] getMaximumSimultaneousContextsPerHour(String day) throws IOException {
		double[] data = new double[24];

		BufferedReader br = new BufferedReader(new FileReader(Engine.LOG_PATH + "/" + day + "_connections.log"));
		String line;
		int hour = 0, _hour;
		int nbSessions = 0;
		int nbMaxCV = 0;
		while ((line = br.readLine()) != null) {
			if (line.indexOf("Context created") != -1) {
				_hour = Integer.parseInt(line.substring(0, 2));
				if (_hour == hour) {
					nbSessions++;
					nbMaxCV = Math.max(nbMaxCV, Math.abs(nbSessions));
					continue;
				}
				data[hour] = nbMaxCV;
				nbMaxCV = 0;
				hour = _hour;
			}
			else if (line.indexOf("Context removed") != -1) {
				_hour = Integer.parseInt(line.substring(0, 2));
				if (_hour == hour) {
					nbSessions--;
					nbMaxCV = Math.max(nbMaxCV, Math.abs(nbSessions));
					continue;
				}
				data[hour] = nbMaxCV;
				nbMaxCV = 0;
				hour = _hour;
			}
		}
		br.close();
		data[hour] = nbMaxCV;
		
		return data;
	}	
}
