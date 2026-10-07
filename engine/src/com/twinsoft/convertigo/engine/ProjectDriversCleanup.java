/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;

/**
 * Defined in a retired generation of the class path of a project (see {@link ProjectClassLoader#defineEngineClass}) to
 * deregister the JDBC drivers that its classes registered themselves, as most drivers do when loaded: DriverManager
 * only lets a class see and deregister the drivers of its own class loader, and a registered driver would keep the
 * generation in memory.
 */
public final class ProjectDriversCleanup {
	private ProjectDriversCleanup() {
	}

	/** @return the number of drivers deregistered */
	public static int deregister() {
		var generation = ProjectDriversCleanup.class.getClassLoader();
		int count = 0;
		for (Driver driver : Collections.list(DriverManager.getDrivers())) {
			if (driver.getClass().getClassLoader() == generation) {
				try {
					DriverManager.deregisterDriver(driver);
					count++;
				} catch (Exception e) {
					// kept: the generation is reported if it stays in memory
				}
			}
		}
		return count;
	}
}
