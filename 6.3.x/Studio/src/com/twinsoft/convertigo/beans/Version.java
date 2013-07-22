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

package com.twinsoft.convertigo.beans;

import com.twinsoft.convertigo.engine.ProductVersion;

public class Version extends com.twinsoft.convertigo.engine.ProductVersion {

	// This module version is useful for migration purposes. We must increment
	// this number each time we have to perform a migration.
	// This number must be in the form "mxxx".
    public final static String moduleVersion = "m005";
    
    public final static String version = ProductVersion.productVersion + "." + Version.moduleVersion;
    
    public static void main(String[] args) {
        System.out.println("beans-" + version);
    }

}
