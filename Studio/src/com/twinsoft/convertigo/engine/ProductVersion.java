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

package com.twinsoft.convertigo.engine;


public class ProductVersion {

    public static String majorProductVersion = "6";
    public static String minorProductVersion = "0";
    public static String servicePack = "0";
    public static String tag = "RC";
    public static String revision = null;
    
    public static String productVersion =
    	ProductVersion.majorProductVersion + "." +
    	ProductVersion.minorProductVersion + "." +
    	ProductVersion.servicePack;
    
    public static String fullProductVersion =
    	ProductVersion.productVersion +
    	(ProductVersion.tag == null ? "" : "_" + ProductVersion.tag) +
    	(ProductVersion.revision == null ? "" : " (build " + ProductVersion.revision + ")");

    public static void main(String[] args) {
        System.out.println(ProductVersion.fullProductVersion);
    }
	
}
